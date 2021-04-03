package server;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import server.util.Constant;
import server.util.Request;
import server.util.Response;

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

                Response response = new Response();
                String html = "Hello User";
                response.getWriter().println(html);

                handle200(socket, response);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}