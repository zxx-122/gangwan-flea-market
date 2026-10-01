package org.example.gwtzsc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemRequest {
    @NotBlank(message = "商品标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    private Integer categoryId;

    private String condition;

    /** 卖点标签，逗号分隔 */
    private String tags;

    /** 是否实物拍摄：1 是，0 否 */
    private Integer isOriginal;

    /** 是否包邮：1 是，0 否 */
    private Integer isFreeShip;

    @Size(max = 9, message = "商品图片最多9张")
    private List<String> images;
}
