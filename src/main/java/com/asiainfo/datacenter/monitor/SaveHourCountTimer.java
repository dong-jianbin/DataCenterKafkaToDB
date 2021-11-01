package com.asiainfo.datacenter.monitor;

/**
 * Created by 董建斌 on 2018/9/26.
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.main.OracleEntry;
import com.asiainfo.datacenter.utils.ParseUtil;
import org.apache.log4j.Logger;

public class SaveHourCountTimer extends TimerTask {

    private static Logger log = Logger.getLogger(SaveHourCountTimer.class);

    Timer myTimer = new Timer();

    public void run() {
        // 保存到文件
        Date time = new Date();

        FileWriter fw;
        try {
            fw = new FileWriter(ConfAttr.HOUR_RESET_COUNT_FILE, true);
            BufferedWriter bufferWritter = new BufferedWriter(fw);
            String line = String.format("%s - In the passed an hour, Reveived: %d, Saved success: %d, Saved failure: %d \r\n", time.toString(), OracleEntry.getReceivedFromKafkaOptCount(), OracleEntry.getSaveToOracleSuccessCount(), OracleEntry.getSaveToOracleFailureCount());
            bufferWritter.write(line);
            bufferWritter.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Save count to file ERROR per hour!");
        }

        OracleEntry.resetReceivedFromKafkaOptCount();
        OracleEntry.resetSaveToOracleSuccessCount();
        OracleEntry.resetSaveToOracleFailureCount();
    }

    public void start(long delay, int internal) {
        myTimer.schedule(this, delay, internal * 1000); // 利用timer.schedule方法
    }

    public void end() {
        myTimer.cancel();
    }

    public static void main(String args[]) {

        long delay = ParseUtil.getFirstClockLong() - System.currentTimeMillis();
        SaveHourCountTimer myTask1 = new SaveHourCountTimer();
        myTask1.start(delay, 3600);

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
        }

        myTask1.end();

    }

}