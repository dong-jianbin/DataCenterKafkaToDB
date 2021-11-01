package com.asiainfo.datacenter.process;

import com.asiainfo.datacenter.main.OracleEntry;
import org.apache.log4j.Logger;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class ShutdownHook {
    private static Logger log = Logger.getLogger(ShutdownHook.class.getName());
    public ShutdownHook(final OracleEntry oracleEntry){
        log.info("-------Start ShutdownHook----------");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                oracleEntry.stop();
                log.info("-----ShutdowHook stop-------------");
            }
        });
    }
}
