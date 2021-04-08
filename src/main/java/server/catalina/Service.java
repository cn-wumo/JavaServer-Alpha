package server.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import server.util.ServerXMLUtil;

import java.util.List;

public class Service {
    private String name;
    private Engine engine;
    private Server server;
    private List<Connector> connectors;
    public Service(Server server){
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.connectors = ServerXMLUtil.getConnectors(this);
        this.engine = new Engine(this);
    }

    public void start() {
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());
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