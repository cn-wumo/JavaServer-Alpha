package server.util;

import cn.hutool.system.SystemUtil;

import java.io.File;

public class Constant {
    public final static String response_head_202 =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: {}\r\n\r\n";
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"), "webapps");
    public final static File rootFolder = new File(webappsFolder,"ROOT");

    public static final File confFolder = new File(SystemUtil.get("user.dir"),"WEB-INF");
    public static final File serverXmlFile = new File(confFolder, "web.xml");
}
