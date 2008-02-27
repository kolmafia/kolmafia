/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import java.io.File;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.RefreshSessionListener;
import net.sourceforge.kolmafia.swingui.listener.WorldPeaceListener;
import net.sourceforge.kolmafia.swingui.menu.GlobalMenuBar;
import net.sourceforge.kolmafia.swingui.panel.CompactSidePane;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import tab.CloseTabbedPane;

public abstract class GenericFrame extends JFrame {

	protected HashMap listenerMap;
	private GlobalMenuBar menuBar;

	public JTabbedPane tabs;
	public String lastTitle;
	public String frameName;
	public JPanel framePanel;

	public CompactSidePane sidepane = null;
	public KoLCharacterAdapter refreshListener = null;

	static {
		GenericFrame.compileScripts();
		GenericFrame.compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be
	 * associated with the given StaticEntity.getClient().
	 */

	public GenericFrame() {

		this("");
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be
	 * associated with the given StaticEntity.getClient().
	 */

	public GenericFrame(final String title) {

		this.setTitle(title);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.tabs = this.getTabbedPane();
		this.framePanel = new JPanel(new BorderLayout(0, 0));

		this.frameName = this.getClass().getName();
		this.frameName =
			this.frameName.substring(this.frameName.lastIndexOf(".") + 1);

		if (this.shouldAddStatusBar()) {
			JScrollPane statusBar =
				KoLConstants.commandBuffer.setChatDisplay(new RequestPane());
			JComponentUtilities.setComponentSize(statusBar, new Dimension(
				200, 50));

			JSplitPane doublePane =
				new JSplitPane(
					JSplitPane.VERTICAL_SPLIT, new GenericScrollPane(
						this.framePanel), statusBar);
			this.getContentPane().add(doublePane, BorderLayout.CENTER);

			doublePane.setOneTouchExpandable(true);
			doublePane.setDividerLocation(0.9);
		}
		else {
			this.getContentPane().add(this.framePanel, BorderLayout.CENTER);
		}

		this.addHotKeys();

		boolean shouldAddFrame =
			!(this instanceof KoLDesktop) &&
				!(this instanceof ContactListFrame) &&
				!(this instanceof LoginFrame);

		if (this instanceof ChatFrame) {
			shouldAddFrame =
				!ChatManager.usingTabbedChat() ||
					this instanceof TabbedChatFrame;
		}

		if (shouldAddFrame) {
			StaticEntity.registerFrame(this);
		}
	}

	public void setJMenuBar(final GlobalMenuBar menuBar) {

		this.menuBar = menuBar;
		super.setJMenuBar(menuBar);
	}

	protected void addActionListener(
		final JCheckBox component, final ActionListener listener) {

		if (this.listenerMap == null) {
			this.listenerMap = new HashMap();
		}

		component.addActionListener(listener);
		this.listenerMap.put(component, new WeakReference(listener));
	}

	protected void addActionListener(
		final JComboBox component, final ActionListener listener) {

		if (this.listenerMap == null) {
			this.listenerMap = new HashMap();
		}

		component.addActionListener(listener);
		this.listenerMap.put(component, new WeakReference(listener));
	}

	private void removeActionListener(
		final Object component, final ActionListener listener) {

		if (component instanceof JCheckBox) {
			((JCheckBox) component).removeActionListener(listener);
		}
		if (component instanceof JComboBox) {
			((JComboBox) component).removeActionListener(listener);
		}
	}

	protected void removeThreadedListeners() {

		if (this.listenerMap == null) {
			return;
		}

		Object[] keys = this.listenerMap.keySet().toArray();
		for (int i = 0; i < keys.length; ++i) {
			WeakReference ref = (WeakReference) this.listenerMap.get(keys[i]);
			if (ref == null) {
				continue;
			}

			Object listener = ref.get();
			if (listener == null) {
				continue;
			}

			if (listener instanceof ActionListener) {
				this.removeActionListener(keys[i], (ActionListener) listener);
			}
		}

		this.listenerMap.clear();
		this.listenerMap = null;
	}

	public boolean appearsInTab() {

		return GenericFrame.appearsInTab(this.frameName);
	}

	public static boolean appearsInTab(String frameName) {

		String tabSetting = Preferences.getString("initialDesktop");
		return tabSetting.indexOf(frameName) != -1;
	}

	public boolean shouldAddStatusBar() {

		return Preferences.getBoolean("addStatusBarToFrames") &&
			!this.appearsInTab();
	}

	public JTabbedPane getTabbedPane() {

		return Preferences.getBoolean("useDecoratedTabs")
			? new CloseTabbedPane() : new JTabbedPane();
	}

	public void addHotKeys() {

		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener());
		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_F5, new RefreshSessionListener());

		if (!System.getProperty("os.name").startsWith("Mac")) {
			JComponentUtilities.addGlobalHotKey(
				this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK,
				new TabForwardListener());
			JComponentUtilities.addGlobalHotKey(
				this.getRootPane(), KeyEvent.VK_F6, InputEvent.CTRL_MASK +
					InputEvent.SHIFT_MASK, new TabBackwardListener());
		}
	}

	private class TabForwardListener implements ActionListener {

		public void actionPerformed(final ActionEvent e) {

			if (GenericFrame.this.tabs == null) {
				return;
			}

			GenericFrame.this.tabs.setSelectedIndex((GenericFrame.this.tabs.getSelectedIndex() + 1) %
				GenericFrame.this.tabs.getTabCount());
		}
	}

	private class TabBackwardListener implements ActionListener {

		public void actionPerformed(final ActionEvent e) {

			if (GenericFrame.this.tabs == null) {
				return;
			}

			GenericFrame.this.tabs.setSelectedIndex(GenericFrame.this.tabs.getSelectedIndex() == 0
				? GenericFrame.this.tabs.getTabCount() - 1
				: GenericFrame.this.tabs.getSelectedIndex() - 1);
		}
	}

	public final void addTab(final String name, final JComponent panel) {

		if (this.tabs == null) {
			return;
		}

		this.tabs.setOpaque(true);

		GenericScrollPane scroller = new GenericScrollPane(panel);
		JComponentUtilities.setComponentSize(scroller, 560, 400);
		this.tabs.add(name, scroller);
	}

	public final void setTitle(final String newTitle) {

		this.lastTitle = newTitle;
		KoLDesktop.setTitle(this, newTitle);

		if (this instanceof LoginFrame) {
			super.setTitle(this.lastTitle);
			return;
		}

		String username = KoLCharacter.getUserName();
		if (username.equals("")) {
			username = "Not Logged In";
		}

		super.setTitle(this.lastTitle + " (" + username + ")");
	}

	public void requestFocus() {

	}

	public boolean useSidePane() {

		return false;
	}

	public JToolBar getToolbar() {

		return this.getToolbar(false);
	}

	public JToolBar getToolbar(final boolean force) {

		JToolBar toolbarPanel = null;

		if (force) {
			toolbarPanel = new JToolBar("KoLmafia Toolbar");
			this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
		}
		else {
			switch (Preferences.getInteger("toolbarPosition")) {
			case 1:
				toolbarPanel = new JToolBar("KoLmafia Toolbar");
				this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
				break;

			case 2:
				toolbarPanel = new JToolBar("KoLmafia Toolbar");
				this.getContentPane().add(toolbarPanel, BorderLayout.SOUTH);
				break;

			case 3:
				toolbarPanel =
					new JToolBar("KoLmafia Toolbar", JToolBar.VERTICAL);
				this.getContentPane().add(toolbarPanel, BorderLayout.WEST);
				break;

			case 4:
				toolbarPanel =
					new JToolBar("KoLmafia Toolbar", JToolBar.VERTICAL);
				this.getContentPane().add(toolbarPanel, BorderLayout.EAST);
				break;

			default:

				toolbarPanel = new JToolBar("KoLmafia Toolbar");
				if (this instanceof LoginFrame || this instanceof ChatFrame) {
					this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
					break;
				}
			}
		}

		if (toolbarPanel != null) {
			toolbarPanel.setFloatable(false);
		}

		return toolbarPanel;
	}

	/**
	 * Overrides the default behavior of dispose so that the frames are removed
	 * from the internal list of existing frames. Also allows for automatic
	 * exit.
	 */

	public void dispose() {

		if (this.isVisible()) {
			this.rememberPosition();
		}

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		KoLDesktop.removeTab(this);
		StaticEntity.unregisterFrame(this);
		KoLConstants.existingFrames.remove(this);

		if (this.refreshListener != null) {
			KoLCharacter.removeCharacterListener(this.refreshListener);
		}

		this.removeThreadedListeners();
		this.getRootPane().resetKeyboardActions();

		if (this.menuBar != null) {
			this.menuBar.dispose();
		}

		super.dispose();
		this.checkForLogout();
	}

	private void checkForLogout() {

		if (!KoLConstants.existingFrames.isEmpty() ||
			LoginFrame.instanceExists()) {
			return;
		}

		GenericFrame.createDisplay(LoginFrame.class);
		RequestThread.postRequest(new LogoutRequest());
	}

	public String toString() {

		return this.lastTitle;
	}

	public String getFrameName() {

		return this.frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component. Note
	 * that this method can only be used if the KoLFrame on which it is called
	 * has not yet added any components. If there are any added components, this
	 * method will do nothing.
	 */

	public void addCompactPane() {

		if (this.sidepane != null) {
			return;
		}

		this.sidepane = new CompactSidePane();
		this.sidepane.run();

		this.refreshListener = new KoLCharacterAdapter(this.sidepane);
		KoLCharacter.addCharacterListener(this.refreshListener);

		this.sidepane.setBackground(KoLConstants.ENABLED_COLOR);
		this.getContentPane().add(this.sidepane, BorderLayout.WEST);
	}

	public void setStatusMessage(final String message) {

	}

	public void updateDisplayState(final int displayState) {

		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		switch (displayState) {
		case KoLConstants.ABORT_STATE:
		case KoLConstants.ERROR_STATE:

			if (this.sidepane != null) {
				this.sidepane.setBackground(KoLConstants.ERROR_COLOR);
			}

			this.setEnabled(true);
			break;

		case KoLConstants.ENABLE_STATE:

			if (this.sidepane != null) {
				this.sidepane.setBackground(KoLConstants.ENABLED_COLOR);
			}

			this.setEnabled(true);
			break;

		default:

			if (this.sidepane != null) {
				this.sidepane.setBackground(KoLConstants.DISABLED_COLOR);
			}

			this.setEnabled(false);
			break;
		}
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled() method
	 * does not call the superclass's version.
	 *
	 * @return <code>true</code>
	 */

	public final boolean isEnabled() {

		return true;
	}

	public void setEnabled(final boolean isEnabled) {

	}

	public void processWindowEvent(final WindowEvent e) {

		if (this.isVisible()) {
			this.rememberPosition();
		}

		super.processWindowEvent(e);

		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			if (KoLConstants.existingFrames.contains(this)) {
				KoLConstants.existingFrames.remove(this);
				KoLConstants.removedFrames.add(this);
				this.checkForLogout();
			}
		}
		else if (e.getID() == WindowEvent.WINDOW_ACTIVATED) {
			if (KoLConstants.removedFrames.contains(this)) {
				KoLConstants.removedFrames.remove(this);
				KoLConstants.existingFrames.add(this);
			}

			InputFieldUtilities.setActiveWindow( this );
		}
	}

	public void setVisible(final boolean isVisible) {

		if (isVisible) {
			this.restorePosition();
		}
		else {
			this.rememberPosition();
		}

		super.setVisible(isVisible);

		if (isVisible) {
			super.setExtendedState(Frame.NORMAL);
			super.repaint();
			KoLConstants.removedFrames.remove(this);
		}
	}

	public void pack() {

		if (!(this instanceof ChatFrame)) {
			super.pack();
		}

		if (!this.isVisible()) {
			this.restorePosition();
		}
	}

	private void rememberPosition() {

		Point p = this.getLocation();

		if (this.tabs == null) {
			Preferences.setString(this.frameName, (int) p.getX() + "," +
				(int) p.getY());
		}
		else {
			Preferences.setString(this.frameName, (int) p.getX() + "," +
				(int) p.getY() + "," + this.tabs.getSelectedIndex());
		}
	}

	private void restorePosition() {

		int xLocation = 0;
		int yLocation = 0;

		Dimension screenSize = KoLConstants.TOOLKIT.getScreenSize();
		String position = Preferences.getString(this.frameName);

		if (position == null || position.indexOf(",") == -1) {
			this.setLocationRelativeTo(null);

			if (!(this instanceof OptionsFrame) && this.tabs != null &&
				this.tabs.getTabCount() > 0) {
				this.tabs.setSelectedIndex(0);
			}

			return;
		}

		String[] location = position.split(",");
		xLocation = StringUtilities.parseInt(location[0]);
		yLocation = StringUtilities.parseInt(location[1]);

		if (xLocation > 0 && yLocation > 0 &&
			xLocation < screenSize.getWidth() &&
			yLocation < screenSize.getHeight()) {
			this.setLocation(xLocation, yLocation);
		}
		else {
			this.setLocationRelativeTo(null);
		}

		if (location.length > 2 && this.tabs != null) {
			int tabIndex = StringUtilities.parseInt(location[2]);

			if (tabIndex >= 0 && tabIndex < this.tabs.getTabCount()) {
				this.tabs.setSelectedIndex(tabIndex);
			}
			else if (this.tabs.getTabCount() > 0) {
				this.tabs.setSelectedIndex(0);
			}
		}
	}

	/**
	 * Utility class used to forward events to JButtons enclosed inside of a
	 * JTable object.
	 */

	public class ButtonEventListener extends MouseAdapter {

		private final JTable table;

		public ButtonEventListener(final JTable table) {

			this.table = table;
		}

		public void mouseReleased(final MouseEvent e) {

			TableColumnModel columnModel = this.table.getColumnModel();

			int row = e.getY() / this.table.getRowHeight();
			int column = columnModel.getColumnIndexAtX(e.getX());

			if (row >= 0 && row < this.table.getRowCount() && column >= 0 &&
				column < this.table.getColumnCount()) {
				Object value = this.table.getValueAt(row, column);

				if (value instanceof JButton) {
					MouseEvent event =
						SwingUtilities.convertMouseEvent(
							this.table, e, (JButton) value);
					((JButton) value).dispatchEvent(event);
					this.table.repaint();
				}
			}
		}
	}

	public abstract class ListWrapperTableModel extends DefaultTableModel
		implements ListDataListener {

		private final String[] headers;
		private final Class[] types;
		private final boolean[] editable;

		protected LockableListModel listModel;

		public ListWrapperTableModel(
			final String[] headers, final Class[] types,
			final boolean[] editable, final LockableListModel listModel) {

			super(0, headers.length);

			this.headers = headers;
			this.types = types;
			this.editable = editable;

			for (int i = 0; i < listModel.size(); ++i) {
				this.insertRow(i, this.constructVector(listModel.get(i)));
			}

			this.listModel = listModel;
			listModel.addListDataListener(this);
		}

		public String getColumnName(final int index) {

			return index < 0 || index >= this.headers.length
				? "" : this.headers[index];
		}

		public Class getColumnClass(final int column) {

			return column < 0 || column >= this.types.length
				? Object.class : this.types[column];
		}

		public abstract Vector constructVector(Object o);

		public boolean isCellEditable(final int row, final int column) {

			return column < 0 || column >= this.editable.length
				? false : this.editable[column];
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param e
		 *            the <code>ListDataEvent</code> that triggered this
		 *            function call
		 */

		public void intervalAdded(final ListDataEvent e) {

			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			for (int i = index0; i <= index1; ++i) {
				this.insertRow(i, this.constructVector(source.get(i)));
			}
		}

		/**
		 * Called whenever contents have been removed from the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param e
		 *            the <code>ListDataEvent</code> that triggered this
		 *            function call
		 */

		public void intervalRemoved(final ListDataEvent e) {

			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			for (int i = index1; i >= index0; --i) {
				this.removeRow(i);
			}
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param e
		 *            the <code>ListDataEvent</code> that triggered this
		 *            function call
		 */

		public void contentsChanged(final ListDataEvent e) {

			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();
			int index1 = e.getIndex1();

			if (index0 < 0 || index1 < 0) {
				return;
			}

			int rowCount = this.getRowCount();

			for (int i = index1; i >= index0; --i) {
				if (source.size() < i) {
					this.removeRow(i);
				}
				else if (i > rowCount) {
					this.insertRow(
						rowCount, this.constructVector(source.get(i)));
				}
				else {
					this.removeRow(i);
					this.insertRow(i, this.constructVector(source.get(i)));
				}
			}
		}
	}

	public class TransparentTable extends JTable {

		public TransparentTable(final TableModel t) {

			super(t);
		}

		public Component prepareRenderer(
			final TableCellRenderer renderer, final int row, final int column) {

			Component c = super.prepareRenderer(renderer, row, column);
			if (c instanceof JComponent) {
				((JComponent) c).setOpaque(false);
			}

			return c;
		}

		public void changeSelection(
			final int row, final int column, final boolean toggle,
			final boolean extend) {

			super.changeSelection(row, column, toggle, extend);

			if (this.editCellAt(row, column)) {
				if (this.getEditorComponent() instanceof JTextField) {
					((JTextField) this.getEditorComponent()).selectAll();
				}
			}
		}
	}

	public class IntegerRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(
			final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column) {

			Component c =
				super.getTableCellRendererComponent(
					table, value, false, hasFocus, row, column);
			if (!(value instanceof Integer)) {
				return c;
			}

			((JLabel) c).setHorizontalAlignment(JLabel.RIGHT);
			((JLabel) c).setText(KoLConstants.COMMA_FORMAT.format(((Integer) value).intValue()));
			return c;
		}
	}

	public class ButtonRenderer implements TableCellRenderer {

		public Component getTableCellRendererComponent(
			final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column) {

			JPanel panel = new JPanel();
			panel.setOpaque(false);

			JComponentUtilities.setComponentSize((JButton) value, 20, 20);
			panel.add((JButton) value);

			return panel;
		}
	}

	public boolean finalizeTable(final JTable table) {

		if (table.isEditing()) {
			int row = table.getEditingRow();
			int col = table.getEditingColumn();
			table.getCellEditor(row, col).stopCellEditing();

			if (table.isEditing()) {
				InputFieldUtilities.alert("One or more fields contain invalid values. (Note: they are currently outlined in red)");
				return false;
			}
		}

		return true;
	}

	public static final void createDisplay(final Class frameClass) {

		KoLmafiaGUI.constructFrame(frameClass);
	}

	public static final void createDisplay(
		final Class frameClass, final Object[] parameters) {

		CreateFrameRunnable creator =
			new CreateFrameRunnable(frameClass, parameters);
		creator.run();
	}

	public static final void compileScripts() {

		KoLConstants.scripts.clear();

		// Get the list of files in the current directory

		File[] scriptList =
			DataUtilities.listFiles(KoLConstants.SCRIPT_LOCATION);

		// Iterate through the files. Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		int directoryIndex = 0;

		for (int i = 0; i < scriptList.length; ++i) {
			if (!GlobalMenuBar.shouldAddScript(scriptList[i])) {
			}
			else if (scriptList[i].isDirectory()) {
				KoLConstants.scripts.add(directoryIndex++, scriptList[i]);
			}
			else {
				KoLConstants.scripts.add(scriptList[i]);
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings file.
	 * This should be called after every update.
	 */

	public static final void saveBookmarks() {

		StringBuffer bookmarkData = new StringBuffer();

		for (int i = 0; i < KoLConstants.bookmarks.getSize(); ++i) {
			if (i > 0) {
				bookmarkData.append('|');
			}
			bookmarkData.append((String) KoLConstants.bookmarks.getElementAt(i));
		}

		Preferences.setString("browserBookmarks", bookmarkData.toString());
	}

	/**
	 * Utility method to compile the list of bookmarks based on the current
	 * settings.
	 */

	public static final void compileBookmarks() {

		KoLConstants.bookmarks.clear();
		String[] bookmarkData =
			Preferences.getString("browserBookmarks").split("\\|");

		if (bookmarkData.length > 1) {
			for (int i = 0; i < bookmarkData.length; ++i) {
				KoLConstants.bookmarks.add(bookmarkData[i] + "|" +
					bookmarkData[++i] + "|" + bookmarkData[++i]);
			}
		}
	}

	protected class LabelColorChanger extends JLabel implements MouseListener {

		protected String property;

		public LabelColorChanger(final String property) {

			this.property = property;
			this.setOpaque(true);
			this.addMouseListener(this);
		}

		public void mousePressed(final MouseEvent e) {

			Color c =
				JColorChooser.showDialog(
					null, "Choose a color:", this.getBackground());
			if (c == null) {
				return;
			}

			Preferences.setString(this.property, DataUtilities.toHexString(c));
			this.setBackground(c);
			this.applyChanges();
		}

		public void mouseReleased(final MouseEvent e) {

		}

		public void mouseClicked(final MouseEvent e) {

		}

		public void mouseEntered(final MouseEvent e) {

		}

		public void mouseExited(final MouseEvent e) {

		}

		public void applyChanges() {

		}
	}
}
