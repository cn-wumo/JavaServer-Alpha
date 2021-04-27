package server.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import server.util.Constant;
import server.util.ServerXMLUtil;
import server.watcher.WarFileWatcher;

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
        scanWarOnWebAppsFolder();
        new WarFileWatcher(this).start();
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
    * 重新加载Host虚拟主机下的web应用程序容器
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

    /**
    * 扫描webapps目录，处理所有的war文件
    * @author cn-wumo
    * @since 2021/4/27
    */
    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = Optional.ofNullable(folder.listFiles()).orElse(new File[]{});
        for (File file : files) {
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            this.loadWar(file);
        }
    }

    /**
    * 把war文件解压为目录，并把文件夹加载为 Context
    * @param warFile 待解压的war文件
    * @author cn-wumo
    * @since 2021/4/27
    */
    public void loadWar(File warFile) {
        String fileName =warFile.getName();
        String folderName = StrUtil.subBefore(fileName,".",true);
        //检查是否已经有对应的Context
        Context context= getContext("/"+folderName);
        if(null!=context)
            return;
        //检查是否已经有对应的文件夹
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;
        //移动war文件，因为jar 命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;
        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解压之后删除临时war
        tempWarFile.delete();
        //创建新的Context
        this.load(contextFolder);
    }

    /**
    * 将文件夹加载为Context
    * @param folder 待加载的文件夹
    * @author cn-wumo
    * @since 2021/4/27
    */
    public void load(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, false);
        contextMap.put(context.getPath(), context);
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