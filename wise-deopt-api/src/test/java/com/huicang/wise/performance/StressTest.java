package com.huicang.wise.performance;

import static org.junit.jupiter.api.Assertions.*;

import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 类功能描述：压力测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
public class StressTest {

    @Autowired private InventoryApplicationService inventoryService;

    @Autowired private DeviceApplicationService deviceService;

    @Autowired private TagApplicationService tagService;

    private static final int THREAD_COUNT = 50;
    private static final int REQUESTS_PER_THREAD = 100;
    private static final int TOTAL_REQUESTS = THREAD_COUNT * REQUESTS_PER_THREAD;

    /** 测试库存服务压力 */
    @Test
    public void testInventoryServiceStress() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(
                    () -> {
                        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                            try {
                                inventoryService.searchInventoryByLocation(null);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                failureCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("压力测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("库存服务压力测试结果:");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均响应时间: " + (duration / TOTAL_REQUESTS) + "ms");
        System.out.println("吞吐量: " + (TOTAL_REQUESTS * 1000 / duration) + " 请求/秒");

        assertTrue(successCount.get() >= TOTAL_REQUESTS * 0.95, "成功率应大于95%");
    }

    /** 测试设备服务压力 */
    @Test
    public void testDeviceServiceStress() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(
                    () -> {
                        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                            try {
                                deviceService.listDevices(null, null, null, null);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                failureCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("压力测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("设备服务压力测试结果:");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均响应时间: " + (duration / TOTAL_REQUESTS) + "ms");
        System.out.println("吞吐量: " + (TOTAL_REQUESTS * 1000 / duration) + " 请求/秒");

        assertTrue(successCount.get() >= TOTAL_REQUESTS * 0.95, "成功率应大于95%");
    }

    /** 测试标签服务压力 */
    @Test
    public void testTagServiceStress() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(
                    () -> {
                        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                            try {
                                tagService.listTags(null, null, null, 1, 10);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                failureCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("压力测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("标签服务压力测试结果:");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均响应时间: " + (duration / TOTAL_REQUESTS) + "ms");
        System.out.println("吞吐量: " + (TOTAL_REQUESTS * 1000 / duration) + " 请求/秒");

        assertTrue(successCount.get() >= TOTAL_REQUESTS * 0.95, "成功率应大于95%");
    }

    /** 测试混合服务压力 */
    @Test
    public void testMixedServiceStress() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            executor.submit(
                    () -> {
                        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                            try {
                                switch (threadIndex % 3) {
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
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                failureCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
        }

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("压力测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("混合服务压力测试结果:");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failureCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均响应时间: " + (duration / TOTAL_REQUESTS) + "ms");
        System.out.println("吞吐量: " + (TOTAL_REQUESTS * 1000 / duration) + " 请求/秒");

        assertTrue(successCount.get() >= TOTAL_REQUESTS * 0.95, "成功率应大于95%");
    }
}
