package com.huicang.wise.application.oss;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.oss.MinioFileJpaEntity;
import com.huicang.wise.infrastructure.repository.oss.MinioFileRepository;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;

/**
 * 类功能描述：对象存储应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现文件记录与预签名链接
 */
@Service
public class OssApplicationService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MinioClient minioClient;
    
    private final MinioFileRepository minioFileRepository;

    public OssApplicationService(MinioFileRepository minioFileRepository) {
        this.minioFileRepository = minioFileRepository;
    }

    /**
     * 方法功能描述：创建MinIO文件记录
     *
     * @param request 文件记录创建请求
     * @return 文件记录信息
     * @throws BusinessException 当必填字段为空时抛出异常
     */
    @Transactional
    public MinioFileDTO createFileRecord(MinioFileCreateRequest request) throws BusinessException {
        if (request.getBucket() == null || request.getBucket().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Bucket不能为空");
        }
        if (request.getObjectKey() == null || request.getObjectKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "对象Key不能为空");
        }
        MinioFileJpaEntity entity = new MinioFileJpaEntity();
        entity.setFileId(System.currentTimeMillis());
        entity.setBucket(request.getBucket());
        entity.setObjectKey(request.getObjectKey());
        entity.setSize(request.getSize());
        entity.setContentType(request.getContentType());
        entity.setCreatedAt(LocalDateTime.now());
        MinioFileJpaEntity saved = minioFileRepository.save(entity);
        return toMinioFileDTO(saved);
    }

    /**
     * 方法功能描述：生成预签名下载链接
     *
     * @param bucket   Bucket名称
     * @param objectKey 对象Key
     * @param expiresIn 过期秒数
     * @return 预签名链接响应
     * @throws BusinessException 当生成链接失败时抛出异常
     */
    public PresignedUrlResponse generatePresignedUrl(String bucket, String objectKey, Integer expiresIn)
            throws BusinessException {
        if (bucket == null || bucket.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Bucket不能为空");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "对象Key不能为空");
        }
        if (expiresIn == null || expiresIn <= 0 || expiresIn > 604800) {
            throw new BusinessException(ErrorCode.OSS_EXPIRES_OUT_OF_RANGE, ErrorCode.OSS_EXPIRES_OUT_OF_RANGE.getMessage());
        }
        if (minioClient == null) {
            throw new BusinessException(ErrorCode.OSS_SERVICE_UNAVAILABLE, "MinIO服务未启用");
        }
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expiresIn)
                            .build());
            PresignedUrlResponse response = new PresignedUrlResponse();
            response.setUrl(url);
            response.setExpiresIn(expiresIn);
            return response;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.OSS_PRESIGN_ERROR, ErrorCode.OSS_PRESIGN_ERROR.getMessage());
        }
    }

    private MinioFileDTO toMinioFileDTO(MinioFileJpaEntity entity) {
        MinioFileDTO dto = new MinioFileDTO();
        dto.setFileId(entity.getFileId());
        dto.setBucket(entity.getBucket());
        dto.setObjectKey(entity.getObjectKey());
        dto.setSize(entity.getSize());
        dto.setContentType(entity.getContentType());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
