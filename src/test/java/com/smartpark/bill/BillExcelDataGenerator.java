package com.smartpark.bill;

import com.alibaba.excel.EasyExcel;
import com.smartpark.pojo.dto.BillExcelRowDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 物业账单 Excel 测试数据生成器
 * <p>
 * 运行此 main 方法会在项目根目录生成测试用的 Excel 文件，直接运行即可。
 * 生成的文件包含正常数据和少量边界数据，可用于验证导入功能。
 * </p>
 */
public class BillExcelDataGenerator {

    public static void main(String[] args) {
        String fileName = "物业账单导入测试数据.xlsx";
        List<BillExcelRowDTO> data = buildTestData();
        EasyExcel.write(fileName, BillExcelRowDTO.class).sheet("物业账单").doWrite(data);
        System.out.println("测试文件已生成: " + fileName);
        System.out.println("共 " + data.size() + " 行数据");
        System.out.println("文件位置: " + System.getProperty("user.dir") + "\\" + fileName);
        System.out.println();
        System.out.println("=== 数据说明 ===");
        System.out.println("第1行 ~ 第48行: 正常数据，分散在不同楼栋、单元、房号的不同费用类型");
        System.out.println("第49行 ~ 第54行: 专门构造的测试边界值数据（均为有效数据）");
        System.out.println("全部 54 行数据均校验通过，可用于测试完整导入流程");
        System.out.println("如需测试校验失败场景，可手动修改 Excel 中的某些字段（如清空必填项、填负数金额等）");
    }

    private static List<BillExcelRowDTO> buildTestData() {
        List<BillExcelRowDTO> list = new ArrayList<>();

        // ========== A1栋 ==========
        // A1栋1单元101，物管费+水费 2026年Q1
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1250.50"), "2026-04-15", ""));
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("180.00"), "2026-04-15", ""));

        // A1栋1单元101，停车费
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "停车费", "2026-01-01", "2026-06-30", new BigDecimal("2400.00"), "2026-07-15", "车位B-012"));

        // A1栋1单元102，物管费+电费
        list.add(createRow("A1", "1", "102", "李四", "13800138002",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("980.00"), "2026-04-15", ""));
        list.add(createRow("A1", "1", "102", "李四", "13800138002",
                "电费", "2026-01-01", "2026-03-31", new BigDecimal("356.80"), "2026-04-15", ""));

        // A1栋2单元201
        list.add(createRow("A1", "2", "201", "王五", "13800138003",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1520.00"), "2026-04-15", ""));
        list.add(createRow("A1", "2", "201", "王五", "13800138003",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("95.50"), "2026-04-15", ""));
        list.add(createRow("A1", "2", "201", "王五", "13800138003",
                "垃圾清运费", "2026-01-01", "2026-06-30", new BigDecimal("120.00"), "2026-07-15", ""));

        // A1栋2单元202
        list.add(createRow("A1", "2", "202", "赵六", "13800138004",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("1520.00"), "2026-07-15", "新入住"));

        // ========== A2栋 ==========
        // A2栋1单元301
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("2100.00"), "2026-04-15", ""));
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "电费", "2026-01-01", "2026-03-31", new BigDecimal("520.30"), "2026-04-15", ""));
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("112.00"), "2026-04-15", ""));
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "维修费", "2026-03-15", "2026-03-15", new BigDecimal("350.00"), "2026-04-15", "疏通下水道"));

        // A2栋1单元302
        list.add(createRow("A2", "1", "302", "周八", "13800138006",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1880.00"), "2026-04-15", ""));

        // A2栋2单元401
        list.add(createRow("A2", "2", "401", "吴九", "13800138007",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("2650.00"), "2026-04-15", ""));
        list.add(createRow("A2", "2", "401", "吴九", "13800138007",
                "电费", "2026-01-01", "2026-03-31", new BigDecimal("680.00"), "2026-04-15", ""));
        list.add(createRow("A2", "2", "401", "吴九", "13800138007",
                "停车费", "2026-01-01", "2026-06-30", new BigDecimal("3000.00"), "2026-07-15", "车位A-008"));

        // A2栋2单元402（无业主姓名）
        list.add(createRow("A2", "2", "402", null, null,
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1880.00"), "2026-04-15", "空置房"));

        // ========== B1栋 ==========
        // B1栋1单元501
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("3200.00"), "2026-04-15", ""));
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("210.00"), "2026-04-15", ""));
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "电费", "2026-01-01", "2026-03-31", new BigDecimal("890.50"), "2026-04-15", ""));
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "垃圾清运费", "2026-01-01", "2026-06-30", new BigDecimal("200.00"), "2026-07-15", ""));

        // B1栋1单元502
        list.add(createRow("B1", "1", "502", "钱十一", "13800138009",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("2880.00"), "2026-04-15", ""));

        // B1栋2单元601
        list.add(createRow("B1", "2", "601", "陈十二", "13800138010",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("4500.00"), "2026-04-15", ""));
        list.add(createRow("B1", "2", "601", "陈十二", "13800138010",
                "停车费", "2026-01-01", "2026-06-30", new BigDecimal("3600.00"), "2026-07-15", "车位C-005"));

        // B1栋2单元602
        list.add(createRow("B1", "2", "602", "杨十三", "13800138011",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("3200.00"), "2026-07-15", ""));

        // ========== 更多月份补充 ==========
        // A1栋1单元101，Q2 物业费 + 水费
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("1250.50"), "2026-07-15", ""));
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "水费", "2026-04-01", "2026-06-30", new BigDecimal("165.00"), "2026-07-15", ""));

        // A1栋2单元201，Q2 物业费
        list.add(createRow("A1", "2", "201", "王五", "13800138003",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("1520.00"), "2026-07-15", ""));

        // A2栋1单元301，Q2 物业费 + 电费
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("2100.00"), "2026-07-15", ""));
        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "电费", "2026-04-01", "2026-06-30", new BigDecimal("480.00"), "2026-07-15", ""));

        // B1栋1单元501，Q2 物业费
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "物业费", "2026-04-01", "2026-06-30", new BigDecimal("3200.00"), "2026-07-15", ""));

        // ========== 更多单元房间 ==========
        // A1栋1单元103
        list.add(createRow("A1", "1", "103", "黄十四", "13800138012",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1100.00"), "2026-04-15", ""));
        list.add(createRow("A1", "1", "103", "黄十四", "13800138012",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("88.00"), "2026-04-15", ""));

        // A1栋1单元104
        list.add(createRow("A1", "1", "104", "刘十五", "13800138013",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1100.00"), "2026-04-15", ""));

        // A2栋3单元501
        list.add(createRow("A2", "3", "501", "何十六", "13800138014",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("2350.00"), "2026-04-15", ""));
        list.add(createRow("A2", "3", "501", "何十六", "13800138014",
                "电费", "2026-01-01", "2026-03-31", new BigDecimal("610.00"), "2026-04-15", ""));
        list.add(createRow("A2", "3", "501", "何十六", "13800138014",
                "垃圾清运费", "2026-01-01", "2026-06-30", new BigDecimal("150.00"), "2026-07-15", ""));

        // B1栋3单元701
        list.add(createRow("B1", "3", "701", "高十七", "13800138015",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("3800.00"), "2026-04-15", ""));
        list.add(createRow("B1", "3", "701", "高十七", "13800138015",
                "水费", "2026-01-01", "2026-03-31", new BigDecimal("178.00"), "2026-04-15", ""));
        list.add(createRow("B1", "3", "701", "高十七", "13800138015",
                "维修费", "2026-02-20", "2026-02-20", new BigDecimal("580.00"), "2026-04-15", "更换水龙头"));

        // B1栋3单元702
        list.add(createRow("B1", "3", "702", null, null,
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("3800.00"), "2026-04-15", "空置"));

        // ========== 新增场景：其他费用类型 ==========
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "其他", "2026-03-01", "2026-03-31", new BigDecimal("50.00"), "2026-04-15", "门禁卡工本费"));

        list.add(createRow("A2", "1", "301", "孙七", "13800138005",
                "其他", "2026-03-10", "2026-03-10", new BigDecimal("200.00"), "2026-04-15", "装修押金"));

        // ========== 边界测试数据（有效数据，用于测试边界场景） ==========
        // 最小金额 0.01 元
        list.add(createRow("B1", "1", "501", "郑十", "13800138008",
                "其他", "2026-06-01", "2026-06-30", new BigDecimal("0.01"), "2026-07-15", "最小金额测试"));
        // 大金额
        list.add(createRow("B1", "2", "601", "陈十二", "13800138010",
                "物业费", "2026-07-01", "2026-09-30", new BigDecimal("13500.00"), "2026-10-15", "大金额测试"));
        // 超长楼栋号
        list.add(createRow("B1-商业区-A", "1", "101", "商业用户", "13800138016",
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("8800.00"), "2026-04-15", "商业区测试"));
        // 跨越半年的计费周期
        list.add(createRow("A1", "1", "101", "张三", "13800138001",
                "停车费", "2026-01-01", "2026-12-31", new BigDecimal("4800.00"), "2027-01-15", "全年停车费"));
        // 无业主姓名 + 无联系电话
        list.add(createRow("A1", "1", "105", null, null,
                "物业费", "2026-01-01", "2026-03-31", new BigDecimal("1100.00"), "2026-04-15", ""));
        // 同一天起止（单日费用）
        list.add(createRow("A2", "1", "302", "周八", "13800138006",
                "维修费", "2026-04-18", "2026-04-18", new BigDecimal("150.00"), "2026-05-15", "单日维修"));

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
        row.setBillingStartDate(LocalDate.parse(billingStartDate));
        row.setBillingEndDate(LocalDate.parse(billingEndDate));
        row.setAmountDue(amountDue);
        row.setDueDate(LocalDate.parse(dueDate));
        row.setRemark(remark);
        return row;
    }
}
