package server.util;

import cn.hutool.http.HttpUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
* 简易版的浏览器，用于服务器的数据测试
* @author cn-wumo
* @since 2021/4/15
*/
public class MiniBrowser {

    /**
    * 返回http的正文内容，若无正文则返回null，gzip:false，params:null，isGet:true
    * @param url 服务器资源地址
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false,null,true);
    }

    /**
    * 返回http的正文内容，若无正文则返回null，params:null，isGet:true
    * @param url 服务器资源地址
 	* @param gzip 是否使用gzip压缩
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip,null,true);
    }

    /**
    * 返回http的正文内容，若无正文则返回null，gzip:false
    * @param url 服务器资源地址
 	* @param params 查询参数
 	* @param isGet 是否使用Get
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static byte[] getContentBytes(String url, Map<String,Object> params, boolean isGet) {
        return getContentBytes(url, false,params,isGet);
    }

    /**
    * 返回http的正文内容，若无正文则返回null
    * @param url 服务器资源地址
 	* @param gzip 是否使用gzip压缩
 	* @param params 查询参数
 	* @param isGet 是否使用Get
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static byte[] getContentBytes(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[] response = getHttpBytes(url,gzip,params,isGet);  //获取全部的http数据
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;
        for (int i = 0; i < response.length-doubleReturn.length; i++) { //定位双换行的位置，以确认正文的起点
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);
            if(Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if(-1==pos)
            return null;    //未找到双换行则无正文
        
        pos += doubleReturn.length;
        return Arrays.copyOfRange(response, pos, response.length);
    }

    /**
    * 以UTF-8编码返回http的正文内容，若无正文则返回null，gzip:false，params:null，isGet:true
    * @param url 服务器资源地址
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    /**
     * 返回http的正文内容，若无正文则返回null，params:null，isGet:true
     * @param url 服务器资源地址
     * @param gzip 是否使用gzip压缩
     * @return byte[]
     * @author cn-wumo
     * @since 2021/4/15
     */
    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    /**
     * 返回http的正文内容，若无正文则返回null，gzip:false
     * @param url 服务器资源地址
     * @param params 查询参数
     * @param isGet 是否使用Get
     * @return byte[]
     * @author cn-wumo
     * @since 2021/4/15
     */
    public static String getContentString(String url, Map<String,Object> params, boolean isGet) {
        return getContentString(url,false,params,isGet);
    }

    /**
     * 以UTF-8编码返回http的正文内容，若无正文则返回null
     * @param url 服务器资源地址
     * @param gzip 是否使用gzip压缩
     * @param params 查询参数
     * @param isGet 是否使用Get
     * @return byte[]
     * @author cn-wumo
     * @since 2021/4/15
     */
    public static String getContentString(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[] result = getContentBytes(url,gzip,params,isGet);
        if(null==result)
            return null;
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    /**
    * 从url中获取服务器的Http数据，通过String返回结果，gzip:false，params:null，isGet:true
    * @param url 服务器资源地址
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    /**
     * 从url中获取服务器的Http数据，通过String返回结果，params:null，isGet:true
     * @param url 服务器资源地址
     * @param gzip 是否使用gzip压缩
     * @return java.lang.String
     * @author cn-wumo
     * @since 2021/4/15
     */
    public static String getHttpString(String url,boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    /**
     * 从url中获取服务器的Http数据，通过String返回结果，gzip:false
     * @param url 服务器资源地址
     * @param params 查询参数
     * @param isGet 是否使用Get
     * @return java.lang.String
     * @author cn-wumo
     * @since 2021/4/15
     */
    public static String getHttpString(String url, Map<String,Object> params, boolean isGet) {
        return getHttpString(url,false,params,isGet);
    }

    /**
    * 从url中获取服务器的Http数据，通过String返回结果
    * @param url 服务器资源地址
    * @param gzip 是否使用gzip压缩
    * @param params 查询参数
    * @param isGet 是否使用Get
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static String getHttpString(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[] bytes=getHttpBytes(url,gzip,params,isGet);
        return new String(bytes).trim();
    }

    /**
    * 从url中获取服务器的Http数据，通过byte数组返回结果
    * @param url 服务器资源地址
    * @param gzip 是否使用gzip压缩
    * @param params 查询参数
    * @param isGet 是否使用Get
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static byte[] getHttpBytes(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        String method = isGet?"GET":"POST";
        byte[] result;
        try(
                Socket socket = new Socket()
                ){
            URI uri = new URI(url);
            int port = uri.getPort();
            if(-1==port)
                port = 80;  //默认端口80
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
            if(path.equals(""))
                path = "/";
            if(null!=params && isGet){
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }
            StringBuilder httpRequestString = new StringBuilder();

            String firstLine = method + " " + path + " HTTP/1.1\r\n";
            httpRequestString.append(firstLine);    //新增http的请求行

            requestHeaders.forEach( //新增http的请求报头
                    (key,value)-> httpRequestString.append(key).append(":").append(value).append("\r\n")
            );

            if(null!=params && !isGet){ //如果是post方法，新增Params报头
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }

            PrintWriter Writer = new PrintWriter(socket.getOutputStream(), true);
            Writer.println(httpRequestString);  //向服务器发送http请求

            InputStream is = socket.getInputStream();
            result = MiniBrowser.readBytes(is,true);    //接受服务器的http响应
        } catch (Exception e) {
            e.printStackTrace();
            result = e.toString().getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }

    /**
    * 从数据流中读取Byte，以1024B为缓冲块，所有数据写入到缓冲区ByteArrayOutputStream
    * @param is 数据输入流
 	* @param fully 缓冲块写满或数据流写完才可以断开数据读取
    * @return byte[]
    * @author cn-wumo
    * @since 2021/4/15
    */
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
