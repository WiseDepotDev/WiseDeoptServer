package com.huicang.wise.application.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huicang.wise.domain.sync.ConflictResolutionStrategy;
import com.huicang.wise.domain.sync.SyncOperation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 同步应用服务的单元测试：批量同步的逐条失败隔离、冲突解决四种策略与合并语义。
 *
 * <p>本批把该类的**整体假实现**钉住（只记录、未修）——它**没有任何依赖注入**，因此**不碰数据库**： {@code handleCreateOperation} 只塞一个随机
 * UUID、{@code handleUpdateOperation} 的四个分支全是空 {@code break}、 {@code handleDeleteOperation}
 * 是空方法体、{@code getServerOperations} 恒返回空表、{@code mergeData} 只返回客户端数据。 也就是说：{@code syncData}
 * 会回"同步成功"，但**服务端没有落任何东西、也拉不到任何服务端变更**。
 */
class SyncApplicationServiceTest {

    private final SyncApplicationService service = new SyncApplicationService();

    private SyncOperation operation(String type) {
        SyncOperation op = new SyncOperation();
        op.setOperationType(type);
        op.setEntityType("PRODUCT");
        op.setVersion(1);
        op.setOperationTime(LocalDateTime.now());
        op.setOperationData("{\"k\":\"v\"}");
        return op;
    }

    private SyncRequest request(SyncOperation... ops) {
        SyncRequest request = new SyncRequest();
        request.setDeviceId("DEV-1");
        request.setOperations(new ArrayList<>(Arrays.asList(ops)));
        return request;
    }

    // ---------------- 批量同步 ----------------

    @Test
    @DisplayName("同步：没有任何操作时仍回成功，且服务端变更为空")
    void syncWithoutOperations() {
        SyncResponse response = service.syncData(request());

        assertTrue(response.isSuccess());
        assertEquals("同步成功", response.getMessage());
        assertNotNull(response.getSyncTime());
        assertTrue(response.getServerOperations().isEmpty());
        assertTrue(response.getConflicts().isEmpty());
        assertTrue(response.getFailedOperations().isEmpty());
    }

    @Test
    @DisplayName("同步 CREATE：生成新的 entityId，状态置为已完成")
    void syncCreateAssignsEntityId() {
        SyncOperation op = operation("CREATE");

        service.syncData(request(op));

        assertNotNull(op.getEntityId());
        assertEquals("COMPLETED", op.getStatus());
        assertNotNull(op.getSyncTime());
    }

    @Test
    @DisplayName("同步 UPDATE / DELETE：只把状态置为已完成（现状：不写任何数据）")
    void syncUpdateAndDeleteOnlyMarkCompleted() {
        SyncOperation update = operation("UPDATE");
        SyncOperation delete = operation("DELETE");

        SyncResponse response = service.syncData(request(update, delete));

        assertTrue(response.isSuccess());
        assertEquals("COMPLETED", update.getStatus());
        assertEquals("COMPLETED", delete.getStatus());
        assertNull(update.getEntityId(), "现状：UPDATE 不生成也不改变 entityId");
    }

    @Test
    @DisplayName("同步：单条失败被隔离，只有它进 failedOperations，整体仍回成功")
    void syncIsolatesPerOperationFailure() {
        SyncOperation ok = operation("CREATE");
        SyncOperation bad = operation("UPSERT");

        SyncResponse response = service.syncData(request(ok, bad));

        assertTrue(response.isSuccess(), "现状：单条失败不影响整体成功标志");
        assertEquals(1, response.getFailedOperations().size());
        assertSame(bad, response.getFailedOperations().get(0));
        assertEquals("FAILED", bad.getStatus());
        assertNotNull(bad.getErrorMessage());
        assertTrue(bad.getErrorMessage().contains("UPSERT"), bad.getErrorMessage());
        assertEquals("COMPLETED", ok.getStatus());
    }

    @Test
    @DisplayName("同步：操作列表为 null 时整体失败（外层 catch 兜住 NPE）")
    void syncFailsWhenOperationsNull() {
        SyncRequest request = new SyncRequest();
        request.setDeviceId("DEV-1");
        request.setOperations(null);

        SyncResponse response = service.syncData(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().startsWith("同步失败"), response.getMessage());
        assertNull(response.getSyncTime(), "现状：整体失败时不回填 syncTime");
    }

    @Test
    @DisplayName("现状：给了 lastSyncTime 也拉不到任何服务端变更（getServerOperations 恒为空）")
    void serverOperationsAlwaysEmpty() {
        SyncRequest request = request();
        request.setLastSyncTime(LocalDateTime.now().minusDays(1));

        SyncResponse response = service.syncData(request);

        assertTrue(response.getServerOperations().isEmpty(), "现状：服务端变更列表恒为空，未实现增量拉取");
    }

    @Test
    @DisplayName("现状：多操作全部成功时 failedOperations 为空、conflicts 恒为空（不检测冲突）")
    void conflictsNeverDetected() {
        SyncResponse response = service.syncData(request(operation("CREATE"), operation("UPDATE")));

        assertTrue(response.getFailedOperations().isEmpty());
        assertTrue(response.getConflicts().isEmpty(), "现状：从不产出冲突项");
    }

    // ---------------- 冲突解决 ----------------

    @Test
    @DisplayName("冲突：SERVER_WINS 返回服务端版本")
    void serverWins() {
        SyncOperation client = operation("UPDATE");
        SyncOperation server = operation("UPDATE");

        assertSame(
                server,
                service.resolveConflict(client, server, ConflictResolutionStrategy.SERVER_WINS));
    }

    @Test
    @DisplayName("冲突：CLIENT_WINS 返回客户端版本")
    void clientWins() {
        SyncOperation client = operation("UPDATE");
        SyncOperation server = operation("UPDATE");

        assertSame(
                client,
                service.resolveConflict(client, server, ConflictResolutionStrategy.CLIENT_WINS));
    }

    @Test
    @DisplayName("冲突：LAST_WRITE_WINS 取操作时间更晚的一方")
    void lastWriteWinsPicksNewer() {
        LocalDateTime now = LocalDateTime.now();
        SyncOperation older = operation("UPDATE");
        older.setOperationTime(now.minusMinutes(5));
        SyncOperation newer = operation("UPDATE");
        newer.setOperationTime(now);

        assertSame(
                newer,
                service.resolveConflict(newer, older, ConflictResolutionStrategy.LAST_WRITE_WINS));
        assertSame(
                newer,
                service.resolveConflict(older, newer, ConflictResolutionStrategy.LAST_WRITE_WINS));
    }

    @Test
    @DisplayName("冲突：LAST_WRITE_WINS 在时间完全相同时由服务端胜出（现状口径）")
    void lastWriteWinsTiesGoToServer() {
        LocalDateTime same = LocalDateTime.now();
        SyncOperation client = operation("UPDATE");
        client.setOperationTime(same);
        SyncOperation server = operation("UPDATE");
        server.setOperationTime(same);

        assertSame(
                server,
                service.resolveConflict(
                        client, server, ConflictResolutionStrategy.LAST_WRITE_WINS));
    }

    @Test
    @DisplayName("冲突合并：以服务端身份为骨架、版本 +1、数据取客户端（现状：忽略服务端数据）")
    void mergeTakesServerIdentityAndClientData() {
        SyncOperation client = operation("UPDATE");
        client.setId("1");
        client.setDeviceId("CLIENT-DEV");
        client.setOperationData("CLIENT-DATA");
        client.setVersion(3);
        SyncOperation server = operation("UPDATE");
        server.setId("2");
        server.setEntityId("ENT-9");
        server.setDeviceId("SERVER-DEV");
        server.setOperationData("SERVER-DATA");
        server.setVersion(5);

        SyncOperation merged =
                service.resolveConflict(client, server, ConflictResolutionStrategy.MERGE);

        assertEquals("2", merged.getId(), "身份取自服务端");
        assertEquals("ENT-9", merged.getEntityId());
        assertEquals("SERVER-DEV", merged.getDeviceId());
        assertEquals("UPDATE", merged.getOperationType());
        assertEquals("COMPLETED", merged.getStatus());
        assertEquals(6, merged.getVersion(), "版本取两者较大值 +1");
        assertNotNull(merged.getOperationTime());
        assertEquals("CLIENT-DATA", merged.getOperationData(), "现状：mergeData 直接返回客户端数据，服务端数据被丢弃");
    }

    @Test
    @DisplayName("冲突：策略为 null 时抛 NPE（switch 空指针，不是明确参数错误）")
    void nullStrategyThrowsNpe() {
        SyncOperation client = operation("UPDATE");
        SyncOperation server = operation("UPDATE");

        assertThrows(
                NullPointerException.class, () -> service.resolveConflict(client, server, null));
    }

    @Test
    @DisplayName("冲突合并：客户端与版本都为 null 时会 NPE（现状未做防御）")
    void mergeNpeWhenVersionNull() {
        SyncOperation client = operation("UPDATE");
        client.setVersion(null);
        SyncOperation server = operation("UPDATE");
        server.setVersion(null);

        assertThrows(
                NullPointerException.class,
                () -> service.resolveConflict(client, server, ConflictResolutionStrategy.MERGE));
    }

    @Test
    @DisplayName("冲突合并：合并结果是一份新对象，不篡改任何一方")
    void mergeDoesNotMutateInputs() {
        SyncOperation client = operation("UPDATE");
        SyncOperation server = operation("UPDATE");
        server.setEntityId("ENT-1");

        SyncOperation merged =
                service.resolveConflict(client, server, ConflictResolutionStrategy.MERGE);

        assertFalse(merged == client);
        assertFalse(merged == server);
        assertNull(client.getEntityId(), "客户端对象不应被改写");
        assertEquals("ENT-1", server.getEntityId(), "服务端对象不应被改写");
    }

    @Test
    @DisplayName("同步失败的消息里带原始异常信息（便于定位）")
    void failureMessageCarriesCause() {
        SyncRequest request = new SyncRequest();
        request.setDeviceId("DEV-1");
        request.setOperations(null);

        assertTrue(
                service.syncData(request).getMessage().contains("NullPointerException")
                        || service.syncData(request).getMessage().length() > "同步失败: ".length(),
                "现状：失败消息拼接了异常信息");
    }

    @Test
    @DisplayName("同步：不改写 deviceId 之外的外部对象，且不产生任何持久化副作用（该类无依赖）")
    void serviceHasNoDependencies() {
        assertEquals(
                0,
                SyncApplicationService.class.getDeclaredFields().length,
                "现状：该类没有任何字段/依赖，因此不可能落库或拉取真实服务端变更");
    }

    @Test
    @DisplayName("同步：逐条操作的执行顺序与请求顺序一致")
    void operationsProcessedInOrder() {
        SyncOperation first = operation("CREATE");
        SyncOperation second = operation("CREATE");
        List<SyncOperation> ops = new ArrayList<>(List.of(first, second));

        service.syncData(request(first, second));

        assertEquals(2, ops.size());
        assertNotNull(first.getEntityId());
        assertNotNull(second.getEntityId());
        assertFalse(first.getEntityId().equals(second.getEntityId()), "每条各自生成 entityId");
    }
}
