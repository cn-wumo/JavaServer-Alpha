package server.catalina;

import cn.hutool.log.LogFactory;
import server.util.Constant;
import server.util.ServerXMLUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Host {
    private String name;
    private final Map<String, Context> contextMap;
    private final Engine engine;

    public Host(String name, Engine engine){
        this.contextMap = new HashMap<>();
        this.name =  name;
        this.engine = engine;

        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private  void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    private  void scanContextsOnWebAppsFolder() {
        File[] folders = Optional.ofNullable(Constant.webappsFolder.listFiles()).orElse(new File[]{});
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }
    private  void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this,true);

        contextMap.put(context.getPath(), context);
    }

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


    public Context getContext(String path) {
        return contextMap.get(path);
    }
}