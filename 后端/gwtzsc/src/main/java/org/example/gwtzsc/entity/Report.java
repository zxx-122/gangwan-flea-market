package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long reporterId;
    /** ITEM 商品 / USER 用户 */
    private String targetType;
    private Long targetId;
    private String reason;
    /** 待处理 / 已处理 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
