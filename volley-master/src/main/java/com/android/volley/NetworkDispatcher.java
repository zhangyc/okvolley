/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley;

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing network dispatch from a queue of requests.
 *
 * Requests added to the specified queue are processed from the network via a
 * specified {@link Network} interface. Responses are committed to cache, if
 * eligible, using a specified {@link Cache} interface. Valid responses and
 * errors are posted back to the caller via a {@link ResponseDelivery}.
 * 网络线程
 */
public class NetworkDispatcher extends Thread {

    /** The queue of requests to service.请求服务的队列。阻塞队列 */
    private final BlockingQueue<Request<?>> mQueue;
    /** The network interface for processing requests. */
    private final Network mNetwork;
    /** The cache to write to. */
    private final Cache mCache;
    /** For posting responses and errors. */
    private final ResponseDelivery mDelivery;
    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    /**
     * Creates a new network dispatcher thread.  You must call {@link #start()}
     * in order to begin processing.
     *
     * @param queue Queue of incoming requests for triage传入的分流请求队列
     * @param network Network interface to use for performing requests网络接口用来执行请求
     * @param cache Cache interface to use for writing responses to cache缓存接口用来把响应写入缓存
     * @param delivery Delivery interface to use for posting responses调度接口用来回复响应
     */
    public NetworkDispatcher(BlockingQueue<Request<?>> queue,
            Network network, Cache cache, ResponseDelivery delivery) {
        mQueue = queue;
        mNetwork = network;
        mCache = cache;
        mDelivery = delivery;
    }

    /**
     * Forces this dispatcher to quit immediately.  If any requests are still in
     * the queue, they are not guaranteed to be processed.
     * 迫使这个调度员立即退出。 如果有任何请求仍在队列中，则不保证被处理。
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void addTrafficStatsTag(Request<?> request) {
        // Tag the request (if API >= 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //设置线程状态标识
            TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
        }
    }

    @Override
    public void run() {
        //根据Linux的优先级设置调用线程的优先级。
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                processRequest();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
            }
        }
    }

    // Extracted to its own method to ensure locals have a constrained liveness scope by the GC.
    // This is needed to avoid keeping previous request references alive for an indeterminate amount
    // of time. Update consumer-proguard-rules.pro when modifying this. See also
    // https://github.com/google/volley/issues/114
    private void processRequest() throws InterruptedException {
        //返回启动以来的毫秒数，包括睡眠时间。
        long startTimeMs = SystemClock.elapsedRealtime();
        // Take a request from the queue.从队列中取一个请求
        Request<?> request = mQueue.take();
        try {
            //添加到请求日志中
            request.addMarker("network-queue-take");

            // If the request was cancelled already, do not perform the
            // network request.如果请求已被取消，请勿执行网络请求。
            if (request.isCanceled()) {
                request.finish("network-discard-cancelled");
                request.notifyListenerResponseNotUsable();
                return;
            }

            addTrafficStatsTag(request);

            // Perform the network request.执行
             NetworkResponse networkResponse = mNetwork.performRequest(request);
            //添加日志，执行完成
            request.addMarker("network-http-complete");
            // If the server returned 304 AND we delivered a response already,
            // we're done -- don't deliver a second identical response.
            //如果服务器返回304，我们已经发送了一个响应，
            //我们完成了 - 不要传递第二个相同的回应。
            if (networkResponse.notModified && request.hasHadResponseDelivered()) {
                request.finish("not-modified");
                request.notifyListenerResponseNotUsable();
                return;
            }

            // Parse the response here on the worker thread.解析这个响应在工作线程
            Response<?> response = request.parseNetworkResponse(networkResponse);
            //添加日志，解析完成
            request.addMarker("network-parse-complete");
            // Write to cache if applicable.如果容许写入缓存
            // TODO: Only update cache metadata instead of entire record for 304s.
            //只更新缓存元数据，而不是整个记录的304。
            if (request.shouldCache() && response.cacheEntry != null) {
                mCache.put(request.getCacheKey(), response.cacheEntry);
                request.addMarker("network-cache-written");
            }

            // Post the response back.返回响应
            request.markDelivered();
            //发送响应
            mDelivery.postResponse(request, response);
            //请求通知监听器响应已发送
            request.notifyListenerResponseReceived(response);
        } catch (VolleyError volleyError) {
            volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            //发生错误返回错误
            parseAndDeliverNetworkError(request, volleyError);
            request.notifyListenerResponseNotUsable();
        } catch (Exception e) {
            VolleyLog.e(e, "Unhandled exception %s", e.toString());
            VolleyError volleyError = new VolleyError(e);
            volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
            mDelivery.postError(request, volleyError);
            request.notifyListenerResponseNotUsable();
        }
    }

    private void parseAndDeliverNetworkError(Request<?> request, VolleyError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
}
