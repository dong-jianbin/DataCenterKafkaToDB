package com.asiainfo.datacenter.attr;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class OracleAttr {

    public static String FILEDVALUE = "META-FILEDVALUE";
    public static String PRIMARYKEY = "META-PRIMARYKEY";
    public static String DATABASE = "META-DATABASE";
    public static String OWNER = "META-OWNER";
    public static String PERATEIONTYPE = "META-PERATEIONTYPE";
    public static String TABLE = "META-TABLE";

    /**
     * 插入操作
     */
    public static String INSERT = "INSERT";
    /**
     * update操作
     */
    public static String UPDATE = "UPDATE_FIELDCOMP";
    /**
     * update pk操作
     */
    public static String UPDATEPK = "UPDATE_FIELDCOMP_PK";
    /**
     * delete操作
     */
    public static String DELETE = "DELETE";

    //以下为可修改的常量
    public static String SSO_ERROR_TABLE = "SSOUSER_OGG.GOME_USER";
    public static String SSO_ERROR_COLUMN = "USER_ID";
    public static String SSO_CORRECT_COLUMN = "ID";

    /**
     * 需要修改的Oracle Owner名
     */
//    public static String CHANGE_OWNER = "UCR_CRM2:CB_UCR_CRM2";
    public static HashMap<String, String> CHANGE_OWNER = new HashMap<String, String>();


    /**
     * 一次commit到Oracle的条数
     */
    public static int ORACLE_BATCH_NUM = 2000;

    /**
     * 写入Oracle的线程数
     */
    public static int TO_ORACLE_THREAD_NUM = 1;

    /**
     * Oracle表元数据文件
     */
    public static String ORACLE_TABLES_META = "xml.tables";

}
