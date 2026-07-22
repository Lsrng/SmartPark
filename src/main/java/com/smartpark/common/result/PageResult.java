package com.smartpark.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应结果
 *
 * @param <T> 当前页每条记录的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private long total;       // 总记录数

    private List<T> records;  // 当前页数据集合
}
