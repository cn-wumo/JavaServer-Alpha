package server;

import server.catalina.Server;

public class Bootstrap {
    public static void main(String[] args) {
        Server server  = new Server();
        server.start();
    }
}