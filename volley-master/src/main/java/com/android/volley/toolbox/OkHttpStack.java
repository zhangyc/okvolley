package com.android.volley.toolbox;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;

import org.apache.http.protocol.HTTP;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

/**
 * Created by zhangyongchun on 2017/12/11.
 * 使用okhttp作为http请求客户端，来实现Volley
 */

public class OkHttpStack extends BaseHttpStack {
    //声明OKhttp客户端
    protected final OkHttpClient mClient;
    private final static String HEADER_CONTENT_TYPE = "Content-Type";
    //构造器初始化okhttp请求客户端


    public OkHttpStack(OkHttpClient client){
        mClient=client;
    }
    //设置请求头
    private  static void addHeaders(okhttp3.Request request,Map<String,String> headers){
        for (String key:headers.keySet()){

        }
    }


    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        ArrayList<Header>  headers=new ArrayList<>();//volley的header主要用来接收响应的头部信息
        FormBody realBody = null;//请求body
        Response okResponse;      //okhttp的响应
        String url=request.getUrl();  //获取请求url
        final okhttp3.Request.Builder okResquest = new okhttp3.Request.Builder().url(url);
        Map<String, String> requests = request.getPostParams();
        FormBody.Builder body=new FormBody.Builder();
        for (String key:requests.keySet()){
            body.add(key,requests.get(key));
        }
        //把请求信息都放入map中
        HashMap<String, String> map = new HashMap<>();
        //添加请求头
        map.putAll(request.getHeaders());
        //本地缓存请求头
        map.putAll(additionalHeaders);
        //为okhttp添加头部信息
        for (String key:map.keySet()){
            //为okhttp添加请求头
            okResquest.addHeader(key,map.get(key));
        }
        //如果为空,这里需要优化。。。。。。。。。。。。。。。。请记住2
        if (body==null){
            realBody=null;
        }else {
            realBody=body.build();
        }
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    okResquest.method("POST",realBody);
                }
                break;
            case Request.Method.GET:
                okResquest.method("GET",null);
                break;
            case Request.Method.DELETE:
                okResquest.method("DELETE",null);
                break;
            case Request.Method.POST:
                okResquest.method("POST",realBody);
                //如果body不为空，添加body
                break;
            case Request.Method.PUT:
                okResquest.method("PUT",realBody);
                //如果body不为空，添加body
                break;
            case Request.Method.HEAD:
                okResquest.method("HEAD",null);
                break;
            case Request.Method.OPTIONS:
                okResquest.method("OPTIONS",null);
                break;
            case Request.Method.TRACE:
                okResquest.method("TRACE",null);
                break;
            case Request.Method.PATCH:
                okResquest.method("PATCH",realBody);
                //如果body不为空，添加body
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
        Call call = mClient.newCall(okResquest.build());
        okResponse=call.execute();
        String response=okResponse.body().string();
        Headers okheaders = okResponse.headers();
        for (int i=0;i<okheaders.size();i++){
           headers.add(new Header(okheaders.name(i),okheaders.value(i)));
        }
        if (!hasResponseBody(request.getMethod(), okResponse.code())) {
            return new HttpResponse(okResponse.code(), headers);
        }
        return new HttpResponse(okResponse.code(),headers, okResponse.body().string().length(),new ByteArrayInputStream(response.getBytes()));
    }
    private static void addBody(HttpURLConnection connection, Request<?> request, byte[] body)
            throws IOException, AuthFailureError {
        // Prepare output. There is no need to set Content-Length explicitly,
        // since this is handled by HttpURLConnection using the size of the prepared
        // output stream.
        connection.setDoOutput(true);
        connection.addRequestProperty(
                HttpHeaderParser.HEADER_CONTENT_TYPE, request.getBodyContentType());
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.write(body);
        out.close();
    }
    /**
     * Checks if a response message contains a body.
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3">RFC 7230 section 3.3</a>
     * @param requestMethod request method
     * @param responseCode response status code
     * @return whether the response has a body
     */
    private static boolean hasResponseBody(int requestMethod, int responseCode) {
        return requestMethod != Request.Method.HEAD
                && !(HTTP_CONTINUE <= responseCode && responseCode < HttpURLConnection.HTTP_OK)
                && responseCode != HttpURLConnection.HTTP_NO_CONTENT
                && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED;
    }
}
