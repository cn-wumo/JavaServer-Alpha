package server.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.jsoup.internal.StringUtil;
import server.catalina.Connector;
import server.catalina.Context;
import server.catalina.Engine;
import server.util.ApplicationRequestDispatcher;
import server.util.MiniBrowser;

import javax.servlet.RequestDispatcher;
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

/**
* 客户端的请求类，保存客户端发完服务器的数据
* @author cn-wumo
* @since 2021/4/17
*/
public class Request extends BaseRequest {

    private String uri;
    private final Socket socket;
    private Context context;
    private final Connector connector;
    private String method;
    private Map<String, String[]> parameterMap;
    private Map<String, String> headerMap;
    private Cookie[] cookies;
    private HttpSession session;
    private boolean forwarded;
    private Map<String, Object> attributesMap;

    /**
    * 根据socket和connector构建新的请求类
    * @param socket 服务器和客户端之间的socket
 	* @param connector 客户端所选择的服务连接器
    * @author cn-wumo
    * @since 2021/4/17
    */
    public Request(Socket socket,Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;

        String requestString = Request.parseHttpRequest(socket);
        if(StrUtil.isEmpty(requestString))
            return;

        this.uri = Request.parseUri(requestString);
        this.context = Request.parseContext(uri,connector);
        this.method = Request.parseMethod(requestString);
        this.parameterMap = Request.parseParameters(requestString,method);
        this.headerMap = Request.parseHeaders(requestString);
        this.cookies = Request.parseCookies(headerMap);
        this.attributesMap = new HashMap<>();

        if(!"/".equals(context.getPath())) {
            this.uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri))
                this.uri = "/";
        }
    }

    /**
    * 从socket中获取http报文
    * @param socket 服务器和客户端之间的socket
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static String parseHttpRequest(Socket socket) throws IOException {
        InputStream is = socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
    * 从http报文中获取uri
    * @param requestString http报文
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static String parseUri(String requestString) {
        String uri = StrUtil.subBetween(requestString, " ", " ");
        return StrUtil.subBefore(uri, '?', false);
    }

    /**
    * 根据uri和服务连接器获取web应用程序容器
    * @param uri 客户端访问的uri地址
 	* @param connector  客户端选择的服务连接器
    * @return server.catalina.Context
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static Context parseContext(String uri,Connector connector) {
        Engine engine = connector.getService().getEngine();

        Context context = engine.getDefaultHost().getContext(uri); //通过uri寻找web应用程序容器
        if(null == context){   //通过uri未找到web应用程序容器
            String path = StrUtil.subBetween(uri, "/", "/");
            if (null == path)   //路径为空，访问根地址
                path = "/";
            else
                path = "/" + path;  //路径不为空，访问对应的web应用程序容器地址

            context = engine.getDefaultHost().getContext(path);
            if (null == context)    //未找到web应用程序容器地址，返回根地址
                context = engine.getDefaultHost().getContext("/");
        }
        return context;
    }

    /**
    * 从http报文中获取method
    * @param requestString http报文
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static String parseMethod(String requestString) {
        return StrUtil.subBefore(requestString, " ", false);
    }

    /**
    * 根据http报文和method获取查询参数
    * @param requestString http报文
 	* @param method 提交方式method
    * @return java.util.Map<java.lang.String,java.lang.String[]>
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static Map<String, String[]> parseParameters(String requestString, String method) {
        String queryString = null;
        Map<String, String[]> parameterMap = new HashMap<>();
        if ("GET".equals(method)) {
            String uri = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(uri, '?')) {
                System.out.println(uri);
                queryString = StrUtil.subAfter(uri, '?', false);
            }
        }else if ("POST".equals(method)) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }

        if (StringUtil.isBlank(queryString)){
            return parameterMap;
        }
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
        return parameterMap;
    }

    /**
    * 根据http报文获取响应报文头
    * @param requestString http报文
    * @return java.util.Map<java.lang.String,java.lang.String>
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static Map<String, String> parseHeaders(String requestString) {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        Map<String, String> headerMap = new HashMap<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
        }
        return headerMap;
    }

    /**
    * 从响应报文头里获取Cookie
    * @param headerMap 响应报文头
    * @return javax.servlet.http.Cookie[]
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static Cookie[] parseCookies(Map<String, String> headerMap) {
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
        return ArrayUtil.toArray(cookieList, Cookie.class);
    }

    public String getUri() {
        return uri;
    }
    public void setUri(String uri){
        this.uri = uri;
    }

    public Context getContext() {
        return context;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
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

    public Connector getConnector() {
        return connector;
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
            port = 80; // 默认端口80
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

    public Socket getSocket() {
        return socket;
    }
    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }
    public boolean isForwarded() {
        return forwarded;
    }
    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }
    @Override
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }
}