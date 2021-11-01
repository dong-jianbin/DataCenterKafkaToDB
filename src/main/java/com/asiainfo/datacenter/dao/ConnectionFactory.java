package com.asiainfo.datacenter.dao;

/**
 * Created by 董建斌 on 2018/9/26.
 */

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionFactory {

    private ConnectionFactory() {
    }

    private static ComboPooledDataSource ds = null;

    static {
        try {
            // Logger log = Logger.getLogger("com.mchange"); // 日志
            // log.setLevel(Level.WARNING);
            ds = new ComboPooledDataSource();
            // 设置JDBC的Driver类
            ds.setDriverClass("oracle.jdbc.OracleDriver");  // 参数由 Config 类根据配置文件读取
            // 设置JDBC的URL
            ds.setJdbcUrl("jdbc:oracle:thin:@10.58.46.150:31521:devbd");
            // 设置数据库的登录用户名
            ds.setUser("bdjg150");
            // 设置数据库的登录用户密码
            ds.setPassword("7F5pDwfe3QPQZ2Aj");
            // 设置连接池的最大连接数
            ds.setMaxPoolSize(200);
            // 设置连接池的最小连接数
            ds.setMinPoolSize(20);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Connection getConnection() {
        Connection con = null;
        try {
            con = ds.getConnection();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return con;
    }
    // C3P0 end
}

