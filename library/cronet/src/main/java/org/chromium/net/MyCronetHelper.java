package org.chromium.net;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.chromium.net.impl.JavaCronetProvider;
import org.chromium.net.impl.NativeCronetProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.ag2s.cronet.CronetLoader;

public class MyCronetHelper {

    private static final String TAG = MyCronetHelper.class.getSimpleName();

    public static ICronetEngineBuilder createBuilderDelegate(Context context) {



        List<CronetProvider> providers = new ArrayList<>(CronetProvider.getAllProviders(context));

        CronetLoader cronetLoader = CronetLoader.getInstance();

        CronetProvider provider = getEnabledCronetProviders(context, providers, cronetLoader).get(0);

        Log.e(TAG, String.format("Using '%s' provider for creating CronetEngine.Builder.", provider));
        ICronetEngineBuilder iCronetEngineBuilder = provider.createBuilder().mBuilderDelegate;
        if (provider.getClass() == NativeCronetProvider.class && cronetLoader.checkCronetNative()) {
            iCronetEngineBuilder.setLibraryLoader(cronetLoader);
        }
        return iCronetEngineBuilder;
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
            Context context, List<CronetProvider> providers,CronetLoader cronetLoader) {
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
