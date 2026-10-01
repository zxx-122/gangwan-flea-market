package org.example.gwtzsc.service;

import org.example.gwtzsc.dto.AdminStats;
import org.example.gwtzsc.dto.PageResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AdminService {
    AdminStats getStats();
    PageResult<Map<String, Object>> getUsers(int page, int size, String keyword);
    void updateUserStatus(Long id, Integer status);
    void setUserBalance(Long id, BigDecimal balance);
    PageResult<Map<String, Object>> getItems(int page, int size, String keyword, String status);
    void offlineItem(Long id);
    void relistItem(Long id);
    PageResult<Map<String, Object>> getOrders(int page, int size, String status, String keyword);
        List<Map<String, Object>> getRevenue();
    void deleteUser(Long id);
}
