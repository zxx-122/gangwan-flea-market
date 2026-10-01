package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("item")
public class Item {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    @TableField("`condition`")
    private String condition;
    private String status;
    private Integer views;
    /** 卖点标签，逗号分隔，如：全新,包邮 */
    private String tags;
    /** 是否实物拍摄：1 是，0 否 */
    private Integer isOriginal;
    /** 是否包邮：1 是，0 否 */
    private Integer isFreeShip;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}