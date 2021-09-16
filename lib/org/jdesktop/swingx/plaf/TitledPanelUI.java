/*
 * $Id: TitledPanelUI.java,v 1.7 2006/04/11 20:51:30 rbair Exp $
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

package org.jdesktop.swingx.plaf;

import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.plaf.PanelUI;

/**
 *
 * @author rbair
 */
public abstract class TitledPanelUI extends PanelUI {
    /**
     * Adds the given JComponent as a decoration on the right of the title
     * @param decoration
     */
    public abstract void setRightDecoration(JComponent decoration);
    public abstract JComponent getRightDecoration();
    
    /**
     * Adds the given JComponent as a decoration on the left of the title
     * @param decoration
     */
    public abstract void setLeftDecoration(JComponent decoration);
    public abstract JComponent getLeftDecoration();
    /**
     * @return the Container acting as the title bar for this component
     */
    public abstract Container getTitleBar();
}
