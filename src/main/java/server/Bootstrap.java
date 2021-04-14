package server;

import server.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {
    public static void main(String[] args) throws Exception{
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        Class<?> serverClazz = commonClassLoader.loadClass("server.catalina.Server");
        Object serverObject = serverClazz.getDeclaredConstructor().newInstance();

        Method m = serverClazz.getMethod("start");
        m.invoke(serverObject);
    }
}