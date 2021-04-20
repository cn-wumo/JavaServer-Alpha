package server.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.*;

/**
* 标准的servlet配置，初始化servlet时所需的参数
* @author cn-wumo
* @since 2021/4/21
*/
public class StandardServletConfig implements ServletConfig {
    private final ServletContext servletContext;
    private final Map<String, String> initParameters;
    private final String servletName;

    /**
    * 创建标准servlet配置
    * @param servletContext web应用程序的全局上下文，保存application的属性
 	* @param servletName web.xml中配置的servlet-name
 	* @param initParameters web.xml中配置的init-parameters
    * @author cn-wumo
    * @since 2021/4/21
    */
    public StandardServletConfig(ServletContext servletContext, String servletName,
                                 Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
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
    public String getServletName() {
        return servletName;
    }

}