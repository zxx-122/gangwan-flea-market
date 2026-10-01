# -*- coding: utf-8 -*-
"""
中国象棋 (Xiangqi) - 单文件 Python 实现
图形界面使用 tkinter，无需第三方依赖。

特性：
- 完整的中国象棋规则：车马炮相仕帅兵卒的走法、蹩马腿、塞象眼、炮翻山、过河兵等
- 将军 / 将死判定，不能让己方被将军的走法
- 交替行棋（红方先走），鼠标点击移动棋子
- 悔棋、重开、走子提示（合法落点高亮）
"""

import tkinter as tk
from tkinter import messagebox
from copy import deepcopy

# ---------------------------------------------------------------------------
# 棋盘表示
# ---------------------------------------------------------------------------
# 棋盘为 9 列 x 10 行（列 0-8，行 0-9）。
# 行 0 在顶部（黑方底线），行 9 在底部（红方底线）。
# 棋子用两字符字符串表示：颜色('r'红/'b'黑) + 类型。
#   类型: K 帅/将, A 仕/士, E 相/象, H 马, R 车, C 炮, P 兵/卒
# 空位用 '.' 表示。

EMPTY = '.'

# 初始局面（红方在下，黑方在上）
INITIAL_BOARD = [
    ['bR', 'bH', 'bE', 'bA', 'bK', 'bA', 'bE', 'bH', 'bR'],
    [EMPTY] * 9,
    [EMPTY, 'bC', EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, 'bC', EMPTY],
    ['bP', EMPTY, 'bP', EMPTY, 'bP', EMPTY, 'bP', EMPTY, 'bP'],
    [EMPTY] * 9,
    [EMPTY] * 9,
    ['rP', EMPTY, 'rP', EMPTY, 'rP', EMPTY, 'rP', EMPTY, 'rP'],
    [EMPTY, 'rC', EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, 'rC', EMPTY],
    [EMPTY] * 9,
    ['rR', 'rH', 'rE', 'rA', 'rK', 'rA', 'rE', 'rH', 'rR'],
]

ROWS, COLS = 10, 9


def color_of(piece):
    return piece[0] if piece != EMPTY else None


def type_of(piece):
    return piece[1] if piece != EMPTY else None


def opponent(color):
    return 'b' if color == 'r' else 'r'


# ---------------------------------------------------------------------------
# 走子规则
# ---------------------------------------------------------------------------

def in_board(r, c):
    return 0 <= r < ROWS and 0 <= c < COLS


def in_palace(r, c, color):
    """判断 (r,c) 是否在给定颜色的九宫内。"""
    if not (3 <= c <= 5):
        return False
    if color == 'r':          # 红方九宫在底部
        return 7 <= r <= 9
    else:                     # 黑方九宫在顶部
        return 0 <= r <= 2


def on_own_side(r, color):
    """兵/卒是否在己方一半（未过河）。红方在下，黑方在上。"""
    if color == 'r':
        return r >= 5
    else:
        return r <= 4


def gen_pseudo_moves(board, r, c):
    """生成某个棋子的所有伪合法走法（不检查是否送将）。"""
    piece = board[r][c]
    if piece == EMPTY:
        return []
    color = color_of(piece)
    t = type_of(piece)
    moves = []

    def add(nr, nc):
        if in_board(nr, nc):
            target = board[nr][nc]
            if target == EMPTY or color_of(target) != color:
                moves.append((nr, nc))

    if t == 'K':  # 帅/将：九宫内一步直行
        for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nr, nc = r + dr, c + dc
            if in_palace(nr, nc, color):
                add(nr, nc)
        # 将帅照面（飞将）：同列且中间无子，可视为"吃对方将"
        for dr in (-1, 1):
            nr = r + dr
            step = dr
            while in_board(nr, c):
                if board[nr][c] != EMPTY:
                    if type_of(board[nr][c]) == 'K' and color_of(board[nr][c]) != color:
                        moves.append((nr, c))
                    break
                nr += step

    elif t == 'A':  # 仕/士：九宫内斜走一步
        for dr, dc in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
            nr, nc = r + dr, c + dc
            if in_palace(nr, nc, color):
                add(nr, nc)

    elif t == 'E':  # 相/象：田字，塞象眼
        for dr, dc in ((-2, -2), (-2, 2), (2, -2), (2, 2)):
            nr, nc = r + dr, c + dc
            er, ec = r + dr // 2, c + dc // 2   # 象眼
            if not in_board(nr, nc):
                continue
            if board[er][ec] != EMPTY:          # 塞象眼
                continue
            if not on_own_side(nr, color):      # 不能过河
                continue
            add(nr, nc)

    elif t == 'H':  # 马：日字，蹩马腿
        for dr, dc in ((-2, -1), (-2, 1), (2, -1), (2, 1),
                       (-1, -2), (-1, 2), (1, -2), (1, 2)):
            nr, nc = r + dr, c + dc
            if not in_board(nr, nc):
                continue
            # 马腿位置：先直后斜，判断第一步经过的格
            if abs(dr) == 2:
                leg_r, leg_c = r + dr // 2, c
            else:
                leg_r, leg_c = r, c + dc // 2
            if board[leg_r][leg_c] != EMPTY:    # 蹩马腿
                continue
            add(nr, nc)

    elif t == 'R':  # 车：直线滑行
        for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nr, nc = r + dr, c + dc
            while in_board(nr, nc):
                target = board[nr][nc]
                if target == EMPTY:
                    moves.append((nr, nc))
                else:
                    if color_of(target) != color:
                        moves.append((nr, nc))
                    break
                nr += dr
                nc += dc

    elif t == 'C':  # 炮：移动像车，吃子需隔一个炮架
        for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nr, nc = r + dr, c + dc
            jumped = False
            while in_board(nr, nc):
                target = board[nr][nc]
                if not jumped:
                    if target == EMPTY:
                        moves.append((nr, nc))
                    else:
                        jumped = True     # 遇到第一个子作为炮架
                else:
                    if target != EMPTY:
                        if color_of(target) != color:
                            moves.append((nr, nc))
                        break
                nr += dr
                nc += dc

    elif t == 'P':  # 兵/卒：向前一步，过河后可左右
        forward = -1 if color == 'r' else 1   # 红方向上(行号减小)
        add(r + forward, c)
        if not on_own_side(r, color):         # 已过河
            add(r, c - 1)
            add(r, c + 1)

    return moves


def find_king(board, color):
    for r in range(ROWS):
        for c in range(COLS):
            if board[r][c] == color + 'K':
                return r, c
    return None


def is_attacked(board, r, c, by_color):
    """判断格子 (r,c) 是否被 by_color 方攻击。"""
    for rr in range(ROWS):
        for cc in range(COLS):
            p = board[rr][cc]
            if p != EMPTY and color_of(p) == by_color:
                if (r, c) in gen_pseudo_moves(board, rr, cc):
                    return True
    return False


def in_check(board, color):
    pos = find_king(board, color)
    if pos is None:
        return True
    return is_attacked(board, pos[0], pos[1], opponent(color))


def apply_move(board, move):
    (r1, c1), (r2, c2) = move
    board[r2][c2] = board[r1][c1]
    board[r1][c1] = EMPTY


def gen_legal_moves(board, color):
    """生成 color 方的所有合法走法（过滤掉会让己方被将军的走法）。"""
    result = []
    for r in range(ROWS):
        for c in range(COLS):
            if board[r][c] != EMPTY and color_of(board[r][c]) == color:
                for (nr, nc) in gen_pseudo_moves(board, r, c):
                    nb = deepcopy(board)
                    apply_move(nb, ((r, c), (nr, nc)))
                    if not in_check(nb, color):
                        result.append(((r, c), (nr, nc)))
    return result


def legal_moves_from(board, r, c):
    color = color_of(board[r][c])
    res = []
    for (a, b) in gen_pseudo_moves(board, r, c):
        nb = deepcopy(board)
        apply_move(nb, ((r, c), a))
        if not in_check(nb, color):
            res.append(a)
    return res


def has_any_legal_move(board, color):
    return len(gen_legal_moves(board, color)) > 0


# ---------------------------------------------------------------------------
# 图形界面
# ---------------------------------------------------------------------------

# 中文字符
CHAR = {
    'rK': '帅', 'rA': '仕', 'rE': '相', 'rH': '馬', 'rR': '車', 'rC': '炮', 'rP': '兵',
    'bK': '将', 'bA': '士', 'bE': '象', 'bH': '馬', 'bR': '車', 'bC': '砲', 'bP': '卒',
}


class XiangqiApp:
    CELL = 70          # 格子像素
    MARGIN = 45        # 边距

    def __init__(self, root):
        self.root = root
        self.root.title('中国象棋')
        self.board = deepcopy(INITIAL_BOARD)
        self.turn = 'r'                # 红先
        self.history = []              # 供悔棋
        self.selected = None           # 当前选中(行,列)
        self.hints = []                # 合法落点列表

        w = self.MARGIN * 2 + self.CELL * (COLS - 1)
        h = self.MARGIN * 2 + self.CELL * (ROWS - 1)
        self.canvas = tk.Canvas(root, width=w, height=h, bg='#f0d9a7',
                                highlightthickness=0)
        self.canvas.pack()
        self.canvas.bind('<Button-1>', self.on_click)

        self.status = tk.Label(root, text='', font=('Microsoft YaHei', 14))
        self.status.pack(fill='x')

        btn_frame = tk.Frame(root)
        btn_frame.pack(pady=6)
        tk.Button(btn_frame, text='悔棋', width=10,
                  command=self.undo).pack(side='left', padx=5)
        tk.Button(btn_frame, text='重新开始', width=10,
                  command=self.restart).pack(side='left', padx=5)

        self.draw()
        self.update_status()

    # ---- 坐标换算 ----
    def xy(self, r, c):
        x = self.MARGIN + c * self.CELL
        y = self.MARGIN + r * self.CELL
        return x, y

    def rc_from_xy(self, x, y):
        c = round((x - self.MARGIN) / self.CELL)
        r = round((y - self.MARGIN) / self.CELL)
        if in_board(r, c):
            # 限制点击落点距离交点不能太远
            cx, cy = self.xy(r, c)
            if abs(cx - x) <= self.CELL // 2 and abs(cy - y) <= self.CELL // 2:
                return r, c
        return None

    # ---- 绘制 ----
    def draw(self):
        self.canvas.delete('all')
        cv = self.canvas
        x0, y0 = self.xy(0, 0)
        x1, y1 = self.xy(ROWS - 1, COLS - 1)

        # 外框
        cv.create_rectangle(x0 - 8, y0 - 8, x1 + 8, y1 + 8,
                            outline='#5a3a1a', width=3)

        # 横线
        for r in range(ROWS):
            _, y = self.xy(r, 0)
            cv.create_line(x0, y, x1, y, fill='#5a3a1a', width=1)
        # 竖线（河界在中间断开：中间列 c=0..8，第 4、5 行之间是河）
        for c in range(COLS):
            x, _ = self.xy(0, c)
            if c == 0 or c == COLS - 1:
                cv.create_line(x, y0, x, y1, fill='#5a3a1a', width=1)
            else:
                # 上半段
                _, ya = self.xy(4, 0)
                _, yb = self.xy(5, 0)
                cv.create_line(x, y0, x, ya, fill='#5a3a1a', width=1)
                cv.create_line(x, yb, x, y1, fill='#5a3a1a', width=1)

        # 九宫斜线
        for (r1, r2, c1, c2) in [(0, 2, 3, 5), (7, 9, 3, 5)]:
            ax, ay = self.xy(r1, c1)
            bx, by = self.xy(r2, c2)
            cv.create_line(ax, ay, bx, by, fill='#5a3a1a', width=1)
            ax, ay = self.xy(r1, c2)
            bx, by = self.xy(r2, c1)
            cv.create_line(ax, ay, bx, by, fill='#5a3a1a', width=1)

        # 河界文字
        mid_y = (self.xy(4, 0)[1] + self.xy(5, 0)[1]) / 2
        cv.create_text(self.MARGIN + self.CELL, mid_y, text='楚 河',
                       font=('Microsoft YaHei', 20), fill='#5a3a1a')
        cv.create_text(self.MARGIN + self.CELL * 7, mid_y, text='汉 界',
                       font=('Microsoft YaHei', 20), fill='#5a3a1a')

        # 合法落点提示
        for (r, c) in self.hints:
            x, y = self.xy(r, c)
            rad = self.CELL // 2 - 6
            cv.create_oval(x - rad, y - rad, x + rad, y + rad,
                           outline='#2e8b57', width=2, dash=(4, 3))

        # 棋子
        for r in range(ROWS):
            for c in range(COLS):
                p = self.board[r][c]
                if p != EMPTY:
                    self.draw_piece(r, c, p)

        # 选中高亮
        if self.selected:
            r, c = self.selected
            x, y = self.xy(r, c)
            rad = self.CELL // 2 - 3
            cv.create_rectangle(x - rad, y - rad, x + rad, y + rad,
                                outline='#e09000', width=3)

    def draw_piece(self, r, c, piece):
        cv = self.canvas
        x, y = self.xy(r, c)
        rad = self.CELL // 2 - 6
        color = color_of(piece)
        fg = '#c01818' if color == 'r' else '#1a1a1a'
        cv.create_oval(x - rad, y - rad, x + rad, y + rad,
                       fill='#fdf5dc', outline=fg, width=2)
        cv.create_text(x, y, text=CHAR.get(piece, '?'),
                       font=('Microsoft YaHei', 22, 'bold'), fill=fg)

    # ---- 交互 ----
    def on_click(self, event):
        hit = self.rc_from_xy(event.x, event.y)
        if hit is None:
            return
        r, c = hit
        piece = self.board[r][c]

        if self.selected:
            sr, sc = self.selected
            if (r, c) in self.hints:
                self.do_move((sr, sc), (r, c))
                return
            # 点自己其它棋子则改选
            if piece != EMPTY and color_of(piece) == self.turn:
                self.select(r, c)
            else:
                self.clear_selection()
        else:
            if piece != EMPTY and color_of(piece) == self.turn:
                self.select(r, c)

    def select(self, r, c):
        self.selected = (r, c)
        self.hints = legal_moves_from(self.board, r, c)
        self.draw()

    def clear_selection(self):
        self.selected = None
        self.hints = []
        self.draw()

    def do_move(self, src, dst):
        self.history.append((deepcopy(self.board), self.turn))
        apply_move(self.board, (src, dst))
        self.selected = None
        self.hints = []
        self.turn = opponent(self.turn)
        self.draw()
        self.update_status()
        self.check_end()

    def check_end(self):
        if not has_any_legal_move(self.board, self.turn):
            loser = '红方' if self.turn == 'r' else '黑方'
            winner = '黑方' if self.turn == 'r' else '红方'
            self.draw()
            messagebox.showinfo('对局结束', f'{loser}被将死！{winner}胜！')

    def update_status(self):
        side = '红方' if self.turn == 'r' else '黑方'
        extra = '（将军！）' if in_check(self.board, self.turn) else ''
        self.status.config(text=f'轮到 {side} 走棋 {extra}')

    def undo(self):
        if not self.history:
            return
        self.board, self.turn = self.history.pop()
        self.selected = None
        self.hints = []
        self.draw()
        self.update_status()

    def restart(self):
        self.board = deepcopy(INITIAL_BOARD)
        self.turn = 'r'
        self.history = []
        self.selected = None
        self.hints = []
        self.draw()
        self.update_status()


def main():
    root = tk.Tk()
    XiangqiApp(root)
    root.mainloop()


if __name__ == '__main__':
    main()
