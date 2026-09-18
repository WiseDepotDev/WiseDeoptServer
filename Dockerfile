# 服务端镜像：WiseDepot Server（Spring Boot 3.2.5 / Java 17）
#
# 构建（构建上下文必须是 WiseDeoptServer 目录，以访问构建产物 jar）：
#   docker build -t wise-depot-server:0.0.23 WiseDeoptServer
#
# 说明：
# - 本镜像只打包构建产物 jar；**不含任何真实凭据**。所有连接信息与口令一律由
#   运行时环境变量注入（WISE_DB_URL / WISE_DB_PASSWORD / ...），见 application.yml。
# - config/application-local.yml 位于源码树之外（不在 jar 内），因此镜像天然不携带本机配置。
# - 缺失必需凭据时容器启动即失败并报出变量名，不会退化到弱口令（STD-SEC-01）。
# - 基础镜像取自可用的镜像站前缀（本机 Docker Hub 直连不可达）。

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="wise-depot-server" \
      org.opencontainers.image.description="WiseDepot 服务端（统一信封契约 / Spring Boot）" \
      org.opencontainers.image.version="0.0.23"

# 以非 root 运行（镜像内已有 curl 供 HEALTHCHECK 使用）
RUN groupadd --system --gid 1001 wise \
 && useradd --system --uid 1001 --gid wise --create-home wise

WORKDIR /app

COPY wise-deopt-api/target/wise-deopt-api-0.0.23.jar /app/wise-depot-server.jar

# 容器内没有 config/application-local.yml，Spring 自动跳过 local profile，配置全部来自环境变量
ENV SPRING_PROFILES_INCLUDE="" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

USER wise

EXPOSE 8080 6606

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD ["sh","-c","curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["java","-jar","/app/wise-depot-server.jar"]
