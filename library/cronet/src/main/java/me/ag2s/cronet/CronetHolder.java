package me.ag2s.cronet;

import android.content.Context;

import androidx.annotation.NonNull;

import org.chromium.net.CronetEngine;
import org.chromium.net.MyCronetEngine;
import org.chromium.net.MyCronetHelper;
import org.json.JSONObject;

import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public class CronetHolder {
    private static final Object lock = new Object();

    private static volatile CronetEngine engine=null;
    private static volatile Executor executorService;

    @NonNull
    public static Executor getExecutor() {
        if (executorService == null) {
            synchronized (lock) {
                if (executorService == null) {
                    executorService = createDefaultExecutorService();
                }
            }
        }
        return executorService;
    }

    public static void setExecutor(@NonNull Executor executorService) {
        CronetHolder.executorService = executorService;
    }

    @NonNull
    private static Executor createDefaultExecutorService() {
        return DirectExecutor.INSTANCE;
    }

    @NonNull
    public static CronetEngine getEngine() {
        if (engine== null) {
            synchronized (lock) {
                if (engine == null) {
                    engine= createDefaultCronetEngine(CronetInitializer.getCtx());
                }
            }
        }
        return engine;
    }

    public static void setEngine(@NonNull CronetEngine engine) {

        synchronized (lock) {
            CronetHolder.engine=engine;
        }
        Runtime.getRuntime().gc();

    }

    @NonNull
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

    @NonNull
    private static CronetEngine createDefaultCronetEngine(Context context) {

        CronetEngine.Builder builder = new MyCronetEngine.Builder(MyCronetHelper.createBuilderDelegate(context))
                .setExperimentalOptions(getExperimentalOptions())
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY,100)
                .enableHttp2(true)
                .enableQuic(true)//设置支持http/3
                .enableHttp2(true);//设置支持http/2
        builder.enableBrotli(true);//Brotli压缩

        return builder.build();
    }
}
