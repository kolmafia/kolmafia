/*
 * $Id: AreaEffect.java,v 1.5 2009/02/01 21:18:40 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.painter.effects;

import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * An effect which works on AbstractPathPainters or any thing else which can provide a shape to be drawn.
 * @author joshy
 */
public interface AreaEffect {
    /*
     * Applies the shape effect. This effect will be drawn on top of the graphics context.
     */
    /**
     * Draws an effect on the specified graphics and path using the specified width and height.
     * @param g 
     * @param clipShape 
     * @param width 
     * @param height 
     */
    public abstract void apply(Graphics2D g, Shape clipShape, int width, int height);
}
