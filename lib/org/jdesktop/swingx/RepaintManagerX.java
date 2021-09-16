/*
 * $Id: RepaintManagerX.java,v 1.11 2009/03/10 17:30:14 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
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

package org.jdesktop.swingx;

import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 * <p>An implementation of {@link RepaintManager} which adds support for transparency
 * in {@link JXPanel}s. <code>JXPanel</code> (which supports translucency) will 
 * replace the current RepaintManager with an instance of RepaintManagerX 
 * <em>unless</em> the current RepaintManager is tagged by the {@link TranslucentRepaintManager}
 * annotation.</p>
 *
 * @author zixle
 * @author rbair
 */
@TranslucentRepaintManager
public class RepaintManagerX extends ForwardingRepaintManager {
    /**
     * @param delegate
     */
    public RepaintManagerX(RepaintManager delegate) {
        super(delegate);
    }

    /** 
     * Add a component in the list of components that should be refreshed.
     * If <i>c</i> already has a dirty region, the rectangle <i>(x,y,w,h)</i> 
     * will be unioned with the region that should be redrawn. 
     * 
     * @param c Component to repaint, null results in nothing happening.
     * @param x X coordinate of the region to repaint
     * @param y Y coordinate of the region to repaint
     * @param w Width of the region to repaint
     * @param h Height of the region to repaint
     * @see JComponent#repaint
     */
    @Override
public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        Rectangle dirtyRegion = getDirtyRegion(c);
        if (dirtyRegion.width == 0 && dirtyRegion.height == 0) {
            int lastDeltaX = c.getX();
            int lastDeltaY = c.getY();
            Container parent = c.getParent();
            while (parent instanceof JComponent) {
                if (!parent.isVisible() || !parent.isDisplayable()) {
                    return;
                }
                if (parent instanceof JXPanel && (((JXPanel)parent).getAlpha() < 1f ||
                    !parent.isOpaque())) {
                    x += lastDeltaX;
                    y += lastDeltaY;
                    lastDeltaX = lastDeltaY = 0;
                    c = (JComponent)parent;
                }
                lastDeltaX += parent.getX();
                lastDeltaY += parent.getY();
                parent = parent.getParent();
            }
        }
        super.addDirtyRegion(c, x, y, w, h);
    }
}
