package com.huicang.wise.api.controller;

import com.huicang.wise.application.sync.SyncApplicationService;
import com.huicang.wise.application.sync.SyncRequest;
import com.huicang.wise.application.sync.SyncResponse;
import com.huicang.wise.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private SyncApplicationService syncApplicationService;

    @PostMapping("/data")
    public ApiResponse<SyncResponse> syncData(@RequestBody SyncRequest request) {
        SyncResponse response = syncApplicationService.syncData(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response);
        } else {
            return ApiResponse.error(500, response.getMessage());
        }
    }
}