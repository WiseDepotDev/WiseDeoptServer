package com.huicang.wise.application.user;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.user.UserCoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类功能描述：用户管理应用服务
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Service
public class UserApplicationService {

    private final UserCoreRepository userCoreRepository;

    public UserApplicationService(UserCoreRepository userCoreRepository) {
        this.userCoreRepository = userCoreRepository;
    }

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        if (userCoreRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        UserCoreJpaEntity user = new UserCoreJpaEntity();
        // Simple ID generation strategy
        user.setUserId(System.nanoTime() + (long)(Math.random() * 1000));
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(hash(request.getPassword()));
        user.setNfcId(request.getNfcId());
        if (request.getPin() != null && !request.getPin().isBlank()) {
            user.setPinHash(hash(request.getPin()));
        }
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginFailCount(0);

        UserCoreJpaEntity savedUser = userCoreRepository.save(user);
        return toUserDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UserUpdateRequest request) {
        UserCoreJpaEntity user = userCoreRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getNfcId() != null) {
            user.setNfcId(request.getNfcId());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(hash(request.getPassword()));
        }
        if (request.getPin() != null && !request.getPin().isBlank()) {
            user.setPinHash(hash(request.getPin()));
        }
        user.setUpdatedAt(LocalDateTime.now());

        UserCoreJpaEntity savedUser = userCoreRepository.save(user);
        return toUserDTO(savedUser);
    }

    public UserDTO getUser(Long userId) {
        UserCoreJpaEntity user = userCoreRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        return toUserDTO(user);
    }

    public UserPageDTO listUsers(Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Page<UserCoreJpaEntity> userPage = userCoreRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        UserPageDTO dto = new UserPageDTO();
        dto.setTotal(userPage.getTotalElements());
        List<UserDTO> items = userPage.getContent().stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
        dto.setItems(items);
        return dto;
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userCoreRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userCoreRepository.deleteById(userId);
    }

    private UserDTO toUserDTO(UserCoreJpaEntity entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setEnabled(entity.getEnabled());
        dto.setNfcId(entity.getNfcId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private String hash(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
