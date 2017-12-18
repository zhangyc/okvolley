/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;

import java.io.File;

import okhttp3.OkHttpClient;

public class Volley {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * 创建一个默认的工作池
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack A {@link BaseHttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, BaseHttpStack stack,boolean useokhttp) {
        BasicNetwork network;

        if (stack == null) {
            //zhangyc start,判断是否使用okhttp
            if (useokhttp){
                //这个客户端应该是单粒的，这里以后做优化
                network=new BasicNetwork(new OkHttpStack(new OkHttpClient()));

            }else {
                //sdk版本大于9
                if (Build.VERSION.SDK_INT >= 9) {
                    network = new BasicNetwork(new HurlStack());
                } else {
                    //小于9
                    // Prior to Gingerbread, HttpUrlConnection was unreliable.
                    // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                    // At some point in the future we'll move our minSdkVersion past Froyo and can
                    // delete this fallback (along with all Apache HTTP code).
                    String userAgent = "volley/0";
                    try {
                        String packageName = context.getPackageName();
                        PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
                        userAgent = packageName + "/" + info.versionCode;
                    } catch (NameNotFoundException e) {
                    }
                    //小于9使用HttpClient
                    network = new BasicNetwork(
                            new HttpClientStack(AndroidHttpClient.newInstance(userAgent)));
                }
            }
        } else {
            network = new BasicNetwork(stack);
        }

        return newRequestQueue(context, network);
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     * @deprecated Use {@link #newRequestQueue(Context, BaseHttpStack)} instead to avoid depending
     *             on Apache HTTP. This method may be removed in a future release of Volley.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        if (stack == null) {
            return newRequestQueue(context, (BaseHttpStack) null);
        }
        return newRequestQueue(context, new BasicNetwork(stack));
    }
    //创建请求对列
    private static RequestQueue newRequestQueue(Context context, Network network) {
        //创建file
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        //创建带有缓存的请求队列，把请求缓存到本地
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        //启动这个请求队列的线程
        queue.start();
        return queue;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context,boolean useOkhttp) {
        //刚才起了一本地队列进行缓存
        return newRequestQueue(context, (BaseHttpStack) null,useOkhttp);
    }
}
