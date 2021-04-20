package server.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import server.catalina.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
* web.xml文件的工具类
* @author cn-wumo
* @since 2021/4/16
*/
public class WebXMLUtil {
    private static final Map<String, String> mimeTypeMapping = WebXMLUtil.initMimeType();
    
    /**
    * 获取web应用程序容器的web.xml文件中的WelcomeFile，未找到则返回index.html
    * @param context web服务器容器
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("welcome-file");
        for (Element e : es) {
            String welcomeFileName = e.text();
            File f = new File(context.getDocBase(), welcomeFileName);
            if (f.exists())
                return f.getName();
        }
        return "index.html";
    }
    
    /**
    * 获取web.xml文件中记录的所有mimeType
    * @return java.util.Map<java.lang.String,java.lang.String>
    * @author cn-wumo
    * @since 2021/4/16
    */
    private static Map<String, String> initMimeType() {
        Map<String, String> mimeTypeMapping = new HashMap<>();
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("mime-mapping");
        for (Element e : es) {
            String extName = e.select("extension").first().text();
            String mimeType = e.select("mime-type").first().text();
            mimeTypeMapping.put(extName, mimeType);
        }
        return mimeTypeMapping;
    }
    
    /**
    * 根据文件的拓展名从从web.xml中获取相对应的mimeType，若未找到则返回text/html
    * @param extName 文件拓展名
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static synchronized String getMimeType(String extName) {
        String mimeType = mimeTypeMapping.get(extName);
        if (null == mimeType)
            return "text/html";
        return mimeType;
    }

    /**
     * 从web.xml中获取Session的过期时间，如果未设置则返回30
     * @return int
     * @author cn-wumo
     * @since 2021/4/16
     */
    public static int getTimeout() {
        int defaultResult = 30;
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("session-config session-timeout");
        if (es.isEmpty())
            return defaultResult;
        else
            return Convert.toInt(es.get(0).text());
    }

}