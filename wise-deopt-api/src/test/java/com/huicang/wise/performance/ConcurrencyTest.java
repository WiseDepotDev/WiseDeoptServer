package com.huicang.wise.performance;

import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类功能描述：并发测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired
    private InventoryApplicationService inventoryService;

    @Autowired
    private DeviceApplicationService deviceService;

    @Autowired
    private TagApplicationService tagService;

    private static final int CONCURRENT_THREADS = 20;
    private static final int OPERATIONS_PER_THREAD = 10;

    /**
     * 测试库存服务并发读取
     */
    @Test
    public void testInventoryConcurrentRead() {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        inventoryService.searchInventoryByLocation(null);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("并发测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("库存服务并发读取测试结果:");
        System.out.println("并发线程数: " + CONCURRENT_THREADS);
        System.out.println("每个线程操作数: " + OPERATIONS_PER_THREAD);
        System.out.println("成功线程数: " + successCount.get());
        System.out.println("失败线程数: " + errorCount.get());
        System.out.println("总耗时: " + duration + "ms");

        assertEquals(CONCURRENT_THREADS, successCount.get() + errorCount.get(), "所有线程都应完成");
        assertEquals(0, errorCount.get(), "不应有错误");
    }

    /**
     * 测试设备服务并发读取
     */
    @Test
    public void testDeviceConcurrentRead() {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        deviceService.listDevices(null, null, null, null);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("并发测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("设备服务并发读取测试结果:");
        System.out.println("并发线程数: " + CONCURRENT_THREADS);
        System.out.println("每个线程操作数: " + OPERATIONS_PER_THREAD);
        System.out.println("成功线程数: " + successCount.get());
        System.out.println("失败线程数: " + errorCount.get());
        System.out.println("总耗时: " + duration + "ms");

        assertEquals(CONCURRENT_THREADS, successCount.get() + errorCount.get(), "所有线程都应完成");
        assertEquals(0, errorCount.get(), "不应有错误");
    }

    /**
     * 测试标签服务并发读取
     */
    @Test
    public void testTagConcurrentRead() {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        tagService.listTags(null, null, null, 1, 10);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("并发测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("标签服务并发读取测试结果:");
        System.out.println("并发线程数: " + CONCURRENT_THREADS);
        System.out.println("每个线程操作数: " + OPERATIONS_PER_THREAD);
        System.out.println("成功线程数: " + successCount.get());
        System.out.println("失败线程数: " + errorCount.get());
        System.out.println("总耗时: " + duration + "ms");

        assertEquals(CONCURRENT_THREADS, successCount.get() + errorCount.get(), "所有线程都应完成");
        assertEquals(0, errorCount.get(), "不应有错误");
    }

    /**
     * 测试混合服务并发读取
     */
    @Test
    public void testMixedServiceConcurrentRead() {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
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
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("并发测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("混合服务并发读取测试结果:");
        System.out.println("并发线程数: " + CONCURRENT_THREADS);
        System.out.println("每个线程操作数: " + OPERATIONS_PER_THREAD);
        System.out.println("成功线程数: " + successCount.get());
        System.out.println("失败线程数: " + errorCount.get());
        System.out.println("总耗时: " + duration + "ms");

        assertEquals(CONCURRENT_THREADS, successCount.get() + errorCount.get(), "所有线程都应完成");
        assertEquals(0, errorCount.get(), "不应有错误");
    }
}