/**
 * @Title: StringUtil.java
 * @Package service
 * @Description: TODO
 * @author lujia
 * @date 2014年9月11日 下午2:32:43
 * @version V1.0
 */
package com.asiainfo.datacenter.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author lujia
 * @version V1.0
 * @Title: StringUtil.java
 * @Package service
 * @Description: TODO
 * @date on 2018/9/26.
 */
public class StringUtil {
    private static Logger log = Logger.getLogger(StringUtil.class.getName());

    public static boolean isEmpty(String str, boolean trim) {
        if (trim) {
            return null == str || "".equals(str.trim());
        }
        return null == str || "".equals(str);
    }

    public static boolean notEmpty(String str, boolean trim) {
        return !isEmpty(str, trim);
    }

    public static SortedMap<String, String> mapSortByKey(Map<String, String> unsort_map) {
        TreeMap<String, String> result = new TreeMap<String, String>();
        Object[] unsort_key = unsort_map.keySet().toArray();
        Arrays.sort(unsort_key);
        for (int i = 0; i < unsort_key.length; i++) {
            result.put(unsort_key[i].toString(), unsort_map.get(unsort_key[i]));
        }
        return result.tailMap(result.firstKey());
    }

    public static int String2Int(String input) {
        int output = -1;
        try {
            output = Integer.parseInt(input);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return output;
    }

    public static String convertDate(String strDate) {
        String result = strDate;
        try {
            DateFormat sourcedf = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss", Locale.US);
            Date sourceDate = sourcedf.parse(strDate);
            DateFormat targetdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            result = targetdf.format(sourceDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String readFile(String localFile) {
        String content = "";
        try {
            File file = new File(localFile);
            FileInputStream is = new FileInputStream(localFile);
            int size = (int) file.length();
            byte[] bytes = getBytes(is, size);
            content = new String(bytes, "utf-8");
            is.close();
        } catch (IOException ex) {
            log.error("Read mapping error!\n" + ex.getMessage());
            ex.printStackTrace();
        }
        return content;
    }

    private static byte[] getBytes(InputStream inputStream, int size) {
        byte[] bytes = new byte[size];
        try {
            int readBytes = inputStream.read(bytes);
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
