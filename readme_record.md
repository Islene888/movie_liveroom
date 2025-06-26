
# 电影直播间 + 实时数据看板项目

## 简介

这是一个基于 Kafka + Flink + Spring Boot + WebSocket 的直播互动与实时数据统计项目，支持直播弹幕、点赞、送礼等互动事件的实时处理与大屏展示。

---
## 快速启动（Docker Compose）

确保本机已安装 Docker 和 Docker Compose。

在项目根目录，执行：


```bash
docker-compose up -d
```

该命令会启动所有关键组件：

* Zookeeper
* Kafka
* Flink 集群（JobManager + TaskManager）
* 后端服务（Spring Boot）
* 可能还有前端服务（如果前端容器化）

---

## 访问地址

* 后端 API 和 WebSocket：`http://localhost:8080`
* 前端页面（如果配置了前端容器）：例如 `http://localhost:3000`

---

## Flink 作业提交

启动后，需要将 Flink 实时作业提交到集群：

```bash
docker exec -it jobmanager flink run -c com.ella.flinkjob.FlinkEventTypeAggregator /opt/flink/usrlib/flinkjob-0.0.1-SNAPSHOT.jar
```

> 说明：
>
> * 主 Jar 包位于 `/opt/flink/usrlib/`
> * 类路径根据你的项目主类调整

---

## 配置注意点

* Kafka 地址配置在容器内一般不能写 `localhost:9092`，应使用服务名和端口，如 `kafka:29092`
* 修改 Kafka 地址或其它配置后，若配置在代码里，需重新构建镜像并重启容器；若通过环境变量注入，修改后重启容器即可生效
* Flink 和后端服务的依赖包需放在正确目录，确保启动正常

---

## 常见问题及排查

| 问题现象                       | 可能原因              | 解决方案                          |
| -------------------------- | ----------------- | ----------------------------- |
| Kafka 连接失败（localhost:9092） | 容器内无法访问 localhost | 改为 `kafka:29092` 等服务名访问       |
| Flink 任务提交失败               | Jar 包路径错误或依赖缺失    | 确认 Jar 包在 `/opt/flink/usrlib` |
| WebSocket 连接失败或断开          | 后端地址配置错误或服务未启动    | 确认后端服务启动并配置正确                 |
| 代码修改后配置无效                  | 镜像未重新构建，配置未生效     | 重新打包构建镜像并重启容器                 |

---

## 停止服务

```bash
docker-compose down
```

---

## 重新构建镜像示例

如果修改了代码（尤其是 Kafka 地址等硬编码配置），需要重新打包并构建镜像：

```bash
./mvnw clean package -DskipTests
docker build -t movie_liveroom-backend:latest ./backend
docker-compose up -d --no-deps --build backend
```

---




### 部署
* 1.上传
   ```sh
    scp -i ~/Desktop/good_luck.pem -r /Users/islenezhao/IdeaProjects/movie_liveroom ubuntu@18.216.138.180:~/
   ```
* 2.进入ec2

   ```sh
   ssh -i ~/Desktop/good_luck.pem ubuntu@ec2-3-15-170-2.us-east-2.compute.amazonaws.com
   cd ~/movie_liveroom
   ls -lh
   ```

* 3.安装 Docker & Docker Compose
   ```sh
    # 安装 Docker
     sudo apt-get update
     sudo apt-get install -y git curl docker.io docker-compose
     sudo usermod -aG docker $USER
  # 立即刷新docker权限
    newgrp docker
  
   rm -rf ~/movie_liveroom

  git clone https://github.com/Islene888/movie_liveroom.git

   cd movie_liveroom
      # flink打包
  cd ~/movie_liveroom/flinkjob
  mvn clean install

    # 后端打包
  cd ~/movie_liveroom/backend
  mvn clean package

  sudo apt-get install -y maven openjdk-17-jdk
  mvn clean package -DskipTests

   docker-compose down
  
   sudo docker-compose up -d
  #前端静态部署
   sudo apt-get install nginx
   sudo cp -r /home/islenezhao/movie_liveroom/frontend/* /var/www/html/
   sudo systemctl restart nginx

  #访问：http://34.134.39.193/
  
  

## **标准 Flink JobManager 手动启动流程总结**

1. **打包 jar**

   ```bash
   mvn clean package -DskipTests
   ```

2. **拷贝 jar 到 jobmanager 容器**

   ```bash
   docker cp target/flinkjob-0.0.1-SNAPSHOT.jar jobmanager:/opt/flink/
   ```

3. **进入容器**

   ```bash
   docker exec -it jobmanager bash
   ```

4. **运行 Flink 任务**

   ```bash
   cd /opt/flink
   ./bin/flink run -c com.ella.flinkjob.FlinkEventTypeAggregator flinkjob-0.0.1-SNAPSHOT.jar
   ```


---

## 一、本地项目打包与上传

1. **SSH 密钥管理**

    * 在 GCP 控制台复制公钥，追加到 VM 上的 `~/.ssh/authorized_keys`；
    * 在本地生成 ED25519 私钥，保存为 `~/.ssh/id_ed25519` 并 `chmod 600`；
    * 验证 `ssh -i ~/.ssh/id_ed25519` 能免密登录。
2. **文件传输**

    * 使用 `scp -i ~/.ssh/id_ed25519 movie_liveroom.zip …` 上传项目压缩包；
    * 在 VM 上安装 `unzip` 并解压。

---

## 二、Docker Compose 全家桶部署

* 编写了包含 Zookeeper、Kafka、Flink（JobManager/TaskManager）、StreamPark、后端、前端、可选存储组件等服务的 `docker-compose.yml`；
* 在 VM 上通过 `docker-compose up -d` 启动全套流处理平台；
* 验证各容器均处于 `Up (healthy)` 状态，前后端、Kafka、Flink 能互通。

---

## 三、磁盘扩容

* 通过 GCP 控制台将 VM 根盘由 10 GB 扩容到 20 GB；
* 在系统里运行 `df -h`，确认 `/dev/sda1` 容量生效，留出足够空间给 Docker 镜像与持久化数据。

---

## 四、Flink 作业提交与高可用思考

1. **直接命令行提交的挑战**

    * 在容器外 `docker exec jobmanager flink run …` 多次因 REST 广告地址（`0.0.0.0:8081`）无法连通而失败；
    * 进入容器内部手动执行能跑，但不便自动化。
2. **高可用设计原则**

    * **服务启动顺序**：保证 Kafka、JobManager 完全就绪再提交作业；
    * **作业自动恢复**：依赖 Flink 的 HA 模式（ZooKeeper 或文件系统存储 Checkpoints），即使 JobManager 重启也能自动恢复；
    * **避免单点操作**：不再人工 `flink run`，用脚本／定时器自动化提交／补提交。

---

## 五、 systemd 自动化服务配置

1. **全栈服务管理**

    * 新建 `/etc/systemd/system/movie_liveroom.service`：

      ```ini
      [Unit]
      Description=Movie LiveRoom Full Stack
      After=network.target
 
      [Service]
      Type=oneshot
      ExecStart=/usr/bin/docker-compose up -d
      RemainAfterExit=yes
 
      [Install]
      WantedBy=multi-user.target
      ```
    * 启用后，每次开机自动 `docker-compose up -d` 启动全部容器。
2. **Flink 作业提交**

    * **脚本** `submit-flink-job.sh`（放在 `/usr/local/bin`，可执行）：

      ```bash
      #!/usr/bin/env bash
      set -euo pipefail
      JM_REST_URL="http://localhost:8081"
      JAR_PATH="/home/islenezhao/movie_liveroom/flinkjob/target/flinkjob-0.0.1-SNAPSHOT.jar"
      MAIN_CLASS="com.ella.flinkjob.FlinkEventTypeAggregator"
 
      echo "Uploading jar…"
      UPLOAD_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/upload" -F "jarfile=@${JAR_PATH}")
      JAR_ID=$(echo "$UPLOAD_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["filename"].split("/")[-1])')
 
      echo "Submitting job…"
      RUN_JSON=$(curl -s -X POST "${JM_REST_URL}/jars/${JAR_ID}/run" \
        -H "Content-Type: application/json" \
        -d "{\"entryClass\":\"${MAIN_CLASS}\"}")
      JOB_ID=$(echo "$RUN_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["jobid"])')
 
      echo "Job submitted: ${JOB_ID}"
      ```
    * **Service 单元** `/etc/systemd/system/movie-flinkjob.service`：

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
    * 启用并启动后，开机即可自动通过 REST 上传 Jar 并提交。

---

## 六、定时器（Timer）补提交

* **Timer 单元** `/etc/systemd/system/movie-flinkjob.timer`：

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
* 启用后：

    * 开机后 2 分钟执行一次提交脚本，
    * 之后每 10 分钟自动重跑一次，保证作业不中断。

---

## 七、验证与效果

1. **服务状态**

   ```bash
   sudo systemctl status movie_liveroom.service
   sudo systemctl status movie-flinkjob.service
   sudo systemctl status movie-flinkjob.timer
   ```

   均为 `active (exited)` 或 `active (waiting)`，无失败。
2. **Flink 作业列表**

   ```bash
   docker exec jobmanager flink list -m jobmanager:6123
   curl http://localhost:8081/jobs/overview
   ```

   均能看到 `FlinkEventTypeAggregator` 正在 RUNNING。

---

### 小结

* **高可用**：依赖 Flink 原生 HA（ZooKeeper/checkpoint），并用定时脚本自动补提交。
* **systemd 自动化**：两套单元（服务 + 定时器），解放运维，开机即起，全程无人值守。
* **REST 提交**：摒弃 CLI 的 `0.0.0.0` 广告坑，直接走 HTTP API，兼容性更好。

