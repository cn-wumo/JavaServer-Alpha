package server;

import cn.hutool.core.net.NetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Bootstrap {

    public static void main(String[] args) {
        int port = 8080;
        if(!NetUtil.isUsableLocalPort(port)) {
            System.out.println(port +" 端口被占用.");
            System.exit(1);
        }
        try(
                ServerSocket ss = new ServerSocket(port)
                ){
            while(true) {
                Socket s =  ss.accept();
                InputStream inputStream= s.getInputStream();
                byte[] buffer = new byte[1024];
                inputStream.read(buffer);
                String requestString = new String(buffer, StandardCharsets.UTF_8);
                System.out.println("浏览器的输入信息： \r\n" + requestString);
                OutputStream os = s.getOutputStream();
                String response_head = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n\r\n";
                String responseString = "Hello User";
                responseString = response_head + responseString;
                os.write(responseString.getBytes());
                os.flush();
                s.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}