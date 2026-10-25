package org.example.gwtzsc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.gwtzsc.entity.Order;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 订单状态条件更新（CAS）：仅当当前状态等于 fromStatus 时才更新为 toStatus。
     * 返回影响行数：0 表示状态已被并发请求修改，拒绝重复操作
     * （防止确认收货双重结算、取消订单重复回补库存）。
     */
    @Update("UPDATE `order` SET status = #{toStatus} WHERE id = #{orderId} AND status = #{fromStatus}")
    int casStatus(@Param("orderId") Long orderId,
                  @Param("fromStatus") String fromStatus,
                  @Param("toStatus") String toStatus);
}
