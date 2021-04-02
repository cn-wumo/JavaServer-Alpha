package server;

import cn.hutool.core.net.NetUtil;
import server.util.Request;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Bootstrap {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        int port = 8080;
        if(!NetUtil.isUsableLocalPort(port)) {
            System.out.println(port +" 端口被占用.");
            System.exit(1);
        }
        try(
                ServerSocket serverSocket = new ServerSocket(port)
                ){
            while(true) {
                Socket socket =  serverSocket.accept();
                Request request = new Request(socket);
                System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                System.out.println("uri:" + request.getUri());
                OutputStream os = socket.getOutputStream();
                String response_head = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n\r\n";
                String responseString = "Hello JavaServer";
                responseString = response_head + responseString;
                os.write(responseString.getBytes());
                os.flush();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}