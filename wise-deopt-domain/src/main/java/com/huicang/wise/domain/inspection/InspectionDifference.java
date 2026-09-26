package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InspectionDifference {

    private Long id;

    @NotNull private Long taskId;

    @NotNull private Long productId;

    private String productName;

    private String productCode;

    private Integer expectedQuantity;

    private Integer scannedQuantity;

    private Integer difference;

    private String status; // MISSING, EXTRA, NORMAL
}
