package me.ag2s.cronet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import okhttp3.Call;


public class OutputStreamProxy extends OutputStream {
    private final OutputStream mRealOutputStream;
    private final Call mCall;
    private final HttpURLConnection mConnection;

    public OutputStreamProxy(OutputStream outputStream, Call mCall, HttpURLConnection mConnection) {
        this.mRealOutputStream = outputStream;
        this.mCall = mCall;
        this.mConnection = mConnection;
    }

    @Override
    public void write(int b) throws IOException {
//        Log.d(CronetHelper.TAG, "OutputStreamProxy write: " + b);
        if (mCall.isCanceled()) {
            //Log.d(CronetHelper.TAG, "OutputStreamProxy write Canceled!!!!!!!!!!!!");
            mConnection.disconnect();
            close();
            throw new IOException("Canceled");
        }
        mRealOutputStream.write(b);
    }
}