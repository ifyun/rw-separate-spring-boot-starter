package me.cloudli.separate.datasource;

import lombok.Data;

@Data
public class DataSourceProperties {
    private String dataSourceName;
    private String driverClassName;
    private String url;
    private String username;
    private String password;
}
