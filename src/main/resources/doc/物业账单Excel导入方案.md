# 物业账单 Excel 导入 — 方案设计文档

---

## 一、需求描述

### 1.1 背景
智慧停车/物业管理系统中，物业公司需要批量导入业主的物业账单数据，包括物业费、水费、电费等多种费用类型，以便进行后续的账单查询、缴费管理和统计分析。目前账单数据只能逐条录入，效率低下，无法满足批量操作的业务需求。

### 1.2 问题
如果直接在 HTTP 请求线程中同步处理 Excel 导入，可能引发以下问题：
- **HTTP 连接长时间占用**：万行级别的数据处理需要 10~30 秒，前端连接长时间挂起
- **Tomcat 线程阻塞**：导入期间占用 Web 容器线程，多个并发导入可能耗尽线程池
- **超时风险**：前端、Nginx 或网关的默认超时时间通常低于 30 秒，可能发生请求中断
- **数据库冲击**：无并发控制时，大量导入任务同时执行可能导致数据库被打满

### 1.3 需求
实现一个物业账单 Excel 导入功能，要求：
- 支持上传固定格式的 Excel 文件，万行级别数据稳定处理
- 采用**全量校验**策略，所有数据行校验完毕后统一决策是否入库
- 校验失败时返回**详细错误清单**（行号、字段名、错误原因），方便用户修正
- 校验通过的数据批量写入数据库
- 每次导入操作记录审计信息（操作人、时间、行数、失败明细等）
- 导入过程不能长时间占用 HTTP 连接，不能阻塞 Tomcat 工作线程
- 支持多用户并发导入，但需限制并发数防止数据库被打满
- 校验通过的数据入库，校验不通过的数据记录到 fail_detail，实现部分成功

---

## 二、解决思路

### 2.1 核心流程
导入功能的核心逻辑分为四个阶段：文件接收与任务创建、异步处理、前端轮询、结果展示。

第一阶段，用户上传 Excel 后，系统立即创建一条导入记录，分配一个 UUID 作为 taskId，然后立即返回 taskId 给前端，HTTP 请求处理完毕。

第二阶段，通过独立的线程池 Worker 异步执行实际的解析、校验和入库工作。

第三阶段，前端通过定时调用查询接口获取任务状态。

第四阶段，任务完成后前端展示最终结果。

处理期间不占用 HTTP 连接，不阻塞 Tomcat 工作线程，通过线程池参数控制并发数。

### 2.2 关键维度
数据导入的几个关键维度如下：

- **解析方式**：万行级数据不能一次性加载到内存，需采用流式读取，每批处理一定数量的行后释放内存。
- **校验策略**：逐行校验，校验通过的行入库，校验不通过的行记录到错误清单。最终结果可能是部分成功（一部分数据入库，一部分数据被拒绝）。
- **写入方式**：万行数据应分批写入数据库，避免单次事务过大导致锁表。
- **并发控制**：通过独立的线程池隔离导入任务，与 Tomcat 线程池解耦。

### 2.3 需要考虑的点
- **大文件 OOM**：万行数据文件可能达到数 MB，一次性读取全部到内存可能导致堆内存溢出。
- **事务一致性**：采用部分成功策略，校验通过的行入库，校验失败的行记录错误。入库阶段分批写入，每批独立提交。
- **服务重启**：异步任务执行过程中服务重启，正在处理的任务状态如何处理。
- **文件清理**：上传的临时文件需要及时清理，防止磁盘空间被占满。
- **并发保护**：多个用户同时导入时如何控制资源使用，防止数据库被打满。
- **审计追溯**：每次导入的操作人、时间、结果等需要完整记录，便于后续排查和责任追溯。

---

## 三、解决方案

### 方案 A：同步全量校验 + 批量入库

#### 方案思路
Controller 接收到 MultipartFile 后，直接在 HTTP 请求线程中完成所有的解析、校验和入库操作，最后将结果返回给前端。整个过程中 HTTP 连接保持打开状态，前端一直等待响应。

1. Controller 接收文件，调用 Service
2. EasyExcel 流式读取并逐行校验，收集校验通过和失败的行到各自的集合中
3. 校验通过的行调用 `saveBatch` 分批入库，失败的行信息存入 fail_detail
4. 返回导入结果（成功行数 + 失败行明细）

#### 技术选型
- `EasyExcel`（流式读取，监听器模式，项目已有依赖）
- `MyBatis-Plus`（saveBatch 分批写入，项目已有依赖）
- 零额外组件，复用项目现有技术栈

#### 优点
- **实现极简**：不需要异步处理、不需要轮询接口、不需要状态表，一个 Controller + 一个 Service 即可完成，代码量最少。
- **数据一致性最好**：整个处理过程在一个事务中完成，全有或全无，不存在部分入库的问题。
- **维护成本低**：没有额外的组件和依赖，不存在线程池管理、降级处理等复杂度。

#### 缺点
- **HTTP 连接过长**：万行数据的处理可能需要 10 到 30 秒，连接长期占用，前端需要设置很长的超时时间。
- **Tomcat 线程阻塞**：导入期间占用一个 Web 容器线程。如果多个用户同时导入，可能耗尽 Tomcat 的线程池，影响其他正常请求的处理。
- **超时风险**：前端、Nginx 或网关的默认超时时间通常只有几十秒，需要额外配置才能适配。如果配置不当，导入过程中请求中断，已经完成的工作全部白费，无法断点续传。
- **并发控制困难**：无法精确控制同时执行的导入任务数量，只能依赖 Tomcat 线程池的容量间接控制。
- **无进度反馈**：用户只能看着页面等待，无法知道处理进度。

---

### 方案 B：异步导入 + 线程池 + 前端轮询

#### 方案思路
Controller 收到文件后立即返回 taskId，后端通过独立的线程池 Worker 异步执行解析、校验和入库操作，前端通过定时轮询查询接口获取任务状态和结果。

1. Controller 接收文件，调用 Service 创建导入任务
2. Service 生成 UUID 作为 taskId，插入导入记录（状态 PENDING），保存文件到临时目录，返回 taskId
3. Controller 立即返回 `{ taskId }` 给前端
4. Service 将任务提交到独立的 `ThreadPoolTaskExecutor` 线程池
5. Worker 线程执行：更新状态为 PROCESSING → EasyExcel 流式读取 → 逐行校验（校验通过的行放入入库集合，失败的行放入错误集合）→ 入库集合中数据 `saveBatch` 分批入库 → 更新导入记录为 SUCCESS，记录成功行数和失败明细
6. 前端轮询 `GET /api/bill/import/task/{taskId}`，状态变为 SUCCESS 或 FAIL 时停止轮询并展示结果

#### 技术选型
- `EasyExcel`（流式读取，避免 OOM）
- `MyBatis-Plus`（saveBatch 分批写入）
- `ThreadPoolTaskExecutor`（Spring 提供的线程池，设置 core/max/queue 参数控制并发）
- 零额外中间件，所有组件项目均有或可直接引入

#### 优点
- **HTTP 秒级返回**：Tomcat 线程仅处理文件保存和 taskId 生成，耗时在毫秒级，不长时间占用连接资源。
- **Tomcat 线程解耦**：导入任务走独立的线程池，不阻塞 Web 容器的工作线程。即使同时有多个导入任务在执行，Tomcat 线程仍然可以正常处理其他请求。
- **并发精确可控**：通过线程池的 `corePoolSize`、`maxPoolSize` 和 `queueCapacity` 三个参数可以精准控制同时处理的导入任务数量，防止数据库被打满。
- **用户体验好**：前端可以通过轮询实时获取任务状态，展示"处理中"或最终的成功/失败结果。
- **可扩展性强**：未来可以平滑升级为 WebSocket 主动推送或消息队列方案，核心解析逻辑完全复用。

#### 缺点
- **实现相对复杂**：需要额外提供任务状态查询接口、线程池配置、任务状态管理等代码。
- **前端需要轮询逻辑**：需要编写定时器定期查询任务状态，并在状态终态时停止轮询。
- **事务管理复杂**：异步线程中的事务需要独立管理，不能依赖请求线程的事务上下文。分批写入时，如果中间某批失败，需要决定是整体回滚还是允许部分成功。
- **重启恢复问题**：服务重启时，正在执行中的任务（状态 PROCESSING）需要处理机制来恢复或标记为失败。

---

### 方案 C：消息队列（MQ）异步导入

#### 方案思路
文件上传后，将导入任务消息发送到消息队列，由独立的消费者处理解析、校验和入库。

1. Controller 接收文件，保存到临时目录，将文件路径和任务信息封装为消息发送到 MQ
2. 立即返回 taskId 给前端
3. MQ 消费者接收到消息后，异步执行解析、校验、入库
4. 前端通过轮询或 WebSocket 获取任务结果

#### 技术选型
- `RabbitMQ` 或 `RocketMQ`
- `EasyExcel` + `MyBatis-Plus`
- 需要部署和维护消息队列中间件

#### 优点
- **彻底解耦**：导入口和处理器完全分离，互不影响，各自可以独立扩展。
- **削峰填谷**：消息队列作为缓冲区，消费端按自身能力处理消息。即使瞬间有大量导入请求，也不会压垮数据库。
- **可靠性高**：消息持久化到磁盘，消费失败后可以自动重试，不丢失任务。
- **分布式友好**：消费者可以水平扩展，多实例并行消费，吞吐量线性增长。

#### 缺点
- **需要引入 MQ 中间件**：需要部署和维护 RabbitMQ 或 RocketMQ 服务，增加了运维成本和部署复杂度。
- **实现最为复杂**：需要处理消息投递、消费幂等、死信队列、消息顺序等问题，代码量和测试工作量显著增加。
- **对当前项目过度设计**：smartPark 项目当前阶段还没有 MQ 基础设施，为单个导入功能引入 MQ 代价较大。
- **调试困难**：异步消息链路排查问题比同步调用复杂，需要依赖消息追踪和日志。

---

## 四、方案对比

在实现复杂度方面，方案 A 最为简单，一个 Controller 加一个 Service 即可完成，不需要异步处理、不需要轮询接口、不需要额外的配置类。方案 B 中等，需要编写线程池配置、任务状态管理、查询接口和前端轮询逻辑，代码量约为方案 A 的 2~3 倍。方案 C 最为复杂，需要部署 MQ 服务、处理消息投递和消费逻辑、考虑消息持久化和重试、编写幂等处理代码。

在 HTTP 连接时间方面，方案 A 需要保持连接 10 到 30 秒直到所有数据处理完毕。方案 B 和方案 C 都是秒级返回，上传文件后立即返回 taskId，将实际处理交由后端异步执行。

在 Tomcat 线程占用方面，方案 A 会全程占用一个 Web 容器线程，如果多个用户同时导入，Tomcat 线程池可能被打满，导致其他业务接口无法响应。方案 B 和方案 C 仅在文件上传和 taskId 生成阶段占用 Tomcat 线程，之后的工作由独立的线程池或 MQ 消费者执行，不占用 Web 容器线程。

在并发控制能力方面，方案 A 依赖 Tomcat 线程池的容量间接控制，控制粒度粗糙且影响其他业务。方案 B 通过线程池的 corePoolSize、maxPoolSize 和 queueCapacity 三个参数可以精准控制同时处理的导入任务数量，与其他业务完全隔离。方案 C 通过消费者的数量和预取计数控制，弹性最好。

在数据一致性方面，方案 A 在单事务中完成整个导入过程，一致性最好且实现简单。方案 B 采用部分成功策略，校验通过的行入库，校验失败的行记录错误，分批写入时每批独立提交，适用于导入场景下用户更关心"能导入多少是多少"的需求。方案 C 在消息消费过程中的事务管理更为复杂，需要处理消息重试带来的幂等问题。

在用户体验方面，方案 A 需要用户等待 10 到 30 秒，页面没有任何进度反馈，体验较差。方案 B 通过轮询可以展示"处理中"的状态，并在完成后展示成功行数或错误清单，体验较好。方案 C 如果配合 WebSocket 推送可以实现实时反馈，体验最好。

在运维依赖方面，方案 A 和方案 B 不需要任何额外的中间件，项目现有技术栈即可满足。方案 C 需要部署和维护消息队列服务，增加了运维成本和故障排查的复杂度。

在代码维护成本方面，方案 A 最低，新增代码量最少。方案 B 中等，需要管理线程池配置、任务状态转换、前端轮询逻辑。方案 C 最高，涉及 MQ 的配置、消息处理、重试、死信等多方面的处理。

在万行数据性能方面，方案 A 和方案 B 底层都使用 EasyExcel 流式读取和 MyBatis-Plus saveBatch，核心数据处理逻辑一致，性能相当。两者的差异不在处理速度上，而是在连接管理和并发控制方面。方案 C 由于 MQ 的缓冲能力，在超高并发场景下稳定性最好。

在可扩展性方面，方案 A 最差，如果未来需要支持更大的数据量或更高的并发，需要整体重构。方案 B 中等，可以平滑升级为 WebSocket 推送或 MQ 方案。方案 C 最好，消费者可以水平扩展，消息队列天然支持分布式。

---

## 五、最终方案：异步导入 + 线程池 + 前端轮询

### 5.1 选型理由

选择方案 B 而非其他两个方案的原因，通过与各方案的逐一对比来说明。

**与方案 A（同步全量入库）对比：**

选择方案 B 而非方案 A，主要因为以下几个决定性因素。

在 HTTP 连接占用方面，方案 A 需要保持连接 10 到 30 秒，前端、Nginx 和网关的默认超时时间一般在 30 秒以内，意味着万行级别的导入随时可能因为超时而失败。即使调大超时配置，长连接也会占用 Tomcat 的连接资源，降低服务的整体吞吐能力。方案 B 秒级返回，没有超时风险，不占用 HTTP 连接资源。

在 Tomcat 线程阻塞方面，方案 A 的导入过程全程占用一个 Tomcat 工作线程。如果两个用户同时导入，就会占用两个线程，以此类推。Tomcat 默认的线程池通常在 200 个线程左右，看似足够，但导入操作是长时间阻塞的 IO 操作，一旦并发导入数量增多，会显著挤压其他业务接口的处理能力。方案 B 将导入任务提交到独立的线程池，Tomcat 线程仅在文件接收和 taskId 生成阶段短暂使用，之后即可处理其他请求。

在并发控制方面，方案 A 无法精确控制同时执行的导入任务数量。虽然可以限制接口的并发访问，但无法区分"同时到达多个导入请求"和"一个导入请求处理较慢"两种情况。方案 B 通过线程池的 `corePoolSize` 设置为 2，保证了同时最多只有 2 个导入任务在执行，其他任务在队列中等待，控制精确且不影响其他接口。

在用户体验方面，方案 A 用户只能看着页面等待，不知道处理进度，也不知道什么时候能完成。如果连接中断，用户无法区分是"正在处理中"还是"已经失败"。方案 B 通过状态查询接口，前端可以实时展示任务状态，用户知道当前处于"排队中"还是"处理中"，以及最终的结果。

**与方案 C（MQ 消息队列）对比：**

选择方案 B 而非方案 C，主要因为以下几个决定性因素。

在运维成本方面，方案 C 需要引入并维护消息队列中间件（如 RabbitMQ 或 RocketMQ）。这意味着需要配置和管理 MQ 服务的高可用、监控告警、磁盘空间清理、版本升级等运维工作。对于 smartPark 项目当前阶段来说，为单一个导入功能引入一套完整的 MQ 基础设施，代价远大于收益。

在实现复杂度方面，方案 C 需要处理消息投递、消费确认、消费幂等、死信队列、消息顺序等问题。这些问题在方案 B 中都不存在，方案 B 的线程池 Worker 执行是同步的、顺序的，天然保证了数据的一致性和完整性。

在一致性保证方面，方案 C 的消息消费有 at-least-once（至少一次）和 at-most-once（至多一次）两种语义。要做到 exactly-once（精确一次）需要引入幂等机制和分布式事务，复杂度大幅增加。方案 B 的异步线程中所有操作在一个明确的作用域内，事务管理相对简单。

在适用场景方面，方案 C 的真正价值在于削峰填谷、微服务解耦和多消费者并行处理。smartPark 当前阶段的导入场景，日导入量和并发量远未达到需要 MQ 的水平。方案 B 的线程池已经能够提供足够的缓冲能力。

**综合结论：**

基于以上对比，选择方案 B 的综合理由可归纳为：它克服了方案 A 的 HTTP 长连接、Tomcat 线程阻塞、无法精确控制并发等生产环境不可接受的问题；又避免了方案 C 引入 MQ 中间件带来的运维成本和实现复杂度，在当前阶段保持架构的轻量和可维护。同时方案 B 的所有技术组件都是 Spring Boot 的标准能力，实现成本可控，且未来可以平滑升级到方案 C 或其他高级架构。

### 5.2 实现架构

```
客户端                              服务端
  │                                   │
  │  POST /api/bill/import            │
  │  (multipart: file)                │
  │──────────────────────────────────▶│  Controller
  │                                   │  ├── 生成 taskId (UUID)
  │                                   │  ├── 保存文件到临时目录
  │                                   │  ├── 插入导入记录 (status=PENDING)
  │                                   │  └── 提交任务到线程池
  │  ← 200 { taskId: "uuid-xxx" }     │
  │◀──────────────────────────────────│
  │                                   │
  │  (下一秒)                          │  [ThreadPool Worker]
  │                                   │  ├── UPDATE status=PROCESSING
  │  GET /api/bill/import/task/uuid   │  ├── EasyExcel 流式读取
  │──────────────────────────────────▶│  ├── 逐行校验
  │  ← { status: "PROCESSING" }       │  │   ├── 通过 → 加入入库集合
  │◀──────────────────────────────────│  │   └── 失败 → 加入错误集合
  │                                   │  ├── 入库集合 → saveBatch 分批写入
  │  (指数退避轮询)                    │  ├── UPDATE status=SUCCESS
  │                                   │  │    successRows, failRows, failDetail
  │  GET /api/bill/import/task/uuid   │
  │──────────────────────────────────▶│
  │  ← { status: "SUCCESS",           │
  │       totalRows: 5000,            │
  │       successRows: 4980,          │
  │       failRows: 20,               │
  │       failDetail: [...],          │
  │       costTimeMs: 8320 }          │
  │◀──────────────────────────────────│
  │                                   │
  │  停止轮询，展示结果                │
```

### 5.3 代码结构

```
src/main/java/com/smartpark/
├── config/
│   └── BillImportThreadPoolConfig.java      # 导入线程池配置
├── controller/
│   └── BillImportController.java             # 导入接口（上传 + 查询）
├── listener/
│   └── BillExcelListener.java                # EasyExcel 流式读取监听器
├── mapper/
│   ├── PropertyBillMapper.java               # 账单 Mapper
│   └── PropertyBillImportRecordMapper.java   # 导入记录 Mapper
├── service/
│   ├── BillImportService.java                # 导入服务接口
│   └── Impl/
│       └── BillImportServiceImpl.java        # 导入业务实现
└── pojo/
    ├── dto/
    │   └── BillExcelRowDTO.java              # Excel 行数据 DTO
    ├── entity/
    │   ├── PropertyBill.java                 # 物业账单实体
    │   └── PropertyBillImportRecord.java     # 导入记录实体
    └── vo/
        └── ImportTaskVO.java                 # 任务状态视图对象
```

### 5.4 各组件职责

- **`BillImportThreadPoolConfig`**：使用 `@Configuration` 和 `@Bean` 定义一个 `ThreadPoolTaskExecutor`，设置核心线程数为 2，最大线程数为 4，队列容量为 10，线程名前缀为 `bill-import-`，拒绝策略为 `CallerRunsPolicy`，开启优雅关闭。

- **`BillImportController`**：提供两个 REST 接口。POST 接口接收 `MultipartFile`，调用 Service 创建导入任务并返回 taskId。GET 接口根据 taskId 查询导入任务的状态和结果。

- **`PropertyBill`**：MyBatis-Plus Entity 类，通过 `@TableName` 和 `@TableField` 注解映射 `property_bill` 表的字段。

- **`PropertyBillImportRecord`**：MyBatis-Plus Entity 类，映射 `property_bill_import_record` 表。

- **`PropertyBillMapper`**：继承 `BaseMapper<PropertyBill>`，提供基础的增删改查。`PropertyBillImportRecordMapper` 同理。

- **`BillImportServiceImpl`**：核心业务实现类。负责创建导入任务（生成 taskId、保存文件、插入记录、提交线程池）和执行异步导入逻辑（更新状态、流式读取、全量校验、分批写入、结果记录）。

- **`BillExcelRowDTO`**：Excel 行数据映射类，使用 EasyExcel 的 `@ExcelProperty` 注解指定列名，包含所有字段和校验注解。

- **`ImportTaskVO`**：任务状态视图对象，包含 taskId、status、totalRows、successRows、failRows、failDetail、costTimeMs 字段，用于返回给前端。

### 5.5 核心业务流程

1. 用户通过前端页面上传 Excel 文件，POST 请求到达 BillImportController。

2. Controller 调用 BillImportServiceImpl 的 createTask 方法。该方法先生成一个 UUID 作为 taskId，然后将文件保存到服务器的临时目录（文件名为 taskId 加原始文件后缀，防止文件名冲突）。接着往 property_bill_import_record 表中插入一条记录，状态设为 PENDING，记录文件名和操作人 ID。最后将 taskId 返回给 Controller。

3. Controller 将 taskId 封装为 `{ taskId }` 返回给前端，HTTP 响应立即返回。此时前端的请求处理完毕，耗时通常在几十毫秒以内。

4. BillImportServiceImpl 将导入任务提交到 BillImportExecutor 线程池。如果线程池有空闲线程，任务立即执行。如果当前已有 2 个任务在执行，新任务进入等待队列。如果队列中也满了（超过 10 个任务排队），则触发 CallerRunsPolicy 拒绝策略，由提交线程（Tomcat 线程）自己执行任务。这种设计保证了在任何情况下都不会因为导入任务过多而导致系统崩溃。

5. 线程池中的 Worker 线程开始执行导入任务。首先将导入记录的状态从 PENDING 更新为 PROCESSING。

6. Worker 线程使用 EasyExcel 的监听器模式读取 Excel 文件。EasyExcel 的读取机制是流式的，不会一次性将整个文件加载到内存，可以避免OOM。通过注册一个 `AnalysisEventListener`，每读取 500 行就会触发一次回调。在回调中，先将这些行加入一个临时集合，当集合大小达到 500 条时进入校验流程。

7. 校验逻辑分为两个层次。第一层是基础校验，检查每个字段的格式是否正确，包括日期格式是否合法、金额是否为正数且不超过两位小数、手机号是否为 11 位数字、费用类型是否在枚举范围内、必填字段是否为空或空字符串。第二层是业务校验，检查计费截止日期是否大于等于计费起始日期，缴费截止日期是否大于等于计费截止日期。校验过程中对每行数据单独判断，将校验通过的行放入入库集合，将校验失败的行记录到错误集合。每个错误记录包含行号、字段名、原始值和错误原因。某一行的多个字段错误会全部收集，不会因为发现一个错误就跳过该行的后续校验。

8. 所有数据行读取和校验完毕后，入库集合中可能包含部分行，错误集合中也可能包含部分行。Worker 线程先处理入库集合，使用 MyBatis-Plus 的 `saveBatch` 方法将校验通过的数据分批写入 property_bill 表，每批 1000 条。

9. 入库完成后，更新导入记录的状态为 SUCCESS，记录总行数、成功行数、失败行数、失败明细和处理耗时。注意这里的 SUCCESS 不代表全部成功，而是表示任务执行完成，具体结果通过 successRows 和 failRows 体现。

10. 前端在接收到 taskId 后，启动一个定时器，采用指数退避策略轮询 GET 查询接口。第一次查询后等待 1 秒，第二次等待 2 秒，第三次等待 4 秒，以此类推，最多等待 5 秒。这种策略既保证了状态变化后能尽快感知到，又避免了在短时间内产生大量不必要的查询请求。

11. 当前端查询到状态为 SUCCESS 或 FAIL 时，停止轮询。如果是 SUCCESS，展示导入成功的行数和耗时。如果是 FAIL，展示错误清单，包含所有错误行的行号、字段名和错误原因。

### 5.6 线程池配置

**为什么需要自定义线程池**

导入任务需要与 Tomcat 的 HTTP 请求处理线程池解耦。如果不使用独立的线程池，导入任务会直接在 Tomcat 线程中执行，万行数据的处理耗时 10 到 30 秒，这段时间内该 Tomcat 线程无法处理其他请求，包括状态查询接口。使用独立的线程池后，导入任务在后台线程中执行，HTTP 请求秒级返回，Tomcat 线程可以继续处理其他请求。

Spring Boot 项目中有三种创建线程池的方式：一是 JDK 自带的 `Executors` 工具类，但其 `newFixedThreadPool` 使用无界队列存在 OOM 风险，`newCachedThreadPool` 最大线程数无上限可能打爆系统，阿里巴巴开发规范已明确禁止使用。二是直接 `new ThreadPoolExecutor`，但需要手动管理生命周期和优雅关闭。三是 Spring 提供的 `ThreadPoolTaskExecutor`，它是对 `ThreadPoolExecutor` 的封装，自动随 Spring 容器启停，支持优雅关闭和监控集成，是 Spring 项目中的最佳实践。

**各参数的选择依据**

核心线程数设为 2。导入任务是 IO 密集型操作，瓶颈在数据库写入而不是 CPU 运算。单线程处理万行数据大约需要 20 到 30 秒，2 个线程同时处理可以满足物业月度账单的日常导入量，也不会因为过多并发写入导致数据库连接池争抢。

最大线程数设为 4，队列容量设为 10。ThreadPoolTaskExecutor 的线程创建逻辑是：先填满核心线程，然后任务进入队列，队列满了才会创建新线程直到最大线程数。正常场景下 2 个核心线程加 10 个队列已经足够应对日常并发，最大线程 4 作为安全余量在极端高峰时提供缓冲。

拒绝策略选择 CallerRunsPolicy。队列和线程池都满时，由提交任务的 Tomcat 线程自己执行导入。这意味着高峰期用户上传时的 HTTP 响应会变慢，但任务不会丢失。相比 AbortPolicy 的抛异常拒绝和 DiscardPolicy 的静默丢弃，CallerRunsPolicy 最适合导入导出这类对数据完整性要求高的业务。

空闲线程存活时间设为 120 秒，因为导入是低频操作，额外线程创建后保留时间长一些可以在短时间内复用，避免反复创建和销毁的开销。同时开启优雅关闭，设置最多等待 30 秒，确保服务重启时正在执行的导入任务能够完成，避免数据写入中断导致的状态不一致。

```java
@Bean("billImportExecutor")
public ThreadPoolTaskExecutor billImportExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);                    
    executor.setMaxPoolSize(4);                     
    executor.setQueueCapacity(10);                  
    executor.setKeepAliveSeconds(120);              
    executor.setThreadNamePrefix("bill-import-");   
    executor.setRejectedExecutionHandler(
        new ThreadPoolExecutor.CallerRunsPolicy()); 
    executor.setWaitForTasksToCompleteOnShutdown(true);  
    executor.setAwaitTerminationSeconds(30);             
    executor.initialize();
    return executor;
}
```

### 5.7 接口设计

POST 接口的路径是 `/api/bill/import`，请求方式为 `multipart/form-data`，参数是一个名为 `file` 的文件。响应体中包含 `code`、`msg` 和 `data` 三个字段，`data` 中包含 `taskId` 字段。

GET 接口的路径是 `/api/bill/import/task/{taskId}`，路径参数为导入任务的唯一标识。响应体中包含 `code`、`msg` 和 `data` 三个字段，`data` 是一个 `ImportTaskVO` 对象，包含 `taskId`、`status`、`totalRows`、`successRows`、`failRows`、`failDetail` 和 `costTimeMs` 七个字段。

status 字段的取值和含义分别是：PENDING 表示任务在排队等待执行，PROCESSING 表示任务正在执行中，SUCCESS 表示导入成功，FAIL 表示导入失败。前端在 status 为 SUCCESS 或 FAIL 时停止轮询。

### 5.7 数据库表设计

物业账单主表命名为 `property_bill`，字段定义如下：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | 自增主键 | 主键ID |
| building_no | VARCHAR(20) | NOT NULL | 楼栋号 |
| unit_no | VARCHAR(20) | NOT NULL | 单元号 |
| room_no | VARCHAR(20) | NOT NULL | 房号 |
| owner_name | VARCHAR(100) | DEFAULT NULL | 业主姓名 |
| phone | VARCHAR(20) | DEFAULT NULL | 联系电话 |
| fee_type | VARCHAR(50) | NOT NULL | 费用类型 |
| billing_start_date | DATE | NOT NULL | 计费起始日期 |
| billing_end_date | DATE | NOT NULL | 计费截止日期 |
| amount_due | DECIMAL(10,2) | NOT NULL | 应收金额 |
| amount_paid | DECIMAL(10,2) | NOT NULL DEFAULT 0 | 已收金额 |
| due_date | DATE | NOT NULL | 缴费截止日期 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'UNPAID' | UNPAID-未缴 / PAID-已缴 / PARTIAL-部分缴 |
| remark | VARCHAR(500) | DEFAULT NULL | 备注 |
| created_by | BIGINT | DEFAULT NULL | 创建人ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

索引设计如下：

| 索引名称 | 索引字段 | 说明 |
|----------|----------|------|
| idx_room | building_no, unit_no, room_no | 联合索引，按房产维度查询 |
| idx_fee_type | fee_type | 按费用类型筛选 |
| idx_status | status | 按账单状态筛选 |
| idx_due_date | due_date | 按缴费截止日期排序和筛选 |

导入记录表命名为 `property_bill_import_record`，字段定义如下：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | 自增主键 | 主键ID |
| task_id | VARCHAR(64) | NOT NULL UNIQUE | 任务唯一标识（UUID） |
| file_name | VARCHAR(255) | NOT NULL | 原始文件名 |
| total_rows | INT | NOT NULL DEFAULT 0 | 总行数（不含表头） |
| success_rows | INT | NOT NULL DEFAULT 0 | 成功行数 |
| fail_rows | INT | NOT NULL DEFAULT 0 | 失败行数 |
| fail_detail | JSON | DEFAULT NULL | 失败行明细，JSON数组 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | PENDING-排队中 / PROCESSING-处理中 / SUCCESS-成功 / FAIL-失败 |
| cost_time_ms | BIGINT | DEFAULT NULL | 处理耗时（毫秒） |
| created_by | BIGINT | DEFAULT NULL | 操作人ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

两张表独立设计的考虑是职责分离。业务数据存入 property_bill 表，导入过程记录存入 property_bill_import_record 表。即使删除导入记录，也不影响已入库的账单数据。

### 5.8 Excel 模板字段

Excel 模板共包含 11 个字段。

楼栋号是文本类型，必填，长度不超过 20 个字符，例如 A1。单元号是文本类型，必填，长度不超过 20 个字符，例如 1。房号是文本类型，必填，长度不超过 20 个字符，例如 101。业主姓名是文本类型，非必填，长度不超过 100 个字符，例如张三。联系电话是文本类型，非必填，如果填写则必须是 11 位数字。费用类型是文本类型，必填，必须是预定义的枚举值之一，包括物业费、水费、电费、停车费、垃圾清运费、维修费、其他。计费起始日期是日期类型，必填，格式为 yyyy-MM-dd。计费截止日期是日期类型，必填，必须大于等于计费起始日期。应收金额是数值类型，必填，必须大于 0，最多保留两位小数。缴费截止日期是日期类型，必填，必须大于等于计费截止日期。备注是文本类型，非必填，长度不超过 500 个字符。

### 5.9 错误处理与边界情况

第一个边界情况是错误结构的设计。当某一行数据有多个字段错误时，需要全部收集并返回。例如，第三行的金额为负数且日期格式错误，需要在错误列表中包含两条记录。

第二个边界情况是文件清理。Worker 线程处理完成后，不论成功还是失败，都需要删除服务器临时目录中保存的 Excel 文件，避免磁盘空间被大量临时文件占用。

第三个边界情况是文件大小限制。在 application.yml 中配置 `spring.servlet.multipart.max-file-size: 10MB` 和 `max-request-size: 10MB`，同时在 Controller 层对文件大小做二次校验，确保不超过预期值（建议 5MB，大约对应 2 到 3 万行数据），超过则直接返回错误。

第四个边界情况是文件名校验。在 Controller 层就需要对文件的后缀名进行检查，只允许 .xlsx 格式，如果上传 .xls 或其他格式直接返回错误。

第五个边界情况是读取阶段异常处理。如果上传的 Excel 文件本身格式损坏（不是有效的 .xlsx），EasyExcel 在解析时会抛出异常。Worker 线程的最外层需要用 try-catch 包裹所有操作。捕获到任何异常后，将导入记录状态更新为 FAIL，记录异常信息到 fail_detail，同时清理已写入的临时文件。防止异步任务异常终止后导入记录停留在 PROCESSING 状态变成悬挂任务。

第六个边界情况是服务重启保护。通过线程池的 `setWaitForTasksToCompleteOnShutdown(true)` 结合 `setAwaitTerminationSeconds(30)`，确保容器关闭时正在执行的导入任务可以正常完成。如果服务非正常崩溃，正在处理中的任务会处于 PROCESSING 状态，后续可以通过定时任务或人工干预来处理这些悬挂任务。

第七个边界情况是并发控制。线程池核心线程数为 2，队列容量为 10，所以同时最多处理 2 个导入任务，另有 10 个任务在等待队列中。超出这个数量的请求会触发 CallerRunsPolicy，由提交线程同步执行。这种设计保证了在任何情况下都不会因为导入任务过多而导致数据库被打满。

第八个边界情况是 Excel 模板格式校验。如果上传的 Excel 缺少必要的列或列的顺序不对，EasyExcel 的注解映射会失败。需要在监听器的头部读取阶段校验表头是否与预期一致，不一致则直接终止处理并返回错误。

### 5.10 数据校验规则

基础校验包括六个方面。第一，所有必填字段不能为空或空字符串。第二，日期字段必须符合 yyyy-MM-dd 格式，且为合法日期。第三，金额字段必须大于 0 且最多保留两位小数，必须是有效的数字格式。第四，联系电话如果填写则必须是 11 位数字，以 1 开头。第五，费用类型必须在预定义的枚举范围内。第六，文本字段的长度不能超过数据库表定义的最大长度。

业务校验包括两个方面。第一，计费截止日期必须大于等于计费起始日期。第二，缴费截止日期必须大于等于计费截止日期。

### 5.11 响应结构设计

导入完成后的返回结构（部分成功场景）：

```json
{
    "code": 200,
    "data": {
        "taskId": "uuid-xxx",
        "status": "SUCCESS",
        "totalRows": 5000,
        "successRows": 4980,
        "failRows": 20,
        "failDetail": [
            { "row": 3, "field": "amountDue", "value": "-100", "reason": "应收金额必须大于0" },
            { "row": 5, "field": "phone", "value": "123", "reason": "联系电话格式不正确" },
            { "row": 8, "field": "billingEndDate", "value": "2025-13-01", "reason": "日期格式不正确" },
            { "row": 8, "field": "amountDue", "value": "abc", "reason": "金额必须为有效数字" }
        ],
        "costTimeMs": 8320
    },
    "msg": "导入完成，成功 4980 条，失败 20 条"
}
```

---

## 六、注意事项

### 6.1 EasyExcel 流式读取说明
- EasyExcel 的监听器模式必须注册一个 `AnalysisEventListener`，不能使用同步读取（`EasyExcel.read().sheet().doReadSync()`），否则会一次性将全部数据加载到内存。
- 监听器中的 `invoke()` 方法每读取一行都会调用一次，应在此方法中做行级别处理（如添加到缓冲集合），而不是「每行直接校验并入库」。
- 当缓冲集合大小达到设置阈值（如 500 条）时，调用校验逻辑，然后清空缓冲集合，防止内存持续增长。
- 在 `doAfterAllAnalysed()` 方法中处理剩余的不足一个批次的数据。

### 6.2 导入任务状态流转
- PENDING → PROCESSING：任务从队列中被 Worker 线程取出，开始执行。
- PROCESSING → SUCCESS：入库完成，记录成功行数和失败行数。SUCCESS 不代表全部成功，而是表示任务执行完成，具体结果通过 successRows 和 failRows 字段区分。
- PROCESSING → FAIL：入库过程中发生严重异常（如数据库不可用），或者 Excel 文件格式损坏无法解析。
- 状态不提供逆向流转，一旦达到终态（SUCCESS 或 FAIL）即不可变更。

### 6.3 事务与一致性
- 异步线程中的事务需要独立管理。Worker 线程内的所有操作（读取、校验、入库）在一个 `@Transactional` 方法中执行。
- 入库阶段的 `saveBatch` 分批写入，每批 1000 条独立提交。如果中间某批失败，前面批次已提交的数据不会自动回滚。考虑到前置校验已经保证了数据格式和业务规则的合法性，入库阶段失败的概率极低。如果出现数据库写入异常（如磁盘满、连接断开），属于基础设施级别的故障，应抛出异常由外层处理，将任务状态标记为 FAIL。
- 整体采用部分成功策略：校验通过的行入库，校验失败的行记录错误。最终结果通过 successRows 和 failRows 字段反映给用户。

### 6.4 文件清理
- 上传的 Excel 文件保存在操作系统的临时目录（`System.getProperty("java.io.tmpdir")`）。
- Worker 线程处理完成后，不论成功还是失败，都需要删除临时文件。
- 如果服务在处理过程中崩溃，临时文件可能残留。可以通过操作系统的临时文件清理策略或启动一个定时任务定期清理超过一定时间的临时文件来处理。

---

## 七、实施步骤

第一阶段是建表和实体类。执行 SQL 创建 property_bill 表和 property_bill_import_record 表，编写对应的 MyBatis-Plus Entity 类和 Mapper 接口。

第二阶段是线程池配置。编写 BillImportThreadPoolConfig 配置类，定义 ThreadPoolTaskExecutor 的 Bean，配置核心线程数、最大线程数、队列容量、线程名前缀和拒绝策略。

第三阶段是 Excel 读取和校验。编写 BillExcelRowDTO 类，使用 EasyExcel 的 @ExcelProperty 注解映射字段。编写 Excel 读取监听器，实现流式读取和逐行校验逻辑。编写校验方法，实现基础校验和业务校验。

第四阶段是异步导入服务。编写 BillImportServiceImpl 实现类，实现任务创建、文件保存、任务提交、状态更新、结果查询等核心逻辑。

第五阶段是 Controller 层。编写 BillImportController，提供文件上传和任务查询两个 REST 接口。

第六阶段是容错处理。处理文件格式错误、空文件、校验失败、入库异常、服务重启等各种边界情况。

---

*本文档为设计方案，后续将按照上述设计进入编码阶段。*
