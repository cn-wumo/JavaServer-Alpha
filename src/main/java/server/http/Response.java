package server.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Response extends BaseResponse {
    private final StringWriter stringWriter;
    private final PrintWriter writer;
    private String contentType;
    private byte[] body;
    private int status;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
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

    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    public int getStatus() {
        return status;
    }

}
