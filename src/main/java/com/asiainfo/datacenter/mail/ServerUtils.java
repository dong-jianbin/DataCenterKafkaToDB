package com.asiainfo.datacenter.mail;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by shenluguo on 2015/6/11.
 */
public class ServerUtils {

    /**
     * 获取服务器名
     * @return
     */
    public static String getServerHostName(){
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String address=addr.getHostName().toString();//获得本机名称
        return address;
    }

    /**
     * 获取本机ip
     * @return
     */
    public static String getLocalIp(){
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip=addr.getHostAddress().toString();//获得本机IP
        return ip;
    }

}
