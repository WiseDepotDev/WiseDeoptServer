package com.huicang.wise.application.device;

import java.util.List;

/**
 * 设备分页DTO
 *
 * @author xingchentye
 * @version 1.0
 * @since 2026-02-27
 */
public class DevicePageDTO {
    private Integer page;
    private Integer size;
    private Long total;
    private List<DeviceDTO> items;

    public DevicePageDTO() {
    }

    public DevicePageDTO(Integer page, Integer size, Long total, List<DeviceDTO> items) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.items = items;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<DeviceDTO> getItems() {
        return items;
    }

    public void setItems(List<DeviceDTO> items) {
        this.items = items;
    }
}
