package me.ag2s.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.net.CronetProviderInstaller;
import com.huawei.hms.hquic.HQUICManager;

import org.chromium.net.ApiVersion;
import org.chromium.net.CronetEngine;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cronet的so加载工具类
 * CronetClient soLoader
 */
public class CronetLoader extends CronetEngine.Builder.LibraryLoader {
    /**
     * Cronet下载链接示例
     * https://storage.googleapis.com/chromium-cronet/android/92.0.4515.127/Release/cronet/libs/arm64-v8a/libcronet.92.0.4515.127.so
     */
   public enum CronetState{
        GMS,HMS,Native,Java
    }

    private final Context mContext;

    private final String soName = "libcronet." + ImplVersion.getCronetVersion() + ".so";
    private final String soUrl;
    private final File soFile;
    private final File downloadFile;
    private final File parentDir;
    private String CPU_ABI;
    private final String md5;
    private final JSONObject json;
    /**
     * 手机是否安装GSM
     */

    public final boolean isGMS;
    /**
     * 手机是否安装HMS
     */
    public final boolean isHMS;
    /**
     * 打包时是否包含Cronet so
     *Whether the aar package include cronet so when package,
     */
    public final boolean includeCronetSo=BuildConfig.includeCronetSo;
    /**
     * 缓存是否安装成功的结果
     */
    private CronetState ins = CronetState.Java;

    /**
     * 优先下载so
     */
    private boolean prefSo=true;

    //private final SharedPreferences preferences;
    private static final String TAG = "CronetLoader";

    private static final class CronetLoaderHolder {
        @SuppressLint("StaticFieldLeak")
        private static final CronetLoader instance = new CronetLoader();
    }

    public static CronetLoader getInstance() {
        return CronetLoaderHolder.instance;
    }


    CronetLoader() {
        mContext = CronetInitializer.getCtx();
        soUrl = "https://storage.googleapis.com/chromium-cronet/android/"
                + ApiVersion.getCronetVersion() + "/Release/cronet/libs/"
                + getCpuAbi(mContext) + "/" + soName;

        parentDir = mContext.getDir("cronet", Context.MODE_PRIVATE);
        soFile = new File(parentDir, soName);

        isGMS = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS;
        isHMS = isHW();


        downloadFile = new File(mContext.getCacheDir() + "/so_download", soName);

        json = initJson(mContext);
        md5 = getMd5(CPU_ABI);
        Log.e(TAG, "isGMS:" + isGMS);
        Log.e(TAG, "isHMS:" + isHMS);
        Log.e(TAG, "md5:" + json);
        Log.e(TAG, "soName+:" + soName);
        Log.e(TAG, "destSuccessFile:" + soFile);
        Log.e(TAG, "tempFile:" + downloadFile);
        Log.e(TAG, "soUrl:" + soUrl);
        Log.e(TAG, "md5:" + md5);


    }

    public JSONObject initJson(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open("cronet.json")));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return new JSONObject(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    public static boolean isHW() {
        String manufacturer = Build.MANUFACTURER;
        return "huawei".equalsIgnoreCase(manufacturer);
    }

    /**
     * 判断是否需要下载Cronet
     * Judge whether cronet needs to be downloaded
     * @return
     */

    public boolean need() {
        return !(isGMS||includeCronetSo);
    }

    /**
     * 判断Cronet是否正确安装
     *
     * @return true or false
     */
    public boolean install() {
        ins=getInstallType();
        return !ins.equals(CronetState.Java);

    }

    /**
     * 检测Cronet So 是否存在
     * @return
     */
    public boolean checkCronetNative(){
        if (md5 == null || md5.length() != 32 || !soFile.exists()) {
            //ins = CronetState.Java;
            return false;
        }
        return md5.equals(getFileMD5(soFile));
    }

    public CronetState getInstallType(){
        if (!ins.equals(CronetState.Java)) {
            return ins;
        }
        if (isGMS) {
            ins = CronetProviderInstaller.isInstalled()?CronetState.GMS:CronetState.Java;
            return ins;
        }
        else {
            ins = checkCronetNative()?CronetState.Native:CronetState.Java;
            return ins;
        }

    }


    /**
     * 预加载Cronet
     */

    public void preDownload() {
        //安装GMS的预加载

        if (isGMS) {
            CronetProviderInstaller.installProvider(mContext);
        } else{

            if (prefSo){
                downloadSo();
            }

            HQUICManager.asyncInit(mContext, new HQUICManager.HQUICInitCallback() {
                @Override
                public void onSuccess() {
                    ins=CronetState.HMS;
                    Log.e(TAG, "HQUIC安装陈功");
                }

                @Override
                public void onFail(Exception e) {
                    Log.e(TAG, "HQUIC安装失败",e);
                    downloadSo();

                }
            });


        }


    }

    private void downloadSo(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (soFile.exists() && Objects.equals(md5, getFileMD5(soFile))) {
                    Log.e(TAG, "So 库已存在");
                    ins=CronetState.Native;
                } else {
                    download(soUrl, md5, downloadFile, soFile);
                }

                Log.e(TAG, soName);
            }
        });
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @Override
    public void loadLibrary(String libName) {
        Log.e(TAG, "libName:" + libName);
        long start = System.currentTimeMillis();
        try {
            //非cronet的so调用系统方法加载
            if (!libName.contains("cronet")) {
                System.loadLibrary(libName);
                return;
            }
            //以下逻辑为cronet加载，优先加载本地，否则从远程加载
            //首先调用系统行为进行加载
            System.loadLibrary(libName);
            Log.e(TAG, "load from system");

        } catch (Throwable e) {
            //如果找不到，则从远程下载


            //删除历史文件
            deleteHistoryFile(parentDir, soFile);
            //md5 = getUrlMd5(md5Url);
            Log.i(TAG, "soMD5:" + md5);


            if (md5 == null || md5.length() != 32 || soUrl.length() == 0) {
                //如果md5或下载的url为空，则调用系统行为进行加载
                System.loadLibrary(libName);
                return;
            }


            if (!soFile.exists() || !soFile.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                soFile.delete();
                download(soUrl, md5, downloadFile, soFile);
                //如果文件不存在或不是文件，则调用系统行为进行加载
                System.loadLibrary(libName);
                return;
            }

            if (soFile.exists()) {
                //如果文件存在，则校验md5值
                String fileMD5 = getFileMD5(soFile);
                Log.e(TAG, "File:md5:" + fileMD5);
                if (fileMD5 != null && fileMD5.equalsIgnoreCase(md5)) {
                    //md5值一样，则加载
                    System.load(soFile.getAbsolutePath());
                    Log.e(TAG, "load from:" + soFile);
                    return;
                }
                //md5不一样则删除
                //noinspection ResultOfMethodCallIgnored
                soFile.delete();

            }
            //不存在则下载
            download(soUrl, md5, downloadFile, soFile);
            //使用系统加载方法
            System.loadLibrary(libName);
        } finally {
            Log.e(TAG, "time:" + (System.currentTimeMillis() - start));
        }
    }


    @SuppressLint({"DiscouragedPrivateApi", "ObsoleteSdkInt"})
    private String getCpuAbi(Context context) {
        if (CPU_ABI != null) {
            return CPU_ABI;
        }
        // 5.0以上Application才有primaryCpuAbi字段
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            Field abiField = ApplicationInfo.class.getDeclaredField("primaryCpuAbi");
            abiField.setAccessible(true);
            CPU_ABI = (String) abiField.get(appInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(CPU_ABI)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CPU_ABI = Build.SUPPORTED_ABIS[0];
            } else {
                CPU_ABI = Build.CPU_ABI;
            }
        }

        //貌似只有这个过时了的API能获取当前APP使用的ABI
        return CPU_ABI;
    }


    private String getMd5(String abi) {
        return json.optString(abi, "");
    }


    /**
     * 删除历史文件
     */
    private static void deleteHistoryFile(File dir, File currentFile) {
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                if (f.exists() && (currentFile == null || !f.getAbsolutePath().equals(currentFile.getAbsolutePath()))) {
                    boolean delete = f.delete();
                    Log.e(TAG, "delete file: " + f + " result: " + delete);
                    if (!delete) {
                        f.deleteOnExit();
                    }
                }
            }
        }
    }

    /**
     * 下载文件
     */
    private static boolean downloadFileIfNotExist(String url, File destFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            inputStream = connection.getInputStream();
            if (destFile.exists()) {
                return true;
            }
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[32768];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            if (destFile.exists() && !destFile.delete()) {
                destFile.deleteOnExit();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    static boolean download = false;
    static ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 下载并拷贝文件
     */
    private static synchronized void download(final String url, final String md5, final File downloadTempFile, final File destSuccessFile) {
        if (download) {
            return;
        }
        download = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = downloadFileIfNotExist(url, downloadTempFile);
                Log.e(TAG, "download result:" + result);
                //文件md5再次校验
                String fileMD5 = getFileMD5(downloadTempFile);
                if (md5 != null && !md5.equalsIgnoreCase(fileMD5)) {
                    boolean delete = downloadTempFile.delete();
                    if (!delete) {
                        downloadTempFile.deleteOnExit();
                    }
                    download = false;
                    return;
                }
                Log.e(TAG, "download success, copy to " + destSuccessFile);
                //下载成功拷贝文件
                copyFile(downloadTempFile, destSuccessFile);
                //文件变动后重新计算MD5
                CronetLoaderHolder.instance.ins=CronetState.Native;
                File parentFile = downloadTempFile.getParentFile();
                deleteHistoryFile(parentFile, null);
            }
        });

    }


    /**
     * 拷贝文件
     */
    private static boolean copyFile(File source, File dest) {
        if (source == null || !source.exists() || !source.isFile() || dest == null) {
            return false;
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return true;
        }
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            boolean mkdirs = parent.mkdirs();
            if (!mkdirs) {
                mkdirs = parent.mkdirs();
            }
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[1024 * 512];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 获得文件md5
     */
    private static String getFileMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fileInputStream.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            return String.format("%032x", new BigInteger(1, md5.digest())).toLowerCase();
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
