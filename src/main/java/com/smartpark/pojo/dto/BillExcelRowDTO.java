package com.smartpark.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(name = "BillExcelRowDTO", description = "物业账单Excel行数据")
public class BillExcelRowDTO {

    @ExcelProperty("楼栋号")
    @Schema(description = "楼栋号")
    private String buildingNo;

    @ExcelProperty("单元号")
    @Schema(description = "单元号")
    private String unitNo;

    @ExcelProperty("房号")
    @Schema(description = "房号")
    private String roomNo;

    @ExcelProperty("业主姓名")
    @Schema(description = "业主姓名")
    private String ownerName;

    @ExcelProperty("联系电话")
    @Schema(description = "联系电话")
    private String phone;

    @ExcelProperty("费用类型")
    @Schema(description = "费用类型")
    private String feeType;

    @ExcelProperty("计费起始日期")
    @Schema(description = "计费起始日期")
    private LocalDate billingStartDate;

    @ExcelProperty("计费截止日期")
    @Schema(description = "计费截止日期")
    private LocalDate billingEndDate;

    @ExcelProperty("应收金额")
    @Schema(description = "应收金额")
    private BigDecimal amountDue;

    @ExcelProperty("缴费截止日期")
    @Schema(description = "缴费截止日期")
    private LocalDate dueDate;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;
}
