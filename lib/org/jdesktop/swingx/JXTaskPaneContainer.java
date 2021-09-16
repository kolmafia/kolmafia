/*
 * $Id: JXTaskPaneContainer.java,v 1.10 2008/08/22 18:24:44 kschaefe Exp $
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

import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

import javax.swing.JViewport;
import javax.swing.Scrollable;

import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.TaskPaneContainerAddon;
import org.jdesktop.swingx.plaf.TaskPaneContainerUI;

/**
 * <code>JXTaskPaneContainer</code> provides an elegant view
 * to display a list of tasks ordered by groups ({@link org.jdesktop.swingx.JXTaskPane}s).
 * 
 * <p>
 * Although {@link org.jdesktop.swingx.JXTaskPane} can be added to any other
 * container, the <code>JXTaskPaneContainer</code> will provide better
 * fidelity when it comes to matching the look and feel of the host operating
 * system than any other panel. As example, when using on a Windows platform,
 * the <code>JXTaskPaneContainer</code> will be painted with a light gradient
 * background. Also <code>JXTaskPaneContainer</code> takes care of using the
 * right {@link java.awt.LayoutManager} (as required by
 * {@link org.jdesktop.swingx.JXCollapsiblePane}) so that
 * {@link org.jdesktop.swingx.JXTaskPane} behaves correctly when collapsing and
 * expanding its content.
 *  
 * <p>
 * <code>JXTaskPaneContainer<code> can be added to a JScrollPane.
 * 
 * <p>
 * Example:
 * <pre>
 * <code>
 * JXFrame frame = new JXFrame();
 * 
 * // a container to put all JXTaskPane together
 * JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
 * 
 * // add JXTaskPanes to the container
 * JXTaskPane actionPane = createActionPane();
 * JXTaskPane miscActionPane = createMiscActionPane();
 * JXTaskPane detailsPane = createDetailsPane();
 * taskPaneContainer.add(actionPane);
 * taskPaneContainer.add(miscActionPane);
 * taskPaneContainer.add(detailsPane);
 *
 * // put the action list on the left in a JScrollPane
 * // as we have several taskPane and we want to make sure they
 * // all get visible.   
 * frame.add(new JScrollPane(taskPaneContainer), BorderLayout.EAST);
 * 
 * // and a file browser in the middle
 * frame.add(fileBrowser, BorderLayout.CENTER);
 * 
 * frame.pack().
 * frame.setVisible(true);
 * </code>
 * </pre>
 *
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 * 
 * @javabean.attribute
 *          name="isContainer"
 *          value="Boolean.TRUE"
 *          rtexpr="true"
 * 
 * @javabean.class
 *          name="JXTaskPaneContainer"
 *          shortDescription="A component that contains JTaskPaneGroups."
 *          stopClass="java.awt.Component"
 * 
 * @javabean.icons
 *          mono16="JXTaskPaneContainer16-mono.gif"
 *          color16="JXTaskPaneContainer16.gif"
 *          mono32="JXTaskPaneContainer32-mono.gif"
 *          color32="JXTaskPaneContainer32.gif"
 */
public class JXTaskPaneContainer extends JXPanel {

  public final static String uiClassID = "swingx/TaskPaneContainerUI";
  
  // ensure at least the default ui is registered
  static {
    LookAndFeelAddons.contribute(new TaskPaneContainerAddon());
  }

  /**
   * Creates a new empty taskpane.
   */
  public JXTaskPaneContainer() {
      super(null);
    updateUI();
    
    addContainerListener(new ContainerAdapter() {
        @Override
	public void componentRemoved(ContainerEvent e) {
            repaint();
        }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
public TaskPaneContainerUI getUI() {
      return (TaskPaneContainerUI) super.getUI();
  }
  
  /**
   * Notification from the <code>UIManager</code> that the L&F has changed.
   * Replaces the current UI object with the latest version from the <code>UIManager</code>.
   * 
   * @see javax.swing.JComponent#updateUI
   */
  @Override
public void updateUI() {
    setUI((TaskPaneContainerUI)LookAndFeelAddons.getUI(this, TaskPaneContainerUI.class));
  }

  /**
   * Sets the L&F object that renders this component.
   * 
   * @param ui the <code>TaskPaneContainerUI</code> L&F object
   * @see javax.swing.UIDefaults#getUI
   * 
   * @beaninfo bound: true hidden: true description: The UI object that
   * implements the taskpane's LookAndFeel.
   */
  public void setUI(TaskPaneContainerUI ui) {
    super.setUI(ui);
  }

  /**
   * Returns the name of the L&F class that renders this component.
   * 
   * @return the string {@link #uiClassID}
   * @see javax.swing.JComponent#getUIClassID
   * @see javax.swing.UIDefaults#getUI
   */
  @Override
public String getUIClassID() {
    return uiClassID;
  }

  /**
   * Adds a <code>JXTaskPane</code> to this JXTaskPaneContainer.
   * 
   * @param group
   */
  public void add(JXTaskPane group) {
    super.add(group);
  }

  /**
   * Removes a <code>JXTaskPane</code> from this JXTaskPaneContainer.
   * 
   * @param group
   */
  public void remove(JXTaskPane group) {
    super.remove(group);
  }

  /**
   * @see Scrollable#getScrollableTracksViewportHeight()
   */
  @Override
public boolean getScrollableTracksViewportHeight() {
    if (getParent() instanceof JViewport) {
      return (((JViewport)getParent()).getHeight() > getPreferredSize().height);
    } else {
      return false;
    }
  }
  
  /**
   * @see Scrollable#getScrollableTracksViewportWidth()
   */
  @Override
public boolean getScrollableTracksViewportWidth() {
    return true;
  }
}
