package server;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import server.util.Constant;
import server.util.Request;
import server.util.Response;
import server.util.ThreadPoolUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Bootstrap {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        int port = 8080;
        try(
                ServerSocket serverSocket = new ServerSocket(port);
                ){
            while(true) {
                Socket socket =  serverSocket.accept();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Request request = new Request(socket);
                            System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                            System.out.println("uri:" + request.getUri());

                            Response response = new Response();
                            String uri = request.getUri();
                            if(null==uri)
                                return;
                            System.out.println(uri);
                            if("/".equals(uri)){
                                String html = "Hello User";
                                response.getWriter().println(html);
                            }
                            else{
                                String fileName = StrUtil.removePrefix(uri, "/");
                                File file = FileUtil.file(Constant.rootFolder,fileName);
                                if(file.exists()){
                                    String fileContent = FileUtil.readUtf8String(file);
                                    response.getWriter().println(fileContent);
                                    if(fileName.equals("timeConsume.html")){
                                        ThreadUtil.safeSleep(1000);
                                    }
                                }
                                else{
                                    response.getWriter().println("File Not Found");
                                }
                            }
                            handle200(socket, response);
                        } catch (IOException e) {
                            LogFactory.get().error(e);
                        }
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

    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "How2J DiyTomcat/1.0.1");
        infos.put("Server built", "2020-04-08 10:20:22");
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
}