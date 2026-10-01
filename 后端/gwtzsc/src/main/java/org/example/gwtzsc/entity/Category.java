package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.ASSIGN_ID)
    private Integer id;
    private String name;
    private String icon;
    private Integer sort;
}
