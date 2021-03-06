package server.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import server.http.Request;
import server.http.Response;
import server.servlets.DefaultServlet;
import server.servlets.InvokerServlet;
import server.servlets.JspServlet;
import server.util.Constant;
import server.util.SessionManager;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
* Http协议处理器，Http业务的具体实现类
* @author cn-wumo
* @since 2021/4/15
*/
public class HttpProcessor {

    /**
    * 执行http协议处理器
    * @param socket 服务器和客户端之间的socket
 	* @param request 客户端的请求
 	* @param response 服务器的响应
    * @author cn-wumo
    * @since 2021/4/18
    */
    public void execute(Socket socket, Request request, Response response){
        try{
            String uri = request.getUri();
            LogFactory.get().info(request.getLocalAddr()+" visit uri:"+uri);    //调试用，打印访客信息
            Context context = request.getContext();
            this.prepareSession(request, response);

            String servletClassName = context.getServletClassName(uri); //根据web应用程序的web.xml配置，寻找url对应的servlet-name
            HttpServlet workingServlet;
            if(null!=servletClassName)
                //在web应用程序中找到servlet-name，则访问servlet处理器
                workingServlet = InvokerServlet.getInstance();
            else if(uri.endsWith(".jsp"))
                //.jsp为拓展名的资源文件，访问JSP处理器
                workingServlet = JspServlet.getInstance();
            else
                //未在web应用程序中找到servlet-name，则访问缺省servlet处理器，例如html文件等静态资源
                workingServlet = DefaultServlet.getInstance();

            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            if(request.isForwarded())
                return;
            //根据response的状态，进入不同的流程
            switch (response.getStatus()) {
                case Constant.CODE_200 -> handle200(socket, request, response);
                case Constant.CODE_404 -> handle404(socket, uri);
                case Constant.CODE_302 -> handle302(socket,response);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket,e);
        }
    }

    /**
    * 返回200响应报文
    * @param socket 服务器和客户端之间的socket
 	* @param request 客户端的请求报文
 	* @param response 服务器的响应报文
    * @author cn-wumo
    * @since 2021/4/20
    */
    private void handle200(Socket socket, Request request, Response response)
            throws IOException {
        try(
                OutputStream os = socket.getOutputStream()
                ){
            String contentType = response.getContentType();
            byte[] body = response.getBody();
            String cookiesHeader = response.getCookiesHeader();
            boolean gzip = isGzip(request, body, contentType);  //是否采用gzip压缩

            String headTextFormat;
            if (gzip) {
                headTextFormat = Constant.response_head_200_gzip;
                body = ZipUtil.gzip(body);
            }else
                headTextFormat = Constant.response_head_200;

            String headText = StrUtil.format(headTextFormat, contentType, cookiesHeader);
            byte[] head = headText.getBytes();
            byte[] responseBytes = new byte[head.length + body.length];
            ArrayUtil.copy(head, 0, responseBytes, 0, head.length); //写入报文头
            ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);   //写入报文体

            os.write(responseBytes,0,responseBytes.length);
        }
    }

    /**
    * 返回404响应报文
    * @param socket 服务器和客户端之间的socket
 	* @param uri 客户端访问的uri
    * @author cn-wumo
    * @since 2021/4/20
    */
    private void handle404(Socket socket, String uri) throws IOException {
        try(
                OutputStream os = socket.getOutputStream()
                ){
            String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
            responseText = Constant.response_head_404 + responseText;
            byte[] responseByte = responseText.getBytes(StandardCharsets.UTF_8);
            os.write(responseByte);
        }
    }

    /**
    * 返回500响应报文
    * @param socket 服务器和客户端之间的socket
 	* @param e 服务器的错误类型
    * @author cn-wumo
    * @since 2021/4/20
    */
    private void handle500(Socket socket, Exception e) {
        try(
                OutputStream os = socket.getOutputStream()
        ) {
            StackTraceElement[] traceElements = e.getStackTrace();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append("\r\n");
            for (StackTraceElement ste : traceElements) {
                stringBuilder.append("\t");
                stringBuilder.append(ste.toString());
                stringBuilder.append("\r\n");
            }

            String msg = e.getMessage();

            if (null != msg && msg.length() > 20)   //显示20行错误报告
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), stringBuilder.toString());
            text = Constant.response_head_500 + text;
            
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            os.write(responseBytes);
        } catch (IOException e1) {
            LogFactory.get().error(e1);
        }
    }

    /**
    * 返回302响应报文
    * @param socket 服务器和客户端之间的socket
    * @param response 服务器的响应报文
    * @author cn-wumo
    * @since 2021/4/24
    */
    private void handle302(Socket socket, Response response) throws IOException {
        try(
                OutputStream os = socket.getOutputStream()
                ){
            String redirectPath = response.getRedirectPath();
            String head_text = Constant.response_head_302;
            String header = StrUtil.format(head_text, redirectPath);
            byte[] responseBytes = header.getBytes(StandardCharsets.UTF_8);
            os.write(responseBytes);
        }
    }

    /**
    * 判断是否采用gzip压缩
    * @param request 客户端的请求报文
 	* @param body 响应报文的实体
 	* @param mimeType 访问文件的mime-type
    * @return boolean
    * @author cn-wumo
    * @since 2021/4/20
    */
    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings=  request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;
        Connector connector = request.getConnector();
        if (mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        if (!"on".equals(connector.getCompression()))   //服务连接器不启动压缩
            return false;
        if (body.length < connector.getCompressionMinSize())    //小于压缩的最短长度
            return false;
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {   //判断客户端的浏览器是否在不压缩名单
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.equals(userAgent, eachUserAgent))
                return false;
        }
        String mimeTypes = connector.getCompressibleMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) { //判断mime-type是否可压缩
            if (mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }

    /**
    * 获取request对应的session
    * @param request 客户端的请求报文
 	* @param response 服务器的响应报文
    * @author cn-wumo
    * @since 2021/4/21
    */
    public void prepareSession(Request request, Response response) {
        String jsessionid = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }
}
