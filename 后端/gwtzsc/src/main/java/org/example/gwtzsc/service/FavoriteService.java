package org.example.gwtzsc.service;

import java.util.List;
import java.util.Map;

public interface FavoriteService {
    void add(Long userId, Long itemId);
    void remove(Long userId, Long itemId);
    List<Map<String, Object>> getList(Long userId);
    boolean check(Long userId, Long itemId);
}
