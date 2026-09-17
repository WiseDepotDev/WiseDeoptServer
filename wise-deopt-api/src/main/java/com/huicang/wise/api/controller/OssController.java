package com.huicang.wise.api.controller;

import com.huicang.wise.application.oss.MinioFileCreateRequest;
import com.huicang.wise.application.oss.MinioFileDTO;
import com.huicang.wise.application.oss.OssApplicationService;
import com.huicang.wise.application.oss.PresignedUrlResponse;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：对象存储控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现MinIO文件记录与预签名接口
 */
@Tag(name = "对象存储接口")
@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssApplicationService ossApplicationService;

    public OssController(OssApplicationService ossApplicationService) {
        this.ossApplicationService = ossApplicationService;
    }

    /**
     * 方法功能描述：创建文件记录
     *
     * @param request 文件记录创建请求
     * @return 文件记录信息
     */
    @Operation(summary = "创建文件记录", description = "创建MinIO文件记录。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.OSS_FILE_CREATE)
    @PostMapping("/files")
    public ApiResponse<MinioFileDTO> createFileRecord(
            @Parameter(description = "文件记录创建请求", required = true)
            @RequestBody MinioFileCreateRequest request) {
        return ApiResponse.success(ossApplicationService.createFileRecord(request));
    }

    /**
     * 方法功能描述：生成预签名下载链接
     *
     * @param bucket    Bucket名称
     * @param objectKey 对象Key
     * @param expiresIn 过期秒数
     * @return 预签名链接
     */
    @Operation(summary = "生成预签名下载链接", description = "生成预签名下载链接。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.OSS_PRESIGNED_URL)
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
            @Parameter(description = "Bucket名称", required = true)
            @RequestParam("bucket") String bucket,
            @Parameter(description = "对象Key", required = true)
            @RequestParam("objectKey") String objectKey,
            @Parameter(description = "过期秒数", required = true)
            @RequestParam("expiresIn") Integer expiresIn) {
        return ApiResponse.success(ossApplicationService.generatePresignedUrl(bucket, objectKey, expiresIn));
    }
}


