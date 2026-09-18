package com.huicang.wise.api.controller;

import com.huicang.wise.application.sync.SyncApplicationService;
import com.huicang.wise.application.sync.SyncRequest;
import com.huicang.wise.application.sync.SyncResponse;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Sync", description = "Sync 接口")
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired private SyncApplicationService syncApplicationService;

    @Operation(summary = "同步离线数据")
    @PostMapping("/data")
    public ApiResponse<SyncResponse> syncData(@Valid @RequestBody SyncRequest request) {
        SyncResponse response = syncApplicationService.syncData(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response);
        } else {
            return ApiResponse.failure(ErrorCode.SYSTEM_ERROR, response.getMessage());
        }
    }
}
