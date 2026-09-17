package com.huicang.wise.api.controller;

import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.oss.FileUploadRequest;
import com.huicang.wise.application.oss.FileUploadResponse;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.oss.MinioFile;
import com.huicang.wise.domain.repository.oss.MinioFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类功能描述：文件存储控制器
 *
 * @author WiseDepot
 * @version 0.1.19
 * @since 2026-02-27
 */
@RestController
@RequestMapping("/api/files")
public class FileStorageController {

    private final FileStorageApplicationService fileStorageService;
    private final MinioFileRepository minioFileRepository;

    @Autowired
    public FileStorageController(FileStorageApplicationService fileStorageService,
                                 MinioFileRepository minioFileRepository) {
        this.fileStorageService = fileStorageService;
        this.minioFileRepository = minioFileRepository;
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param bucketName 存储桶名称
     * @param uploadBy 上传者ID
     * @return 文件上传响应
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long uploadBy) {

        try {
            FileUploadRequest request = new FileUploadRequest();
            request.setFile(file);
            request.setBucketName(bucketName);

            FileUploadResponse response = fileStorageService.uploadFile(request, uploadBy);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        try {
            byte[] fileData = fileStorageService.downloadFile(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "file_" + fileId);
            headers.setContentLength(fileData.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileData);
        } catch (BusinessException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成临时访问链接
     *
     * @param fileId 文件ID
     * @param expiresIn 过期时间（秒）
     * @return 临时访问链接
     */
    @GetMapping("/{fileId}/presigned-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> generatePresignedUrl(
            @PathVariable Long fileId,
            @RequestParam(value = "expiresIn", defaultValue = "3600") Integer expiresIn) {

        try {
            String url = fileStorageService.generatePresignedUrl(fileId, expiresIn);
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("expiresIn", expiresIn.toString());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, "生成访问链接失败: " + e.getMessage()));
        }
    }

    /**
     * 查询文件列表
     *
     * @param uploadBy 上传者ID
     * @return 文件列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MinioFile>>> listFiles(
            @RequestParam(value = "uploadBy", required = false) Long uploadBy) {

        try {
            List<MinioFile> files = fileStorageService.listFiles(uploadBy);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, "查询文件列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<MinioFile>> getFileDetail(@PathVariable Long fileId) {
        try {
            MinioFile file = minioFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));
            return ResponseEntity.ok(ApiResponse.success(file));
        } catch (BusinessException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId) {
        try {
            fileStorageService.deleteFile(fileId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, "文件删除失败: " + e.getMessage()));
        }
    }
}