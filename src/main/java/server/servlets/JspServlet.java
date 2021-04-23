package server.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import server.catalina.Context;
import server.classloader.JspClassLoader;
import server.http.Request;
import server.http.Response;
import server.util.Constant;
import server.util.JspUtil;
import server.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
* JSP处理器，采用单例模式，处理JSP文件的servlet
* @author cn-wumo
* @since 2021/4/22
*/
public class JspServlet extends HttpServlet {
    private static final JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return instance;
    }
    
    private JspServlet() {
    }

    /**
    * 根据请求报文的信息，完善响应报文的内容
    * @param httpServletRequest 客户端的请求报文
    * @param httpServletResponse 服务器的响应报文
    * @author cn-wumo
    * @since 2021/4/23
    */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException, RuntimeException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getRequestURI();

            if ("/".equals(uri))
                uri = WebXMLUtil.getWelcomeFile(request.getContext());

            String fileName = StrUtil.removePrefix(uri, "/");
            File jspFile = FileUtil.file(request.getRealPath(fileName));

            if (jspFile.exists()) {
                Context context = request.getContext();
                String path = context.getPath();
                String subFolder;
                if ("/".equals(path))
                    subFolder = "_";
                else
                    subFolder = StrUtil.subAfter(path, '/', false);

                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);

                File jspServletClassFile = new File(servletClassPath);
                if (!jspServletClassFile.exists()) {
                    //判断jspServlet是否存在
                    JspUtil.compileJsp(context, jspFile);
                } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) {
                    //判断jspServlet的Class文件的更新时间是否晚于jspServlet的Java文件
                    JspUtil.compileJsp(context, jspFile);
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                String extName = FileUtil.extName(jspFile);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);

                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                Class<?> jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                HttpServlet servlet = context.getServlet(jspServletClass);  //获取jspServlet
                servlet.service(request,response);  //调用servlet的service方法

                response.setStatus(Constant.CODE_200);
            } else {
                response.setStatus(Constant.CODE_404);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}