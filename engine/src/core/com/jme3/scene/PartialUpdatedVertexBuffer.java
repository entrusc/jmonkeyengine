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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A vertex buffer that allows for partial updating
 * in comparison to the normal VertxBuffer implementation
 * that only allows to update the whole buffer at once.
 *
 * @author Florian Frankenberger
 */
public class PartialUpdatedVertexBuffer extends VertexBuffer {

    private List<Update> updates = new ArrayList<Update>();
    private ObservedBuffer updateBuffer = null;

    public static class Update {
        private final int pos;
        private final int length;

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
            updates.add(new Update(data.position(), length * (Float.SIZE / 8)));
            fBuffer.put(d, offset, length);
        }

    }

    public class ObservedIntBuffer extends ObservedBuffer {

        public void put(int[] d) {
            this.put(d, 0, d.length);
        }

        public void put(int[] d, int offset, int length) {
            IntBuffer iBuffer = (IntBuffer) data;
            updates.add(new Update(data.position(), length * (Integer.SIZE / 8)));
            iBuffer.put(d, offset, length);
        }

    }

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
    public void updateData(Buffer data) {
        if (this.data != null && !this.data.getClass().equals(data.getClass())) {
            dataSizeChanged = true;
            updates.clear();

            createObservedBuffer(data);
        }
        super.updateData(data);
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

    public List<Update> getAndClearUpdates() {
        final List<Update> result = this.updates;
        this.updates = new ArrayList<Update>();
        return result;
    }

    private void createObservedBuffer(Buffer data1) throws IllegalArgumentException {
        if (data1 instanceof FloatBuffer) {
            this.updateBuffer = new ObservedFloatBuffer();
        } else if (data1 instanceof IntBuffer) {
            this.updateBuffer = new ObservedIntBuffer();
        } else {
            throw new IllegalArgumentException("Unsupported buffer type");
        }
    }



}
