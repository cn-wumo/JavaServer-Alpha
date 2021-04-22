package server.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import server.classloader.WebappClassLoader;
import server.exception.WebConfigDuplicatedException;
import server.http.ApplicationContext;
import server.http.StandardServletConfig;
import server.util.ContextXMLUtil;
import server.watcher.ContextFileChangeWatcher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
* Context容器，代表一个web应用程序
* @author cn-wumo
* @since 2021/4/18
*/
public class Context {
    private String path;
    private String docBase;
    private final Host host;
    private boolean reloadable;

    private final File contextWebXmlFile; //web.xml所处的位置
    private final Map<String, String> url_servletClassName; //url和servletClassName之间的映射，关键映射
    private final Map<String, String> url_servletName;  //url和servletName之间的映射
    private final Map<String, String> servletName_className;    //servletName和className之间的映射
    private final Map<String, String> className_servletName;    //className和servletName之间的映射
    private final WebappClassLoader webappClassLoader;  //web应用程序所使用的类加载器
    private final Map<String, Map<String, String>> servlet_className_init_params;   //servlet_className和init_params之间的映射
    private final List<String> loadOnStartupServletClassNames;  //需要自启动的类列表
    private final ServletContext servletContext;    //web应用程序的全局上下文，即application空间
    private final Map<Class<?>, HttpServlet> servletPool;   //Servlet单例的对象池，只能从该池子中获取Servlet
    private ContextFileChangeWatcher contextFileChangeWatcher;  //文件改变监听器，监视web应用程序的class情况


    /**
    * 创建新的web应用程序容器
    * @param path web应用程序部署的url路径
 	* @param docBase web应用程序所处的目录路径
 	* @param host web应用程序容器所属的虚拟主机
 	* @param reloadable 是否自动更新class
    * @author cn-wumo
    * @since 2021/4/18
    */
    public Context(String path, String docBase,Host host,boolean reloadable) {
        TimeInterval timeInterval = DateUtil.timer();
        this.path = path;
        this.docBase = docBase;
        this.host = host;
        this.reloadable = reloadable;

        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servletPool = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();

        this.servletContext = new ApplicationContext(this);
        this.loadOnStartupServletClassNames = new ArrayList<>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        LogFactory.get().info("正在部署web应用项目 {}", this.docBase);
        this.deploy();
        LogFactory.get().info("web应用项目 {} 在 {} 毫秒内部署完成", this.docBase,timeInterval.intervalMs());
    }

    /**
    * 部署web应用程序
    * @author cn-wumo
    * @since 2021/4/18
    */
    private void deploy() {
        this.init();
        if(reloadable){
            this.contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            this.contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    /**
    * 初始化web应用程序
    * @author cn-wumo
    * @since 2021/4/18
    */
    private void init() {
        if (!contextWebXmlFile.exists()) {
            LogFactory.get().error(contextWebXmlFile.getPath() + "不存在");
        }else{
            try {
                this.checkDuplicated();
            } catch (WebConfigDuplicatedException e) {
                LogFactory.get().error(e);
                return;
            }

            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document document = Jsoup.parse(xml);
            this.parseServletMapping(document);
            this.parseServletInitParams(document);
            this.parseLoadOnStartup(document);
            this.handleLoadOnStartup();
        }
    }

    /**
     * 检查web.xml是否有重复配置的servlet
     * @author cn-wumo
     * @since 2021/4/18
     */
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        this.checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        this.checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        this.checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }

    /**
    * 检查文档中是否有重复配置
    * @param document 待检查的文档
 	* @param mapping 待检查的标签
 	* @param desc 发生错误时的描述
    * @author cn-wumo
    * @since 2021/4/18
    */
    private void checkDuplicated(Document document, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = document.select(mapping);
        List<String> contents = new ArrayList<>();
        // 排序标签，检查相邻标签的文本域是否相同
        elements.forEach(element->contents.add(element.text()));

        Collections.sort(contents);
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    /**
    * 将文档中的Servlet映射压入到web应用程序中
    * @param document 待检查的文档
    * @author cn-wumo
    * @since 2021/4/18
    */
    private void parseServletMapping(Document document) {
        // url和ServletName之间的映射
        Elements mappingElements = document.select("servlet-mapping");
        for (Element mappingElement : mappingElements) {
            String urlPattern = mappingElement.select("url-pattern").first().text();
            String servletName = mappingElement.select("servlet-name").first().text();
            url_servletName.put(urlPattern, servletName);
        }
        // servletName和className之间的映射，className和servletName之间的映射
        Elements servletElements = document.select("servlet");
        for (Element servletElement : servletElements) {
            String servletName = servletElement.select("servlet-name").first().text();
            String servletClass = servletElement.select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        // url和servletClassName之间的映射
        url_servletName.forEach((url,servletName)->{
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        });
    }

    /**
     * 将文档中的servlet-class和init-param映射压入到web应用程序中
     * @param document 待检查的文档
     * @author cn-wumo
     * @since 2021/4/18
     */
    private void parseServletInitParams(Document document) {
        Elements servletElements = document.select("servlet");
        for(Element servlet : servletElements){
            String servletClassName = servlet.select("servlet-class").first().text();
            Elements initElements = servlet.select("init-param");
            if (initElements.isEmpty())
                return;
            Map<String, String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = element.select("param-name").first().text();
                String value = element.select("param-value").first().text();
                initParams.put(name, value);
            }
            servlet_className_init_params.put(servletClassName, initParams);
        }
    }

    /**
     * 将文档中的自启动Servlet压入到web应用程序中
     * @param document 待检查的文档
     * @author cn-wumo
     * @since 2021/4/18
     */
    public void parseLoadOnStartup(Document document) {
        Elements es = document.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").first().text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }
    
    /**
    * 启动web应用程序的自启动servlet
    * @author cn-wumo
    * @since 2021/4/18
    */
    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                this.initServlet(clazz); //初始化servlet
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化web应用程序的servlet，将其压入单例的servlet对象池中
     * @param clazz 需要被初始化的servlet
     * @return javax.servlet.http.HttpServlet
     * @author cn-wumo
     * @since 2021/4/18
     */
    public synchronized  HttpServlet initServlet(Class<?> clazz) {
        try {
            HttpServlet servlet = (HttpServlet) clazz.getDeclaredConstructor().newInstance();
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> initParameters = servlet_className_init_params.get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
            return servlet;
        } catch (InstantiationException | IllegalAccessException |
                ServletException | NoSuchMethodException |
                InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
    * 获取web应用程序的servlet
    * @param clazz 需要被初始化的servlet
    * @return javax.servlet.http.HttpServlet
    * @author cn-wumo
    * @since 2021/4/18
    */
    public HttpServlet getServlet(Class<?> clazz) {
        HttpServlet servlet = servletPool.get(clazz);
        if (null == servlet) {
            servlet = initServlet(clazz);
        }
        return servlet;
    }

    /**
    * 关闭web应用程序
    * @author cn-wumo
    * @since 2021/4/18
    */
    public void stop() {
        this.webappClassLoader.stop();  //摧毁类加载器
        this.contextFileChangeWatcher.stop();   //摧毁文件改变监听器
        this.destroyServlets(); //摧毁servlet类
    }

    /**
    * 摧毁servlet对象池中的servlet类
    * @author cn-wumo
    * @since 2021/4/18
    */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }
    
    /**
    * web应用程序容器
    * @author cn-wumo
    * @since 2021/4/18
    */
    public void reload() {
        host.reload(this);
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }
}