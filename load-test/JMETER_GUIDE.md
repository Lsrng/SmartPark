# JMeter 压测实操流程

> 面向本项目（物业账单导入功能）的 JMeter 完整使用指南，从零开始到跑通压测。

---

## 快速目录

1. [下载安装 JMeter](#1-下载安装-jmeter)
2. [启动 Spring Boot 应用](#2-启动-spring-boot-应用)
3. [启动 JMeter GUI](#3-启动-jmeter-gui)
4. [打开测试计划](#4-打开测试计划)
5. [验证登录认证](#5-验证登录认证必须做)
6. [运行压测](#6-运行压测)
7. [查看结果](#7-查看结果)
8. [附录：常见问题排查](#8-附录常见问题排查)

---

## 1. 下载安装 JMeter

你的环境已具备 JDK 17（JMeter 需要 Java 8+），直接下载安装：

**步骤：**

1. 打开官网：[https://jmeter.apache.org/download_jmeter.cgi](https://jmeter.apache.org/download_jmeter.cgi)
2. 在 **Binaries** 区域下载 `apache-jmeter-5.6.3.zip`（约 80MB）
3. 解压到无中文路径的目录，例如 `D:\apache-jmeter-5.6.3`

```
解压后的目录结构：
D:\apache-jmeter-5.6.3\
├── bin\
│   ├── jmeter.bat    ← Windows 启动入口
│   └── jmeter.sh     ← Linux/Mac 启动入口
├── lib\
├── docs\
└── ...
```

> ⚠️ **重要**：不要解压到含中文或空格的路径，例如 j`D:\工具\JMeter\`，否则可能启动报错。

**可选：安装插件（推荐）**

下载 JMeter Plugins Manager：[https://jmeter-plugins.org/downloads/](https://jmeter-plugins.org/downloads/)

将 `jmeter-plugins-manager-x.x.x.jar` 放入 `JMeter/lib/ext/` 目录，然后在 JMeter 中：
`Options → Plugins Manager` 安装：
- **PerMon Plugin**：监控 JVM 内存、CPU 等
- **Custom Thread Groups**：更灵活的线程控制

---

## 2. 启动 Spring Boot 应用

打开新的命令行窗口：

```powershell
cd e:\后端\项目练习\smartPark
mvn spring-boot:run
```

确保应用在 `http://localhost:8080` 正常运行。可在浏览器访问：
- `http://localhost:8080/doc.html`（Swagger 文档）

---

## 3. 启动 JMeter GUI

双击 `D:\apache-jmeter-5.6.3\bin\jmeter.bat`

首次启动可能需要几秒，界面如下：

```
┌─────────────────────────────────────────────────────┐
│  Apache JMeter                                      │
│                                                     │
│  左侧：测试计划树                                    │
│  ├── Test Plan                                      │
│  │   ├── Thread Group                               │
│  │   │   ├── HTTP Request                           │
│  │   │   └── View Results Tree                      │
│  │   └── ...                                        │
│                                                     │
│  右侧：配置面板                                     │
└─────────────────────────────────────────────────────┘
```

如需中文界面，修改 `bin/jmeter.properties`：
```properties
language=zh_CN
```

---

## 4. 打开测试计划

在 JMeter 中操作：

```
菜单栏 → File → Open
```

选择文件：
```
e:\后端\项目练习\smartPark\load-test\bill-import-test.jmx
```

打开后左侧树结构如下：

```
物业账单导入压测计划(带认证)
│
├── 【配置元件】
│   ├── 用户定义变量（authToken 占位）
│   ├── HTTP Request Defaults（localhost:8080）
│   └── CSV Data Set Config（测试文件列表）
│
├── 【线程组0】0-登录获取Token              ← 登录线程组
│   ├── HTTP Header Manager (JSON)
│   ├── POST /auth/login
│   ├── 响应断言-200
│   └── JSR223-提取并保存Token
│
├── 【线程组1】线程组1-基础能力验证
│   ├── HTTP Header Manager (带Token)
│   ├── POST /api/bill/import
│   ├── 响应断言-上传成功
│   └── JSR223-提取taskId
│
├── 【线程组2】线程组2-端到端完成时间
│   ├── HTTP Header Manager (带Token)
│   ├── STEP1-上传Excel创建任务
│   ├── STEP2-轮询任务状态（60次，500ms间隔）
│
├── 【线程组3】线程组3-阶梯加压(找拐点)
│   ├── HTTP Header Manager (带Token)
│   └── POST /api/bill/import - 阶梯加压
│
├── 【线程组4】线程组4-极限压力(10万行)
│   ├── HTTP Header Manager (带Token)
│   └── POST /api/bill/import - 10万行极限
│
└── 【监听器】
    ├── 聚合报告-Summary Report
    ├── 查看结果树(View Results Tree)
    └── CSV 日志输出
```

---

## 5. 验证登录认证（必须做！）

**这一步确保 Token 获取成功，否则后续所有请求都会返回 401。**

### 步骤：

**1. 只启用登录线程组**

在左侧树中，右键点击以下线程组 → **Disable**（禁用，灰色变灰）：
- `线程组1-基础能力验证`
- `线程组2-端到端完成时间`
- `线程组3-阶梯加压`
- `线程组4-极限压力`

**保留启用**：只有 `0-登录获取Token`

**2. 运行**

点击顶部工具栏的 **▶ 绿色三角按钮**（或按快捷键 `Ctrl+R`）

**3. 查看结果**

点击左侧 **`查看结果树(View Results Tree)`**，应该看到：

| 样本 | 状态 | 说明 |
|------|------|------|
| `POST /auth/login` | ✅ 200 OK | 登录成功 |
| `JSR223-提取并保存Token` | ✅ | 日志显示 `Token saved: eyJ...` |

**如果看到 `Token saved`** → 登录验证通过，继续下一步。

**如果失败** → 检查 [附录：常见问题排查](#8-附录常见问题排查)

---

## 6. 运行压测

### 方案 A：基础能力验证（推荐先跑）

**场景**：10 个并发用户，每个用户上传 10 次，验证接口正常。

**配置**：
- 启用：`0-登录获取Token` + `线程组1-基础能力验证`
- 禁用：`线程组2`、`线程组3`、`线程组4`

点击 **▶ 运行**

### 方案 B：端到端完成时间测试

**场景**：测试从上传 Excel 到异步处理完成的总耗时。

**配置**：
- 启用：`0-登录获取Token` + `线程组2-端到端完成时间`
- 禁用：`线程组1`、`线程组3`、`线程组4`

**说明**：该线程组会自动轮询任务状态（每 500ms 一次，最多 60 次），直到状态变为 SUCCESS 或 FAIL。

### 方案 C：极限压力测试（内存安全验证）

**场景**：8 个线程同时上传 10 万行 Excel 文件，验证内存不溢出。

**配置**：
- 启用：`0-登录获取Token` + `线程组4-极限压力(10万行)`
- 禁用：其他全部

**说明**：这是面试重点场景，验证自定义线程池 + 异步批量导入方案在高负载下的稳定性。

### 方案 D：全部运行（综合测试）

**配置**：所有线程组全部启用

**注意**：由于 `serialize_threadgroups=true`，线程组按 0→1→2→3→4 顺序依次执行。

---

## 7. 查看结果

### GUI 模式（调试用）

运行完成后，点击左侧 **`聚合报告-Summary Report`**，查看关键指标：

| 指标 | 含义 | 合格标准 |
|------|------|---------|
| **# Samples** | 总请求数 | — |
| **Average** | 平均响应时间 | < 500ms |
| **Min** | 最快响应时间 | — |
| **Max** | 最慢响应时间 | — |
| **Error %** | 错误率 | < 1% |
| **Throughput** | 吞吐量（请求/秒） | 越高越好 |
| **Received KB/sec** | 接收速率 | — |

### 命令行模式（正式压测，生成 HTML 报告）

打开命令行：

```powershell
cd D:\apache-jmeter-5.6.3\bin

jmeter.bat -n `
  -t e:\后端\项目练习\smartPark\load-test\bill-import-test.jmx `
  -l e:\后端\项目练习\smartPark\load-test\results\result.jtl `
  -e -o e:\后端\项目练习\smartPark\load-test\results\report
```

**参数说明**：
- `-n`：非 GUI 模式（无头执行）
- `-t`：测试计划文件路径
- `-l`：结果日志文件（JTL 格式）
- `-e`：测试结束后自动生成 HTML 报告
- `-o`：HTML 报告输出目录

运行完成后，在浏览器打开 `results\report\index.html`，可以看到：
- 📊 响应时间趋势图（Response Time Graph）
- 📊 吞吐量趋势图（Throughput）
- 📊 错误统计
- 📋 各接口详细指标

### JVM 监控辅助

压测过程中，可以用以下命令监控 Java 进程：

```powershell
# 找到 Java 进程 PID
jps -l

# 查看 GC 统计（每秒刷新）
jstat -gcutil <pid> 1000

# 查看堆内存详情
jmap -heap <pid>

# 查看对象分布（关注 BillExcelRowDTO 数量）
jmap -histo:live <pid> | findstr BillExcel

# 查看线程池状态
jstack <pid> | findstr bill-import
```

---

## 8. 附录：常见问题排查

### Q1：登录返回 401 或用户名密码错误？

**排查步骤**：
1. 确认数据库中 `cxk` 用户存在，密码为 `1234`（明文存储）
2. 如果密码使用了 BCrypt 加密，需要修改 [UserServiceImpl.java:38](file:///e:/后端/项目练习/smartPark/src/main/java/com/smartpark/service/Impl/UserServiceImpl.java#L38) 的密码校验逻辑
3. 查看 JMeter `查看结果树` 中的登录响应详情

### Q2：上传接口返回 401？

**排查步骤**：
1. 确认登录线程组已执行成功（查看结果树中 Token 是否提取成功）
2. 检查 HTTP Header Manager 中 header 名称是否为 `token`（不是 `Authorization`）
3. 检查 Token 是否已过期（默认有效期见 `application.yml` 中的 `jwt.expiration`）

### Q3：上传失败，提示"文件大小超过限制"？

**原因**：`bill-oversize-6mb.xlsx` 是故意生成的超限文件（8MB > 5MB 限制）

**解决**：换用 ≤ 5MB 的测试文件：
- `bill-1k-50kb.xlsx`
- `bill-5k-250kb.xlsx`
- `bill-10k-500kb.xlsx`
- `bill-20k-1mb.xlsx`
- `bill-50k-2.5mb.xlsx`
- `bill-100k-5mb.xlsx`

### Q4：JMeter 启动报错 "Could not open ..."？

**原因**：解压路径包含中文或空格

**解决**：重新解压到纯英文路径，如 `D:\apache-jmeter-5.6.3`

### Q5：测试文件找不到？

**排查步骤**：
1. 确认文件路径使用**绝对路径**（已在 CSV 中配置）
2. 检查文件是否实际存在于 `test-files/` 目录
3. 如果 JMeter 仍找不到，手动在 `bill-files.csv` 中修改路径

### Q6：10 万行文件只有 6.7MB，符合 5MB 限制吗？

**不完全符合**：`bill-100k-5mb.xlsx` 实际大小为 6.7MB，会被文件大小校验拦截。

**解决方案**（二选一）：
- **方案 A**：使用 `bill-50k-2.5mb.xlsx`（50,000 行，3.3MB）做高压测试
- **方案 B**：临时修改 [BillImportServiceImpl.java:45](file:///e:/后端/项目练习/smartPark/src/main/java/com/smartpark/service/Impl/BillImportServiceImpl.java#L45) 的 `MAX_FILE_SIZE` 为 10MB 进行测试

### Q7：如何单独运行某个线程组？

**GUI 方式**：右键线程组 → Enable/Disable

**命令行方式**：
```powershell
# 只运行线程组1（基础验证）
jmeter.bat -n -t bill-import-test.jmx -l result.jtl ^
  -Jthreads0=true -Jthreads1=true -Jthreads2=false -Jthreads3=false -Jthreads4=false
```

---

## 快速流程图

```
┌──────────────────────────┐
│ 1. 下载解压 JMeter        │
│    jmeter.apache.org      │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 2. 启动 Spring Boot       │
│    mvn spring-boot:run    │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 3. 双击 jmeter.bat        │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 4. File → Open → .jmx     │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 5. 只跑登录线程组          │
│    → 验证 Token 获取       │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 6. 启用压测线程组          │
│    → 点击 ▶ 运行          │
└─────────────┬────────────┘
              ▼
┌──────────────────────────┐
│ 7. 查看聚合报告 / HTML    │
│    → 分析结果             │
└──────────────────────────┘
```

---

## 关键文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| JMeter 测试计划 | [bill-import-test.jmx](file:///e:/后端/项目练习/smartPark/load-test/bill-import-test.jmx) | 带 JWT 认证的完整压测计划 |
| CSV 数据配置 | [bill-files.csv](file:///e:/后端/项目练习/smartPark/load-test/bill-files.csv) | 9 个测试文件路径配置 |
| Excel 生成脚本 | [generate_excel.py](file:///e:/后端/项目练习/smartPark/load-test/generate_excel.py) | 生成测试数据 |
| 通用 README | [README.md](file:///e:/后端/项目练习/smartPark/load-test/README.md) | 技术参数、指标标准等 |
| 测试数据目录 | `load-test/test-files/` | 已生成的 9 个 Excel 文件 |