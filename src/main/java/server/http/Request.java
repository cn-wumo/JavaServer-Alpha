package server.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import server.catalina.Context;
import server.catalina.Engine;
import server.catalina.Service;
import server.util.MiniBrowser;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request extends BaseRequest {

    private String requestString;
    private String uri;
    private final Socket socket;
    private Context context;
    private final Service service;
    private String method;
    private String queryString;
    private Map<String, String[]> parameterMap;
    private Map<String, String> headerMap;
    private Cookie[] cookies;
    private HttpSession session;

    public Request(Socket socket,Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        this.parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        this.parseUri();
        this.parseContext();
        this.parseMethod();
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        if(!"/".equals(context.getPath())) {
            this.uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri))
                this.uri = "/";
        }
        this.parseParameters();
        this.parseHeaders();
        this.parseCookies();
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

    public Context getContext() {
        return context;
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }else if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString)
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        for (String parameterValue : parameterValues) {
            String[] nameValues = parameterValue.split("=");
            String name = nameValues[0];
            String value = nameValues[1];
            String[] values = parameterMap.get(name);
            if (null == values) {
                values = new String[] { value };
            } else {
                values = ArrayUtil.append(values, value);
            }
            parameterMap.put(name, values);
        }
    }

    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
        }
    }

    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void setSession(HttpSession session){
        this.session = session;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }
    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }
    @Override
    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }
    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }
    @Override
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }
    @Override
    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }
    @Override
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }
    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    @Override
    public String getProtocol() {
        return "HTTP:/1.1";
    }
    @Override
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }
    @Override
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }
    @Override
    public int getRemotePort() {
        return socket.getPort();
    }
    @Override
    public String getScheme() {
        return "http";
    }
    @Override
    public String getServerName() {
        return getHeader("host").trim();
    }
    @Override
    public int getServerPort() {
        return getLocalPort();
    }
    @Override
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    @Override
    public String getRequestURI() {
        return uri;
    }
    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }
    @Override
    public String getServletPath() {
        return uri;
    }
    @Override
    public Cookie[] getCookies() {
        return cookies;
    }
    @Override
    public HttpSession getSession() {
        return session;
    }
}