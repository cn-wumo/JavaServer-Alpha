package server.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

/**
* @Description: 公共类加载器
* @Author: cn-wumo
* @Date: 2021/4/14
*/
public class CommonClassLoader extends URLClassLoader {

    /**
    * @Description: 将web程序的依赖包路径读入
    * @Param: []
    * @Author: cn-wumo
    * @Date: 2021/4/14
    */
    public CommonClassLoader() {
        super(new URL[]{});
        try {
            File libFolder = new File(System.getProperty("user.dir"), "lib");
            File[] jarFiles = Optional.ofNullable(libFolder.listFiles()).orElse(new File[]{});
            for (File file : jarFiles) {
                if (file.getName().endsWith("jar")) {
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}