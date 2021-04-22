package server.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import server.http.Request;
import server.http.Response;
import server.util.Constant;
import server.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
* 缺省servlet处理器，采用单例模式，处理未被web.xml注册的servlet
* @author cn-wumo
* @since 2021/4/19
*/
public class DefaultServlet extends HttpServlet {
    private static final DefaultServlet instance = new DefaultServlet();

    public static synchronized DefaultServlet getInstance() {
        return instance;
    }

    private DefaultServlet() {
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
        if("/500.html".equals(uri)) //500错误测试
            throw new ServletException();
        if ("/".equals(uri))
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
        if(uri.endsWith(".jsp")){
            JspServlet.getInstance().service(request,response);
            return;
        }
        String fileName = StrUtil.removePrefix(uri, "/");

        File file = FileUtil.file(request.getRealPath(fileName));
        if (file.exists()) {
            String extName = FileUtil.extName(file);    //获取拓展名
            String mimeType = WebXMLUtil.getMimeType(extName);
            response.setContentType(mimeType);

            byte[] body = FileUtil.readBytes(file);
            response.setBody(body);

            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(Constant.CODE_404);
        }

    }

}