/*
 * $Id: ListRolloverController.java,v 1.2 2008/10/14 22:31:44 rah003 Exp $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx.rollover;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JList;
import javax.swing.ListCellRenderer;


/**
     * listens to rollover properties. Repaints effected component regions.
     * Updates link cursor.
     * 
     * @author Jeanette Winzenburg
     */
    public class ListRolloverController<T extends JList> extends
            RolloverController<T> {

        private Cursor oldCursor;

        // --------------------------------- JList rollover

        @Override
        protected void rollover(Point oldLocation, Point newLocation) {
            // PENDING JW - track down the -1 in location.y
            if (oldLocation != null) {
                Rectangle r = component.getCellBounds(oldLocation.y, oldLocation.y);
                // LOG.info("old index/cellbounds: " + index + "/" + r);
                if (r != null) {
                    component.repaint(r);
                }
            }
            if (newLocation != null) {
                Rectangle r = component.getCellBounds(newLocation.y, newLocation.y);
                // LOG.info("new index/cellbounds: " + index + "/" + r);
                if (r != null) {
                    component.repaint(r);
                }
            }
            setRolloverCursor(newLocation);
        }

        /**
         * something weird: cursor in JList behaves different from JTable?
         * Hmm .. no: using the table code snippets seems to fix #503-swingx
         * @param location
         */
        private void setRolloverCursor(Point location) {
            if (hasRollover(location)) {
                if (oldCursor == null) {
                    oldCursor = component.getCursor();
                    component.setCursor(Cursor
                            .getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            } else {
                if (oldCursor != null) {
                    component.setCursor(oldCursor);
                    oldCursor = null;
                }
            }
//            if (hasRollover(location)) {
//                oldCursor = component.getCursor();
//                component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//            } else {
//                component.setCursor(oldCursor);
//                oldCursor = null;
//            }

        }

        @Override
        protected RolloverRenderer getRolloverRenderer(Point location,
                boolean prepare) {
            ListCellRenderer renderer = component.getCellRenderer();
            RolloverRenderer rollover = renderer instanceof RolloverRenderer 
                ? (RolloverRenderer) renderer : null;
            if ((rollover != null) && !rollover.isEnabled()) {
                rollover = null;
            }
            if ((rollover != null) && prepare) {
                Object element = component.getModel().getElementAt(location.y);
                renderer.getListCellRendererComponent(component, element,
                        location.y, false, true);
            }
            return rollover;
        }

        @Override
        protected Point getFocusedCell() {
            int leadRow = component.getLeadSelectionIndex();
            if (leadRow < 0)
                return null;
            return new Point(0, leadRow);
        }

    }