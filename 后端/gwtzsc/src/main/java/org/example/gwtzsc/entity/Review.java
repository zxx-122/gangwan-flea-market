package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 关联订单，一单一评 */
    private Long orderId;
    private Long itemId;
    /** 评价人（买家） */
    private Long fromUserId;
    /** 被评价人（卖家） */
    private Long toUserId;
    /** 1-5 星 */
    private Integer rating;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
