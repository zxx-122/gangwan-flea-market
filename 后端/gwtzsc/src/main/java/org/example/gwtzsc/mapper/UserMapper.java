package org.example.gwtzsc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.gwtzsc.entity.User;

import java.math.BigDecimal;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子扣减余额（仅当余额充足时生效），返回影响行数：0 表示余额不足或用户不存在。
     * 防止并发提现/结算时的"读-改-写"丢更新与双花。
     */
    @Update("UPDATE `user` SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 原子增加余额，防止并发充值/结算互相覆盖 */
    @Update("UPDATE `user` SET balance = balance + #{amount} WHERE id = #{userId}")
    int increaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
