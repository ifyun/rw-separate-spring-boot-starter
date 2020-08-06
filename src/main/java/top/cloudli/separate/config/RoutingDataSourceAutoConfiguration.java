package top.cloudli.separate.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import top.cloudli.separate.datasource.RoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.util.*;

/**
 * 配置主从数据源
 */
@Configuration
@EnableConfigurationProperties(SeparatedDataSourceConfig.class)
public class RoutingDataSourceAutoConfiguration {

    private final SeparatedDataSourceConfig separatedDataSourceConfig;

    private List<String> masterNames, slaveNames;

    public RoutingDataSourceAutoConfiguration(SeparatedDataSourceConfig separatedDataSourceConfig) {
        this.separatedDataSourceConfig = separatedDataSourceConfig;
    }

    /**
     * @return 主库数据源集合
     */
    @Bean
    public List<? extends DataSource> masterDataSources() {
        List<DataSource> masterDataSources = new ArrayList<>();
        masterNames = new ArrayList<>();

        for (DataSourceConfig config : separatedDataSourceConfig.getMasters()) {
            setDataSources(masterDataSources, masterNames, config);
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

        if (separatedDataSourceConfig.getSlaves() != null) {
            for (DataSourceConfig config : separatedDataSourceConfig.getSlaves()) {
                setDataSources(slaveDataSources, slaveNames, config);
            }
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
     * @param config      数据源的配置
     */
    private void setDataSources(List<DataSource> dataSources,
                                List<String> dataSourceNames,
                                DataSourceConfig config) {
        HikariDataSource dataSource = new HikariDataSource(new HikariConfig(config.getHikari()));

        dataSources.add(dataSource);
        String name = config.getDataSourceName();
        // 如果没有配置数据源名称，使用 Hikari 自己生成的名称
        dataSourceNames.add(name == null ? dataSource.toString() : name);
    }

    /**
     * <p>将数据源加入到 RoutingDataSource</p>
     *
     * @param masters 主库数据源
     * @param slaves  从库数据源
     * @return {@link RoutingDataSource} 包含主从数据源的 DataSource
     */
    @Bean
    @DependsOn({"masterDataSources", "slaveDataSources"})
    public DataSource dataSource(@Qualifier("masterDataSources") List<? extends DataSource> masters,
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
