package server.servlets;

import cn.hutool.core.util.ReflectUtil;
import server.catalina.Context;
import server.http.Request;
import server.http.Response;
import server.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
* servlet处理器，采用单例模式，处理被web.xml注册的servlet
* @author cn-wumo
* @since 2021/4/19
*/
public class InvokerServlet extends HttpServlet {
    private static final InvokerServlet instance = new InvokerServlet();

    public static synchronized InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet(){
    }

    /**
    * 根据请求报文的信息，完善响应报文的内容
    * @param httpServletRequest 客户端的请求报文
 	* @param httpServletResponse 服务器的响应报文
    * @author cn-wumo
    * @since 2021/4/19
    */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            Class<?> servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            Object servletObject = context.getServlet(servletClass);    //从web应用程序容器的对象池里获取对应的单例对象
            ReflectUtil.invoke(servletObject, "service", request, response);
            if(null!=response.getRedirectPath())
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}