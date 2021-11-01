package com.asiainfo.datacenter.dao;

/**
 * Created by 董建斌 on 2018/9/26.
 */

import com.asiainfo.datacenter.utils.PropertiesUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.log4j.Logger;

import java.beans.PropertyVetoException;
import java.sql.Connection;

public class C3P0Utils {
    private static Logger log = Logger.getLogger(C3P0Utils.class.getName());
    private static ComboPooledDataSource ds;

    private static ThreadLocal<Connection> tl = new ThreadLocal<Connection>();

    static {
        ds = new ComboPooledDataSource();//直接使用即可，不用显示的配置，其会自动识别配置文件
        String DRIVER_NAME = PropertiesUtil.getInstance().getProperty("jdbc.driverClass");
        String DATABASE_URL = PropertiesUtil.getInstance().getProperty("jdbc.url");
        String DATABASE_USER = PropertiesUtil.getInstance().getProperty("jdbc.username");
        String DATABASE_PASSWORD = PropertiesUtil.getInstance().getProperty("jdbc.password");
        boolean Validate = Boolean.parseBoolean(PropertiesUtil.getInstance().getProperty("c3p0.validate"));
        int Min_PoolSize = Integer.parseInt(PropertiesUtil.getInstance().getProperty("c3p0.minPoolSize"));
        int Acquire_Increment = Integer.parseInt(PropertiesUtil.getInstance().getProperty("c3p0.acquireIncrement"));
        int Max_PoolSize = Integer.parseInt(PropertiesUtil.getInstance().getProperty("c3p0.maxPoolSize"));
        int Initial_PoolSize = Integer.parseInt(PropertiesUtil.getInstance().getProperty("c3p0.initialPoolSize"));
        int Idle_Test_Period = Integer.parseInt(PropertiesUtil.getInstance().getProperty("c3p0.idleConnectionTestPeriod"));
        try {
            ds.setDriverClass(DRIVER_NAME);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
            log.error("c3p0 init error!\n" + e.getLocalizedMessage());
            System.exit(0);
        }
        ds.setJdbcUrl(DATABASE_URL);
        // cpds.setJdbcUrl("jdbc:mysql://10.58.47.155:3306/data_helper?useUnicode=true&characterEncoding=utf8");
        ds.setUser(DATABASE_USER);
        ds.setPassword(DATABASE_PASSWORD);
        ds.setInitialPoolSize(Initial_PoolSize);
        ds.setMinPoolSize(Min_PoolSize);
        ds.setMaxPoolSize(Max_PoolSize);
        ds.setAcquireIncrement(Acquire_Increment);
        ds.setIdleConnectionTestPeriod(Idle_Test_Period);
        ds.setTestConnectionOnCheckout((Validate));
    }

    public static ComboPooledDataSource getDataSource() {
        return ds;
    }

    public static Connection getConnection() {
        try {
//			 得到当前线程上绑定的连接
            Connection conn = tl.get();
            if (conn == null) { // 代表线程上没有绑定连接
                conn = ds.getConnection();
                tl.set(conn);
            }
            return conn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void startTransaction() {
        try {
            // 得到当前线程上绑定连接开启事务
            Connection conn = getConnection();
            conn.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void commitTransaction() {
        try {
            Connection conn = tl.get();
            if (conn != null) {
                conn.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeConnection() {
        try {
            Connection conn = tl.get();
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tl.remove(); // 千万注意，解除当前线程上绑定的链接（从threadlocal容器中移除对应当前线程的链接）
        }
    }
}