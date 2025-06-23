
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
   ```
  