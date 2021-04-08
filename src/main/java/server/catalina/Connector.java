package server.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import server.util.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Connector implements Runnable {
    int port;
    private Service service;
    public Connector(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }

    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try(
                ServerSocket serverSocket = new ServerSocket(port)
        ){
            while(true) {
                Socket socket =  serverSocket.accept();
                Runnable r = () -> {
                    try{
                        Request request = new Request(socket,service);
                        Response response = new Response();
                        String uri = request.getUri();
                        System.out.println("uri:"+uri);
                        Context context = request.getContext();
                        if(null==uri)
                            return;
                        if("/500.html".equals(uri)){
                            throw new Exception("this is a deliberately created exception");
                        }
                        if("/".equals(uri))
                            uri = WebXMLUtil.getWelcomeFile(request.getContext());
                        String fileName = StrUtil.removePrefix(uri, "/");
                        File file = FileUtil.file(context.getDocBase(),fileName);
                        if(file.exists()){
                            String extName = FileUtil.extName(file);
                            String mimeType = WebXMLUtil.getMimeType(extName);
                            response.setContentType(mimeType);

                            byte[] body = FileUtil.readBytes(file);
                            response.setBody(body);

                            if(fileName.equals("timeConsume.html")){
                                ThreadUtil.safeSleep(1000);
                            }
                            handle200(socket, response);
                        }else{
                            handle404(socket,uri);
                        }
                    } catch (Exception e) {
                        LogFactory.get().error(e);
                        handle500(socket,e);
                    }
                };
                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
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
            stringBuffer.append(e.toString());
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