package org.chromium.net;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.chromium.net.impl.CronetLogger;
import org.chromium.net.impl.CronetLoggerFactory;
import org.chromium.net.impl.JavaCronetProvider;
import org.chromium.net.impl.NativeCronetProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.ag2s.cronet.CronetPreloader;

public class MyCronetHelper {

    private static final String TAG = MyCronetHelper.class.getSimpleName();

    public static ICronetEngineBuilder createBuilderDelegate(Context context) {

        var startUptimeMillis = SystemClock.uptimeMillis();
        CronetProvider.ProviderInfo providerInfo =
                getEnabledCronetProviders(
                        context,
                        new ArrayList<>(CronetProvider.getAllProviderInfos(context)))
                        .get(0);
        CronetPreloader cronetLoader = CronetPreloader.getInstance();



        var logger = CronetLoggerFactory.createLogger(context, providerInfo.logSource);
        var logInfo = new CronetLogger.CronetEngineBuilderInitializedInfo();
        try {
            logInfo.creationSuccessful = false;
            logInfo.author = CronetLogger.CronetEngineBuilderInitializedInfo.Author.API;
            logInfo.source = providerInfo.logSource;
            logInfo.uid = Process.myUid();
            logInfo.apiVersion = new CronetLogger.CronetVersion(ApiVersion.getCronetVersion());
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                        TAG,
                        String.format(
                                "Using '%s' provider for creating CronetEngine.Builder.",
                                providerInfo.provider));
            }
            var builderDelegate = providerInfo.provider.createBuilder().mBuilderDelegate;

            if (providerInfo.provider.getClass() == NativeCronetProvider.class && cronetLoader.checkCronetNative()) {
                builderDelegate.setLibraryLoader(cronetLoader.getLibraryLoader());
            }
            var implCronetVersion = getImplCronetVersion(builderDelegate);
            if (implCronetVersion != null) {
                logInfo.implVersion = new CronetLogger.CronetVersion(implCronetVersion);
            }
            logInfo.cronetInitializationRef = builderDelegate.getLogCronetInitializationRef();
            logInfo.creationSuccessful = true;
            return builderDelegate;
        } finally {
            logInfo.engineBuilderCreatedLatencyMillis =
                    (int) (SystemClock.uptimeMillis() - startUptimeMillis);
            logger.logCronetEngineBuilderInitializedInfo(logInfo);
        }

//        List<CronetProvider> providers = new ArrayList<>(CronetProvider.getAllProviders(context));
//
//        CronetPreloader cronetLoader = CronetPreloader.getInstance();
//
//        CronetProvider provider = getEnabledCronetProviders(context, providers, cronetLoader).get(0);
//
//        Log.e(TAG, String.format("Using '%s' provider for creating CronetEngine.Builder.", provider));
//        ICronetEngineBuilder iCronetEngineBuilder = provider.createBuilder().mBuilderDelegate;
//        if (provider.getClass() == NativeCronetProvider.class && cronetLoader.checkCronetNative()) {
//            iCronetEngineBuilder.setLibraryLoader(cronetLoader.getLibraryLoader());
//        }
//        return iCronetEngineBuilder;
    }


    private static String getImplCronetVersion(ICronetEngineBuilder builderDelegate) {
        try {
            var implVersionClass = getImplVersionClass(builderDelegate);
            if (implVersionClass == null) return null;
            return (String) implVersionClass.getMethod("getCronetVersion").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to retrieve Cronet impl version", exception);
        }
    }

    /**
     * Returns the ImplVersion class from the impl.
     *
     * <p>NOTE: this functionality is not available if the impl was built before
     * https://crrev.com/c/5190726, in which case this function will return null.
     *
     * @see org.chromium.net.impl.ImplVersion
     */
    private static Class<?> getImplVersionClass(ICronetEngineBuilder builderDelegate) {
        try {
            return builderDelegate
                    .getClass()
                    .getClassLoader()
                    .loadClass("org.chromium.net.impl.ImplVersion");
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    @VisibleForTesting
    static List<CronetProvider.ProviderInfo> getEnabledCronetProviders(
            Context context, List<CronetProvider.ProviderInfo> providers) {

        CronetPreloader cronetLoader = CronetPreloader.getInstance();
        // Check that there is at least one available provider.
        if (providers.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find any Cronet provider."
                            + " Have you included all necessary jars?");
        }

        // Exclude disabled providers from the list.
        for (Iterator<CronetProvider.ProviderInfo> i = providers.iterator(); i.hasNext(); ) {
            CronetProvider.ProviderInfo providerInfo = i.next();
            if (!providerInfo.provider.isEnabled()|| (providerInfo.provider.getClass() == NativeCronetProvider.class && !cronetLoader.checkCronetNative())) {
                i.remove();
                Log.e(TAG,"remove:"+providerInfo.provider.getName());
            }
        }

        // Check that there is at least one enabled provider.
        if (providers.isEmpty()) {
            throw new RuntimeException(
                    "All available Cronet providers are disabled."
                            + " A provider should be enabled before it can be used.");
        }

        // Sort providers based on version and type.
        Collections.sort(
                providers,
                new Comparator<CronetProvider.ProviderInfo>() {
                    @Override
                    public int compare(
                            CronetProvider.ProviderInfo p1, CronetProvider.ProviderInfo p2) {

                        if(CronetPreloader.getInstance().prefSo){
                            if (CronetProvider.PROVIDER_NAME_APP_PACKAGED.equals(
                                    p1.provider.getName())) {
                                return -1;
                            }
                            if (CronetProvider.PROVIDER_NAME_APP_PACKAGED.equals(
                                    p2.provider.getName())) {
                                return 1;
                            }
                        }


                        // The fallback provider should always be at the end of the list.
                        if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(
                                p1.provider.getName())) {
                            return 1;
                        }

                        if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(
                                p2.provider.getName())) {
                            return -1;
                        }
                        // A provider with higher version should go first.
                        return -compareVersions(
                                p1.provider.getVersion(), p2.provider.getVersion());
                    }
                });
        return providers;
    }

    /**
     * Returns the list of available and enabled {@link CronetProvider}. The returned list is
     * sorted based on the provider versions and types.
     *
     * @param context   Android Context to use.
     * @param providers the list of enabled and disabled providers to filter out and sort.
     * @return the sorted list of enabled providers. The list contains at least one provider.
     * @throws RuntimeException is the list of providers is empty or all of the providers are
     *                          disabled.
     */
    @VisibleForTesting
    private static List<CronetProvider> getEnabledCronetProviders(
            Context context, List<CronetProvider> providers,CronetPreloader cronetLoader) {
        // Check that there is at least one available provider.
        if (providers.isEmpty()) {
            throw new RuntimeException("Unable to find any Cronet provider."
                    + " Have you included all necessary jars?");
        }

        // Exclude disabled providers from the list.
        for (Iterator<CronetProvider> i = providers.iterator(); i.hasNext(); ) {
            CronetProvider provider = i.next();
            Log.e(TAG,provider.toString());
            if (!provider.isEnabled() || (provider.getClass() == NativeCronetProvider.class && !cronetLoader.checkCronetNative())) {
                Log.e(TAG,"removed:"+provider.toString());
                i.remove();
            }
        }
        if (providers.isEmpty()) {
            providers.add(new JavaCronetProvider(context));
        }

        // Check that there is at least one enabled provider.
        if (providers.isEmpty()) {
            throw new RuntimeException("All available Cronet providers are disabled."
                    + " A provider should be enabled before it can be used.");
        }

        // Sort providers based on version and type.
        Collections.sort(providers, new Comparator<CronetProvider>() {
            @Override
            public int compare(CronetProvider p1, CronetProvider p2) {
                // The fallback provider should always be at the end of the list.
                if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(p1.getName())) {
                    return 1;
                }
                if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(p2.getName())) {
                    return -1;
                }
                // A provider with higher version should go first.
                return -compareVersions(p1.getVersion(), p2.getVersion());
            }
        });
        Log.e(TAG,providers.toString());

        return providers;
    }

    @VisibleForTesting
    private static int compareVersions(String s1, String s2) {
        if (s1 == null || s2 == null) {
            throw new IllegalArgumentException("The input values cannot be null");
        }
        String[] s1segments = s1.split("\\.");
        String[] s2segments = s2.split("\\.");
        for (int i = 0; i < s1segments.length && i < s2segments.length; i++) {
            try {
                int s1segment = Integer.parseInt(s1segments[i]);
                int s2segment = Integer.parseInt(s2segments[i]);
                if (s1segment != s2segment) {
                    return Integer.signum(s1segment - s2segment);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to convert version segments into"
                        + " integers: " + s1segments[i] + " & " + s2segments[i],
                        e);
            }
        }
        return Integer.signum(s1segments.length - s2segments.length);
    }


}
