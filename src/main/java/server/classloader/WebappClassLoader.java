package server.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
* web应用程序的类加载器
* @author cn-wumo
* @since 2021/4/16
*/
public class WebappClassLoader extends URLClassLoader {

    /**
     * 载入web应用程序的lib依赖和class文件，默认使用公共类加载器CommonClassLoader
     * @param docBase web应用程序的地址
     * @author cn-wumo
     * @since 2021/4/16
     */
    public WebappClassLoader(String docBase) {
        this(docBase,new CommonClassLoader());
    }

    /**
    * 载入web应用程序的lib依赖和class文件
    * @param docBase web应用程序的地址
 	* @param commonClassLoader 双亲类加载器
    * @author cn-wumo
    * @since 2021/4/16
    */
    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[] {}, commonClassLoader);

        try {
            File webInfFolder = new File(docBase, "WEB-INF");
            File classesFolder = new File(webInfFolder, "classes");
            File libFolder = new File(webInfFolder, "lib");

            this.addURL(new URL("file:" + classesFolder.getAbsolutePath() + "/"));
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for (File file : jarFiles) {
                this.addURL(new URL("file:" + file.getAbsolutePath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}