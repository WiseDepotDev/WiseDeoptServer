package com.huicang.wise.api.controller;

import com.huicang.wise.application.tag.ProductTagCreateRequest;
import com.huicang.wise.application.tag.ProductTagDTO;
import com.huicang.wise.application.tag.ProductTagPageDTO;
import com.huicang.wise.application.tag.ProductTagUpdateRequest;
import com.huicang.wise.application.tag.TagApplicationService;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "标签服务接口")
@RestController
@RequestMapping({"/api/tag", "/tag"})
public class TagController {

    private final TagApplicationService tagApplicationService;

    public TagController(TagApplicationService tagApplicationService) {
        this.tagApplicationService = tagApplicationService;
    }

    @ApiOperation(
            value = "创建产品标签",
            notes = "创建产品标签。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @PostMapping
    public ApiResponse<ProductTagDTO> createTag(
            @ApiParam(value = "标签创建请求", required = true)
            @RequestBody ProductTagCreateRequest request) {
        return ApiResponse.success(tagApplicationService.createTag(request));
    }

    @ApiOperation(
            value = "更新产品标签",
            notes = "更新产品标签。成功返回200；标签不存在返回404；服务器异常返回500。"
    )
    @PutMapping
    public ApiResponse<ProductTagDTO> updateTag(
            @ApiParam(value = "标签更新请求", required = true)
            @RequestBody ProductTagUpdateRequest request) {
        return ApiResponse.success(tagApplicationService.updateTag(request));
    }

    @ApiOperation(
            value = "获取产品标签详情",
            notes = "获取产品标签详情。成功返回200；标签不存在返回404；服务器异常返回500。"
    )
    @GetMapping("/{tagId}")
    public ApiResponse<ProductTagDTO> getTag(
            @ApiParam(value = "标签ID", required = true)
            @PathVariable("tagId") Long tagId) {
        return ApiResponse.success(tagApplicationService.getTag(tagId));
    }

    @ApiOperation(
            value = "获取产品标签列表",
            notes = "获取产品标签分页列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping
    public ApiResponse<ProductTagPageDTO> listTags(
            @ApiParam(value = "页码", required = false)
            @RequestParam(value = "page", required = false) Integer page,
            @ApiParam(value = "每页数量", required = false)
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.success(tagApplicationService.listTags(page, size));
    }
}
