package server.util;

import server.catalina.HttpProcessor;
import server.http.Request;
import server.http.Response;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class ApplicationRequestDispatcher implements RequestDispatcher {

    private final String uri;
    public ApplicationRequestDispatcher(String uri) {
        if(!uri.startsWith("/"))
            uri = "/" + uri;
        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;

        request.setUri(uri);

        HttpProcessor processor = new HttpProcessor();
        processor.execute(request.getSocket(), request,response);
        request.setForwarded(true);

    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}