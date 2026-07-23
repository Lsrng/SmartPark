package com.smartpark.service;

import com.smartpark.pojo.vo.ImportTaskVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 物业账单导入服务接口
 */
public interface BillImportService {

    /**
     * 创建导入任务并提交到线程池
     *
     * @param file 上传的 Excel 文件
     * @return 任务唯一标识（taskId）
     */
    String createImportTask(MultipartFile file);

    /**
     * 查询导入任务状态
     *
     * @param taskId 任务唯一标识
     * @return 导入任务状态视图
     */
    ImportTaskVO getTaskStatus(String taskId);
}
