
* Docker Compose 配置模版（包含环境变量注入示例）
* Backend & Flink Kafka 连接配置示范
* 容器日志查看与排查详细流程
* 版本控制与镜像管理建议
* 常见问题排查技巧与调优建议

---

# 【详细】Docker Compose + Backend & Flink 配置与运维全指南

---

## 1. Docker Compose 配置模版（示例）

```yaml
version: '3.8'  # 版本可去掉，避免警告

services:

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.1
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.1
    container_name: kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
      - "29092:29092"

  jobmanager:
    image: flink:1.17.1-java11
    container_name: jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
    ports:
      - "8081:8081"
    command: jobmanager

  taskmanager:
    image: flink:1.17.1-java11
    container_name: taskmanager
    depends_on:
      - jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
    command: taskmanager

  flink-submitter:
    image: flink:1.17.1-java11
    container_name: flink-submitter
    depends_on:
      - jobmanager
    entrypoint: ["sleep", "infinity"]
    volumes:
      - ./flinkjob/target:/opt/flink/usrlib   # 挂载作业jar
      - ./libs:/opt/flink/lib                 # 挂载依赖jar
    networks:
      - appnet

  backend:
    image: movie_liveroom-backend:latest
    container_name: backend
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SERVER_PORT: 8080
    ports:
      - "8080:8080"
    depends_on:
      - kafka
    networks:
      - appnet

networks:
  appnet:
    driver: bridge
```

---

### 说明

* **Kafka 地址统一使用** `kafka:29092`，即 Docker Compose 服务名+容器内端口。
* **环境变量注入 Backend 容器**，避免写死配置。
* **Flink Submitter 容器挂载 Jar 包和依赖**，方便动态提交作业。
* **使用 bridge 网络**，保证容器间可以通过服务名访问。

---

## 2. Backend Kafka 连接配置示范

> 推荐通过环境变量配置，Spring Boot `application.properties` 使用：

```properties
spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:29092}
spring.kafka.consumer.group-id=test-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
server.port=${SERVER_PORT:8080}
```

> 代码中 Kafka Producer/Consumer 配置示例：

```java
Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:29092"));
props.put(ConsumerConfig.GROUP_ID_CONFIG, "agg-stats-consumer-group");
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
```

---

## 3. Flink Kafka 连接配置要点

* Flink 作业中 `bootstrap.servers` 配置为 `kafka:29092`
* Flink 连接依赖（Kafka Connector 等）放置于 `/opt/flink/lib/`，且版本必须与 Flink 兼容
* 提交作业时，Jar 包路径在 Flink 容器内正确挂载
* Kafka Topic 权限、分区、数据格式统一确认

---

## 4. 容器日志查看及排查流程

### 4.1 查看容器状态

```bash
docker ps -a
```

### 4.2 查看具体容器日志

```bash
docker-compose logs backend
docker-compose logs jobmanager
docker-compose logs taskmanager
docker-compose logs flink-submitter
docker-compose logs kafka
docker-compose logs zookeeper
```

* 可以加 `-f` 实时追踪日志，如 `docker-compose logs -f backend`
* 关键日志关键词：`Kafka connection`, `Consumer`, `Producer`, `WebSocket`, `ERROR`, `WARN`

### 4.3 进入容器调试

```bash
docker-compose exec backend bash
docker-compose exec jobmanager bash
```

### 4.4 常用调试命令

* 查看网络连通性

```bash
ping kafka
telnet kafka 29092
```

* 查看文件是否存在

```bash
ls /opt/flink/usrlib
ls /opt/flink/lib
```

---

## 5. 版本控制与镜像管理建议

* 镜像打标签，例如 `movie_liveroom-backend:v1.0.0`，避免 latest 混淆
* 保留旧版本镜像，方便回滚
* 每次重大配置或代码变动，更新版本号并构建新镜像
* Dockerfile 内尽量少写死配置，使用环境变量

---

## 6. 常见问题排查建议

| 问题描述                                   | 原因及建议                                        |
| -------------------------------------- | -------------------------------------------- |
| 连接 Kafka 报错 `Connection to node -1...` | Kafka 地址配置错误（如写成 localhost）应改为 `kafka:29092` |
| Kafka 消息收发失败                           | Kafka 容器未启动或网络未连通，检查容器状态和端口映射                |
| Flink 作业启动失败，缺少类或方法                    | 依赖包未放置或版本不匹配，确认 `/opt/flink/lib/` 下依赖版本      |
| 配置修改后无效                                | 修改代码配置后未重构镜像或未重启容器                           |
| WebSocket 连接异常或断开                      | Kafka 连接失败导致消息发送阻塞，检查 Kafka 配置与状态            |

---

## 7. 优化建议与扩展思路

* **配置中心或环境变量管理**：引入 Spring Cloud Config 或 Kubernetes ConfigMap 统一管理配置
* **监控与告警**：集成 Prometheus + Grafana，监控 Kafka、Flink、Backend 服务健康和消息吞吐
* **自动化 CI/CD**：结合 GitHub Actions / Jenkins 实现自动构建镜像和部署
* **日志聚合**：使用 ELK/EFK 集中采集日志，方便搜索和定位问题
* **多环境分离**：开发、测试、生产环境配置隔离，避免混用

