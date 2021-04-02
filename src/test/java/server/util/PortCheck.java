package server.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentString(url);
    }
}
