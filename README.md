# Spring Boot 主从读写分离

rw-separate-spring-boot-starter 是一个可以实现任意多个主从数据源读写分离的工具，
使用 Hikari 连接池（暂不知持其他连接池）。

首先需要排除默认的 `DataSourceAutoConfiguration` ：

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FooApplication {

    public static void main(String[] args) {
        SpringApplication.run(FooApplication.class, args);
    }

}
```

## HikariCP 配置

hikari 的配置没有变化，与 Spring Boot 默认的配置方式一样：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 50
      maximum-pool-size: 500
      connection-test-query: "SELECT 1"
      ...
```

## MyBatis 数据源配置

自动装配的数据源为 `routingDataSource`。

```java
@Configuration
@EnableTransactionManagement
public class MyBatisConfig {

    // 注入 routingDataSource
    @Resource(name = "routingDataSource")
    private DataSource dataSource;

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();

        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mybatis/mapper/*.xml"));

        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

## 使用注解实现读写分离

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

## application.yml

```yaml
spring:
  separated-datasource:
    # 主库数据源
    masters:
      - {
        dataSourceName: master_1,
        driverClassName: com.mysql.cj.jdbc.Driver,
        url: jdbc:mysql://10.0.0.100:3306/test,
        username: root,
        password: root
      }

    # 从库数据源
    slaves:
      - {
        dataSourceName: slave_1,
        driverClassName: com.mysql.cj.jdbc.Driver,
        url: jdbc:mysql://10.0.0.101:3306/test,
        username: reader,
        password: reader
      }
      - {
        dataSourceName: slave_2,
        driverClassName: com.mysql.cj.jdbc.Driver,
        url: jdbc:mysql://10.0.0.102:3306/test,
        username: reader,
        password: reader
      }
```

- `dataSourceName` 为数据源的名称，请避免名称重复（如果不需要显式地指定数据源，可以不设置）。
- 当设置多个数据源时，默认使用轮询的方式来切换数据源。
