package server.util;

import cn.hutool.core.util.StrUtil;
import server.Bootstrap;
import server.catalina.Context;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Request {

    private String requestString;
    private String uri;
    private final Socket socket;
    private Context context;


    public Request(Socket socket) throws IOException {
        this.socket = socket;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        if(!"/".equals(context.getPath()))
            this.uri = StrUtil.removePrefix(uri, context.getPath());
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is);
        requestString = new String(bytes, StandardCharsets.UTF_8);
    }

    private void parseUri() {
        String temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            this.uri = temp;
        }else{
            this.uri = StrUtil.subBefore(temp, '?', false);
        }
    }

    private void parseContext() {
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;

        this.context = Bootstrap.contextMap.get(path);
        if (null == context)
            this.context = Bootstrap.contextMap.get("/");
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }

    public Context getContext() {
        return context;
    }

}