package com.asiainfo.datacenter.attr;

import java.util.HashMap;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class ConfAttr {

    /**
     * 写入oracle前的BQ的大小
     */
    public static int BQ_BUFFER_SIZE = 100000;

    /**
     * 程序内存监控文件地址
     */
    public static String BUFFER_MONITOR_FILE = "logs/monitor.txt";

    /**
     * 统计每小时的接收和保存数量
     */
    public static String HOUR_RESET_COUNT_FILE = "logs/hourCount.txt";

    /**
     * 内存监控的时间间隔，秒
     */
    public static int MEM_MONITOR_SECONDS = 10;

    /**
     * KAFKA SASL验证的用户名，密码
     */
    public static String KAFKA_SASL_FLAG = "1";
    public static String KAFKA_SASL_APP_KEY = "97-xinxihua-01";
    public static String KAFKA_SASL_SECRET_KEY = "DZ4acLWYz";

    /**
     * KAFKA没有按照省分表的消息，需要在本地做过滤掉非本省消息，以便提高效率
     */
    public static String KAFKA_FILTER_USERID = "";
    public static String KAFKA_FILTER_PROV = "";

    /**
     * KAFKA控制offset，0.不调整按照当前值读取，1.begginning从开始读取，2.调整offset到指定值x，3.调整offset到x分钟前
     */
    public static String KAFKA_OFFSET_ADJUST_TYPE = "0";
    public static String KAFKA_OFFSET_ADJUST_VALUE = "0";

    /**
     * KAFKA控制Deserializer,ByteArray,String
     */
    public static String KAFKA_DESERIALIZER = "ByteArray";

    /**
     * KAFKA指定分区消费消息，1按照分区，0按照topic整体
     */
    public static String KAFKA_PARTITION_FLAG = "0";


    /**
     * 目标类型.0.无只是打印测试,1.只入oracle数据库，（需要解析）2.只入kafka数据库接收的全部消息（不需要解析，速度快）
     * 3.只入kafka数据库接收的部分消息（需要解析，过滤的纬度按照表） 4.同时入oracle和kafka数据库接收的部分消息（需要解析，过滤的纬度按照表，)
     */
    public static String KAFKA_TARGET_TYPE = "1";

    /**
     * KAFKA MIRROR 开关
     */
    public static String KAFKA_MIRROR_SERVER = "133.224.217.123:9092";

    /**
     * KAFKA MIRROR 开关
     */
    public static String KAFKA_MIRROR_TOPIC = "BigdataDBCrm2_97";

    /**
     * 批量入库提交的时间间隔
     */
    public static int KAFKA_COMMIT_INTERVAL = 10000;

    /**
     * 解析类型，0.不解析，1.cb原有解析方式，2.cb20新解析方式
     */
    public static String KAFKA_PARSE_TYPE = "1";

    /**
     * 入kafka表列表,oracle表列表 (表名:目标类型) 1，3，4
     */
    public static HashMap<String, String> TARGET_TABLES = new HashMap<String, String>();

}
