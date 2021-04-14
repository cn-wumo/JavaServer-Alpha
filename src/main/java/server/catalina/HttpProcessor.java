package server.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import server.servlets.DefaultServlet;
import server.servlets.InvokerServlet;
import server.util.Constant;
import server.http.Request;
import server.http.Response;
import server.util.SessionManager;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
* @Description: Http协议处理器，Http业务的具体实现类
* @Author: cn-wumo
* @Date: 2021/4/14
*/
public class HttpProcessor {

    public void execute(Socket socket, Request request, Response response){
        try{
            String uri = request.getUri();
            System.out.println("uri:"+uri);
            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);
            this.prepareSession(request, response);
            if(null!=servletClassName){
                InvokerServlet.getInstance().service(request,response);
            }else {
                DefaultServlet.getInstance().service(request,response);
            }
            if(Constant.CODE_200 == response.getStatus()){
                handle200(socket, request, response);
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(socket, uri);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket,e);
        }
    }
    private void handle200(Socket s, Request request, Response response)
            throws IOException {
        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();
        byte[] body = response.getBody();
        String cookiesHeader = response.getCookiesHeader();
        boolean gzip = isGzip(request, body, contentType);
        String headText;
        if (gzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_200;
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        if (gzip)
            body = ZipUtil.gzip(body);
        byte[] head = headText.getBytes();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);
        os.write(responseBytes,0,responseBytes.length);
        os.flush();
        os.close();
    }

    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings=  request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;
        Connector connector = request.getConnector();
        if (mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        if (!"on".equals(connector.getCompression()))
            return false;
        if (body.length < connector.getCompressionMinSize())
            return false;
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.equals(userAgent, eachUserAgent))
                return false;
        }
        String mimeTypes = connector.getCompressibleMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }

    private void handle404(Socket socket, String uri) throws IOException {
        OutputStream os = socket.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes(StandardCharsets.UTF_8);
        os.write(responseByte);
    }

    private void handle500(Socket socket, Exception e) {
        try {
            OutputStream os = socket.getOutputStream();
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

            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), stringBuilder.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void prepareSession(Request request, Response response) {
        String jsessionid = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }
}
