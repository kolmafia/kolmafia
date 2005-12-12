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
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

// basic utilities
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

	private static final String [] LICENSE_FILENAME = { "kolmafia-license.gif", "spellcast-license.gif", "browserlauncher-license.htm" };
	private static final String [] LICENSE_NAME = { "KoLmafia BSD", "Spellcast BSD", "BrowserLauncher" };

	private String frameName;
	protected boolean isEnabled;
	protected KoLPanel contentPanel;
	protected JCheckBoxMenuItem [] consumeFilter;

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

		KoLFrame.client = client;
		this.isEnabled = true;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		addWindowListener( new LocationAdapter() );

		this.frameName = getClass().getName();
		this.frameName = frameName.substring( frameName.lastIndexOf( "." ) + 1 );

		if ( !(this instanceof LoginFrame || this instanceof AdventureFrame) )
			existingFrames.add( this );

		// All frames will have to access the same menu bar for consistency.
		// Later on, all menu items not added by default will be placed onto
		// the panel for increased visibility.

		addMenuBar();
	}

	public void dispose()
	{
		super.dispose();

		if ( !(this instanceof LoginFrame || this instanceof AdventureFrame) )
		{
			Object [] frames = existingFrames.toArray();

			for ( int i = frames.length - 1; i >= 0; --i )
				if ( frames[i] == this )
					existingFrames.remove(i);
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
		if ( getContentPane().getComponentCount() != 0 )
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

		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add( this.compactPane, BorderLayout.WEST );
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
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		addStatusMenu( menuBar );
		addTravelMenu( menuBar );
		addPeopleMenu( menuBar );
		addScriptMenu( menuBar );
		addOptionsMenu( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * Utility method which encapsulates the addition of
	 * things to the standard status menu.
	 */

	protected final JMenu addStatusMenu( JComponent menu )
	{
		JMenu statusMenu = new JMenu( "My KoL" );
		menu.add( statusMenu );

		if ( client == null || !client.inLoginState() )
		{
			statusMenu.add( new MiniBrowserMenuItem( "Navigate Map", "main.php", true ) );
			statusMenu.add( new DisplayFrameMenuItem( "Visit Council", CouncilFrame.class ) );
			statusMenu.add( new MiniBrowserMenuItem( "Weird Records", "records.php?which=0", false ) );
			statusMenu.add( new JSeparator() );
		}

		statusMenu.add( new DisplayFrameMenuItem( "KoL Almanac", CalendarFrame.class ) );
		statusMenu.add( new DisplayFrameMenuItem( "Graphical CLI", CommandDisplayFrame.class ) );

		if ( client == null || !client.inLoginState() )
		{
			statusMenu.add( new JSeparator() );

			statusMenu.add( new DisplayFrameMenuItem( "Status Pane", CharsheetFrame.class ) );
			statusMenu.add( new DisplayFrameMenuItem( "Gear Changer", GearChangeFrame.class ) );
			statusMenu.add( new DisplayFrameMenuItem( "Item Manager", ItemManageFrame.class ) );
			statusMenu.add( new DisplayFrameMenuItem( "Familiar Trainer", FamiliarTrainingFrame.class ) );

			statusMenu.add( new JSeparator() );

			statusMenu.add( new DisplayFrameMenuItem( "Your Mall Store", StoreManageFrame.class ) );
			statusMenu.add( new DisplayFrameMenuItem( "Museum Display", MuseumFrame.class ) );
			statusMenu.add( new DisplayFrameMenuItem( "Hagnk's Storage", HagnkStorageFrame.class ) );
		}

		return statusMenu;
	}

	/**
	 * Utility method which encapsulates the addition of
	 * things to the travel menu.
	 */

	protected final JMenu addTravelMenu( JComponent menu )
	{
		JMenu travelMenu = new JMenu( "Travel" );
		menu.add( travelMenu );

		travelMenu.add( new InvocationMenuItem( "Eat Cake-Arena", client, "visitCakeShapedArena" ) );
		travelMenu.add( new InvocationMenuItem( "Loot the Hermit", client, "makeHermitRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Mountain Traps", client, "makeTrapperRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Bounty Hunter", client, "makeHunterRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Untinker Items", client, "makeUntinkerRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Doc Galaktik", client, "makeGalaktikRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Canadian Device", client, "makeMindControlRequest" ) );
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
		commMenu.add( new DisplayFrameMenuItem( "Run a KoL BuffBot", BuffBotFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Patronize a BuffBot", BuffRequestFrame.class ) );

		commMenu.add( new JSeparator() );

		commMenu.add( new DisplayFrameMenuItem( "Write a New K-mail", GreenMessageFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Propose a New Trade", ProposeTradeFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "View Pending Offers", PendingTradesFrame.class ) );
		commMenu.add( new DisplayFrameMenuItem( "Gift Shop Back Room", GiftMessageFrame.class ) );

		return commMenu;
	}

	protected final JMenu addConsumeMenu( JComponent menu )
	{
		JMenu consumeMenu = new JMenu( "Consumables" );
		menu.add( consumeMenu );

		consumeFilter = new JCheckBoxMenuItem[3];

		consumeFilter[0] = new FilterMenuItem( "Show food", KoLCharacter.canEat() );
		consumeFilter[1] = new FilterMenuItem( "Show booze", KoLCharacter.canDrink() );
		consumeFilter[2] = new FilterMenuItem( "Show others", true );

		for ( int i = 0; i < consumeFilter.length; ++i )
			consumeMenu.add( consumeFilter[i] );

		return consumeMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * scripting menu to the given menu bar.
	 */

	protected final JMenu addScriptMenu( JComponent menu )
	{
		JMenu scriptMenu = new ScriptMenu();
		menu.add( scriptMenu );
		return scriptMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * configuration menu to the given menu bar.  The default menu
	 * contains the ability to customize preferences (global if it
	 * is invoked before login, character-specific if after) and
	 * initialize the debugger.
	 *
	 * @param	menu	The <code>JMenuBar</code> to which the configuration menu will be attached
	 */

	protected final JMenu addOptionsMenu( JComponent menu )
	{
		JMenu optionsMenu = new JMenu( "Options" );
		menu.add( optionsMenu );

		optionsMenu.add( new DisplayFrameMenuItem( "Preferences", OptionsFrame.class ) );
		optionsMenu.add( new ToggleDebugMenuItem() );
		optionsMenu.add( new InvocationMenuItem( "Clear Results", client, "resetSessionTally" ) );
		optionsMenu.add( new InvocationMenuItem( "Session Time-In", client, "executeTimeInRequest" ) );

		return optionsMenu;
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code> Help
	 * menu to the given menu bar.  The default Help menu contains the
	 * copyright statement for <code>KoLmafia</code>.
	 *
	 * @param	menu	The <code>JMenuBar</code> to which the Help menu will be attached
	 */

	protected final JMenu addHelpMenu( JComponent menu )
	{
		JMenu helpMenu = new JMenu( "Help" );
		menu.add( helpMenu );

		helpMenu.add( new DisplayFrameMenuItem( "About KoLmafia...", LicenseDisplay.class ) );
		helpMenu.add( new DisplayPageMenuItem( "KoLmafia Home", "http://kolmafia.sourceforge.net/" ) );
		helpMenu.add( new DisplayPageMenuItem( "End-User Manual", "http://kolmafia.sourceforge.net/manual.html" ) );
		helpMenu.add( new DisplayPageMenuItem( "Sourceforge Page", "https://sourceforge.net/project/showfiles.php?group_id=126572&package_id=138474" ) );
		helpMenu.add( new DisplayPageMenuItem( "Read Forum Thread", "http://forums.kingdomofloathing.com/viewtopic.php?t=19779" ) );


		if ( client == null || !client.inLoginState() )
		{
			helpMenu.add( new JSeparator() );
			helpMenu.add( new DisplayFrameMenuItem( "KoL Encyclopedia", ExamineItemsFrame.class ) );
		}

		return helpMenu;
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

			setText( client == null || client.getMacroStream() instanceof NullStream ? "Record Script..." : "Stop Recording" );
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

				setText( "Stop Recording" );
			}
			else if ( client != null )
			{
				client.closeMacroStream();
				setText( "Record Script..." );
			}
		}
	}

	/**
	 * Internal class which attempts to create a menu item
	 * which toggles the text pending on current debug state.
	 */

	private class ToggleDebugMenuItem extends JMenuItem implements ActionListener
	{
		public ToggleDebugMenuItem()
		{
			super( "" );
			addActionListener( this );

			setText( KoLmafia.getLogStream() instanceof NullStream ? "Start Debug" : "Stop Debug" );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( KoLmafia.getLogStream() instanceof NullStream )
			{
				KoLmafia.openDebugLog();
				setText( "Stop Debug" );
			}
			else if ( client != null )
			{
				KoLmafia.closeDebugLog();
				setText( "Start Debug" );
			}
		}
	}

	private class RefreshScriptsMenuItem extends JMenuItem implements ActionListener
	{
		public RefreshScriptsMenuItem()
		{
			super( "Refresh Script Menu" );
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

				if ( client.permitsContinue() )
					updateDisplay( ENABLE_STATE, "" );
				else
					updateDisplay( ERROR_STATE, "" );
			}
			catch ( Exception e )
			{
				// Here, notify the display that the script
				// file specified could not be loaded

				client.updateDisplay( ERROR_STATE, "Script \"" + executePath + "\" could not be loaded." );
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );

				return;
			}

			if ( client.permitsContinue() )
				client.updateDisplay( NORMAL_STATE, "Script completed successfully." );

			setEnabled( true );
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the client's current settings.
	 */

	protected abstract class LabeledKoLPanel extends KoLPanel
	{
		private String panelTitle;
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		public LabeledKoLPanel( String panelTitle, Dimension left, Dimension right )
		{	this( panelTitle, "apply", "defaults", left, right );
		}

		public LabeledKoLPanel( String panelTitle, String confirmButton, String cancelButton, Dimension left, Dimension right )
		{
			super( confirmButton, cancelButton, left, right, true );
			this.panelTitle = panelTitle;
		}

		protected void setContent( VerifiableElement [] elements )
		{	setContent( elements, true );
		}

		protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding )
		{	setContent( elements, isLabelPreceeding, false );
		}

		protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding, boolean bothDisabledOnClick )
		{
			super.setContent( elements, null, null, isLabelPreceeding, bothDisabledOnClick );

			if ( panelTitle != null )
				add( JComponentUtilities.createLabel( panelTitle, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( int displayState, String s )
		{
			if ( !s.equals( "" ) )
				actionStatusLabel.setText( s );
		}

		protected void actionCancelled()
		{
		}

		public void requestFocus()
		{
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

			elementList = (ShowDescriptionList) getScrollComponent();
			elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			elementList.setVisibleRowCount( 8 );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			elementList.setEnabled( isEnabled );
		}
	}

	/**
	 * An internal class which creates a panel which displays
	 * a generic scroll pane.  Note that the code for this
	 * frame was lifted from the ActionVerifyPanel found in
	 * the Spellcast package.
	 */

	protected abstract class LabeledScrollPanel extends ActionPanel
	{
		private JComponent scrollComponent;
		private VerifyButtonPanel buttonPanel;

		public LabeledScrollPanel( String title, String confirmedText, String cancelledText, JComponent scrollComponent )
		{
			this.scrollComponent = scrollComponent;

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout() );

			centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( scrollComponent, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText );
			buttonPanel.setBothDisabledOnClick( true );

			JPanel actualPanel = new JPanel();
			actualPanel.setLayout( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );
			actualPanel.add( buttonPanel, BorderLayout.EAST );

			setLayout( new CardLayout( 10, 10 ) );
			add( actualPanel, "" );
			buttonPanel.setBothDisabledOnClick( true );
		}

		public JComponent getScrollComponent()
		{	return scrollComponent;
		}

		protected abstract void actionConfirmed();
		protected abstract void actionCancelled();

		public void setEnabled( boolean isEnabled )
		{
			scrollComponent.setEnabled( isEnabled );
			buttonPanel.setEnabled( isEnabled );
		}
	}

	/**
	 * In order to do the things we need to do when windows are closed,
	 * this internal class is used to listen for window events.
	 */

	private class LocationAdapter extends WindowAdapter
	{
		public void windowClosing( WindowEvent e )
		{
			if ( client != null && client.getSettings().getProperty( "savePositions" ).equals( "true" ) )
			{
				Point p = getLocationOnScreen();
				client.getSettings().setProperty( frameName, ((int)p.getX()) + "," + ((int)p.getY()) );
				client.getSettings().saveSettings();
			}
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

	protected class KoLHyperlinkAdapter implements HyperlinkListener
	{
		public void hyperlinkUpdate( HyperlinkEvent e )
		{
			if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
			{
				String location = e.getDescription();

				if ( location.startsWith( "http://" ) || location.startsWith( "https://" ) )
				{
					// Attempt to open the URL on the system's default
					// browser.  This could theoretically cause problems,
					// but for now, let's just do a try-catch and cross
					// our fingers.

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
				else if ( location.startsWith( "javascript:" ) && (location.indexOf( "submit()" ) == -1 || location.indexOf( "messageform" ) != -1) )
				{
					// The default editor pane does not handle
					// Javascript links.  Adding support would
					// be an unnecessary time investment.

					JOptionPane.showMessageDialog( null, "Ironically, Java does not support Javascript." );
				}
				else if ( location.indexOf( "submit()" ) == -1 )
				{
					// If it's a link internal to KoL, handle the
					// internal link.  Note that by default, this
					// method does nothing, but descending classes
					// can change this behavior.

					handleInternalLink( location );
				}
				else
				{
					// If it's an attempt to submit an adventure form,
					// examine the location string to see which form is
					// being submitted and submit it manually.

					String [] locationSplit = location.split( "\\." );
					String formID = "\"" + locationSplit[ locationSplit.length - 2 ] + "\"";

					String editorText = ((JEditorPane)e.getSource()).getText();
					int formIndex =  editorText.indexOf( formID );

					String locationText = editorText.substring( editorText.lastIndexOf( "<form", formIndex ),
						editorText.toLowerCase().indexOf( "</form>", formIndex ) );

					Matcher inputMatcher = Pattern.compile( "<input.*?>" ).matcher( locationText );

					Pattern [] actionPatterns = new Pattern[3];
					Pattern [] namePatterns = new Pattern[3];
					Pattern [] valuePatterns = new Pattern[3];

					actionPatterns[0] = Pattern.compile( "action=\"(.*?)\"" );
					namePatterns[0] = Pattern.compile( "name=\"(.*?)\"" );
					valuePatterns[0]  = Pattern.compile( "value=\"(.*?)\"" );

					actionPatterns[1] = Pattern.compile( "action=\'(.*?)\'" );
					namePatterns[1] = Pattern.compile( "name=\'(.*?)\'" );
					valuePatterns[1]  = Pattern.compile( "value=\'(.*?)\'" );

					actionPatterns[2] = Pattern.compile( "action=([^\\s]*?)" );
					namePatterns[2] = Pattern.compile( "name=([^\\s]*?)" );
					valuePatterns[2] = Pattern.compile( "value=([^\\s]*?)" );

					String lastInput;
					int patternIndex;
					Matcher actionMatcher, nameMatcher, valueMatcher;
					StringBuffer inputString = new StringBuffer();

					// Determine the action associated with the
					// form -- this is used for the URL.

					patternIndex = 0;
					do
					{
						actionMatcher = actionPatterns[patternIndex].matcher( locationText );
					}
					while ( !actionMatcher.find() && ++patternIndex < 3 );

					// Figure out which inputs need to be submitted.
					// This is determined through the existing HTML,
					// looking at preset values only.

					while ( inputMatcher.find() )
					{
						lastInput = inputMatcher.group();

						// Each input has a name associated with it.
						// This should be determined first.

						patternIndex = 0;
						do
						{
							nameMatcher = namePatterns[patternIndex].matcher( lastInput );
						}
						while ( !nameMatcher.find() && ++patternIndex < 3 );

						// Each input has a name associated with it.
						// This should be determined next.

						patternIndex = 0;
						do
						{
							valueMatcher = valuePatterns[patternIndex].matcher( lastInput );
						}
						while ( !valueMatcher.find() && ++patternIndex < 3 );

						// Append the latest input's name and value to
						// the complete input string.

						inputString.append( inputString.length() == 0 ? '?' : '&' );

						try
						{
							inputString.append( URLEncoder.encode( nameMatcher.group(1), "UTF-8" ) );
							inputString.append( '=' );
							inputString.append( URLEncoder.encode( valueMatcher.group(1), "UTF-8" ) );
						}
						catch ( Exception e2 )
						{
						}
					}

					// Now that the entire form string is known, handle
					// the appropriate internal link.

					handleInternalLink( actionMatcher.group(1) + inputString.toString() );
				}
			}
		}

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

	protected class MiniBrowserMenuItem extends JMenuItem implements ActionListener
	{
		private String location;
		private boolean useSavedRequest;

		public MiniBrowserMenuItem( String title, String location, boolean useSavedRequest )
		{
			super( title );
			addActionListener( this );

			this.location = location;
			this.useSavedRequest = useSavedRequest;
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( useSavedRequest )
			{
				KoLRequest request = client.getCurrentRequest();
				if ( request != null )
				{
					client.setCurrentRequest( null );
					openRequestFrame( request );
					return;
				}
			}
			openRequestFrame( location );
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
		{
			super( title );
			addActionListener( this );

			try
			{
				this.object = object;
				this.method = c.getMethod( methodName, NOPARAMS );
				this.requiresCancel = methodName.equals( "executeTimeInRequest" );
			}
			catch ( Exception e )
			{
				System.out.println( c );
				System.out.println( methodName );
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

	public void refreshFilters()
	{
	}

	protected class FilterMenuItem extends JCheckBoxMenuItem implements ActionListener
	{
		public FilterMenuItem( String name )
		{	this( name, true );
		}

		public FilterMenuItem( String name, boolean isSelected )
		{
			super( name );
			setSelected( isSelected );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	refreshFilters();
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
			JComponent [] headers = new JComponent[ KoLFrame.this instanceof AdventureFrame ? 5 : 3 ];

			headers[0] = new LoadScriptMenuItem();
			headers[1] = new ToggleMacroMenuItem();
			headers[2] = new RefreshScriptsMenuItem();

			if ( KoLFrame.this instanceof AdventureFrame )
			{
				headers[3] = new JSeparator();

				JMenu modulesMenu = new JMenu( "Built-In Scripts" );

				modulesMenu.add( new InvocationMenuItem( "Get Breakfast!", client, "getBreakfast" ) );
				modulesMenu.add( new InvocationMenuItem( "Pwn Clan Otori!", client, "pwnClanOtori" ) );

				modulesMenu.add( new JSeparator() );

				modulesMenu.add( new InvocationMenuItem( "Face Nemesis", Nemesis.class, "faceNemesis" ) );
				modulesMenu.add( new InvocationMenuItem( "Rob Strange Leaflet", StrangeLeaflet.class, "robStrangeLeaflet" ) );

				modulesMenu.add( new JSeparator() );

				modulesMenu.add( new InvocationMenuItem( "Lair Entryway", SorceressLair.class, "completeEntryway" ) );
				modulesMenu.add( new InvocationMenuItem( "Hedge Rotation", SorceressLair.class, "completeHedgeMaze" ) );
				modulesMenu.add( new InvocationMenuItem( "Tower Guardians", SorceressLair.class, "fightTowerGuardians" ) );
				modulesMenu.add( new InvocationMenuItem( "Naughty Chamber", SorceressLair.class, "completeSorceressChamber" ) );

				headers[4] = modulesMenu;
			}

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

	/**
	 * A special class which renders the menu holding the list of menu items
	 * Tsynchronized to a lockable list model.
	 */

	protected abstract class MenuItemList extends JMenu implements ListDataListener
	{
		private int headerCount;
		private LockableListModel model;

		public MenuItemList( String title, LockableListModel model )
		{
			super( title );

			// Add the headers to the list of items which
			// need to be added.

			JComponent [] headers = getHeaders();

			for ( int i = 0; i < headers.length; ++i )
				this.add( headers[i] );

			// Add a separator between the headers and the
			// elements displayed in the list.  Also go
			// ahead and initialize the header count.

			this.add( new JSeparator() );
			this.headerCount = headers.length + 1;

			// Now, add everything that's contained inside of
			// the current list.

			for ( int i = 0; i < model.size(); ++i )
				this.add( (JComponent) model.get(i) );

			// Add this as a listener to the list of so that
			// the menu gets updated whenever the list updates.

			model.addListDataListener( this );
		}

		public abstract JComponent [] getHeaders();

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalAdded( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 >= source.size() || source.size() + headerCount == getMenuComponentCount() )
				return;

			for ( int i = index0; i <= index1; ++i )
				add( (JComponent) source.get(i), i + headerCount );

			validate();
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void intervalRemoved( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 + headerCount >= getMenuComponentCount() || source.size() + headerCount == getMenuComponentCount() )
				return;

			for ( int i = index1; i >= index0; --i )
				remove( i + headerCount );

			validate();
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public synchronized void contentsChanged( ListDataEvent e )
		{
			LockableListModel source = (LockableListModel) e.getSource();
			int index0 = e.getIndex0();  int index1 = e.getIndex1();

			if ( index1 + headerCount >= getMenuComponentCount() || source.size() + headerCount == getMenuComponentCount() )
				return;

			for ( int i = index1; i >= index0; --i )
			{
				remove( i + headerCount );
				add( (JComponent) source.get(i), i + headerCount );
			}

			validate();
		}
	}

	/**
	 * Utility class used to forward events to JButtons enclosed inside
	 * of a JTable object.
	 */

	protected class ButtonEventListener extends MouseAdapter
	{
		private JTable table;

		public ButtonEventListener( JTable table )
		{	this.table = table;
		}

		public void mouseReleased( MouseEvent e )
		{
		    TableColumnModel columnModel = table.getColumnModel();

		    int row = e.getY() / table.getRowHeight();
		    int column = columnModel.getColumnIndexAtX( e.getX() );

			if ( row >= 0 && row < table.getRowCount() && column >= 0 && column < table.getColumnCount() )
			{
				Object value = table.getValueAt( row, column );

				if ( value instanceof JButton )
				{
					((JButton) value).dispatchEvent( SwingUtilities.convertMouseEvent( table, e, (JButton) value ) );
					table.repaint();
				}
			}
		}
	}

	protected class MouseListeningButton extends JButton implements MouseListener
	{
		public MouseListeningButton( ImageIcon icon )
		{
			super( icon );
			addMouseListener( this );
		}

		public void mouseClicked( MouseEvent e )
		{
		}

		public void mouseEntered( MouseEvent e )
		{
		}

		public void mouseExited( MouseEvent e )
		{
		}

		public void mousePressed( MouseEvent e )
		{
		}

		public void mouseReleased( MouseEvent e )
		{
		}
	}

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	protected class LogoutRequestAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			StaticEntity.closeSession();
			KoLMessenger.dispose();
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
}
