package server.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import server.util.ServerXMLUtil;

import java.util.List;

/**
* @Description: JavaServer-Alpha服务实例，建造connectors多线程以提供服务器的多端口链接
* @Author: cn-wumo
* @Date: 2021/4/14
*/
public class Service {
    private final String name;
    private final Engine engine;
    private final Server server;
    private final List<Connector> connectors;

    public Service(Server server){
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.connectors = ServerXMLUtil.getConnectors(this);    //从XML文件里读取端口配置
        this.engine = new Engine(this);
    }

    public void start() {
        this.init();
    }

    /**
    * @Description: 初始化且启动Connector
    * @Param: []
    * @return: void
    * @Throws void
    * @Author: cn-wumo
    * @Date: 2021/4/14
    */
    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("进程在{}毫秒内初始化",timeInterval.intervalMs());
        for (Connector c : connectors)
            c.start();
    }

    public Engine getEngine() {
        return engine;
    }

    public Server getServer() {
        return server;
    }
}