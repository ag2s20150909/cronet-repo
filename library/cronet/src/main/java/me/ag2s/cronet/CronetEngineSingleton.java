//package me.ag2s.cronet;
//
//import android.content.Context;
//
//import org.chromium.net.CronetEngine;
//import org.chromium.net.ExperimentalCronetEngine;
//import org.chromium.net.MyCronetEngine;
//
//public final class CronetEngineSingleton {
//
//    // non instantiable
//    private CronetEngineSingleton() {}
//
//    private static volatile CronetEngine cronetEngineSingleton;
//
//    public static CronetEngine getSingleton() {
//
//        // Lazily create the engine.
//        if (cronetEngineSingleton == null) {
//            synchronized (CronetEngineSingleton.class) {
//                // have to re-check since this might have changed before synchronization, but we don't
//                // want to synchronize just to check for null.
//                if (cronetEngineSingleton == null) {
//                    cronetEngineSingleton = createEngine();
//                }
//            }
//        }
//
//        return cronetEngineSingleton;
//    }
//
//    private static CronetEngine createEngine() {
//        Context mContext = CronetContentProvider.getCtx();
//      ExperimentalCronetEngine.Builder builder = new MyCronetEngine.Builder(mContext)
//                .enableQuic(true)
//                .enablePublicKeyPinningBypassForLocalTrustAnchors(true)
//                .enableNetworkQualityEstimator(true)
//                .addQuicHint("http3.is", 443, 443)
//                .enableHttp2(true)
//                .enableSdch(true)
//                .setExperimentalOptions("")
//
//                .setThreadPriority(0);
//
//      return builder.build();
//    }
//}
