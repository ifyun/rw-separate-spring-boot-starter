package top.cloudli.separate.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import top.cloudli.separate.datasource.DataSourceProperties;
import top.cloudli.separate.datasource.RoutingDataSource;
import top.cloudli.separate.datasource.SeparatedDataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置主从数据源
 */
@Configuration
@EnableConfigurationProperties(SeparatedDataSourceProperties.class)
public class RoutingDataSourceAutoConfiguration {

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.validation-timeout:5000}")
    private long validationTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:60000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.leak-detection-threshold:0}")
    private long leakDetectionThreshold;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:-1}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-init-sql:}")
    private String connectionInitSql;

    @Value("${spring.datasource.hikari.connection-test-query:}")
    private String connectionTestQuery;

    private final SeparatedDataSourceProperties separatedDataSourceProperties;

    private List<String> masterNames, slaveNames;

    public RoutingDataSourceAutoConfiguration(SeparatedDataSourceProperties separatedDataSourceProperties) {
        this.separatedDataSourceProperties = separatedDataSourceProperties;
    }

    /**
     * @return 主库数据源集合
     */
    @Bean
    public List<? extends DataSource> masterDataSources() {
        List<DataSource> masterDataSources = new ArrayList<>();
        masterNames = new ArrayList<>();

        for (DataSourceProperties properties : separatedDataSourceProperties.getMasters()) {
            setDataSources(masterDataSources, masterNames, properties);
        }

        return masterDataSources;
    }

    /**
     * @return 从库数据源集合
     */
    @Bean
    public List<? extends DataSource> slaveDataSources() {
        List<DataSource> slaveDataSources = new ArrayList<>();
        slaveNames = new ArrayList<>();

        for (DataSourceProperties properties : separatedDataSourceProperties.getSlaves()) {
            setDataSources(slaveDataSources, slaveNames, properties);
        }

        return slaveDataSources;
    }

    /**
     * @return 主库数据源的名称集合
     */
    @Bean
    @DependsOn("masterDataSources")
    public List<String> masterNames() {
        return masterNames;
    }

    /**
     * @return 从库数据源的名称集合
     */
    @Bean
    @DependsOn("slaveDataSources")
    public List<String> slaveNames() {
        return slaveNames;
    }

    /**
     * <p>设置数据源</p>
     *
     * @param dataSources     数据源集合
     * @param dataSourceNames 数据源名称集合
     * @param properties      数据源的配置
     */
    private void setDataSources(List<DataSource> dataSources,
                                List<String> dataSourceNames,
                                DataSourceProperties properties) {

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(properties.getDriverClassName());
        hikariConfig.setJdbcUrl(properties.getUrl());
        hikariConfig.setUsername(properties.getUsername());
        hikariConfig.setPassword(properties.getPassword());

        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setValidationTimeout(validationTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setLeakDetectionThreshold(leakDetectionThreshold);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionInitSql(connectionInitSql);
        hikariConfig.setConnectionTestQuery(connectionTestQuery);

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        dataSources.add(dataSource);
        String name = properties.getDataSourceName();
        // 如果没有配置数据源名称，用数据源内存地址作为名称
        dataSourceNames.add(name == null ? dataSource.toString() : name);
    }

    /**
     * <p>将数据源加入到 RoutingDataSource</p>
     *
     * @param masters 主库数据源
     * @param slaves  从库数据源
     * @return {@link RoutingDataSource}
     */
    @Bean
    @DependsOn({"masterDataSources", "slaveDataSources"})
    public DataSource routingDataSource(@Qualifier("masterDataSources") List<? extends DataSource> masters,
                                        @Qualifier("slaveDataSources") List<? extends DataSource> slaves) {
        Map<Object, Object> targetDataSources = new HashMap<>();

        for (int i = 0; i < masters.size(); i++) {
            targetDataSources.put(masterNames.get(i), masters.get(i));
        }

        for (int i = 0; i < slaves.size(); i++) {
            targetDataSources.put(slaveNames.get(i), slaves.get(i));
        }

        RoutingDataSource routingDataSource = new RoutingDataSource();

        // 设置默认数据源
        routingDataSource.setDefaultTargetDataSource(masters.get(0));
        routingDataSource.setTargetDataSources(targetDataSources);

        return routingDataSource;
    }
}
