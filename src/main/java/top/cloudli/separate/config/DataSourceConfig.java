package top.cloudli.separate.config;

import lombok.Data;

import java.util.Properties;

@Data
public class DataSourceConfig {
    private String dataSourceName;
    private Properties hikari;
}
