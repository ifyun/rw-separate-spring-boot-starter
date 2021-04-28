# Spring Boot 主从读写分离

![GitHub last commit](https://img.shields.io/github/last-commit/imcloudfloating/rw-separate-spring-boot-starter?style=flat-square)
![GitHub license](https://img.shields.io/github/license/imcloudfloating/rw-separate-spring-boot-starter?style=flat-square)
[![JitPack](https://img.shields.io/jitpack/v/github/imcloudfloating/rw-separate-spring-boot-starter?style=flat-square)](https://jitpack.io/#imcloudfloating/rw-separate-spring-boot-starter)

rw-separate-spring-boot-starter 是一个可以实现任意多个主从数据源读写分离的工具，
使用 Hikari 连接池（暂不支持其他连接池，后期可能会添加支持）。

> 本工具库只是替换了 Spring Boot 数据源，你可以随意选择 ORM 框架，JPA，MyBatis 都可以使用。

## 使用

Step 1. 添加 jitpack 到 pom.xml：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://www.jitpack.io</url>
    </repository>
</repositories>
```

Step 2. 添加依赖：

```xml
<dependency>
    <groupId>com.github.imcloudfloating</groupId>
    <artifactId>rw-separate-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

首先需要排除默认的 `DataSourceAutoConfiguration` ：

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FooApplication {

    public static void main(String[] args) {
        SpringApplication.run(FooApplication.class, args);
    }
}
```

> 不排除默认的 `DataSource` 会报冲突错误。

### application.yml 配置

示例：

```yaml
spring:
  separated-datasource:
    # 主库数据源
    masters:
      - dataSourceName: master_1
        hikari:
          driverClassName: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://10.0.0.100:3306/test
          username: root
          password: root

    # 从库数据源
    slaves:
      - dataSourceName: slave_1
        hikari:
          driverClassName: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://10.0.0.101:3306/test
          username: reader
          password: reader
    
      - dataSourceName: slave_2
        hikari:
          driverClassName: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://10.0.0.102:3306/test
          username: reader
          password: reader
```

- 以上配置文件中的 `hikari` 部分，可以参考 `HikariConfig` 类中的 setter 方法来填写（首字母小写）
- `dataSourceName` 为数据源的名称，请避免名称重复（如果不需要显式地指定数据源，可以不设置）
- 当设置多个数据源时，默认使用轮询的方式来切换数据源
- 可以只设置 `master` ，使用名称切换，当作多数据源使用，使用 `@Write("datasource")` 切换即可

### 使用注解实现读写分离

```java
@Service
public class FooService {

    @Resource
    FooMapper fooMapper;
    
    /**
     * 使用读库
     */
    @Read
    public List<Item> getFoo() {
        return fooMapper.getAll();
    }

    /**
     * 使用写库
     */
    @Write
    public int addFoo(Foo foo) {
        return fooMapper.add(foo);
    }
}
```

可以使用参数显式地指定数据源（需要在 `application.yml` 中指定 `dataSourceName`）:

```java
@Service
public class FooService {

    /**
     * 显式地指定数据源为 master_1
     */
    @Write("master_1")
    public int addFoo(Foo foo) {
        return fooMapper.add(foo);
    }

}
```

显式地指定数据源时，`@Write` 只能指定 masters 中的数据源，`@Read` 只能指定 slaves 中的数据源。

> 可以在 DAO 层使用，但是无法支持事务。
