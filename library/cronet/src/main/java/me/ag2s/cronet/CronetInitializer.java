package me.ag2s.cronet;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import org.chromium.base.ThreadUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CronetInitializer implements Initializer<Void> {


    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    public static Context getCtx() {
        if (mContext == null) {
            mContext = initAndGetAppCtxWithReflection();
        }
        return mContext;
    }

    /**
     * 反射获取Context
     */
    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
    private static Context initAndGetAppCtxWithReflection() {
        // Fallback, should only run once per non default process.
        try {
            return (Context) Class.forName("android.app.ActivityThread").getDeclaredMethod("currentApplication").invoke(null);
        } catch (Exception e) {
            return null;
        }

    }

    private static void disableThreadCheck() {
        try {
            Field field  = ThreadUtils.class.getDeclaredField("sThreadAssertsDisabledForTesting");
            field.setAccessible(true);
            field.set(null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes and a component given the application {@link Context}
     *
     * @param context The application context.
     */
    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        mContext = context;
        disableThreadCheck();
        CronetPreloader.getInstance().preDownload();
        return null;
    }

    /**
     * @return A list of dependencies that this {@link Initializer} depends on. This is
     * used to determine initialization order of {@link Initializer}s.
     * <br/>
     * For e.g. if a {@link Initializer} `B` defines another
     * {@link Initializer} `A` as its dependency, then `A` gets initialized before `B`.
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return new ArrayList<>();
    }


}
