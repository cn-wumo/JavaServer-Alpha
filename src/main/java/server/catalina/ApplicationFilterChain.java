package server.catalina;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
* web应用程序的FilterChain，处理服务器的责任链
* @author cn-wumo
* @since 2021/4/26
*/
public class ApplicationFilterChain implements FilterChain {

    private final Filter[] filters;
    private final Servlet servlet;
    int pos;

    public ApplicationFilterChain(List<Filter> filterList, Servlet servlet){
        this.filters = ArrayUtil.toArray(filterList,Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        for( Filter filter:filters) {
            filter.doFilter(request, response, this);
        }
        servlet.service(request, response);
    }

}