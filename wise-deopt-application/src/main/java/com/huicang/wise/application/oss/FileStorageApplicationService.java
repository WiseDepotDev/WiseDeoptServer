package com.huicang.wise.application.oss;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.oss.MinioFile;
import com.huicang.wise.domain.repository.oss.MinioFileRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类功能描述：文件存储应用服务
 *
 * @author WiseDepot
 * @version 0.1.19
 * @since 2026-02-27
 */
@Service
public class FileStorageApplicationService {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    private static final List<String> ALLOWED_FILE_TYPES = List.of(
        "jpg", "jpeg", "png", "gif", "bmp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "zip", "rar", "7z"
    );

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif", "image/bmp", "image/*",
        "application/pdf",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain", "text/csv",
        "application/zip", "application/x-rar-compressed", "application/x-7z-compressed"
    );

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${spring.minio.bucket-name:wise-depot}")
    private String defaultBucket;

    @Value("${spring.minio.endpoint:http://10.0.0.4:9000}")
    private String minioEndpoint;

    @Value("${spring.minio.access-key:}")
    private String minioAccessKey;

    @Value("${spring.minio.secret-key:}")
    private String minioSecretKey;

    private final MinioFileRepository minioFileRepository;

    public FileStorageApplicationService(MinioFileRepository minioFileRepository) {
        this.minioFileRepository = minioFileRepository;
    }

    /**
     * 上传文件
     *
     * @param request 文件上传请求
     * @param uploadBy 上传者ID
     * @return 文件上传响应
     * @throws BusinessException 当上传失败时抛出异常
     */
    @Transactional
    public FileUploadResponse uploadFile(FileUploadRequest request, Long uploadBy) throws BusinessException {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        validateFile(file);

        String bucketName = request.getBucketName() != null ? request.getBucketName() : defaultBucket;
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String objectKey = generateObjectKey(originalFilename);
        String contentType = file.getContentType();

        try {
            ensureBucketExists(bucketName);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );

            MinioFile minioFile = new MinioFile();
            minioFile.setBucketName(bucketName);
            minioFile.setFilePath(objectKey);
            minioFile.setFileSize(file.getSize());
            minioFile.setUploadTime(LocalDateTime.now());
            minioFile.setUploadBy(uploadBy);

            MinioFile savedFile = minioFileRepository.save(minioFile);

            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(savedFile.getFileId());
            response.setFilePath(savedFile.getFilePath());
            response.setFileSize(savedFile.getFileSize());
            response.setUploadTime(savedFile.getUploadTime());
            response.setAccessUrl(generateAccessUrl(bucketName, objectKey));

            return response;

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件字节数组
     * @throws BusinessException 当下载失败时抛出异常
     */
    public byte[] downloadFile(Long fileId) throws BusinessException {
        MinioFile minioFile = minioFileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));

        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioFile.getBucketName())
                    .object(minioFile.getFilePath())
                    .build()
            ).readAllBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 生成临时访问链接
     *
     * @param fileId 文件ID
     * @param expiresIn 过期时间（秒）
     * @return 临时访问链接
     * @throws BusinessException 当生成链接失败时抛出异常
     */
    public String generatePresignedUrl(Long fileId, Integer expiresIn) throws BusinessException {
        MinioFile minioFile = minioFileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioFile.getBucketName())
                    .object(minioFile.getFilePath())
                    .expiry(expiresIn != null && expiresIn > 0 ? expiresIn : 3600)
                    .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成访问链接失败: " + e.getMessage());
        }
    }

    /**
     * 查询文件列表
     *
     * @param uploadBy 上传者ID
     * @return 文件列表
     */
    public List<MinioFileDTO> listFiles(Long uploadBy) {
        List<MinioFile> files = (uploadBy != null)
                ? minioFileRepository.findByUploadBy(uploadBy)
                : minioFileRepository.findAll();
        return files.stream().map(FileStorageApplicationService::toDto).collect(Collectors.toList());
    }

    /**
     * 查询文件详情（对外返回 DTO，避免领域实体出现在接口签名）。
     *
     * @param fileId 文件ID
     * @return 文件详情 DTO
     * @throws BusinessException 文件不存在时抛出
     */
    public MinioFileDTO getFileDetail(Long fileId) throws BusinessException {
        return toDto(findFileOrThrow(fileId));
    }

    /**
     * 领域实体转 DTO。
     *
     * @param file 文件实体
     * @return 传输对象
     */
    private static MinioFileDTO toDto(MinioFile file) {
        MinioFileDTO dto = new MinioFileDTO();
        dto.setFileId(file.getFileId());
        dto.setBucketName(file.getBucketName());
        dto.setFilePath(file.getFilePath());
        dto.setFileSize(file.getFileSize());
        dto.setUploadTime(file.getUploadTime());
        dto.setUploadBy(file.getUploadBy());
        return dto;
    }

    /**
     * 按主键查询文件，不存在时抛出业务异常。
     *
     * @param fileId 文件ID
     * @return 文件实体
     * @throws BusinessException 文件不存在时抛出
     */
    private MinioFile findFileOrThrow(Long fileId) throws BusinessException {
        return minioFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在: " + fileId));
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @throws BusinessException 当删除失败时抛出异常
     */
    @Transactional
    public void deleteFile(Long fileId) throws BusinessException {
        MinioFile minioFile = minioFileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));

        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioFile.getBucketName())
                    .object(minioFile.getFilePath())
                    .build()
            );

            minioFileRepository.deleteById(fileId);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 校验文件
     *
     * @param file 上传的文件
     * @throws BusinessException 当文件校验失败时抛出异常
     */
    private void validateFile(MultipartFile file) throws BusinessException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小超过限制（最大100MB）");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_FILE_TYPES.contains(fileExtension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + fileExtension);
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !ALLOWED_CONTENT_TYPES.contains(contentType))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件内容类型: " + contentType);
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * 生成对象键
     *
     * @param originalFilename 原始文件名
     * @return 对象键
     */
    private String generateObjectKey(String originalFilename) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileExtension = getFileExtension(originalFilename);
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%02d/%02d/%s.%s",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), uuid, fileExtension);
    }

    /**
     * 确保存储桶存在
     *
     * @param bucketName 存储桶名称
     * @throws Exception 当创建存储桶失败时抛出异常
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );

        if (!found) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        }
    }

    /**
     * 生成访问URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键
     * @return 访问URL
     */
    private String generateAccessUrl(String bucketName, String objectKey) {
        return String.format("%s/%s/%s", minioEndpoint, bucketName, objectKey);
    }
}