package me.ag2s.cronet;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


/**
 * 自动注册CronetLoader预加载
 */
public class CronetContentProvider extends ContentProvider {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    public CronetContentProvider() {
    }

    public static Context getCtx() {
        if (mContext == null) {
            mContext = initAndGetAppCtxWithReflection();
        }
        return mContext;
    }

    /**
     * 初始化CronetLoader 预加载
     */
    @Override
    public boolean onCreate() {
        mContext = getContext().getApplicationContext();
        CronetLoader.getInstance().preDownload();
        //CronetLoader.getInstance(getContext().getApplicationContext()).preDownload();
        return false;
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


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}