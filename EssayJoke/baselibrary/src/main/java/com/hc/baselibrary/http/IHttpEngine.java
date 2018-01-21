package com.hc.baselibrary.http;

import android.content.Context;

import java.util.Map;

/**
 * Email 240336124@qq.com
 * Created by Darren on 2017/3/4.
 * Version 1.0
 * Description:  引擎的规范
 */
public interface IHttpEngine {

    // get请求
    void get(boolean cache,Context context,String url,Map<String,Object> params,EngineCallBack callBack);

    // post请求
    void post(boolean cache,Context context,String url,Map<String,Object> params,EngineCallBack callBack);

    // 下载文件


    // 上传文件


    // https 添加证书
}