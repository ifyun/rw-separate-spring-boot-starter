# Spring Boot 主从读写分离

可以实现任意多个主从数据源读写分离，配置文件模板如下：

```yaml
spring:
  separated-datasource:
    masters:
      - {
        dataSourceName: master_1,
        driverClassName: com.mysql.cj.jdbc.Driver,
        url: jdbc:mysql://10.0.0.100:3306/test,
        username: root,
        password: root
      }

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

- `dataSourceName` 为数据源的名称，可以不指定，若指定请避免名称重复。
- 当设置多个数据源时（n 个主，m 个从），使用轮询的方式来切换数据源。
- 自动装配的数据源 Bean 的名称为 routingDataSource。
- 只支持 HikariCP 连接池。

需要排除默认的 `DataSourceAutoConfiguration` 
使用 `@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})`

HikariCP 配置：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 50
      maximum-pool-size: 500
      connection-test-query: "SELECT 1"
      ...
```

## 配置 MyBatis 数据源

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

    @Read
    public List<Item> getFoo() {
        return fooMapper.getAll();
    }

    @Write
    public int addFoo(Foo foo) {
        return fooMapper.add(foo);
    }
}
```

`@Read` 或 `@Slave` 表示切换到读库  
`@Write` 或 `@Master` 表示切换到写库  

> 可以在 DAO 层使用，但是无法支持事务。