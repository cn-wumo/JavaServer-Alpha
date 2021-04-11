package server;

import server.catalina.Server;
import server.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {
    public static void main(String[] args) throws Exception{
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);
        String serverClassName = "server.catalina.Server";
        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);
        Object serverObject = serverClazz.getDeclaredConstructor().newInstance();
        Method m = serverClazz.getMethod("start");
        m.invoke(serverObject);
        System.out.println("Bootstrap' ClassLoader: "+serverClazz.getClassLoader());
    }
}