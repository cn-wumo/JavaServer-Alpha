package server.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
* 线程池的工具类，提供最多20个线程在同时运行，任务队列里的等待线程最多存活60秒
* @author cn-wumo
* @since 2021/4/16
*/
public class ThreadPoolUtil {
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            20, 100, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    public static void run(Runnable r) {
        threadPool.execute(r);
    }

}