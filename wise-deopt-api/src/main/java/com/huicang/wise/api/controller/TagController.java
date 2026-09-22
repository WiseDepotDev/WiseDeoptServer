package com.huicang.wise.api.controller;

import com.huicang.wise.application.tag.BatchBindResult;
import com.huicang.wise.application.tag.BatchUnbindResult;
import com.huicang.wise.application.tag.ProductTagBatchBindRequest;
import com.huicang.wise.application.tag.ProductTagBatchBindRequestWithCaptcha;
import com.huicang.wise.application.tag.ProductTagCreateRequest;
import com.huicang.wise.application.tag.ProductTagDTO;
import com.huicang.wise.application.tag.ProductTagPageDTO;
import com.huicang.wise.application.tag.ProductTagUpdateRequest;
import com.huicang.wise.application.tag.TagApplicationService;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：标签服务控制器
 *
 * @author xingchentye
 * @date 2026-02-27
 * @modified xingchentye 2026-02-27 实现版本0.1.12功能：标签CRUD、绑定/解绑、批量操作
 */
@Tag(name = "标签服务接口")
@RestController
@RequestMapping("/api/tag")
public class TagController {

    private final TagApplicationService tagApplicationService;

    public TagController(TagApplicationService tagApplicationService) {
        this.tagApplicationService = tagApplicationService;
    }

    @Operation(summary = "创建标签", description = "创建产品标签。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_CREATE)
    @PostMapping
    public ApiResponse<ProductTagDTO> createTag(
            @Parameter(description = "标签创建请求", required = true) @Valid @RequestBody
                    ProductTagCreateRequest request) {
        return ApiResponse.success(tagApplicationService.createTag(request));
    }

    @Operation(summary = "更新标签", description = "更新产品标签信息。成功返回200；标签不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_UPDATE)
    @PutMapping("/{tagId}")
    public ApiResponse<ProductTagDTO> updateTag(
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId,
            @Parameter(description = "标签更新请求", required = true) @Valid @RequestBody
                    ProductTagUpdateRequest request) {
        return ApiResponse.success(tagApplicationService.updateTag(tagId, request));
    }

    @Operation(summary = "删除标签", description = "删除产品标签。成功返回200；标签不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_DELETE)
    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> deleteTag(
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId) {
        tagApplicationService.deleteTag(tagId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取标签详情", description = "根据标签ID获取标签详情。成功返回200；标签不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_DETAIL)
    @GetMapping("/{tagId}")
    public ApiResponse<ProductTagDTO> getTag(
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId) {
        return ApiResponse.success(tagApplicationService.getTag(tagId));
    }

    @Operation(
            summary = "根据标签编码查询",
            description = "根据标签编码（RFID/条码）查询标签详情。成功返回200；标签不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_DETAIL)
    @GetMapping("/code/{tagCode}")
    public ApiResponse<ProductTagDTO> getTagByCode(
            @Parameter(description = "标签编码", required = true) @PathVariable("tagCode")
                    String tagCode) {
        return ApiResponse.success(tagApplicationService.getTagByCode(tagCode));
    }

    @Operation(summary = "查询标签列表", description = "查询标签列表，支持按产品ID、状态筛选、搜索和分页查询。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_LIST)
    @GetMapping
    public ApiResponse<ProductTagPageDTO> listTags(
            @Parameter(description = "产品ID（可选）", required = false)
                    @RequestParam(value = "productId", required = false)
                    Long productId,
            @Parameter(description = "标签状态（可选）", required = false)
                    @RequestParam(value = "status", required = false)
                    String status,
            @Parameter(description = "搜索关键词（可选，支持RFID、条码、NFC UID）", required = false)
                    @RequestParam(value = "search", required = false)
                    String search,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(
                tagApplicationService.listTags(productId, status, search, page, pageSize));
    }

    @Operation(summary = "按产品查询标签", description = "根据产品ID查询该产品的所有标签，支持分页。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_LIST)
    @GetMapping("/product/{productId}")
    public ApiResponse<ProductTagPageDTO> listTagsByProduct(
            @Parameter(description = "产品ID", required = true) @PathVariable("productId")
                    Long productId,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(
                tagApplicationService.listTagsByProduct(productId, page, pageSize));
    }

    @Operation(summary = "绑定标签", description = "将标签绑定到指定产品。成功返回200；标签或产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_BIND)
    @PostMapping("/{tagId}/bind")
    public ApiResponse<ProductTagDTO> bindTag(
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId,
            @Parameter(description = "产品ID", required = true) @RequestParam("productId")
                    Long productId) {
        return ApiResponse.success(tagApplicationService.bindTag(tagId, productId));
    }

    @Operation(summary = "解绑标签", description = "解绑标签与产品的关联。成功返回200；标签不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_UNBIND)
    @PostMapping("/{tagId}/unbind")
    public ApiResponse<ProductTagDTO> unbindTag(
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId) {
        return ApiResponse.success(tagApplicationService.unbindTag(tagId));
    }

    @Operation(summary = "批量绑定标签", description = "批量将标签绑定到指定产品。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_BATCH_BIND)
    @PostMapping("/batch-bind")
    public ApiResponse<BatchBindResult> batchBindTags(
            @Parameter(description = "批量绑定请求", required = true) @Valid @RequestBody
                    ProductTagBatchBindRequest request) {
        return ApiResponse.success(tagApplicationService.batchBindTags(request));
    }

    @Operation(
            summary = "批量绑定标签（带验证码）",
            description = "批量将标签绑定到指定产品，需要验证码验证。成功返回200；参数错误返回400；验证码错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_BATCH_BIND)
    @PostMapping("/batch-bind-with-captcha")
    public ApiResponse<BatchBindResult> batchBindTagsWithCaptcha(
            @Parameter(description = "批量绑定请求（带验证码）", required = true) @Valid @RequestBody
                    ProductTagBatchBindRequestWithCaptcha request) {
        return ApiResponse.success(tagApplicationService.batchBindTagsWithCaptcha(request));
    }

    @Operation(summary = "批量解绑标签", description = "批量解绑标签与产品的关联。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_BATCH_UNBIND)
    @PostMapping("/batch-unbind")
    public ApiResponse<BatchUnbindResult> batchUnbindTags(
            @Parameter(description = "标签ID列表", required = true) @Valid @RequestBody
                    List<Long> tagIds) {
        return ApiResponse.success(tagApplicationService.batchUnbindTags(tagIds));
    }

    @Operation(summary = "搜索标签", description = "根据标签编码关键字搜索标签，支持模糊查询和分页。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_LIST)
    @GetMapping("/search")
    public ApiResponse<ProductTagPageDTO> searchTags(
            @Parameter(description = "搜索关键字", required = true) @RequestParam("keyword")
                    String keyword,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(tagApplicationService.searchTags(keyword, page, pageSize));
    }

    @Operation(summary = "批量查询标签", description = "根据标签编码列表批量查询标签信息。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.TAG_LIST)
    @PostMapping("/batch-query")
    public ApiResponse<List<ProductTagDTO>> batchGetTags(
            @Parameter(description = "标签编码列表", required = true) @Valid @RequestBody
                    List<String> tagCodes) {
        return ApiResponse.success(tagApplicationService.batchGetTags(tagCodes));
    }
}
