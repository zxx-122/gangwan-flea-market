package org.example.gwtzsc.service;

import org.example.gwtzsc.entity.FundFlow;

import java.math.BigDecimal;
import java.util.List;

public interface FundFlowService {

    String TYPE_INCOME = "INCOME";
    String TYPE_EXPENSE = "EXPENSE";
    String TYPE_WITHDRAW = "WITHDRAW";
    String TYPE_RECHARGE = "RECHARGE";

    /** 记录一条资金流水（amount 为变动金额，balance 为变动后余额） */
    void record(Long userId, String type, BigDecimal amount, BigDecimal balance, String remark, Long orderId);

    /** 当前用户流水列表（按时间倒序） */
    List<FundFlow> listByUser(Long userId);
}
