package me.ag2s.demo;

import android.util.Log;
import android.webkit.URLUtil;

import org.chromium.net.impl.ImplVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.ag2s.cronetlib.CronetClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.ByteString;


public class HttpTool {
    public static final String HTTPERROR = "error:";






    public static String httpGet(String url) {
        return httpGet(url, new HashMap<>());
    }

    public static String httpGet(String url, HashMap<String, String> header) {
        if (url == null || !URLUtil.isNetworkUrl(url)) {
            return HTTPERROR + "url is null";
        }
        OkHttpClient client = CronetClient.getInstance().getOkhttpClient();
        Request.Builder requestbuilder = new Request.Builder().get().url(url);
        requestbuilder.header("Referer", url);
        requestbuilder.header("dnt", "1");
        requestbuilder.removeHeader("User-Agent");
        requestbuilder.header("User-Agent", UA);
        requestbuilder.header("sec-ch-ua-mobile", "?1");
        header.put("upgrade-insecure-requests", "1");
        requestbuilder.header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,ru;q=0.6,ko;q=0.5");

        for (Map.Entry<String, String> entry : header.entrySet()) {
            requestbuilder.removeHeader(entry.getKey());
            requestbuilder.addHeader(entry.getKey(), entry.getValue());
        }

        Request request = requestbuilder.build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return Objects.requireNonNull(response.body()).string();
            } else {
                return HTTPERROR + response.message() + " errorcode:" + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HTTPERROR + Log.getStackTraceString(e);
        }
    }


    public static final String PcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ImplVersion.getCronetVersion() + " Safari / 537.36";
    public static final String UA = "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ImplVersion.getCronetVersion() + " Mobile Safari/537.3";


}
