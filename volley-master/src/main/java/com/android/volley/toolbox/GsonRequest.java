package com.android.volley.toolbox;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.google.gson.Gson;

import org.xmlpull.v1.XmlPullParserException;

import java.io.UnsupportedEncodingException;

/**
 * Created by zhangyongchun on 2017/11/23.
 *
 */

public class GsonRequest<T> extends Request<T> {
    private Response.Listener<T> mlistener;
    private Gson gson;
    Class<T> tClass;
    public GsonRequest(int method, String url,Class<T> clazz,Response.Listener<T> reslistener, Response.ErrorListener errorlistener) {
        super(method, url, errorlistener);
        mlistener=reslistener;
        tClass=clazz;
        gson=new Gson();

    }

    /***
     *
     * @param response Response from the network
     * @return
     * @throws XmlPullParserException
     */
    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) throws XmlPullParserException {
        try {
            String jsonString =new String(response.data,"utf-8");
            return Response.success(gson.fromJson(jsonString,tClass),HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(T response) {
        mlistener.onResponse(response);

    }
}
