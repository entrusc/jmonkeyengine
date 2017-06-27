/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.bullet.collision;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.EventObject;

/**
 * A CollisionEvent stores all information about a collision in the PhysicsWorld.
 * Do not store this Object, as it will be reused after the collision() method has been called.
 * Get/reference all data you need in the collide method.
 * @author normenhansen
 */
public class PhysicsCollisionEvent extends EventObject {

    public static final int TYPE_ADDED = 0;
    public static final int TYPE_PROCESSED = 1;
    public static final int TYPE_DESTROYED = 2;
    private int type;
    private PhysicsCollisionObject nodeA;
    private PhysicsCollisionObject nodeB;

    private float appliedImpulse, appliedImpulseLateral1, appliedImpulseLateral2, combinedFriction, combinedRestitution, distance1;
    private int index0, index1, lifeTime, partId0, partId1;
    private boolean lateralFrictionInitialized;
    private final Vector3f lateralFrictionDir1 = new Vector3f();
    private final Vector3f lateralFrictionDir2 = new Vector3f();
    private final Vector3f localPointA = new Vector3f();
    private final Vector3f localPointB = new Vector3f();
    private final Vector3f normalWorldOnB = new Vector3f();
    private final Vector3f positionWorldOnA = new Vector3f();
    private final Vector3f positionWorldOnB = new Vector3f();

    public PhysicsCollisionEvent(int type, PhysicsCollisionObject nodeA, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        super(nodeA);
        this.type = type;
        this.nodeA = nodeA;
        this.nodeB = nodeB;

        readData(manifoldPointObjectId);
    }

    private void readData(long manifildPointObjectId) {
        this.appliedImpulse = getAppliedImpulse(manifildPointObjectId);
        this.appliedImpulseLateral1 = getAppliedImpulseLateral1(manifildPointObjectId);
        this.appliedImpulseLateral2 = getAppliedImpulseLateral2(manifildPointObjectId);
        this.combinedFriction = getCombinedFriction(manifildPointObjectId);
        this.combinedRestitution = getCombinedRestitution(manifildPointObjectId);
        this.distance1 = getDistance1(manifildPointObjectId);
        this.index0 = getIndex0(manifildPointObjectId);
        this.index1 = getIndex1(manifildPointObjectId);
        getLateralFrictionDir1(manifildPointObjectId, lateralFrictionDir1);
        getLateralFrictionDir2(manifildPointObjectId, lateralFrictionDir2);
        this.lateralFrictionInitialized = isLateralFrictionInitialized(manifildPointObjectId);
        this.lifeTime = getLifeTime(manifildPointObjectId);
        getLocalPointA(manifildPointObjectId, localPointA);
        getLocalPointB(manifildPointObjectId, localPointB);
        getNormalWorldOnB(manifildPointObjectId, normalWorldOnB);
        this.partId0 = getPartId0(manifildPointObjectId);
        this.partId1 = getPartId1(manifildPointObjectId);
        getPositionWorldOnA(manifildPointObjectId, positionWorldOnA);
        getPositionWorldOnB(manifildPointObjectId, positionWorldOnB);
    }

    /**
     * used by event factory, called when event is destroyed
     */
    public void clean() {
        source = null;
        this.type = 0;
        this.nodeA = null;
        this.nodeB = null;
    }

    /**
     * used by event factory, called when event reused
     */
    public void refactor(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        this.source = source;
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;

        readData(manifoldPointObjectId);
    }

    public int getType() {
        return type;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeA() {
        if (nodeA.getUserObject() instanceof Spatial) {
            return (Spatial) nodeA.getUserObject();
        }
        return null;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeB() {
        if (nodeB.getUserObject() instanceof Spatial) {
            return (Spatial) nodeB.getUserObject();
        }
        return null;
    }

    public PhysicsCollisionObject getObjectA() {
        return nodeA;
    }

    public PhysicsCollisionObject getObjectB() {
        return nodeB;
    }

    public float getAppliedImpulse() {
        return this.appliedImpulse;
    }
    private native float getAppliedImpulse(long manifoldPointObjectId);

    public float getAppliedImpulseLateral1() {
        return this.appliedImpulseLateral1;
    }
    private native float getAppliedImpulseLateral1(long manifoldPointObjectId);

    public float getAppliedImpulseLateral2() {
        return this.appliedImpulseLateral2;
    }
    private native float getAppliedImpulseLateral2(long manifoldPointObjectId);

    public float getCombinedFriction() {
        return this.combinedFriction;
    }
    private native float getCombinedFriction(long manifoldPointObjectId);

    public float getCombinedRestitution() {
        return this.combinedRestitution;
    }
    private native float getCombinedRestitution(long manifoldPointObjectId);

    public float getDistance1() {
        return this.distance1;
    }
    private native float getDistance1(long manifoldPointObjectId);

    public int getIndex0() {
        return this.index0;
    }
    private native int getIndex0(long manifoldPointObjectId);

    public int getIndex1() {
        return this.index1;
    }
    private native int getIndex1(long manifoldPointObjectId);

    public Vector3f getLateralFrictionDir1() {
        return this.lateralFrictionDir1.clone();
    }

    public Vector3f getLateralFrictionDir1(Vector3f lateralFrictionDir1) {
        lateralFrictionDir1.set(this.lateralFrictionDir1);
        return lateralFrictionDir1;
    }
    private native void getLateralFrictionDir1(long manifoldPointObjectId, Vector3f lateralFrictionDir1);

    public Vector3f getLateralFrictionDir2() {
        return this.lateralFrictionDir2.clone();
    }

    public Vector3f getLateralFrictionDir2(Vector3f lateralFrictionDir2) {
        lateralFrictionDir2.set(this.lateralFrictionDir2);
        return lateralFrictionDir2;
    }
    private native void getLateralFrictionDir2(long manifoldPointObjectId, Vector3f lateralFrictionDir2);

    public boolean isLateralFrictionInitialized() {
        return this.lateralFrictionInitialized;
    }
    private native boolean isLateralFrictionInitialized(long manifoldPointObjectId);

    public int getLifeTime() {
        return this.lifeTime;
    }
    private native int getLifeTime(long manifoldPointObjectId);

    public Vector3f getLocalPointA() {
        return this.localPointA.clone();
    }

    public Vector3f getLocalPointA(Vector3f localPointA) {
        localPointA.set(this.localPointA);
        return localPointA;
    }
    private native void getLocalPointA(long manifoldPointObjectId, Vector3f localPointA);

    public Vector3f getLocalPointB() {
        return this.localPointB.clone();
    }

    public Vector3f getLocalPointB(Vector3f localPointB) {
        localPointB.set(this.localPointB);
        return localPointB;
    }
    private native void getLocalPointB(long manifoldPointObjectId, Vector3f localPointB);

    public Vector3f getNormalWorldOnB() {
        return this.normalWorldOnB.clone();
    }

    public Vector3f getNormalWorldOnB(Vector3f normalWorldOnB) {
        normalWorldOnB.set(this.normalWorldOnB);
        return normalWorldOnB;
    }
    private native void getNormalWorldOnB(long manifoldPointObjectId, Vector3f normalWorldOnB);

    public int getPartId0() {
        return this.partId0;
    }
    private native int getPartId0(long manifoldPointObjectId);

    public int getPartId1() {
        return this.partId1;
    }

    private native int getPartId1(long manifoldPointObjectId);

    public Vector3f getPositionWorldOnA() {
        return this.positionWorldOnA.clone();
    }

    public Vector3f getPositionWorldOnA(Vector3f positionWorldOnA) {
        positionWorldOnA.set(this.positionWorldOnA);
        return positionWorldOnA;
    }
    private native void getPositionWorldOnA(long manifoldPointObjectId, Vector3f positionWorldOnA);

    public Vector3f getPositionWorldOnB() {
        return this.positionWorldOnB.clone();
    }

    public Vector3f getPositionWorldOnB(Vector3f positionWorldOnB) {
        positionWorldOnB.set(this.positionWorldOnB);
        return positionWorldOnB;
    }
    private native void getPositionWorldOnB(long manifoldPointObjectId, Vector3f positionWorldOnB);

//    public Object getUserPersistentData() {
//        return userPersistentData;
//    }
}
