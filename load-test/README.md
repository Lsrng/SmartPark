# 物业账单导入 JMeter 压测指南（带 JWT 认证）

## 一、文件清单

```
load-test/
├── generate_excel.py              # Excel 测试数据生成脚本
├── bill-import-test.jmx           # JMeter 测试计划文件（带登录+认证）
├── bill-files.csv                 # JMeter CSV 数据配置
├── README.md                      # 本说明文档
└── test-files/                    # 测试数据目录（已生成）
    ├── bill-1k-50kb.xlsx          # 1,000 行 / 71KB
    ├── bill-5k-250kb.xlsx         # 5,000 行 / 334KB
    ├── bill-10k-500kb.xlsx        # 10,000 行 / 662KB
    ├── bill-20k-1mb.xlsx          # 20,000 行 / 1.3MB
    ├── bill-50k-2.5mb.xlsx        # 50,000 行 / 3.3MB
    ├── bill-100k-5mb.xlsx         # 100,000 行 / 6.6MB（边界）
    ├── bill-oversize-6mb.xlsx     # 120,000 行 / 7.9MB（超限）
    ├── bill-tiny-100rows.xlsx    # 100 行 / 12KB（小文件）
    └── bill-empty.xlsx            # 空文件（仅表头）
```

## 二、认证流程说明

本测试计划使用 **JWT 认证**，流程如下：

```
线程组0 (登录) → POST /auth/login → 获取 token → 存入全局变量 authToken
线程组1~4 (压测) → HTTP Header 中自动携带 token: ${authToken}
```

- **登录账号**: `cxk` / `1234`
- **Token 请求头名称**: `token`（来自 `application.yml` 配置 `jwt.token-name: token`）
- **Token 提取路径**: `data.token`（登录成功响应 `Result.data.token`）
- **线程组执行顺序**: 登录线程组最先执行（`serialize_threadgroups=true`），确保 Token 获取后再执行压测

## 三、环境准备

### 3.1 安装 JMeter

1. 下载 JMeter 5.6+：https://jmeter.apache.org/download_jmeter.cgi
2. 解压到任意目录（如 `D:/apache-jmeter-5.6.3`）
3. 可选插件安装（推荐）：
   ```
   下载 JMeter Plugins Manager:
   https://jmeter-plugins.org/downloads/
   将 jmeter-plugins-manager-x.x.x.jar 放入 JMeter/lib/ext/ 目录
   启动 JMeter → Options → Plugins Manager → 安装:
   - Custom Thread Groups
   - PerMon Plugin
   ```

### 3.2 启动被测应用

```bash
cd smartPark
mvn spring-boot:run
# 或
java -jar target/smartPark-0.0.1-SNAPSHOT.jar
```

### 3.3 可选：开启 JMX 监控（用于 PerMon 插件）

```bash
java -Dcom.sun.management.jmxremote ^
     -Dcom.sun.management.jmxremote.port=9999 ^
     -Dcom.sun.management.jmxremote.authenticate=false ^
     -Dcom.sun.management.jmxremote.ssl=false ^
     -jar target/smartPark-0.0.1-SNAPSHOT.jar
```

## 四、执行压测

### 4.1 配置检查

打开 `bill-import-test.jmx`，确认以下内容：

1. **服务器地址**：HTTP Request Defaults 中 `localhost:8080` 正确
2. **登录账号**：线程组0 中的 `username=cxk`, `password=1234` 正确
3. **文件路径**：CSV 使用绝对路径，JMeter 能访问到

### 4.2 方式一：JMeter GUI 模式（调试用）

```
双击 JMeter 启动脚本:
  Windows: bin/jmeter.bat
  Linux:   bin/jmeter.sh

打开后:
  1. File → Open → 选择 bill-import-test.jmx
  2. 【重要】先只运行 "0-登录获取Token" 线程组，确认 Token 获取成功
     - 右键 "0-登录获取Token" → Enable
     - 右键其他线程组 → Disable
     - 点击绿色三角启动
     - 查看 View Results Tree → 确认 token 提取成功
  3. 启用需要的压测线程组，运行压测
  4. 查看 Summary Report
```

**首次运行必做验证**：先单独跑登录线程组，在 View Results Tree 中检查：
- 登录响应码为 200
- JSR223 脚本日志中出现 "Token saved:"
- 如果报 "Token not found"，检查登录响应结构是否匹配

### 4.3 方式二：命令行模式（正式压测）

```bash
# Windows
cd D:\apache-jmeter-5.6.3\bin

jmeter.bat -n ^
  -t E:\后端\项目练习\smartPark\load-test\bill-import-test.jmx ^
  -l E:\后端\项目练习\smartPark\load-test\results\result.jtl ^
  -e -o E:\后端\项目练习\smartPark\load-test\results\report

参数说明:
  -n: 非 GUI 模式
  -t: 测试计划文件路径
  -l: 结果日志文件路径
  -e: 测试结束后自动生成 HTML 报告
  -o: HTML 报告输出目录
```

### 4.4 单独运行某个线程组

JMeter 支持通过命令行参数禁用/启用线程组：

```bash
# 只运行线程组1（基础验证）
jmeter.bat -n -t bill-import-test.jmx -l result.jtl ^
  -Jthreads0=true -Jthreads1=true -Jthreads2=false -Jthreads3=false -Jthreads4=false
```

注意：由于 `serialize_threadgroups=true`，所有启用的线程组按顺序执行。

## 五、压测场景说明

| 线程组 | 目的 | 配置 | 关注指标 |
|--------|------|------|----------|
| **线程组0** | 登录获取Token | 1线程 × 1循环 | 登录成功率 |
| **线程组1** | 基础功能验证 | 10线程 × 10循环 | 成功率、响应时间 |
| **线程组2** | 端到端完成时间 | 5线程 × 5循环 + 轮询 | 任务完成耗时、成功率 |
| **线程组3** | 阶梯加压找拐点 | 100线程, 600s, 斜坡30s | QPS、CPU、堆使用 |
| **线程组4** | 极限压力(内存安全) | 8线程 × 3循环 × 10万行 | 堆峰值、Full GC |

## 六、结果分析

### 6.1 HTML 报告

命令行模式完成后，在 `results/report/` 目录下打开 `index.html`：

- **Summary Report**: 查看各接口的平均响应时间、P90/P95/P99
- **Aggregate Report**: 多线程组聚合对比
- **Response Time Graph**: 响应时间趋势图
- **Throughput**: 吞吐量趋势图

### 6.2 JVM 监控（命令行配合）

```bash
# 查看 GC 统计
jstat -gcutil <pid> 1000

# 查看堆内存详情
jmap -heap <pid>

# 查看对象分布
jmap -histo:live <pid> | findstr BillExcel

# 查看线程状态
jstack <pid> | findstr bill-import
```

### 6.3 关键指标合格标准

| 指标 | 合格标准 | 危险阈值 |
|------|----------|----------|
| 接口响应成功率 | ≥ 99% | < 95% |
| 平均响应时间（创建任务） | < 500ms | > 2s |
| 单任务完成时间（1万行） | < 5s | > 15s |
| 堆内存使用率 | < 60% | > 75% |
| Full GC 次数 | 0 | > 0 |
| 线程池活跃线程 | < maxPoolSize | = maxPoolSize 持续 |

## 七、重新生成测试数据

如果需要不同规模或内容的测试数据：

```bash
cd smartPark/load-test

# 默认生成所有规模
python generate_excel.py

# 指定输出目录
python generate_excel.py D:/my-test-data

# 依赖安装（如果未安装）
pip install openpyxl
```

修改 `generate_excel.py` 中的 `configs` 列表可添加新规模。

## 八、常见问题

**Q: JMeter 报告中登录失败？**
A: 检查账号密码是否正确。确认用户 `cxk` 在数据库中存在且密码为 `1234`。如果使用 BCrypt 加密存储密码，需要在登录 Service 中确认密码校验逻辑。

**Q: 上传接口返回 401？**
A: 
1. 检查日志中是否有 "Token saved" 输出，确认 Token 提取成功
2. 检查 HTTP Header Manager 中 Token header 名称是否为 `token`（不是 `Authorization`）
3. 确认 Token 未过期（默认有效期见 `application.yml`）

**Q: Token 提取路径不对？**
A: 登录响应格式为 `{"code":200,"msg":"登录成功","data":{"token":"xxx","tokenName":"token","user":{...}}}`。如果你的 UserService 返回结构不同，修改 JSR223 脚本中的 `json.data.token` 路径。

**Q: 测试文件超过 5MB 怎么办？**
A: `bill-oversize-6mb.xlsx` 是故意生成的超限文件，用于测试系统的拒绝能力。正常压测只使用 ≤ 5MB 的文件。

**Q: 如何验证异步方案比同步快 3 倍？**
A: 在线程组2中端到端测量完成时间，与同步接口（如果有的话）对比每分钟完成任务数。如果没有同步接口，可临时把 `executeImport` 方法改为同步调用做对比测试。