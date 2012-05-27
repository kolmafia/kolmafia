/*
 * $Id: JXRootPane.java,v 1.20 2009/05/22 19:25:39 kschaefe Exp $
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * Extends the JRootPane by supporting specific placements for a toolbar and a
 * status bar. If a status bar exists, then toolbars, menus will be registered 
 * with the status bar.
 * 
 * @see JXStatusBar
 * @author Mark Davidson
 */
public class JXRootPane extends JRootPane {
    /**
     * An extended {@code RootLayout} offering support for managing the status
     * bar.
     * 
     * @author Karl George Schaefer
     * @author Jeanette Winzenberg
     */
    protected class XRootLayout extends RootLayout {

        LayoutManager2 delegate;

        /**
         * The layout manager backing this manager. The delegate is used to
         * calculate the size when the UI handles the window decorations.
         * 
         * @param delegate
         *            the backing manager
         */
        public void setLayoutManager(LayoutManager2 delegate) {
            this.delegate = delegate;
        }

        private Dimension delegatePreferredLayoutSize(Container parent) {
            if (delegate == null)
                return super.preferredLayoutSize(parent);
            return delegate.preferredLayoutSize(parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension pref = delegatePreferredLayoutSize(parent);
            if (statusBar != null && statusBar.isVisible()) {
                Dimension statusPref = statusBar.getPreferredSize();
                pref.width = Math.max(pref.width, statusPref.width);
                pref.height += statusPref.height;
            }
            return pref;
        }

        private Dimension delegateMinimumLayoutSize(Container parent) {
            if (delegate == null)
                return super.minimumLayoutSize(parent);
            return delegate.minimumLayoutSize(parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Dimension pref = delegateMinimumLayoutSize(parent);
            if (statusBar != null && statusBar.isVisible()) {
                Dimension statusPref = statusBar.getMinimumSize();
                pref.width = Math.max(pref.width, statusPref.width);
                pref.height += statusPref.height;
            }
            return pref;

        }

        private Dimension delegateMaximumLayoutSize(Container parent) {
            if (delegate == null)

                return super.maximumLayoutSize(parent);
            return delegate.maximumLayoutSize(parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension maximumLayoutSize(Container target) {
            Dimension pref = delegateMaximumLayoutSize(target);
            if (statusBar != null && statusBar.isVisible()) {
                Dimension statusPref = statusBar.getMaximumSize();
                pref.width = Math.max(pref.width, statusPref.width);
                // PENDING JW: overflow?
                pref.height += statusPref.height;
            }
            return pref;
        }

        private void delegateLayoutContainer(Container parent) {
            if (delegate == null) {
                super.layoutContainer(parent);
            } else {
                delegate.layoutContainer(parent);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layoutContainer(Container parent) {
            delegateLayoutContainer(parent);
            if (statusBar == null || !statusBar.isVisible())
                return;
            Rectangle b = parent.getBounds();
            Insets i = getInsets();
            int w = b.width - i.right - i.left;
            int h = b.height - i.top - i.bottom;
            Dimension statusPref = statusBar.getPreferredSize();
            statusBar.setBounds(i.right, b.height - i.bottom
                    - statusPref.height, w, statusPref.height);
            if (contentPane != null) {
                Rectangle bounds = contentPane.getBounds();
                contentPane.setBounds(bounds.x, bounds.y, bounds.width,
                        bounds.height - statusPref.height);
            }

        }
    }
    
    /**
     * The current status bar for this root pane.
     */
    protected JXStatusBar statusBar;

    private JToolBar toolBar;

    /** 
     * The button that gets activated when the pane has the focus and
     * a UI-specific action like pressing the <b>ESC</b> key occurs.
     */
    private JButton cancelButton;

    /**
     * Creates an extended root pane.
     */
    public JXRootPane() {
        installKeyboardActions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container createContentPane() {
        JComponent c = new JXPanel() {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void addImpl(Component comp, Object constraints, int index) {
                synchronized (getTreeLock()) {
                    super.addImpl(comp, constraints, index);
                    registerStatusBar(comp);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove(int index) {
                synchronized (getTreeLock()) {
                    unregisterStatusBar(getComponent(index));
                    super.remove(index);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void removeAll() {
                synchronized (getTreeLock()) {
                    for (Component c : getComponents()) {
                        unregisterStatusBar(c);
                    }
                    
                    super.removeAll();
                }
            }
        };
        c.setName(this.getName()+".contentPane");
        c.setLayout(new BorderLayout() {
            /* This BorderLayout subclass maps a null constraint to CENTER.
             * Although the reference BorderLayout also does this, some VMs
             * throw an IllegalArgumentException.
             */
            @Override
            public void addLayoutComponent(Component comp, Object constraints) {
                if (constraints == null) {
                    constraints = BorderLayout.CENTER;
                }
                super.addLayoutComponent(comp, constraints);
            }
        });
        return c;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setLayout(LayoutManager layout) {
        if (layout instanceof XRootLayout) {
            // happens if decoration is uninstalled by ui
            if ((layout != null) && (layout == getLayout())) {
                ((XRootLayout) layout).setLayoutManager(null);
            }
            super.setLayout(layout);
        } else {
            if (layout instanceof LayoutManager2) {
                ((XRootLayout) getLayout()).setLayoutManager((LayoutManager2) layout);
                if (!isValid()) {
                    invalidate();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutManager createRootLayout() {
        return new XRootLayout();
    } 

    /**
     * PENDING: move to UI
     * 
     */
    private void installKeyboardActions() {
        Action escAction = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                JButton cancelButton = getCancelButton();
                if (cancelButton != null) {
                    cancelButton.doClick(20);
                }
            }
            
            /**
             * Overridden to hack around #566-swing: 
             * JXRootPane eats escape keystrokes from datepicker popup.
             * Disable action if there is no cancel button.<p>
             * 
             * That's basically what RootPaneUI does - only not in 
             * the parameterless isEnabled, but in the one that passes
             * in the sender (available in UIAction only). We can't test 
             * nor compare against core behaviour, UIAction has
             * sun package scope. <p>
             * 
             * 
             */
            @Override
            public boolean isEnabled() {
                return (cancelButton != null) && (cancelButton.isEnabled());
            }
        };
        getActionMap().put("esc-action", escAction);
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(key, "esc-action");
    }
    
    private void registerStatusBar(Component comp) {
        if (statusBar == null || comp == null) {
            return;
        }
//        if (comp instanceof MessageSource) {
//            MessageSource source = (MessageSource) comp;
//            source.addMessageListener(statusBar);
//        }
//        if (comp instanceof ProgressSource) {
//            ProgressSource source = (ProgressSource) comp;
//            source.addProgressListener(statusBar);
//        }
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++) {
                registerStatusBar(comps[i]);
            }
        }
    }

    private void unregisterStatusBar(Component comp) {
        if (statusBar == null || comp == null) {
            return;
        }
//        if (comp instanceof MessageSource) {
//            MessageSource source = (MessageSource) comp;
//            source.removeMessageListener(statusBar);
//        }
//        if (comp instanceof ProgressSource) {
//            ProgressSource source = (ProgressSource) comp;
//            source.removeProgressListener(statusBar);
//        }
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++) {
                unregisterStatusBar(comps[i]);
            }
        }
    }

    /**
     * Set the status bar for this root pane. Any components held by this root
     * pane will be registered. If this is replacing an existing status bar then
     * the existing component will be unregistered from the old status bar.
     * 
     * @param statusBar
     *            the status bar to use
     */
    public void setStatusBar(JXStatusBar statusBar) {
        JXStatusBar oldStatusBar = this.statusBar;
        this.statusBar = statusBar;

//        if (statusBar != null) {
//            if (handler == null) {
                // Create the new mouse handler and register the toolbar
                // and menu components.
//                handler = new MouseMessagingHandler(this, statusBar);
//                if (toolBar != null) {
//                    handler.registerListeners(toolBar.getComponents());
//                }
//                if (menuBar != null) {
//                    handler.registerListeners(menuBar.getSubElements());
//                }
//            } else {
//                handler.setMessageListener(statusBar);
//            }
//        }

        Component[] comps = getContentPane().getComponents();
        for (int i = 0; i < comps.length; i++) {
            // Unregister the old status bar.
            unregisterStatusBar(comps[i]);

            // register the new status bar.
            registerStatusBar(comps[i]);
        }
        if (oldStatusBar != null) {
            remove(oldStatusBar);
        }
        if (statusBar != null) {
            add(statusBar);
        }
        firePropertyChange("statusBar", oldStatusBar, getStatusBar());
    }

    /**
     * Gets the currently installed status bar.
     * 
     * @return the current status bar
     */
    public JXStatusBar getStatusBar() {
        return statusBar;
    }

//    private MouseMessagingHandler handler;

    /**
     * Set the toolbar bar for this root pane. If a tool bar is currently registered with this
     * {@code JXRootPane}, then it is removed prior to setting the new tool
     * bar. If an implementation needs to handle more than one tool bar, a
     * subclass will need to override the singleton logic used here or manually
     * add toolbars with {@code getContentPane().add}.
     * 
     * @param toolBar
     *            the toolbar to register
     */
    public void setToolBar(JToolBar toolBar) {
        JToolBar oldToolBar = getToolBar();
        this.toolBar = toolBar;

        if (oldToolBar != null) {
            getContentPane().remove(oldToolBar);
            
//            if (handler != null) {
//                handler.unregisterListeners(oldToolBar.getComponents());
//            }
        }
        
//        if (handler != null && this.toolBar != null) {
//            handler.registerListeners(this.toolBar.getComponents());
//        }

        getContentPane().add(BorderLayout.NORTH, this.toolBar);
        
        //ensure the new toolbar is correctly sized and displayed
        getContentPane().validate();
        
        firePropertyChange("toolBar", oldToolBar, getToolBar());
    }

    /**
     * The currently installed tool bar.
     * 
     * @return the current tool bar
     */
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJMenuBar(JMenuBar menuBar) {
        JMenuBar oldMenuBar = this.menuBar;

        super.setJMenuBar(menuBar);

//        if (handler != null && oldMenuBar != null) {
//            handler.unregisterListeners(oldMenuBar.getSubElements());
//        }
//
//        if (handler != null && menuBar != null) {
//            handler.registerListeners(menuBar.getSubElements());
//        }
    }

    /**
     * Sets the <code>cancelButton</code> property,
     * which determines the current default cancel button for this <code>JRootPane</code>.
     * The cancel button is the button which will be activated 
     * when a UI-defined activation event (typically the <b>ESC</b> key) 
     * occurs in the root pane regardless of whether or not the button 
     * has keyboard focus (unless there is another component within 
     * the root pane which consumes the activation event,
     * such as a <code>JTextPane</code>).
     * For default activation to work, the button must be an enabled
     * descendant of the root pane when activation occurs.
     * To remove a cancel button from this root pane, set this
     * property to <code>null</code>.
     *
     * @param cancelButton the <code>JButton</code> which is to be the cancel button
     * @see #getCancelButton() 
     *
     * @beaninfo
     *  description: The button activated by default for cancel actions in this root pane
     */
    public void setCancelButton(JButton cancelButton) { 
        JButton old = this.cancelButton;

        if (old != cancelButton) {
            this.cancelButton = cancelButton;

            if (old != null) {
                old.repaint();
            }
            if (cancelButton != null) {
                cancelButton.repaint();
            } 
        }

        firePropertyChange("cancelButton", old, cancelButton);        
    }

    /**
     * Returns the value of the <code>cancelButton</code> property. 
     * @return the <code>JButton</code> which is currently the default cancel button
     * @see #setCancelButton
     */
    public JButton getCancelButton() { 
        return cancelButton;
    }

}
