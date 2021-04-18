package server.catalina;

import cn.hutool.log.LogFactory;
import server.util.Constant;
import server.util.ServerXMLUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
* 服务器的虚拟主机，提供主机ip地址供客户端访问
* @author cn-wumo
* @since 2021/4/17
*/
public class Host {
    private String name;
    private final Map<String, Context> contextMap;
    private final Engine engine;

    /**
    * 创建新的虚拟主机
    * @param name 虚拟主机名称
 	* @param engine 虚拟主机所属的服务引擎
    * @author cn-wumo
    * @since 2021/4/17
    */
    public Host(String name, Engine engine){
        this.name =  name;
        this.engine = engine;

        this.contextMap = Host.scanContextsInServerXML(this);
        Host.scanContextsOnWebAppsFolder(this, contextMap);

    }

    /**
    * 从Server.xml文件中获取虚拟主机Host的配置
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static Map<String, Context> scanContextsInServerXML(Host host) {
        List<Context> contexts = ServerXMLUtil.getContexts(host);
        Map<String, Context> contextMap = new HashMap<>();
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
        return contextMap;
    }

    /**
    * 将服务器默认的webapps添加到虚拟主机Host中
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static void scanContextsOnWebAppsFolder(Host host,Map<String, Context> contextMap) {
        File[] folders = Optional.ofNullable(Constant.webappsFolder.listFiles()).orElse(new File[]{});
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            Host.loadContext(host,contextMap,folder);
        }
    }
    
    /**
    * 加载web应用程序
    * @param folder web应用程序所处的目录
    * @author cn-wumo
    * @since 2021/4/17
    */
    private static void loadContext(Host host,Map<String, Context> contextMap,File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/"; //将ROOT文件夹视作根文件夹
        else
            path = "/" + path;  //将webapps下的其他文件夹视作普通的web应用容器
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,host,true);

        contextMap.put(context.getPath(), context);
    }

    /**
    * 重新加载Host虚拟主机下的容器
    * @param context web应用容器
    * @author cn-wumo
    * @since 2021/4/17
    */
    public void reload(Context context) {
        LogFactory.get().info("正在重新加载 [{}]", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        context.stop();
        contextMap.remove(path);
        Context newContext = new Context(path, docBase, this, reloadable);
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("重新加载 [{}] 已完成", context.getPath());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }
}