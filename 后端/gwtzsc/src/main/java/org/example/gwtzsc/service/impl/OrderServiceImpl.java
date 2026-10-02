package org.example.gwtzsc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.dto.OrderRequest;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.entity.Order;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.mapper.OrderMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.ItemImageService;
import org.example.gwtzsc.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemImageService itemImageService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private org.example.gwtzsc.service.FundFlowService fundFlowService;

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.016");

    /** 清除首页商品列表缓存（home:items 前缀的所有 key） */
    private void clearHomeItemCache() {
        Set<String> keys = redisTemplate.keys("home:items*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    @Transactional
    public Order create(Long buyerId, OrderRequest req) {
        Item item = itemMapper.selectById(req.getItemId());
        if (item == null) throw new RuntimeException("商品不存在");
        if (!"在售".equals(item.getStatus())) throw new RuntimeException("商品已下架或已售出");
        int stock = item.getStock() == null ? 1 : item.getStock();
        if (stock <= 0) throw new RuntimeException("商品库存不足");
        if (item.getUserId().equals(buyerId)) throw new RuntimeException("不能购买自己的商品");

        BigDecimal price = item.getPrice();
        BigDecimal commission = price.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = price.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setItemId(item.getId());
        order.setSellerId(item.getUserId());
        order.setBuyerId(buyerId);
        order.setPrice(price);
        order.setCommission(commission);
        order.setSellerIncome(sellerIncome);
        order.setStatus("待发货");
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setReceiverAddress(req.getReceiverAddress());
        orderMapper.insert(order);

        // 扣减库存，售罄自动置为已售
        int remain = stock - 1;
        item.setStock(remain);
        if (remain <= 0) item.setStatus("已售");
        itemMapper.updateById(item);

        clearHomeItemCache();
        redisTemplate.delete("item:" + item.getId());

        return order;
    }

    @Override
    public List<Map<String, Object>> getBuyerOrders(Long buyerId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getCreatedAt));
        return buildOrderList(orders);
    }

    @Override
    public List<Map<String, Object>> getSellerOrders(Long sellerId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreatedAt));
        return buildOrderList(orders);
    }

    @Override
    @Transactional
    public void ship(Long sellerId, Long orderId, String courierCompany, String trackingNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getSellerId().equals(sellerId)) throw new RuntimeException("无权操作此订单");
        if (!"待发货".equals(order.getStatus())) throw new RuntimeException("当前状态无法发货");

        order.setStatus("待收货");
        order.setCourierCompany(courierCompany);
        order.setTrackingNo(trackingNo);
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void confirm(Long buyerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getBuyerId().equals(buyerId)) throw new RuntimeException("无权操作此订单");
        if (!"待收货".equals(order.getStatus())) throw new RuntimeException("当前状态无法确认收货");

        order.setStatus("已完成");
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 结算：扣买家余额，加卖家余额
        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer != null) {
            if (buyer.getBalance().compareTo(order.getPrice()) < 0) {
                throw new RuntimeException("买家余额不足，请先充值");
            }
            buyer.setBalance(buyer.getBalance().subtract(order.getPrice()));
            userMapper.updateById(buyer);
            fundFlowService.record(buyer.getId(), "EXPENSE", order.getPrice(), buyer.getBalance(),
                    "购买商品支出", order.getId());
        }
        User seller = userMapper.selectById(order.getSellerId());
        if (seller != null) {
            seller.setBalance(seller.getBalance().add(order.getSellerIncome()));
            userMapper.updateById(seller);
            fundFlowService.record(seller.getId(), "INCOME", order.getSellerIncome(), seller.getBalance(),
                    "出售商品收入", order.getId());
        }
        clearHomeItemCache();
    }

    @Override
    @Transactional
    public void cancel(Long buyerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getBuyerId().equals(buyerId)) throw new RuntimeException("无权操作此订单");
        if (!"待发货".equals(order.getStatus())) throw new RuntimeException("当前状态无法取消");

        order.setStatus("已取消");
        orderMapper.updateById(order);

        Item item = itemMapper.selectById(order.getItemId());
        if (item != null) {
            // 取消订单回补库存；若因售罄自动置为已售则恢复在售
            int stock = item.getStock() == null ? 1 : item.getStock();
            item.setStock(stock + 1);
            if ("已售".equals(item.getStatus())) item.setStatus("在售");
            itemMapper.updateById(item);
            redisTemplate.delete("item:" + item.getId());
        }
        clearHomeItemCache();
    }

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    private List<Map<String, Object>> buildOrderList(List<Order> orders) {
        // 批量查询商品与图片，避免逐行查询（N+1）
        Map<Long, Item> itemsById = orders.isEmpty() ? Collections.emptyMap()
                : itemMapper.selectBatchIds(orders.stream().map(Order::getItemId).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(Item::getId, i -> i));
        Map<Long, List<String>> imagesByItem = itemImageService.getImagesByItemIds(itemsById.keySet());

        return orders.stream().map(order -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("itemId", order.getItemId());
            map.put("sellerId", order.getSellerId());
            map.put("buyerId", order.getBuyerId());
            map.put("price", order.getPrice());
            map.put("commission", order.getCommission());
            map.put("sellerIncome", order.getSellerIncome());
            map.put("status", order.getStatus());
            map.put("receiverName", order.getReceiverName());
            map.put("receiverPhone", order.getReceiverPhone());
            map.put("receiverAddress", order.getReceiverAddress());
            map.put("courierCompany", order.getCourierCompany());
            map.put("trackingNo", order.getTrackingNo());
            map.put("shippedAt", order.getShippedAt());
            map.put("completedAt", order.getCompletedAt());
            map.put("createdAt", order.getCreatedAt());

            Item item = itemsById.get(order.getItemId());
            if (item != null) {
                map.put("itemTitle", item.getTitle());
                List<String> images = imagesByItem.getOrDefault(item.getId(), Collections.emptyList());
                map.put("itemImage", images.isEmpty() ? null : images.get(0));
            }
            return map;
        }).collect(Collectors.toList());
    }
}