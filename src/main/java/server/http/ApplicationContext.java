package server.http;

import server.catalina.Context;

import java.io.File;
import java.util.*;

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
