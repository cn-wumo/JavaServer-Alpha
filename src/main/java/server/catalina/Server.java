package server.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
* JavaServer-Alpha服务器，提供服务器启动前的准备服务
* @author cn-wumo
* @since 2021/4/15
*/
public class Server {
    private final Service service;

    public Server(){
        this.service = new Service(this);
    }

    /**
    * 服务器的启动接口
    * @author cn-wumo
    * @since 2021/4/15
    */
    public void start(){
        TimeInterval timeInterval = DateUtil.timer();
        Server.logVM();
        this.init();
        LogFactory.get().info("服务器在{}毫秒内启动",timeInterval.intervalMs());
    }
    
    /**
    * 服务器的初始化
    * @author cn-wumo
    * @since 2021/4/15
    */
    private void init() {
        service.start();
    }

    /**
    * 打印虚拟机信息
    * @author cn-wumo
    * @since 2021/4/15
    */
    private static void logVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("服务器名称", "JavaServer-Alpha");
        infos.put("服务器建造时间", new Date().toString());
        infos.put("服务器版本", "1.0.2");
        infos.put("宿主操作系统\t", SystemUtil.get("os.name"));
        infos.put("宿主操作系统版本", SystemUtil.get("os.version"));
        infos.put("处理器架构", SystemUtil.get("os.arch"));
        infos.put("虚拟机路径", SystemUtil.get("java.home"));
        infos.put("虚拟机版本", SystemUtil.get("java.runtime.version"));
        infos.put("虚拟机供应商", SystemUtil.get("java.vm.specification.vendor"));

        infos.forEach((key,value)-> LogFactory.get().info(key+":\t" + value));
    }
}