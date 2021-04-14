package server.catalina;

import cn.hutool.log.LogFactory;
import server.http.Request;
import server.http.Response;
import server.util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable {
    int port;
    private final Service service;
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressibleMimeType;

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
                    try {
                        Request request = new Request(socket, Connector.this);
                        Response response = new Response();
                        HttpProcessor processor = new HttpProcessor();
                        processor.execute(socket,request, response);
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