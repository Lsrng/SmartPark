package com.smartpark;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * validRows 内存占用精确测量（自包含版本，不依赖项目 DTO 类）
 * <p>
 * 使用 JOL (Java Object Layout) 精确测量对象内存。
 * 内含自有的 BillExcelRowDTO 类，避免项目编译问题。
 */
public class BillExcelRowMemoryTest {

    /**
     * 测试 1: 100000 条 validRows 的总内存占用 + 并发安全验证
     */
    @Test
    public void testHundredThousandRowsMemory() {
        int rowCount = 100000;
        int concurrentTasks = 8;

        System.out.println("========================================");
        System.out.println("  100000 条 validRows 内存测量");
        System.out.println("========================================");

        List<BillExcelRowDTO> validRows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            validRows.add(buildSampleRow());
        }

        long totalSize = GraphLayout.parseInstance(validRows).totalSize();
        double totalMB = totalSize / 1024.0 / 1024.0;

        long singleDtoSize = GraphLayout.parseInstance(validRows.get(0)).totalSize();
        long dtoTotal = singleDtoSize * rowCount;
        long arrayListOverhead = totalSize - dtoTotal;

        System.out.println("--- 100000 条单列内存 ---");
        System.out.printf("总字节数: %d bytes%n", totalSize);
        System.out.printf("总 MB: %.4f MB%n", totalMB);
        System.out.printf("单个 DTO: %d bytes%n", singleDtoSize);
        System.out.printf("DTO 合计: %d bytes = %.2f MB%n", dtoTotal, dtoTotal / 1024.0 / 1024.0);
        System.out.printf("ArrayList 引用数组: %d bytes = %.2f KB%n", arrayListOverhead, arrayListOverhead / 1024.0);
        System.out.printf("平均每条: %.1f bytes%n", (double) totalSize / rowCount);

        long heapSize = Runtime.getRuntime().maxMemory();
        double singlePercent = (double) totalSize / heapSize * 100;
        System.out.println();
        System.out.println("--- 堆内存占比（单列） ---");
        System.out.printf("JVM 最大堆: %.2f MB%n", heapSize / 1024.0 / 1024.0);
        System.out.printf("100000 条占比: %.4f%%%n", singlePercent);

        double concurrentTotalMB = totalMB * concurrentTasks;
        double concurrentPercent = singlePercent * concurrentTasks;
        long usableHeap = (long) (heapSize * 0.625);
        System.out.println();
        System.out.println("--- 并发安全验证（8 任务同时处理 10 万条） ---");
        System.out.printf("并发数: %d 个任务%n", concurrentTasks);
        System.out.printf("并发总内存: %.2f MB%n", concurrentTotalMB);
        System.out.printf("并发占堆比: %.4f%%%n", concurrentPercent);
        System.out.printf("可用堆（扣除基础占用约 25%%）: %.2f MB%n", usableHeap / 1024.0 / 1024.0);
        System.out.printf("并发总占用/可用堆: %.2f%%%n", (concurrentTotalMB * 1024 * 1024.0) / usableHeap * 100);

        boolean safe = concurrentTotalMB * 1024 * 1024.0 < usableHeap * 0.7;
        System.out.println();
        System.out.println("--- 结论 ---");
        System.out.println(safe ? "安全：并发总占用低于 70% 安全水位" : "危险：并发总占用超过安全水位");
    }

    /**
     * 测试 2: 10000 条 validRows 的总内存占用
     */
    @Test
    public void testTenThousandRowsMemory() {
        int rowCount = 10000;

        System.out.println("========================================");
        System.out.println("  10000 条 validRows 内存测量");
        System.out.println("========================================");

        List<BillExcelRowDTO> validRows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            validRows.add(buildSampleRow());
        }

        long totalSize = GraphLayout.parseInstance(validRows).totalSize();
        double totalMB = totalSize / 1024.0 / 1024.0;

        long singleDtoSize = GraphLayout.parseInstance(validRows.get(0)).totalSize();
        long dtoTotal = singleDtoSize * rowCount;
        long arrayListOverhead = totalSize - dtoTotal;

        System.out.printf("总 MB: %.4f MB%n", totalMB);
        System.out.printf("单个 DTO: %d bytes%n", singleDtoSize);
        System.out.printf("DTO 合计: %d bytes = %.2f MB%n", dtoTotal, dtoTotal / 1024.0 / 1024.0);
        System.out.printf("ArrayList 引用数组: %d bytes = %.2f KB%n", arrayListOverhead, arrayListOverhead / 1024.0);
        System.out.printf("平均每条: %.1f bytes%n", (double) totalSize / rowCount);

        long heapSize = Runtime.getRuntime().maxMemory();
        System.out.printf("堆占比: %.4f%%%n", (double) totalSize / heapSize * 100);
    }

    /**
     * 测试 3: 内存增长趋势
     */
    @Test
    public void testMemoryGrowthTrend() {
        System.out.println("========================================");
        System.out.println("  内存增长趋势");
        System.out.println("========================================");
        System.out.printf("%-10s %-15s %-15s%n", "条数", "总内存(MB)", "平均(bytes/条)");
        System.out.println("--------------------------------------------");

        int[] scales = {100, 1000, 5000, 10000, 50000, 100000};
        for (int scale : scales) {
            List<BillExcelRowDTO> rows = new ArrayList<>(scale);
            for (int i = 0; i < scale; i++) {
                rows.add(buildSampleRow());
            }
            long total = GraphLayout.parseInstance(rows).totalSize();
            double mb = total / 1024.0 / 1024.0;
            double avg = (double) total / scale;
            System.out.printf("%-10d %-15.4f %-15.1f%n", scale, mb, avg);
        }
    }

    private BillExcelRowDTO buildSampleRow() {
        BillExcelRowDTO dto = new BillExcelRowDTO();
        dto.buildingNo = "A栋";
        dto.unitNo = "1单元";
        dto.roomNo = "101";
        dto.ownerName = "张三";
        dto.phone = "13800138000";
        dto.feeType = "物业费";
        dto.billingStartDate = LocalDate.of(2025, 1, 1);
        dto.billingEndDate = LocalDate.of(2025, 12, 31);
        dto.amountDue = new BigDecimal("1200.00");
        dto.dueDate = LocalDate.of(2026, 1, 15);
        dto.remark = "正常缴费";
        return dto;
    }

    /**
     * 自包含的 DTO 类，避免项目编译依赖
     */
    public static class BillExcelRowDTO {
        public String buildingNo;
        public String unitNo;
        public String roomNo;
        public String ownerName;
        public String phone;
        public String feeType;
        public LocalDate billingStartDate;
        public LocalDate billingEndDate;
        public BigDecimal amountDue;
        public LocalDate dueDate;
        public String remark;
    }
}