package com.huicang.wise.api.controller;

import com.huicang.wise.application.rfid.RfidDataApplicationService;
import com.huicang.wise.application.rfid.RfidReportDTO;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RFID数据上报控制器
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@RestController
@RequestMapping("/rfid")
@Tag(name = "RFID数据上报控制器")
public class RfidDataController {

    private final RfidDataApplicationService rfidDataApplicationService;

    public RfidDataController(RfidDataApplicationService rfidDataApplicationService) {
        this.rfidDataApplicationService = rfidDataApplicationService;
    }

    /**
     * RFID数据上报接口
     *
     * @param request RFID数据上报DTO
     * @return 处理结果
     */
    @PostMapping("/report")
    @Operation(summary = "RFID数据上报接口")
    public ApiResponse<Void> reportRfidData(
            @Parameter(description = "RFID数据上报DTO", required = true)
            @RequestBody RfidReportDTO request) {
        rfidDataApplicationService.processRfidReport(request);
        return ApiResponse.success(null);
    }
}

