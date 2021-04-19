package server.http;

import server.catalina.Context;

import java.io.File;
import java.util.*;

/**
* web应用程序的全局上下文，保存application全局的属性
* @author cn-wumo
* @since 2021/4/18
*/
public class ApplicationContext extends BaseServletContext{
    private final Map<String, Object> attributesMap;
    private final Context context;

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath();
    }
}
