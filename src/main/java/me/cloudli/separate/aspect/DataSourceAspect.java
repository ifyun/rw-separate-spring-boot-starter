package me.cloudli.separate.aspect;

import me.cloudli.separate.datasource.DataSourceHolder;
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
    @Pointcut("@annotation(me.cloudli.separate.annotation.Master) ||" +
            "@annotation(me.cloudli.separate.annotation.Write)))")
    public void writePointcut() {
    }

    /**
     * 读切入点
     */
    @Pointcut("@annotation(me.cloudli.separate.annotation.Master) ||" +
            "@annotation(me.cloudli.separate.annotation.Read) ||" +
            "@annotation(me.cloudli.separate.annotation.Slave))")
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
