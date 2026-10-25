package org.example.gwtzsc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.gwtzsc.entity.Item;

public interface ItemMapper extends BaseMapper<Item> {

    /**
     * 原子扣减库存并自动置"已售"，返回影响行数：0 表示库存不足。
     * WHERE stock > 0 保证并发下单不会超卖；CASE 判断用扣减前的 stock。
     */
    @Update("UPDATE item SET stock = stock - 1, "
            + "status = CASE WHEN stock <= 1 THEN '已售' ELSE status END "
            + "WHERE id = #{itemId} AND stock > 0")
    int deductStock(@Param("itemId") Long itemId);

    /** 原子回补库存（取消订单），若商品因售罄被置为"已售"则恢复"在售" */
    @Update("UPDATE item SET stock = stock + 1, "
            + "status = CASE WHEN status = '已售' THEN '在售' ELSE status END "
            + "WHERE id = #{itemId}")
    int restock(@Param("itemId") Long itemId);
}
