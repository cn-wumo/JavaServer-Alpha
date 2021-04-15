package server.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
* Context的工具类
* @author cn-wumo
* @since 2021/4/15
*/
public class ContextXMLUtil {
    
    /**
    * 监视服务器的web.xml配置文件的位置，如果未找到则返回WEB-INF/web.xml
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/15
    */
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXmlFile);
            Document d = Jsoup.parse(xml);
            Element e = d.select("WatchedResource").first();
            return e.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
