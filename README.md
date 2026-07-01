# MemCredentialDumper

MemCredentialDumper 是一款 Godzilla 插件，注入目标 JVM 后一键提取内存中的数据库连接、中间件密钥、云服务 AK/SK等敏感凭据，全程无文件落地。
<img width="1272" height="645" alt="0880c08b011087908541" src="https://github.com/user-attachments/assets/ff03e93a-99d6-4ed5-9de9-e1199978e43b" />


## 免责声明

本工具**仅限**企业内部授权的安全评估、红蓝对抗、设备/系统自身的审计检查等合法场景使用。

在未获得目标系统明确授权的情况下，使用本工具对任何系统进行凭据提取或其他形式的检测，均可能违反《中华人民共和国网络安全法》《中华人民共和国数据安全法》等相关法律法规。

使用者须确保已获得被测系统所属单位/组织的书面授权。任何未经授权使用本工具所引发的法律责任及连带损失，均由使用者本人自行承担，与工具作者无关。

下载、复制或以任何形式使用本工具源代码及二进制文件的行为，即表示您已阅读并同意上述声明。

## 支持的凭据类型

- Spring Environment 属性（application.yml/application.properties 里的各种 key）
- DataSource（JDBC 连接串、用户名、密码，支持 javax.sql/jakarta.sql）
- Druid DataSource（含 config.decrypt.key）
- Redis（JedisPool / RedisStandaloneConfiguration）
- Shiro RememberMe Key + 加密算法
- MongoDB（MongoClient / MongoClientSettings，含用户名密码和 authSource）
- LDAP（LdapContextSource）
- OAuth2 Client Registrations
- Kafka（KafkaProperties，含 sasl.jaas.config）
- JWT Secret（扫描 Bean，匹配包含 jwt/token 的名称）
- Spring Security Authentication（当前线程上下文的认证信息）
- Sa-Token（SaTokenConfig / StpLogic）
- Jasypt Encryptor（StandardPBEStringEncryptor / PooledPBEStringEncryptor）
- 云服务：阿里云 OSS、腾讯云 COS、MinIO、七牛
- Nacos（Config / Discovery / ClientWorker）

## 编译

依赖 Godzilla 的 JAR，pom.xml 里用 system scope 引用，需要改成你本地的路径：

```xml
<systemPath>D:\desktop\Godzilla-tezhan.jar</systemPath>
```

然后：

```
mvn clean package
```

输出到 `target/MemCredentialDumper-1.0.jar`。

## 使用

1. 在 Godzilla 里加载生成的 JAR
2. 连接目标 Shell
3. 打开 MemCredentialDumper 插件面板，点 "Extract Credentials"
4. 结果会显示在下面的文本框里

## 原理

分两部分：

**客户端（MemCredentialDumper.java）**：Godzilla 插件 UI，把 `HashMemDumper.class` 和 `HashBootstrap.class` 的字节码通过 `payload.include()` 发给目标，然后调用 `HashBootstrap.run`。

**目标端（HashBootstrap + HashMemDumper）**：HashBootstrap 负责在目标 ClassLoader 里 define 这两个类，然后 HashMemDumper 通过 `findApplicationContext` 找 Spring 容器，再逐个提取器从 Bean 里反射拿凭据。

## JDK 兼容

- 源码 target 是 JDK 6，兼容大部分老环境
- 高版本 JDK（9+ / 17+）通过三层 fallback 加载类：
  1. `ClassLoader.defineClass` 反射
  2. `sun.misc.Unsafe.defineClass`
  3. `MethodHandles.Lookup.defineClass`

## 注意事项

- 如果目标之前加载过旧版 HashMemDumper，需要重启目标 JVM 才能用上新代码。因为 `loadClass` 优先返回已加载的类，不会重新 define。
- 提取能力取决于目标 classpath 里有没有对应框架的类，缺对应依赖的提取器会自动跳过。
- 结果里的密码是明文，注意保存和传输安全。
