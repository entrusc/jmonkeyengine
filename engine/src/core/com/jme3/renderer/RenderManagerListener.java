/**
 * Copyright 2015 by moebiusgames.com
 *
 * Be inspired by this source but please don't just copy it ;)
 */
package com.jme3.renderer;

/**
 *
 * @author Florian Frankenberger
 */
public interface RenderManagerListener {

    /**
     * called before any view port is rendered
     *
     * @param tpf
     */
    void preFrame(float tpf);

    /**
     * called after any view port is rendered
     *
     * @param tpf
     */
    void postFrame(float tpf);

}
