package server.http;

import cn.hutool.core.util.StrUtil;
import server.catalina.Context;
import server.catalina.Engine;
import server.catalina.Service;
import server.util.MiniBrowser;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Request extends BaseRequest {

    private String requestString;
    private String uri;
    private final Socket socket;
    private Context context;
    private final Service service;
    private String method;


    public Request(Socket socket,Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        this.parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        this.parseUri();
        this.parseContext();
        this.parseMethod();
        if(!"/".equals(context.getPath())) {
            this.uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri))
                this.uri = "/";
        }
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
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
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        if (null == path)
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
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

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

}