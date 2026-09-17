# 基础镜像：Java 21 精简版
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY target/*.jar app.jar

# 暴露服务端口
EXPOSE 8080 6606

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
