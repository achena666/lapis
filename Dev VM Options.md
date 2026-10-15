# 本地开发 JVM 参数（IDE → Run/Debug Configurations → VM options）

```text
-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss512k
```

## 说明（2026-10 调整）

原参数 `-Xms256m -Xmx256m -Xmn96m -Xss1024k` 的问题：

- `256m` 堆上限对 Spring Boot 4 + Spring Cloud 微服务过于紧张。
  单个服务启动后光是 Spring 容器、Hibernate/MyBatis 元数据、Eureka 客户端缓存就会占掉
  大部分空间，压测或批量操作时极易频繁 Full GC 甚至 OOM；
- `-Xmn96m` 显式固定年轻代，配合 256m 堆会让老年代只剩约 160m，进一步放大 GC 压力。
  交给 G1 自适应管理即可，不必手工指定；
- `-Xss1024k` 单线程栈 1MB，微服务默认线程栈 512k 足够，调小可在同样内存下容纳更多线程。

## 本地同时起多个服务时

本机需要同时运行 register / config / gateway / uaa / admin / eventlog 六个服务，
按 1GB 上限计算约需 6GB 可用内存。若机器内存吃紧，可把每个服务降到：

```text
-Xms256m -Xmx768m -XX:+UseG1GC -Xss512k
```

## 生产环境

生产环境不要使用这里的参数，建议容器化后通过环境变量设置，并让 JVM 感知容器内存限制：

```text
-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```
