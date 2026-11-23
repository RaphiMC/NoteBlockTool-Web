/*
 * This file is part of NoteBlockTool-Web - https://github.com/RaphiMC/NoteBlockTool-Web
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblocktool.util.js;

import net.raphimc.audiomixer.io.pcm.PcmAudioInputStream;
import net.raphimc.audiomixer.util.JavaAudioFormatUtil;
import net.raphimc.audiomixer.util.PcmAudioFormat;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.Atomics;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.SharedArrayBuffer;
import org.teavm.jso.workers.MessagePort;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public class WebAudioSourceDataLine implements SourceDataLine {

    private static final int DEFAULT_BUFFER_MILLIS = 500;

    private final MessagePort hostPort;
    private final AudioFormat defaultFormat;
    private MessagePort processorPort;
    private AudioFormat format;
    private PcmAudioFormat pcmFormat;
    private int bufferSize;
    private int waitMillis;
    private RingBuffer audioBuffer;
    private Int8Array controlSignal;
    private volatile boolean open;
    private volatile boolean running;
    private volatile boolean flushing;
    private boolean closed;

    private AsyncCallback<Response> pendingResponse;
    private int requestId;
    private volatile int operationVersion;
    private volatile String lastError;

    public WebAudioSourceDataLine(final MessagePort hostPort, final AudioFormat format) {
        this.hostPort = hostPort;
        this.defaultFormat = format;
        this.format = format;
        this.bufferSize = JavaAudioFormatUtil.getPcmAudioFormat(this.format).millisToByteCount(DEFAULT_BUFFER_MILLIS);
        this.setupResponseListener(this.hostPort);
    }

    @Override
    public synchronized void open(final AudioFormat format, final int bufferSize) throws LineUnavailableException {
        if (this.open) {
            throw new IllegalStateException("The line is already open");
        }
        if (this.closed) {
            throw new LineUnavailableException("A closed line cannot be reopened");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be > 0: " + bufferSize);
        }
        if (bufferSize % format.getFrameSize() != 0) {
            throw new IllegalArgumentException("Buffer size must represent an integral number of frames (" + bufferSize + " bytes, frameSize = " + format.getFrameSize() + " bytes)");
        }
        final PcmAudioFormat pcmFormat = JavaAudioFormatUtil.getPcmAudioFormat(format);
        if (format.getFrameSize() != pcmFormat.bytesPerFrame() || format.getFrameRate() != format.getSampleRate()) {
            throw new IllegalArgumentException("Unsupported PCM frame format: " + format);
        }
        this.checkError();
        final SharedArrayBuffer audioBuffer = RingBuffer.getStorageForCapacity(pcmFormat.byteCountToSampleCount(bufferSize), JsNative.Float32Array_getConstructor());
        final SharedArrayBuffer controlBuffer = JsNative.SharedArrayBuffer_create(1);
        final OpenRequest request = OpenRequest.create();
        request.setSampleRate(format.getSampleRate());
        request.setChannels(format.getChannels());
        request.setAudioBuffer(audioBuffer);
        request.setControlBuffer(controlBuffer);
        final OpenResponse response;
        try {
            response = this.request(request);
        } catch (final IllegalStateException e) {
            throw new LineUnavailableException(e.getMessage());
        }
        this.processorPort = response.getPort();
        this.setupResponseListener(this.processorPort);
        this.format = format;
        this.pcmFormat = pcmFormat;
        this.bufferSize = bufferSize;
        this.waitMillis = Math.clamp(Math.round(this.pcmFormat.byteCountToMillis(this.bufferSize) / 4F), 1, 25);
        this.audioBuffer = new RingBuffer(audioBuffer, JsNative.Float32Array_getConstructor());
        this.controlSignal = new Int8Array(controlBuffer);
        this.open = true;
    }

    @Override
    public synchronized void open(final AudioFormat format) throws LineUnavailableException {
        this.open(format, JavaAudioFormatUtil.getPcmAudioFormat(format).millisToByteCount(DEFAULT_BUFFER_MILLIS));
    }

    @Override
    public synchronized void open() throws LineUnavailableException {
        this.open(this.format, this.bufferSize);
    }

    @Override
    public int write(final byte[] b, final int off, final int len) {
        if (len == 0) {
            return 0;
        }
        if (len < 0) {
            throw new IllegalArgumentException("illegal len: " + len);
        }
        if (len % this.getFormat().getFrameSize() != 0) {
            throw new IllegalArgumentException("illegal request to write non-integral number of frames (" + len + " bytes, frameSize = " + this.getFormat().getFrameSize() + " bytes)");
        }
        if (off < 0) {
            throw new ArrayIndexOutOfBoundsException(off);
        }
        if ((long) off + (long) len > (long) b.length) {
            throw new ArrayIndexOutOfBoundsException(b.length);
        }
        this.checkError();
        if (!this.open) {
            return 0;
        }
        final PcmAudioInputStream inputStream = new PcmAudioInputStream(new ByteArrayInputStream(b, off, len), this.pcmFormat);
        final int totalSampleCount = this.pcmFormat.byteCountToSampleCount(len);
        final float[] samples = new float[Math.min(totalSampleCount, this.pcmFormat.byteCountToSampleCount(this.bufferSize))];
        final int version = this.operationVersion;
        int writtenSampleCount = 0;
        while (writtenSampleCount < totalSampleCount && this.open && version == this.operationVersion) {
            if (!this.flushing) {
                final int sampleCount = Math.min(Math.min(totalSampleCount - writtenSampleCount, samples.length), this.audioBuffer.availableWrite());
                if (sampleCount > 0) {
                    try {
                        for (int i = 0; i < sampleCount; i++) {
                            samples[i] = inputStream.read();
                        }
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    writtenSampleCount += this.audioBuffer.push(samples, sampleCount, 0);
                    continue;
                }
                if (!this.running) {
                    break;
                }
            }
            if (!this.awaitBufferProgress(this.waitMillis)) {
                break;
            }
        }
        return this.pcmFormat.sampleCountToByteCount(writtenSampleCount);
    }

    @Override
    public void drain() {
        this.checkError();
        final int version = this.operationVersion;
        while (this.open && this.running && version == this.operationVersion && this.audioBuffer.availableRead() > 0) {
            final int remainingMillis = Math.round(this.pcmFormat.format().sampleCountToMillis(this.audioBuffer.availableRead()));
            if (!this.awaitBufferProgress(Math.clamp(remainingMillis, 1, this.waitMillis))) {
                break;
            }
        }
    }

    @Override
    public synchronized void flush() {
        if (!this.open) {
            return;
        }
        this.operationVersion++;
        this.flushing = true;
        try {
            this.request(FlushRequest.create());
        } finally {
            this.flushing = false;
        }
    }

    @Override
    public synchronized void start() {
        this.checkError();
        if (this.open && !this.running) {
            this.request(StartRequest.create());
            this.running = true;
        }
    }

    @Override
    public synchronized void stop() {
        if (!this.open) {
            return;
        }
        this.operationVersion++;
        if (this.running) {
            this.request(StopRequest.create());
            this.running = false;
        }
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.open = false;
        this.running = false;
        this.lastError = null;
        try {
            this.request(CloseRequest.create());
        } finally {
            this.hostPort.close();
            if (this.processorPort != null) {
                this.processorPort.close();
            }
            this.format = this.defaultFormat;
            this.bufferSize = JavaAudioFormatUtil.getPcmAudioFormat(this.format).millisToByteCount(DEFAULT_BUFFER_MILLIS);
        }
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public int available() {
        this.checkError();
        return this.open ? this.pcmFormat.sampleCountToByteCount(this.audioBuffer.availableWrite()) : 0;
    }

    @Override
    public int getFramePosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLongFramePosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMicrosecondPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Line.Info getLineInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Control[] getControls() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isControlSupported(final Control.Type control) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Control getControl(final Control.Type control) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLineListener(final LineListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLineListener(final LineListener listener) {
        throw new UnsupportedOperationException();
    }

    private void setupResponseListener(final MessagePort port) {
        port.onMessage(event -> {
            final Response response = (Response) event.getData();
            if (response.getId() == ErrorResponse.ID) {
                this.handleError(((ErrorResponse) response).getErrorMessage());
                return;
            }
            final AsyncCallback<Response> callback = this.pendingResponse;
            this.pendingResponse = null;
            if (callback != null) {
                callback.complete(response);
            }
        });
        port.onMessageError(_ -> this.handleError("The source data line received an unreadable response"));
        port.start();
    }

    private boolean awaitBufferProgress(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
        this.checkError();
        return true;
    }

    @SuppressWarnings("unchecked")
    private <T extends Response> T request(final Request request) {
        request.setId(this.requestId++);
        final Response response = this.executeRequest(request);
        if (response.getId() != request.getId()) {
            throw new IllegalStateException("ID mismatch: " + response.getId() + " != " + request.getId());
        }
        return (T) response;
    }

    @Async
    private native Response executeRequest(final Request request);

    private void executeRequest(final Request request, final AsyncCallback<Response> callback) {
        this.pendingResponse = callback;
        switch (request.getType()) {
            case OpenRequest.TYPE, CloseRequest.TYPE -> this.hostPort.postMessage(request);
            default -> {
                Atomics.store(this.controlSignal, 0, (byte) 1);
                this.processorPort.postMessage(request);
            }
        }
    }

    private void handleError(final String errorMessage) {
        this.lastError = errorMessage;
        this.open = false;
        this.running = false;
        final AsyncCallback<Response> callback = this.pendingResponse;
        this.pendingResponse = null;
        if (callback != null) {
            callback.error(new IllegalStateException(errorMessage));
        }
    }

    private void checkError() {
        if (this.lastError != null) {
            throw new IllegalStateException(this.lastError);
        }
    }

    private interface Request extends JSObject {

        @JSProperty
        String getType();

        @JSProperty
        void setType(final String type);

        @JSProperty
        int getId();

        @JSProperty
        void setId(final int id);

    }

    private interface OpenRequest extends Request {

        String TYPE = "open";

        static OpenRequest create() {
            final OpenRequest openRequest = JSObjects.create();
            openRequest.setType(TYPE);
            return openRequest;
        }

        @JSProperty
        void setSampleRate(final float sampleRate);

        @JSProperty
        void setChannels(final int channels);

        @JSProperty
        void setAudioBuffer(final SharedArrayBuffer audioBuffer);

        @JSProperty
        void setControlBuffer(final SharedArrayBuffer controlBuffer);

    }

    private interface StartRequest extends Request {

        String TYPE = "start";

        static StartRequest create() {
            final StartRequest startRequest = JSObjects.create();
            startRequest.setType(TYPE);
            return startRequest;
        }

    }

    private interface FlushRequest extends Request {

        String TYPE = "flush";

        static FlushRequest create() {
            final FlushRequest flushRequest = JSObjects.create();
            flushRequest.setType(TYPE);
            return flushRequest;
        }

    }

    private interface StopRequest extends Request {

        String TYPE = "stop";

        static StopRequest create() {
            final StopRequest stopRequest = JSObjects.create();
            stopRequest.setType(TYPE);
            return stopRequest;
        }

    }

    private interface CloseRequest extends Request {

        String TYPE = "close";

        static CloseRequest create() {
            final CloseRequest closeRequest = JSObjects.create();
            closeRequest.setType(TYPE);
            return closeRequest;
        }

    }

    private interface Response extends JSObject {

        @JSProperty
        int getId();

    }

    private interface OpenResponse extends Response {

        @JSProperty
        MessagePort getPort();

    }

    private interface ErrorResponse extends Response {

        int ID = -1;

        @JSProperty
        String getErrorMessage();

    }

}
