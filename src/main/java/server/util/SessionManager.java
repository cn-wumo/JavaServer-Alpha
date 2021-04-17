package server.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import server.http.Request;
import server.http.Response;
import server.http.StandardSession;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* 会话管理器，提供服务器会话所需的工具
* @author cn-wumo
* @since 2021/4/16
*/
public class SessionManager {
    private static final Map<String, StandardSession> sessionMap = new HashMap<>();
    private static final int defaultTimeout = WebXMLUtil.getTimeout();
    static {
        SessionManager.startSessionOutDateCheckThread();
    }

    /**
    * 根据客户端提供的Session的id获取相对应的Session实例
    * @param jsessionid 客户端提供的Jsessionid，单次会话具有唯一性
 	* @param request 客户端的请求对象
 	* @param response 服务器的响应对象
    * @return javax.servlet.http.HttpSession
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static HttpSession getSession(String jsessionid, Request request, Response response) {
        if (null == jsessionid) {   //客户端未提供Jsessionid
            return SessionManager.newSession(request, response);
        } else {
            StandardSession currentSession = sessionMap.get(jsessionid);
            if (null == currentSession) {   //客户端提供的Jsessionid已过期
                return SessionManager.newSession(request, response);
            } else {
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                SessionManager.createCookieBySession(currentSession, request, response);
                return currentSession;
            }
        }
    }

    /**
    * 利用Cookie，在客户端保存Session的id
    * @param session 当前的Session
 	* @param request 客户端的请求对象
 	* @param response 服务器的响应对象
    * @author cn-wumo
    * @since 2021/4/16
    */
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    /**
    * 创建一个新的Session，塞入sessionMap并设置Session的过期时间
    * @param request 客户端的请求对象
 	* @param response 服务器的响应对象
    * @return javax.servlet.http.HttpSession
    * @author cn-wumo
    * @since 2021/4/16
    */
    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext();
        String sid = SessionManager.generateSessionId();
        StandardSession session = new StandardSession(sid, servletContext);
        session.setMaxInactiveInterval(defaultTimeout);
        sessionMap.put(sid, session);
        SessionManager.createCookieBySession(session, request, response);
        return session;
    }

    /**
     * 创建线程来检测Session的过期情况，30秒检测一次
     * @author cn-wumo
     * @since 2021/4/16
     */
    private static void startSessionOutDateCheckThread() {
        new Thread(() -> {
            while (true) {
                SessionManager.checkOutDateSession();
                ThreadUtil.sleep(1000 * 30);
            }
        }).start();
    }

    /**
    * 检查sessionMap中的是否过期，如果过期则将其剔除，默认过期时间30分钟
    * @author cn-wumo
    * @since 2021/4/16
    */
    private static void checkOutDateSession() {
        List<String> outDateSession = new ArrayList<>();
        sessionMap.forEach((key,value)->{
            StandardSession session = sessionMap.get(key);
            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            if (interval > (long)session.getMaxInactiveInterval()*1000*60)
                outDateSession.add(key);
        });
        outDateSession.forEach(sessionMap::remove);
    }

    /**
    * 随机生成md5加密过的SessionId
    * @return java.lang.String
    * @author cn-wumo
    * @since 2021/4/16
    */
    public static synchronized String generateSessionId() {
        String result;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }

}