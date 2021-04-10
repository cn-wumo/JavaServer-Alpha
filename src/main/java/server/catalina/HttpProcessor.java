package server.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import server.servlets.DefaultServlet;
import server.servlets.InvokerServlet;
import server.util.Constant;
import server.http.Request;
import server.http.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpProcessor {
    public void execute(Socket socket, Request request, Response response){
        try{
            String uri = request.getUri();
            System.out.println("uri:"+uri);
            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);
            if(null!=servletClassName){
                InvokerServlet.getInstance().service(request,response);
            }else {
                DefaultServlet.getInstance().service(request,response);
            }
            if(Constant.CODE_200 == response.getStatus()){
                handle200(socket, response);
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(socket, uri);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket,e);
        }
    }
    private static void handle200(Socket socket, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);
        byte[] head = headText.getBytes();

        byte[] body = response.getBody();

        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream os = socket.getOutputStream();
        os.write(responseBytes);
        socket.close();
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
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(e);
            stringBuffer.append("\r\n");
            for (StackTraceElement ste : traceElements) {
                stringBuffer.append("\t");
                stringBuffer.append(ste.toString());
                stringBuffer.append("\r\n");
            }

            String msg = e.getMessage();

            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), stringBuffer.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
