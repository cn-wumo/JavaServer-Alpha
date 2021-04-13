package server.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public void Test() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello JavaServer-Alpha@ROOT");
    }

    @Test
    public void JPG() {
        byte[] bytes = getContentBytes("/emoticon.jpg");
        int pngFileLength = 64429;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    @Test
    public void TimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for(int i = 0; i<3; i++){
            threadPool.execute(() -> getContentString("/timeConsume.html"));
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.MILLISECONDS);

        Assert.assertTrue(timeInterval.intervalMs() < 1000);
    }

    @Test
    public void Hello() {
        String html1 = getContentString("/javaee/hello");
        String html2 = getContentString("/javaee/hello");
        Assert.assertEquals(html1,html2);
    }

    @Test
    public void GetParam() {
        String uri = "/javaee/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, true);
        Assert.assertEquals(html,"get name:meepo");
    }

    @Test
    public void PostParam() {
        String uri = "/javaee/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, false);
        Assert.assertEquals(html,"post name:meepo");
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
    public void JavaeeHello() {
        String html = getContentString("/javaee/hello");
        Assert.assertEquals(html,"Hello JavaServer-Alpha from HelloServlet@javaee");
    }

    @Test
    public void Header() {
        String html = getContentString("/javaee/header");
        Assert.assertEquals(html,"mini browser");
    }

    @Test
    public void Cookie() throws IOException {
        URL url = new URL(StrUtil.format("http://{}:{}{}", ip,port,"/javaee/getCookie"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Cookie","name=java(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, StandardCharsets.UTF_8);
        Assert.assertTrue(StrUtil.containsAny(html,"name:java(cookie)"));
    }

    @Test
    public void Session() throws IOException {
        String jsessionid = getContentString("/javaee/setSession");
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

    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }
    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,false);
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
