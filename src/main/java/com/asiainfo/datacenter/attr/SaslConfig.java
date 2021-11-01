package com.asiainfo.datacenter.attr;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: kafkaclientdemo
 * @description:
 * @author: 董建斌
 * @create: on 2018/9/26.
 **/
public class SaslConfig extends Configuration {
    private String appKey;    //登陆认证用户名
    private String secretKey; //登陆认证密码

    public SaslConfig(String appKey, String secretKey) {
        this.appKey = appKey;
        this.secretKey = secretKey;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("username", appKey);
        options.put("password", secretKey);
        AppConfigurationEntry entry = new AppConfigurationEntry(
                "org.apache.kafka.common.security.plain.PlainLoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
        AppConfigurationEntry[] configurationEntries = new AppConfigurationEntry[1];
        configurationEntries[0] = entry;
        return configurationEntries;
    }
}