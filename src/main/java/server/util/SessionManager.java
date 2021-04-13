package server.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import server.http.Request;
import server.http.Response;
import server.http.StandardSession;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.*;

public class SessionManager {
    private static Map<String, StandardSession> sessionMap = new HashMap<>();
    private static int defaultTimeout = getTimeout();
    static {
        startSessionOutdateCheckThread();
    }

    public static HttpSession getSession(String jsessionid, Request request, Response response) {

        if (null == jsessionid) {
            return newSession(request, response);
        } else {
            StandardSession currentSession = sessionMap.get(jsessionid);
            if (null == currentSession) {
                return newSession(request, response);
            } else {
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                createCookieBySession(currentSession, request, response);
                return currentSession;
            }
        }
    }

    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext();
        String sid = generateSessionId();
        StandardSession session = new StandardSession(sid, servletContext);
        session.setMaxInactiveInterval(defaultTimeout);
        sessionMap.put(sid, session);
        createCookieBySession(session, request, response);
        return session;
    }

    private static int getTimeout() {
        int defaultResult = 30;
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("session-config session-timeout");
        if (es.isEmpty())
            return defaultResult;
        return Convert.toInt(es.get(0).text());
    }

    private static void checkOutDateSession() {
        Set<String> jsessionids = sessionMap.keySet();
        List<String> outdateJessionIds = new ArrayList<>();

        for (String jsessionid : jsessionids) {
            StandardSession session = sessionMap.get(jsessionid);
            long interval = System.currentTimeMillis() -  session.getLastAccessedTime();
            if (interval > session.getMaxInactiveInterval() * 1000)
                outdateJessionIds.add(jsessionid);
        }

        for (String jsessionid : outdateJessionIds) {
            sessionMap.remove(jsessionid);
        }
    }

    private static void startSessionOutdateCheckThread() {
        new Thread() {
            public void run() {
                while (true) {
                    checkOutDateSession();
                    ThreadUtil.sleep(1000 * 30);
                }
            }

        }.start();

    }

    public static synchronized String generateSessionId() {
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }

}