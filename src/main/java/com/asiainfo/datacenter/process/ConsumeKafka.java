package com.asiainfo.datacenter.process;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.attr.OracleAttr;
import com.asiainfo.datacenter.attr.SaslConfig;
import com.asiainfo.datacenter.parse.CbOggMessage;
import com.asiainfo.datacenter.parse.OracleParser;
import com.asiainfo.datacenter.main.OracleEntry;
import com.asiainfo.datacenter.utils.StringUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;

import javax.security.auth.login.Configuration;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.temporal.ChronoUnit.MINUTES;

import com.asiainfo.datacenter.parse.MessageDbDrds;
import com.asiainfo.datacenter.parse.MessageDbDrds.Column;
import com.asiainfo.datacenter.parse.MessageDbDrds.Record;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class ConsumeKafka {
    private static Logger log = Logger.getLogger(ConsumeKafka.class.getName());

    private final BlockingQueue<JSONObject> queue;
    public static boolean complete = false;
    private boolean adjustOffsetFlag = true;

    public ConsumeKafka(BlockingQueue<JSONObject> queue) {
        this.queue = queue;
    }

    public void stop() {
        log.info("--------Kafka Consumer stop---------");
        this.complete = true;
        log.info("-----------Kafka Consumer stopped--------------------");
    }

    public void consume(String bootstrap, String topic, String groupid, final String client, int part) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrap);
        props.put("group.id", groupid);
        props.put("enable.auto.commit", "false");
        //解析器
        if (ConfAttr.KAFKA_DESERIALIZER.equals("String")) {
            props.put("key.deserializer", StringDeserializer.class.getName());
            props.put("value.deserializer", StringDeserializer.class.getName());
        } else {
            props.put("key.deserializer", ByteArrayDeserializer.class.getName());
            props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        }
        props.put("max.poll.interval.ms", "300000");
        props.put("max.poll.records", "100000");
//        props.put("auto.offset.reset", "earliest");
        props.put("auto.offset.reset", "latest");


        //加密设置
        if (ConfAttr.KAFKA_SASL_FLAG.equals("1")) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "PLAIN");
            Configuration.setConfiguration(new SaslConfig(ConfAttr.KAFKA_SASL_APP_KEY, ConfAttr.KAFKA_SASL_SECRET_KEY));
        }

        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props);
        //按照分区消费
        if (ConfAttr.KAFKA_PARTITION_FLAG.equals("1")) {
            TopicPartition partition = new TopicPartition(topic, part);
            consumer.assign(Arrays.asList(partition));
        } else {
            consumer.subscribe(Arrays.asList(topic));
        }

        Properties propsProduce = new Properties();
        propsProduce.put("bootstrap.servers", ConfAttr.KAFKA_MIRROR_SERVER);
        propsProduce.put("acks", "all");
        propsProduce.put("retries", 0);
        propsProduce.put("batch.size", 16384);
        propsProduce.put("linger.ms", 1);
        propsProduce.put("buffer.memory", 33554432);
        propsProduce.put("key.serializer", ByteArraySerializer.class.getName());
        propsProduce.put("value.serializer", ByteArraySerializer.class.getName());
        Producer<byte[], byte[]> producer = new KafkaProducer<>(propsProduce);

        AtomicLong atomicLong = new AtomicLong();
        while (true) {
            try {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(50000);

                //调整offset
                if (adjustOffsetFlag) {
                    adjustOffset(consumer);
                    adjustOffsetFlag = false;
                }
                records.forEach(record -> {
                    log.debug("client : " + client + " topic: " + record.topic() + " partition: " + record.partition() + " offset = " + record.offset() + " key = " + record.key().toString() + "value = " + record.value().toString());
                    byte[] kafkaMsg = new byte[0];
                    kafkaMsg = record.value();



                    String tableName = "";
                    //不解析消息，直接kafka mirror
                    if (ConfAttr.KAFKA_TARGET_TYPE.equals("0") || ConfAttr.KAFKA_TARGET_TYPE.equals("1")) {
                        if (ConfAttr.KAFKA_PARSE_TYPE.equals("1")) {
                            tableName = parseMsg(kafkaMsg);
                        } else if (ConfAttr.KAFKA_PARSE_TYPE.equals("2")) {
                            tableName = parseMsgcb20(kafkaMsg, topic);
                        }
                    } else if (ConfAttr.KAFKA_TARGET_TYPE.equals("2") ) {
                        ProducerRecord<byte[],byte[]> record1 = new ProducerRecord< byte[], byte[]>(ConfAttr.KAFKA_MIRROR_TOPIC,record.key(),record.value());
                        producer.send(record1);
                    } else if (ConfAttr.KAFKA_TARGET_TYPE.equals("3") || ConfAttr.KAFKA_TARGET_TYPE.equals("4")) {
                        if (ConfAttr.KAFKA_PARSE_TYPE.equals("1")) {
                            tableName = parseMsg(kafkaMsg);
                            if (StringUtil.notEmpty(tableName,true)) {
                                ProducerRecord<byte[],byte[]> record1 = new ProducerRecord< byte[], byte[]>(ConfAttr.KAFKA_MIRROR_TOPIC,record.key(),record.value());
                                producer.send(record1);
                            }
                        } else if (ConfAttr.KAFKA_PARSE_TYPE.equals("2")) {
                            tableName = parseMsgcb20(kafkaMsg, topic);
                            log.info("kafka_tableName---------->>" + tableName);
                            if (ConfAttr.TARGET_TABLES.containsKey(tableName)) {
                                if (ConfAttr.TARGET_TABLES.get(tableName).equals("3") || ConfAttr.TARGET_TABLES.get(tableName).equals("4")) {
                                    ProducerRecord<byte[],byte[]> record1 = new ProducerRecord< byte[], byte[]>(ConfAttr.KAFKA_MIRROR_TOPIC,record.key(),record.value());
                                    producer.send(record1);
                                }
                            }
                        }
                    }
                });
                consumer.commitAsync();
            } catch (Exception e) {
                log.error("kafka_Exception---------->>" + e);
            }
        }
    }

    /**
     * int version = parseFrom.getVersion();  // 消息版本标识 1
     * int extract = parseFrom.getExtract();  // 数据提取程序类型(1:OGG/2:DTS) 2
     * String source = parseFrom.getSource();  // 数据来源(取值自配置文件,用以区分不同的源端系统) instanceid
     * int source_type = parseFrom.getSourceType();  // 源端数据库类型(1:Oracle/2:MySql) 2
     * String csn = parseFrom.getCsn();  // 提交序号 recordid 唯一标识（每个rds）
     * String xid = parseFrom.getXid();  // 事务ID .......
     * int begin_trans_trail_seq = parseFrom.getBeginTransTrailSeq();  // 事务开始所在文件序号......
     * int begin_trans_trail_rba = parseFrom.getBeginTransTrailRba();  // 事务开始所在文件内位置.....
     * int trail_seq = parseFrom.getTrailSeq();  // 数据所在文件序号(准确) seq
     * int trail_rba = parseFrom.getTrailRba(); // 数据所在文件内位置(准确) rba
     * int trans_seq = parseFrom.getTransSeq(); // 事务序号(参考).....
     * int trans_flag = parseFrom.getTransFlag(); // 事务标志(0:开始/1:中间/2:结束/3:整个)(参考) 3
     * int optType = parseFrom.getOperationType(); // 操作类型(透传) 0 insert 1 update  2 delete
     * String operation_time = parseFrom.getOperationTime(); // 操作时间(事务开始时间) 再确认 YYYY-MM-DD HH:MM:SS
     * @param kafkaMsg
     * @param topic
     * @return
     */
    private String parseMsgcb20(byte[] kafkaMsg, String topic) {
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(kafkaMsg);
            Record parseFrom = MessageDbDrds.Record.parseFrom(bi);

            int optType = parseFrom.getOperationType(); // 操作类型(透传) 0 insert 1 update  2 delete
            String optTable = parseFrom.getTableName(); // 表名  都转成大写
            List<Column> columnList = parseFrom.getColumnList();
            List<Column> keyColumnList = parseFrom.getKeyColumnList();

            OracleEntry.incrReceivedFromKafkaOptCount(1);

            while (this.queue.remainingCapacity() <= 200) {
                try {
                    Thread.sleep(100l);
                } catch (InterruptedException e) {
                    log.warn("BQ does not have enough room to save operations!");
                }
            }

            String optOwner = "";

            if (OracleAttr.CHANGE_OWNER != null) {
                optOwner = OracleAttr.CHANGE_OWNER.get(topic);
            }
            StringBuilder optTableBuilder = new StringBuilder();
            optTableBuilder = new StringBuilder(optOwner + ".");

            String optSql = "";

            String tableTargetType = "";
            if (ConfAttr.TARGET_TABLES != null) {
                tableTargetType = ConfAttr.TARGET_TABLES.get(optTable);
            }

            if (StringUtil.notEmpty(tableTargetType,true) && (tableTargetType.equals("1") || tableTargetType.equals("4"))) {
                try {
                    optTableBuilder.append(optTable);
                    switch (optType) {
                        case 1:
                            optSql = OracleParser.jsonToUpdateOrUpdatePkSqlCb20(columnList, keyColumnList, optTableBuilder.toString());
                            break;
                        case 0:
                            optSql = OracleParser.jsonToInsertSqlCb20(columnList, optTableBuilder.toString());
                            break;
                        case 2:
                            optSql = OracleParser.jsonToDeleteSqlCb20(keyColumnList, optTableBuilder.toString());
                            break;
                        default:
                            log.error("Unaccepted operation:\n" + new String(kafkaMsg));
                            break;
                    }

                    JSONObject optSqlJson = new JSONObject();
                    optSqlJson.put("opt", optType);
                    optSqlJson.put("table", optTable);
                    optSqlJson.put("sql", optSql);
                    this.queue.add(optSqlJson);
                } catch (Exception e) {
                    log.error("Parse to SQL ERROR : getMessage - " + new String(kafkaMsg) + "\n" + e.getMessage(), e);
                }
            }
            return optTable;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String parseMsg(byte[] kafkaMsg) {
        try {
            CbOggMessage oggMsg = CbOggMessage.parse(kafkaMsg);
            OracleEntry.incrReceivedFromKafkaOptCount(1);

            while (this.queue.remainingCapacity() <= 200) {
                try {
                    Thread.sleep(100l);
                } catch (InterruptedException e) {
                    log.warn("BQ does not have enough room to save operations!");
                }
            }

            String optType =  new String(oggMsg.getOperate().name()).toUpperCase();
            String optTable = new String(oggMsg.getTableName());
            String optOwner = new String(oggMsg.getSchemeName());
            if (OracleAttr.CHANGE_OWNER != null) {
                optOwner = OracleAttr.CHANGE_OWNER.get(optOwner);
            }
            log.info("kafka_tableName---------->>" + optTable + "  " + optOwner);
            StringBuilder optTableBuilder = new StringBuilder();
            optTableBuilder = new StringBuilder(optOwner + ".");

            String optSql = "";

            if (OracleParser.checkTable(oggMsg)) {
                try {
                    optTableBuilder.append(optTable);
                    List<List> filedList = OracleParser.getFiledList();
                    List<List> primarykeyList = OracleParser.getPrimaryKeyList();

                    switch (oggMsg.getOperate()) {
                        case Update:
                        case Key:
                            optSql = OracleParser.jsonToUpdateOrUpdatePkSql(filedList, primarykeyList, optTableBuilder.toString());
                            break;
                        case Insert:
                            optSql = OracleParser.jsonToInsertSql(filedList, optTableBuilder.toString());
                            break;
                        case Delete:
                            optSql = OracleParser.jsonToDeleteSql(primarykeyList, optTableBuilder.toString());
                            break;
                        default:
                            log.error("Unaccepted operation:\n" + new String(kafkaMsg));
                            break;
                    }

                    if (ConfAttr.KAFKA_TARGET_TYPE.equals("1") || ConfAttr.KAFKA_TARGET_TYPE.equals("4")) {
                        JSONObject optSqlJson = new JSONObject();
                        optSqlJson.put("opt", optType);
                        optSqlJson.put("table", optTable);
                        optSqlJson.put("sql", optSql);
                        this.queue.add(optSqlJson);
                    }
                } catch (Exception e) {
                    log.error("Parse to SQL ERROR : getMessage - " + new String(kafkaMsg) + "\n" + e.getMessage(), e);
                }
                return optTable;
            } else {
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void adjustOffset(KafkaConsumer<byte[], byte[]> consumer) {
        Set<TopicPartition> assignments = consumer.assignment();
        switch (ConfAttr.KAFKA_OFFSET_ADJUST_TYPE) {
            case "0":
                break;
            case "1":
                assignments.forEach(topicPartition ->
                        consumer.seekToBeginning(
                                Arrays.asList(topicPartition)));
                break;
            case "2":
                assignments.forEach(topicPartition ->
                        consumer.seek(
                                topicPartition,
                                Integer.parseInt(ConfAttr.KAFKA_OFFSET_ADJUST_VALUE)));
                break;
            case "3":
                Map<TopicPartition, Long> query = new HashMap<>();
                for (TopicPartition topicPartition : assignments) {
                    query.put(
                            topicPartition,
                            Instant.now().minus(Integer.parseInt(ConfAttr.KAFKA_OFFSET_ADJUST_VALUE), MINUTES).toEpochMilli());
                }

                Map<TopicPartition, OffsetAndTimestamp> result = consumer.offsetsForTimes(query);

                result.entrySet()
                        .stream()
                        .forEach(entry ->
                                consumer.seek(
                                        entry.getKey(),
                                        Optional.ofNullable(entry.getValue())
                                                .map(OffsetAndTimestamp::offset)
                                                .orElse(new Long(0))));

        }
        adjustOffsetFlag = false;
    }

    public static void main(String[] args) {
        BlockingQueue<JSONObject> queue = new LinkedBlockingQueue<JSONObject>();
        ConsumeKafka kafkaConsumer = new ConsumeKafka(queue);
    }


}
