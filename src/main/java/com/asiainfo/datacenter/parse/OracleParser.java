package com.asiainfo.datacenter.parse;

import com.asiainfo.datacenter.attr.ConfAttr;
import com.asiainfo.datacenter.utils.XmlTable;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.asiainfo.datacenter.parse.MessageDbDrds;
import com.asiainfo.datacenter.parse.MessageDbDrds.Column;
import com.asiainfo.datacenter.parse.MessageDbDrds.Record;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class OracleParser {
    private static Logger log = Logger.getLogger(OracleParser.class.getName());

    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static XmlTable xml2table = new XmlTable();
    private static List<List> fieldList = new ArrayList<List>();
    private static List<List> primarykeyList = new ArrayList<List>();

    /**
     * checkTable 检查信息对应的表是否需要处理
     * @param oggMsg cb kafka信息结构体
     * @return
     */
    public static boolean checkTable( CbOggMessage oggMsg)  {
        try {
            fieldList = new ArrayList<List>();
            primarykeyList = new ArrayList<List>();
            String configTableName = new String(oggMsg.getSchemeName()) + "." + new String(oggMsg.getTableName());
            ArrayList<String> metaColslist = xml2table.getAllCol(configTableName);
            ArrayList<String> metaKeylist = xml2table.getAllKeyPos(configTableName);
            if (metaColslist != null && metaKeylist != null && metaColslist.size() > 0 && metaKeylist.size() > 0 ) {
                List<CbOggMessage.Column> msgColumns = oggMsg.getColumns();

                for (int i = 0;i < msgColumns.size();i++) {
                    CbOggMessage.Column msgColumn = msgColumns.get(i);
                    int keypos = msgColumn.getIndex();

                    String msgColumnCurrentValue = null;
                    if (msgColumn.getCurrentValue() != null ) {
                        msgColumnCurrentValue  = new String(msgColumn.getCurrentValue());
                    }
                    String msgColumnOldValue = null;
                    if (msgColumn.getOldValue() != null ) {
                        msgColumnOldValue = new String(msgColumn.getOldValue());
                    }

                    String[] colVal = metaColslist.get(keypos).split(",");
                    String metaColName = colVal[0];

                    if (ConfAttr.KAFKA_FILTER_PROV != null && ConfAttr.KAFKA_FILTER_PROV != "") {
                        if ("PROVINCE_CODE".equals(metaColName)) {
                            if (msgColumnCurrentValue != null && !msgColumnCurrentValue.equals(ConfAttr.KAFKA_FILTER_PROV)) {
                                return false;
                            }
                            if (msgColumnOldValue != null && !msgColumnOldValue.equals(ConfAttr.KAFKA_FILTER_PROV)) {
                                return false;
                            }
                        }
                    }

                    if (ConfAttr.KAFKA_FILTER_USERID != null && ConfAttr.KAFKA_FILTER_USERID != "") {
                        if ("USER_ID".equals(metaColName)) {
                            if (msgColumnCurrentValue != null && msgColumnCurrentValue.length() > 2 && msgColumnCurrentValue.substring(0,2) != ConfAttr.KAFKA_FILTER_USERID ) {
                                return false;
                            }
                            if (msgColumnOldValue != null && msgColumnOldValue.length() > 2 && msgColumnOldValue.substring(0,2) != ConfAttr.KAFKA_FILTER_USERID) {
                                return false;
                            }
                        }
                    }

                }

                switch (oggMsg.getOperate()) {
                    case Update:
                    case Key:
                        for (int i = 0;i < msgColumns.size();i++) {
                            CbOggMessage.Column msgColumn = msgColumns.get(i);
                            int keypos= msgColumn.getIndex();
                            String[] colVal = metaColslist.get(keypos).split(",");
                            String metaColName = colVal[0];

                            if(metaKeylist.contains(String.valueOf(keypos))){
                                if (msgColumn.isOldValueExist()) {
                                    if (msgColumn.getOldValue() != null) {
                                        primarykeyList.add(createField(i, metaColName, msgColumn.getOldValue()));
                                    }else{
                                        primarykeyList.add(createField(i, metaColName, "".getBytes()));
                                    }
                                }else{
                                    primarykeyList.add(createField(i, metaColName, "".getBytes()));
                                }
                                if (msgColumn.isCurrentValueExist()) {
                                    if (msgColumn.getCurrentValue() != null) {
                                        fieldList.add(createField(i, metaColName, msgColumn.getCurrentValue()));
                                    }else{
                                        fieldList.add(createField(i, metaColName, "".getBytes()));
                                    }
                                }else{
                                    fieldList.add(createField(i, metaColName, "".getBytes()));
                                }
                            }else{
                                if (msgColumn.isCurrentValueExist()) {
                                    if (msgColumn.getCurrentValue() != null) {
                                        fieldList.add(createField(i, metaColName, msgColumn.getCurrentValue()));
                                    }else{
                                        fieldList.add(createField(i, metaColName, "".getBytes()));
                                    }
                                }else{
                                    fieldList.add(createField(i, metaColName, "".getBytes()));
                                }
                            }
                        }
                        break;
                    case Insert:
                        for (int i = 0; i < metaColslist.size(); i++) {
                            CbOggMessage.Column msgColumn = msgColumns.get(i);
                            String[] colVal = metaColslist.get(i).split(",");
                            String metaColName = colVal[0];

                            if (msgColumn.isCurrentValueExist()) {
                                if (msgColumn.getCurrentValue() != null) {
                                    fieldList.add(createField(i, metaColName, msgColumn.getCurrentValue()));
                                } else {
                                    fieldList.add(createField(i, metaColName, "".getBytes()));
                                }
                            } else {

                            }
                        }
                        break;
                    case Delete:
                        for (int i = 0; i < msgColumns.size(); i++) {
                            CbOggMessage.Column msgColumn = msgColumns.get(i);
                            int keypos = msgColumn.getIndex();
                            String[] colVal = metaColslist.get(keypos).split(",");
                            String metaColName = colVal[0];

                            if (metaKeylist.contains(String.valueOf(keypos))) {
                                if (msgColumn.isOldValueExist()) {
                                    if (msgColumn.getOldValue() != null) {
                                        primarykeyList.add(createField(i, metaColName, msgColumn.getOldValue()));
                                    } else {
                                    }
                                } else {

                                }
                            }
                        }
                        break;
                    default:
                        log.error("Unaccepted operation:\n" + new String("非法动作类型"));
                        break;
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static List createField(int fieldPos, String fieldName,  byte[] fieldValue)  {
        try {
            List listFiled = new ArrayList();
            listFiled.add(fieldPos);
            listFiled.add(new StringBuffer("\"").append(fieldName).append("\"").toString());
//            listFiled.add( new String(fieldValue,"GB2312").toUpperCase());
            listFiled.add( new String(fieldValue,"GB2312"));
            return listFiled;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<List> getFiledList() {
        return fieldList;
    }

    public static List<List> getPrimaryKeyList() {
        return primarykeyList;
    }

    public static boolean isMatcher(String regex, String string) {
        String result = "";
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                result = matcher.group();
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        return true;
    }

    public static String formatDate(String dateString) {
        Date date = null;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            log.error(e.getLocalizedMessage());
        }
        return df.format(date);
    }

    public static String valueFormat(Object value) {
        String formatValume = "";
        if (value == null) {
            formatValume = null;
        } else if (isMatcher("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$", value
                .toString().trim())) {
            formatValume = formatDate(value.toString().trim());
            formatValume = "TO_DATE(" + "'" + formatValume + "'," + "'YYYY-MM-DD HH24:MI:SS')";
        } else if (value.toString().contains("'")) {
            formatValume = "'" + value.toString().replace("'", "''") + "'";
        } else {
            formatValume = "'" + value + "'";
        }
        return formatValume;

    }


    /**
     * 生成update操作的 sql
     *
     * @param filedList      操作list
     * @param primarykeyList 主键list
     * @param tableName
     * @return update sql
     */
    public static String jsonToUpdateOrUpdatePkSql(List<List> filedList, List<List> primarykeyList, String tableName) {

        String sql = "";
        String filedPart = "";
        String primaryKeyPart = "";
        for (int i = 0; i < filedList.size(); i++) {
            List list = filedList.get(i);
            String filed = list.get(1).toString();
            String valueFormat = valueFormat(list.get(2));
            if (filedList.size() != i + 1) {
                filedPart = filedPart + filed + "=" + valueFormat + ",";
            } else {
                filedPart = filedPart + filed + "=" + valueFormat;
            }
        }

        for (int i = 0; i < primarykeyList.size(); i++) {
            List list = primarykeyList.get(i);
            String filed = list.get(1).toString();
            String valueFormat = valueFormat(list.get(2));
            if (primarykeyList.size() != i + 1) {
                primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + " AND ";
            } else {
                primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + "";
            }
        }
        sql = "UPDATE " + tableName + " SET " + filedPart + " WHERE " + primaryKeyPart;
        log.info("DEBUG UPDATE SQL: " + sql);
        return sql;
    }

    /**
     * 生成 insert操作的 sql
     *
     * @param filedList 操作的数据list
     * @param tableName 表名
     * @return insert sql
     */
    public static String jsonToInsertSql(List<List> filedList, String tableName) {
        String sql = "";
        String filedPart = "(";
        String valePart = "(";
        for (int i = 0; i < filedList.size(); i++) {
            List list = filedList.get(i);
            String filed = list.get(1).toString();
            String valueFormat = valueFormat(list.get(2));
            if (filedList.size() != i + 1) {
                filedPart = filedPart + filed + ",";
                valePart = valePart + valueFormat + ",";
            } else {
                filedPart = filedPart + filed + ")";
                valePart = valePart + valueFormat + ")";
            }
        }
        sql = "INSERT INTO " + tableName + filedPart + " VALUES" + valePart;
        log.info("DEBUG INSERT SQL: " + sql);
        return sql;
    }

    /**
     * 生成delete操作的 sql
     *
     * @param primarykeyList
     * @param table
     * @return
     */
    public static String jsonToDeleteSql(List<List> primarykeyList, String table) {

        String sql = "";
        String primaryKeyPart = "";
        for (int i = 0; i < primarykeyList.size(); i++) {
            List list = (List) primarykeyList.get(i);
            if (primarykeyList.size() != i + 1) {
                primaryKeyPart = primaryKeyPart + list.get(1) + "=" + valueFormat(list.get(2)) + " AND ";
            } else {
                primaryKeyPart = primaryKeyPart + list.get(1) + "=" + valueFormat(list.get(2)) + "";
            }
        }

        sql = " DELETE FROM " + table + " WHERE " + primaryKeyPart;
        log.info("DEBUG DELETE SQL: " + sql);
        return sql;
    }

    /**
     * 生成update操作的 sql
     *
     * @param filedList      操作list
     * @param primarykeyList 主键list
     * @param tableName
     * @return update sql
     */
    public static String jsonToUpdateOrUpdatePkSqlCb20(List<Column> filedList, List<Column> primarykeyList, String tableName) {

        String sql = "";
        String filedPart = "";
        String primaryKeyPart = "";
        for (int i = 0; i < filedList.size(); i++) {
            Column column = filedList.get(i);
            if (column.getNewHave() && !column.getNewNull()) {
                String filed = column.getName();
                String valueFormat = valueFormat(column.getNewValue().toStringUtf8());
                filedPart = filedPart + filed + "=" + valueFormat + ",";
            }
        }

        for (int i = 0; i < primarykeyList.size(); i++) {
            Column column = primarykeyList.get(i);
            if (column.getOldHave() && !column.getOldNull()) {
                String filed = column.getName();
                String valueFormat = valueFormat(column.getOldValue().toStringUtf8());
                if (primarykeyList.size() != i + 1) {
                    primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + " AND ";
                } else {
                    primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + "";
                }
            }
        }
        filedPart = filedPart.substring(0,filedPart.length() - 1);
        sql = "UPDATE " + tableName + " SET " + filedPart + " WHERE " + primaryKeyPart;
        log.info("DEBUG UPDATE SQL: " + sql);
        return sql;
    }

    /**
     * 生成 insert操作的 sql
     *
     * @param filedList 操作的数据list
     * @param tableName 表名
     * @return insert sql
     */
    public static String jsonToInsertSqlCb20(List<Column> filedList, String tableName) {
        String sql = "";
        String filedPart = "(";
        String valePart = "(";
        for (int i = 0; i < filedList.size(); i++) {
            Column column = filedList.get(i);
            if (column.getNewHave() && !column.getNewNull()) {
                String filed = column.getName();
                String valueFormat = valueFormat(column.getNewValue().toStringUtf8());
                filedPart = filedPart + filed + ",";
                valePart = valePart + valueFormat + ",";
            }
        }
        filedPart = filedPart.substring(0,filedPart.length() - 1) + ")";
        valePart = valePart.substring(0,valePart.length() - 1) + ")";

        sql = "INSERT INTO " + tableName + filedPart + " VALUES" + valePart;
        log.info("DEBUG INSERT SQL: " + sql);
        return sql;
    }

    /**
     * 生成delete操作的 sql
     *
     * @param primarykeyList
     * @param table
     * @return
     */
    public static String jsonToDeleteSqlCb20(List<Column> primarykeyList, String table) {

        String sql = "";
        String primaryKeyPart = "";
        for (int i = 0; i < primarykeyList.size(); i++) {
            Column column = primarykeyList.get(i);
            if (column.getOldHave() && !column.getOldNull()) {
                String filed = column.getName();
                String valueFormat = valueFormat(column.getOldValue().toStringUtf8());
                if (primarykeyList.size() != i + 1) {
                    primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + " AND ";
                } else {
                    primaryKeyPart = primaryKeyPart + filed + "=" + valueFormat + "";
                }
            }
        }

        sql = " DELETE FROM " + table + " WHERE " + primaryKeyPart;
        log.info("DEBUG DELETE SQL: " + sql);
        return sql;
    }

}
