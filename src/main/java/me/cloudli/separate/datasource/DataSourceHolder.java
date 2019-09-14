package me.cloudli.separate.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class DataSourceHolder {
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    @Resource
    private List<String> masterNames;

    @Resource
    private List<String> slaveNames;

    private static void set(String type) {
        contextHolder.set(type);
    }

    static String get() {
        return contextHolder.get();
    }

    public void master() {
        setDataSource(masterNames);
    }

    public void slave() {
        setDataSource(slaveNames);
    }

    /**
     * <p>切换数据源，如果有多个数据源，使用轮训的方式</p>
     * @param nameList 数据源名称集合
     */
    private static void setDataSource(List<String> nameList) {
        if (nameList.size() == 1) {
            set(nameList.get(0));
            log.info("Using {} DataSource.", nameList.get(0));
            return;
        }

        int index = 0;
        for (int i = 0; i < nameList.size(); i++) {
            int nextIndex = (index + 1) % nameList.size();
            set(nameList.get(index));
            log.info("Using {} DataSource.", nameList.get(index));
            index = nextIndex;
        }
    }
}
