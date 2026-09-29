package com.huicang.wise.application.oss;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.oss.MinioFile;
import com.huicang.wise.infrastructure.persistence.repository.oss.MinioFileRepository;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储应用服务的单元测试：上传校验与失败补偿、下载/临时链接、列表/详情/删除。
 *
 * <p>两个测试要点： ① {@code minioClient} 与三个 {@code @Value} 字段是**字段注入**（无 setter），只能用 {@code
 * ReflectionTestUtils} 塞进去； ② 上传失败补偿（{@code removeObjectQuietly}）必须验证"**只有对象已落盘时才回收**"，
 * 且回收自身失败**不得掩盖**原始失败原因 —— 这两条是 STD-DATA-01 事务边界的核心语义。
 */
@ExtendWith(MockitoExtension.class)
class FileStorageApplicationServiceTest {

    private static final long FILE_ID = 77L;
    private static final String ENDPOINT = "http://minio.local:9000";
    private static final String DEFAULT_BUCKET = "wise-depot";

    @Mock private MinioFileRepository minioFileRepository;
    @Mock private MinioClient minioClient;
    @Mock private GetObjectResponse objectResponse;

    private FileStorageApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageApplicationService(minioFileRepository);
        ReflectionTestUtils.setField(service, "minioClient", minioClient);
        ReflectionTestUtils.setField(service, "defaultBucket", DEFAULT_BUCKET);
        ReflectionTestUtils.setField(service, "minioEndpoint", ENDPOINT);
    }

    /** 通用夹具：所有桩都用 lenient —— 校验失败的用例走不到后面的取值。 */
    private MultipartFile uploaded(String name, String contentType, long size) throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getOriginalFilename()).thenReturn(name);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        lenient()
                .when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        return file;
    }

    private FileUploadRequest request(MultipartFile file) {
        FileUploadRequest request = new FileUploadRequest();
        request.setFile(file);
        return request;
    }

    private MinioFile minioFile() {
        MinioFile entity = new MinioFile();
        entity.setFileId(FILE_ID);
        entity.setBucketName(DEFAULT_BUCKET);
        entity.setFilePath("2026/02/27/abc.png");
        entity.setFileSize(1024L);
        entity.setUploadTime(LocalDateTime.now());
        entity.setUploadBy(5L);
        return entity;
    }

    private void stubUploadSuccess() {
        lenient()
                .when(minioFileRepository.save(any(MinioFile.class)))
                .thenAnswer(
                        invocation -> {
                            MinioFile saved = invocation.getArgument(0);
                            saved.setFileId(FILE_ID);
                            return saved;
                        });
    }

    private PutObjectArgs capturedPut() throws Exception {
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        return captor.getValue();
    }

    // ---------------- 上传：校验 ----------------

    @Test
    @DisplayName("上传：文件为 null 被拒")
    void uploadRejectsNullFile() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(null), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：空文件被拒")
    void uploadRejectsEmptyFile() {
        MultipartFile empty = Mockito.mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(empty), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：超过 100MB 被拒")
    void uploadRejectsOversize() throws Exception {
        MultipartFile file = uploaded("big.zip", "application/zip", 100L * 1024 * 1024 + 1);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：文件名为空被拒")
    void uploadRejectsMissingFilename() throws Exception {
        MultipartFile file = uploaded("", "image/png", 10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：扩展名不在白名单被拒")
    void uploadRejectsDisallowedExtension() throws Exception {
        MultipartFile file = uploaded("evil.exe", "application/octet-stream", 10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：无扩展名的文件名被拒（扩展名为空串）")
    void uploadRejectsFilenameWithoutExtension() throws Exception {
        MultipartFile file = uploaded("README", "text/plain", 10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：内容类型为 null 被拒")
    void uploadRejectsNullContentType() throws Exception {
        MultipartFile file = uploaded("a.png", null, 10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("上传：白名单外的非图片内容类型被拒")
    void uploadRejectsDisallowedContentType() throws Exception {
        MultipartFile file = uploaded("a.png", "application/octet-stream", 10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.uploadFile(request(file), 1L));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    // ---------------- 上传：成功路径 ----------------

    @Test
    @DisplayName("上传：成功写对象并落库，返回访问URL；对象键按 年/月/日/uuid.扩展名")
    void uploadSuccess() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        stubUploadSuccess();
        MultipartFile file = uploaded("报告.PNG", "image/png", 2048);

        FileUploadResponse response = service.uploadFile(request(file), 5L);

        PutObjectArgs put = capturedPut();
        assertEquals(DEFAULT_BUCKET, put.bucket());
        assertTrue(
                put.object().matches("\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.PNG"),
                "对象键形状不符: " + put.object());
        assertEquals(ENDPOINT + "/" + DEFAULT_BUCKET + "/" + put.object(), response.getAccessUrl());
        assertEquals(FILE_ID, response.getFileId().longValue());
        assertEquals(2048L, response.getFileSize().longValue());
        assertNotNull(response.getUploadTime());
    }

    @Test
    @DisplayName("上传：请求指定桶时覆盖默认桶")
    void uploadUsesRequestedBucket() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        stubUploadSuccess();
        FileUploadRequest request = request(uploaded("a.png", "image/png", 10));
        request.setBucketName("custom-bucket");

        service.uploadFile(request, 5L);

        assertEquals("custom-bucket", capturedPut().bucket());
    }

    @Test
    @DisplayName("上传：桶不存在时自动创建")
    void uploadCreatesMissingBucket() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(false);
        stubUploadSuccess();

        service.uploadFile(request(uploaded("a.png", "image/png", 10)), 5L);

        verify(minioClient).makeBucket(any());
    }

    @Test
    @DisplayName("上传：图片子类型走 image/* 兜底（不必在白名单里）")
    void uploadAcceptsAnyImageSubtype() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        stubUploadSuccess();

        assertNotNull(service.uploadFile(request(uploaded("a.png", "image/webp", 10)), 5L));
    }

    @Test
    @DisplayName("上传：扩展名大小写不敏感（.PNG 可通过白名单）")
    void uploadAcceptsUppercaseExtension() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        stubUploadSuccess();

        assertNotNull(service.uploadFile(request(uploaded("a.PNG", "image/png", 10)), 5L));
    }

    // ---------------- 上传：失败补偿 ----------------

    @Test
    @DisplayName("上传：写对象失败时抛 SYSTEM_ERROR，且不做回收、不落库")
    void uploadFailsWhenPutObjectFails() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.uploadFile(request(uploaded("a.png", "image/png", 10)), 5L));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        verify(minioFileRepository, never()).save(any(MinioFile.class));
    }

    @Test
    @DisplayName("STD-DATA-01：对象已落盘但落库失败时，尽力回收对象并抛 SYSTEM_ERROR")
    void uploadRemovesObjectWhenRecordSaveFails() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioFileRepository.save(any(MinioFile.class)))
                .thenThrow(new RuntimeException("db down"));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.uploadFile(request(uploaded("a.png", "image/png", 10)), 5L));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals(DEFAULT_BUCKET, captor.getValue().bucket());
        assertEquals(capturedPut().object(), captor.getValue().object());
    }

    @Test
    @DisplayName("STD-DATA-01：回收自身失败不得掩盖原始失败原因")
    void uploadSwallowsCleanupFailure() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioFileRepository.save(any(MinioFile.class)))
                .thenThrow(new RuntimeException("db down"));
        doThrow(new RuntimeException("cleanup also down"))
                .when(minioClient)
                .removeObject(any(RemoveObjectArgs.class));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.uploadFile(request(uploaded("a.png", "image/png", 10)), 5L));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("db down"), "原始失败原因应出现在异常信息里，实际=" + ex.getMessage());
    }

    // ---------------- 下载 / 临时链接 ----------------

    @Test
    @DisplayName("下载：文件不存在抛 NOT_FOUND")
    void downloadMissing() {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.downloadFile(FILE_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("下载：成功返回对象字节")
    void downloadSuccess() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));
        when(minioClient.getObject(any())).thenReturn(objectResponse);
        when(objectResponse.readAllBytes()).thenReturn(new byte[] {9, 8, 7});

        assertArrayEquals(new byte[] {9, 8, 7}, service.downloadFile(FILE_ID));
    }

    @Test
    @DisplayName("下载：对象存储异常转 SYSTEM_ERROR")
    void downloadFailure() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));
        when(minioClient.getObject(any())).thenThrow(new RuntimeException("minio down"));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.downloadFile(FILE_ID));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("临时链接：文件不存在抛 NOT_FOUND")
    void presignedMissing() {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.generatePresignedUrl(FILE_ID, 60));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("临时链接：过期时间为空或非正时回落默认值（不报错）")
    void presignedFallsBackToDefaultExpiry() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://signed");

        assertEquals("http://signed", service.generatePresignedUrl(FILE_ID, null));
        assertEquals("http://signed", service.generatePresignedUrl(FILE_ID, 0));
        assertEquals("http://signed", service.generatePresignedUrl(FILE_ID, 120));
    }

    @Test
    @DisplayName("临时链接：对象存储异常转 SYSTEM_ERROR")
    void presignedFailure() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));
        when(minioClient.getPresignedObjectUrl(any()))
                .thenThrow(new RuntimeException("minio down"));

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.generatePresignedUrl(FILE_ID, 60));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    }

    // ---------------- 列表 / 详情 / 删除 ----------------

    @Test
    @DisplayName("文件列表：指定上传者时按上传者查询")
    void listFilesByUploader() {
        when(minioFileRepository.findByUploadBy(5L)).thenReturn(List.of(minioFile()));

        List<MinioFileDTO> rows = service.listFiles(5L);

        assertEquals(1, rows.size());
        assertEquals(FILE_ID, rows.get(0).getFileId().longValue());
        verify(minioFileRepository, never()).findAll();
    }

    @Test
    @DisplayName("文件列表：上传者为空时查全量")
    void listFilesWithoutUploader() {
        when(minioFileRepository.findAll()).thenReturn(List.of(minioFile()));

        assertEquals(1, service.listFiles(null).size());
    }

    @Test
    @DisplayName("文件详情：命中返回 DTO（不暴露领域实体）")
    void getFileDetailFound() {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));

        MinioFileDTO dto = service.getFileDetail(FILE_ID);

        assertEquals(FILE_ID, dto.getFileId().longValue());
        assertEquals(DEFAULT_BUCKET, dto.getBucketName());
        assertEquals(5L, dto.getUploadBy().longValue());
    }

    @Test
    @DisplayName("文件详情：不存在抛 NOT_FOUND")
    void getFileDetailMissing() {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getFileDetail(FILE_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("删除：文件不存在抛 NOT_FOUND 且不删记录")
    void deleteMissing() {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteFile(FILE_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(minioFileRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除：先删对象再删记录")
    void deleteSuccess() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));

        service.deleteFile(FILE_ID);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        verify(minioFileRepository).deleteById(FILE_ID);
    }

    @Test
    @DisplayName("STD-DATA-01：对象删除失败时抛 SYSTEM_ERROR，且不得先删记录")
    void deleteKeepsRecordWhenObjectRemovalFails() throws Exception {
        when(minioFileRepository.findById(FILE_ID)).thenReturn(Optional.of(minioFile()));
        doThrow(new RuntimeException("minio down"))
                .when(minioClient)
                .removeObject(any(RemoveObjectArgs.class));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteFile(FILE_ID));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        verify(minioFileRepository, never()).deleteById(any());
    }
}
