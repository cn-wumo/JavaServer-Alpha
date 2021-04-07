package server.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import server.util.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Server {
    private Service service;
    public Server(){
        this.service = new Service(this);
    }

    public void start(){
        logJVM();
        init();
    }
    @SuppressWarnings("InfiniteLoopStatement")
    private void init() {
        int port = 8080;
        try(
                ServerSocket serverSocket = new ServerSocket(port)
        ){
            while(true) {
                Socket socket =  serverSocket.accept();
                Runnable r = () -> {
                    try{
                        Request request = new Request(socket,service);
                        System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                        System.out.println("uri:" + request.getUri());

                        Response response = new Response();
                        String uri = request.getUri();
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
                            String fileContent = FileUtil.readUtf8String(file);
                            response.getWriter().println(fileContent);
                            if(fileName.equals("timeConsume.html")){
                                ThreadUtil.safeSleep(1000);
                            }
                        }else{
                            handle404(socket,uri);
                        }
                        handle200(socket, response);
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

    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "JavaServer-Alpha");
        infos.put("Server built", new Date().toString());
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));
        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key+":\t\t" + infos.get(key));
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