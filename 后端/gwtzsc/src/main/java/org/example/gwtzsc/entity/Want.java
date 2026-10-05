package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("want")
public class Want {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    /** 期望价格上限（可空） */
    private BigDecimal priceMax;
    private Integer categoryId;
    /** 求购中 / 已成交 / 已下架 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
