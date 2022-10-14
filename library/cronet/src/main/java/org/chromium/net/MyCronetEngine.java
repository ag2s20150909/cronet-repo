package org.chromium.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.chromium.net.impl.NativeCronetProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import me.ag2s.cronet.CronetLoader;

/**
 * {@link CronetEngine} that exposes experimental features. To obtain an
 * instance of this class, cast a {@code CronetEngine} to this type. Every
 * instance of {@code CronetEngine} can be cast to an instance of this class,
 * as they are backed by the same implementation and hence perform identically.
 * Instances of this class are not meant for general use, but instead only
 * to access experimental features. Experimental features may be deprecated in the
 * future. Use at your own risk.
 * <p>
 * {@hide since this class exposes experimental features that should be hidden.}
 */
public abstract class MyCronetEngine extends ExperimentalCronetEngine {
    private static final String TAG = "MyCronetEngine";


    /**
     * A version of {@link CronetEngine.Builder} that exposes experimental
     * features. Instances of this class are not meant for general use, but
     * instead only to access experimental features. Experimental features
     * may be deprecated in the future. Use at your own risk.
     */
    public static class Builder extends ExperimentalCronetEngine.Builder {
        /**
         * Constructs a {@link Builder} object that facilitates creating a
         * {@link CronetEngine}. The default configuration enables HTTP/2 and
         * disables QUIC, SDCH and the HTTP cache.
         *
         * @param context Android {@link Context}, which is used by
         *                {@link Builder} to retrieve the application
         *                context. A reference to only the application
         *                context will be kept, so as to avoid extending
         *                the lifetime of {@code context} unnecessarily.
         */
        public Builder(Context context) {
            super(createBuilderDelegate(context));
        }

        /**
         * Constructs {@link Builder} with a given delegate that provides the actual implementation
         * of the {@code Builder} methods. This constructor is used only by the internal
         * implementation.
         *
         * @param builderDelegate delegate that provides the actual implementation.
         *                        <p>
         *                        {@hide}
         */
        public Builder(ICronetEngineBuilder builderDelegate) {
            super(builderDelegate);
        }


        /**
         * Creates an implementation of {@link ICronetEngineBuilder} that can be used
         * to delegate the builder calls to. The method uses {@link CronetProvider}
         * to obtain the list of available providers.
         *
         * @param context Android Context to use.
         * @return the created {@code ICronetEngineBuilder}.
         */
        private static ICronetEngineBuilder createBuilderDelegate(Context context) {
            CronetLoader cronetLoader = CronetLoader.getInstance();
            List<CronetProvider> providers =
                    new ArrayList<>(MyCronetProvider.getAllProviders(context));
            CronetProvider provider = getEnabledCronetProviders(context, cronetLoader, providers).get(0);

            Log.e(TAG, String.format("Using '%s' provider for creating CronetEngine.Builder.", provider));
            ICronetEngineBuilder iCronetEngineBuilder = provider.createBuilder().mBuilderDelegate;
            if (provider.getClass() == NativeCronetProvider.class && cronetLoader.checkCronetNative()) {
                iCronetEngineBuilder.setLibraryLoader(cronetLoader);
            }
            return iCronetEngineBuilder;
        }

        /**
         * Returns the list of available and enabled {@link CronetProvider}. The returned list
         * is sorted based on the provider versions and types.
         *
         * @param context   Android Context to use.
         * @param providers the list of enabled and disabled providers to filter out and sort.
         * @return the sorted list of enabled providers. The list contains at least one provider.
         * @throws RuntimeException is the list of providers is empty or all of the providers
         *                          are disabled.
         */
        @VisibleForTesting
        static List<CronetProvider> getEnabledCronetProviders(
                Context context, CronetLoader loader, List<CronetProvider> providers) {
            // Check that there is at least one available provider.
            if (providers.size() == 0) {
                throw new RuntimeException("Unable to find any Cronet provider."
                        + " Have you included all necessary jars?");
            }

            // Exclude disabled providers from the list.
            for (Iterator<CronetProvider> i = providers.iterator(); i.hasNext(); ) {
                CronetProvider provider = i.next();
                if (!provider.isEnabled()) {
                    i.remove();
                } else if (provider.getClass() == NativeCronetProvider.class && !loader.checkCronetNative()) {
                    i.remove();
                }
            }

            // Check that there is at least one enabled provider.
            if (providers.size() == 0) {
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
            return providers;
        }

        /**
         * Compares two strings that contain versions. The string should only contain
         * dot-separated segments that contain an arbitrary number of digits digits [0-9].
         *
         * @param s1 the first string.
         * @param s2 the second string.
         * @return -1 if s1<s2, +1 if s1>s2 and 0 if s1=s2. If two versions are equal, the
         * version with the higher number of segments is considered to be higher.
         * @throws IllegalArgumentException if any of the strings contains an illegal
         *                                  version number.
         */
        @VisibleForTesting
        static int compareVersions(String s1, String s2) {
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


        /**
         * Enables the network quality estimator, which collects and reports
         * measurements of round trip time (RTT) and downstream throughput at
         * various layers of the network stack. After enabling the estimator,
         * listeners of RTT and throughput can be added with
         * {@link #addRttListener} and {@link #addThroughputListener} and
         * removed with {@link #removeRttListener} and
         * {@link #removeThroughputListener}. The estimator uses memory and CPU
         * only when enabled.
         *
         * @param value {@code true} to enable network quality estimator,
         *              {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        public Builder enableNetworkQualityEstimator(boolean value) {
            mBuilderDelegate.enableNetworkQualityEstimator(value);
            return this;
        }

        /**
         * Sets experimental options to be used in Cronet.
         *
         * @param options JSON formatted experimental options.
         * @return the builder to facilitate chaining.
         */
        public Builder setExperimentalOptions(String options) {
            mBuilderDelegate.setExperimentalOptions(options);
            return this;
        }

        /**
         * Sets the thread priority of Cronet's internal thread.
         *
         * @param priority the thread priority of Cronet's internal thread.
         *                 A Linux priority level, from -20 for highest scheduling
         *                 priority to 19 for lowest scheduling priority. For more
         *                 information on values, see
         *                 {@link android.os.Process#setThreadPriority(int, int)} and
         *                 {@link android.os.Process#THREAD_PRIORITY_DEFAULT
         *                 THREAD_PRIORITY_*} values.
         * @return the builder to facilitate chaining.
         */
        public Builder setThreadPriority(int priority) {
            mBuilderDelegate.setThreadPriority(priority);
            return this;
        }

        /**
         * Returns delegate, only for testing.
         *
         * @hide
         */
        @VisibleForTesting
        public ICronetEngineBuilder getBuilderDelegate() {
            return mBuilderDelegate;
        }

        // To support method chaining, override superclass methods to return an
        // instance of this class instead of the parent.

        @Override
        public Builder setUserAgent(String userAgent) {
            super.setUserAgent(userAgent);
            return this;
        }

        @Override
        public Builder setStoragePath(String value) {
            super.setStoragePath(value);
            return this;
        }

        @Override
        public Builder setLibraryLoader(LibraryLoader loader) {
            super.setLibraryLoader(loader);
            return this;
        }

        @Override
        public Builder enableQuic(boolean value) {
            super.enableQuic(value);
            return this;
        }

        @Override
        public Builder enableHttp2(boolean value) {
            super.enableHttp2(value);
            return this;
        }

        @Override
        public Builder enableSdch(boolean value) {
            return this;
        }

        @Override
        public Builder enableHttpCache(int cacheMode, long maxSize) {
            super.enableHttpCache(cacheMode, maxSize);
            return this;
        }

        @Override
        public Builder addQuicHint(String host, int port, int alternatePort) {
            super.addQuicHint(host, port, alternatePort);
            return this;
        }

        @Override
        public Builder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256,
                                        boolean includeSubdomains, Date expirationDate) {
            super.addPublicKeyPins(hostName, pinsSha256, includeSubdomains, expirationDate);
            return this;
        }

        @Override
        public Builder enablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
            super.enablePublicKeyPinningBypassForLocalTrustAnchors(value);
            return this;
        }

        @Override
        public ExperimentalCronetEngine build() {
            return mBuilderDelegate.build();
        }
    }


}
