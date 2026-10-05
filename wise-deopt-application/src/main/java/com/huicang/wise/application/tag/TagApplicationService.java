package com.huicang.wise.application.tag;

import com.huicang.wise.application.human.HumanPurpose;
import com.huicang.wise.application.human.HumanVerifyApplicationService;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.TagRepository;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类功能描述：标签应用服务
 *
 * @author xingchentye
 * @date 2026-02-27
 * @modified xingchentye 2026-02-27 实现版本0.1.12功能：标签CRUD、绑定/解绑、批量操作
 */
@Service
public class TagApplicationService {

    private final TagRepository tagRepository;
    private final ProductRepository productRepository;
    private final HumanVerifyApplicationService humanVerifyApplicationService;

    public TagApplicationService(
            TagRepository tagRepository,
            ProductRepository productRepository,
            HumanVerifyApplicationService humanVerifyApplicationService) {
        this.tagRepository = tagRepository;
        this.productRepository = productRepository;
        this.humanVerifyApplicationService = humanVerifyApplicationService;
    }

    /**
     * 方法功能描述：创建标签
     *
     * @param request 标签创建请求
     * @return 标签信息
     * @throws BusinessException 当标签编码为空或产品不存在时抛出异常
     */
    @Transactional
    public ProductTagDTO createTag(ProductTagCreateRequest request) throws BusinessException {
        boolean hasBarcode = request.getBarcode() != null && !request.getBarcode().isBlank();
        boolean hasNfc = request.getNfcUid() != null && !request.getNfcUid().isBlank();
        boolean hasRfid = request.getRfid() != null && !request.getRfid().isBlank();

        if (!hasBarcode && !hasNfc && !hasRfid) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "条形码、NFC标识、RFID标识至少需填写一项");
        }

        if (hasBarcode && tagRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "条形码已存在");
        }

        if (hasRfid && tagRepository.existsByRfid(request.getRfid())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "RFID标识已存在");
        }

        if (hasNfc && tagRepository.existsByNfcUid(request.getNfcUid())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "NFC标识已存在");
        }

        if (request.getProductId() != null) {
            if (productRepository.findById(request.getProductId()).isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
            }
        }

        ProductTag entity = new ProductTag();
        if (request.getProductId() != null) {
            entity.setProductId(request.getProductId());
        }
        entity.setBarcode(
                request.getBarcode() != null && !request.getBarcode().isBlank()
                        ? request.getBarcode()
                        : null);
        entity.setNfcUid(
                request.getNfcUid() != null && !request.getNfcUid().isBlank()
                        ? request.getNfcUid()
                        : null);
        entity.setRfid(
                request.getRfid() != null && !request.getRfid().isBlank()
                        ? request.getRfid()
                        : null);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        entity.setCreateBy(1L);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        ProductTag saved = tagRepository.save(entity);
        return toProductTagDTO(saved);
    }

    /**
     * 方法功能描述：更新标签
     *
     * @param tagId 标签ID
     * @param request 标签更新请求
     * @return 标签信息
     * @throws BusinessException 当标签不存在时抛出异常
     */
    @Transactional
    public ProductTagDTO updateTag(Long tagId, ProductTagUpdateRequest request)
            throws BusinessException {
        ProductTag entity =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));

        if (request.getProductId() != null) {
            if (productRepository.findById(request.getProductId()).isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
            }
            entity.setProductId(request.getProductId());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (request.getBarcode() != null) {
            if (!request.getBarcode().isBlank()
                    && !request.getBarcode().equals(entity.getBarcode())
                    && tagRepository.existsByBarcode(request.getBarcode())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "条形码已存在");
            }
            entity.setBarcode(request.getBarcode().isBlank() ? null : request.getBarcode());
        }

        if (request.getNfcUid() != null) {
            if (!request.getNfcUid().isBlank()
                    && !request.getNfcUid().equals(entity.getNfcUid())
                    && tagRepository.existsByNfcUid(request.getNfcUid())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "NFC标识已存在");
            }
            entity.setNfcUid(request.getNfcUid().isBlank() ? null : request.getNfcUid());
        }

        if (request.getRfid() != null) {
            if (!request.getRfid().isBlank()
                    && !request.getRfid().equals(entity.getRfid())
                    && tagRepository.existsByRfid(request.getRfid())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "RFID标识已存在");
            }
            entity.setRfid(request.getRfid().isBlank() ? null : request.getRfid());
        }

        entity.setUpdateTime(LocalDateTime.now());

        ProductTag saved = tagRepository.save(entity);
        return toProductTagDTO(saved);
    }

    /**
     * 方法功能描述：删除标签
     *
     * @param tagId 标签ID
     * @throws BusinessException 当标签不存在时抛出异常
     */
    @Transactional
    public void deleteTag(Long tagId) throws BusinessException {
        if (!tagRepository.existsById(tagId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tagRepository.deleteById(tagId);
    }

    /**
     * 方法功能描述：获取标签详情
     *
     * @param tagId 标签ID
     * @return 标签信息
     * @throws BusinessException 当标签不存在时抛出异常
     */
    @Cacheable(prefix = "tag", key = "#tagId", timeout = 1800)
    public ProductTagDTO getTag(Long tagId) throws BusinessException {
        ProductTag entity =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toProductTagDTO(entity);
    }

    /**
     * 方法功能描述：根据条形码查询标签
     *
     * @param barcode 条形码
     * @return 标签信息
     * @throws BusinessException 当标签不存在时抛出异常
     */
    @Cacheable(prefix = "tag:code", key = "#barcode", timeout = 1800)
    public ProductTagDTO getTagByCode(String barcode) throws BusinessException {
        ProductTag entity =
                tagRepository
                        .findByBarcode(barcode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toProductTagDTO(entity);
    }

    /**
     * 方法功能描述：查询标签列表（支持分页和筛选）
     *
     * @param productId 产品ID（可选）
     * @param status 标签状态（可选）
     * @param search 搜索关键词（可选，支持RFID、条码、NFC UID）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 标签分页数据
     */
    public ProductTagPageDTO listTags(
            Long productId, String status, String search, Integer page, Integer pageSize) {
        int actualPage = page != null && page >= 1 ? page : 1;
        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        int pageNum = actualPage - 1;

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum, actualPageSize, sort);

        Short statusValue = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusValue = Short.parseShort(status);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "标签状态格式不正确: " + status);
            }
        }

        Page<ProductTag> pageResult =
                tagRepository.findTags(productId, statusValue, search, pageable);

        ProductTagPageDTO result = new ProductTagPageDTO();
        result.setTotal(pageResult.getTotalElements());
        result.setRows(
                pageResult.getContent().stream()
                        .map(this::toProductTagDTO)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 方法功能描述：按产品查询标签
     *
     * @param productId 产品ID
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 标签分页数据
     */
    public ProductTagPageDTO listTagsByProduct(Long productId, Integer page, Integer pageSize) {
        int actualPage = page != null && page >= 1 ? page : 1;
        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        int pageNum = actualPage - 1;

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum, actualPageSize, sort);

        Page<ProductTag> pageResult = tagRepository.findByProductId(productId, pageable);

        ProductTagPageDTO result = new ProductTagPageDTO();
        result.setTotal(pageResult.getTotalElements());
        result.setRows(
                pageResult.getContent().stream()
                        .map(this::toProductTagDTO)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 方法功能描述：绑定标签到产品
     *
     * @param tagId 标签ID
     * @param productId 产品ID
     * @return 标签信息
     * @throws BusinessException 当标签或产品不存在时抛出异常
     */
    @Transactional
    public ProductTagDTO bindTag(Long tagId, Long productId) throws BusinessException {
        if (!tagRepository.existsById(tagId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }

        if (productRepository.findById(productId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
        }

        int updated = tagRepository.bindProduct(tagId, productId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "绑定失败");
        }

        ProductTag entity =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toProductTagDTO(entity);
    }

    /**
     * 方法功能描述：解绑标签
     *
     * @param tagId 标签ID
     * @return 标签信息
     * @throws BusinessException 当标签不存在时抛出异常
     */
    @Transactional
    public ProductTagDTO unbindTag(Long tagId) throws BusinessException {
        if (!tagRepository.existsById(tagId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }

        int updated = tagRepository.unbindProduct(tagId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "解绑失败");
        }

        ProductTag entity =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toProductTagDTO(entity);
    }

    /**
     * 方法功能描述：批量绑定标签
     *
     * @param request 批量绑定请求
     * @return 绑定结果
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @Transactional
    public BatchBindResult batchBindTags(ProductTagBatchBindRequest request)
            throws BusinessException {
        /*
         * 人机验证票据为必填项，**校验写在 Service 层**（不是 Controller）。
         *
         * 为什么强调这一点：原先有**两条** HTTP 路径 —— 无票的 `/batch-bind` 与带验证码的
         * `/batch-bind-with-captcha` —— 而守卫只加在后者，于是任何持有权限的会话直接调前者
         * 就绕过了验证码。校验写在这一层，才谈得上"同一业务效果只有一个入口"。
         */
        humanVerifyApplicationService.enforce(request.getHumanToken(), HumanPurpose.TAG_BATCH_BIND);

        if (request.getProductId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品ID不能为空");
        }

        if (request.getTagIds() == null || request.getTagIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签ID列表不能为空");
        }

        if (productRepository.findById(request.getProductId()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
        }

        int updated = tagRepository.batchBindProducts(request.getTagIds(), request.getProductId());

        BatchBindResult result = new BatchBindResult();
        result.setProductId(request.getProductId());
        result.setTotalCount(request.getTagIds().size());
        result.setSuccessCount(updated);
        result.setFailedCount(request.getTagIds().size() - updated);

        return result;
    }

    /**
     * 方法功能描述：批量解绑标签
     *
     * @param tagIds 标签ID列表
     * @return 解绑结果
     */
    @Transactional
    public BatchUnbindResult batchUnbindTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签ID列表不能为空");
        }

        int updated = tagRepository.batchUnbindProducts(tagIds);

        BatchUnbindResult result = new BatchUnbindResult();
        result.setTotalCount(tagIds.size());
        result.setSuccessCount(updated);
        result.setFailedCount(tagIds.size() - updated);

        return result;
    }

    /**
     * 方法功能描述：搜索标签
     *
     * @param keyword 搜索关键字
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 标签分页数据
     */
    public ProductTagPageDTO searchTags(String keyword, Integer page, Integer pageSize) {
        int actualPage = page != null && page >= 1 ? page : 1;
        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        int pageNum = actualPage - 1;

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum, actualPageSize, sort);

        Page<ProductTag> pageResult = tagRepository.findByBarcodeContaining(keyword, pageable);

        ProductTagPageDTO result = new ProductTagPageDTO();
        result.setTotal(pageResult.getTotalElements());
        result.setRows(
                pageResult.getContent().stream()
                        .map(this::toProductTagDTO)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 方法功能描述：批量查询标签
     *
     * @param barcodes 条形码列表
     * @return 标签列表
     */
    public List<ProductTagDTO> batchGetTags(List<String> barcodes) {
        List<ProductTag> entities = tagRepository.findByBarcodeIn(barcodes);
        return entities.stream().map(this::toProductTagDTO).collect(Collectors.toList());
    }

    private ProductTagDTO toProductTagDTO(ProductTag entity) {
        ProductTagDTO dto = new ProductTagDTO();
        dto.setTagId(entity.getTagId());
        dto.setProductId(entity.getProductId());
        dto.setBarcode(entity.getBarcode());
        dto.setNfcUid(entity.getNfcUid());
        dto.setRfid(entity.getRfid());
        dto.setStatus(entity.getStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        if (entity.getProductId() != null) {
            Product product = productRepository.findById(entity.getProductId()).orElse(null);
            if (product != null) {
                dto.setProductName(product.getName());
            }
        }

        return dto;
    }
}
