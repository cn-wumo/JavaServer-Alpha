package server.catalina;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.*;

/**
* Filter的配置类，存放Filter的初始化参数
* @author cn-wumo
* @since 2021/4/26
*/
public class StandardFilterConfig implements FilterConfig {
    private final ServletContext servletContext;    //web应用程序的全局上下文
    private final Map<String, String> initParameters; //待初始化的参数列表
    private final String filterName;    //拦截器的名称

    public StandardFilterConfig(ServletContext servletContext, String filterName,
                                Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.filterName = filterName;
        this.initParameters = Objects.requireNonNullElseGet(initParameters, HashMap::new);
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

}