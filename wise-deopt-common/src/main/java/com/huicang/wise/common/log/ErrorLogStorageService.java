package com.huicang.wise.common.log;

import com.huicang.wise.common.exception.BusinessException;

/**
 * 错误日志存储服务接口
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-09
 */
public interface ErrorLogStorageService {

    /**
     * 存储错误日志
     *
     * @param logDTO 错误日志信息
     * @throws BusinessException 如果存储失败
     */
    void storeErrorLog(ServerErrorLogDTO logDTO) throws BusinessException;

    /**
     * 检查服务健康状态
     *
     * @return true表示服务可用，false表示不可用
     */
    boolean checkHealth();
}
