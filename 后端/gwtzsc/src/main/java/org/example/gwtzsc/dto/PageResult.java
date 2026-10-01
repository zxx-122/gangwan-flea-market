package org.example.gwtzsc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> data;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> data) {
        return new PageResult<>(total, page, size, data);
    }
}
