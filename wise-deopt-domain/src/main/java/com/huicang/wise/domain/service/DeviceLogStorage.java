package com.huicang.wise.domain.service;

import java.io.InputStream;

/**
 * 接口功能描述：设备日志存储服务接口
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-13
 */
public interface DeviceLogStorage {
    /**
     * 存储设备日志
     *
     * @param deviceCode 设备编码
     * @param fileName 文件名
     * @param content 内容流
     * @param size 大小
     */
    void storeLog(String deviceCode, String fileName, InputStream content, long size);
}
