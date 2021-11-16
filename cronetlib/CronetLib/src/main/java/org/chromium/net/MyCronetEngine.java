package org.chromium.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;


import org.chromium.net.impl.NativeCronetProvider;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import me.ag2s.cronetlib.CronetLoader;

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
                throw new RuntimeException("Unable to find any Cronet provider. Have you included all necessary jars?");
            } else {
                Iterator i = providers.iterator();

                while(i.hasNext()) {
                    CronetProvider provider = (CronetProvider)i.next();
                    if (!provider.isEnabled()) {
                        i.remove();
                        Log.e(TAG,"无效："+provider.getName());
                    }
                    if(provider.getClass()==NativeCronetProvider.class&&!cronetLoader.cheackCronetNative()){
                        i.remove();
                        Log.e(TAG,"无效："+provider.getName());
                    }
                }

                if (providers.size() == 0) {
                    throw new RuntimeException("All available Cronet providers are disabled. A provider should be enabled before it can be used.");
                } else {
                    Collections.sort(providers, new Comparator<CronetProvider>() {
                        public int compare(CronetProvider p1, CronetProvider p2) {
                            if ("Fallback-Cronet-Provider".equals(p1.getName())) {
                                return 1;
                            } else {
                                return "Fallback-Cronet-Provider".equals(p2.getName()) ? -1 : -CronetEngine.Builder.compareVersions(p1.getVersion(), p2.getVersion());
                            }
                        }
                    });
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

    /**
     * Creates a builder for {@link BidirectionalStream} objects. All callbacks for
     * generated {@code BidirectionalStream} objects will be invoked on
     * {@code executor}. {@code executor} must not run tasks on the
     * current thread, otherwise the networking operations may block and exceptions
     * may be thrown at shutdown time.
     *
     * @param url      URL for the generated streams.
     * @param callback the {@link BidirectionalStream.Callback} object that gets invoked upon
     *                 different events occurring.
     * @param executor the {@link Executor} on which {@code callback} methods will be invoked.
     * @return the created builder.
     */
    public abstract ExperimentalBidirectionalStream.Builder newBidirectionalStreamBuilder(
            String url, BidirectionalStream.Callback callback, Executor executor);

    @Override
    public abstract ExperimentalUrlRequest.Builder newUrlRequestBuilder(
            String url, UrlRequest.Callback callback, Executor executor);

    /**
     * Starts NetLog logging to a specified directory with a bounded size. The NetLog will contain
     * events emitted by all live CronetEngines. The NetLog is useful for debugging.
     * Once logging has stopped {@link #stopNetLog}, the data will be written
     * to netlog.json in {@code dirPath}. If logging is interrupted, you can
     * stitch the files found in .inprogress subdirectory manually using:
     * https://chromium.googlesource.com/chromium/src/+/main/net/tools/stitch_net_log_files.py.
     * The log can be viewed using a Chrome browser navigated to chrome://net-internals/#import.
     *
     * @param dirPath the directory where the netlog.json file will be created. dirPath must
     *                already exist. NetLog files must not exist in the directory. If actively
     *                logging, this method is ignored.
     * @param logAll  {@code true} to include basic events, user cookies,
     *                credentials and all transferred bytes in the log. This option presents a
     *                privacy risk, since it exposes the user's credentials, and should only be
     *                used with the user's consent and in situations where the log won't be public.
     *                {@code false} to just include basic events.
     * @param maxSize the maximum total disk space in bytes that should be used by NetLog. Actual
     *                disk space usage may exceed this limit slightly.
     */
    public void startNetLogToDisk(String dirPath, boolean logAll, int maxSize) {
    }

    /**
     * Returns an estimate of the effective connection type computed by the network quality
     * estimator. Call {@link Builder#enableNetworkQualityEstimator} to begin computing this
     * value.
     *
     * @return the estimated connection type. The returned value is one of
     * {@link #EFFECTIVE_CONNECTION_TYPE_UNKNOWN EFFECTIVE_CONNECTION_TYPE_* }.
     */
    public int getEffectiveConnectionType() {
        return EFFECTIVE_CONNECTION_TYPE_UNKNOWN;
    }

    /**
     * Configures the network quality estimator for testing. This must be called
     * before round trip time and throughput listeners are added, and after the
     * network quality estimator has been enabled.
     *
     * @param useLocalHostRequests include requests to localhost in estimates.
     * @param useSmallerResponses  include small responses in throughput estimates.
     * @param disableOfflineCheck  when set to true, disables the device offline checks when
     *                             computing the effective connection type or when writing the prefs.
     */
    public void configureNetworkQualityEstimatorForTesting(boolean useLocalHostRequests,
                                                           boolean useSmallerResponses, boolean disableOfflineCheck) {
    }

    /**
     * Registers a listener that gets called whenever the network quality
     * estimator witnesses a sample round trip time. This must be called
     * after {@link Builder#enableNetworkQualityEstimator}, and with throw an
     * exception otherwise. Round trip times may be recorded at various layers
     * of the network stack, including TCP, QUIC, and at the URL request layer.
     * The listener is called on the {@link java.util.concurrent.Executor} that
     * is passed to {@link Builder#enableNetworkQualityEstimator}.
     *
     * @param listener the listener of round trip times.
     */
    public void addRttListener(NetworkQualityRttListener listener) {
    }

    /**
     * Removes a listener of round trip times if previously registered with
     * {@link #addRttListener}. This should be called after a
     * {@link NetworkQualityRttListener} is added in order to stop receiving
     * observations.
     *
     * @param listener the listener of round trip times.
     */
    public void removeRttListener(NetworkQualityRttListener listener) {
    }

    /**
     * Registers a listener that gets called whenever the network quality
     * estimator witnesses a sample throughput measurement. This must be called
     * after {@link Builder#enableNetworkQualityEstimator}. Throughput observations
     * are computed by measuring bytes read over the active network interface
     * at times when at least one URL response is being received. The listener
     * is called on the {@link java.util.concurrent.Executor} that is passed to
     * {@link Builder#enableNetworkQualityEstimator}.
     *
     * @param listener the listener of throughput.
     */
    public void addThroughputListener(NetworkQualityThroughputListener listener) {
    }

    /**
     * Removes a listener of throughput. This should be called after a
     * {@link NetworkQualityThroughputListener} is added with
     * {@link #addThroughputListener} in order to stop receiving observations.
     *
     * @param listener the listener of throughput.
     */
    public void removeThroughputListener(NetworkQualityThroughputListener listener) {
    }

    /**
     * Establishes a new connection to the resource specified by the {@link URL} {@code url}
     * using the given proxy.
     * <p>
     * <b>Note:</b> Cronet's {@link java.net.HttpURLConnection} implementation is subject to certain
     * limitations, see {@link #createURLStreamHandlerFactory} for details.
     *
     * @param url   URL of resource to connect to.
     * @param proxy proxy to use when establishing connection.
     * @return an {@link java.net.HttpURLConnection} instance implemented by this CronetEngine.
     * @throws IOException if an error occurs while opening the connection.
     */
    // TODO(pauljensen): Expose once implemented, http://crbug.com/418111
    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return url.openConnection(proxy);
    }

    /**
     * Registers a listener that gets called after the end of each request with the request info.
     *
     * <p>The listener is called on an {@link java.util.concurrent.Executor} provided by the
     * listener.
     *
     * @param listener the listener for finished requests.
     */
    public void addRequestFinishedListener(RequestFinishedInfo.Listener listener) {
    }

    /**
     * Removes a finished request listener.
     *
     * @param listener the listener to remove.
     */
    public void removeRequestFinishedListener(RequestFinishedInfo.Listener listener) {
    }

    /**
     * Returns the HTTP RTT estimate (in milliseconds) computed by the network
     * quality estimator. Set to {@link #CONNECTION_METRIC_UNKNOWN} if the value
     * is unavailable. This must be called after
     * {@link Builder#enableNetworkQualityEstimator}, and will throw an
     * exception otherwise.
     *
     * @return Estimate of the HTTP RTT in milliseconds.
     */
    public int getHttpRttMs() {
        return CONNECTION_METRIC_UNKNOWN;
    }

    /**
     * Returns the transport RTT estimate (in milliseconds) computed by the
     * network quality estimator. Set to {@link #CONNECTION_METRIC_UNKNOWN} if
     * the value is unavailable. This must be called after
     * {@link Builder#enableNetworkQualityEstimator}, and will throw an
     * exception otherwise.
     *
     * @return Estimate of the transport RTT in milliseconds.
     */
    public int getTransportRttMs() {
        return CONNECTION_METRIC_UNKNOWN;
    }

    /**
     * Returns the downstream throughput estimate (in kilobits per second)
     * computed by the network quality estimator. Set to
     * {@link #CONNECTION_METRIC_UNKNOWN} if the value is
     * unavailable. This must be called after
     * {@link Builder#enableNetworkQualityEstimator}, and will
     * throw an exception otherwise.
     *
     * @return Estimate of the downstream throughput in kilobits per second.
     */
    public int getDownstreamThroughputKbps() {
        return CONNECTION_METRIC_UNKNOWN;
    }
}
