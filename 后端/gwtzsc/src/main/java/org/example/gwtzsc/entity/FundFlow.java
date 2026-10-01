package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_flow")
public class FundFlow {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    /** INCOME/EXPENSE/WITHDRAW/RECHARGE */
    private String type;
    private BigDecimal amount;
    /** 变动后余额 */
    private BigDecimal balance;
    private String remark;
    private Long orderId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
