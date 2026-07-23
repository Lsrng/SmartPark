package com.smartpark.controller;

import com.smartpark.common.result.Result;
import com.smartpark.pojo.vo.ImportTaskVO;
import com.smartpark.service.BillImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

/**
 * 物业账单导入接口
 */
@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "物业账单导入", description = "物业账单 Excel 导入相关接口")
public class BillImportController {

    private final BillImportService billImportService;

    @PostMapping("/import")
    @Operation(summary = "上传并导入物业账单", description = "上传 Excel 文件，创建异步导入任务，返回 taskId")
    public Result<ImportTaskVO> importBills(
            @Parameter(description = "Excel 文件（.xlsx 格式）")
            @RequestParam("file") MultipartFile file) {

        String taskId = billImportService.createImportTask(file);

        ImportTaskVO vo = ImportTaskVO.builder()
                .taskId(taskId)
                .status("PENDING")
                .totalRows(0)
                .successRows(0)
                .failRows(0)
                .failDetail(Collections.emptyList())
                .costTimeMs(0L)
                .build();

        return Result.success("导入任务已创建", vo);
    }

    @GetMapping("/import/task/{taskId}")
    @Operation(summary = "查询导入任务状态", description = "根据 taskId 查询导入任务的当前状态和处理结果")
    public Result<ImportTaskVO> getTaskStatus(
            @Parameter(description = "任务唯一标识")
            @PathVariable("taskId") String taskId) {

        ImportTaskVO vo = billImportService.getTaskStatus(taskId);
        if (vo == null) {
            return Result.error(404, "导入任务不存在");
        }

        // 根据状态和失败行数拼接提示消息
        String msg = buildStatusMessage(vo);
        return Result.success(msg, vo);
    }

    /**
     * 根据任务状态构建提示消息
     */
    private String buildStatusMessage(ImportTaskVO vo) {
        return switch (vo.getStatus()) {
            case "PENDING" -> "任务正在排队等待处理";
            case "PROCESSING" -> "任务正在处理中，请稍候";
            case "SUCCESS" -> {
                if (vo.getFailRows() != null && vo.getFailRows() > 0) {
                    yield "导入完成，成功 " + vo.getSuccessRows() + " 条，失败 " + vo.getFailRows() + " 条";
                } else {
                    yield "导入完成，共导入 " + vo.getSuccessRows() + " 条数据";
                }
            }
            case "FAIL" -> "导入失败，请查看错误详情后重新上传";
            default -> "未知状态";
        };
    }
}
