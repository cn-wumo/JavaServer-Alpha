package server.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
* 服务器的响应类，保存服务器发往客户端的数据
* @author cn-wumo
* @since 2021/4/16
*/
public class Response extends BaseResponse {
    private final StringWriter stringWriter;
    private final PrintWriter writer;
    private final ArrayList<Cookie> cookies;
    private String contentType;
    private byte[] body;
    private int status;
    private String redirectPath;

    /**
    * 构造新的Response，contentType默认是"text/html"
    * @author cn-wumo
    * @since 2021/4/17
    */
    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getBody(){
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
    * 获取Cookie的响应报头
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/17
    */
    public String getCookiesHeader() {
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuilder stringBuilder = new StringBuilder();
        for (Cookie cookie : this.getCookies()) {
            stringBuilder.append("\r\n");
            stringBuilder.append("Set-Cookie: ");
            stringBuilder.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");  //设置Cookie的Session
            if (-1 != cookie.getMaxAge()) { //-1 mean forever
                stringBuilder.append("Expires=");   //设置Cookie的过期时间
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                stringBuilder.append(sdf.format(expire));
                stringBuilder.append("; ");
            }
            if (null != cookie.getPath()) {
                stringBuilder.append("Path=").append(cookie.getPath()); //设置Cookie的web应用
            }
        }
        return stringBuilder.toString();
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }
    @Override
    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }
}
