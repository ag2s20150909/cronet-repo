package org.chromium.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Set;

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
public abstract class MyCronetEngine extends CronetEngine {
    private static final String TAG = "MyCronetEngine";


    /**
     * A builder for {@link CronetEngine}s, which allows runtime configuration of {@code
     * CronetEngine}. Configuration options are set on the builder and then {@link #build} is called
     * to create the {@code CronetEngine}.
     */
    // NOTE(kapishnikov): In order to avoid breaking the existing API clients, all future methods
    // added to this class and other API classes must have default implementation.
    public static class Builder extends CronetEngine.Builder {
        private static final String TAG = "CronetEngine.Builder";


        /**
         * Constructs a {@link Builder} object that facilitates creating a {@link CronetEngine}. The
         * default configuration enables HTTP/2 and QUIC, but disables the HTTP cache.
         *
         * @param context Android {@link Context}, which is used by {@link Builder} to retrieve the
         * application context. A reference to only the application context will be kept, so as to
         * avoid extending the lifetime of {@code context} unnecessarily.
         */
        public Builder(Context context) {
            this(MyCronetHelper.createBuilderDelegate(context));
        }

        /**
         * Constructs {@link Builder} with a given delegate that provides the actual implementation
         * of the {@code Builder} methods. This constructor is used only by the internal
         * implementation.
         *
         * @param builderDelegate delegate that provides the actual implementation.
         * <p>{@hide}
         */
        public Builder(ICronetEngineBuilder builderDelegate) {
            super(builderDelegate);
        }



        /**
         * Overrides the User-Agent header for all requests. An explicitly set User-Agent header
         * (set using {@link UrlRequest.Builder#addHeader}) will override a value set using this
         * function.
         *
         * @param userAgent the User-Agent string to use for all requests.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder setUserAgent(String userAgent) {
            mBuilderDelegate.setUserAgent(userAgent);
            return this;
        }

        /**
         * Sets directory for HTTP Cache and Cookie Storage. The directory must exist.
         *
         * <p><b>NOTE:</b> Do not use the same storage directory with more than one {@code
         * CronetEngine} at a time. Access to the storage directory does not support concurrent
         * access by multiple {@code CronetEngine}s.
         *
         * @param value path to existing directory.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder setStoragePath(String value) {
            mBuilderDelegate.setStoragePath(value);
            return this;
        }

        /**
         * Sets a {@link LibraryLoader} to be used to load the native library. If not set, the
         * library will be loaded using {@link System#loadLibrary}.
         *
         * @param loader {@code LibraryLoader} to be used to load the native library.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder setLibraryLoader(LibraryLoader loader) {
            mBuilderDelegate.setLibraryLoader(loader);
            return this;
        }

        /**
         * Sets whether <a href="https://www.chromium.org/quic">QUIC</a> protocol is enabled.
         * Defaults to enabled. If QUIC is enabled, then QUIC User Agent Id containing application
         * name and Cronet version is sent to the server.
         *
         * @param value {@code true} to enable QUIC, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder enableQuic(boolean value) {
            mBuilderDelegate.enableQuic(value);
            return this;
        }

        /**
         * Sets whether <a href="https://tools.ietf.org/html/rfc7540">HTTP/2</a> protocol is
         * enabled. Defaults to enabled.
         *
         * @param value {@code true} to enable HTTP/2, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder enableHttp2(boolean value) {
            mBuilderDelegate.enableHttp2(value);
            return this;
        }

        /**
         * @deprecated SDCH is deprecated in Cronet M63. This method is a no-op. {@hide exclude from
         * JavaDoc}.
         */
        @Override
        @Deprecated
        public Builder enableSdch(boolean value) {
            return this;
        }

        /**
         * Sets whether <a href="https://tools.ietf.org/html/rfc7932">Brotli</a> compression is
         * enabled. If enabled, Brotli will be advertised in Accept-Encoding request headers.
         * Defaults to disabled.
         *
         * @param value {@code true} to enable Brotli, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder enableBrotli(boolean value) {
            mBuilderDelegate.enableBrotli(value);
            return this;
        }



        /**
         * Enables or disables caching of HTTP data and other information like QUIC server
         * information.
         *
         * @param cacheMode control location and type of cached data. Must be one of {@link
         * #HTTP_CACHE_DISABLED HTTP_CACHE_*}.
         * @param maxSize maximum size in bytes used to cache data (advisory and maybe exceeded at
         * times).
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder enableHttpCache(int cacheMode, long maxSize) {
            mBuilderDelegate.enableHttpCache(cacheMode, maxSize);
            return this;
        }

        /**
         * Adds hint that {@code host} supports QUIC. Note that {@link #enableHttpCache
         * enableHttpCache}
         * ({@link #HTTP_CACHE_DISK}) is needed to take advantage of 0-RTT connection establishment
         * between sessions.
         *
         * @param host hostname of the server that supports QUIC.
         * @param port host of the server that supports QUIC.
         * @param alternatePort alternate port to use for QUIC.
         * @return the builder to facilitate chaining.
         */
        @Override
        public Builder addQuicHint(String host, int port, int alternatePort) {
            mBuilderDelegate.addQuicHint(host, port, alternatePort);
            return this;
        }

        /** Sets experimental options to be used in Cronet.
         *
         * @param options JSON formatted experimental options.
         * @return the builder to facilitate chaining.
         */
        public Builder setExperimentalOptions(String options) {
            mBuilderDelegate.setExperimentalOptions(options);
            return this;
        }

        /**
         * Pins a set of public keys for a given host. By pinning a set of public keys, {@code
         * pinsSha256}, communication with {@code hostName} is required to authenticate with a
         * certificate with a public key from the set of pinned ones. An app can pin the public key
         * of the root certificate, any of the intermediate certificates or the end-entry
         * certificate. Authentication will fail and secure communication will not be established if
         * none of the public keys is present in the host's certificate chain, even if the host
         * attempts to authenticate with a certificate allowed by the device's trusted store of
         * certificates.
         *
         * <p>Calling this method multiple times with the same host name overrides the previously
         * set pins for the host.
         *
         * <p>More information about the public key pinning can be found in <a
         * href="https://tools.ietf.org/html/rfc7469">RFC 7469</a>.
         *
         * @param hostName name of the host to which the public keys should be pinned. A host that
         * consists only of digits and the dot character is treated as invalid.
         * @param pinsSha256 a set of pins. Each pin is the SHA-256 cryptographic hash of the
         * DER-encoded ASN.1 representation of the Subject Public Key Info (SPKI) of the host's
         * X.509 certificate. Use {@link java.security.cert.Certificate#getPublicKey()
         * Certificate.getPublicKey()} and {@link java.security.Key#getEncoded() Key.getEncoded()}
         * to obtain DER-encoded ASN.1 representation of the SPKI. Although, the method does not
         * mandate the presence of the backup pin that can be used if the control of the primary
         * private key has been lost, it is highly recommended to supply one.
         * @param includeSubdomains indicates whether the pinning policy should be applied to
         *         subdomains
         * of {@code hostName}.
         * @param expirationDate specifies the expiration date for the pins.
         * @return the builder to facilitate chaining.
         * @throws NullPointerException if any of the input parameters are {@code null}.
         * @throws IllegalArgumentException if the given host name is invalid or {@code pinsSha256}
         * contains a byte array that does not represent a valid SHA-256 hash.
         */
        public Builder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256,
                                        boolean includeSubdomains, Date expirationDate) {
            mBuilderDelegate.addPublicKeyPins(
                    hostName, pinsSha256, includeSubdomains, expirationDate);
            return this;
        }

        /**
         * Enables or disables public key pinning bypass for local trust anchors. Disabling the
         * bypass for local trust anchors is highly discouraged since it may prohibit the app from
         * communicating with the pinned hosts. E.g., a user may want to send all traffic through an
         * SSL enabled proxy by changing the device proxy settings and adding the proxy certificate
         * to the list of local trust anchor. Disabling the bypass will most likely prevent the app
         * from sending any traffic to the pinned hosts. For more information see 'How does key
         * pinning interact with local proxies and filters?' at
         * https://www.chromium.org/Home/chromium-security/security-faq
         *
         * @param value {@code true} to enable the bypass, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        public Builder enablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
            mBuilderDelegate.enablePublicKeyPinningBypassForLocalTrustAnchors(value);
            return this;
        }

        /**
         * Sets the thread priority of Cronet's internal thread.
         *
         * @param priority the thread priority of Cronet's internal thread. A Linux priority level,
         *         from
         * -20 for highest scheduling priority to 19 for lowest scheduling priority. For more
         * information on values, see {@link android.os.Process#setThreadPriority(int, int)} and
         * {@link android.os.Process#THREAD_PRIORITY_DEFAULT THREAD_PRIORITY_*} values.
         * @return the builder to facilitate chaining.
         */
        public Builder setThreadPriority(int priority) {
            mBuilderDelegate.setThreadPriority(priority);
            return this;
        }

        /**
         * Enables the network quality estimator, which collects and reports measurements of round
         * trip time (RTT) and downstream throughput at various layers of the network stack. After
         * enabling the estimator, listeners of RTT and throughput can be added with {@link
         * #addRttListener} and
         * {@link #addThroughputListener} and removed with {@link #removeRttListener} and {@link
         * #removeThroughputListener}. The estimator uses memory and CPU only when enabled.
         *
         * @param value {@code true} to enable network quality estimator, {@code false} to disable.
         * @return the builder to facilitate chaining.
         */
        public Builder enableNetworkQualityEstimator(boolean value) {
            mBuilderDelegate.enableNetworkQualityEstimator(value);
            return this;
        }

        /**
         * Build a {@link CronetEngine} using this builder's configuration.
         *
         * @return constructed {@link CronetEngine}.
         */
        public ExperimentalCronetEngine build() {
            int implLevel = getImplementationApiLevel();
            if (implLevel != -1 && implLevel < getMaximumApiLevel()) {
                Log.w(TAG,
                        "The implementation version is lower than the API version. Calls to "
                                + "methods added in API " + (implLevel + 1) + " and newer will "
                                + "likely have no effect.");
            }

            return mBuilderDelegate.build();
        }



        private int getMaximumApiLevel() {
            return ApiVersion.getMaximumAvailableApiLevel();
        }

        /**
         * Returns the implementation version, the implementation being represented by the delegate
         * builder, or {@code -1} if the version couldn't be retrieved.
         */
        private int getImplementationApiLevel() {
            try {
                ClassLoader implClassLoader = mBuilderDelegate.getClass().getClassLoader();
                Class<?> implVersionClass =
                        implClassLoader.loadClass("org.chromium.net.impl.ImplVersion");
                Method getApiLevel = implVersionClass.getMethod("getApiLevel");
                int implementationApiLevel = (Integer) getApiLevel.invoke(null);

                return implementationApiLevel;
            } catch (Exception e) {
                // Any exception in the block above isn't critical, don't bother the app about it.
                return -1;
            }
        }


    }





}
