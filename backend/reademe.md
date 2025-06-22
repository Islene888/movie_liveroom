
# **Backend 模块说明**

本模块是 **Spring Boot 后端服务**，实现了直播间弹幕事件的 WebSocket 推送、事件入 Kafka、聚合统计数据的实时推送。可作为大数据/实时数据产品的基础骨架。

## 目录结构

```
src/main/java/com/ella/backend/
├── config/
│   └── WebSocketConfig.java        # WebSocket 配置，注册 ws 路由
├── consumer/
│   └── KafkaAggConsumer.java       # Kafka 消费聚合统计数据，推送给前端
├── dto/
│   └── LiveEvent.java              # 弹幕/互动事件的数据结构定义
├── handler/
│   └── LiveWebSocketHandler.java   # 处理 WebSocket 连接和消息，入 Kafka 并群发消息
├── service/
│   ├── KafkaService.java           # 提供发送消息到 Kafka 的功能
│   └── StatsWebSocketHandler.java  # 统计数据推送到 WebSocket，聚合后展示
└── BackendApplication.java         # Spring Boot 启动入口
```

---

## 各文件功能说明

### 1. `config/WebSocketConfig.java`

* **功能**：配置 WebSocket 路由，将 `/ws` 端点和 `LiveWebSocketHandler` 绑定，实现主弹幕通道。

### 2. `consumer/KafkaAggConsumer.java`

* **功能**：消费 `live-events-agg` Kafka Topic 里的聚合统计数据，解析并推送实时累计统计给前端 UI（通常用于实时看板/可视化）。

### 3. `dto/LiveEvent.java`

* **功能**：定义前端/后端交互用的事件数据结构，包括用户ID、事件类型、时间戳和事件内容（如弹幕文本、礼物等）。

### 4. `handler/LiveWebSocketHandler.java`

* **功能**：负责 WebSocket 连接的管理和消息收发。

    * 新连接欢迎
    * 收到消息后群发给所有连接
    * 入 Kafka

### 5. `service/KafkaService.java`

* **功能**：封装 Kafka 消息发送逻辑，将 WebSocket 收到的消息写入 Kafka Topic（`live-events`）。

### 6. `service/StatsWebSocketHandler.java`

* **功能**：聚合和累计统计（如总点赞数、评论数、送礼数、进房数），并推送给前端。被聚合 Kafka 消费者和业务调用。

### 7. `BackendApplication.java`

* **功能**：Spring Boot 项目入口，启动后端服务。

---

## 典型业务流程

1. **前端**通过 WebSocket `/ws` 发送弹幕/互动事件消息到服务端
2. **LiveWebSocketHandler** 将消息群发，并通过 `KafkaService` 投递到 Kafka（`live-events` Topic）
3. **Flink 作业** 消费 `live-events`、聚合后写入 `live-events-agg` Topic
4. **KafkaAggConsumer** 消费聚合数据，通过 `StatsWebSocketHandler` 推送给所有前端用户，实现“直播互动实时大屏”

---

## 扩展说明

* 如果你要扩展新事件类型、聚合方式，只需扩展 `LiveEvent` 结构，调整 Flink 聚合作业，及前端展示逻辑即可。
* 如有疑问可随时联系开发者或提 issue！
