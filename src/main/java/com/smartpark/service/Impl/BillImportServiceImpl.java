package com.smartpark.service.Impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartpark.common.context.BaseContext;
import com.smartpark.listener.BillExcelListener;
import com.smartpark.mapper.PropertyBillImportRecordMapper;
import com.smartpark.pojo.dto.BillExcelRowDTO;
import com.smartpark.pojo.entity.PropertyBill;
import com.smartpark.pojo.entity.PropertyBillImportRecord;
import com.smartpark.pojo.vo.ImportTaskVO;
import com.smartpark.service.BillImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * 物业账单导入服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillImportServiceImpl implements BillImportService {

    private final PropertyBillImportRecordMapper importRecordMapper;
    private final ThreadPoolTaskExecutor billImportExecutor;
    private final SqlSessionFactory sqlSessionFactory;

    /** 允许的最大文件大小：5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    /** 允许的文件扩展名 */
    private static final String ALLOWED_EXTENSION = ".xlsx";

    @Override
    public String createImportTask(MultipartFile file) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成 taskId，保存文件到临时目录
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = file.getOriginalFilename();
        String tempFileName = taskId + "_" + (originalFilename != null ? originalFilename : "unknown.xlsx");
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "bill-import");
        Path tempFile = tempDir.resolve(tempFileName);

        try {
            Files.createDirectories(tempDir);
            file.transferTo(tempFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }

        // 3. 创建导入记录
        PropertyBillImportRecord record = PropertyBillImportRecord.builder()
                .taskId(taskId)
                .fileName(originalFilename)
                .status("PENDING")
                .totalRows(0)
                .successRows(0)
                .failRows(0)
                .createdBy(BaseContext.getCurrentId())
                .build();
        importRecordMapper.insert(record);

        // 4. 提交到线程池异步执行
        Long recordId = record.getId();
        billImportExecutor.submit(() -> executeImport(recordId, tempFile.toString()));

        log.info("导入任务已创建 - taskId: {}, fileName: {}", taskId, originalFilename);
        return taskId;
    }

    @Override
    public ImportTaskVO getTaskStatus(String taskId) {
        PropertyBillImportRecord record = importRecordMapper.selectOne(
                new LambdaQueryWrapper<PropertyBillImportRecord>()
                        .eq(PropertyBillImportRecord::getTaskId, taskId));

        if (record == null) {
            return null;
        }

        ImportTaskVO.ImportTaskVOBuilder builder = ImportTaskVO.builder()
                .taskId(record.getTaskId())
                .status(record.getStatus())
                .totalRows(record.getTotalRows())
                .successRows(record.getSuccessRows())
                .failRows(record.getFailRows())
                .costTimeMs(record.getCostTimeMs());

        // 解析 failDetail JSON
        if (record.getFailDetail() != null && !record.getFailDetail().isEmpty()) {
            List<ImportTaskVO.RowError> failDetail = JSON.parseArray(
                    record.getFailDetail(), ImportTaskVO.RowError.class);
            builder.failDetail(failDetail);
        }

        return builder.build();
    }

    /**
     * 异步执行导入（由线程池 Worker 调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeImport(Long recordId, String filePath) {
        long startTime = System.currentTimeMillis();
        File excelFile = new File(filePath);

        try {
            log.info("导入任务开始执行 - recordId: {}", recordId);

            // 1. 更新状态为 PROCESSING
            updateRecordStatus(recordId, "PROCESSING", null, null, null, null, null);

            // 2. 创建监听器并读取 Excel
            BillExcelListener listener = new BillExcelListener();
            EasyExcel.read(excelFile, BillExcelRowDTO.class, listener).sheet().doRead();

            // 3. 获取校验结果
            List<BillExcelRowDTO> validRows = listener.getValidRows();
            List<ImportTaskVO.RowError> errorRows = listener.getErrorRows();
            int totalRows = validRows.size() + errorRows.size();

            log.info("Excel 解析完成 - 有效: {} 行, 无效: {} 行", validRows.size(), errorRows.size());

            // 4. 将有效的行分批写入数据库
            int successRows = 0;
            if (!validRows.isEmpty()) {
                successRows = batchInsertBills(validRows);
            }

            // 5. 构建 failDetail JSON
            String failDetailJson = null;
            int failRows = errorRows.size();
            if (!errorRows.isEmpty()) {
                failDetailJson = JSON.toJSONString(errorRows);
            }

            // 6. 更新导入记录为 SUCCESS
            long costTime = System.currentTimeMillis() - startTime;
            updateRecordStatus(recordId, "SUCCESS", totalRows, successRows, failRows, failDetailJson, costTime);

            log.info("导入任务执行完成 - recordId: {}, 成功: {} 条, 失败: {} 条, 耗时: {}ms",
                    recordId, successRows, failRows, costTime);

        } catch (Exception e) {
            log.error("导入任务执行异常 - recordId: {}", recordId, e);

            // 更新状态为 FAIL
            long costTime = System.currentTimeMillis() - startTime;
            String errorDetail = JSON.toJSONString(List.of(
                    ImportTaskVO.RowError.builder()
                            .row(0)
                            .field("system")
                            .value(null)
                            .reason("系统处理异常: " + e.getMessage())
                            .build()
            ));

            try {
                updateRecordStatus(recordId, "FAIL", null, null, null, errorDetail, costTime);
            } catch (Exception ex) {
                log.error("更新导入记录状态失败 - recordId: {}", recordId, ex);
            }

        } finally {
            // 7. 清理临时文件
            if (excelFile.exists()) {
                boolean deleted = excelFile.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", filePath);
                }
            }
        }
    }

    /**
     * 分批入库（使用 MyBatis 批量执行器）
     */
    private int batchInsertBills(List<BillExcelRowDTO> rows) {
        Long currentUserId = BaseContext.getCurrentId();
        int inserted = 0;

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            com.smartpark.mapper.PropertyBillMapper batchMapper =
                    sqlSession.getMapper(com.smartpark.mapper.PropertyBillMapper.class);

            for (BillExcelRowDTO row : rows) {
                PropertyBill bill = buildPropertyBill(row, currentUserId);
                batchMapper.insert(bill);
                inserted++;
            }

            sqlSession.commit();
        }

        return inserted;
    }

    /**
     * 将 Excel 行数据转换为实体
     */
    private PropertyBill buildPropertyBill(BillExcelRowDTO row, Long createdBy) {
        return PropertyBill.builder()
                .buildingNo(row.getBuildingNo().trim())
                .unitNo(row.getUnitNo().trim())
                .roomNo(row.getRoomNo().trim())
                .ownerName(row.getOwnerName() != null ? row.getOwnerName().trim() : null)
                .phone(row.getPhone() != null ? row.getPhone().trim() : null)
                .feeType(row.getFeeType().trim())
                .billingStartDate(row.getBillingStartDate())
                .billingEndDate(row.getBillingEndDate())
                .amountDue(row.getAmountDue())
                .amountPaid(BigDecimal.ZERO)
                .dueDate(row.getDueDate())
                .status("UNPAID")
                .remark(row.getRemark() != null ? row.getRemark().trim() : null)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 更新导入记录状态
     */
    private void updateRecordStatus(Long recordId, String status, Integer totalRows,
                                    Integer successRows, Integer failRows,
                                    String failDetail, Long costTimeMs) {
        PropertyBillImportRecord record = new PropertyBillImportRecord();
        record.setId(recordId);
        record.setStatus(status);
        if (totalRows != null) record.setTotalRows(totalRows);
        if (successRows != null) record.setSuccessRows(successRows);
        if (failRows != null) record.setFailRows(failRows);
        if (failDetail != null) record.setFailDetail(failDetail);
        if (costTimeMs != null) record.setCostTimeMs(costTimeMs);
        importRecordMapper.updateById(record);
    }

    /**
     * 校验上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大 5MB），当前文件大小: " +
                    (file.getSize() / 1024 / 1024) + "MB");
        }

        // 文件后缀校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
            throw new IllegalArgumentException("仅支持 .xlsx 格式的 Excel 文件");
        }
    }
}
