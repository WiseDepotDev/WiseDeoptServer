package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "手动补录请求")
public class ManualRecordRequest {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", required = true)
    private Long taskId;

    @NotEmpty(message = "补录明细不能为空")
    @Schema(description = "补录明细列表", required = true)
    private List<ManualRecordItem> items;

    @Data
    @Schema(description = "补录明细项")
    public static class ManualRecordItem {

        @NotNull(message = "NFC不能为空")
        @Schema(description = "NFC标签", required = true)
        private String rfid;

        @Schema(description = "TID")
        private String tid;

        @Schema(description = "备注")
        private String remark;
    }
}
