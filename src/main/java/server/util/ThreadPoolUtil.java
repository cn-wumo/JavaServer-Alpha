package server.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            20, 100, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10));

    public static void run(Runnable r) {
        threadPool.execute(r);
    }

}