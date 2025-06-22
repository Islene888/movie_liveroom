
# Flinkjob 模块说明

本模块为 **Flink 实时处理作业工程**，负责消费弹幕/互动事件（Kafka），实时聚合统计结果并写回 Kafka，用于直播、互动、消息流等场景的实时指标分析。

---

## 目录结构与功能

```
src/main/java/com/ella/flinkjob/
├── config/
│   ├── KafkaProducerConfig.java      # Producer配置，提供KafkaProducer Bean
│   ├── KafkaSinkUtil.java            # Flink写Kafka的工具
├── controller/
│   └── FlinkEventTypeAggregator.java # Flink作业主类，实时聚合
├── dto/
│   ├── LiveEvent.java                # 事件结构定义
│   └── StatsDto.java                 # 聚合结果结构定义
├── service/
│   └── LiveEventProcessor.java       # 事件解析/MapFunction
├── FlinkjobApplication.java          # (可选) Spring Boot 启动入口
```

---

## 主要文件说明

* **KafkaProducerConfig.java / KafkaSinkUtil.java**
  Kafka连接参数和Flink Sink工具，方便 Flink 与 Kafka 集成。

* **FlinkEventTypeAggregator.java**
  Flink主作业入口，负责：

    * 消费 `live-events` topic
    * 解析 eventType
    * 按窗口实时聚合，如点赞/评论/送礼数
    * 聚合结果写入 `live-events-agg` topic

* **LiveEvent.java / StatsDto.java**
  数据结构定义，便于类型安全和下游展示。

* **LiveEventProcessor.java**
  事件JSON解析、二元组转化，为窗口聚合准备。

---

## 运行流程说明

1. **Kafka Producer**（前端/后端）：写入用户行为到 `live-events`
2. **Flink作业**：解析聚合，写结果到 `live-events-agg`
3. **前后端消费聚合结果**：用于大屏、看板、统计等实时展示

---

## 文件命名建议

* 主类结尾建议：`*Aggregator.java`、`*Job.java`、`*Processor.java`
* 工具/配置类结尾建议：`*Util.java`、`*Config.java`

---

## 注意事项

* Flink 作业仅做数据流 ETL、聚合与 Kafka 输出，不直接涉及 WebSocket
* WebSocket/大屏推送由后端负责

---


# Flink 作业自动化提交与运维最佳实践（含 flink-submitter）

## 自动化提交器（flink-submitter）说明

`flink-submitter` 是一种常见的自动化实践，用于在 Flink 集群（JobManager/TaskManager）和依赖服务（Kafka/Zookeeper）**启动就绪后**，自动提交主作业 jar，避免每次都手动 exec。

### 为什么要用 flink-submitter？

* **CI/CD 友好**：部署脚本一条龙，启动即上线，无需人工干预。
* **本地/测试/云原生环境一致**，比如云端自动扩缩、重启能保证作业随集群恢复。
* **避免手抖、忘记 submit，节省调试时间**。

---

## Flink 作业自动化提交标准流程（以 docker-compose 为例）

### 1. 目录准备（推荐结构）

```
project-root/
├── docker-compose.yml
├── libs/                        # flink/lib 挂载目录
│   ├── flinkjob-0.0.1-SNAPSHOT.jar  # 主作业 JAR
│   ├── flink-connector-kafka-1.17.1.jar
│   ├── kafka-clients-3.1.0.jar
│   ├── jackson-databind-2.19.1.jar
│   └── ...
├── backend/                     # 你的后端工程目录
│   └── Dockerfile
└── README.md
```

### 2. docker-compose 配置（关键段落）

```yaml
services:
  jobmanager:
    image: flink:1.17.1-java11
    volumes:
      - ./libs/:/opt/flink/lib/
    # ...

  taskmanager:
    image: flink:1.17.1-java11
    volumes:
      - ./libs/:/opt/flink/lib/
    # ...

  flink-submitter:
    image: flink:1.17.1-java11
    depends_on:
      - jobmanager
      - taskmanager
    entrypoint: [ "/bin/sh", "-c" ]
    command: >
      flink run -m jobmanager:8081 \
        -c com.ella.flinkjob.FlinkEventTypeAggregator \
        /opt/flink/lib/flinkjob-0.0.1-SNAPSHOT.jar
    volumes:
      - ./libs/:/opt/flink/lib/
    networks:
      - appnet
```

#### **常见注意事项**（flink-submitter 相关）

1. **依赖要和 jobmanager/taskmanager 保持一致挂载方式**，确保 submitter 能拿到所有 jar，否则会 NoClassDefFoundError。
2. **不要在 jobmanager/taskmanager 的 lib 里留“老版本”依赖**，否则优先加载有坑。
3. **`depends_on` 不等于 “启动完成”**。如果提交器太快，Flink 还没 ready 会 submit 失败，可以加 `flink-submitter` 启动前的健康检测或等待逻辑（比如 shell 脚本里轮询 8081 健康）。
4. **不要频繁重启 submitter**，它只负责 submit 一次，JobManager 会维持作业。如果 submitter 容器报错退出不用管，不影响 Flink 作业本身。
5. **如要提交多个 job，可以写成多个 submitter 容器或一个 entrypoint 里依次 submit。**

---

## **完整配置流程**（“从0到现在”）

### 1. 打包与依赖收集

* 用 `mvn clean package -DskipTests` 打出主 jar
* 从本地 .m2 或 maven repository 拷贝所有依赖到 `libs` 目录
* 不要用“胖 jar”（uber-jar），用 `provided`/`compile` 控制 scope，保证 lib 干净

### 2. docker-compose 配置挂载

* jobmanager/taskmanager/flink-submitter 的 `/opt/flink/lib` 都挂本地 `libs` 目录

### 3. 配置 submitter 容器自动提交

* 提交命令用 `flink run -m jobmanager:8081 -c 主类 完整jar路径`
* 推荐用 entrypoint shell，先健康检查 jobmanager 8081（curl 通过再 submit），防止 race condition

  ```yaml
  entrypoint: [ "/bin/sh", "-c" ]
  command: |
    flink run -m jobmanager:8081 \
      -c com.ella.flinkjob.FlinkEventTypeAggregator \
      /opt/flink/lib/flinkjob-0.0.1-SNAPSHOT.jar
  ```

### 4. 一键启动

```bash
docker-compose up -d
```

* 这样所有依赖、JobManager、TaskManager、Kafka、Submitter 一条龙自动上线。

### 5. 查看作业 & 日志

* Flink UI：[http://localhost:8081](http://localhost:8081)
* 查看各容器日志：

  ```bash
  docker-compose logs jobmanager
  docker-compose logs flink-submitter
  ```

---

## **常见坑与最佳实践补充**

### 1. **多次自动提交的坑**

* 如果你不希望每次都自动 submit，可以把 submitter 注释掉，需要时 `docker-compose up flink-submitter`。

### 2. **提交多个 Flink Job**

* 可以为每个 Job 写独立 submitter 服务，或合并写成 shell 脚本依次 submit。

### 3. **生产建议：分离部署、版本回滚**

* 线上推荐 submitter 独立运行，主流程 jobmanager/taskmanager 先 ready，验证无误后再 submit，方便回滚和多版本部署。

---

## **常见 Q\&A**

* **Q: submitter 启动后报 “JAR file does not exist”？**
  **A:** 检查 jar 路径和 volume 挂载，确认 libs 目录和文件名一致，所有依赖都已映射到容器。

* **Q: 提交命令能否在本地直接跑？**
  **A:** 可以进入 jobmanager 容器手动 flink run，和 submitter 效果一样，区别只是自动化与否。

* **Q: Flink 依赖变更如何生效？**
  **A:** 重新 build jar，替换到 libs，重启 jobmanager/taskmanager/submitter，确保所有服务读到新版依赖。

---

## **一图流总结**

```mermaid
flowchart LR
    A[本地打包/收集依赖] --> B[libs目录集中依赖]
    B --> C[docker-compose挂载到各flink组件]
    C --> D[flink-submitter自动提交job]
    D --> E[UI/日志确认任务上线]
```

---

## **一句话经验终极版**

> Flink 生产作业自动化部署建议主 jar 和外部依赖全部用 volume 分开挂载、通过 submitter 自动提交，依赖升级或变动及时重启服务并检查 submitter 日志，切勿混用胖 jar 和“依赖打包进主 jar”，并始终保持 jar 路径一致、Kafka 服务名写对，才能避免99%的踩坑与环境不一致问题！
