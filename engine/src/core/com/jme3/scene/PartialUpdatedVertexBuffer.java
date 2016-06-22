/*
 * Copyright (c) 2009-2016 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A vertex buffer that allows for partial updating
 * in comparison to the normal VertxBuffer implementation
 * that only allows to update the whole buffer at once.
 *
 * @author Florian Frankenberger
 */
public class PartialUpdatedVertexBuffer extends VertexBuffer {

    private final List<Update> updates = new LinkedList<Update>();
    private final NavigableMap<Integer, Update> updatesMap = new TreeMap<Integer, Update>();
    private volatile ObservedBuffer updateBuffer = null;

    private volatile int maxElementsUpdatePerFrame = Integer.MAX_VALUE;
    private volatile boolean optimizeUpdates = true;

    public PartialUpdatedVertexBuffer(Type type) {
        super(type);
    }

    @Override
    public void setupData(Usage usage, int components, Format format, Buffer data) {
        super.setupData(usage, components, format, data);

        //if this is called a second time the parent call will already rise an exception
        createObservedBuffer(this.data);
    }

    @Override
    public synchronized void updateData(Buffer data) {
        if (this.data != null && !this.data.getClass().equals(data.getClass())) {
            dataSizeChanged = true;

            //clear old updates
            updates.clear();
            updatesMap.clear();

            //enqueue the whole buffer again (we can't be sure that the GPU's memory range
            //is just enlarged)
            int pos = 0;
            while (pos < data.capacity()) {
                final int length = Math.min(data.capacity() - pos, maxElementsUpdatePerFrame);
                if (length > 0) {
                    final Update update = new Update(pos, length);
                    this.updates.add(update);
                    this.updatesMap.put(update.pos, update);
                }
                pos += length;
            }

            createObservedBuffer(data);
        }
        super.updateData(data);
    }

    public int getMaxElementsUpdatePerFrame() {
        return maxElementsUpdatePerFrame;
    }

    /**
     * sets the amount of updated elements per frame. This is an estimate
     * as element updates are aggregated.
     *
     * @param maxElementsUpdatePerFrame
     */
    public void setMaxElementUpdatesPerFrame(int maxElementsUpdatePerFrame) {
        this.maxElementsUpdatePerFrame = maxElementsUpdatePerFrame;
    }

    public boolean isOptimizeUpdates() {
        return optimizeUpdates;
    }

    /**
     * sets whether this vertex buffer should optimize
     * updates automatically by joining updates with
     * neighboring updates if they touch or overlap.
     *
     * @param optimizeUpdates
     */
    public void setOptimizeUpdates(boolean optimizeUpdates) {
        this.optimizeUpdates = optimizeUpdates;
    }

    /**
     * returns a buffer-like object that is under observation
     * by this vertex buffer. Any change to this buffer is also
     * propagated down to this vertex buffer's internal data buffer,
     * but in addition the changes are noted and then transfered
     * to the GPU without the need to transfer the entire buffer
     * to the GPU.
     *
     * @return
     */
    public ObservedBuffer getUpdateBuffer() {
        return updateBuffer;
    }

    @Override
    public boolean isUpdateNeeded() {
        return hasUpdates() || super.isUpdateNeeded();
    }

    public synchronized boolean hasUpdates() {
        return !this.updates.isEmpty();
    }

    public synchronized Update getNextUpdate() {
        final Update update = this.updates.remove(0);
        this.updatesMap.remove(update.pos);
        return update;
    }

    private synchronized void createObservedBuffer(Buffer buffer) throws IllegalArgumentException {
        if (buffer instanceof FloatBuffer) {
            this.updateBuffer = new ObservedFloatBuffer();
        } else if (buffer instanceof IntBuffer) {
            this.updateBuffer = new ObservedIntBuffer();
        } else if (buffer instanceof ShortBuffer) {
            this.updateBuffer = new ObservedShortBuffer();
        } else if (buffer instanceof ByteBuffer) {
            this.updateBuffer = new ObservedByteBuffer();
        } else if (buffer instanceof DoubleBuffer) {
            this.updateBuffer = new ObservedDoubleBuffer();
        } else {
            throw new IllegalArgumentException("Unsupported buffer type");
        }
    }

    private synchronized void addUpdate(Update update) {
        if (optimizeUpdates) {
            do {
                //overlapping exactly?
                Update otherUpdate = this.updatesMap.get(update.pos);
                if (otherUpdate != null) {
                    if (otherUpdate.length < update.length) {
                        otherUpdate.length = update.length;
                    }

                    this.updatesMap.remove(otherUpdate.pos);
                    this.updates.remove(otherUpdate);
                    update = otherUpdate;

                    continue; //the other update also updates the region of this update!
                }

                //overlapping with lower entry?
                Map.Entry<Integer, Update> entry = this.updatesMap.floorEntry(update.pos);
                if (entry != null) {
                    otherUpdate = entry.getValue();
                    if (otherUpdate.pos + otherUpdate.length >= update.pos) {
                        if (otherUpdate.pos + otherUpdate.length < update.pos + update.length) {
                            otherUpdate.length = (update.pos + update.length - otherUpdate.pos);
                        } //otherwise this update is already included in the previous update

                        this.updatesMap.remove(otherUpdate.pos);
                        this.updates.remove(otherUpdate);
                        update = otherUpdate;

                        continue;
                    }
                }

                //overlapping with higher entry?
                entry = this.updatesMap.ceilingEntry(update.pos);
                if (entry != null) {
                    otherUpdate = entry.getValue();
                    if (update.pos + update.length >= otherUpdate.pos) {
                        if (otherUpdate.pos + otherUpdate.length > update.pos + update.length) {
                            otherUpdate.length = (otherUpdate.pos + otherUpdate.length - update.pos);
                        } else {
                            otherUpdate.length = update.length;
                        }
                        this.updatesMap.remove(otherUpdate.pos);
                        this.updates.remove(otherUpdate);
                        otherUpdate.pos = update.pos;

                        update = otherUpdate;
                        continue;
                    }
                }

                //not overlapping at all
                this.updates.add(update);
                this.updatesMap.put(update.pos, update);
                update = null;
            } while (update != null);
        } else {
            this.updates.add(update);
        }
    }

    public static class Update {
        private int pos;
        private int length;

        public Update(int pos, int length) {
            this.pos = pos;
            this.length = length;
        }

        public int getPos() {
            return pos;
        }

        public int getLength() {
            return length;
        }
    }

    public abstract class ObservedBuffer {

        public void rewind() {
            data.rewind();
        }

        public int position() {
            return data.position();
        }

        public void position(int pos) {
            data.position(pos);
        }

    }

    public class ObservedFloatBuffer extends ObservedBuffer {

        public void put(float[] d) {
            this.put(d, 0, d.length);
        }

        public void put(float[] d, int offset, int length) {
            FloatBuffer fBuffer = (FloatBuffer) data;
            addUpdate(new Update(fBuffer.position(), length));
            fBuffer.put(d, offset, length);
        }

    }

    public class ObservedIntBuffer extends ObservedBuffer {

        public void put(int[] d) {
            this.put(d, 0, d.length);
        }

        public void put(int[] d, int offset, int length) {
            IntBuffer iBuffer = (IntBuffer) data;
            addUpdate(new Update(iBuffer.position(), length));
            iBuffer.put(d, offset, length);
        }

    }

    public class ObservedShortBuffer extends ObservedBuffer {

        public void put(short[] d) {
            this.put(d, 0, d.length);
        }

        public void put(short[] d, int offset, int length) {
            ShortBuffer sBuffer = (ShortBuffer) data;
            addUpdate(new Update(sBuffer.position(), length));
            sBuffer.put(d, offset, length);
        }

    }

    public class ObservedByteBuffer extends ObservedBuffer {

        public void put(byte[] d) {
            this.put(d, 0, d.length);
        }

        public void put(byte[] d, int offset, int length) {
            ByteBuffer bBuffer = (ByteBuffer) data;
            addUpdate(new Update(bBuffer.position(), length));
            bBuffer.put(d, offset, length);
        }

    }

    public class ObservedDoubleBuffer extends ObservedBuffer {

        public void put(double[] d) {
            this.put(d, 0, d.length);
        }

        public void put(double[] d, int offset, int length) {
            DoubleBuffer dBuffer = (DoubleBuffer) data;
            addUpdate(new Update(dBuffer.position(), length));
            dBuffer.put(d, offset, length);
        }

    }




}
