package com.huicang.wise.performance;

import static org.junit.jupiter.api.Assertions.*;

import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 类功能描述：响应时间测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
public class ResponseTimeTest {

    @Autowired private InventoryApplicationService inventoryService;

    @Autowired private DeviceApplicationService deviceService;

    @Autowired private TagApplicationService tagService;

    private static final int TEST_ITERATIONS = 100;
    private static final long MAX_ACCEPTABLE_RESPONSE_TIME = 1000;

    /** 测试库存服务响应时间 */
    @Test
    public void testInventoryServiceResponseTime() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            try {
                inventoryService.searchInventoryByLocation(null);
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            } catch (Exception e) {
                fail("库存服务调用失败: " + e.getMessage());
            }
        }

        long averageResponseTime =
                responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();

        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("库存服务响应时间测试结果:");
        System.out.println("测试次数: " + TEST_ITERATIONS);
        System.out.println("平均响应时间: " + averageResponseTime + "ms");
        System.out.println("最大响应时间: " + maxResponseTime + "ms");
        System.out.println("最小响应时间: " + minResponseTime + "ms");

        assertTrue(
                averageResponseTime < MAX_ACCEPTABLE_RESPONSE_TIME,
                "平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
    }

    /** 测试设备服务响应时间 */
    @Test
    public void testDeviceServiceResponseTimeWithParams() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            try {
                deviceService.listDevices((short) 1, null, null, null);
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            } catch (Exception e) {
                fail("设备服务调用失败: " + e.getMessage());
            }
        }

        long averageResponseTime =
                responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();

        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("设备服务响应时间测试结果:");
        System.out.println("测试次数: " + TEST_ITERATIONS);
        System.out.println("平均响应时间: " + averageResponseTime + "ms");
        System.out.println("最大响应时间: " + maxResponseTime + "ms");
        System.out.println("最小响应时间: " + minResponseTime + "ms");

        assertTrue(
                averageResponseTime < MAX_ACCEPTABLE_RESPONSE_TIME,
                "平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
    }

    /** 测试标签服务响应时间 */
    @Test
    public void testTagServiceResponseTime() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            try {
                tagService.listTags(null, null, null, 1, 10);
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            } catch (Exception e) {
                fail("标签服务调用失败: " + e.getMessage());
            }
        }

        long averageResponseTime =
                responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();

        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("标签服务响应时间测试结果:");
        System.out.println("测试次数: " + TEST_ITERATIONS);
        System.out.println("平均响应时间: " + averageResponseTime + "ms");
        System.out.println("最大响应时间: " + maxResponseTime + "ms");
        System.out.println("最小响应时间: " + minResponseTime + "ms");

        assertTrue(
                averageResponseTime < MAX_ACCEPTABLE_RESPONSE_TIME,
                "平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
    }

    /** 测试混合服务响应时间 */
    @Test
    public void testMixedServiceResponseTime() {
        List<Long> inventoryResponseTimes = new ArrayList<>();
        List<Long> deviceResponseTimes = new ArrayList<>();
        List<Long> tagResponseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            try {
                switch (i % 3) {
                    case 0:
                        inventoryService.searchInventoryByLocation(null);
                        break;
                    case 1:
                        deviceService.listDevices(null, null, null, null);
                        break;
                    case 2:
                        tagService.listTags(null, null, null, 1, 10);
                        break;
                }
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                switch (i % 3) {
                    case 0:
                        inventoryResponseTimes.add(responseTime);
                        break;
                    case 1:
                        deviceResponseTimes.add(responseTime);
                        break;
                    case 2:
                        tagResponseTimes.add(responseTime);
                        break;
                }
            } catch (Exception e) {
                fail("服务调用失败: " + e.getMessage());
            }
        }

        long inventoryAvg =
                inventoryResponseTimes.stream().mapToLong(Long::longValue).sum()
                        / inventoryResponseTimes.size();

        long deviceAvg =
                deviceResponseTimes.stream().mapToLong(Long::longValue).sum()
                        / deviceResponseTimes.size();

        long tagAvg =
                tagResponseTimes.stream().mapToLong(Long::longValue).sum()
                        / tagResponseTimes.size();

        System.out.println("混合服务响应时间测试结果:");
        System.out.println("测试次数: " + TEST_ITERATIONS);
        System.out.println("库存服务平均响应时间: " + inventoryAvg + "ms");
        System.out.println("设备服务平均响应时间: " + deviceAvg + "ms");
        System.out.println("标签服务平均响应时间: " + tagAvg + "ms");

        assertTrue(
                inventoryAvg < MAX_ACCEPTABLE_RESPONSE_TIME,
                "库存服务平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
        assertTrue(
                deviceAvg < MAX_ACCEPTABLE_RESPONSE_TIME,
                "设备服务平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
        assertTrue(
                tagAvg < MAX_ACCEPTABLE_RESPONSE_TIME,
                "标签服务平均响应时间应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
    }

    /** 测试响应时间稳定性 */
    @Test
    public void testResponseTimeStability() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            try {
                inventoryService.searchInventoryByLocation(null);
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
            } catch (Exception e) {
                fail("库存服务调用失败: " + e.getMessage());
            }
        }

        long averageResponseTime =
                responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();

        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        long variance = maxResponseTime - minResponseTime;

        System.out.println("响应时间稳定性测试结果:");
        System.out.println("测试次数: " + TEST_ITERATIONS);
        System.out.println("平均响应时间: " + averageResponseTime + "ms");
        System.out.println("最大响应时间: " + maxResponseTime + "ms");
        System.out.println("最小响应时间: " + minResponseTime + "ms");
        System.out.println("响应时间方差: " + variance + "ms");

        assertTrue(
                variance < MAX_ACCEPTABLE_RESPONSE_TIME,
                "响应时间方差应小于" + MAX_ACCEPTABLE_RESPONSE_TIME + "ms");
    }
}
