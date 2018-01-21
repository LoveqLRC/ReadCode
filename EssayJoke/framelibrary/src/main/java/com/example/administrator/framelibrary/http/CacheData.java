package com.example.administrator.framelibrary.http;

/**
 * Email 240336124@qq.com
 * Created by Darren on 2017/3/12.
 * Version 1.0
 * Description: 缓存的实体类
 */
public class CacheData {
    // 请求链接
    private String mUrlKey;

    // 后台返回的Json
    private String mResultJson;

    public CacheData() {

    }

    public CacheData(String urlKey, String resultJson) {
        this.mUrlKey = urlKey;
        this.mResultJson = resultJson;
    }

    public String getResultJson() {
        return mResultJson;
    }
}
