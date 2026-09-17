package com.huicang.wise.infrastructure.repository.user;

import com.huicang.wise.domain.repository.user.UserRepository;
import com.huicang.wise.domain.repository.user.UserCoreRepository;
import com.huicang.wise.domain.user.UserCore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户核心仓储实现
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserCoreRepository jpaRepository;

    public UserRepositoryImpl(UserCoreRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserCore> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public List<UserCore> findByStatus(Integer status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public Page<UserCore> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<UserCore> findByUsernameContaining(String username, Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public long countByStatus(Integer status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public <S extends UserCore> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<UserCore> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void delete(UserCore entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public List<UserCore> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<UserCore> findAllById(Iterable<Long> ids) {
        return jpaRepository.findAllById(ids);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}