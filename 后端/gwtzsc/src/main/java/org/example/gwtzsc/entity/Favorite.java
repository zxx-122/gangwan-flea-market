package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long itemId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
