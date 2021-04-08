package server.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void Html() {
        String response = getHttpString("/a.txt");
        Assert.assertTrue(StrUtil.containsAny(response, "Content-Type: text/plain"));
        String html = getHttpString("/a.html");
        Assert.assertTrue(StrUtil.containsAny(html, "Content-Type: text/html"));
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
    public void Index() {
        String html = getContentString("/a/index.html");
        Assert.assertEquals(html,"Hello JavaServer from index.html@a");
        html = getContentString("/b/index.html");
        Assert.assertEquals(html,"Hello JavaServer from index.html@b");
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
