package server.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MiniBrowser {
    public static void main(String[] args){
        String url = "http://localhost:8080";
        String contentString= getContentString(url);
        System.out.println(contentString);
        String httpString= getHttpString(url);
        System.out.println(httpString);
    }

    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false);
    }

    public static String getContentString(String url) {
        return getContentString(url,false);
    }

    public static String getContentString(String url, boolean gzip) {
        byte[] result = getContentBytes(url, gzip);
        if(null==result)
            return null;
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    public static byte[] getContentBytes(String url, boolean gzip) {
        byte[] response = getHttpBytes(url,gzip);
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;
        for (int i = 0; i < response.length-doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);

            if(Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if(-1==pos)
            return null;

        pos += doubleReturn.length;

        return Arrays.copyOfRange(response, pos, response.length);
    }

    public static String getHttpString(String url,boolean gzip) {
        byte[]  bytes=getHttpBytes(url,gzip);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url) {
        return getHttpString(url,false);
    }

    public static byte[] getHttpBytes(String url,boolean gzip) {
        byte[] result;
        try(
                Socket socket = new Socket()
                ){
            URI uri = new URI(url);
            int port = uri.getPort();
            if(-1==port)
                port = 80;
            InetSocketAddress inetSocketAddress = new InetSocketAddress(uri.getHost(), port);
            socket.connect(inetSocketAddress, 1000);
            Map<String,String> requestHeaders = new HashMap<>();

            requestHeaders.put("Host", uri.getHost()+":"+port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "mini browser");

            if(gzip)
                requestHeaders.put("Accept-Encoding", "gzip");

            String path = uri.getPath();
            if(path.length()==0)
                path = "/";

            String firstLine = "GET " + path + " HTTP/1.1\r\n";

            StringBuilder httpRequestString = new StringBuilder();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+"\r\n";
                httpRequestString.append(headerLine);
            }

            OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
            writer.write(httpRequestString.toString());
            writer.flush();
            InputStream is = socket.getInputStream();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while(true) {
                int length = is.read(buffer);
                if(-1==length)
                    break;
                outputStream.write(buffer, 0, length);
                if(length!=1024)
                    break;
            }
            result = outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            result = e.toString().getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }
}
