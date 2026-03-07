/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletResponse wrapper that captures the response body in a byte array,
 * allowing it to be read for HMAC signing before being written to the client.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    /**
     * Creates a new CachedBodyHttpServletResponse.
     *
     * @param response the original response
     */
    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new CachedServletOutputStream(cachedOutputStream);
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(cachedOutputStream, StandardCharsets.UTF_8));
        }
        return writer;
    }

    /**
     * Returns the captured response body bytes.
     *
     * @return the response body as a byte array
     */
    public byte[] getCachedBody() {
        if (writer != null) {
            writer.flush();
        }
        return cachedOutputStream.toByteArray();
    }

    /**
     * Writes the cached body to the original response output stream.
     *
     * @throws IOException if writing fails
     */
    public void writeBodyToResponse() throws IOException {
        byte[] body = getCachedBody();
        getResponse().setContentLength(body.length);
        getResponse().getOutputStream().write(body);
        getResponse().getOutputStream().flush();
    }

    private static class CachedServletOutputStream extends ServletOutputStream {

        private final ByteArrayOutputStream outputStream;

        CachedServletOutputStream(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException("setWriteListener is not supported");
        }

        @Override
        public void write(int b) {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            outputStream.write(b, off, len);
        }
    }
}
