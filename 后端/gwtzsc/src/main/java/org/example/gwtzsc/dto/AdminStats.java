package org.example.gwtzsc.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminStats {
    private long userCount;
    private long itemCount;
    private long orderCount;
    private BigDecimal totalCommission;
    private BigDecimal todayCommission;
    private long todayOrders;
    /** 待发货订单数（管理员待办） */
    private long pendingShipCount;
    /** 待收货订单数 */
    private long pendingReceiveCount;
}
