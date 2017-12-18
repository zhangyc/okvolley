package com.android.volley.toolbox;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;

/**
 * Created by zhangyongchun on 2017/11/23.
 * 自定义一个xml请求
 */

public class XmlRequest extends Request<XmlPullParser> {

    private Response.Listener<XmlPullParser> responseListener;

    public XmlRequest(int method, String url, Response.Listener<XmlPullParser> mlistener, Response.ErrorListener listener) {
        super(method, url, listener);
        responseListener=mlistener;
    }

    @Override
    protected Response<XmlPullParser> parseNetworkResponse(NetworkResponse response){
        try {
            String xmlString =new String(response.data,"utf-8");
            XmlPullParserFactory factory=XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser=factory.newPullParser();
            //这里需要补充
            xmlPullParser.setInput(new StringReader(xmlString));
            return Response.success(xmlPullParser,HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (XmlPullParserException e) {
            return Response.error(new ParseError(e));
        }
    }

    /***
     * 交付响应
     * @param response The parsed response returned by
     */
    @Override
    protected void deliverResponse(XmlPullParser response) {
        responseListener.onResponse(response);
    }
}
