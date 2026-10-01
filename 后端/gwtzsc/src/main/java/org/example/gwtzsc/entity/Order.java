package org.example.gwtzsc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long itemId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal price;
    private BigDecimal commission;
    private BigDecimal sellerIncome;
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String courierCompany;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
