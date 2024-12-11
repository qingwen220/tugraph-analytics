/*
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.antgroup.geaflow.shuffle.pipeline.slice;

import com.antgroup.geaflow.common.encoder.Encoders;
import com.antgroup.geaflow.common.exception.GeaflowRuntimeException;
import com.antgroup.geaflow.common.iterator.CloseableIterator;
import com.antgroup.geaflow.shuffle.config.ShuffleConfig;
import com.antgroup.geaflow.shuffle.message.SliceId;
import com.antgroup.geaflow.shuffle.pipeline.buffer.OutBuffer;
import com.antgroup.geaflow.shuffle.pipeline.buffer.PipeBuffer;
import com.antgroup.geaflow.shuffle.pipeline.buffer.ShuffleMemoryTracker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpillablePipelineSlice extends AbstractSlice {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpillablePipelineSlice.class);

    private static final int BUFFER_SIZE = 64 * 1024;

    private final String fileName;
    private OutputStream outputStream;
    private CloseableIterator<PipeBuffer> streamBufferIterator;
    private PipeBuffer value;
    private volatile boolean ready2read = false;

    // Whether force write data to memory;
    private final boolean forceMemory;
    // Whether force write data to disk;
    private final boolean forceDisk;
    // Bytes count in memory.
    private long memoryBytes = 0;
    // Bytes count on disk.
    private long diskBytes = 0;

    public SpillablePipelineSlice(String taskLogTag, SliceId sliceId, int refCount) {
        super(taskLogTag, sliceId, refCount);
        this.fileName = String.format("shuffle-%d-%d-%d",
            sliceId.getPipelineId(), sliceId.getEdgeId(), sliceId.getSliceIndex());
        this.forceMemory = ShuffleConfig.getInstance().isForceMemory();
        this.forceDisk = ShuffleConfig.getInstance().isForceDisk();
    }

    public String getFileName() {
        return this.fileName;
    }

    //////////////////////////////
    // Produce data.
    //////////////////////////////

    @Override
    public boolean add(PipeBuffer buffer) {
        if (this.isReleased || this.ready2read) {
            throw new GeaflowRuntimeException("slice already released or mark finish: " + this.getSliceId());
        }
        totalBufferCount++;

        if (this.forceMemory) {
            this.writeMemory(buffer);
            return true;
        }
        if (this.forceDisk) {
            this.writeDisk(buffer);
            return true;
        }

        this.writeMemory(buffer);
        if (!ShuffleMemoryTracker.getInstance().checkMemoryEnough()) {
            this.spillDisk();
        }
        return true;
    }

    private void writeMemory(PipeBuffer buffer) {
        this.buffers.add(buffer);
        this.memoryBytes += buffer.getBufferSize();
    }

    private void writeDisk(PipeBuffer buffer) {
        try {
            if (this.outputStream == null) {
                this.outputStream = this.createTmpFile();
            }
            this.write2Stream(buffer);
        } catch (IOException e) {
            throw new GeaflowRuntimeException(e);
        }
    }

    private void spillDisk() {
        try {
            if (this.outputStream == null) {
                this.outputStream = this.createTmpFile();
            }
            while (!buffers.isEmpty()) {
                PipeBuffer buffer = buffers.poll();
                write2Stream(buffer);
            }
        } catch (IOException e) {
            throw new GeaflowRuntimeException(e);
        }
    }

    private void write2Stream(PipeBuffer buffer) throws IOException {
        OutBuffer outBuffer = buffer.getBuffer();
        if (outBuffer == null) {
            Encoders.INTEGER.encode(0, this.outputStream);
            Encoders.LONG.encode(buffer.getBatchId(), this.outputStream);
            Encoders.INTEGER.encode(buffer.getCount(), this.outputStream);
        } else {
            this.diskBytes += buffer.getBufferSize();
            Encoders.INTEGER.encode(outBuffer.getBufferSize(), this.outputStream);
            Encoders.LONG.encode(buffer.getBatchId(), this.outputStream);
            outBuffer.write(this.outputStream);
            outBuffer.release();
        }
    }

    private OutputStream createTmpFile() {
        try {
            Path path = Paths.get(this.fileName);
            Files.deleteIfExists(path);
            Files.createFile(path);
            return new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.WRITE), BUFFER_SIZE);
        } catch (IOException e) {
            throw new GeaflowRuntimeException(e);
        }
    }

    private InputStream readFromTmpFile() {
        try {
            Path path = Paths.get(this.fileName);
            return new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ), BUFFER_SIZE);
        } catch (IOException e) {
            throw new GeaflowRuntimeException(e);
        }
    }

    @Override
    public void flush() {
        if (this.outputStream != null) {
            try {
                spillDisk();
                this.outputStream.flush();
                this.outputStream.close();
                this.outputStream = null;
            } catch (IOException e) {
                throw new GeaflowRuntimeException(e);
            }
            this.streamBufferIterator = new FileStreamIterator();
        }
        this.ready2read = true;
        LOGGER.info("write file {} {} {}", this.fileName, this.memoryBytes, this.diskBytes);
    }

    //////////////////////////////
    // Consume data.
    //////////////////////////////

    @Override
    public Iterator<PipeBuffer> getBufferIterator() {
        if (diskBytes > 0) {
            return new FileStreamIterator();
        } else {
            return buffers.iterator();
        }
    }

    @Override
    public boolean hasNext() {
        if (this.isReleased) {
            return false;
        }
        if (this.value != null) {
            return true;
        }

        if (!buffers.isEmpty()) {
            this.value = buffers.poll();
            return true;
        }

        if (streamBufferIterator != null && streamBufferIterator.hasNext()) {
            this.value = streamBufferIterator.next();
            return true;
        }

        return false;
    }

    @Override
    public PipeBuffer next() {
        PipeBuffer next = this.value;
        this.value = null;
        return next;
    }

    @Override
    public boolean isReady2read() {
        return this.ready2read;
    }

    @Override
    public synchronized void release() {
        if (this.isReleased) {
            return;
        }
        this.buffers.clear();
        this.buffers = null;
        try {
            if (streamBufferIterator != null) {
                streamBufferIterator.close();
                streamBufferIterator = null;
            }
            Path path = Paths.get(this.fileName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new GeaflowRuntimeException(e);
        }
        this.isReleased = true;
    }


    class FileStreamIterator implements CloseableIterator<PipeBuffer> {
        private InputStream inputStream;
        private PipeBuffer next;

        FileStreamIterator() {
            this.inputStream = readFromTmpFile();
        }

        @Override
        public boolean hasNext() {
            if (this.next != null) {
                return true;
            }
            InputStream input = this.inputStream;
            try {
                if (input != null && input.available() > 0) {
                    int size = Encoders.INTEGER.decode(input);
                    long batchId = Encoders.LONG.decode(input);
                    if (size == 0) {
                        int count = Encoders.INTEGER.decode(input);
                        this.next = new PipeBuffer(batchId, count, false, true);
                        return true;
                    } else {
                        byte[] bytes = new byte[size];
                        int read = input.read(bytes);
                        if (read != bytes.length) {
                            String msg = String.format("illegal read size, expect %d, actual %d",
                                bytes.length, read);
                            throw new GeaflowRuntimeException(msg);
                        }
                        this.next = new PipeBuffer(bytes, batchId, true);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new GeaflowRuntimeException(e);
            }

            return false;
        }

        @Override
        public PipeBuffer next() {
            PipeBuffer buffer = this.next;
            this.next = null;
            return buffer;
        }

        @Override
        public void close() {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException e) {
                    throw new GeaflowRuntimeException(e);
                }
            }
        }
    }

}