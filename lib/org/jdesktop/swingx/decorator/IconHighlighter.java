/*
 * $Id: IconHighlighter.java,v 1.3 2008/10/14 22:31:38 rah003 Exp $
 *
 * Copyright 2007 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx.decorator;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * Highlighter which decorates by setting the icon property of a label.
 *  
 * @author Jeanette Winzenburg
 */
public class IconHighlighter extends AbstractHighlighter {

    private Icon icon;

    /**
     * Instantiates a IconHighlighter with null Icon and default
     * HighlightPredicate.
     */
    public IconHighlighter() {
        this((HighlightPredicate) null);
    }
    
    /**
     * Instantiates a IconHighlighter with null Icon the given predicate.
     * 
     * @param predicate the HighlightPredicate to use.
     */
    public IconHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }


    /**
     * Instantiates a IconHighlighter with the specified Icon and default
     * HighlightPredicate.
     * 
     * @param icon the icon to use for decoration.
     */
    public IconHighlighter(Icon icon) {
        this(null, icon);
    }

    
    /**
     * Instantiates a IconHighlighter with the specified Icon and 
     * HighlightPredicate.
     * 
     * @param predicate the HighlightPredicate to use.
     * @param icon the Icon to use for decoration.
     */
    public IconHighlighter(HighlightPredicate predicate, Icon icon) {
        super(predicate);
        setIcon(icon);
    }

    /**
     * Sets the icon to use for decoration. A null icon indicates 
     * to not decorate. <p>
     * 
     * The default value is null.
     * 
     * @param icon the Icon to use for decoration, might be null.
     */
    public void setIcon(Icon icon) {
        if (areEqual(icon, getIcon())) return;
        this.icon = icon;
        fireStateChanged();
    }

    /**
     * Returns the Icon used for decoration.
     * 
     * @return icon the Icon used for decoration.
     * @see #setIcon(Icon)
     */
    public Icon getIcon() {
        return icon;
    }
    
    /**
     * {@inheritDoc}
     * 
     * Implemented to set the component's Icon property, if possible and
     * this Highlighter's icon is not null. Does nothing if the decorating icon is null.
     * @see #canHighlight(Component, ComponentAdapter)
     * @see #setIcon(Icon)
     */
    @Override
    protected Component doHighlight(Component component,
            ComponentAdapter adapter) {
        if (getIcon() != null) {
            ((JLabel) component).setIcon(getIcon());
        }
        return component;
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to return true if the component is of type JLabel, false otherwise.
     */
    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return component instanceof JLabel;
    }

    
}
