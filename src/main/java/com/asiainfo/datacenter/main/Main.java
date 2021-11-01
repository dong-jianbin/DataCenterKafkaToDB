package com.asiainfo.datacenter.main;

import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.utils.PropertiesUtil;
import com.asiainfo.datacenter.process.ShutdownHook;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("---------START-----");

        PropertyConfigurator.configure(PropertiesUtil.getInstance().getProperty("log4j.properties"));
        //检查启动参数
        if (args == null || args.length != 5) {
            log.error("Init Failer because of lacking parameters...");
            log.error("Usage:\n\tjava -jar kafka_consumer.jar ${bootstrap_server} ${topic_name} ${group_name} ${client_id} ${partition_id}");
            System.exit(1);
        }

        String bootstrap = args[0];
        String topic = args[1];
        String groupid = args[2];
        String client = args[3];
        String partition = args[4];
        OracleEntry oracleEntry = new OracleEntry();
        ShutdownHook shutdownHook = new ShutdownHook(oracleEntry);

        oracleEntry.initConfig();

        oracleEntry.startOracleEntry();

        oracleEntry.startKafkaConsumer(bootstrap, topic, groupid, client, Integer.parseInt(partition));
    }
}
