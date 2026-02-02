package com.huicang.wise.infrastructure.repository.inspection;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 巡检路线实体
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Entity
@Table(name = "inspection_route")
public class InspectionRouteJpaEntity {

    @Id
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "route_name", length = 100)
    private String routeName;

    @Column(name = "route_data", columnDefinition = "TEXT")
    private String routeData;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
