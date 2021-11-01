package com.asiainfo.datacenter.monitor;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.main.OracleEntry;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

/**
 * Created by 董建斌 on 2018/9/26.
 */

public class OracleSqlBufferMonitor extends TimerTask {
    private static Logger log = Logger.getLogger(OracleSqlBufferMonitor.class);

    private Timer monitorTimer = null;
    private final BlockingQueue<JSONObject> oracleSqlBuffer;

    public OracleSqlBufferMonitor(BlockingQueue<JSONObject> oracleSqlBuffer) {
        monitorTimer = new Timer();
        this.oracleSqlBuffer = oracleSqlBuffer;
    }

    @Override
    public void run() {
        Date time = new Date();
        FileWriter fw;
        try {
            fw = new FileWriter(ConfAttr.BUFFER_MONITOR_FILE, true);
            BufferedWriter bufferWritter = new BufferedWriter(fw);
            String countline = String.format(
                    "%s -This hour,Received count: %d, Saved success count: %d, Saved failure count: %d,  Buffer Size: %d  \n\n", time.toString(), OracleEntry.getReceivedFromKafkaOptCount(), OracleEntry.getSaveToOracleSuccessCount(), OracleEntry.getSaveToOracleFailureCount(), oracleSqlBuffer.size());
            bufferWritter.write(countline);
            bufferWritter.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("BufferMonitorTimer : Save buffer monitor ERROR!");
        }

    }

    /**
     * 监控buffer
     *
     * @param delay    , 延迟, 毫秒
     * @param internal , s 秒
     */
    public void start(long delay, int internal) {
        monitorTimer.schedule(this, delay, internal * 1000); // 利用timer.schedule方法
    }

    public void end() {
        monitorTimer.cancel();
    }
}
