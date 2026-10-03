package org.example.gwtzsc.service;

import org.example.gwtzsc.dto.OrderRequest;
import org.example.gwtzsc.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Order create(Long buyerId, OrderRequest req);
    List<Map<String, Object>> getBuyerOrders(Long buyerId);
    List<Map<String, Object>> getSellerOrders(Long sellerId);
    void ship(Long sellerId, Long orderId, String courierCompany, String trackingNo);
    void confirm(Long buyerId, Long orderId);
    void cancel(Long buyerId, Long orderId);
    Order getById(Long id);

    /** 系统自动取消超时未发货订单（下单超过48小时仍是待发货），返回取消数量 */
    int autoCancelExpired();
}