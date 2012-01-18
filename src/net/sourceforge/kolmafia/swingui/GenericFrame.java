/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import java.io.File;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.LogoutManager;
import net.sourceforge.kolmafia.swingui.button.LoadScriptButton;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.RefreshSessionListener;
import net.sourceforge.kolmafia.swingui.listener.WorldPeaceListener;
import net.sourceforge.kolmafia.swingui.menu.GlobalMenuBar;
import net.sourceforge.kolmafia.swingui.menu.ScriptMenu;
import net.sourceforge.kolmafia.swingui.panel.CompactSidePane;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class GenericFrame
	extends JFrame
	implements Runnable
{
	private static int existingFrameCount = 0;
	private boolean packedOnce = false;
	private boolean exists = true;

	private HashMap listenerMap;
	private GlobalMenuBar menuBar;

	private FramePanel framePanel;

	public JTabbedPane tabs;
	protected String lastTitle;
	protected String frameName;

	public CompactSidePane sidepane = null;
	public KoLCharacterAdapter refreshListener = null;

	static
	{
		GenericFrame.compileScripts();
		GenericFrame.compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public GenericFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public GenericFrame( final String title )
	{
		this.setTitle( title );
		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

		this.tabs = this.getTabbedPane();
		this.framePanel = new FramePanel();

		this.frameName = this.getClass().getName();
		this.frameName = this.frameName.substring( this.frameName.lastIndexOf( "." ) + 1 );

		if ( this.shouldAddStatusBar() )
		{
			JScrollPane statusBar = KoLConstants.commandBuffer.addDisplay( new RequestPane() );
			JComponentUtilities.setComponentSize( statusBar, new Dimension( 200, 50 ) );

			JSplitPane doublePane =
				new JSplitPane( JSplitPane.VERTICAL_SPLIT, new GenericScrollPane( framePanel ), statusBar );

			doublePane.setOneTouchExpandable( true );
			doublePane.setDividerLocation( 0.9 );

			JPanel wrappedDoublePane = new JPanel( new BorderLayout( 0, 0 ) );
			wrappedDoublePane.add( doublePane, BorderLayout.CENTER );

			this.setContentPane( wrappedDoublePane );
		}
		else
		{
			this.setContentPane( framePanel );
		}

		this.menuBar = new GlobalMenuBar();
		this.setJMenuBar( this.menuBar );
		this.addHotKeys();

		++GenericFrame.existingFrameCount;

		if ( this.showInWindowMenu() )
		{
			KoLConstants.existingFrames.add( this.getFrameName() );
		}

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.framePanel ) );
	}

	public void setCenterComponent( Component c )
	{
		this.framePanel.add( c, BorderLayout.CENTER );
	}

	public Component getCenterComponent()
	{
		return this.framePanel.centerComponent;
	}

	public JPanel getFramePanel()
	{
		return this.framePanel;
	}

	public boolean shouldAddStatusBar()
	{
		return Preferences.getBoolean( "addStatusBarToFrames" ) && !this.appearsInTab();
	}

	public boolean showInWindowMenu()
	{
		return true;
	}

	protected void addActionListener( final JCheckBox component, final ActionListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void addActionListener( final JComboBox component, final ActionListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void removeActionListener( final JComponent component, final ActionListener listener )
	{
		if ( component instanceof JCheckBox )
		{
			( (JCheckBox) component ).removeActionListener( listener );
		}
		if ( component instanceof JComboBox )
		{
			( (JComboBox) component ).removeActionListener( listener );
		}
	}

	public boolean appearsInTab()
	{
		return GenericFrame.appearsInTab( this.frameName );
	}

	public static boolean appearsInTab( String frameName )
	{
		String tabSetting = Preferences.getString( "initialDesktop" );
		return tabSetting.indexOf( frameName ) != -1;
	}

	public JTabbedPane getTabbedPane()
	{
		return KoLmafiaGUI.getTabbedPane();
	}

	public void addHotKeys()
	{
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener() );
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F5, new RefreshSessionListener() );

		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK, new TabForwardListener() );
		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK, new TabBackwardListener() );
	}

	public void removeHotKeys()
	{
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ) );
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 ) );

		this.getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK ) );
		this.getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK ) );
	}

	private class TabForwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( GenericFrame.this.tabs == null )
			{
				return;
			}

			GenericFrame.this.tabs.setSelectedIndex( ( GenericFrame.this.tabs.getSelectedIndex() + 1 ) % GenericFrame.this.tabs.getTabCount() );
		}
	}

	private class TabBackwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( GenericFrame.this.tabs == null )
			{
				return;
			}

			GenericFrame.this.tabs.setSelectedIndex( GenericFrame.this.tabs.getSelectedIndex() == 0 ? GenericFrame.this.tabs.getTabCount() - 1 : GenericFrame.this.tabs.getSelectedIndex() - 1 );
		}
	}

	public final void addTab( final String name, final JComponent panel )
	{
		if ( this.tabs == null )
		{
			return;
		}

		this.tabs.setOpaque( true );

		GenericScrollPane scroller = new GenericScrollPane( panel );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		this.tabs.add( name, scroller );
	}

	public final void setTitle( final String newTitle )
	{
		this.lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		if ( this instanceof LoginFrame )
		{
			super.setTitle( this.lastTitle );
			return;
		}

		String username = KoLCharacter.getUserName();
		if ( username.equals( "" ) )
		{
			username = "Not Logged In";
		}

		super.setTitle( this.lastTitle + " (" + username + ")" );
	}

	public boolean useSidePane()
	{
		return false;
	}

	public JToolBar getToolbar()
	{
		return this.getToolbar( false );
	}

	public JToolBar getToolbar( final boolean force )
	{
		JToolBar toolbarPanel = null;

		if ( force )
		{
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.framePanel.add( toolbarPanel, BorderLayout.NORTH );
			toolbarPanel.setFloatable( false );
			return toolbarPanel;
		}

		switch ( Preferences.getInteger( "toolbarPosition" ) )
		{
		case 1:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.framePanel.add( toolbarPanel, BorderLayout.NORTH );
			break;

		case 2:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			this.framePanel.add( toolbarPanel, BorderLayout.SOUTH );
			break;

		case 3:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
			this.framePanel.add( toolbarPanel, BorderLayout.WEST );
			break;

		case 4:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
			this.framePanel.add( toolbarPanel, BorderLayout.EAST );
			break;

		default:

			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			if ( this instanceof LoginFrame || this instanceof ChatFrame )
			{
				this.framePanel.add( toolbarPanel, BorderLayout.NORTH );
				break;
			}
		}

		if ( toolbarPanel != null )
		{
			toolbarPanel.setFloatable( false );
		}

		return toolbarPanel;
	}

	/**
	 * Overrides the default behavior of dispose so that the frames are removed from the internal list of existing
	 * frames. Also allows for automatic exit.
	 */

	public void dispose()
	{
		StaticEntity.unregisterPanels( this );

		if ( this.isVisible() )
		{
			this.rememberPosition();
			this.setVisible( false );
		}

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		if ( this.exists )
		{
			this.exists = false;
			--GenericFrame.existingFrameCount;

			KoLConstants.existingFrames.remove( this.getFrameName() );
			this.checkForLogout();
		}

		// Remove listeners from interface elements

		this.removeHotKeys();

		if ( this.listenerMap != null )
		{
			Object[] entries = this.listenerMap.entrySet().toArray();

			for ( int i = 0; i < entries.length; ++i )
			{
				Entry entry = (Entry) entries[ i ];

				JComponent component = (JComponent) entry.getKey();

				WeakReference reference = (WeakReference) entry.getValue();

				ActionListener listener = (ActionListener) reference.get();

				if ( listener != null )
				{
					this.removeActionListener( component, listener );
				}
			}
		}

		if ( this.refreshListener != null )
		{
			KoLCharacter.removeCharacterListener( this.refreshListener );
		}

		this.menuBar.dispose();

		super.dispose();
	}

	public boolean exists()
	{
		return this.exists;
	}

	public static boolean instanceExists()
	{
		return GenericFrame.existingFrameCount != 0;
	}

	protected void checkForLogout()
	{
		if ( StaticEntity.getClient() instanceof KoLmafiaGUI && !GenericFrame.instanceExists() )
		{
			RequestThread.runInParallel( new LogoutRunnable() );
		}
	}

	public String toString()
	{
		return this.lastTitle;
	}

	public String getLastTitle()
	{
		return this.lastTitle;
	}

	public String getFrameName()
	{
		return this.frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component. Note that this method can only be used if the
	 * KoLFrame on which it is called has not yet added any components. If there are any added components, this method
	 * will do nothing.
	 */

	public void addCompactPane()
	{
		if ( this.sidepane != null )
		{
			return;
		}

		this.sidepane = new CompactSidePane();
		this.sidepane.run();

		this.refreshListener = new KoLCharacterAdapter( this.sidepane );
		KoLCharacter.addCharacterListener( this.refreshListener );

		this.sidepane.setBackground( KoLConstants.ENABLED_COLOR );
		this.framePanel.add( this.sidepane, BorderLayout.WEST );
	}

	public void addScriptPane()
	{
		int scriptButtons = Preferences.getInteger( "scriptButtonPosition" );
		String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );

		if ( scriptButtons == 0 || scriptList.length == 0 )
		{
			return;
		}

		JToolBar scriptBar;

		if ( scriptButtons == 1 )
		{
			scriptBar = this.getToolbar();
		}
		else
		{
			scriptBar = new JToolBar( SwingConstants.VERTICAL );
			scriptBar.setFloatable( false );
		}

		for ( int i = 0; i < scriptList.length; ++i )
		{
			scriptBar.add( new LoadScriptButton( i + 1, scriptList[ i ] ) );
		}

		if ( scriptButtons == 2 )
		{
			this.framePanel.add( scriptBar, BorderLayout.EAST );
		}
	}

	public void setStatusMessage( final String message )
	{
	}

	public void updateDisplayState( final int displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		switch ( displayState )
		{
		case KoLConstants.ABORT_STATE:
		case KoLConstants.ERROR_STATE:

			if ( this.sidepane != null )
			{
				this.sidepane.setBackground( KoLConstants.ERROR_COLOR );
			}

			this.setEnabled( true );
			break;

		case KoLConstants.ENABLE_STATE:

			if ( this.sidepane != null )
			{
				this.sidepane.setBackground( KoLConstants.ENABLED_COLOR );
			}

			this.setEnabled( true );
			break;

		default:

			if ( this.sidepane != null )
			{
				this.sidepane.setBackground( KoLConstants.DISABLED_COLOR );
			}

			this.setEnabled( false );
			break;
		}
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled() method does not call the superclass's version.
	 *
	 * @return <code>true</code>
	 */

	public final boolean isEnabled()
	{
		return true;
	}

	public void setEnabled( final boolean isEnabled )
	{
	}

	public void processWindowEvent( final WindowEvent e )
	{
		if ( this.isVisible() && e.getID() == WindowEvent.WINDOW_CLOSING )
		{
			this.rememberPosition();
		}

		super.processWindowEvent( e );

		if ( e.getID() == WindowEvent.WINDOW_ACTIVATED )
		{
			InputFieldUtilities.setActiveWindow( this );
		}
	}

	public void setVisible( final boolean isVisible )
	{
		if ( isVisible )
		{
			this.restorePosition();
		}
		else
		{
			this.rememberPosition();
		}

		if ( isVisible )
		{
			try
			{
				if ( SwingUtilities.isEventDispatchThread() )
				{
					this.run();
				}
				else
				{
					SwingUtilities.invokeAndWait( this );
				}
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}
		else
		{
			super.setVisible( false );
		}
	}

	public void run()
	{
		super.setVisible( true );
		super.setExtendedState( Frame.NORMAL );
		super.repaint();
	}

	public void pack()
	{
		if ( !( this instanceof ChatFrame ) && !packedOnce )
		{
			super.pack();
			packedOnce = true;
		}
	}

	private void rememberPosition()
	{
		Point p = this.getLocation();

		if ( this.tabs == null )
		{
			Preferences.setString( this.frameName, (int) p.getX() + "," + (int) p.getY() );
		}
		else
		{
			Preferences.setString(
				this.frameName, (int) p.getX() + "," + (int) p.getY() + "," + this.tabs.getSelectedIndex() );
		}
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;

		Dimension screenSize = KoLConstants.TOOLKIT.getScreenSize();
		String position = Preferences.getString( this.frameName );

		if ( position == null || position.indexOf( "," ) == -1 )
		{
			this.setLocationRelativeTo( null );

			if ( !( this instanceof OptionsFrame ) && !( this instanceof KoLDesktop ) && this.tabs != null && this.tabs.getTabCount() > 0 )
			{
				this.tabs.setSelectedIndex( 0 );
			}

			return;
		}

		String[] location = position.split( "," );
		xLocation = StringUtilities.parseInt( location[ 0 ] );
		yLocation = StringUtilities.parseInt( location[ 1 ] );

		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
		{
			this.setLocation( xLocation, yLocation );
		}
		else
		{
			this.setLocationRelativeTo( null );
		}

		if ( location.length > 2 && this.tabs != null )
		{
			int tabIndex = StringUtilities.parseInt( location[ 2 ] );

			if ( tabIndex >= 0 && tabIndex < this.tabs.getTabCount() )
			{
				this.tabs.setSelectedIndex( tabIndex );
			}
			else if ( this.tabs.getTabCount() > 0 )
			{
				this.tabs.setSelectedIndex( 0 );
			}
		}
	}

	public static final void createDisplay( final Class frameClass )
	{
		KoLmafiaGUI.constructFrame( frameClass );
	}

	public static final void createDisplay( final Class frameClass, final Object[] parameters )
	{
		CreateFrameRunnable creator = new CreateFrameRunnable( frameClass, parameters );
		creator.run();
	}

	public static final void compileScripts()
	{
		KoLConstants.scripts.clear();

		// Get the list of files in the current directory or build from MRU
		File[] scriptList = null;
		if ( Preferences.getInteger( "scriptMRULength" ) <= 0 )
		{
			scriptList = DataUtilities.listFiles( KoLConstants.SCRIPT_LOCATION );
		}
		else
		{
			scriptList = KoLConstants.scriptMList.listAsFiles();
		}

		// Iterate through the files. Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		int directoryIndex = 0;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( !ScriptMenu.shouldAddScript( scriptList[ i ] ) )
			{
			}
			else if ( scriptList[ i ].isDirectory() )
			{
				KoLConstants.scripts.add( directoryIndex++ , scriptList[ i ] );
			}
			else
			{
				KoLConstants.scripts.add( scriptList[ i ] );
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings file. This should be called after every
	 * update.
	 */

	public static final void saveBookmarks()
	{
		StringBuffer bookmarkData = new StringBuffer();

		for ( int i = 0; i < KoLConstants.bookmarks.getSize(); ++i )
		{
			if ( i > 0 )
			{
				bookmarkData.append( '|' );
			}
			bookmarkData.append( (String) KoLConstants.bookmarks.getElementAt( i ) );
		}

		Preferences.setString( "browserBookmarks", bookmarkData.toString() );
	}

	/**
	 * Utility method to compile the list of bookmarks based on the current settings.
	 */

	public static final void compileBookmarks()
	{
		KoLConstants.bookmarks.clear();
		String[] bookmarkData = Preferences.getString( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
		{
			for ( int i = 0; i < bookmarkData.length; ++i )
			{
				KoLConstants.bookmarks.add( bookmarkData[ i ] + "|" + bookmarkData[ ++i ] + "|" + bookmarkData[ ++i ] );
			}
		}
	}

	private static class FramePanel
		extends JPanel
	{
		private Component centerComponent;

		public FramePanel()
		{
			super( new BorderLayout() );
		}

		public void add( Component c, Object constraint )
		{
			super.add( c, constraint );

			if ( constraint == BorderLayout.CENTER )
			{
				this.centerComponent = c;

				this.setFocusCycleRoot( true );
				this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( c ) );
			}
		}
	}

	private static class LogoutRunnable
		implements Runnable
	{
		public void run()
		{
			LogoutManager.logout();
		}
	}
}
