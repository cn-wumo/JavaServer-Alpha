package server.watcher;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import server.catalina.Host;
import server.util.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
* 监控webapps目录下的war文件变化
* @author cn-wumo
* @since 2021/4/27
*/
public class WarFileWatcher {
    private final WatchMonitor monitor;
    public WarFileWatcher(Host host) {
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {
            private void dealWith(WatchEvent<?> event) {
                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString();
                    if(fileName.toLowerCase().endsWith(".war")  && ENTRY_CREATE.equals(event.kind())) {
                        File warFile = FileUtil.file(Constant.webappsFolder, fileName);
                        host.loadWar(warFile);
                    }
                }
            }
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event);

            }
            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }
            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

        });
    }

    public void start() {
        monitor.start();
    }

    public void stop() {
        monitor.interrupt();
    }

}