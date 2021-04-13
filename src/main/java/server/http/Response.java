package server.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Response extends BaseResponse {
    private final StringWriter stringWriter;
    private final PrintWriter writer;
    private String contentType;
    private byte[] body;
    private int status;
    private ArrayList<Cookie> cookies;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public String getContentType() {
        return contentType;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody(){
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    public String getCookiesHeader() {
        if(null==cookies)
            return "";
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : getCookies()) {
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
            sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
            if (-1 != cookie.getMaxAge()) { //-1 mean forever
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if (null != cookie.getPath()) {
                sb.append("Path=").append(cookie.getPath());
            }
        }
        return sb.toString();
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    public int getStatus() {
        return status;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }
    public List<Cookie> getCookies() {
        return this.cookies;
    }
}
