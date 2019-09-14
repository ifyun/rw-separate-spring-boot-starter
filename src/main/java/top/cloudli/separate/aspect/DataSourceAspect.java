package top.cloudli.separate.aspect;

import top.cloudli.separate.datasource.DataSourceHolder;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Aspect
@Component
public class DataSourceAspect {

    @Resource
    private DataSourceHolder dataSourceHolder;

    /**
     * 写切入点
     */
    @Pointcut("@annotation(top.cloudli.separate.annotation.Master) ||" +
            "@annotation(top.cloudli.separate.annotation.Write)))")
    public void writePointcut() {
    }

    /**
     * 读切入点
     */
    @Pointcut("@annotation(top.cloudli.separate.annotation.Master) ||" +
            "@annotation(top.cloudli.separate.annotation.Read) ||" +
            "@annotation(top.cloudli.separate.annotation.Slave))")
    public void readPointcut() {
    }

    @Before("writePointcut()")
    public void setWriteDataSource() {
        dataSourceHolder.master();
    }

    @Before("readPointcut()")
    public void setReadDataSource() {
        dataSourceHolder.slave();
    }
}
