package server.util;

import cn.hutool.http.HttpUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MiniBrowser {

    public static byte[] getContentBytes(String url, Map<String,Object> params, boolean isGet) {
        return getContentBytes(url, false,params,isGet);
    }

    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip,null,true);
    }

    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false,null,true);
    }

    public static String getContentString(String url, Map<String,Object> params, boolean isGet) {
        return getContentString(url,false,params,isGet);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[] result = getContentBytes(url,gzip,params,isGet);
        if(null==result)
            return null;
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[] response = getHttpBytes(url,gzip,params,isGet);
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
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[]  bytes=getHttpBytes(url,gzip,params,isGet);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url, Map<String,Object> params, boolean isGet) {
        return getHttpString(url,false,params,isGet);
    }

    public static byte[] getHttpBytes(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        String method = isGet?"GET":"POST";
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
            if(null!=params && isGet){
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }
            String firstLine = method + " " + path + " HTTP/1.1\r\n";

            StringBuilder httpRequestString = new StringBuilder();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+"\r\n";
                httpRequestString.append(headerLine);
            }

            if(null!=params && !isGet){
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }

            PrintWriter Writer = new PrintWriter(socket.getOutputStream(), true);
            Writer.println(httpRequestString);

            InputStream is = socket.getInputStream();
            result = MiniBrowser.readBytes(is,true);
        } catch (Exception e) {
            e.printStackTrace();
            result = e.toString().getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }

    public static byte[] readBytes(InputStream is,boolean fully) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(true) {
            int length = is.read(buffer);
            if(length<=0){
                break;
            }
            baos.write(buffer, 0, length);
            if(!fully && length!=1024)
                break;
        }
        return baos.toByteArray();
    }
}
