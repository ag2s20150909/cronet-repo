package me.ag2s.cronet.okhttp;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.ByteString;

public class FileDescriptorRequestBody extends RequestBody {

    private static final int BYTE_BUFFER_CAPACITY = 32 * 1024;
    private final ParcelFileDescriptor pfd;
    private final MediaType mediaType;
    private long size = -1;

    public FileDescriptorRequestBody(ParcelFileDescriptor pfd) {
        this.pfd = pfd;
        this.mediaType=MediaType.parse("application/octet-stream");
    }
    public FileDescriptorRequestBody(ParcelFileDescriptor pfd,MediaType mediaType) {
        this.pfd = pfd;
        this.mediaType=mediaType;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        if (size > 0) {
            return size;
        }
        if (checkPfd()) {
            throw new IOException("pfd is closed");
        }

        try {
            size = Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_END);
            return size;
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }

    public ParcelFileDescriptor getPfd(){
        return pfd;
    }

    @Override
    public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
        if (checkPfd()) {
            throw new IOException("pfd is closed");
        }
        try {
            size = Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_END);
            if (size <= 0) {
                throw new IOException("size is negative");
            }
            Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_SET);
            Log.e("Cronet","file size is  "+size+"");
            byte[] byteBuffer = new byte[Math.min((int) size, BYTE_BUFFER_CAPACITY)];
            long pos = 0;

            while (pos < size) {
                int read= Os.read(pfd.getFileDescriptor(), byteBuffer,0,byteBuffer.length);
                //Log.e("Cronet","read11  "+read+" to request");
                read= (int)(Os.lseek(pfd.getFileDescriptor(),0,OsConstants.SEEK_CUR)-pos);
                //Log.e("Cronet", new String(byteBuffer, Charset.forName("GBK")));
                Log.e("Cronet","read12 "+read+" to request");
                bufferedSink.write(byteBuffer,0,read);
                pos=Os.lseek(pfd.getFileDescriptor(),0,OsConstants.SEEK_CUR);
            }
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public boolean isOneShot() {
        return !checkPfd();
    }

    private boolean checkPfd() {
        return !pfd.getFileDescriptor().valid();

    }


    private void closePfd() {
        try {
            pfd.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
