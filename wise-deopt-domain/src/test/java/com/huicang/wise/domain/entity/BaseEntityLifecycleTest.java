package com.huicang.wise.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BaseEntity} 生命周期回调的行为测试。
 *
 * <p>这两个方法当前由 JPA 的 `@PrePersist` / `@PreUpdate` 触发。P2-05「domain 去 ORM」会移除这些注解，
 * 但**审计字段的填充语义必须保持不变**——本测试先把语义钉住，避免迁移后 `createdAt` 写空、 或 `onUpdate` 顺手改掉了 `createdAt`。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class BaseEntityLifecycleTest {

    /** 具体子类，用于访问 protected 回调并实例化抽象基类。 */
    private static class SampleEntity extends BaseEntity {

        void triggerCreate() {
            onCreate();
        }

        void triggerUpdate() {
            onUpdate();
        }
    }

    @Test
    @DisplayName("新建时应默认未删除")
    void shouldDefaultToNotDeleted() {
        assertThat(new SampleEntity().getDeleted()).isFalse();
    }

    @Test
    @DisplayName("onCreate 应同时填充 createdAt 与 updatedAt")
    void shouldFillBothTimestampsOnCreate() {
        SampleEntity entity = new SampleEntity();
        assertThat(entity.getCreatedAt()).isNull();

        entity.triggerCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    @DisplayName("onUpdate 只刷新 updatedAt，不得改动 createdAt")
    void shouldOnlyRefreshUpdatedAtOnUpdate() {
        SampleEntity entity = new SampleEntity();
        entity.triggerCreate();

        LocalDateTime createdAt = entity.getCreatedAt();
        LocalDateTime firstUpdatedAt = entity.getUpdatedAt();
        // 拉开时间差，确保"未改动"不是碰巧相等
        entity.setCreatedAt(createdAt.minusDays(1));
        entity.setUpdatedAt(firstUpdatedAt.minusDays(1));

        entity.triggerUpdate();

        assertThat(entity.getCreatedAt())
                .as("onUpdate 不应改写 createdAt")
                .isEqualTo(createdAt.minusDays(1));
        assertThat(entity.getUpdatedAt())
                .as("onUpdate 应把 updatedAt 刷新为当前时间")
                .isAfter(firstUpdatedAt.minusDays(1));
    }

    @Test
    @DisplayName("审计字段应可读写（迁移后仍需保持 setter 语义）")
    void shouldExposeAuditFieldsThroughAccessors() {
        SampleEntity entity = new SampleEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setCreatedBy("tester");
        entity.setUpdatedBy("tester-2");
        entity.setDeleted(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertThat(entity.getCreatedBy()).isEqualTo("tester");
        assertThat(entity.getUpdatedBy()).isEqualTo("tester-2");
        assertThat(entity.getDeleted()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }
}
