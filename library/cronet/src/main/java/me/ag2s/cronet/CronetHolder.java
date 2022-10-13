package me.ag2s.cronet;

import android.content.Context;

import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.MyCronetEngine;
import org.json.JSONObject;

@SuppressWarnings("unused")
public class CronetHolder {
    private static volatile ExperimentalCronetEngine engine;

    public static ExperimentalCronetEngine getEngine() {
        if (engine == null) {
            synchronized (CronetHolder.class) {
                if (engine == null) {
                    engine = createDefaultCronetEngine(CronetInitializer.getCtx());
                }
            }
        }
        return engine;
    }

    public static void setEngine(ExperimentalCronetEngine engine) {
        CronetHolder.engine = engine;
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
                .enableQuic(true)//设置支持http/3
                .enableHttp2(true)  //设置支持http/2
                .setExperimentalOptions(getExperimentalOptions());
        builder.enableBrotli(true);//Brotli压缩

        return builder.build();
    }
}
