package server.catalina;

import cn.hutool.log.LogFactory;
import server.http.Request;
import server.http.Response;
import server.util.ThreadPoolUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
* 服务器连接器，接收Socket并构建request和response
* @author cn-wumo
* @since 2021/4/15
*/
public class Connector implements Runnable {
    private final int port;
    private final Service service;
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressibleMimeType;

    /**
    * 链接service服务实例，选择服务端口
    * @param service service服务实例
 	* @param port 服务端口
    * @author cn-wumo
    * @since 2021/4/15
    */
    public Connector(Service service,int port) {
        this.service = service;
        this.port = port;
    }

    public void start() {
        LogFactory.get().info("启动协议处理器 [http-bio-{}]", port);
        new Thread(this).start();
    }

    /**
    * Connector的具体初始化流程
    * @author cn-wumo
    * @since 2021/4/15
    */
    public void init() {
        LogFactory.get().info("初始化协议处理器 [http-bio-{}]", port);
    }
    
    /**
    * 自旋获取客户端发送的Socket，具体业务在HttpProcessor中处理
    * @author cn-wumo
    * @since 2021/4/15
    */
    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try (
                ServerSocket serverSocket = new ServerSocket(port)
        ) {
            while (true) {
                Socket socket = serverSocket.accept();
                Runnable r = () -> {
                    try {
                        Request request = new Request(socket, Connector.this);
                        Response response = new Response();
                        HttpProcessor processor = new HttpProcessor();
                        processor.execute(socket, request, response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
        }
    }

    public Service getService() {
        return service;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressibleMimeType() {
        return compressibleMimeType;
    }

    public void setCompressibleMimeType(String compressibleMimeType) {
        this.compressibleMimeType = compressibleMimeType;
    }
}