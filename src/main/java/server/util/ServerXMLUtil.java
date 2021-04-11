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

public class ServerXMLUtil {
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

    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Service").first();
        return host.attr("name");
    }

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

    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            Connector c = new Connector(service);
            c.setPort(port);
            result.add(c);
        }
        return result;
    }
}