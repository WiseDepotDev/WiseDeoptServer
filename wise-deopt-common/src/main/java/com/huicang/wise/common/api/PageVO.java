package com.huicang.wise.common.api;

import lombok.Data;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页视图对象
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
public class PageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private int pageIndex;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总记录数
     */
    private long totalCount;

    /**
     * 总页数
     */
    private int totalPage;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageVO() {
    }

    public PageVO(int pageIndex, int pageSize, long totalCount, List<T> list) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.list = list;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }
    
    public static <T> PageVO<T> of(int pageIndex, int pageSize, long totalCount, List<T> list) {
        return new PageVO<>(pageIndex, pageSize, totalCount, list);
    }

    public static <T> PageVO<T> empty(int pageIndex, int pageSize) {
        return new PageVO<>(pageIndex, pageSize, 0, Collections.emptyList());
    }
}
