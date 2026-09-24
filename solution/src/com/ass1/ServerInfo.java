package com.ass1;
import java.io.Serializable;

//need to implement serializable to be sent across network
public class ServerInfo implements  Serializable {
    public int port;
    public String host;
    public String serverName;
    public int zone;

    public ServerInfo(int portt, String hostt, String name,int zonee){
        this.port=portt;
        this.host=hostt;
        this.serverName=name;
        this.zone=zonee;
    }
}
