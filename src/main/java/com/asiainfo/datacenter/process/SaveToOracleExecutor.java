package com.asiainfo.datacenter.process;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.attr.OracleAttr;
import com.asiainfo.datacenter.dao.C3P0Factory;
import com.asiainfo.datacenter.main.OracleEntry;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class SaveToOracleExecutor implements Runnable {

    private static Logger log = Logger.getLogger(SaveToOracleExecutor.class);

    private final BlockingQueue<JSONObject> queue;
    private AtomicBoolean run = new AtomicBoolean(true);

    public SaveToOracleExecutor(BlockingQueue<JSONObject> queue) {
        this.queue = queue;
    }

    // 只能单线程处理，如果多线程这里的全局变量可能有问题
    private Connection conn = null;
    private Statement stmt = null;

    List<String> preSqlList = new ArrayList<String>();

    @Override
    public void run() {
        conn = null;
        String sql = "";
        try {
            conn = C3P0Factory.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            Long lastTime = System.currentTimeMillis();
            Long currentTime = System.currentTimeMillis();
            Long diffTime = 0L;

            while (run.get()) {

                if (conn == null) {
                    conn = C3P0Factory.getConnection();
                    conn.setAutoCommit(false);
                    stmt = conn.createStatement();
                }

                currentTime = System.currentTimeMillis();
                diffTime = currentTime - lastTime;

                if (queue.size() <= 0) {
                    if (preSqlList.size() > 0 && diffTime > ConfAttr.KAFKA_COMMIT_INTERVAL) {
                        try {
                            conn.commit();
                            OracleEntry.incrSaveToOracleSuccessCount(preSqlList.size());
                            preSqlList.clear();
                            lastTime = currentTime;
                            diffTime = 0L;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            log.error("Batch submit error!");
                            conn.close();
                            conn = null;
                            remedyCommit();
                        }
                    }
                    continue;
                } else {
                    try {
                        sql = queue.take().getString("sql");
                        preSqlList.add(sql);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        log.error("Taking out sql error: " + e.getMessage());
                    }
                    try {
                        stmt.execute(sql);
                    } catch (SQLException e) {
                        log.warn("Patch execute SQL error: " + sql + "\n" + e.getMessage());
                        conn.close();
                        conn = null;
                        remedyCommit();
                        continue;
                    }

                    if (preSqlList.size() >= OracleAttr.ORACLE_BATCH_NUM || diffTime > ConfAttr.KAFKA_COMMIT_INTERVAL) {
                        try {
                            conn.commit();
                            OracleEntry.incrSaveToOracleSuccessCount(preSqlList.size());
                            preSqlList.clear();
                            lastTime = currentTime;
                            diffTime = 0L;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            log.error("Batch submit error!");
                            conn.close();
                            conn = null;
                            remedyCommit();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("EXECUTE RUN SQL ERROR: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    log.info("main connection closed...");
                } catch (SQLException e) {
                    log.error("Finally close connection!", e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 停止程序时调用
     */
    public void stop() {
        while (this.queue.size() > 0) {

        }
        run.set(false);

        log.info("-----Batch sql list-----" + preSqlList.size());
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Shutdown hook close conn ERROR", e);
        }
        for (int i = 0; i < preSqlList.size(); i++) {
            log.info("single sql: " + i + " - " + preSqlList.get(i));
            singleCommit(preSqlList.get(i));
        }
        log.info("-------------save to oracle executor stopped------------------");
    }

    /**
     * 提交单个sql
     *
     * @param sql
     */
    private void singleCommit(String sql) {
        log.info("Single commit! - " + sql);
        Connection conn = null;
        try {
            conn = C3P0Factory.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            conn.commit();
            OracleEntry.incrSaveToOracleSuccessCount(1);
        } catch (SQLException e) {
            log.error("Single commit ERROR! - " + sql + "\n" + e.getMessage(), e);
            OracleEntry.incrSaveToOracleFailureCount(1);
        } finally {
            if (null != conn) {
                try {
                    conn.close();
                } catch (SQLException e1) {
                    log.error("Single commit CONN close ERROR", e1);
                }
            }
        }
    }

    /**
     * 批量提交sql时 出现异常就调用
     */
    private void remedyCommit() {
        log.info("Remedy commit! preSqlList size: " + preSqlList.size());
        for (int i = 0; i < preSqlList.size(); i++) {
            singleCommit(preSqlList.get(i));
        }
        preSqlList.clear();
    }
}
