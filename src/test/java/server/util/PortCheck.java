package server.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PortCheck {
    private static final int port = 8080;
    private static final String ip = "127.0.0.1";
    @BeforeClass
    public static void beforeClass() {
        //所有测试开始前看server是否已经启动了
        if(NetUtil.isUsableLocalPort(port)) {
            System.err.println("请先启动端口: " +port+ "的JavaServer，否则无法进行单元测试");
            System.exit(1);
        }
        else {
            System.out.println("检测到JavaServer已经启动，开始进行单元测试");
        }
    }

    @Test
    public void test() {
        String html = getContentString("/");
        Assert.assertEquals(html,"hello jsp@ROOT");
    }

    @Test
    public void testJPG() {
        byte[] bytes = getContentBytes("/emoticon.jpg");
        int pngFileLength = 64429;
        Assert.assertEquals(pngFileLength, bytes.length);
        bytes = getContentBytes("/index.jsp");
        Assert.assertEquals(new String(bytes),"hello jsp@ROOT");
    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        CountDownLatch countDownLatch=new CountDownLatch(3);
        TimeInterval timeInterval= DateUtil.timer();
        for (int i = 0; i <3; i++) {
            new Thread(()->{
                getContentString("/timeConsume.html");
                countDownLatch.countDown();
            },"Thread "+i).start();
        }
        countDownLatch.await();
        long duration=timeInterval.intervalMs();
        Assert.assertTrue(duration<3000);
    }

    @Test
    public void testHello() {
        String html1 = getContentString("/javaee/hello");
        String html2 = getContentString("/javaee/hello");
        Assert.assertEquals(html1,html2);
    }

    @Test
    public void testGetParam() {
        String uri = "/javaee/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","java");
        String html = MiniBrowser.getContentString(url, params, true);
        Assert.assertEquals(html,"get name:java");
    }

    @Test
    public void testPostParam() {
        String uri = "/javaee/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","java");
        String html = MiniBrowser.getContentString(url, params, false);
        Assert.assertEquals(html,"post name:java");
    }

    @Test
    public void test404() {
        String response  = getHttpString("/not_exist.html");
        Assert.assertTrue(StrUtil.containsAny(response, "HTTP/1.1 404 Not Found"));
    }

    @Test
    public void test500() {
        String response  = getHttpString("/500.html");
        Assert.assertTrue(StrUtil.containsAny(response, "HTTP/1.1 500 Internal Server Error"));
    }

    @Test
    public void testJavaeeHello() {
        String html = getContentString("/javaee/hello");
        Assert.assertEquals(html,"Hello JavaServer-Alpha from HelloServlet@javaee");
    }

    @Test
    public void testHeader() {
        String html = getContentString("/javaee/header");
        Assert.assertEquals(html,"mini browser");
    }

    @Test
    public void testCookie() throws IOException {
        URL url = new URL(StrUtil.format("http://{}:{}{}", ip,port,"/javaee/getCookie"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Cookie","name=java(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, StandardCharsets.UTF_8);
        Assert.assertTrue(StrUtil.containsAny(html,"name:java(cookie)"));
    }

    @Test
    public void testSession() throws IOException {
        String jsessionid = getContentString("/javaee/setSession?a=12&b=32");
        if(null!=jsessionid)
            jsessionid = jsessionid.trim();
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaee/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, StandardCharsets.UTF_8);
        Assert.assertTrue(StrUtil.containsAny(html,"java(session)"));
    }

    @Test
    public void testGzip() {
        byte[] gzipContent = getContentBytes("/",true);
        byte[] unGzipContent = ZipUtil.unGzip(gzipContent);
        String html = new String(unGzipContent);
        Assert.assertEquals(html, "hello jsp@ROOT");
    }

    @Test
    public void testJsp() {
        String html = getContentString("/javaee/");
        Assert.assertEquals(html, "hello jsp@javaweb");
        html = getContentString("/");
        Assert.assertEquals(html, "hello jsp@ROOT");
    }

    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }

    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,gzip);
    }

    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentString(url);
    }

    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getHttpString(url);
    }
}
