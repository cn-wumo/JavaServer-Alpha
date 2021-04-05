package server;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import server.catalina.Context;
import server.util.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Bootstrap {
    public static Map<String, Context> contextMap = new HashMap<>();
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        int port = 8080;
//        logJVM();
        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
        try(
                ServerSocket serverSocket = new ServerSocket(port)
                ){
            while(true) {
                Socket socket =  serverSocket.accept();
                Runnable r = () -> {
                    try{
                        Request request = new Request(socket);
                        System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                        System.out.println("uri:" + request.getUri());

                        Response response = new Response();
                        String uri = request.getUri();
                        Context context = request.getContext();
                        if(null==uri)
                            return;
                        if("/".equals(uri)){
                            String html = "Hello User";
                            response.getWriter().println(html);
                        }
                        else{
                            String fileName = StrUtil.removePrefix(uri, "/");
                            File file = FileUtil.file(context.getDocBase(),fileName);
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
            LogFactory.get().info(key+":\t" + infos.get(key));
        }
    }

    private static void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }

    private static void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase);
        contextMap.put(context.getPath(), context);
    }

    private static void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts();
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }
}