package server.classloader;

import cn.hutool.core.util.StrUtil;
import server.catalina.Context;
import server.util.Constant;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
* JSP类加载器
* @author cn-wumo
* @since 2021/4/23
*/
public class JspClassLoader extends URLClassLoader {

    private static final Map<String, JspClassLoader> map = new HashMap<>();

    /**
    * 将web应用程序容器的workFolder下的jspServlet类导入
    * @param context 需要jsp类加载器的web应用程序容器
    * @author cn-wumo
    * @since 2021/4/23
    */
    private JspClassLoader(Context context) {
        super(new URL[] {}, context.getWebappClassLoader());

        try {
            String subFolder;
            String path = context.getPath();
            if ("/".equals(path))
                subFolder = "_";    // "_"为JavaServer-Alpha的默认web应用程序容器名称
            else
                subFolder = StrUtil.subAfter(path, '/', false);

            File classesFolder = new File(Constant.workFolder, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * 移除jsp和jsp类加载器的关联
    * @param uri jsp文件的uri
 	* @param context jsp文件所属的web应用程序容器
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static void invalidJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

    /**
    * 获取jsp对应的jsp类加载器
    * @param uri jsp文件的uri
 	* @param context jsp文件所属的web应用程序容器
    * @return server.classloader.JspClassLoader
    * @author cn-wumo
    * @since 2021/4/23
    */
    public static JspClassLoader getJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if (null == loader) {
            loader = new JspClassLoader(context);
            map.put(key, loader);
        }
        return loader;
    }
}