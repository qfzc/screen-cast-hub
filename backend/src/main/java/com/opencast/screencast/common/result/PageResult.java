package com.opencast.screencast.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private Long total;

    /**
     * 列表数据
     */
    private List<T> list;

    /**
     * 当前页
     */
    private Long page;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
    }

    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public PageResult(Long total, List<T> list, Long page, Long size) {
        this.total = total;
        this.list = list;
        this.page = page;
        this.size = size;
        this.pages = (total + size - 1) / size;
    }

    public static <T> PageResult<T> of(Long total, List<T> list) {
        return new PageResult<>(total, list);
    }

    public static <T> PageResult<T> of(Long total, List<T> list, Long page, Long size) {
        return new PageResult<>(total, list, page, size);
    }
}
