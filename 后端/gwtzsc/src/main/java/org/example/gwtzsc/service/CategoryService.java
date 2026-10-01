package org.example.gwtzsc.service;

import org.example.gwtzsc.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getList();
    void create(Category category);
    void update(Category category);
    void delete(Integer id);
}