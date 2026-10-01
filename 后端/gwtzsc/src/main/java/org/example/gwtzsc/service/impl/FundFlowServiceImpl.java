package org.example.gwtzsc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.entity.FundFlow;
import org.example.gwtzsc.mapper.FundFlowMapper;
import org.example.gwtzsc.service.FundFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FundFlowServiceImpl implements FundFlowService {

    @Autowired
    private FundFlowMapper fundFlowMapper;

    @Override
    public void record(Long userId, String type, BigDecimal amount, BigDecimal balance, String remark, Long orderId) {
        FundFlow flow = new FundFlow();
        flow.setUserId(userId);
        flow.setType(type);
        flow.setAmount(amount);
        flow.setBalance(balance);
        flow.setRemark(remark);
        flow.setOrderId(orderId);
        fundFlowMapper.insert(flow);
    }

    @Override
    public List<FundFlow> listByUser(Long userId) {
        return fundFlowMapper.selectList(
                new LambdaQueryWrapper<FundFlow>()
                        .eq(FundFlow::getUserId, userId)
                        .orderByDesc(FundFlow::getCreatedAt)
                        .orderByDesc(FundFlow::getId));
    }
}
