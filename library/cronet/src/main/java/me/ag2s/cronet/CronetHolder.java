package me.ag2s.cronet;

import android.content.Context;

import androidx.annotation.NonNull;

import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.MyCronetEngine;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public class CronetHolder {
    private static final Object lock = new Object();

    private static volatile WeakReference<ExperimentalCronetEngine> engine;
    private static volatile WeakReference<Executor> executorService;

    @NonNull
    public static Executor getExecutor() {
        if (executorService == null || executorService.get() == null) {
            synchronized (lock) {
                if (executorService == null || executorService.get() == null) {

                    executorService = new WeakReference<>(createDefaultExecutorService());
                }
            }
        }
        return executorService.get();
    }

    public static void setExecutor(@NonNull Executor executorService) {
        CronetHolder.executorService = new WeakReference<>(executorService);
    }

    @NonNull
    private static Executor createDefaultExecutorService() {
        return DirectExecutor.INSTANCE;
    }

    @NonNull
    public static ExperimentalCronetEngine getEngine() {
        if (engine == null || engine.get() == null) {
            synchronized (lock) {
                if (engine == null || engine.get() == null) {
                    engine = new WeakReference<>(createDefaultCronetEngine(CronetInitializer.getCtx()));
                }
            }
        }
        return engine.get();
    }

    public static void setEngine(@NonNull ExperimentalCronetEngine engine) {
        CronetHolder.engine = new WeakReference<>(engine);
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
