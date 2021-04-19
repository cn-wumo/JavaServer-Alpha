package server.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import server.catalina.*;

import java.util.ArrayList;
import java.util.List;

/**
* Server.xml文件的工具类
* @author cn-wumo
* @since 2021/4/16
*/
public class ServerXMLUtil {
    
    /**
    * 从Server.xml中读取服务器的Contexts配置，例如web服务器容器，web服务器的路径和是否自动重载
    * @param host 主机地址
    * @return java.util.List<server.catalina.Context>
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static List<Context> getContexts(Host host) {
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Context");
        for (Element e : es) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = Convert.toBool(e.attr("reloadable"), true);
            Context context = new Context(path, docBase, host, reloadable);
            result.add(context);
        }
        return result;
    }

    /**
    * 获取引擎的默认主机地址，默认设置localhost
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    /**
    * 获取服务实例的名称，默认是Catalina
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Service").first();
        return host.attr("name");
    }

    /**
    * 获得服务器引擎下辖的主机地址，默认包含localhost
    * @param engine 服务器引擎
    * @return java.util.List<server.catalina.Host>
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Host");
        for (Element e : es) {
            String name = e.attr("name");
            Host host = new Host(name,engine);
            result.add(host);
        }
        return result;
    }

    /**
    * 获取服务器实例下辖的服务器连接器
    * @param service 服务器实例
    * @return java.util.List<server.catalina.Connector>
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressibleMimeType = e.attr("compressibleMimeType");

            Connector c = new Connector(service,port);
            c.setCompression(compression);
            c.setCompressibleMimeType(compressibleMimeType);
            c.setNoCompressionUserAgents(noCompressionUserAgents);
            c.setCompressibleMimeType(compressibleMimeType);
            c.setCompressionMinSize(compressionMinSize);
            result.add(c);
        }
        return result;
    }
}