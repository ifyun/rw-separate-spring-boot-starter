package top.cloudli.separate.aspect;

import org.aspectj.lang.annotation.After;
import top.cloudli.separate.annotation.Read;
import top.cloudli.separate.annotation.Write;
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
    @Pointcut("@annotation(top.cloudli.separate.annotation.Write)")
    public void writePointcut() {
    }

    /**
     * 读切入点
     */
    @Pointcut("@annotation(top.cloudli.separate.annotation.Read)")
    public void readPointcut() {
    }

    @Before("writePointcut() && @annotation(master)")
    public void setWriteDataSource(Write master) {
        dataSourceHolder.setMaster(master.value());
    }

    @Before("readPointcut() && @annotation(slave)")
    public void setReadDataSource(Read slave) {
        dataSourceHolder.setSlave(slave.value());
    }

    /**
     * 读操作完成后切回主库，避免没有使用注解的方法执行出错
     */
    @After("readPointcut()")
    public void setToWriteDataSource() {
        dataSourceHolder.setMaster("");
    }
}
