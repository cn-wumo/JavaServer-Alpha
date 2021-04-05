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
        Assert.assertEquals(html,"Hello User");
    }

    @Test
    public void Html() {
        String html = getContentString("/a.html");
        Assert.assertEquals(html,"Hello World from a.html");
    }

    @Test
    public void TimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for(int i = 0; i<10; i++){
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

    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentString(url);
    }
}
