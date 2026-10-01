package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("item_image")
public class ItemImage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long itemId;
    private String url;
    private Integer sort;
}
