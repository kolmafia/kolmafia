/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

// containers
import javax.swing.JDialog;
import javax.swing.JToolBar;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JEditorPane;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

// layout
import java.awt.Point;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.table.TableColumnModel;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import javax.swing.ListSelectionModel;

// basic utilities
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

// other stuff
import javax.swing.SwingUtilities;
import edu.stanford.ejalbert.BrowserLauncher;

// spellcast imports
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame implements KoLConstants
{
	protected static KoLmafia client;

	static
	{
		// Ensure that images are properly loaded
		// and the appropriate HTML rendering engine
		// is installed to the frames.

		System.setProperty( "SHARED_MODULE_DIRECTORY", "net/sourceforge/kolmafia/" );
		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );
	};

	protected static LockableListModel scripts = null;
	protected static int addedScriptCount = 0;

	static
	{
		// Load the scripts statically, rather than
		// inside of the addScriptMenu() call.

		File scriptDirectory = new File( "scripts" );

		if ( !scriptDirectory.exists() )
			scriptDirectory.mkdirs();

		// Add all the scripts from this directory
		scripts = addScripts( scriptDirectory, "scripts" );
	}

	private static LockableListModel addScripts( File directory, String prefix )
	{
		// Make a list to store menu items in
		LockableListModel list = new LockableListModel();

		// Get the list of files in the current directory
		File [] scriptList = directory.listFiles();

		// Iterate through the files
		for ( int i = 0; i < scriptList.length; ++i )
		{
			JComponent item = addScriptFile( scriptList[i], prefix );
			if ( item != null)
				// Add the menu item to the list
				list.add( item );
		}

		return list;
	}

	private static JComponent addScriptFile( File file, String prefix )
	{
		// Get path components of this file
		String [] pieces;

		try
		{
			pieces = file.getCanonicalPath().split( "[\\\\/]" );
		}
		catch ( Exception e )
		{
			return null;
		}

		// There must be at least a file name
		if ( pieces.length < 1 )
			return null;

		String name = pieces[ pieces.length - 1 ];
		String path = prefix + File.separator + name;

		if ( file.isDirectory() )
		{
			// Get a list of all the files
			LockableListModel list = addScripts( file, path );

			//  Convert the list into a menu
			JMenu menu = new JMenu( name );
			for ( int i = 0; i < list.size(); ++i )
				menu.add( (JComponent) list.get(i) );

			// Return the menu
			return menu;
		}

		return new LoadScriptMenuItem( (++addedScriptCount) + "	 " + name, path );
	}

	protected static LockableListModel bookmarks = new LockableListModel();
	protected JMenu bookmarkMenu;

	private static final String [] LICENSE_FILENAME = { "kolmafia-license.gif", "spellcast-license.gif", "browserlauncher-license.htm", "sungraphics-license.txt" };
	private static final String [] LICENSE_NAME = { "KoLmafia BSD", "Spellcast BSD", "BrowserLauncher", "Sun Graphics" };

	private String frameName;
	protected boolean isEnabled;

	protected JPanel framePanel;
	protected JToolBar toolbarPanel;
	protected KoLPanel contentPanel;

	protected JPanel compactPane;
	protected JLabel hpLabel, mpLabel, advLabel;
	protected JLabel meatLabel, drunkLabel;
	protected JLabel familiarLabel, weightLabel;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given client.
	 */

	protected KoLFrame( KoLmafia client, String title )
	{
		super( VERSION_NAME + ": " + title );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		this.isEnabled = true;
		KoLFrame.client = client;

		this.framePanel = new JPanel();
		getContentPane().setLayout( new BorderLayout( 0, 0 ) );
		getContentPane().add( this.framePanel, BorderLayout.CENTER );

		this.toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
		getContentPane().add( toolbarPanel, BorderLayout.NORTH );

		this.frameName = getClass().getName();
		this.frameName = frameName.substring( frameName.lastIndexOf( "." ) + 1 );

		this.existingFrames.add( this );

		// All frames will have to access the same menu bar for consistency.
		// Later on, all menu items not added by default will be placed onto
		// the panel for increased visibility.

		addToolBar();  addMenuBar();
	}

	public void dispose()
	{
		super.dispose();

		Object [] frames = existingFrames.toArray();

		for ( int i = frames.length - 1; i >= 0; --i )
			if ( frames[i] == this )
				existingFrames.remove(i);

		if ( existingFrames.isEmpty() )
		{
			KoLMessenger.dispose();
			StaticEntity.closeSession();
			System.exit(0);
		}
	}

	public void setTitle( String title )
	{
		if ( title.startsWith( VERSION_NAME ) )
			super.setTitle( VERSION_NAME );
		else
			super.setTitle( VERSION_NAME + ": " + title );
	}

	public String getFrameName()
	{	return frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component.
	 * Note that this method can only be used if the KoLFrame on which it
	 * is called has not yet added any components.  If there are any added
	 * components, this method will do nothing.
	 */

	public void addCompactPane()
	{
		if ( framePanel.getComponentCount() != 0 )
			return;

		JPanel compactPane = new JPanel();
		compactPane.setOpaque( false );
		compactPane.setLayout( new GridLayout( 14, 1 ) );

		compactPane.add( Box.createHorizontalStrut( 80 ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "hp.gif" ), JLabel.CENTER ) );
		compactPane.add( hpLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "mp.gif" ), JLabel.CENTER ) );
		compactPane.add( mpLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( familiarLabel = new JLabel( " ", JLabel.CENTER ) );
		compactPane.add( weightLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "meat.gif" ), JLabel.CENTER ) );
		compactPane.add( meatLabel = new JLabel( " ", JLabel.CENTER ) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "hourglass.gif" ), JLabel.CENTER ) );
		compactPane.add( advLabel = new JLabel( " ",  JLabel.CENTER) );

		compactPane.add( new JLabel( JComponentUtilities.getSharedImage( "sixpack.gif" ), JLabel.CENTER ) );
		compactPane.add( drunkLabel = new JLabel( " ",  JLabel.CENTER) );

		compactPane.add( Box.createHorizontalStrut( 80 ) );

		this.compactPane = new JPanel();
		this.compactPane.setLayout( new BorderLayout() );
		this.compactPane.add( compactPane, BorderLayout.NORTH );

		framePanel.setLayout( new BorderLayout() );
		framePanel.add( this.compactPane, BorderLayout.WEST );
		(new StatusRefresher()).run();

		KoLCharacter.addKoLCharacterListener( new KoLCharacterAdapter( new StatusRefresher() ) );
	}

	protected class StatusRefresher implements Runnable
	{
		public void run()
		{
			hpLabel.setText( KoLCharacter.getCurrentHP() + " / " + KoLCharacter.getMaximumHP() );
			mpLabel.setText( KoLCharacter.getCurrentMP() + " / " + KoLCharacter.getMaximumMP() );
			meatLabel.setText( df.format( KoLCharacter.getAvailableMeat() ) );
			advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
			drunkLabel.setText( String.valueOf( KoLCharacter.getInebriety() ) );
			familiarLabel.setIcon( JComponentUtilities.getSharedImage( "itemimages/familiar" + KoLCharacter.getFamiliar().getID() + ".gif" ) );
			weightLabel.setText( KoLCharacter.getFamiliar().getWeight() + " lbs." );
		}
	}

	/**
	 * Method used to set the current extended state of the
	 * frame.  In most KoLFrames, this defaults to iconified
	 * or normal, and maximizing is disabled.  However, request
	 * frames and profile frames display raw HTML - therefore,
	 * they need to be maximizable.
	 */

	public void setExtendedState( int state )
	{
		if ( this instanceof ChatFrame || this instanceof RequestFrame || state == ICONIFIED )
			super.setExtendedState( state );
		else
			super.setExtendedState( NORMAL );

	}

	/**
	 * Updates the display to reflect the given display state and
	 * to contain the given message.  Note that if there is no
	 * content panel, this method does nothing.
	 */

	public void updateDisplay( int displayState, String message )
	{
		if ( contentPanel != null )
			contentPanel.setStatusMessage( displayState, message );

		switch ( displayState )
		{
			case DISABLE_STATE:
				setEnabled( false );
				break;

			case NORMAL_STATE:
				break;

			default:
				setEnabled( true );
				break;

		}
	}

	/**
	 * Utility method used to give the content panel for this
	 * <code>KoLFrame</code> focus.  Note that if the content
	 * panel is <code>null</code>< this method does nothing.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( contentPanel != null )
			contentPanel.requestFocus();
	}

	/**
	 * Utility method which adds a menu bar to the frame.
	 * This is called by default to allow for all frames to
	 * have equivalent menu items.
	 */

	protected void addMenuBar()
	{
		compileBookmarks();

		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		addTravelMenu( menuBar );
		addPeopleMenu( menuBar );

		menuBar.add( new ScriptMenu() );
		menuBar.add( new BookmarkMenu() );

		addRefreshMenu( menuBar );
	}

	protected void addToolBar()
	{
		boolean isMainFrame =
			INTERFACE_MODES[ Integer.parseInt( GLOBAL_SETTINGS.getProperty( "userInterfaceMode" ) ) ].isAssignableFrom( getClass() );

		if ( isMainFrame )
		{
			toolbarPanel.add( new MiniBrowserButton() );
			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", CommandDisplayFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Adventure", "hourglass.gif", AdventureFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Status", "hp.gif", CharsheetFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment", "equipment.gif", GearChangeFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", ItemManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", StoreManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Display Case", "museum.gif", MuseumFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", HagnkStorageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );
		}

		if ( isMainFrame || this instanceof LoginFrame )
		{
			toolbarPanel.add( new DisplayFrameButton( "KoL Almanac", "calendar.gif", CalendarFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "KoL Encyclopedia", "encyclopedia.gif", ExamineItemsFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
			toolbarPanel.add( new InvocationButton( "Debugger", "debug.gif", KoLmafia.class, "openDebugLog" ) );

			toolbarPanel.add( new JToolBar.Separator() );
		}
	}

	protected JMenu addRefreshMenu( JComponent menu )
	{
		JMenu refreshMenu = new JMenu();
		menu.add( refreshMenu );

		refreshMenu.setIcon( JComponentUtilities.getSharedImage( "refresh.gif" ) );
		refreshMenu.add( new InvocationMenuItem( "Clear Results", client, "resetSession" ) );
		refreshMenu.add( new InvocationMenuItem( "Session Time-In", client, "executeTimeInRequest" ) );
		refreshMenu.add( new JSeparator() );

		refreshMenu.add( new RequestMenuItem( "Refresh Status", new CharsheetRequest( client ) ) );
		refreshMenu.add( new RequestMenuItem( "Refresh Items", new EquipmentRequest( client, EquipmentRequest.CLOSET ) ) );
		refreshMenu.add( new JSeparator() );

		refreshMenu.add( new RequestMenuItem( "Refresh Outfits", new EquipmentRequest( client, EquipmentRequest.EQUIPMENT ) ) );
		refreshMenu.add( new RequestMenuItem( "Refresh Familiars", new FamiliarRequest( client ) ) );
		refreshMenu.add( new JSeparator() );

		refreshMenu.add( new DisplayFrameMenuItem( "About KoLmafia...", LicenseDisplay.class ) );
		return refreshMenu;
	}

	/**
	 * Utility method which encapsulates the addition of
	 * things to the travel menu.
	 */

	protected final JMenu addTravelMenu( JComponent menu )
	{
		JMenu travelMenu = new JMenu( "Travel" );
		menu.add( travelMenu );

		travelMenu.add( new DisplayFrameMenuItem( "Visit Council", CouncilFrame.class ) );
		travelMenu.add( new InvocationMenuItem( "Eat Cake-Arena", client, "visitCakeShapedArena" ) );
		travelMenu.add( new InvocationMenuItem( "Doc Galaktik", client, "makeGalaktikRequest" ) );
		travelMenu.add( new DisplayFrameMenuItem( "Knoll Mushrooms", MushroomFrame.class ) );

		return travelMenu;
	}

	protected final JMenu addPeopleMenu( JComponent menu )
	{
		JMenu commMenu = new JMenu( "People" );
		menu.add( commMenu );

		commMenu.add( new InvocationMenuItem( "Chat of Loathing", KoLMessenger.class, "initialize" ) );
		commMenu.add( new DisplayFrameMenuItem( "IcePenguin Express", MailboxFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Administrate Clan", ClanManageFrame.class ) );

		commMenu.add( new JSeparator() );

		commMenu.add( new DisplayFrameMenuItem( "Write a New K-mail", GreenMessageFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Propose a New Trade", ProposeTradeFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "View Pending Offers", PendingTradesFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Gift Shop Back Room", GiftMessageFrame.class ) );

		return commMenu;
	}

	/**
	 * Auxiliary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on the core
	 * content panel.  It is advised that descendants override this
	 * behavior whenever necessary.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled;

		if ( contentPanel != null )
			contentPanel.setEnabled( isEnabled );

	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled()
	 * method does not call the superclass's version.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isEnabled()
	{	return true;
	}

	private class ToggleMacroMenuItem extends JMenuItem implements ActionListener
	{
		public ToggleMacroMenuItem()
		{
			super( "" );
			addActionListener( this );

			setText( client == null || client.getMacroStream() instanceof NullStream ? "Record script..." : "Stop recording" );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( client != null && client.getMacroStream() instanceof NullStream )
			{
				JFileChooser chooser = new JFileChooser( "scripts" );
				int returnVal = chooser.showSaveDialog( KoLFrame.this );

				if ( chooser.getSelectedFile() == null )
					return;

				String filename = chooser.getSelectedFile().getAbsolutePath();

				if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
					client.openMacroStream( filename );

				setText( "Stop recording" );
			}
			else if ( client != null )
			{
				client.closeMacroStream();
				setText( "Record script..." );
			}
		}
	}

	private class RefreshScriptsMenuItem extends JMenuItem implements ActionListener
	{
		public RefreshScriptsMenuItem()
		{
			super( "Refresh menu" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			// Update existing list in place so that change
			// listeners can update as appropriate

			scripts.clear();
			addedScriptCount = 0;
			scripts.addAll( addScripts( new File( "scripts" ), "scripts" ) );
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for loading a script.
	 */

	private static class LoadScriptMenuItem extends JMenuItem implements ActionListener, Runnable
	{
		private String scriptPath;

		public LoadScriptMenuItem()
		{	this( "Load script...", null );
		}

		public LoadScriptMenuItem( String scriptName, String scriptPath )
		{
			super( scriptName );
			addActionListener( this );

			this.scriptPath = scriptPath;
		}

		public void actionPerformed( ActionEvent e )
		{	(new DaemonThread( this )).start();
		}

		public void run()
		{
			String executePath = scriptPath;

			try
			{
				if ( scriptPath == null )
				{
					JFileChooser chooser = new JFileChooser( "scripts" );
					int returnVal = chooser.showOpenDialog( null );

					if ( chooser.getSelectedFile() == null )
						return;

					if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
						executePath = chooser.getSelectedFile().getAbsolutePath();
				}

				if ( executePath == null )
					return;

				(new KoLmafiaCLI( client, new FileInputStream( executePath ) )).listenForCommands();
			}
			catch ( Exception e )
			{
				// Here, notify the display that the script
				// file specified could not be loaded

				client.updateDisplay( ERROR_STATE, "Script \"" + executePath + "\" could not be loaded." );
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
			}

			client.enableDisplay();
		}
	}

	/**
	 * An internal class which creates a panel which manages items.
	 * This is done because most of the item management displays
	 * are replicated.  Note that a lot of this code was borrowed
	 * directly from the ActionVerifyPanel class in the utilities
	 * package for Spellcast.
	 */

	protected abstract class ItemManagePanel extends LabeledScrollPanel
	{
		protected ShowDescriptionList elementList;
		private VerifyButtonPanel buttonPanel;

		public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
		{
			super( title, confirmedText, cancelledText, new ShowDescriptionList( elements ) );

			elementList = (ShowDescriptionList) scrollComponent;
			elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			elementList.setVisibleRowCount( 8 );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}
	}

	protected void processWindowEvent( WindowEvent e )
	{
		if ( e.getID() == WindowEvent.WINDOW_CLOSING )
		{
			if ( client != null && client.getSettings().getProperty( "savePositions" ).equals( "true" ) )
			{
				Point p = getLocationOnScreen();
				client.getSettings().setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
				client.getSettings().saveSettings();
			}

			if ( !existingFrames.isEmpty() && getProperty( "oversightProtect" ).equals( "true" ) )
			{
				boolean isMainFrame =
					INTERFACE_MODES[ Integer.parseInt( GLOBAL_SETTINGS.getProperty( "userInterfaceMode" ) ) ].isAssignableFrom( getClass() );

				if ( isMainFrame && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
					"Would you like to stay logged in?", "SDUGA Keep-Alive Feature", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
						existingFrames.clear();
			}
		}

		super.processWindowEvent( e );
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	protected class DisplayFrameButton extends JButton implements ActionListener
	{
		private Class frameClass;
		private CreateFrameRunnable displayer;

		public DisplayFrameButton( String tooltip, String icon, Class frameClass )
		{
			super( JComponentUtilities.getSharedImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );

			addActionListener( this );
			this.frameClass = frameClass;

			Object [] parameters;
			if ( frameClass == LicenseDisplay.class )
			{
				parameters = new Object[4];
				parameters[0] = "KoLmafia: Copyright Notice";
				parameters[1] = new VersionDataPanel();
				parameters[2] = LICENSE_FILENAME;
				parameters[3] = LICENSE_NAME;
			}
			else
			{
				parameters = new KoLmafia[1];
				parameters[0] = client;
			}

			this.displayer = new CreateFrameRunnable( frameClass, parameters );
		}

		public void actionPerformed( ActionEvent e )
		{
			displayer.setEnabled( isEnabled );
			SwingUtilities.invokeLater( displayer );
		}
	}


	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	protected class DisplayFrameMenuItem extends JMenuItem implements ActionListener
	{
		private Class frameClass;
		private CreateFrameRunnable displayer;

		public DisplayFrameMenuItem( String title, Class frameClass )
		{
			super( title );
			addActionListener( this );

			this.frameClass = frameClass;

			Object [] parameters;
			if ( frameClass == LicenseDisplay.class )
			{
				parameters = new Object[4];
				parameters[0] = "KoLmafia: Copyright Notice";
				parameters[1] = new VersionDataPanel();
				parameters[2] = LICENSE_FILENAME;
				parameters[3] = LICENSE_NAME;
			}
			else
			{
				parameters = new KoLmafia[1];
				parameters[0] = client;
			}

			this.displayer = new CreateFrameRunnable( frameClass, parameters );
		}

		public void actionPerformed( ActionEvent e )
		{
			displayer.setEnabled( isEnabled );
			SwingUtilities.invokeLater( displayer );
		}
	}

	/**
	 * Internal class which opens the operating system's default
	 * browser to the given location.
	 */

	protected class DisplayPageMenuItem extends JMenuItem implements ActionListener
	{
		private String location;

		public DisplayPageMenuItem( String title, String location )
		{
			super( title );
			addActionListener( this );

			this.location = location;
		}

		public void actionPerformed( ActionEvent e )
		{
			try
			{
				BrowserLauncher.openURL( location );
			}
			catch ( java.io.IOException e1 )
			{
				KoLmafia.getLogStream().println( "Failed to open browser:" );
				KoLmafia.getLogStream().print( e1 );
				e1.printStackTrace( KoLmafia.getLogStream() );
			}
		}
	}

	/**
	 * Action listener responsible for handling links clicked
	 * inside of a <code>JEditorPane</code>.
	 */

	protected class KoLHyperlinkAdapter extends HyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			if ( location.startsWith( "desc" ) || location.startsWith( "doc" ) || location.startsWith( "searchp" ) )
			{
				// Certain requests should open in a new window.
				// These include description data, documentation
				// and player searches.

				openRequestFrame( location );
			}
			else if ( KoLFrame.this instanceof RequestFrame )
			{
				// If this is a request frame, make sure that
				// you minimize the number of open windows by
				// making an attempt to refresh.

				((RequestFrame)KoLFrame.this).refresh( extractRequest( location ) );
			}
			else
			{
				// Otherwise, if this isn't a request frame,
				// open up a new request frame in order to
				// display the appropriate data.

				openRequestFrame( location );
			}
		}
	}

	/**
	 * An internal class which opens a new <code>RequestFrame</code>
	 * to the given frame whenever an action event is triggered.
	 */

	protected class MiniBrowserButton extends JButton implements ActionListener
	{
		private String location;
		private boolean useSavedRequest;

		public MiniBrowserButton()
		{
			super( JComponentUtilities.getSharedImage( "browser.gif" ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			addActionListener( this );
			setToolTipText( "Mini-Browser" );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLRequest request = client.getCurrentRequest();
			if ( request != null )
			{
				client.setCurrentRequest( null );
				openRequestFrame( request );
				return;
			}
			else
				openRequestFrame( "main.php" );
		}
	}

	private static final Class [] NOPARAMS = new Class[0];

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	protected class InvocationMenuItem extends JMenuItem implements ActionListener, Runnable
	{
		private Object object;
		private Method method;
		private boolean requiresCancel;

		public InvocationMenuItem( String title, Object object, String methodName )
		{
			this( title, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationMenuItem( String title, Class c, String methodName )
		{	this( title, null, c, methodName );
		}

		public InvocationMenuItem( String title, String icon, Class c, String methodName )
		{
			super( title );
			if ( icon != null )
				setIcon( JComponentUtilities.getSharedImage( icon ) );

			addActionListener( this );

			try
			{
				this.object = object;
				this.method = c.getMethod( methodName, NOPARAMS );
				this.requiresCancel = methodName.equals( "executeTimeInRequest" );
			}
			catch ( Exception e )
			{
			}
		}

		public void actionPerformed( ActionEvent e )
		{	(new DaemonThread( this )).start();
		}

		public void run()
		{
			try
			{
				if ( method != null )
				{
					if ( requiresCancel )
						client.cancelRequest();

					method.invoke( object, null );
				}
			}
			catch ( Exception e )
			{
			}
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	protected class InvocationButton extends JButton implements ActionListener, Runnable
	{
		private Object object;
		private Method method;

		public InvocationButton( String tooltip, String icon, Object object, String methodName )
		{
			this( tooltip, icon, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String tooltip, String icon, Class c, String methodName )
		{
			super( JComponentUtilities.getSharedImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			addActionListener( this );
			setToolTipText( tooltip );

			try
			{
				this.object = object;
				this.method = c.getMethod( methodName, NOPARAMS );
			}
			catch ( Exception e )
			{
			}
		}

		public void actionPerformed( ActionEvent e )
		{	(new DaemonThread( this )).start();
		}

		public void run()
		{
			try
			{
				if ( method != null )
					method.invoke( object, null );
			}
			catch ( Exception e )
			{
			}
		}
	}

	/**
	 * Utility method used to determine the KoLRequest that
	 * should be sent, given the appropriate location.
	 */

	private KoLRequest extractRequest( String location )
	{
		String [] urlData = location.split( "\\?" );
		String [] formData = urlData.length == 1 ? new String[0] : urlData[1].split( "&" );

		String [] currentField;
		KoLRequest request = new KoLRequest( client, urlData[0], true );

		for ( int i = 0; i < formData.length; ++i )
		{
			currentField = formData[i].split( "=" );

			if ( currentField.length == 2 )
				request.addFormField( currentField[0], currentField[1] );
			else
				request.addFormField( formData[i] );
		}

		return request;
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays
	 * the given location, relative to the KoL home directory for the current
	 * session.  This should be called whenever <code>RequestFrame</code>s
	 * need to be created in order to keep code modular.
	 */

	public void openRequestFrame( String location )
	{	openRequestFrame( extractRequest( location ) );
	}

	public void openRequestFrame( KoLRequest request )
	{
		Object [] parameters;

		if ( this instanceof RequestFrame )
		{
			if ( ((RequestFrame)this).hasSideBar() )
			{
				((RequestFrame)this).refresh( request );
				return;
			}

			parameters = new Object[3];
			parameters[0] = client;
			parameters[1] = this;
			parameters[2] = request;
		}
		else
		{
			parameters = new Object[2];
			parameters[0] = client;
			parameters[1] = request;
		}

		SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class, parameters ) );
	}

	/**
	 * An internal class used to handle requests to open a new frame
	 * using a local panel inside of the adventure frame.
	 */

	protected class KoLPanelFrameMenuItem extends JMenuItem implements ActionListener
	{
		private CreateFrameRunnable creator;

		public KoLPanelFrameMenuItem( String title, ActionPanel panel )
		{
			super( title );
			addActionListener( this );

			Object [] parameters = new Object[3];
			parameters[0] = client;
			parameters[1] = title;
			parameters[2] = panel;

			creator = new CreateFrameRunnable( KoLPanelFrame.class, parameters );
		}

		public void actionPerformed( ActionEvent e )
		{	creator.run();
		}
	}

	/**
	 * An internal class used to handle requests which reset a property
	 * in the settings file.
	 */

	protected class SettingChangeMenuItem extends JCheckBoxMenuItem implements ActionListener
	{
		private String property;

		public SettingChangeMenuItem( String title, String property )
		{
			super( title );
			setSelected( getProperty( property ).equals( "true" ) );

			this.property = property;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	setProperty( property, String.valueOf( isSelected() ) );
		}
	}

	protected class SettingChangeCheckBox extends JCheckBox implements ActionListener
	{
		private String property;

		public SettingChangeCheckBox( String title, String property )
		{
			super( title );
			setSelected( getProperty( property ).equals( "true" ) );

			this.property = property;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	setProperty( property, String.valueOf( isSelected() ) );
		}
	}

	/**
	 * An internal class used to handle requests which resets a property
	 * for the duration of the current session.
	 */

	protected class LocalSettingChangeMenuItem extends JCheckBoxMenuItem implements ActionListener
	{
		private String property;

		public LocalSettingChangeMenuItem( KoLmafia client, String title, String property )
		{
			super( title );
			setSelected( client.getLocalBooleanProperty( property ) );

			this.property = property;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	client.setLocalProperty( property, isSelected() );
		}
	}

	/**
	 * This internal class is used to process the request for selecting
	 * a script using the file dialog.
	 */

	protected class ScriptSelectPanel extends JPanel implements ActionListener
	{
		private JTextField scriptField;

		public ScriptSelectPanel( JTextField scriptField )
		{
			setLayout( new BorderLayout( 0, 0 ) );

			add( scriptField, BorderLayout.CENTER );
			JButton scriptButton = new JButton( "..." );

			JComponentUtilities.setComponentSize( scriptButton, 20, 20 );
			scriptButton.addActionListener( this );
			add( scriptButton, BorderLayout.EAST );

			this.scriptField = scriptField;
		}

		public void actionPerformed( ActionEvent e )
		{
			JFileChooser chooser = new JFileChooser( "." );
			int returnVal = chooser.showOpenDialog( KoLFrame.this );

			if ( chooser.getSelectedFile() == null )
				return;

			scriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
		}
	}

	/**
	 * An internal class which displays KoLmafia's current version
	 * information.  This is passed to the constructor for the
	 * <code>LicenseDisplay</code>.
	 */

	private class VersionDataPanel extends JPanel
	{
		private final String [] versionData = {
			VERSION_NAME, VERSION_DATE, " ",
			"Copyright © 2005 KoLmafia development team",
			"Berkeley Software Development (BSD) License",
			"http://kolmafia.sourceforge.net/",
			" ",
			"Current Running on " + System.getProperty( "os.name" ),
			"Using Java v" + System.getProperty( "java.runtime.version" )
		};

		public VersionDataPanel()
		{
			JPanel versionPanel = new JPanel();
			versionPanel.setLayout( new BorderLayout( 20, 20 ) );

			versionPanel.add( new JLabel( JComponentUtilities.getSharedImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.NORTH );

			JPanel labelPanel = new JPanel();
			labelPanel.setLayout( new GridLayout( versionData.length, 1 ) );

			for ( int i = 0; i < versionData.length; ++i )
				labelPanel.add( new JLabel( versionData[i], JLabel.CENTER ) );

			versionPanel.add( labelPanel, BorderLayout.CENTER );

			JButton donateButton = new JButton( JComponentUtilities.getSharedImage( "paypal.gif" ) );
			JComponentUtilities.setComponentSize( donateButton, 74, 31 );
			donateButton.addActionListener( new DisplayPageMenuItem( "", "http://sourceforge.net/donate/index.php?user_id=813949" ) );

			JPanel donatePanel = new JPanel();
			donatePanel.setLayout( new FlowLayout() );
			donatePanel.add( donateButton );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 20, 20 ) );
			centerPanel.add( versionPanel, BorderLayout.CENTER );
			centerPanel.add( donatePanel, BorderLayout.SOUTH );

			setLayout( new CardLayout( 20, 20 ) );
			add( centerPanel, "" );
		}
	}

	/**
	 * A special class which renders the list of available scripts.
	 */

	private class ScriptMenu extends MenuItemList
	{
		public ScriptMenu()
		{	super( "Scripts", scripts );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[6];

			headers[0] = new LoadScriptMenuItem();
			headers[1] = new ToggleMacroMenuItem();
			headers[2] = new RefreshScriptsMenuItem();

			headers[3] = new JSeparator();

			JMenu toolsMenu = new JMenu( "Built-In Tools" );

			toolsMenu.add( new InvocationMenuItem( "Get Breakfast!", client, "getBreakfast" ) );
			toolsMenu.add( new InvocationMenuItem( "Pwn Clan Otori!", client, "pwnClanOtori" ) );

			toolsMenu.add( new JSeparator() );

			toolsMenu.add( new DisplayFrameMenuItem( "Run a KoL BuffBot", BuffBotFrame.class ) );
			toolsMenu.add( new DisplayFrameMenuItem( "Patronize a BuffBot", BuffRequestFrame.class ) );
			toolsMenu.add( new DisplayFrameMenuItem( "Familiar Trainer", FamiliarTrainingFrame.class ) );

			headers[4] = toolsMenu;

			JMenu modulesMenu = new JMenu( "Quest Scripts" );

			modulesMenu.add( new InvocationMenuItem( "Face Nemesis", Nemesis.class, "faceNemesis" ) );
			modulesMenu.add( new InvocationMenuItem( "Rob Strange Leaflet", StrangeLeaflet.class, "robStrangeLeaflet" ) );

			modulesMenu.add( new JSeparator() );

			modulesMenu.add( new InvocationMenuItem( "Lair Entryway", SorceressLair.class, "completeEntryway" ) );
			modulesMenu.add( new InvocationMenuItem( "Hedge Rotation", SorceressLair.class, "completeHedgeMaze" ) );
			modulesMenu.add( new InvocationMenuItem( "Tower Guardians", SorceressLair.class, "fightTowerGuardians" ) );
			modulesMenu.add( new InvocationMenuItem( "Naughty Chamber", SorceressLair.class, "completeSorceressChamber" ) );

			headers[5] = modulesMenu;

			return headers;
		}
	}

	/**
	 * A special class which displays an item's description after you double
	 * click on the JList.
	 */

	protected class ShowDescriptionList extends JList
	{
		public ShowDescriptionList( LockableListModel model )
		{
			super( model );
			addMouseListener( new ShowDescriptionAdapter() );
		}


		private class ShowDescriptionAdapter extends MouseAdapter
		{
			public void mouseClicked( MouseEvent e )
			{
				if ( e.getClickCount() == 2 )
				{
					int index = locationToIndex( e.getPoint() );
					Object item = getModel().getElementAt( index );

					if ( item instanceof AdventureResult && ((AdventureResult)item).isItem() )
					{
						ensureIndexIsVisible( index );
						openRequestFrame( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( ((AdventureResult)item).getItemID() ) );
					}
					if ( item instanceof ItemCreationRequest )
					{
						ensureIndexIsVisible( index );
						openRequestFrame( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionID( ((ItemCreationRequest)item).getItemID() ) );
					}
				}
			}
		}
	}

	private class RequestMenuItem extends JMenuItem implements ActionListener
	{
		private KoLRequest request;

		public RequestMenuItem( String title, KoLRequest request )
		{
			super( title );
			this.request = request;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	(new RequestThread( request )).start();
		}
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the number "0" is returned instead.
	 */

	protected static final int getValue( JTextField field )
	{	return getValue( field, 0 );
	}

	/**
	 * Utility method which retrieves an integer value from the given
	 * field.  In the event that the field does not contain an integer
	 * value, the default value provided will be returned instead.
	 */

	protected static final int getValue( JTextField field, int defaultValue )
	{
		try
		{
			return field.getText() == null || field.getText().length() == 0 ?
				defaultValue : df.parse( field.getText().trim() ).intValue();
		}
		catch ( Exception e )
		{
			// If something's wrong with the parsing, assume
			// that the person wanted the default value.

			return defaultValue;
		}
	}

	protected static final int getQuantity( String title, int maximumValue, int defaultValue )
	{
		// Check parameters; avoid programmer error.
		if ( defaultValue > maximumValue )
			return 0;

		if ( maximumValue == 1 && maximumValue == defaultValue )
			return 1;

		try
		{
			String currentValue = JOptionPane.showInputDialog( title, df.format( defaultValue ) );
			if ( currentValue == null )
				return 0;

			int desiredValue = df.parse( currentValue ).intValue();
			return Math.max( 0, Math.min( desiredValue, maximumValue ) );
		}
		catch ( Exception e )
		{
			return 0;
		}
	}

	protected static final int getQuantity( String title, int maximumValue )
	{	return getQuantity( title, maximumValue, maximumValue );
	}

	protected final void setProperty( String name, String value )
	{	StaticEntity.setProperty( name, value );
	}

	protected final String getProperty( String name )
	{	return StaticEntity.getProperty( name );
	}

	/**
	 * Internal class which displays the given request inside
	 * of the current frame.
	 */

	protected class DisplayRequestMenuItem extends JMenuItem implements ActionListener
	{
		private String location;

		public DisplayRequestMenuItem( String label, String location )
		{
			super( label.replaceAll( "\\|", "" ) );
			addActionListener( this );
			this.location = location;
		}

		public void actionPerformed( ActionEvent e )
		{	openRequestFrame( location );
		}

		public String toString()
		{	return getText();
		}

		public String toSettingString()
		{
			return getText() +  "|" +
				location.replaceFirst( "pwd=" + client.getPasswordHash(), "" ).replaceFirst( "\\?&", "?" ).replaceFirst( "&&", "&" ) + "|" +
					String.valueOf( location.indexOf( "pwd=" ) != -1 );
		}
	}

	/**
	 * A special class which renders the menu holding the list of bookmarks.
	 * This class also synchronizes with the list of available bookmarks.
	 */

	protected class BookmarkMenu extends MenuItemList
	{
		public BookmarkMenu()
		{	super( "Bookmarks", bookmarks );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[ KoLCharacter.inMysticalitySign() ? 12 : 11 ];

			headers[0] = new AddBookmarkMenuItem();
			headers[1] = new KoLPanelFrameMenuItem( "Manage Bookmarks", new BookmarkManagePanel() );
			headers[2] = new JSeparator();

			headers[3] = new DisplayPageMenuItem( "Visual Wiki", "http://www.thekolwiki.net/" );
			headers[4] = new DisplayPageMenuItem( "Item Effects", "http://www.lysator.liu.se/~jhs/KoL/effects/" );
			headers[5] = new DisplayPageMenuItem( "Moxie Survival", "http://kol.network-forums.com/cgi-bin/moxie.cgi" );

			headers[6] = new JSeparator();

			headers[7] = new DisplayPageMenuItem( "Loot the Hermit", "hermit.php" );
			headers[8] = new DisplayPageMenuItem( "Mountain Trapper", "trapper.php" );
			headers[9] = new DisplayPageMenuItem( "Bounty Hunter", "town_wrong?place=bountyhunter" );
			headers[10] = new DisplayPageMenuItem( "Untinker Items", "town_right.php?place=untinker" );

			if ( KoLCharacter.inMysticalitySign() )
				headers[11] = new DisplayPageMenuItem( "Canadian Device", "canadia.php?place=machine" );

			return headers;
		}

		/**
		 * An internal class which handles the addition of new
		 * bookmarks to the bookmark menu.
		 */

		private class AddBookmarkMenuItem extends JMenuItem implements ActionListener
		{
			public AddBookmarkMenuItem()
			{
				super( "Add Bookmark..." );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				AddBookmarkDialog prompter = new AddBookmarkDialog();
				prompter.pack();  prompter.setVisible( true );
			}
		}

		private class AddBookmarkDialog extends JDialog
		{
			public AddBookmarkDialog()
			{
				super( KoLFrame.this, "Add a KoLmafia Bookmark!" );
				getContentPane().add( new AddBookmarkPanel() );
			}

			private class AddBookmarkPanel extends KoLPanel
			{
				private JTextField nameField;
				private JTextField locationField;

				public AddBookmarkPanel()
				{
					super( "add", "cancel" );

					VerifiableElement [] elements = new VerifiableElement[2];
					elements[0] = new VerifiableElement( "Name", nameField = new JTextField() );
					elements[1] = new VerifiableElement( "Location", locationField = new JTextField() );
					setContent( elements );
				}

				public void actionConfirmed()
				{
					bookmarks.add( new DisplayRequestMenuItem( nameField.getText(), locationField.getText() ) );
					saveBookmarks();

					AddBookmarkDialog.this.dispose();
				}

				public void actionCancelled()
				{
					AddBookmarkDialog.this.dispose();
				}
			}
		}
	}

	/**
	 * A special panel which generates a list of bookmarks which
	 * can subsequently be managed.
	 */

	protected class BookmarkManagePanel extends ItemManagePanel
	{
		public BookmarkManagePanel()
		{
			super( "Bookmark Management", "rename", "delete", bookmarks );
			elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}

		public void actionConfirmed()
		{
			DisplayRequestMenuItem currentItem = (DisplayRequestMenuItem) elementList.getSelectedValue();
			if ( currentItem == null )
				return;

			String name = JOptionPane.showInputDialog( "Name your bookmark?", currentItem.getText() );

			if ( name == null )
				return;

			currentItem.setText( name );
			saveBookmarks();
		}

		public void actionCancelled()
		{
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			bookmarks.remove( index );
			saveBookmarks();
		}
	}

	/**
	 * Utility method to compile the list of bookmarks based on the
	 * current server settings.
	 */

	protected void compileBookmarks()
	{
		bookmarks.clear();

		String [] bookmarkData = GLOBAL_SETTINGS.getProperty( "browserBookmarks" ).split( "\\|" );
		String name, location, pwdhash;

		if ( bookmarkData.length > 1 )
		{
			for ( int i = 0; i < bookmarkData.length; ++i )
			{
				name = bookmarkData[i];
				location = bookmarkData[++i];
				pwdhash = bookmarkData[++i];

				if ( pwdhash.equals( "true" ) )
					location += "&pwd=" + client.getPasswordHash();

				bookmarks.add( new DisplayRequestMenuItem( name, location ) );
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings
	 * file.  This should be called after every update.
	 */

	protected void saveBookmarks()
	{
		StringBuffer bookmarkData = new StringBuffer();

		if ( !bookmarks.isEmpty() )
			bookmarkData.append( ((DisplayRequestMenuItem)bookmarks.get(0)).toSettingString() );

		for ( int i = 1; i < bookmarks.size(); ++i )
		{
			bookmarkData.append( '|' );
			bookmarkData.append( ((DisplayRequestMenuItem)bookmarks.get(i)).toSettingString() );
		}

		GLOBAL_SETTINGS.setProperty( "browserBookmarks", bookmarkData.toString() );
		GLOBAL_SETTINGS.saveSettings();
	}
}
