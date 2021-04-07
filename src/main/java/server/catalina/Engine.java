package server.catalina;

import server.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private final String defaultHost;
    private final List<Host> hostList;
    private Service service;

    public Engine(Service service){
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hostList = ServerXMLUtil.getHosts(this);
        this.service = service;
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
