package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import java.io.File;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Map;
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

import apple.dts.samplecode.osxadapter.OSXAdapter;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

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
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class GenericFrame
	extends JFrame
	implements Runnable, FocusListener
{
	private static int existingFrameCount = 0;
	private boolean packedOnce = false;
	private boolean exists = true;

	private Map<JComponent, WeakReference<ActionListener>> listenerMap;
	private final GlobalMenuBar menuBar;

	private final FramePanel framePanel;

	public JTabbedPane tabs;
	protected String lastTitle;
	protected String frameName;

	public CompactSidePane sidepane = null;
	public CharacterListener refreshListener = null;

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
			KoLGUIConstants.existingFrames.add( this.getFrameName() );
		}

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.framePanel ) );

		this.addFocusListener( this );

		OSXAdapter.setWindowCanFullScreen( this, true );
	}

	public void focusGained( FocusEvent e )
	{
		this.framePanel.requestFocus();
	}

	public void focusLost( FocusEvent e )
	{
	}

	public void setCenterComponent( Component c )
	{
		this.framePanel.add( c, BorderLayout.CENTER );
	}

	public Component getCenterComponent()
	{
		return this.framePanel.centerComponent;
	}

	public void removeCenterComponent()
	{
		this.framePanel.remove( this.framePanel.centerComponent );
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
			this.listenerMap = new HashMap<>();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference<>( listener ) );
	}

	protected void addActionListener( final JComboBox<?> component, final ActionListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap<>();
		}

		component.addActionListener( listener );
		this.listenerMap.put( component, new WeakReference<>( listener ) );
	}

	protected void removeActionListener( final JComponent component, final ActionListener listener )
	{
		if ( component instanceof JCheckBox )
		{
			( (JCheckBox) component ).removeActionListener( listener );
		}
		if ( component instanceof JComboBox )
		{
			( (JComboBox<?>) component ).removeActionListener( listener );
		}
	}

	public boolean appearsInTab()
	{
		return GenericFrame.appearsInTab( this.frameName );
	}

	public static boolean appearsInTab( String frameName )
	{
		String tabSetting = Preferences.getString( "initialDesktop" );
		return tabSetting.contains( frameName );
	}

	public JTabbedPane getTabbedPane()
	{
		return KoLmafiaGUI.getTabbedPane();
	}

	public void addHotKeys()
	{
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener() );
		JComponentUtilities.addGlobalHotKey( this.getRootPane(), KeyEvent.VK_F5, new RefreshSessionListener() );

		int platform_META_DOWN= (  System.getProperty( "os.name" ).startsWith( "Mac" ) )
					? KeyEvent.META_DOWN_MASK
					: KeyEvent.CTRL_DOWN_MASK;
		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(),
			KeyEvent.VK_W,
			platform_META_DOWN, // CMD on MacOS, CTRL on others...
			new CloseWindowListener() );
		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_PAGE_UP, InputEvent.CTRL_DOWN_MASK, new TabForwardListener() );
		JComponentUtilities.addGlobalHotKey(
			this.getRootPane(), KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK, new TabBackwardListener() );

	}

	public void removeHotKeys()
	{
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ) );
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 ) );
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_W, KeyEvent.META_DOWN_MASK ) );
		this.getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK ) );

		this.getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_UP, InputEvent.CTRL_DOWN_MASK ) );
		this.getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK ) );
	}

	private class CloseWindowListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			dispose();
		}
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

	@Override
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
		JToolBar toolbarPanel;

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

	@Override
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
			KoLGUIConstants.existingFrames.remove( this.getFrameName() );
		}

		// Remove listeners from interface elements

		this.removeHotKeys();

		if ( this.listenerMap != null )
		{
			for ( Entry<JComponent, WeakReference<ActionListener>> entry : this.listenerMap.entrySet() )
			{
				JComponent component = entry.getKey();
				WeakReference<ActionListener> reference = entry.getValue();
				ActionListener listener = reference.get();

				if ( listener != null )
				{
					this.removeActionListener( component, listener );
				}
			}
		}

		if ( this.refreshListener != null )
		{
			CharacterListenerRegistry.removeCharacterListener( this.refreshListener );
		}

		this.menuBar.dispose();

		super.dispose();
		this.checkForLogout();
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
		if ( StaticEntity.isGUIRequired() && !GenericFrame.instanceExists() )
		{
			RequestThread.runInParallel( new LogoutRunnable() );
		}
	}

	@Override
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
		Color sidePaneColor = (KoLmafiaGUI.isDarkTheme() )
				      ? KoLGUIConstants.ENABLED_COLOR_DARK
				      : KoLGUIConstants.ENABLED_COLOR;
		if ( this.sidepane != null )
		{
			return;
		}

		this.sidepane = new CompactSidePane();
		this.sidepane.run();

		this.refreshListener = new CharacterListener( this.sidepane );
		CharacterListenerRegistry.addCharacterListener( this.refreshListener );

		this.sidepane.setBackground( sidePaneColor );


		this.framePanel.add( this.sidepane, BorderLayout.WEST );
	}

	public void addScriptPane()
	{
		int scriptButtonPosition = Preferences.getInteger( "scriptButtonPosition" );
		String scriptList = Preferences.getString( "scriptList" ).trim();

		// Scripts are added to a new panel on right
		// You are allowed to have zero buttons in it.
		ScriptBar scriptBar = new ScriptBar( scriptList, scriptButtonPosition );
		this.framePanel.add( scriptBar, BorderLayout.EAST );
	}

	private class ScriptBar
		extends JToolBar
		implements Listener
	{
		private String scriptList = "";
		private boolean showScriptList = false;

		public ScriptBar( final String scriptList, final int scriptButtonPosition )
		{
			super( SwingConstants.VERTICAL );
			this.setFloatable( false );

			PreferenceListenerRegistry.registerPreferenceListener( "scriptList", this );
			PreferenceListenerRegistry.registerPreferenceListener( "scriptButtonPosition", this );

			this.update( scriptList, scriptButtonPosition );
		}

		public void update()
		{
			this.update( Preferences.getString( "scriptList" ), Preferences.getInteger( "scriptButtonPosition" ) );
		}

		public void update( String scriptList, final int scriptButtonPosition )
		{
			scriptList = scriptList.trim();

			if ( scriptButtonPosition == 0 )
			{
				// We are not showing script buttons
				if ( !this.scriptList.equals( "" ) )
				{
					this.removeAll();
					this.revalidate();
					this.repaint();
				}
				this.scriptList = scriptList;
				this.showScriptList = false;
				return;
			}

			// We are showing scripts. If we previously were and
			// they have not changed, nothing to do.
			if ( this.showScriptList && this.scriptList.equals( scriptList ) )
			{
				return;
			}

			// We are showing scripts when we were not before, or
			// scripts have changed. Update buttons.

			// Remove all current script buttons
			this.removeAll();

			// Create new script buttons for current script list
			String[] scripts = scriptList.split( " +\\| +" );
			int index = 1;
			for ( String script : scripts )
			{
				if ( !script.equals( "" ) )
				{
					this.add( new LoadScriptButton( index++, script ) );
				}
			}

			this.revalidate();
			this.repaint();

			// Save current scriptList
			this.showScriptList = true;
			this.scriptList = scriptList;
		}
	}

	public void setStatusMessage( final String message )
	{
	}

	public void updateDisplayState( final MafiaState displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		Color color;
		boolean enabled;

		switch ( displayState )
		{
		case ABORT:
		case ERROR:
			color =  (KoLmafiaGUI.isDarkTheme() )
				 ? KoLGUIConstants.ERROR_COLOR_DARK
				 : KoLGUIConstants.ERROR_COLOR ;
			enabled = true;
			break;

		case ENABLE:
			color =  (KoLmafiaGUI.isDarkTheme() )
				 ? KoLGUIConstants.ENABLED_COLOR_DARK
				 : KoLGUIConstants.ENABLED_COLOR ;
			enabled = true;
			break;

		default:
			color =  (KoLmafiaGUI.isDarkTheme() )
				 ? KoLGUIConstants.DISABLED_COLOR_DARK
				 : KoLGUIConstants.DISABLED_COLOR;
			enabled = false;
			break;
		}

		if ( this.sidepane != null )
		{
			this.sidepane.setBackground( color );
		}

		this.setEnabled( enabled );
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled() method does not call the superclass's version.
	 *
	 * @return <code>true</code>
	 */

	@Override
	public final boolean isEnabled()
	{
		return true;
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
	}

	@Override
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

	@Override
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

		if ( !isVisible )
		{
			super.setVisible( false );
			return;
		}

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

	public void run()
	{
		super.setVisible( true );
		super.setExtendedState( Frame.NORMAL );
		super.repaint();
	}

	@Override
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
		int xLocation;
		int yLocation;

		Rectangle display = new Rectangle();
		GraphicsEnvironment ge =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for ( GraphicsDevice gd : gs )
		{
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for ( GraphicsConfiguration graphicsConfiguration : gc )
			{
				display = display.union( graphicsConfiguration.getBounds() );
			}
		}

		String position = Preferences.getString( this.frameName );

		if ( position == null || !position.contains( "," ) )
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

		if ( xLocation > display.x && yLocation > display.y && xLocation < ( display.x + display.width ) && yLocation < ( display.y + display.height ) )
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

	public static void createDisplay( final Class<?> frameClass )
	{
		KoLmafiaGUI.constructFrame( frameClass );
	}

	public static final void createDisplay( final Class<?> frameClass, final Object[] parameters )
	{
		CreateFrameRunnable creator = new CreateFrameRunnable( frameClass, parameters );
		creator.run();
	}

	public static final void compileScripts()
	{
		GenericFrame.compileScripts( Preferences.getInteger( "scriptMRULength" ) > 0 );
	}

	public static final void compileScripts( final boolean useMRUlist )
	{
		KoLConstants.scripts.clear();

		// Get the list of files in the current directory or build from MRU

		File [] scriptList = useMRUlist ?
			KoLConstants.scriptMList.listAsFiles() :
			DataUtilities.listFiles( KoLConstants.SCRIPT_LOCATION );

		// Iterate through the files. Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		int directoryIndex = 0;

		for ( File file : scriptList )
		{
			if ( !ScriptMenu.shouldAddScript( file ) )
			{
			}
			else if ( file.isDirectory() )
			{
				KoLConstants.scripts.add( directoryIndex++, file );
			}
			else
			{
				KoLConstants.scripts.add( file );
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings file. This should be called after every
	 * update.
	 */

	public static final void saveBookmarks()
	{
		StringBuilder bookmarkData = new StringBuilder();

		for ( int i = 0; i < LockableListFactory.getsize( KoLConstants.bookmarks ); ++i )
		{
			if ( i > 0 )
			{
				bookmarkData.append( '|' );
			}
			bookmarkData.append( LockableListFactory.getElementAt( KoLConstants.bookmarks, i ) );
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
		implements FocusListener
	{
		private Component centerComponent;

		public FramePanel()
		{
			super( new BorderLayout() );
			this.addFocusListener( this );
		}

		@Override
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

		public void focusGained( FocusEvent e )
		{
			if ( this.centerComponent != null )
			{
				this.centerComponent.requestFocus();
			}
		}

		public void focusLost( FocusEvent e )
		{
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
