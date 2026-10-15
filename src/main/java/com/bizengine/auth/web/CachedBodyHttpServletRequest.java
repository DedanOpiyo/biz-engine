package com.bizengine.auth.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
        super(request);
        if (request.getContentLengthLong() > maxBytes) throw new PayloadTooLargeException();
        this.body = request.getInputStream().readNBytes(maxBytes + 1);
        if (body.length > maxBytes) throw new PayloadTooLargeException();
    }

    byte[] getCachedBody() { return body.clone(); }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return input.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {
                try { if (isFinished()) listener.onAllDataRead(); else listener.onDataAvailable(); }
                catch (IOException ex) { listener.onError(ex); }
            }
            @Override public int read() { return input.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    static final class PayloadTooLargeException extends RuntimeException { }
}
