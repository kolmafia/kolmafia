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
import java.awt.Image;
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
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;

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
import javax.swing.ImageIcon;
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
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

// other stuff
import javax.swing.SwingUtilities;
import java.lang.ref.WeakReference;
import edu.stanford.ejalbert.BrowserLauncher;

// spellcast imports
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame implements KoLConstants
{
	private static final Color ERROR_COLOR = new Color( 255, 128, 128 );
	private static final Color ENABLED_COLOR = new Color( 128, 255, 128 );
	private static final Color DISABLED_COLOR = null;

	private String lastTitle;
	protected boolean isEnabled;
	protected static KoLmafia client;

	static
	{
		// Ensure that images are properly loaded
		// and the appropriate HTML rendering engine
		// is installed to the frames.

		System.setProperty( "SHARED_MODULE_DIRECTORY", "net/sourceforge/kolmafia/" );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
	};

	protected static LockableListModel scripts = new LockableListModel();
	protected ScriptMenu scriptMenu;

	protected static final FilenameFilter BACKUP_FILTER = new FilenameFilter()
	{
		public boolean accept( File dir, String name )
		{	return !name.startsWith( "." ) && !name.endsWith( "~" ) && !name.endsWith( ".bak" );
		}
	};

	protected static SortedListModel bookmarks = new SortedListModel( String.class );
	private static boolean bookmarksCompiled = false;
	protected BookmarkMenu bookmarkMenu;

	private static final String [] LICENSE_FILENAME = { "kolmafia-license.gif", "spellcast-license.gif", "browserlauncher-license.htm", "sungraphics-license.txt", "systray-license.txt" };
	private static final String [] LICENSE_NAME = { "KoLmafia BSD", "Spellcast BSD", "BrowserLauncher", "Sun Graphics", "System Tray" };

	private String frameName;
	protected JPanel framePanel;
	protected JToolBar toolbarPanel;

	protected JPanel compactPane;
	protected JLabel hpLabel, mpLabel, advLabel;
	protected JLabel meatLabel, drunkLabel;
	protected JLabel familiarLabel;

	protected KoLCharacterAdapter refreshListener;
	protected JMenuItem debugMenuItem, macroMenuItem;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given client.
	 */

	protected KoLFrame( KoLmafia client, String title )
	{
		super( KoLCharacter.getUsername().equals( "" ) ? VERSION_NAME + ": " + title :
			KoLCharacter.getUsername() + ": " + title + " (" + KoLRequest.getRootHostName() + ")" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		this.isEnabled = true;
		this.lastTitle = title;
		KoLFrame.client = client;

		this.framePanel = new JPanel( new BorderLayout( 0, 0 ) );
		getContentPane().add( this.framePanel, BorderLayout.CENTER );

		this.toolbarPanel = null;

		switch ( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "toolbarPosition" ) ) )
		{
			case 1:
				this.toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				getContentPane().add( toolbarPanel, BorderLayout.NORTH );
				break;

			case 2:
				this.toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				getContentPane().add( toolbarPanel, BorderLayout.SOUTH );
				break;

			case 3:
				this.toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
				getContentPane().add( toolbarPanel, BorderLayout.WEST );
				break;

			case 4:
				this.toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
				getContentPane().add( toolbarPanel, BorderLayout.EAST );
				break;

			default:

				this.toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				if ( this instanceof LoginFrame || this instanceof ChatFrame )
				{
					getContentPane().add( toolbarPanel, BorderLayout.NORTH );
					break;
				}
		}

		this.frameName = getClass().getName();
		this.frameName = frameName.substring( frameName.lastIndexOf( "." ) + 1 );

		if ( !(this instanceof SystemTrayFrame) )
			this.existingFrames.add( this );

		// All frames will have to access the same menu bar for consistency.
		// Later on, all menu items not added by default will be placed onto
		// the panel for increased visibility.

		if ( !(this instanceof ContactListFrame) )
		{
			JMenuBar container = new JMenuBar();
			this.setJMenuBar( container );

			compileBookmarks();
			constructMenus( container );
		}
	}

	/**
	 * Overrides the default behavior of dispose so that the frames
	 * are removed from the internal list of existing frames.  Also
	 * allows for automatic exit.
	 */

	public void dispose()
	{
		super.dispose();

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		existingFrames.remove( this );

		if ( refreshListener != null )
			KoLCharacter.removeCharacterListener( refreshListener );

		// If the list of frames is now empty, make sure
		// you end the session.  Ending the session for
		// a login frame involves exiting, and ending the
		// session for all other frames is restarting the
		// initial client.

		if ( existingFrames.isEmpty() )
		{
			KoLMessenger.dispose();
			StaticEntity.closeSession();

			if ( this instanceof LoginFrame )
			{
				SystemTrayFrame.removeTrayIcon();
				System.exit(0);
			}
			else
				KoLmafiaGUI.main( new String[1] );
		}
	}

	public void updateTitle()
	{	setTitle( lastTitle );
	}

	public void setTitle( String title )
	{
		this.lastTitle = title;
		super.setTitle( KoLCharacter.getUsername() + ": " + title + " (" + KoLRequest.getRootHostName() + ")" );
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

		JPanel compactPane = new JPanel( new GridLayout( 7, 1, 0, 20 ) );
		compactPane.setOpaque( false );

		compactPane.add( hpLabel = new JLabel( " ", JComponentUtilities.getSharedImage( "hp.gif" ), JLabel.CENTER ) );
		compactPane.add( mpLabel = new JLabel( " ", JComponentUtilities.getSharedImage( "mp.gif" ), JLabel.CENTER ) );

		compactPane.add( familiarLabel = new UnanimatedLabel() );

		compactPane.add( meatLabel = new JLabel( " ", JComponentUtilities.getSharedImage( "meat.gif" ), JLabel.CENTER ) );
		compactPane.add( advLabel = new JLabel( " ", JComponentUtilities.getSharedImage( "hourglass.gif" ), JLabel.CENTER ) );
		compactPane.add( drunkLabel = new JLabel( " ", JComponentUtilities.getSharedImage( "sixpack.gif" ), JLabel.CENTER) );

		compactPane.add( Box.createHorizontalStrut( 80 ) );

		this.compactPane = new JPanel();
		this.compactPane.setLayout( new BoxLayout( this.compactPane, BoxLayout.Y_AXIS ) );
		this.compactPane.add( Box.createVerticalStrut( 20 ) );
		this.compactPane.add( compactPane );

		framePanel.setLayout( new BorderLayout() );
		framePanel.add( this.compactPane, BorderLayout.WEST );
		(new StatusRefresher()).run();

		this.refreshListener = new KoLCharacterAdapter( new StatusRefresher() );
		KoLCharacter.addCharacterListener( refreshListener );
	}

	protected class StatusRefresher implements Runnable
	{
		public void run()
		{
			hpLabel.setText( KoLCharacter.getCurrentHP() + " / " + KoLCharacter.getMaximumHP() );
			hpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			hpLabel.setHorizontalTextPosition( JLabel.CENTER );

			mpLabel.setText( KoLCharacter.getCurrentMP() + " / " + KoLCharacter.getMaximumMP() );
			mpLabel.setVerticalTextPosition( JLabel.BOTTOM );
			mpLabel.setHorizontalTextPosition( JLabel.CENTER );

			meatLabel.setText( df.format( KoLCharacter.getAvailableMeat() ) );
			meatLabel.setVerticalTextPosition( JLabel.BOTTOM );
			meatLabel.setHorizontalTextPosition( JLabel.CENTER );

			advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
			advLabel.setVerticalTextPosition( JLabel.BOTTOM );
			advLabel.setHorizontalTextPosition( JLabel.CENTER );

			drunkLabel.setText( String.valueOf( KoLCharacter.getInebriety() ) );
			drunkLabel.setVerticalTextPosition( JLabel.BOTTOM );
			drunkLabel.setHorizontalTextPosition( JLabel.CENTER );

			FamiliarData familiar = KoLCharacter.getFamiliar();
			int id = familiar == null ? -1 : familiar.getID();

			if ( id == -1 )
			{
				familiarLabel.setIcon( JComponentUtilities.getSharedImage( "debug.gif" ) );
				familiarLabel.setText( "0 lbs." );
				familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
			}
			else
			{
				ImageIcon familiarIcon = FamiliarsDatabase.getFamiliarImage( id );
				familiarLabel.setIcon( familiarIcon );
				familiarLabel.setText( familiar.getModifiedWeight() + (familiar.getModifiedWeight() == 1 ? " lb." : " lbs.") );
				familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
				familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
			}
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
		if ( state == ICONIFIED && System.getProperty( "os.name" ).startsWith( "Windows" ) && GLOBAL_SETTINGS.getProperty( "useSystemTrayIcon" ).equals( "true" ) )
			setVisible( false );
		else
			super.setExtendedState( state );

	}

	public void updateDisplayState( int displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		switch ( displayState )
		{
			case ERROR_STATE:
			case ABORT_STATE:

				if ( compactPane != null )
					compactPane.setBackground( ERROR_COLOR );

				setEnabled( true );
				break;

			case ENABLE_STATE:

				if ( compactPane != null )
					compactPane.setBackground( ENABLED_COLOR );

				setEnabled( true );
				break;

			case DISABLE_STATE:

				if ( compactPane != null )
					compactPane.setBackground( DISABLED_COLOR );

				setEnabled( false );
				break;
		}
	}

	/**
	 * Utility method which adds a menu bar to the frame.
	 * This is called by default to allow for all frames to
	 * have equivalent menu items.
	 */

	protected void constructMenus( JComponent container )
	{
		// Add general features.

		JMenu statusMenu = new JMenu( "General" );
		container.add( statusMenu );

		// Add the refresh menu, which holds the ability to refresh
		// everything in the session.

		statusMenu.add( new DisplayFrameMenuItem( "Main Interface", AdventureFrame.class ) );
		statusMenu.add( new DisplayRequestMenuItem( "Mini-Browser", "main.php" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Graphical CLI", CommandDisplayFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Preferences", OptionsFrame.class ) );

		statusMenu.add( new JSeparator() );

		statusMenu.add( new DisplayFrameMenuItem( "Player Status", CharsheetFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Item Manager", ItemManageFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Gear Changer", GearChangeFrame.class ) );

		statusMenu.add( new JSeparator() );

		statusMenu.add( new DisplayFrameMenuItem( "Mall Manager", StoreManageFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Museum Display", MuseumFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Hagnk Storage", HagnkStorageFrame.class ) );

		// Add specialized tools.

		JMenu toolsMenu = new JMenu( "Tools" );
		container.add( toolsMenu );

		toolsMenu.add( new InvocationMenuItem( "Clear Results", client, "resetSession" ) );
		toolsMenu.add( new StopEverythingItem() );
		toolsMenu.add( new InvocationMenuItem( "Session Time-In", client, "executeTimeInRequest" ) );

		toolsMenu.add( new JSeparator() );

		toolsMenu.add( new KoLPanelFrameMenuItem( "Manual Buffings", new SkillBuffPanel() ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Run a Buffbot", BuffBotFrame.class ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Purchase Buffs", BuffRequestFrame.class ) );

		toolsMenu.add( new JSeparator() );

		toolsMenu.add( new DisplayFrameMenuItem( "Flower Hunter", FlowerHunterFrame.class ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Mushroom Plot", MushroomFrame.class ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Familiar Trainer", FamiliarTrainingFrame.class ) );

		// Add the old-school people menu.

		JMenu peopleMenu = new JMenu( "People" );
		container.add( peopleMenu );

		peopleMenu.add( new DisplayFrameMenuItem( "Mail Manager", MailboxFrame.class ) );
		peopleMenu.add( new InvocationMenuItem( "KoLmafia Chat", KoLMessenger.class, "initialize" ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Clan Manager", ClanManageFrame.class ) );

		peopleMenu.add( new JSeparator() );

		peopleMenu.add( new DisplayFrameMenuItem( "Green Message", GreenMessageFrame.class ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Purple Message", GiftMessageFrame.class ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Propose Trade", ProposeTradeFrame.class ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Accept Trades", PendingTradesFrame.class ) );

		// Add in common tasks menu

		JMenu travelMenu = new JMenu( "Travel" );
		container.add( travelMenu );

		travelMenu.add( new InvocationMenuItem( "Doc Galaktik", client, "makeGalaktikRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Mind Control", client, "makeMindControlRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Get Breakfast", client, "getBreakfast" ) );

		travelMenu.add( new JSeparator() );

		travelMenu.add( new InvocationMenuItem( "Loot the Hermit", client, "makeHermitRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Skin the Trapper", client, "makeTrapperRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Trading Hunters", client, "makeHunterRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Untinker Items", client, "makeUntinkerRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Gourd Trading", client, "tradeGourdItems" ) );

		// Add in automatic quest completion scripts.

		JMenu questsMenu = new JMenu( "Quests" );
		container.add( questsMenu );

		questsMenu.add( new InvocationMenuItem( "Unlock Guild", client, "unlockGuildStore" ) );
		questsMenu.add( new InvocationMenuItem( "Tavern Faucet", client, "locateTavernFaucet" ) );

		questsMenu.add( new JSeparator() );

		questsMenu.add( new InvocationMenuItem( "Nemesis Quest", Nemesis.class, "faceNemesis" ) );
		questsMenu.add( new InvocationMenuItem( "Strange Leaflet", StrangeLeaflet.class, "robStrangeLeaflet" ) );

		questsMenu.add( new JSeparator() );

		questsMenu.add( new InvocationMenuItem( "Lair Entryway", SorceressLair.class, "completeEntryway" ) );
		questsMenu.add( new InvocationMenuItem( "Hedge Rotation", SorceressLair.class, "completeHedgeMaze" ) );
		questsMenu.add( new InvocationMenuItem( "Tower Guardians", SorceressLair.class, "fightTowerGuardians" ) );
		questsMenu.add( new InvocationMenuItem( "Final Chamber", SorceressLair.class, "completeSorceressChamber" ) );

		JMenu bookmarkMenu = new BookmarkMenu();
		container.add( bookmarkMenu );

		// Add script and bookmark menus, which use the
		// listener-driven static lists.

		scriptMenu = new ScriptMenu();
		container.add( scriptMenu );

		if ( container instanceof JMenuBar )
		{
			JMenu toggleMenu = new JMenu( "Toggles" );
			container.add( toggleMenu );

			toggleMenu.add( debugMenuItem = new ToggleDebugMenuItem() );
			toggleMenu.add( macroMenuItem = new ToggleMacroMenuItem() );
		}

		// Add help information for KoLmafia.  This includes
		// the additional help-oriented stuffs.

		JMenu helperMenu = new JMenu( "Help" );
		container.add( helperMenu );

		helperMenu.add( new DisplayFrameMenuItem( "Copyright Notice", LicenseDisplay.class ) );
		helperMenu.add( new DisplayFrameMenuItem( "Farmer's Almanac", CalendarFrame.class ) );
		helperMenu.add( new DisplayFrameMenuItem( "KoL Encyclopedia", ExamineItemsFrame.class ) );

		helperMenu.add( new JSeparator() );

		helperMenu.add( new DisplayPageMenuItem( "KoLmafia Thread", "http://forums.kingdomofloathing.com/viewtopic.php?t=19779" ) );
		helperMenu.add( new DisplayPageMenuItem( "Sourceforge Page", "https://sourceforge.net/projects/kolmafia" ) );
		helperMenu.add( new DisplayPageMenuItem( "End-User Manual", "http://kolmafia.sourceforge.net/manual.html" ) );

		helperMenu.add( new JSeparator() );

		helperMenu.add( new DisplayPageMenuItem( "Subjunctive KoL", "http://www.subjunctive.net/kol/FrontPage.html" ) );
		helperMenu.add( new DisplayPageMenuItem( "Jinya's Visual Wiki", "http://www.thekolwiki.net/" ) );
		helperMenu.add( new DisplayPageMenuItem( "Ohayou's Item Effects", "http://www.lysator.liu.se/~jhs/KoL/effects/" ) );
		helperMenu.add( new DisplayPageMenuItem( "Moxie Survival Lookup", "http://kol.network-forums.com/cgi-bin/moxie.cgi" ) );
	}

	public void setEnabled( boolean isEnabled )
	{	this.isEnabled = isEnabled;
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled()
	 * method does not call the superclass's version.
	 *
	 * @return	<code>true</code>
	 */

	public final boolean isEnabled()
	{	return true;
	}

	private class ToggleDebugMenuItem extends JMenuItem implements ActionListener
	{
		public ToggleDebugMenuItem()
		{
			super( JComponentUtilities.getSharedImage( "debug.gif" ) );
			addActionListener( this );
			setText( client == null || KoLmafia.getLogStream() instanceof NullStream ? "Begin recording debug..." : "Stop recording debug" );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
			existingFrames.toArray( frames );

			if ( KoLmafia.getLogStream() instanceof NullStream )
			{
				KoLmafia.openDebugLog();
				for ( int i = 0; i < frames.length; ++i )
					if ( frames[i] != null && frames[i].debugMenuItem != null )
						frames[i].debugMenuItem.setText( "Stop recording debug" );
			}
			else
			{
				KoLmafia.closeDebugLog();
				for ( int i = 0; i < frames.length; ++i )
					if ( frames[i] != null && frames[i].debugMenuItem != null )
						frames[i].debugMenuItem.setText( "Begin recording debug..." );
			}
		}
	}

	private class ToggleMacroMenuItem extends JMenuItem implements ActionListener
	{
		public ToggleMacroMenuItem()
		{
			super( JComponentUtilities.getSharedImage( "command.gif" ) );
			addActionListener( this );
			setText( client == null || client.getMacroStream() instanceof NullStream ? "Begin recording script..." : "Stop recording script" );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
			existingFrames.toArray( frames );

			if ( client != null && client.getMacroStream() instanceof NullStream )
			{
				JFileChooser chooser = new JFileChooser( "scripts" );
				int returnVal = chooser.showSaveDialog( KoLFrame.this );

				if ( chooser.getSelectedFile() == null )
					return;

				String filename = chooser.getSelectedFile().getAbsolutePath();

				if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
					client.openMacroStream( filename );

				for ( int i = 0; i < frames.length; ++i )
					frames[i].macroMenuItem.setText( "Stop recording script" );
			}
			else if ( client != null )
			{
				client.closeMacroStream();
				for ( int i = 0; i < frames.length; ++i )
					frames[i].macroMenuItem.setText( "Begin recording script..." );
			}
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

				client.resetContinueState();
				DEFAULT_SHELL.executeLine( executePath );
			}
			catch ( Exception e )
			{
				// Here, notify the display that the script
				// file specified could not be loaded

				client.cancelRequest();
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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

		public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements )
		{	this( title, confirmedText, cancelledText, elements, true );
		}

		public ItemManagePanel( String title, String confirmedText, String cancelledText, LockableListModel elements, boolean isRootPane )
		{
			super( title, confirmedText, cancelledText, new ShowDescriptionList( elements ), isRootPane );

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

	protected class MultiButtonPanel extends JPanel
	{
		protected boolean showMovers;
		protected JPanel enclosingPanel;
		protected JPanel optionPanel;
		protected LockableListModel elementModel;
		protected ShowDescriptionList elementList;

		protected JButton [] buttons;
		protected JRadioButton [] movers;

		public MultiButtonPanel( String title, LockableListModel elementModel, boolean showMovers )
		{
			existingPanels.add( new WeakReference( this ) );
			this.showMovers = showMovers;
			this.optionPanel = new JPanel();

			this.elementModel = elementModel;
			this.elementList = new ShowDescriptionList( elementModel );

			enclosingPanel = new JPanel( new BorderLayout( 10, 10 ) );
			enclosingPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			enclosingPanel.add( new JScrollPane( elementList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			setLayout( new CardLayout( 10, 0 ) );
			add( enclosingPanel, "" );
		}

		public void setButtons( String [] buttonLabels, ActionListener [] buttonListeners )
		{
			JPanel buttonPanel = new JPanel();
			buttons = new JButton[ buttonLabels.length ];

			for ( int i = 0; i < buttonLabels.length; ++i )
			{
				buttons[i] = new JButton( buttonLabels[i] );
				buttons[i].addActionListener( buttonListeners[i] );
				buttonPanel.add( buttons[i] );
			}

			JPanel moverPanel = new JPanel();

			movers = new JRadioButton[4];
			movers[0] = new JRadioButton( "Move all", true );
			movers[1] = new JRadioButton( "Move all but one" );
			movers[2] = new JRadioButton( "Move multiple" );
			movers[3] = new JRadioButton( "Move exactly one" );

			ButtonGroup moverGroup = new ButtonGroup();
			for ( int i = 0; i < 4; ++i )
			{
				moverGroup.add( movers[i] );
				if ( showMovers )
					moverPanel.add( movers[i] );
			}

			JPanel northPanel = new JPanel( new BorderLayout() );
			northPanel.add( buttonPanel, BorderLayout.SOUTH );
			northPanel.add( moverPanel, BorderLayout.CENTER );
			northPanel.add( optionPanel, BorderLayout.NORTH );

			enclosingPanel.add( northPanel, BorderLayout.NORTH );
		}

		public void setEnabled( boolean isEnabled )
		{
			elementList.setEnabled( isEnabled );
			for ( int i = 0; i < buttons.length; ++i )
				buttons[i].setEnabled( isEnabled );

			for ( int i = 0; i < movers.length; ++i )
				movers[i].setEnabled( isEnabled );
		}

		protected Object [] getDesiredItems( String message )
		{
			Object [] items = elementList.getSelectedValues();
			if ( items.length == 0 )
				return null;

			int neededSize = items.length;
			AdventureResult currentItem;

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];

				int quantity = movers[0].isSelected() ? currentItem.getCount() : movers[1].isSelected() ?
					currentItem.getCount() - 1 : movers[2].isSelected() ? getQuantity( message + " " + currentItem.getName() + "...", currentItem.getCount() ) : 1;

				// If the user manually enters zero, return from
				// this, since they probably wanted to cancel.

				if ( quantity == 0 && movers[2].isSelected() )
					return null;

				// Otherwise, if it was not a manual entry, then reset
				// the entry to null so that it can be re-processed.

				if ( quantity == 0 )
				{
					items[i] = null;
					--neededSize;
				}
				else
				{
					items[i] = currentItem.getInstance( quantity );
				}
			}

			// If none of the array entries were nulled,
			// then return the array as-is.

			if ( neededSize == items.length )
				return items;

			// Otherwise, shrink the array which will be
			// returned so that it removes any nulled values.

			Object [] desiredItems = new Object[ neededSize ];
			neededSize = 0;

			for ( int i = 0; i < items.length; ++i )
				if ( items[i] != null )
					desiredItems[ neededSize++ ] = items[i];

			return desiredItems;
		}
	}

	protected void processWindowEvent( WindowEvent e )
	{
		if ( e.getID() == WindowEvent.WINDOW_CLOSING )
		{
			Point p = getLocation();
			Dimension screenSize = TOOLKIT.getScreenSize();

			if ( p.getX() > 0 && p.getY() > 0 && p.getX() + getWidth() < screenSize.getWidth() && p.getY() + getHeight() < screenSize.getHeight() )
			{
				KoLSettings settings = GLOBAL_SETTINGS.getProperty( "windowPositions" ).equals( "1" ) ? GLOBAL_SETTINGS : StaticEntity.getSettings();
				settings.setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
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

		public DisplayFrameButton( String text, Class frameClass )
		{
			super( text );

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
		{	openSystemBrowser( location );
		}
	}

	protected static void openSystemBrowser( String location )
	{
		try
		{
			BrowserLauncher.openURL( location );
		}
		catch ( java.io.IOException e )
		{
			KoLmafia.getLogStream().println( "Failed to open browser:" );

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
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
			client.setCurrentRequest( null );
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

		public InvocationMenuItem( String title, Object object, String methodName )
		{
			this( title, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationMenuItem( String title, Class c, String methodName )
		{
			super( title );
			addActionListener( this );

			if ( c == null )
				return;

			try
			{
				this.object = object;
				this.method = c.getMethod( methodName, NOPARAMS );
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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
					boolean wasEnabled = client.currentState != DISABLE_STATE;

					client.disableDisplay();
					client.resetContinueState();
					method.invoke( object, null );

					if ( wasEnabled )
						client.enableDisplay();
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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

		public InvocationButton( String text, Object object, String methodName )
		{
			this( text, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String text, Class c, String methodName )
		{
			super( text );
			this.object = c;

			completeConstruction( c, methodName );
		}

		public InvocationButton( String tooltip, String icon, Object object, String methodName )
		{
			this( tooltip, icon, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationButton( String tooltip, String icon, Class c, String methodName )
		{
			super( JComponentUtilities.getSharedImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );

			this.object = c;
			setToolTipText( tooltip );
			completeConstruction( c, methodName );
		}

		private void completeConstruction( Class c, String methodName )
		{
			addActionListener( this );

			try
			{
				this.method = c.getMethod( methodName, NOPARAMS );
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
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
		String location = request.getURLString();

		if ( this instanceof RequestFrame )
		{
			if ( !location.startsWith( "search" ) && !location.startsWith( "desc" ) && ((RequestFrame)this).hasSideBar() )
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

	protected class KoLPanelFrameButton extends JButton implements ActionListener
	{
		private CreateFrameRunnable creator;

		public KoLPanelFrameButton( String tooltip, String icon, ActionPanel panel )
		{
			super( JComponentUtilities.getSharedImage( icon ) );
			JComponentUtilities.setComponentSize( this, 32, 32 );
			setToolTipText( tooltip );
			addActionListener( this );

			Object [] parameters = new Object[3];
			parameters[0] = client;
			parameters[1] = tooltip;
			parameters[2] = panel;

			creator = new CreateFrameRunnable( KoLPanelFrame.class, parameters );
		}

		public void actionPerformed( ActionEvent e )
		{	creator.run();
		}
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
			JPanel versionPanel = new JPanel( new BorderLayout( 20, 20 ) );
			versionPanel.add( new JLabel( JComponentUtilities.getSharedImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.NORTH );

			JPanel labelPanel = new JPanel( new GridLayout( versionData.length, 1 ) );
			for ( int i = 0; i < versionData.length; ++i )
				labelPanel.add( new JLabel( versionData[i], JLabel.CENTER ) );

			versionPanel.add( labelPanel, BorderLayout.CENTER );

			JButton donateButton = new JButton( JComponentUtilities.getSharedImage( "paypal.gif" ) );
			JComponentUtilities.setComponentSize( donateButton, 74, 31 );
			donateButton.addActionListener( new DisplayPageMenuItem( "", "http://sourceforge.net/donate/index.php?user_id=813949" ) );

			JPanel donatePanel = new JPanel();
			donatePanel.add( donateButton );

			JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
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
		{
			super( "Scripts", scripts );

			File scriptDirectory = new File( "scripts" );

			if ( !scriptDirectory.exists() )
				scriptDirectory.mkdirs();

			compileScripts();
		}

		public JComponent constructMenuItem( Object o )
		{	return o instanceof JSeparator ? new JSeparator() : constructMenuItem( (File) o, "scripts" );
		}

		private JComponent constructMenuItem( File file, String prefix )
		{
			// Get path components of this file
			String [] pieces;

			try
			{
				pieces = file.getCanonicalPath().split( "[\\\\/]" );
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();

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
				File [] scriptList = file.listFiles();

				//  Convert the list into a menu
				JMenu menu = new JMenu( name );

				// Iterate through the files.  Do this in two
				// passes to make sure that directories start
				// up top, followed by non-directories.

				boolean hasDirectories = false;

				for ( int i = 0; i < scriptList.length; ++i )
				{
					if ( scriptList[i].isDirectory() )
					{
						menu.add( constructMenuItem( scriptList[i], path ) );
						hasDirectories = true;
					}
				}

				if ( hasDirectories )
					menu.add( new JSeparator() );

				for ( int i = 0; i < scriptList.length; ++i )
					if ( !scriptList[i].isDirectory() )
						menu.add( constructMenuItem( scriptList[i], path ) );

				// Return the menu
				return menu;
			}

			return new LoadScriptMenuItem( name, path );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[2];

			headers[0] = new LoadScriptMenuItem();
			headers[1] = new InvocationMenuItem( "Refresh menu", KoLFrame.this, "compileScripts" );

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

	protected class RequestButton extends JButton implements ActionListener
	{
		private KoLRequest request;

		public RequestButton( String title, KoLRequest request )
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
			String currentValue = field.getText();

			if ( currentValue == null || currentValue.length() == 0 )
				return defaultValue;

			if ( currentValue.equals( "*" ) )
				return defaultValue;

			return df.parse( field.getText().trim() ).intValue();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return 0;
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

			if ( currentValue.equals( "*" ) )
				return maximumValue;

			int desiredValue = df.parse( currentValue ).intValue();
			return Math.max( 0, Math.min( desiredValue, maximumValue ) );
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

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

		public JComponent constructMenuItem( Object o )
		{
			String [] bookmarkData = ((String)o).split( "\\|" );

			String name = bookmarkData[0];
			String location = bookmarkData[1];
			String pwdhash = bookmarkData[2];

			if ( pwdhash.equals( "true" ) )
				location += "&pwd=" + client.getPasswordHash();

			return new DisplayRequestMenuItem( name, location );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[6];

			headers[0] = new AddBookmarkMenuItem();
			headers[1] = new KoLPanelFrameMenuItem( "Manage Bookmarks", new BookmarkManagePanel() );
			headers[2] = new JSeparator();

			if ( KoLFrame.this instanceof RequestFrame )
				headers[3] = new DisplayRequestMenuItem( "Council of Loathing", "council.php" );
			else
				headers[3] = new DisplayFrameMenuItem( "Council of Loathing", CouncilFrame.class );

			headers[4] = new DisplayRequestMenuItem( "Weird Records Board", "records.php?which=0" );
			headers[5] = new DisplayRequestMenuItem( "View Mall Store Log", "storelog.php" );

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
				super( KoLFrame.this, "Add a KoL-relative bookmark!" );
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
					nameField = new JTextField();
					elements[0] = new VerifiableElement( "Name", nameField );

					locationField = new JTextField( client.getCurrentRequest() == null ? "" : client.getCurrentRequest().getURLString() );
					elements[1] = new VerifiableElement( "Location", locationField );
					setContent( elements );
				}

				public void actionConfirmed()
				{
					// Strip pwdhash out of location and
					// set third parameter correctly
					bookmarks.add( nameField.getText() + "|" + locationField.getText() + "|false" );
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
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
				return;

			String currentItem = (String)elementList.getSelectedValue();
			if ( currentItem == null )
				return;

			String [] bookmarkData = currentItem.split( "\\|" );

			String name = bookmarkData[0];
			String location = bookmarkData[1];
			String pwdhash = bookmarkData[2];

			String newName = JOptionPane.showInputDialog( "Name your bookmark?", name );

			if ( newName == null )
				return;

			bookmarks.remove( index );
			bookmarks.add( newName + "|" + location + "|" + pwdhash );
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

	public static void compileScripts()
	{
		scripts.clear();
		File directory = new File( "scripts" );

		// Get the list of files in the current directory
		File [] scriptList = directory.listFiles( BACKUP_FILTER );

		// Iterate through the files.  Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		boolean hasDirectories = false;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( scriptList[i].isDirectory() )
			{
				scripts.add( scriptList[i] );
				hasDirectories = true;
			}
		}

		if ( hasDirectories )
			scripts.add( new JSeparator() );

		for ( int i = 0; i < scriptList.length; ++i )
			if ( !scriptList[i].isDirectory() )
				scripts.add( scriptList[i] );
	}

	/**
	 * Utility method to compile the list of bookmarks based on the
	 * current server settings.
	 */

	protected void compileBookmarks()
	{
		// Read the setting into the list only once.  The list and
		// setting are subsequently kept in synch.
		if ( bookmarksCompiled )
			return;

		bookmarksCompiled = true;

		String [] bookmarkData = GLOBAL_SETTINGS.getProperty( "browserBookmarks" ).split( "\\|" );
		String name, location, pwdhash;

		if ( bookmarkData.length > 1 )
			for ( int i = 0; i < bookmarkData.length; ++i )
				bookmarks.add( bookmarkData[i] + "|" + bookmarkData[++i] + "|" + bookmarkData[++i] );
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings
	 * file.  This should be called after every update.
	 */

	protected void saveBookmarks()
	{
		StringBuffer bookmarkData = new StringBuffer();

		for ( int i = 0; i < bookmarks.size(); ++i )
		{
			if ( i > 0 )
				bookmarkData.append( '|' );
			bookmarkData.append( (String)bookmarks.get(i) );
		}

		GLOBAL_SETTINGS.setProperty( "browserBookmarks", bookmarkData.toString() );
	}

	/**
	 * An internal class which represents the panel used for adding
	 * effects to a character (yourself or others).
	 */

	protected class SkillBuffPanel extends LabeledKoLPanel
	{
		private JComboBox skillSelect;
		private JComboBox targetSelect;
		private JTextField countField;

		public SkillBuffPanel()
		{	this ( "" );
		}

		public SkillBuffPanel( String initialRecipient )
		{
			super( "Got Skills?", "cast", "maxcast", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			skillSelect = new JComboBox( client == null ? new LockableListModel() : KoLCharacter.getUsableSkills() );
			targetSelect = new MutableComboBox( client == null ? new SortedListModel() : (SortedListModel) client.getContactList().clone() );

			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Skill Name: ", skillSelect );
			elements[1] = new VerifiableElement( "The Victim: ", targetSelect );
			elements[2] = new VerifiableElement( "# of Times: ", countField );
			setContent( elements, true, true );
			setDefaultButton( confirmedButton );

			if ( !initialRecipient.equals( "" ) )
			{
				targetSelect.addItem( initialRecipient );
				targetSelect.getEditor().setItem( initialRecipient );
				targetSelect.setSelectedItem( initialRecipient );
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			skillSelect.setEnabled( isEnabled );
			targetSelect.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{	buff( false );
		}

		protected void actionCancelled()
		{	buff( true );
		}

		private void buff( boolean maxBuff )
		{
			String buffName = ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName();
			if ( buffName == null )
				return;

			String [] targets = client.extractTargets( (String) targetSelect.getSelectedItem() );

			int buffCount = maxBuff ?
				(int) ( KoLCharacter.getCurrentMP() /
					ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( buffName ) ) ) : getValue( countField, 1 );

			Runnable [] requests;

			if ( targets.length == 0 )
			{
				requests = new Runnable[1];
				requests[0] = new UseSkillRequest( client, buffName, "", buffCount );
			}
			else
			{
				requests = new Runnable[ targets.length ];
				for ( int i = 0; i < requests.length && client.permitsContinue(); ++i )
					if ( targets[i] != null )
						requests[i] = new UseSkillRequest( client, buffName, targets[i], buffCount );
			}

			(new RequestThread( requests )).start();
		}
	}

	private class StopEverythingItem extends JMenuItem implements ActionListener
	{
		public StopEverythingItem()
		{
			super( "Stop Everything" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( client != null )
				client.updateDisplay( ABORT_STATE, "KoLmafia declares world peace." );
		}
	}

	protected class FilterCheckBox extends JCheckBox implements ActionListener
	{
		private JCheckBox [] filters;
		private ShowDescriptionList elementList;

		public FilterCheckBox( JCheckBox [] filters, ShowDescriptionList elementList, String label, boolean isSelected )
		{
			super( label, isSelected );
			addActionListener( this );

			this.filters = filters;
			this.elementList = elementList;
		}

		public void actionPerformed( ActionEvent e )
		{
			elementList.setCellRenderer( AdventureResult.getConsumableCellRenderer( filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected() ) );
			elementList.updateUI();
		}
	}

	private class UnanimatedLabel extends JLabel
	{
		public UnanimatedLabel()
		{	super( " ", null, CENTER );
		}

		public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height )
		{
			if ( infoflags == FRAMEBITS )
				return true;

			super.imageUpdate( img, infoflags, x, y, width, height );
			return true;
		}
	}
}
