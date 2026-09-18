package com.huicang.wise.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.TagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类功能描述：数据一致性测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DataConsistencyTest {

    @Autowired private UserRepository userRepository;

    @Autowired private ProductRepository productRepository;

    @Autowired private InventoryRepository inventoryRepository;

    @Autowired private TagRepository tagRepository;

    /** 测试用户数据一致性 */
    @Test
    public void testUserDataConsistency() {
        List<UserCore> users = userRepository.findAll();
        for (UserCore user : users) {
            assertNotNull(user.getUserId(), "用户ID不能为空");
            assertNotNull(user.getUsername(), "用户名不能为空");
        }
    }

    /** 测试产品数据一致性 */
    @Test
    public void testProductDataConsistency() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            assertNotNull(product.getProductId(), "产品ID不能为空");
            assertNotNull(product.getName(), "产品名称不能为空");
        }
    }

    /** 测试库存数据一致性 */
    @Test
    public void testInventoryDataConsistency() {
        List<Inventory> inventories = inventoryRepository.findAll();
        for (Inventory inventory : inventories) {
            assertNotNull(inventory.getInventoryId(), "库存ID不能为空");
            assertNotNull(inventory.getProductId(), "产品ID不能为空");
            assertNotNull(inventory.getQuantity(), "库存数量不能为空");
            assertTrue(inventory.getQuantity() >= 0, "库存数量不能为负数");
        }
    }

    /** 测试标签数据一致性 */
    @Test
    public void testTagDataConsistency() {
        List<ProductTag> tags = tagRepository.findAll();
        for (ProductTag tag : tags) {
            assertNotNull(tag.getTagId(), "标签ID不能为空");
            assertNotNull(tag.getProductId(), "产品ID不能为空");
        }
    }

    /** 测试库存与产品关联一致性 */
    @Test
    public void testInventoryProductConsistency() {
        List<Inventory> inventories = inventoryRepository.findAll();
        for (Inventory inventory : inventories) {
            Product product = productRepository.findById(inventory.getProductId()).orElse(null);
            assertNotNull(product, "库存关联的产品必须存在");
        }
    }

    /** 测试标签与产品关联一致性 */
    @Test
    public void testTagProductConsistency() {
        List<ProductTag> tags = tagRepository.findAll();
        for (ProductTag tag : tags) {
            Product product = productRepository.findById(tag.getProductId()).orElse(null);
            assertNotNull(product, "标签关联的产品必须存在");
        }
    }

    /** 测试数据完整性约束 */
    @Test
    public void testDataIntegrityConstraints() {
        List<UserCore> users = userRepository.findAll();
        for (UserCore user : users) {
            assertNotNull(user.getUserId(), "用户ID不能为空");
            assertFalse(user.getUsername().isEmpty(), "用户名不能为空");
        }

        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            assertNotNull(product.getProductId(), "产品ID不能为空");
            assertNotNull(product.getName(), "产品名称不能为空");
        }
    }

    /** 测试数据唯一性约束 */
    @Test
    public void testDataUniquenessConstraints() {
        List<UserCore> users = userRepository.findAll();
        List<String> usernames = users.stream().map(UserCore::getUsername).toList();

        assertEquals(usernames.size(), usernames.stream().distinct().count(), "用户名必须唯一");
    }
}
