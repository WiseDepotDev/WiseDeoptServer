package com.huicang.wise.application.tag;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.repository.tag.ProductTagJpaEntity;
import com.huicang.wise.infrastructure.repository.tag.ProductTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagApplicationService {

    private static final String DEFAULT_STATUS = "NOT_INBOUND";

    private final ProductTagRepository productTagRepository;
    private final ProductRepository productRepository;

    public TagApplicationService(ProductTagRepository productTagRepository, ProductRepository productRepository) {
        this.productTagRepository = productTagRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductTagDTO createTag(ProductTagCreateRequest request) throws BusinessException {
        if (request.getProductId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品ID不能为空");
        }
        if (productRepository.findById(request.getProductId()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
        }
        ProductTagJpaEntity entity = new ProductTagJpaEntity();
        entity.setTagId(System.currentTimeMillis());
        entity.setProductId(request.getProductId());
        entity.setBarcode(request.getBarcode());
        entity.setNfcUid(request.getNfcUid());
        entity.setRfid(request.getRfid());
        entity.setStatus(DEFAULT_STATUS);
        entity.setCreatedAt(LocalDateTime.now());
        ProductTagJpaEntity saved = productTagRepository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public ProductTagDTO updateTag(ProductTagUpdateRequest request) throws BusinessException {
        if (request.getTagId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签ID不能为空");
        }
        ProductTagJpaEntity entity = productTagRepository.findById(request.getTagId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        if (request.getProductId() != null) {
            if (productRepository.findById(request.getProductId()).isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
            }
            entity.setProductId(request.getProductId());
        }
        entity.setBarcode(request.getBarcode());
        entity.setNfcUid(request.getNfcUid());
        entity.setRfid(request.getRfid());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        ProductTagJpaEntity saved = productTagRepository.save(entity);
        return toDTO(saved);
    }

    public ProductTagDTO getTag(Long tagId) throws BusinessException {
        ProductTagJpaEntity entity = productTagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));
        return toDTO(entity);
    }

    public ProductTagPageDTO listTags(Integer page, Integer size) {
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<ProductTagJpaEntity> result = productTagRepository.findAll(PageRequest.of(pageIndex, pageSize));
        ProductTagPageDTO dto = new ProductTagPageDTO();
        dto.setTotal(result.getTotalElements());
        dto.setRows(result.getContent().stream().map(this::toDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public List<ProductTagDTO> batchBindTags(ProductTagBatchBindRequest request) throws BusinessException {
        if (request.getTags() == null || request.getTags().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签列表不能为空");
        }

        Long commonProductId = request.getProductId();
        if (commonProductId != null) {
            if (productRepository.findById(commonProductId).isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "目标产品不存在: " + commonProductId);
            }
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "必须指定目标产品ID");
        }

        List<ProductTagJpaEntity> entities = new java.util.ArrayList<>();
        long baseId = System.currentTimeMillis();
        int index = 0;

        for (ProductTagBatchBindRequest.TagBindInfo tagInfo : request.getTags()) {
            ProductTagJpaEntity entity = new ProductTagJpaEntity();
            entity.setTagId(baseId + (index++));
            entity.setProductId(commonProductId);
            entity.setBarcode(tagInfo.getBarcode());
            entity.setNfcUid(tagInfo.getNfcUid());
            entity.setRfid(tagInfo.getRfid());
            entity.setStatus(DEFAULT_STATUS);
            entity.setCreatedAt(LocalDateTime.now());
            entities.add(entity);
        }

        List<ProductTagJpaEntity> savedEntities = productTagRepository.saveAll(entities);
        return savedEntities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ProductTagDTO toDTO(ProductTagJpaEntity entity) {
        ProductTagDTO dto = new ProductTagDTO();
        dto.setTagId(entity.getTagId());
        dto.setProductId(entity.getProductId());
        dto.setBarcode(entity.getBarcode());
        dto.setNfcUid(entity.getNfcUid());
        dto.setRfid(entity.getRfid());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

