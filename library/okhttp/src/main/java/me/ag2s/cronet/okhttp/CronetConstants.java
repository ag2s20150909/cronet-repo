package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okio.Source;

final class CronetConstants {
    public static final int MAX_FOLLOW_COUNT = 20;
    public static final int BYTE_BUFFER_CAPACITY = 32 * 1024;
    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    public static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    public static final String CONTENT_ENCODING_HEADER_NAME = "Content-Encoding";
    public static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";

    public static final Set<String> ENCODINGS_HANDLED_BY_CRONET = Set.of("br", "deflate", "gzip", "x-gzip");


    static Protocol protocolFromNegotiatedProtocol(UrlResponseInfo responseInfo) {
        String negotiatedProtocol = responseInfo.getNegotiatedProtocol().toLowerCase();
        if (negotiatedProtocol.contains("h3")) {
            return Protocol.QUIC;
        } else if (negotiatedProtocol.contains("quic")) {
            return Protocol.QUIC;
        } else if (negotiatedProtocol.contains("spdy")) {
            return Protocol.SPDY_3;
        } else if (negotiatedProtocol.contains("h2")) {
            return Protocol.HTTP_2;
        } else if (negotiatedProtocol.contains("1.1")) {
            return Protocol.HTTP_1_1;
        } else {
            return Protocol.HTTP_1_0;
        }
    }

    static Response.Builder buildPriorResponse(Response.Builder builder, Request request, List<UrlResponseInfo> infoList, List<String> urlChain) throws IOException {

        Response priorResponse = null;
        if (!infoList.isEmpty()) {
            if (urlChain.size() < infoList.size()) {
                throw new IOException("The number of redirects should be consistent across URLs and headers!");
            }

            for (int i = 0; i < infoList.size(); i++) {
                UrlResponseInfo info = infoList.get(i);

                priorResponse=builder.request(request.newBuilder().url(urlChain.get(i)).build())
                        .code(info.getHttpStatusCode())
                        .message(info.getHttpStatusText())
                        .headers(headersFromResponse(info, false)).build();
                builder.priorResponse(priorResponse);

            }
        }


        return builder;
    }

    @NonNull
    static Headers headersFromResponse(@NonNull UrlResponseInfo responseInfo, boolean keepEncodingAffectedHeaders) {
        List<Map.Entry<String, String>> headers = responseInfo.getAllHeadersAsList();


        Headers.Builder headerBuilder = new Headers.Builder();
        for (Map.Entry<String, String> entry : headers) {

            //Log.e(TAG, entry.getKey() + ":" + entry.getValue());


            try {
                if (!keepEncodingAffectedHeaders && (entry.getKey().equalsIgnoreCase(CONTENT_LENGTH_HEADER_NAME) || entry.getKey().equalsIgnoreCase(CONTENT_ENCODING_HEADER_NAME))) {
                    // Strip all content encoding headers as decoding is done handled by cronet
                    continue;
                }

                headerBuilder.add(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                //Log.w(TAG, "Invalid HTTP header/value: " + entry.getKey() + entry.getValue());
                // Ignore that header
            }
        }

        return headerBuilder.build();
    }

    @NonNull
    static Response.Builder responseFromResponse(@NonNull Response.Builder builder, Call call, @NonNull UrlResponseInfo info, boolean keepEncodingAffectedHeaders) {
        Protocol protocol = protocolFromNegotiatedProtocol(info);
        Headers headers = headersFromResponse(info, keepEncodingAffectedHeaders);

        return builder
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .request(call.request().newBuilder().url(info.getUrl()).build())
                .code(info.getHttpStatusCode())
                .message(info.getHttpStatusText())
                .headers(headers);
    }


    static Response buildResponse(@NonNull Response response, @NonNull Call call, @NonNull UrlResponseInfo info, Source source) {
        boolean keepEncodingAffectedHeaders = keepEncodingAffectedHeaders(info);
        Protocol protocol = protocolFromNegotiatedProtocol(info);
        Headers headers = headersFromResponse(info, keepEncodingAffectedHeaders);
        Response.Builder builder = response.newBuilder()
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .request(call.request().newBuilder().url(info.getUrl()).build())
                .code(info.getHttpStatusCode())
                .message(info.getHttpStatusText())
                .headers(headers);
        if (!call.request().method().equalsIgnoreCase("HEAD")) {


        }

        return builder.build();


    }

    static long getContentLength(@NonNull UrlResponseInfo info) {
        List<String> contentLengthHeader = info.getAllHeaders().get(CONTENT_LENGTH_HEADER_NAME);
        if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
            return -1;
        }
        try {
            return Long.parseLong(contentLengthHeader.get(0));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static MediaType getMediaType(@NonNull UrlResponseInfo info) {
        List<String> contentLengthHeader = info.getAllHeaders().get(CronetConstants.CONTENT_TYPE_HEADER_NAME);
        if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
            return MediaType.parse("text/plain; charset=\"utf-8\"");
        } else {
            return MediaType.parse(contentLengthHeader.get(0));
        }

    }

    static boolean keepEncodingAffectedHeaders(@NonNull UrlResponseInfo info) {
        List<String> contentEncodingItems = new ArrayList<>();
        contentEncodingItems = getOrDefault(info.getAllHeaders(), CONTENT_ENCODING_HEADER_NAME, contentEncodingItems);
        if (contentEncodingItems.isEmpty()) {
            return true;
        }
        Set<String> types = new HashSet<>();
        for (String s : contentEncodingItems) {
            types.addAll(Arrays.asList(s.split(",")));
        }
        return !ENCODINGS_HANDLED_BY_CRONET.containsAll(types);
    }


    private static <K, V> V getOrDefault(Map<K, V> map, K key, @NonNull V defaultValue) {
        V value = map.get(key);
        return Objects.requireNonNullElse(value, defaultValue);
    }

}
