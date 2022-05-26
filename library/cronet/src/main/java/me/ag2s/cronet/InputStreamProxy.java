package me.ag2s.cronet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import okhttp3.Call;


public class InputStreamProxy extends InputStream {
    private final InputStream mRealInputStream;
    private final Call mCall;
    private final HttpURLConnection mConnection;

    public InputStreamProxy(InputStream inputStream, Call mCall, HttpURLConnection mConnection) {
        this.mRealInputStream = inputStream;
        this.mCall = mCall;
        this.mConnection = mConnection;
    }


    @Override
    public int read() throws IOException {
        if (mCall.isCanceled()) {
            //Log.d(CronetHelper.TAG, "InputStreamProxy read Canceled!!!!!!!!!!!!");
            mConnection.disconnect();
            close();
            throw new IOException("Canceled");
        }
        return mRealInputStream.read();
    }
}