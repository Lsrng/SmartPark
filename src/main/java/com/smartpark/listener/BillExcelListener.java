package com.smartpark.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.smartpark.pojo.dto.BillExcelRowDTO;
import com.smartpark.pojo.vo.ImportTaskVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EasyExcel 读取监听器
 * <p>
 * 流式读取 Excel 数据，每 500 行触发一次批量校验。
 * 校验通过的行加入 validRows 集合，校验失败的行加入 errorRows 集合。
 * </p>
 */
@Slf4j
public class BillExcelListener implements ReadListener<BillExcelRowDTO> {

    /** 每批处理的行数 */
    private static final int BATCH_SIZE = 500;

    /** 允许的费用类型枚举 */
    private static final Set<String> VALID_FEE_TYPES = new HashSet<>();

    static {
        VALID_FEE_TYPES.add("物业费");
        VALID_FEE_TYPES.add("水费");
        VALID_FEE_TYPES.add("电费");
        VALID_FEE_TYPES.add("停车费");
        VALID_FEE_TYPES.add("垃圾清运费");
        VALID_FEE_TYPES.add("维修费");
        VALID_FEE_TYPES.add("其他");
    }

    /** 当前批次缓冲行 */
    private final List<BillExcelRowDTO> buffer = new ArrayList<>();

    /** 校验通过的行 */
    @Getter
    private final List<BillExcelRowDTO> validRows = new ArrayList<>();

    /** 校验失败的行 */
    @Getter
    private final List<ImportTaskVO.RowError> errorRows = new ArrayList<>();

    /** 当前读取的行号（从 1 开始，含表头） */
    private int currentRowNum = 1;

    @Override
    public void invoke(BillExcelRowDTO row, AnalysisContext context) {
        currentRowNum++;
        buffer.add(row);

        // 达到批次阈值时执行校验
        if (buffer.size() >= BATCH_SIZE) {
            processBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余不足一批的数据
        if (!buffer.isEmpty()) {
            processBatch();
        }
        log.info("Excel 解析完成 - 有效数据: {} 行, 错误数据: {} 行", validRows.size(), errorRows.size());
    }

    /**
     * 处理当前批次的校验
     */
    private void processBatch() {
        // 当前批次的第一行在 Excel 中的实际行号
        // currentRowNum 已递增到下一个未读行的行号，buffer.size() 是本批次行数
        // 本批次第一行的行号 = currentRowNum - buffer.size() + 1
        int batchStartRow = currentRowNum - buffer.size() + 1;

        for (int i = 0; i < buffer.size(); i++) {
            BillExcelRowDTO row = buffer.get(i);
            int excelRowNum = batchStartRow + i;
            List<ImportTaskVO.RowError> errors = validateRow(row, excelRowNum);
            if (errors.isEmpty()) {
                validRows.add(row);
            } else {
                errorRows.addAll(errors);
            }
        }
        buffer.clear();
    }

    /**
     * 校验单行数据
     *
     * @param row         Excel 行数据
     * @param excelRowNum 该行在 Excel 中的实际行号（含表头，第 1 行为表头）
     * @return 该行的错误列表（无错误时返回空列表）
     */
    private List<ImportTaskVO.RowError> validateRow(BillExcelRowDTO row, int excelRowNum) {
        List<ImportTaskVO.RowError> errors = new ArrayList<>();

        // 1. 楼栋号
        if (isBlank(row.getBuildingNo())) {
            errors.add(buildError(excelRowNum, "buildingNo", null, "楼栋号不能为空"));
        } else if (row.getBuildingNo().length() > 20) {
            errors.add(buildError(excelRowNum, "buildingNo", row.getBuildingNo(), "楼栋号长度不能超过20个字符"));
        }

        // 2. 单元号
        if (isBlank(row.getUnitNo())) {
            errors.add(buildError(excelRowNum, "unitNo", null, "单元号不能为空"));
        } else if (row.getUnitNo().length() > 20) {
            errors.add(buildError(excelRowNum, "unitNo", row.getUnitNo(), "单元号长度不能超过20个字符"));
        }

        // 3. 房号
        if (isBlank(row.getRoomNo())) {
            errors.add(buildError(excelRowNum, "roomNo", null, "房号不能为空"));
        } else if (row.getRoomNo().length() > 20) {
            errors.add(buildError(excelRowNum, "roomNo", row.getRoomNo(), "房号长度不能超过20个字符"));
        }

        // 4. 业主姓名（非必填，填了才校验长度）
        if (row.getOwnerName() != null && row.getOwnerName().length() > 100) {
            errors.add(buildError(excelRowNum, "ownerName", row.getOwnerName(), "业主姓名长度不能超过100个字符"));
        }

        // 5. 联系电话（非必填，填了才校验格式）
        if (!isBlank(row.getPhone())) {
            String phone = row.getPhone().trim();
            if (!phone.matches("^1\\d{10}$")) {
                errors.add(buildError(excelRowNum, "phone", row.getPhone(), "联系电话格式不正确，必须为11位数字且以1开头"));
            }
        }

        // 6. 费用类型
        if (isBlank(row.getFeeType())) {
            errors.add(buildError(excelRowNum, "feeType", null, "费用类型不能为空"));
        } else {
            String feeType = row.getFeeType().trim();
            if (!VALID_FEE_TYPES.contains(feeType)) {
                errors.add(buildError(excelRowNum, "feeType", row.getFeeType(),
                        "费用类型无效，可选值：" + String.join("、", VALID_FEE_TYPES)));
            }
        }

        // 7. 计费起始日期
        if (row.getBillingStartDate() == null) {
            errors.add(buildError(excelRowNum, "billingStartDate", null, "计费起始日期不能为空或格式不正确（应为 yyyy-MM-dd）"));
        }

        // 8. 计费截止日期
        if (row.getBillingEndDate() == null) {
            errors.add(buildError(excelRowNum, "billingEndDate", null, "计费截止日期不能为空或格式不正确（应为 yyyy-MM-dd）"));
        }

        // 9. 日期范围校验（两个日期都非空时）
        if (row.getBillingStartDate() != null && row.getBillingEndDate() != null) {
            if (row.getBillingEndDate().isBefore(row.getBillingStartDate())) {
                errors.add(buildError(excelRowNum, "billingEndDate",
                        row.getBillingEndDate().toString(), "计费截止日期不能早于计费起始日期"));
            }
        }

        // 10. 应收金额
        if (row.getAmountDue() == null) {
            errors.add(buildError(excelRowNum, "amountDue", null, "应收金额不能为空"));
        } else if (row.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(buildError(excelRowNum, "amountDue", row.getAmountDue().toString(), "应收金额必须大于0"));
        } else if (row.getAmountDue().scale() > 2) {
            errors.add(buildError(excelRowNum, "amountDue", row.getAmountDue().toString(), "应收金额最多保留两位小数"));
        }

        // 11. 缴费截止日期
        if (row.getDueDate() == null) {
            errors.add(buildError(excelRowNum, "dueDate", null, "缴费截止日期不能为空或格式不正确（应为 yyyy-MM-dd）"));
        }

        // 12. 缴费截止日期 >= 计费截止日期
        if (row.getDueDate() != null && row.getBillingEndDate() != null) {
            if (row.getDueDate().isBefore(row.getBillingEndDate())) {
                errors.add(buildError(excelRowNum, "dueDate",
                        row.getDueDate().toString(), "缴费截止日期不能早于计费截止日期"));
            }
        }

        // 13. 备注（非必填，填了才校验长度）
        if (row.getRemark() != null && row.getRemark().length() > 500) {
            errors.add(buildError(excelRowNum, "remark", row.getRemark(), "备注长度不能超过500个字符"));
        }

        return errors;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private ImportTaskVO.RowError buildError(int row, String field, String value, String reason) {
        return ImportTaskVO.RowError.builder()
                .row(row)
                .field(field)
                .value(value)
                .reason(reason)
                .build();
    }
}
