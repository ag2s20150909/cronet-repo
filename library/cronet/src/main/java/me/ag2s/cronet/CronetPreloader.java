package me.ag2s.cronet;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.net.CronetProviderInstaller;
import com.huawei.hms.hquic.HQUICManager;

import org.chromium.net.impl.ImplVersion;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CronetPreloader {
    public static final String PREF_CRONET_SO = "prefCronetSo";
    private static final String TAG = "CronetLoader";

    /**
     * 打包时是否包含Cronet so
     */
    public final boolean includeCronetSo = BuildConfig.includeCronetSo;
    // 改为非 final，支持延迟初始化以防构造函数阻塞主线程
    public boolean includeCronetApkSo;

    private final Context mContext;
    private final String soName = "libcronet." + ImplVersion.getCronetVersion() + ".so";
    private final String soUrl;
    public final File soFile;
    private final File downloadFile;
    private final File parentDir;
    private final String CPU_ABI;

    // 延迟加载字段
    private String md5;
    private JSONObject json;
    private boolean isInitialized = false;

    public final boolean isGMS;
    public final boolean isHMS;
    public boolean prefSo = false;

    private volatile CronetState ins = CronetState.Java;

    // 改为实例变量，更符合单例面向对象设计
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    CronetPreloader() {
        mContext = CronetInitializer.getCtx();
        CPU_ABI = getCpuAbi(mContext);

        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                prefSo = appInfo.metaData.getBoolean(PREF_CRONET_SO, false);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Get meta data failed", e);
        }

        soUrl = "https://storage.googleapis.com/chromium-cronet/android/"
                + ImplVersion.getCronetVersion() + "/Release/cronet/libs/"
                + CPU_ABI + "/" + soName;

        parentDir = mContext.getDir("cronet", Context.MODE_PRIVATE);
        soFile = new File(parentDir, soName);
        downloadFile = new File(mContext.getCacheDir() + "/so_download", soName);

        isGMS = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS;
        isHMS = isHW();

        Log.i(TAG, "Init: GMS=" + isGMS + ", HMS=" + isHMS + ", ABI=" + CPU_ABI);
    }

    /**
     * 确保耗时的 I/O 操作（读 Json、检查 APK）在后台或首次使用时执行，防止 ANR
     */
    private synchronized void ensureInitialized() {
        if (isInitialized) return;

        json = initJson(mContext);
        md5 = getMd5(CPU_ABI);
        includeCronetApkSo = checkApk();
        isInitialized = true;

        Log.i(TAG, "Initialized: includeCronetApkSo=" + includeCronetApkSo + ", md5=" + md5);
    }

    public static CronetPreloader getInstance() {
        return CronetPreloaderHolder.instance;
    }

    /**
     * 下载并拷贝文件
     */
    private void download(final String url, final String md5, final File downloadTempFile, final File destSuccessFile) {
        // 使用 CAS 保证原子性，防止并发下载
        if (!isDownloading.compareAndSet(false, true)) {
            Log.w(TAG, "Download task is already running.");
            return;
        }

        executor.execute(() -> {
            try {
                boolean result = downloadFileIfNotExist(url, downloadTempFile);
                Log.i(TAG, "download result: " + result);

                if (!result) return;

                // 文件 MD5 再次校验
                String fileMD5 = getFileMD5(downloadTempFile);
                if (md5 != null && !md5.equalsIgnoreCase(fileMD5)) {
                    Log.e(TAG, "MD5 mismatch! Expected: " + md5 + ", Got: " + fileMD5);
                    deleteFileSafely(downloadTempFile);
                    return;
                }

                Log.i(TAG, "download success, copy to " + destSuccessFile);
                copyFile(downloadTempFile, destSuccessFile);

                // 下载拷贝成功后尝试加载
                loadSo(destSuccessFile);

                // 清理下载临时目录的历史文件
                File parentFile = downloadTempFile.getParentFile();
                if (parentFile != null) {
                    deleteHistoryFile(parentFile, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download process error", e);
            } finally {
                // 【关键修复】无论成功失败，必须重置标志位，否则后续永远无法下载
                isDownloading.set(false);
            }
        });
    }

    private boolean need() {
        return !(isGMS || includeCronetSo || includeCronetApkSo);
    }

    private JSONObject initJson(Context context) {
        // 使用 try-with-resources 防止流泄露
        try (InputStream is = context.getAssets().open("cronet.json");
             BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return new JSONObject(stringBuilder.toString());
        } catch (Exception e) {
            Log.e(TAG, "initJson failed", e);
            return null;
        }
    }

    public static boolean isHW() {
        String manufacturer = Build.MANUFACTURER;
        return "huawei".equalsIgnoreCase(manufacturer);
    }

    public boolean isJavaImplement() {
        ins = getInstallType();
        return ins.equals(CronetState.Java);
    }

    public boolean checkCronetNative() {
        ensureInitialized();
        if (includeCronetSo || includeCronetApkSo) {
            return true;
        }
        if (md5 == null || md5.length() != 32 || !soFile.exists()) {
            Log.w(TAG, "so not found or md5 invalid");
            return false;
        }
        String soMd5 = getFileMD5(soFile);
        return md5.equalsIgnoreCase(soMd5);
    }

    public CronetState getInstallType() {
        if (!ins.equals(CronetState.Java)) {
            return ins;
        }
        ensureInitialized();

        if (includeCronetSo || includeCronetApkSo) {
            ins = CronetState.Native;
        } else if (isGMS) {
            ins = CronetProviderInstaller.isInstalled() ? CronetState.GMS : CronetState.Java;
        } else {
            ins = checkCronetNative() ? CronetState.Native : CronetState.Java;
        }
        return ins;
    }

    public boolean checkApk() {
        try (ZipFile zf = new ZipFile(mContext.getPackageResourcePath())) {
            Enumeration<? extends ZipEntry> zes = zf.entries();
            while (zes.hasMoreElements()) {
                ZipEntry ze = zes.nextElement();
                if (ze.getName().contains("libcronet")) {
                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "checkApk failed", e);
        }
        return false;
    }

    public void preDownload() {
        ensureInitialized();
        if (prefSo) {
            downloadSo();
        } else if (isGMS) {
            CronetProviderInstaller.installProvider(mContext);
        } else {
            if (isHMS) {
                HQUICManager.asyncInit(mContext, new HQUICManager.HQUICInitCallback() {
                    @Override
                    public void onSuccess() {
                        ins = CronetState.HMS;
                        Log.i(TAG, "HQUIC安装成功");
                    }

                    @Override
                    public void onFail(Exception e) {
                        Log.e(TAG, "HQUIC安装失败，降级下载So", e);
                        downloadSo();
                    }
                });
            } else {
                downloadSo();
            }
        }
    }

    public void preLoadSo() {
        ensureInitialized();
        deleteHistoryFile(parentDir, soFile);

        if (md5 == null || md5.length() != 32 || TextUtils.isEmpty(soUrl)) {
            Log.w(TAG, "Invalid md5 or url, fallback to Java");
            return;
        }

        if (soFile.exists() && soFile.isFile()) {
            String fileMD5 = getFileMD5(soFile);
            if (md5.equalsIgnoreCase(fileMD5)) {
                loadSo(soFile);
                return;
            } else {
                deleteFileSafely(soFile);
            }
        }
        download(soUrl, md5, downloadFile, soFile);
    }

    private void downloadSo() {
        executor.execute(() -> {
            ensureInitialized();
            if (soFile.exists() && Objects.equals(md5, getFileMD5(soFile))) {
                Log.i(TAG, "So 库已存在");
                loadSo(soFile);
            } else {
                download(soUrl, md5, downloadFile, soFile);
            }
        });
    }

    /**
     * 统一的 SO 加载逻辑，精准捕获异常防止状态错乱
     */
    private void loadSo(File file) {
        if (file == null || !file.exists()) return;
        try {
            bypassCronetLibraryLoading();
            System.load(file.getAbsolutePath());
            ins = CronetState.Native;
            Log.i(TAG, "Successfully loaded so from: " + file.getAbsolutePath());
        } catch (UnsatisfiedLinkError | SecurityException e) {
            Log.e(TAG, "Failed to load cronet so, file might be corrupted.", e);
            deleteFileSafely(file); // 损坏的文件直接删除
            ins = CronetState.Java; // 回退到 Java 状态
        }
    }

    public static void bypassCronetLibraryLoading() {
        try {

            // 反射获取 sLibAlreadyLoaded 字段
            @SuppressLint("VisibleForTests")
            Field sLibAlreadyLoadedField = org.chromium.net.impl.CronetLibraryLoader.class.getDeclaredField("sLibAlreadyLoaded");
            sLibAlreadyLoadedField.setAccessible(true);

            // 将其强制设置为 true，欺骗 Cronet 库已加载
            sLibAlreadyLoadedField.set(null, true);

            System.out.println("成功通过反射绕过 CronetLibraryLoader 的默认加载逻辑");

            /*
             * ⚠️ 注意：在执行此代码前，请确保你已经手动加载了 SO 库，例如：
             * ReLinker.loadLibrary(context, "cronet");
             * 或 System.load("/data/data/your.package/app_so/libcronet.so");
             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum CronetState {
        GMS, HMS, Native, Java
    }

    @SuppressLint({"DiscouragedPrivateApi", "ObsoleteSdkInt"})
    private String getCpuAbi(Context context) {
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            @SuppressLint("PrivateApi")
            Field abiField = ApplicationInfo.class.getDeclaredField("primaryCpuAbi");
            abiField.setAccessible(true);
            Object object = abiField.get(appInfo);
            if (object != null) {
                return (String) object;
            }
        } catch (Exception e) {
            Log.w(TAG, "getCpuAbi reflection failed", e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    private String getMd5(String abi) {
        // 修复 NPE：防止 json 解析失败时报错
        return json != null ? json.optString(abi, "") : "";
    }

    private void deleteFileSafely(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private void deleteHistoryFile(File dir, File currentFile) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.exists() && (currentFile == null || !f.getAbsolutePath().equals(currentFile.getAbsolutePath()))) {
                    deleteFileSafely(f);
                }
            }
        }
    }

    private boolean downloadFileIfNotExist(String url, File destFile) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // try-with-resources 自动关流
            try (InputStream inputStream = connection.getInputStream();

                 OutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "Download file failed", e);
            deleteFileSafely(destFile);
        } finally {
            if (connection != null) {
                connection.disconnect(); // 防止连接池泄露
            }
        }
        return false;
    }

    private static final class CronetPreloaderHolder {
        @SuppressLint("StaticFieldLeak")
        private static final CronetPreloader instance = new CronetPreloader();
    }

    private boolean copyFile(File source, File dest) {
        if (source == null || !source.exists() || !source.isFile() || dest == null) {
            return false;
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return true;
        }

        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(dest, false)) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Copy file failed", e);
        }
        return false;
    }

    private String getFileMD5(File file) {
        if (file == null || !file.exists()) return "";
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192]; // 增大缓冲区提升读取速度
            int numRead;
            while ((numRead = fis.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            return String.format("%032x", new BigInteger(1, md5.digest())).toLowerCase();
        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "getFileMD5 failed", e);
            return "";
        }
    }
}