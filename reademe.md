
# Movie LiveRoom + Real-Time Data Dashboard

> Kafka + Flink + Spring Boot + WebSocket + Docker Compose + systemd HA automation
> Real-time event ingestion, analytics, and dashboard for interactive live streaming.

---

## ⭐ Project Overview

This project provides a **full-stack real-time analytics platform** for live streaming scenarios:

* **Kafka**: Real-time event ingestion (chat, like, gift, etc.)
* **Flink**: Streaming analytics, aggregation, and ETL
* **Spring Boot**: Backend API & WebSocket push
* **React/Vue**: (Optional) Real-time data dashboard
* **Docker Compose**: One-click stack deployment
* **systemd**: High-availability (HA), automatic startup, periodic job submission

---

## 🏁 Quick Start (Dev/Test)

### 1. Requirements

* Docker & Docker Compose
* Recommended: Linux (Ubuntu 20.04+ or similar)

### 2. Deploy all services

```bash
git clone https://github.com/Islene888/movie_liveroom.git
cd movie_liveroom
docker-compose up -d
```

Services include: Zookeeper, Kafka, Flink (JobManager/TaskManager), Backend API, (Frontend dashboard optional)

### 3. Service Endpoints

| Service          | Public Access URL / Note                               |
| ---------------- |--------------------------------------------------------|
| Backend API      | [http://34.45.211.142:8080](http://34.45.211.142:8080) |
| Frontend UI      | [http://34.45.211.142:3000](http://34.45.211.142:3000) |
| Flink UI         | [http://34.45.211.142:8081](http://34.45.211.142:8081) |
| Kafka (internal) | Use `kafka:29092` **inside containers only**           |

---

## 🛰️ Flink Job Submission & Automation

### Manual Flink job submission

1. **Build your JAR**

   ```bash
   cd flinkjob
   mvn clean package -DskipTests
   ```

2. **Copy & submit**

   ```bash
   docker cp target/flinkjob-0.0.1-SNAPSHOT.jar jobmanager:/opt/flink/usrlib/
   docker exec -it jobmanager flink run -c com.ella.flinkjob.FlinkEventTypeAggregator /opt/flink/usrlib/flinkjob-0.0.1-SNAPSHOT.jar
   ```

### Recommended: Automated job submit via systemd

* Place `submit-flink-job.sh` in `/usr/local/bin/`
* Create `/etc/systemd/system/movie-flinkjob.service` to automate submission on boot or timer
* (See below for HA / systemd config)

#### Sample submission script

```bash
#!/usr/bin/env bash
set -euo pipefail
JM_REST_URL="http://localhost:8081"
JAR_PATH="/home/islenezhao/movie_liveroom/flinkjob/target/flinkjob-0.0.1-SNAPSHOT.jar"
MAIN_CLASS="com.ella.flinkjob.FlinkEventTypeAggregator"
UPLOAD_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/upload" -F "jarfile=@${JAR_PATH}")
JAR_ID=$(echo "$UPLOAD_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["filename"].split("/")[-1])')
RUN_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/${JAR_ID}/run" -H "Content-Type: application/json" -d "{\"entryClass\":\"${MAIN_CLASS}\"}")
echo "Flink Job submitted: $(echo $RUN_JSON | python3 -c 'import sys,json;print(json.load(sys.stdin).get("jobid",""))')"
```

---

## ⚙️ systemd Service & HA Ops

### 1. Full stack compose service

`/etc/systemd/system/movie_liveroom.service`:

```ini
[Unit]
Description=Movie LiveRoom Full Stack
After=network.target

[Service]
Type=oneshot
WorkingDirectory=/home/islenezhao/movie_liveroom
ExecStart=/usr/bin/docker-compose up -d
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable movie_liveroom.service
sudo systemctl start movie_liveroom.service
```

### 2. Flink job submit via systemd

`/etc/systemd/system/movie-flinkjob.service`:

```ini
[Unit]
Description=Submit Flink Job via REST API
After=movie_liveroom.service
Requires=movie_liveroom.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/submit-flink-job.sh

[Install]
WantedBy=multi-user.target
```

**Optional timer** (`/etc/systemd/system/movie-flinkjob.timer`) for auto-resubmit every 10 minutes:

```ini
[Unit]
Description=Run Flink submit script every 10 minutes
[Timer]
OnBootSec=2min
OnUnitActiveSec=10min
Persistent=true
[Install]
WantedBy=timers.target
```

---

## 🧩 Config & Troubleshooting

* **Kafka address:** Use `kafka:29092` (not `localhost:9092`) inside containers
* **App/config changes:** Rebuild corresponding Docker images, restart containers
* **Large files:** Use `.gitignore` to skip logs/jars, or [Git LFS](https://git-lfs.github.com/)
* **Git push failures:** For files >100MB, see LFS or remove files from history

---

## 💡 FAQ / DevOps Tips

| Problem                                   | Solution                                       |
| ----------------------------------------- | ---------------------------------------------- |
| Flink CLI "REST advertised address" error | Use REST API submission script (see above)     |
| Kafka "localhost:9092" connection fails   | Use service name\:port, e.g. `kafka:29092`     |
| App/image changes not reflected           | Rebuild image, `docker-compose up -d`          |
| Container crash/restart                   | Handled by systemd + Docker Compose            |
| Flink job missing after restart           | Use systemd timer + REST script for auto-resub |
| Git push fails for large files            | Use Git LFS or exclude in `.gitignore`         |

---

## 📦 Recommended Production Enhancements

* External DBs (MySQL, ClickHouse) should use persistent storage/volumes
* Frontend can be served by nginx as static files for performance
* For true Flink HA, enable checkpoints + ZooKeeper

---

# 电影直播间 + 实时数据看板

> Kafka + Flink + Spring Boot + WebSocket + Docker Compose + systemd 自动高可用
> 面向互动直播场景的弹幕、点赞等实时事件采集、处理和大屏可视化展示

---

## ⭐ 项目简介

本项目是一个**全栈实时数据分析平台**，用于直播互动、实时统计与大屏展示，技术栈包括：

* **Kafka**：采集实时弹幕、点赞、送礼等事件
* **Flink**：流式分析、聚合与 ETL
* **Spring Boot**：后端 API 与 WebSocket 推送
* **React/Vue**（可选）：实时数据可视化大屏
* **Docker Compose**：一键全栈部署
* **systemd**：高可用后台托管、自动重启与作业定时提交

---

## 🏁 快速启动（开发/测试环境）

### 1. 环境要求

* Docker & Docker Compose
* 建议 Linux（如 Ubuntu 20.04+）

### 2. 一键启动全栈服务

```bash
git clone https://github.com/Islene888/movie_liveroom.git
cd movie_liveroom
docker-compose up -d
```

启动后包括：Zookeeper、Kafka、Flink（JobManager/TaskManager）、后端服务、（可选前端大屏）

### 3. 服务访问入口

| 服务          | 公网访问地址                                                 |
| ----------- | ------------------------------------------------------ |
| 后端 API      | [http://34.45.211.142:8080](http://34.45.211.142:8080) |
| 前端大屏        | [http://34.45.211.142:3000](http://34.45.211.142:3000) |
| Flink UI    | [http://34.45.211.142:8081](http://34.45.211.142:8081) |
| Kafka（容器内部） | 仅限容器间用 `kafka:29092`，主机无法直接访问                          |

---

## 🛰️ Flink 作业提交与自动化

### 手动提交 Flink 任务

1. **打包 JAR**

   ```bash
   cd flinkjob
   mvn clean package -DskipTests
   ```

2. **拷贝并提交**

   ```bash
   docker cp target/flinkjob-0.0.1-SNAPSHOT.jar jobmanager:/opt/flink/usrlib/
   docker exec -it jobmanager flink run -c com.ella.flinkjob.FlinkEventTypeAggregator /opt/flink/usrlib/flinkjob-0.0.1-SNAPSHOT.jar
   ```

### 推荐方式：systemd 自动提交（高可用）

* 将 `submit-flink-job.sh` 放到 `/usr/local/bin/`
* 配置 `/etc/systemd/system/movie-flinkjob.service`，支持开机自动/定时提交
* 定时器可自动补交任务，提升高可用

#### 示例自动提交脚本

```bash
#!/usr/bin/env bash
set -euo pipefail
JM_REST_URL="http://localhost:8081"
JAR_PATH="/home/islenezhao/movie_liveroom/flinkjob/target/flinkjob-0.0.1-SNAPSHOT.jar"
MAIN_CLASS="com.ella.flinkjob.FlinkEventTypeAggregator"
UPLOAD_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/upload" -F "jarfile=@${JAR_PATH}")
JAR_ID=$(echo "$UPLOAD_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["filename"].split("/")[-1])')
RUN_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/${JAR_ID}/run" -H "Content-Type: application/json" -d "{\"entryClass\":\"${MAIN_CLASS}\"}")
echo "Flink Job submitted: $(echo $RUN_JSON | python3 -c 'import sys,json;print(json.load(sys.stdin).get("jobid",""))')"
```

---

## ⚙️ systemd 服务与高可用运维

### 1. 全栈 compose 服务托管

`/etc/systemd/system/movie_liveroom.service`:

```ini
[Unit]
Description=Movie LiveRoom Full Stack
After=network.target

[Service]
Type=oneshot
WorkingDirectory=/home/islenezhao/movie_liveroom
ExecStart=/usr/bin/docker-compose up -d
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable movie_liveroom.service
sudo systemctl start movie_liveroom.service
```

### 2. Flink 作业自动提交（含定时补交）

`/etc/systemd/system/movie-flinkjob.service`:

```ini
[Unit]
Description=Submit Flink Job via REST API
After=movie_liveroom.service
Requires=movie_liveroom.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/submit-flink-job.sh

[Install]
WantedBy=multi-user.target
```

**可选：定时器（每10分钟补提交）** `/etc/systemd/system/movie-flinkjob.timer`:

```ini
[Unit]
Description=Run Flink submit script every 10 minutes
[Timer]
OnBootSec=2min
OnUnitActiveSec=10min
Persistent=true
[Install]
WantedBy=timers.target
```

---

## 🧩 配置说明与常见问题

* **Kafka 地址**：容器内部统一用 `kafka:29092`，不要用 localhost:9092
* **代码/配置变动**：重建镜像并重启容器
* **大文件管理**：日志/jar/zk-log 等应 `.gitignore`，或用 [Git LFS](https://git-lfs.github.com/)
* **Git 大文件 push 报错**：超 100MB 建议 LFS 或从历史记录中移除

---

## 💡 FAQ / 运维经验

| 问题现象                      | 解决办法                                |
| ------------------------- | ----------------------------------- |
| Flink CLI “REST 广告地址”报错   | 推荐用 REST API 脚本提交任务                 |
| Kafka “localhost:9092”连不上 | 用 `kafka:29092` 容器服务名端口             |
| 镜像/配置变更无效                 | 重新 build 镜像并 `docker-compose up -d` |
| 容器挂掉自动重启                  | 交给 systemd + Docker Compose         |
| Flink 作业重启后消失             | systemd timer + REST 脚本自动补交         |
| Git 推送大文件失败               | 用 Git LFS 或 `.gitignore` 排除         |

---

## 📦 生产部署建议

* MySQL、ClickHouse 等外部 DB 推荐挂载数据盘持久化
* 前端静态页面建议用 nginx 服务
* Flink 生产环境建议开启 checkpoint + ZooKeeper 实现真正 HA

