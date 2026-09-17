package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "inspection_difference", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id")
})
public class InspectionDifference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    @Column(name = "scanned_quantity")
    private Integer scannedQuantity;

    @Column(name = "difference")
    private Integer difference;

    @Column(name = "status")
    private String status; // MISSING, EXTRA, NORMAL
}
