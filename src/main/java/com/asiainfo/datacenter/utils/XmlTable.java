package com.asiainfo.datacenter.utils;

import com.asiainfo.datacenter.attr.OracleAttr;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by 董建斌 on 2018/9/26.
 */
public class XmlTable {

    private final static String xmlFile = OracleAttr.ORACLE_TABLES_META;
    //	private final static String xmlFile="D:\\workspace\\KafkaOggMessage\\etc\\xml\\tables_act.xml";
    public static  HashMap<String, ArrayList<String>> mapAllCol = new HashMap<String, ArrayList<String> >();
    public static  HashMap<String, ArrayList<String>> mapAllKey = new HashMap<String, ArrayList<String> >();
    public static  HashMap<String, ArrayList<String>> mapAllKeyPos = new HashMap<String, ArrayList<String> >();
    public static  HashMap<String, ArrayList<String>> mapAllProv = new HashMap<String, ArrayList<String> >();


    public static  long time;

    //遍历当前节点下的所有节点
    public static void listNodes(Element node){
        //首先获取当前节点的所有属性节点
        List<Attribute> list = node.attributes();
        //遍历属性节点  
        for(Attribute attribute : list){
            System.out.println("属性"+attribute.getName() +":" + attribute.getValue());
        }
        //如果当前节点内容不为空，则输出  
        if(!(node.getTextTrim().equals(""))){
            System.out.println( node.getName() + "：" + node.getText());
        }
        //同时迭代当前节点下面的所有子节点  
        //使用递归  
        Iterator<Element> iterator = node.elementIterator();
        while(iterator.hasNext()){
            Element e = iterator.next();
            listNodes(e);
        }
    }

    /**
     * 解析xml文件放入内存中 
     */
    public static void getAllColXML(){
        String path=xmlFile ;
        File f = new File(path);
        time=f.lastModified();
        System.out.println("time: " + time);
        SAXReader reader = new SAXReader();
        try {
            Document  doc = reader.read(f);
            //获取根节点元素对象
            Element element = doc.getRootElement();
            //遍历
            for (Iterator i = element.elementIterator("Table"); i.hasNext();) {
                Element  el=(Element)i.next();
                String type=el.attributeValue("type");
                System.out.println("type： " + type );
                ArrayList<String> colArray = new ArrayList<String> ();
                ArrayList<String> keyArray = new ArrayList<String> ();
                ArrayList<String> keyPosArray = new ArrayList<String> ();
                //同时迭代当前节点下面的所有子节点
                //使用递归
                Iterator<Element> iterator = el.elementIterator();
                int c=0;
                while(iterator.hasNext()){

                    Element e = iterator.next();
                    //如果当前节点内容不为空，则输出
                    if(!(e.getTextTrim().equals(""))){
                        String[] colVal = e.getText().split(",");
                        String colName = colVal[0];
                        String colKey = colVal[1];
                        String colType = colVal[2];
                        colArray.add(colName+","+colType);
                        if("1".equals(colKey)){
                            keyArray.add(c+","+colName);
                            keyPosArray.add(c+"");
                        }
                    }
                    mapAllCol.put(type, colArray);
                    mapAllKey.put(type, keyArray);
                    mapAllKeyPos.put(type, keyPosArray);
                    c++;
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
    /**
     * 输入参数，获取属性 
     *
     * 如果内存中没有，则先去解析，如果存在，则直接从内存中获取  
     * @param type 传入的参数
     * @return
     */
    public static ArrayList<String> getAllCol(String type){
//    	System.out.println("getAllCol");
        if(mapAllCol.size()==0){
            getAllColXML();
        }else if(getTime()!=time) {
            getAllColXML();
        }

        ArrayList<String> value=mapAllCol.get(type);
        return value;
    }

    public static ArrayList<String> getAllKey(String type){
//    	System.out.println("getAllKey");
        if(mapAllKey.size()==0){
            getAllColXML();
        }else if(getTime()!=time) {
            getAllColXML();
        }

        ArrayList<String> value=mapAllKey.get(type);
        return value;
    }

    public static ArrayList<String> getAllKeyPos(String type){
//    	System.out.println("getAllKeyPos");
        if(mapAllKeyPos.size()==0){
            getAllColXML();
        }else if(getTime()!=time) {
            getAllColXML();
        }

        ArrayList<String> value=mapAllKeyPos.get(type);
        return value;
    }
    /**
     * 获取到文件最后修改时间 
     * @return
     */
    public static long getTime(){
        String path = xmlFile;
        File f = new File(path);
        long  lastTime=f.lastModified();
        return lastTime;
    }

    public static void main(String[] args) {
        for(int i=0;i<50;i++){
            Long timeE0 = System.nanoTime();
            ArrayList<String> aa= XmlTable.getAllCol("UCR_ACT2.TI_B_RESOURCEINFO") ;

            for (int j=0;j<aa.size();j++){
                String col = aa.get(j);
                System.out.println("-------col:" + col);
            }

            ArrayList<String> cc= XmlTable.getAllKeyPos("UCR_ACT2.TI_B_RESOURCEINFO") ;

            for (int j=0;j<cc.size();j++){
                String keyPos = cc.get(j);
                System.out.println("-------keyPos:" + keyPos);
            }

            Long timeE1 = System.nanoTime();
            System.out.println("E1_elapsed(ns): "+(timeE1 - timeE0) );
        }

    }
}  
