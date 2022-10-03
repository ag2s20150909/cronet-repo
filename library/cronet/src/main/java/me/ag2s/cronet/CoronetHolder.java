package me.ag2s.cronet;

import android.content.Context;

import org.chromium.net.CronetEngine;
import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.MyCronetEngine;
import org.json.JSONObject;

import java.io.File;

@SuppressWarnings("unused")
public class CoronetHolder {
    private static ExperimentalCronetEngine engine;


    public static void setEngine(ExperimentalCronetEngine engine) {
        CoronetHolder.engine = engine;
    }

    public static ExperimentalCronetEngine getEngine() {
        if (engine == null) {
            synchronized (CoronetHolder.class) {
                if (engine == null) {
                    engine = createDefaultCronetEngine(CronetInitializer.getCtx());
                }
            }
        }
        return engine;
    }

    private static String getExperimentalOptions() {
        JSONObject options = new JSONObject();

        try {
            JSONObject dnsSvcb = new JSONObject();
            dnsSvcb.put("enable", true);
            dnsSvcb.put("enable_insecure", true);
            dnsSvcb.put("use_alpn", true);
            options.put("UseDnsHttpsSvcb", dnsSvcb);

            options.put("AsyncDNS", new JSONObject("{'enable':true}"));

        } catch (Exception ignored) {

        }

        return options.toString();
    }

    private static ExperimentalCronetEngine createDefaultCronetEngine(Context context) {
        MyCronetEngine.Builder builder = new MyCronetEngine.Builder(context)
                .enableHttp2(true)
                .setStoragePath(new File(context.getExternalCacheDir(), "cronet").getAbsolutePath())////设置缓存路径
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, (1024 * 1024 * 50))//设置50M的磁盘缓存
                .enableQuic(true)//设置支持http/3
                .enableHttp2(true)  //设置支持http/2
                .setExperimentalOptions(getExperimentalOptions());
        builder.enableBrotli(true);//Brotli压缩

        return builder.build();
    }
}
