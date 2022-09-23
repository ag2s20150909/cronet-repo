package org.chromium.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;


import org.chromium.net.impl.NativeCronetProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import me.ag2s.cronet.CronetLoader;

public abstract class MyCronetEngine extends ExperimentalCronetEngine {
    private static final String TAG="MyCronetEngine";


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
//
//        private static ICronetEngineBuilder createBuilderDelegate(Context context) {
//
//            CronetLoader cronetLoader = CronetLoader.getInstance();
//            boolean install = cronetLoader.install();
//            CronetProvider cronetProvider = install ? new NativeCronetProvider(context) : new JavaCronetProvider(context);
//            ICronetEngineBuilder iCronetEngineBuilder = cronetProvider.createBuilder().mBuilderDelegate;
//            if (cronetLoader.need()) {
//                iCronetEngineBuilder.setLibraryLoader(cronetLoader);
//            }
//
//            return iCronetEngineBuilder;
//        }

        private static ICronetEngineBuilder createBuilderDelegate(Context context) {
            CronetLoader cronetLoader = CronetLoader.getInstance();
            List<CronetProvider> providers = new ArrayList(MyCronetProvider.getAllProviders(context));
            CronetProvider provider = getEnabledCronetProviders(context,cronetLoader, providers).get(0);
            Log.e(TAG,providers.toString());
            Log.e(TAG,String.format("Using '%s' provider for creating CronetEngine.Builder.", provider));
//            if (Log.isLoggable(TAG, 3)) {
//                Log.d(CronetEngine.TAG, String.format("Using '%s' provider for creating CronetEngine.Builder.", provider));
//            }
            ICronetEngineBuilder iCronetEngineBuilder = provider.createBuilder().mBuilderDelegate;
            if(provider.getClass()== NativeCronetProvider.class && cronetLoader.install()){
                iCronetEngineBuilder.setLibraryLoader(cronetLoader);
            }

            return iCronetEngineBuilder;
        }

        @VisibleForTesting
        static List<CronetProvider> getEnabledCronetProviders(Context context,CronetLoader cronetLoader, List<CronetProvider> providers) {
            if (providers.size() == 0) {
                throw new RuntimeException("Unable to find any CronetClient provider. Have you included all necessary jars?");
            } else {
                Iterator<CronetProvider> i = providers.iterator();

                while(i.hasNext()) {
                    CronetProvider provider = (CronetProvider)i.next();
                    if (!provider.isEnabled()) {
                        i.remove();
                        Log.e(TAG,"无效："+provider.getName());
                    }
                    if(provider.getClass()==NativeCronetProvider.class&&!cronetLoader.checkCronetNative()){
                        i.remove();
                        Log.e(TAG,"无效："+provider.getName());
                    }
                }

                if (providers.size() == 0) {
                    throw new RuntimeException("All available CronetClient providers are disabled. A provider should be enabled before it can be used.");
                } else {
//                    Collections.sort(providers, new Comparator<CronetProvider>() {
//                        public int compare(CronetProvider p1, CronetProvider p2) {
//                            Log.e("Cronet",p1.getName());
//                            Log.e("Cronet",p2.getName());
//                            if ("Fallback-CronetClient-Provider".equals(p1.getName())&&"HQUICProvider".equals(p2.getName())){
//                                return -1;
//                            }
//                            else if ("Fallback-CronetClient-Provider".equals(p1.getName())) {
//                                return 1;
//                            } else {
//                                return "Fallback-CronetClient-Provider".equals(p2.getName()) ? -1 : -CronetEngine.Builder.compareVersions(p1.getVersion(), p2.getVersion());
//                            }
//                        }
//                    });
                    return providers;
                }
            }
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
         * Sets experimental options to be used in CronetClient.
         *
         * @param options JSON formatted experimental options.
         * @return the builder to facilitate chaining.
         */
        public Builder setExperimentalOptions(String options) {
            mBuilderDelegate.setExperimentalOptions(options);
            return this;
        }

        /**
         * Sets the thread priority of CronetClient's internal thread.
         *
         * @param priority the thread priority of CronetClient's internal thread.
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
        //@VisibleForTesting
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
