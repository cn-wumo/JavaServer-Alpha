package server.catalina;

import server.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private final String defaultHost;
    private final List<Host> hostList;

    public Engine(){
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hostList = ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    private void checkDefault() {
        if(null==getDefaultHost())
            throw new RuntimeException("the defaultHost" + defaultHost + " does not exist!");
    }

    public Host getDefaultHost(){
        for (Host host : hostList) {
            if(host.getName().equals(defaultHost))
                return host;
        }
        return null;
    }
}
