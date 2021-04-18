package server.catalina;

import server.util.ServerXMLUtil;

import java.util.List;

/**
* 服务器的引擎类，提供Servlet容器，下辖多个Host虚拟主机
* @author cn-wumo
* @since 2021/4/17
*/
public class Engine {
    private final String defaultHost;
    private final List<Host> hostList;
    private final Service service;

    /**
    * 根据service实例创建新的Engine
    * @param service service服务实例
    * @author cn-wumo
    * @since 2021/4/17
    */
    public Engine(Service service){
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hostList = ServerXMLUtil.getHosts(this);
        this.service = service;
        Engine.checkDefault(this);
    }

    /**
    * 检查引擎的默认虚拟主机是否存在
    * @param engine 服务引擎
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static void checkDefault(Engine engine) {
        if(null==engine.getDefaultHost())
            throw new RuntimeException("默认端口" + engine.defaultHost + " 不存在！");
    }

    /**
    * 获取该引擎的默认虚拟主机，如若没有则返回null
    * @return server.catalina.Host
    * @author cn-wumo
    * @since 2021/4/17
    */
    public Host getDefaultHost(){
        for (Host host : hostList) {
            if(host.getName().equals(defaultHost))
                return host;
        }
        return null;
    }
}
