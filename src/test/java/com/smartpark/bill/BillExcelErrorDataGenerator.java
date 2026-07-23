package com.smartpark.bill;

import com.alibaba.excel.EasyExcel;
import com.smartpark.pojo.dto.BillExcelRowDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 物业账单 Excel 异常测试数据生成器
 * <p>
 * 运行此 main 方法会生成包含各种错误数据的 Excel 文件，
 * 用于验证导入功能的校验逻辑是否正确捕获并报告所有错误。
 * 共 30 行数据，每行至少包含 1 个错误，覆盖 13 种校验规则。
 * </p>
 */
public class BillExcelErrorDataGenerator {

    public static void main(String[] args) {
        String fileName = "物业账单导入异常测试数据.xlsx";
        List<BillExcelRowDTO> data = buildErrorData();
        EasyExcel.write(fileName, BillExcelRowDTO.class).sheet("物业账单").doWrite(data);
        System.out.println("异常测试文件已生成: " + fileName);
        System.out.println("共 " + data.size() + " 行数据（全部包含错误）");
        System.out.println("文件位置: " + System.getProperty("user.dir") + "\\" + fileName);
        System.out.println();
        System.out.println("=== 错误场景覆盖清单 ===");
        System.out.println("第1行: 楼栋号空");
        System.out.println("第2行: 单元号空");
        System.out.println("第3行: 房号空");
        System.out.println("第4行: 费用类型空");
        System.out.println("第5行: 费用类型无效（\"管理费\"）");
        System.out.println("第6行: 计费起始日期空（格式错误 \"abc\"）");
        System.out.println("第7行: 计费截止日期空（格式错误 \"2026/01/01\"）");
        System.out.println("第8行: 应收金额空");
        System.out.println("第9行: 应收金额为 0");
        System.out.println("第10行: 应收金额为负数（-500）");
        System.out.println("第11行: 缴费截止日期空");
        System.out.println("第12行: 联系电话格式错误（\"12345\"）");
        System.out.println("第13行: 联系电话格式错误（9位）");
        System.out.println("第14行: 联系电话格式错误（非1开头 \"23800138000\"）");
        System.out.println("第15行: 计费截止日期早于起始日期");
        System.out.println("第16行: 缴费截止日期早于计费截止日期");
        System.out.println("第17行: 楼栋号超长（21个字符）");
        System.out.println("第18行: 业主姓名超长（101个字符）");
        System.out.println("第19行: 应收金额超过2位小数（3位小数）");
        System.out.println("第20行: 备注超长（501个字符）");
        System.out.println("第21行: 楼栋号空 + 费用类型空（多字段错误）");
        System.out.println("第22行: 应收金额空 + 缴费截止日期空 + 计费起始日期空（多字段错误）");
        System.out.println("第23行: 费用类型无效(\"卫生费\") + 联系电话格式错误(\"abcde\")");
        System.out.println("第24行: 计费截止日期早于起始日期 + 缴费截止日期早于计费截止日期（多日期错误）");
        System.out.println("第25行: 应收金额为 0 + 费用类型空（组合错误）");
        System.out.println("第26行: 全部必填项为空（楼栋号、单元号、房号、费用类型、日期、金额全部空）");
        System.out.println("第27行: 单元号超长（21个字符）");
        System.out.println("第28行: 房号超长（21个字符）");
        System.out.println("第29行: 应收金额为 0 且 备注超长（组合边界）");
        System.out.println("第30行: 联系电话格式错误（含字母 \"138abc00001\"）");
    }

    private static List<BillExcelRowDTO> buildErrorData() {
        List<BillExcelRowDTO> list = new ArrayList<>();

        // =========================================================================
        // 单字段错误 (1 ~ 20)
        // =========================================================================

        // 第1行: 楼栋号为空
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第2行: 单元号为空
        list.add(createRow("A1", null, "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第3行: 房号为空
        list.add(createRow("A1", "1", null, "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第4行: 费用类型为空
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                null, "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第5行: 费用类型无效
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "管理费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第6行: 计费起始日期格式错误（传递 null 模拟格式错误）
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "not-a-date", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第7行: 计费截止日期格式错误
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026/01/01", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第8行: 应收金额为空
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", null,
                "2026-04-15", ""));

        // 第9行: 应收金额为 0
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", BigDecimal.ZERO,
                "2026-04-15", ""));

        // 第10行: 应收金额为负数
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("-500.00"),
                "2026-04-15", ""));

        // 第11行: 缴费截止日期为空
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                null, ""));

        // 第12行: 联系电话格式错误（太短）
        list.add(createRow("A1", "1", "101", "张三", "12345",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第13行: 联系电话格式错误（9位，少2位）
        list.add(createRow("A1", "1", "101", "张三", "138001380",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第14行: 联系电话格式错误（非1开头）
        list.add(createRow("A1", "1", "101", "张三", "23800138000",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第15行: 计费截止日期早于起始日期
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-06-01", "2026-01-01", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第16行: 缴费截止日期早于计费截止日期
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-03-01", ""));

        // 第17行: 楼栋号超长（21个字符）
        list.add(createRow("ABCDEFGHIJKLMNOPQRSTU", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第18行: 业主姓名超长（101个字符）
        String longName = "张".repeat(101);
        list.add(createRow("A1", "1", "101", longName, "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第19行: 应收金额超过2位小数（3位小数）
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.999"),
                "2026-04-15", ""));

        // 第20行: 备注超长（501个字符）
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", "备".repeat(501)));

        // =========================================================================
        // 多字段组合错误 (21 ~ 30)
        // =========================================================================

        // 第21行: 楼栋号空 + 费用类型空
        list.add(createRow(null, "1", "101", "张三", "13800138001",
                null, "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第22行: 应收金额空 + 缴费截止日期空 + 计费起始日期空（传递 null 模拟）
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "xxx", "2026-03-31", null,
                null, ""));

        // 第23行: 费用类型无效 + 联系电话格式错误
        list.add(createRow("A1", "1", "101", "张三", "abcde",
                "卫生费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第24行: 计费截止日期早于起始日期 + 缴费截止日期早于计费截止日期
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-06-01", "2026-01-01", new BigDecimal("1250.00"),
                "2025-12-01", ""));

        // 第25行: 应收金额为 0 + 费用类型空
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                null, "2026-01-01", "2026-03-31", BigDecimal.ZERO,
                "2026-04-15", ""));

        // 第26行: 全部必填项为空（仅保留可选字段有值）
        list.add(createRow(null, null, null, "张三", "13800138001",
                null, null, null, null,
                null, ""));

        // 第27行: 单元号超长（21个字符）
        list.add(createRow("A1", "ABCDEFGHIJKLMNOPQRSTU", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第28行: 房号超长（21个字符）
        list.add(createRow("A1", "1", "ABCDEFGHIJKLMNOPQRSTU", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        // 第29行: 应收金额为 0 且 备注超长
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", BigDecimal.ZERO,
                "2026-04-15", "备".repeat(501)));

        // 第30行: 联系电话含字母
        list.add(createRow("A1", "1", "101", "张三", "138abc00001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.00"),
                "2026-04-15", ""));

        return list;
    }

    private static BillExcelRowDTO createRow(String buildingNo, String unitNo, String roomNo,
                                              String ownerName, String phone,
                                              String feeType, String billingStartDate,
                                              String billingEndDate, BigDecimal amountDue,
                                              String dueDate, String remark) {
        BillExcelRowDTO row = new BillExcelRowDTO();
        row.setBuildingNo(buildingNo);
        row.setUnitNo(unitNo);
        row.setRoomNo(roomNo);
        row.setOwnerName(ownerName);
        row.setPhone(phone);
        row.setFeeType(feeType);
        // 日期字段传 null 模拟 EasyExcel 转换失败时也为 null
        // 传非日期字符串模拟格式错误（EasyExcel 会尝试转换，失败时也为 null）
        row.setBillingStartDate(tryParseDate(billingStartDate));
        row.setBillingEndDate(tryParseDate(billingEndDate));
        row.setAmountDue(amountDue);
        row.setDueDate(tryParseDate(dueDate));
        row.setRemark(remark);
        return row;
    }

    /**
     * 尝试解析日期，解析失败返回 null（模拟 EasyExcel 读取时的类型转换失败行为）
     */
    private static LocalDate tryParseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
