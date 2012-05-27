/*
 * $Id: ForwardingRepaintManager.java,v 1.3 2009/05/29 20:10:53 kschaefe Exp $
 *
 * Copyright 2009 Sun Microsystems, Inc., 4150 Network Circle,
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

import org.jdesktop.swingx.util.Contract;

/**
 * A {@code RepaintManager} that is designed to forward all calls to a contained
 * delegate. This class is designed for extension, such that subclasses should
 * override method as appropriate and allow the original repaint manager to
 * handle the rest of the work.
 * <p>
 * Install a forwarding repaint manager:
 * 
 * <pre>
 * RepaintManager manager = RepaintManager.currentManager(this);
 * RepaintManager frm = new ForwardingRepaintManager(manager);
 * RepaintManager.setCurrentManager(frm);
 * </pre>
 * 
 * @author Karl George Schaefer
 * @author pietblok (original facade/delegate idea)
 */
public class ForwardingRepaintManager extends RepaintManager {
    private RepaintManager delegate;

    /**
     * Creates a new forwarding manager that forwards all calls to the delegate.
     * 
     * @param delegate
     *            the manager backing this {@code ForwardingRepaintManager}
     * @throws NullPointerException
     *             if {@code delegate} is {@code null}
     */
    public ForwardingRepaintManager(RepaintManager delegate) {
        this.delegate = Contract.asNotNull(delegate, "delegate is null");
    }
    
    //TODO in 1.6
//    /**
//     * {@inheritDoc}
//     */
//    public void addDirtyRegion(Applet applet, int x, int y, int w, int h) {
//        delegate.addDirtyRegion(applet, x, y, w, h);
//    }

    /**
     * {@inheritDoc}
     */
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        delegate.addDirtyRegion(c, x, y, w, h);
    }

    //TODO in 1.6
//    /**
//     * {@inheritDoc}
//     */
//    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
//        delegate.addDirtyRegion(window, x, y, w, h);
//    }

    /**
     * {@inheritDoc}
     */
    public void addInvalidComponent(JComponent invalidComponent) {
        delegate.addInvalidComponent(invalidComponent);
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getDirtyRegion(JComponent component) {
        return delegate.getDirtyRegion(component);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getDoubleBufferMaximumSize() {
        return delegate.getDoubleBufferMaximumSize();
    }

    /**
     * {@inheritDoc}
     */
    public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return delegate.getOffscreenBuffer(c, proposedWidth, proposedHeight);
    }

    /**
     * {@inheritDoc}
     */
    public Image getVolatileOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return delegate.getVolatileOffscreenBuffer(c, proposedWidth, proposedHeight);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCompletelyDirty(JComponent component) {
        return delegate.isCompletelyDirty(component);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDoubleBufferingEnabled() {
        return delegate.isDoubleBufferingEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void markCompletelyClean(JComponent component) {
        delegate.markCompletelyClean(component);
    }

    /**
     * {@inheritDoc}
     */
    public void markCompletelyDirty(JComponent component) {
        delegate.markCompletelyDirty(component);
    }

    /**
     * {@inheritDoc}
     */
    public void paintDirtyRegions() {
        delegate.paintDirtyRegions();
    }

    /**
     * {@inheritDoc}
     */
    public void removeInvalidComponent(JComponent component) {
        delegate.removeInvalidComponent(component);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoubleBufferingEnabled(boolean flag) {
        delegate.setDoubleBufferingEnabled(flag);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoubleBufferMaximumSize(Dimension d) {
        delegate.setDoubleBufferMaximumSize(d);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return delegate.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void validateInvalidComponents() {
        delegate.validateInvalidComponents();
    }

    /**
     * Gets the delegate repaint manager backing this forwarding repaint
     * manager.
     * 
     * @return the delegate for this forwarding manager
     */
    public final RepaintManager getDelegateManager() {
        return delegate;
    }
}
