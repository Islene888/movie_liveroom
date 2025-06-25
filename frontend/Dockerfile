# 基于官方 nginx 镜像
FROM nginx:alpine

# 拷贝前端所有文件到 nginx 默认站点
COPY . /usr/share/nginx/html

# 默认 80 端口（无需修改）
EXPOSE 80

# Nginx 默认启动命令，无需修改
CMD ["nginx", "-g", "daemon off;"]
