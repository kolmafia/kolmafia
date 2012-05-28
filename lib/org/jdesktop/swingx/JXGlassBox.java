/*
 * $Id: JXGlassBox.java,v 1.7 2009/02/01 15:01:00 rah003 Exp $
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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Component used to display transluscent user-interface content.
 * This component and all of its content will be displayed with the specified
 * &quot;alpha&quot; transluscency property value.  When this component is made visible,
 * it's content will fade in until the alpha transluscency level is reached.
 * <p>
 * If the glassbox's &quot;dismissOnClick&quot; property is <code>true</code>
 * (the default) then the glassbox will be made invisible when the user
 * clicks on it.</p>
 * <p>
 * This component is particularly useful for displaying transient messages
 * on the glasspane.</p>
 *
 * @author Amy Fowler
 * @author Karl George Schaefer
 * @version 1.0
 */
public class JXGlassBox extends JXPanel {
    private static final int SHOW_DELAY = 30; // ms
    private static final int TIMER_INCREMENT = 10; // ms

    private float alphaStart = 0.01f;
    private float alphaEnd = 0.8f;

    private Timer animateTimer;
    private float alphaIncrement = 0.02f;

    private boolean dismissOnClick = false;
    private MouseAdapter dismissListener = null;

    public JXGlassBox() {
        setOpaque(false);
        setAlpha(alphaStart);
        setBackground(Color.white);
        setDismissOnClick(true);

        animateTimer = new Timer(TIMER_INCREMENT, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setAlpha(Math.min(alphaEnd, getAlpha() + alphaIncrement));
            }
        });
    }

    public JXGlassBox(float alpha) {
        this();
        setAlpha(alpha);
    }

    @Override
public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.alphaIncrement = (alphaEnd - alphaStart)/(SHOW_DELAY/TIMER_INCREMENT);
    }

    /**
     * Dismisses this glass box. This causes the glass box to be removed from
     * it's parent and ensure that the display is correctly updated.
     */
    public void dismiss() {
        JComponent parent = (JComponent) getParent();
        
        if (parent != null) {
            Container toplevel = parent.getTopLevelAncestor();
            parent.remove(this);
            toplevel.validate();
            toplevel.repaint();
        }
    }

    /**
     * Determines if the glass box if dismissed when a user clicks on it.
     * 
     * @return {@code true} if the glass box can be dismissed with a click;
     *         {@code false} otherwise
     * @see #setDismissOnClick(boolean)
     * @see #dismiss()
     */
    public boolean isDismissOnClick() {
        return dismissOnClick;
    }

    /**
     * Configures the glass box to dismiss (or not) when clicked.
     * 
     * @param dismissOnClick
     *            {@code true} if the glass box should dismiss when clicked;
     *            {@code false} otherwise
     * @see #isDismissOnClick()
     * @see #dismiss()
     */
    public void setDismissOnClick(boolean dismissOnClick) {
        boolean oldDismissOnClick = this.dismissOnClick;
        this.dismissOnClick = dismissOnClick;
        
        firePropertyChange("dismissOnClick", oldDismissOnClick, isDismissOnClick());
        
        //TODO do this as a reaction to the property change?
        if (dismissOnClick && !oldDismissOnClick) {
            if (dismissListener == null) {
                dismissListener = new MouseAdapter() {
                    @Override
		public void mouseClicked(MouseEvent e) {
                        dismiss();
                    }
                };
            }
            addMouseListener(dismissListener);
        }
        else if (!dismissOnClick && oldDismissOnClick) {
            removeMouseListener(dismissListener);
        }
    }

    @Override
public void paint(Graphics g) {
        super.paint(g);
        if (!animateTimer.isRunning() && getAlpha() < alphaEnd ) {
            animateTimer.start();
        }
        if (animateTimer.isRunning() && getAlpha() >= alphaEnd) {
            animateTimer.stop();
        }
    }

    @Override
public void setVisible(boolean visible) {
        boolean old = isVisible();
        setAlpha(alphaStart);
        super.setVisible(visible);
        firePropertyChange("visible", old, isVisible());
    }

    private Container getTopLevel() {
        Container p = getParent();
        while (p != null && !(p instanceof Window || p instanceof Applet)) {
            p = p.getParent();
        }
        return p;
    }

    /**
     * Shows this glass box on the glass pane. The position of the box is
     * relative to the supplied component and offsets.
     * 
     * @param glassPane
     *            the glass pane
     * @param origin
     *            the component representing the origin location
     * @param offsetX
     *            the offset on the X-axis from the origin
     * @param offsetY
     *            the offset on the Y-axis from the origin
     * @param positionHint
     *            a {@code SwingConstants} box position hint ({@code CENTER},
     *            {@code TOP}, {@code BOTTOM}, {@code LEFT}, or {@code RIGHT})
     * @throws NullPointerException
     *             if {@code glassPane} or {@code origin} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code positionHint} is not a valid hint
     */
    //TODO replace SwingConstant with enum other non-int
    //TODO this method places the box outside of the origin component
    // that continues the implementation approach that Amy used.  I
    // think it would be a useful poll to determine whether the box
    // should be place inside or outside of the origin (by default).
    public void showOnGlassPane(Container glassPane, Component origin,
                                int offsetX, int offsetY, int positionHint) {
        Rectangle r = SwingUtilities.convertRectangle(origin, 
                origin.getBounds(), glassPane);
        Dimension d = getPreferredSize();
        
        int originX = offsetX + r.x;
        int originY = offsetY + r.y;
        
        switch (positionHint) {
        case SwingConstants.TOP:
            originX += (r.width - d.width) / 2; 
            originY -= d.height;
            break;
        case SwingConstants.BOTTOM:
            originX += (r.width - d.width) / 2; 
            originY += r.height;
            break;
        case SwingConstants.LEFT:
            originX -= d.width; 
            originY += (r.height - d.height) / 2;
            break;
        case SwingConstants.RIGHT:
            originX += r.width; 
            originY += (r.height - d.height) / 2;
            break;
        case SwingConstants.CENTER:
            originX += (r.width - d.width) / 2; 
            originY += (r.height - d.height) / 2;
            break;
        default:
            throw new IllegalArgumentException("inavlid position hint");
        }
        
        showOnGlassPane(glassPane, originX, originY);
    }

    /**
     * Shows this glass box on the glass pane.
     * 
     * @param glassPane
     *            the glass pane
     * @param originX
     *            the location on the X-axis to position the glass box
     * @param originY
     *            the location on the Y-axis to position the glass box
     */
    public void showOnGlassPane(Container glassPane, int originX, int originY) {
        Dimension gd = glassPane.getSize();
        Dimension bd = getPreferredSize();
        
        int x = Math.min(originX, gd.width - bd.width);
        int y = Math.min(originY, gd.height - bd.height);
        
        if (x < 0) {
            x = 0;
        }
        
        if (y < 0) {
            y = 0;
        }
        
        int width = x + bd.width < gd.width ? bd.width : gd.width;
        int height = y + bd.height < gd.height ? bd.height : gd.height;
        
        glassPane.setLayout(null);
        setBounds(x, y, width, height);
        glassPane.add(this);
        glassPane.setVisible(true);

        Container topLevel = getTopLevel();
        topLevel.validate();
        topLevel.repaint();
    }

}