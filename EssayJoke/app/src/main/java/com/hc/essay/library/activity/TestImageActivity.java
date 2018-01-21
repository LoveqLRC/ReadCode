package com.hc.essay.library.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.example.administrator.framelibrary.BaseSkinActivity;
import com.hc.essay.library.R;
import com.hc.essay.library.selectimage.ImageSelector;
import com.hc.essay.library.selectimage.SelectImageActivity;
import com.hc.essay.library.util.ImageUtil;
import com.hc.essay.library.util.PatchUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Email 240336124@qq.com
 * Created by Darren on 2017/4/9.
 * Version 1.0
 * Description:
 */
public class TestImageActivity extends BaseSkinActivity{
    private ArrayList<String> mImageList;
    private final int SELECT_IMAGE_REQUEST = 0x0011;

    private String mPatchPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            +File.separator+"version_1.0_2.0.patch";

    private String mNewApkPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            +File.separator+"Version2.0.apk";

    @Override
    protected void initData() {
        // 1.访问后台接口，需不需要更新版本

        // 2.需要更新版本，那么提示用需要下载 （腾讯视频）,
        //   直接下载，然后提示用户更新

        // 3.下载完差分包之后，调用我们的方法去合并生成新的apk
        // 是一个耗时操作，怎么弄 开线程+，Handler, AsyncTask , RXJava
        // 本地apk路径怎么来，已经被安装了  1.0
        // 获取本地的getPackageResourcePath()apk路径
        if(!new File(mPatchPath).exists()){
            return;
        }
        PatchUtils.combine(getPackageResourcePath(),mNewApkPath,mPatchPath);

        // 4.需要校验签名    就是获取本地apk的签名，与我们新版本的apk作对比
        // 怎么获取2.0版本apk的签名  百度 灵感 系统是不是会校验，系统的安装apk那个应用看它是怎么校验的

        // 5.安装最新版本 (网上搜索怎么安装apk)
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mNewApkPath)),
                "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    protected void initView() {

    }


    @Override
    protected void initTitle() {

    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_test_image);
    }

    // 选择图片
    public void selectImage(View view){
        // 6.0 请求权限，危险权限，读取内存卡，拍照

        // 这样传递参数有没有问题，有问题打个1 ， 没问题 2
        // 没问题，但是不符合框架的思想，你知道的太多了，在公司里面是我写的，写完给别人用，
        // 用可能SelectImageActivity 别人是看不到的只能用，中间搞一层不要让开发者关注太多
        /*Intent intent = new Intent(this,SelectImageActivity.class);
        intent.putExtra(SelectImageActivity.EXTRA_SELECT_COUNT,9);
        intent.putExtra(SelectImageActivity.EXTRA_SELECT_MODE,SelectImageActivity.MODE_MULTI);
        intent.putStringArrayListExtra(SelectImageActivity.EXTRA_DEFAULT_SELECTED_LIST, mImageList);
        intent.putExtra(SelectImageActivity.EXTRA_SHOW_CAMERA, true);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);*/

        // 第一个只关注想要什么，良好的封装性，不要暴露太多
        ImageSelector.create().count(9).multi().origin(mImageList)
                .showCamera(true).start(this, SELECT_IMAGE_REQUEST);
    }

    public void compressImg(View view){


        // 把选择好的图片做了一下压缩
        for (String path : mImageList) {
            // 做优化  第一个decodeFile有可能会内存移除
            // 一般后台会规定尺寸  800  小米 规定了宽度 720
            // 上传的时候可能会多张 for循环 最好用线程池 （2-3）
            Bitmap bitmap = ImageUtil.decodeFile(path);
            // 调用写好的native方法
            // 用Bitmap.compress压缩1/10
            ImageUtil.compressBitmap(bitmap, 75,
                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                            new File(path).getName()
            );
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_IMAGE_REQUEST && data != null){
                mImageList = data.getStringArrayListExtra(SelectImageActivity.EXTRA_RESULT);
                // 做一下显示
                Log.e("TAG",mImageList.toString());
            }
        }
    }
}
