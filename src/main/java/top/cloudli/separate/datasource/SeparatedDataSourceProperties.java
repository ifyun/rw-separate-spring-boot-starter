package top.cloudli.separate.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix="spring.separated-datasource")
public class SeparatedDataSourceProperties {
    List<DataSourceProperties> masters;
    List<DataSourceProperties> slaves;
}
