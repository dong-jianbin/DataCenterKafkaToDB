package com.asiainfo.datacenter.utils;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import java.util.Map;


/**
 * 
 * 配置文件properties自动加载类
 * @author 董建斌
 * @version on 2018/9/26.
 * @see PropertiesUtil
 * @since
 */
public class PropertiesUtil
{
    /**
     * 日志
     */
    private static final Logger log = Logger.getLogger(PropertiesUtil.class);

    /**
     * Singleton
     */
    private static final PropertiesUtil AUTO_LOAD = new PropertiesUtil();

    /**
     * Configuration
     */
    private static PropertiesConfiguration propConfig;

    /**
     * 自动保存
     */
    private static boolean autoSave = true;

    //TODO 写死在程序中的配置文件，配置文件
//    private final static String propertiesFile = "config.properties";
    private final static String propertiesFile = "conf/config.properties";

    /**
     * properties文件路径
     * @return
     * @see
     */
    public static PropertiesUtil getInstance()
    {
        //执行初始化
        init();

        return AUTO_LOAD;
    }

    /**
     * 根据Key获得对应的value
     * @param key
     * @return 
     * @see
     */
    public String getProperty(String key)
    {
        return String.valueOf(propConfig.getProperty(key));
    }

    /**
     * 获得对应的value数组
     * @param key
     * @return 
     * @see
     */
    public String[] getArrayFromPropFile(String key)
    {
        return propConfig.getStringArray(key);
    }

    /**
     * 设置属性
     * @param key
     * @param value 
     * @see
     */
    public void setProperty(String key, String value)
    {
        propConfig.setProperty(key, value);
    }

    /**
     * 设置属性
     * @param map 
     * @see
     */
    public void setProperty(Map<String, String> map)
    {
        for (String key : map.keySet())
        {
            propConfig.setProperty(key, map.get(key));
        }
    }

    /**
     * 构造器私有化
     */
    private PropertiesUtil()
    {

    }

    /**
     * 初始化
     * @see
     */
    private static void init( )
    {
        try
        {
            propConfig = new PropertiesConfiguration(propertiesFile);

            //自动重新加载
            propConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

            //自动保存
            propConfig.setAutoSave(autoSave);

            propConfig.addConfigurationListener(new ConfigurationListener() {
                @Override
                public void configurationChanged(ConfigurationEvent configurationEvent) {

                }
            });
        }
        catch (ConfigurationException e)
        {
            log.error(e.getMessage());
        }
    }



}
