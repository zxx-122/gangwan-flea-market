package org.example.gwtzsc.task;

import org.example.gwtzsc.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单定时任务：自动取消超时未发货订单（下单超过 48 小时仍待发货）。
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    @Autowired
    private OrderService orderService;

    /** 每 30 分钟执行一次 */
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void cancelExpiredOrders() {
        try {
            int n = orderService.autoCancelExpired();
            if (n > 0) {
                log.info("[OrderTimeout] 已自动取消 {} 笔超时未发货订单", n);
            }
        } catch (Exception e) {
            log.error("[OrderTimeout] 自动取消超时订单失败", e);
        }
    }
}
