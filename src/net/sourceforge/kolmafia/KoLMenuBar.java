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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LicenseDisplay;

public class KoLMenuBar extends JMenuBar implements KoLConstants
{
	private WindowMenu windowMenu;

	public ScriptMenu scriptMenu;
	public BookmarkMenu bookmarkMenu;
	public JMenuItem debugMenuItem = new ToggleDebugMenuItem();

	private static final String [] LICENSE_FILENAME = {
		"kolmafia-license.txt", "spellcast-license.txt", "browserlauncher-license.htm", "sungraphics-license.txt", "foxtrot-license.txt", "jsmooth-license.txt", "unlicensed.htm" };

	private static final String [] LICENSE_NAME = {
		"KoLmafia", "Spellcast", "BrowserLauncher", "Sun Graphics", "Foxtrot", "JSmooth", "Unlicensed" };

	public KoLMenuBar()
	{
		// Add general features.

		JMenu statusMenu = new JMenu( "General" );
		this.add( statusMenu );

		// Add the refresh menu, which holds the ability to refresh
		// everything in the session.

		statusMenu.add( new DisplayFrameMenuItem( "Adventure", "AdventureFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Purchases", "MallSearchFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Graphical CLI", "CommandDisplayFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Preferences", "OptionsFrame" ) );

		statusMenu.add( new JSeparator() );

		statusMenu.add( new DisplayFrameMenuItem( "Mini-Browser", "RequestFrame" ) );
		statusMenu.add( new RelayBrowserMenuItem( "Relay Browser" ) );
		statusMenu.add( new InvocationMenuItem( "KoL Simulator", StaticEntity.getClient(), "launchSimulator" ) );

		statusMenu.add( new JSeparator() );

		statusMenu.add( new DisplayFrameMenuItem( "Player Status", "CharsheetFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Item Manager", "ItemManageFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Gear Changer", "GearChangeFrame" ) );
		statusMenu.add( new DisplayFrameMenuItem( "Skill Casting", "SkillBuffFrame" ) );

		if ( !System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			statusMenu.add( new JSeparator() );
			statusMenu.add( new LogoutMenuItem() );
			statusMenu.add( new EndSessionMenuItem() );
		}

		// Add specialized tools.

		JMenu toolsMenu = new JMenu( "Tools" );
		this.add( toolsMenu );

		toolsMenu.add( new InvocationMenuItem( "Clear Results", StaticEntity.getClient(), "resetSession" ) );
		toolsMenu.add( new StopEverythingItem() );
		toolsMenu.add( new RefreshEverythingItem() );

		toolsMenu.add( new JSeparator() );

		toolsMenu.add( new DisplayFrameMenuItem( "Meat Manager", "MeatManageFrame" ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Store Manager", "StoreManageFrame" ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Museum Display", "MuseumFrame" ) );

		toolsMenu.add( new JSeparator() );

		toolsMenu.add( new DisplayFrameMenuItem( "Mushroom Plot", "MushroomFrame" ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Flower Hunter", "FlowerHunterFrame" ) );
		toolsMenu.add( new DisplayFrameMenuItem( "Familiar Trainer", "FamiliarTrainingFrame" ) );

		// Add the old-school people menu.

		JMenu peopleMenu = new JMenu( "People" );
		this.add( peopleMenu );

		peopleMenu.add( new DisplayFrameMenuItem( "Read KoLmail", "MailboxFrame" ) );
		peopleMenu.add( new DisplayFrameMenuItem( "KoLmafia Chat", "KoLMessenger" ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Recent Events", "EventsFrame" ) );

		peopleMenu.add( new JSeparator() );

		peopleMenu.add( new DisplayFrameMenuItem( "Clan Manager", "ClanManageFrame" ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Send a Message", "SendMessageFrame" ) );
		peopleMenu.add( new RelayBrowserMenuItem( "Propose a Trade", "makeoffer.php" ) );

		peopleMenu.add( new JSeparator() );

		peopleMenu.add( new DisplayFrameMenuItem( "Run a Buffbot", "BuffBotFrame" ) );
		peopleMenu.add( new DisplayFrameMenuItem( "Purchase Buffs", "BuffRequestFrame" ) );


		// Add in common tasks menu

		JMenu travelMenu = new JMenu( "Travel" );
		this.add( travelMenu );

		travelMenu.add( new RelayBrowserMenuItem( "Doc Galaktik", "galaktik.php" ) );
		travelMenu.add( new InvocationMenuItem( "Rest in House", StaticEntity.getClient(), "makeCampgroundRestRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Relax in Beanbag", StaticEntity.getClient(), "makeCampgroundRelaxRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Sleep in Sofa", StaticEntity.getClient(), "makeClanSofaRequest" ) );

		travelMenu.add( new JSeparator() );

		travelMenu.add( new InvocationMenuItem( "Monster Level", StaticEntity.getClient(), "makeMindControlRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Untinker Item", StaticEntity.getClient(), "makeUntinkerRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Wand-Zap Item", StaticEntity.getClient(), "makeZapRequest" ) );
		travelMenu.add( new JSeparator() );
		travelMenu.add( new InvocationMenuItem( "Loot the Hermit", StaticEntity.getClient(), "makeHermitRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Visit the Trapper", StaticEntity.getClient(), "makeTrapperRequest" ) );
		travelMenu.add( new InvocationMenuItem( "Visit the Hunter", StaticEntity.getClient(), "makeHunterRequest" ) );

		// Add in automatic quest completion scripts.

		JMenu questsMenu = new JMenu( "Quests" );
		this.add( questsMenu );

		questsMenu.add( new InvocationMenuItem( "Unlock Guild", StaticEntity.getClient(), "unlockGuildStore" ) );
		questsMenu.add( new InvocationMenuItem( "Tavern Quest", StaticEntity.getClient(), "locateTavernFaucet" ) );

		questsMenu.add( new JSeparator() );

		questsMenu.add( new InvocationMenuItem( "Nemesis Quest", Nemesis.class, "faceNemesis" ) );
		questsMenu.add( new InvocationMenuItem( "Leaflet (No Stats)", StrangeLeaflet.class, "leafletNoMagic" ) );
		questsMenu.add( new InvocationMenuItem( "Leaflet (With Stats)", StrangeLeaflet.class, "leafletWithMagic" ) );

		questsMenu.add( new JSeparator() );

		questsMenu.add( new InvocationMenuItem( "Lucky Entryway", SorceressLair.class, "completeCloveredEntryway" ) );
		questsMenu.add( new InvocationMenuItem( "Unlucky Entryway", SorceressLair.class, "completeCloverlessEntryway" ) );
		questsMenu.add( new InvocationMenuItem( "Hedge Rotation", SorceressLair.class, "completeHedgeMaze" ) );
		questsMenu.add( new InvocationMenuItem( "Tower (Complete)", SorceressLair.class, "fightAllTowerGuardians" ) );
		questsMenu.add( new InvocationMenuItem( "Tower (To Shadow)", SorceressLair.class, "fightMostTowerGuardians" ) );

		// Add script and bookmark menus, which use the
		// listener-driven static final lists.

		if ( !bookmarks.isEmpty() )
		{
			this.bookmarkMenu = new BookmarkMenu();
			this.add( this.bookmarkMenu );
		}

		this.scriptMenu = new ScriptMenu();
		this.add( this.scriptMenu );

		this.add( this.windowMenu = new WindowMenu() );

		// Add help information for KoLmafia.  This includes
		// the additional help-oriented stuffs.

		JMenu helperMenu = new JMenu( "Help" );
		this.add( helperMenu );

		helperMenu.add( new DisplayFrameMenuItem( "Copyright Notice", "LicenseDisplay" ) );
		helperMenu.add( this.debugMenuItem );
		helperMenu.add( new CheckForUpdatesMenuItem() );
		helperMenu.add( new DisplayPageMenuItem( "Donate to KoLmafia", "http://kolmafia.sourceforge.net/credits.html" ) );

		helperMenu.add( new JSeparator() );

		helperMenu.add( new DisplayFrameMenuItem( "Farmer's Almanac", "CalendarFrame" ) );
		helperMenu.add( new DisplayFrameMenuItem( "Internal Database", "ExamineItemsFrame" ) );

		helperMenu.add( new JSeparator() );

		helperMenu.add( new DisplayPageMenuItem( "KoLmafia Thread", "http://forums.kingdomofloathing.com/vb/showthread.php?t=88408" ) );
		helperMenu.add( new DisplayPageMenuItem( "End-User Manual", "http://kolmafia.sourceforge.net/manual.html" ) );
		helperMenu.add( new DisplayPageMenuItem( "Unofficial Guide", "http://forums.kingdomofloathing.com/vb/showthread.php?t=140340" ) );
		helperMenu.add( new DisplayPageMenuItem( "Script Repository", "http://kolmafia.us/" ) );

		helperMenu.add( new JSeparator() );

		helperMenu.add( new DisplayPageMenuItem( "Subjunctive KoL", "http://www.subjunctive.net/kol/FrontPage.html" ) );
		helperMenu.add( new DisplayPageMenuItem( "KoL Visual Wiki", "http://kol.coldfront.net/thekolwiki/index.php/Main_Page" ) );
		helperMenu.add( new InvocationMenuItem( "Violet Fog Mapper", VioletFog.class, "showGemelliMap" ) );
		helperMenu.add( new InvocationMenuItem( "Louvre Mapper", Louvre.class, "showGemelliMap" ) );
	}

	public void dispose()
	{
		if ( windowMenu != null )
			windowMenu.dispose();

		if ( scriptMenu != null )
			scriptMenu.dispose();

		if ( bookmarkMenu != null )
			bookmarkMenu.dispose();
	}

	private class CheckForUpdatesMenuItem extends DisplayPageMenuItem
	{
		public CheckForUpdatesMenuItem()
		{	super( "Check for Updates", "https://sourceforge.net/project/showfiles.php?group_id=126572" );
		}

		public void run()
		{
			CommandDisplayFrame.executeCommand( "update" );
			super.run();
		}
	}

	/**
	 * Internal class which displays the given request inside
	 * of the current frame.
	 */

	public class DisplayRequestMenuItem extends ThreadedMenuItem
	{
		private String location;

		public DisplayRequestMenuItem( String label, String location )
		{
			super( label );
			this.location = location;
		}

		public void run()
		{
			if ( this.location.startsWith( "http" ) )
				StaticEntity.openSystemBrowser( this.location );
			else
				StaticEntity.openRequestFrame( this.location );
		}

		public String toString()
		{	return this.getText();
		}
	}

	public class WindowMenu extends MenuItemList
	{
		public WindowMenu()
		{	super( "Window", existingFrames );
		}

		public JComponent constructMenuItem( Object o )
		{	return new WindowDisplayMenuItem( (KoLFrame) o );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[1];
			headers[0] = new WindowDisplayMenuItem( null );
			return headers;
		}

		private class WindowDisplayMenuItem extends ThreadedMenuItem
		{
			private WeakReference frameReference;

			public WindowDisplayMenuItem( KoLFrame frame )
			{
				super( frame == null ? "Show All Displays" : frame.toString() );
				this.frameReference = frame == null ? null : new WeakReference( frame );
			}

			public void run()
			{
				if ( this.frameReference == null )
				{
					KoLFrame [] frames = StaticEntity.getExistingFrames();
					String interfaceSetting = KoLSettings.getGlobalProperty( "initialDesktop" );

					for ( int i = 0; i < frames.length; ++i )
						if ( interfaceSetting.indexOf( frames[i].getFrameName() ) == -1 )
							frames[i].setVisible( true );

					if ( KoLDesktop.instanceExists() )
						KoLDesktop.getInstance().setVisible( true );

					return;
				}

				KoLFrame frame = (KoLFrame) this.frameReference.get();
				if ( frame != null )
				{
					boolean appearsInTab = KoLSettings.getGlobalProperty( "initialDesktop" ).indexOf(
						frame instanceof ChatFrame ? "KoLMessenger" : frame.getFrameName() ) != -1;

					if ( !appearsInTab )
						frame.setVisible( true );

					frame.requestFocus();
				}
			}
		}
	}

	/**
	 * A special class which renders the menu holding the list of bookmarks.
	 * This class also synchronizes with the list of available bookmarks.
	 */

	public class BookmarkMenu extends MenuItemList
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
				location += "&pwd";

			return new DisplayRequestMenuItem( name, location );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[0];
			return headers;
		}
	}

	private class ToggleDebugMenuItem extends ThreadedMenuItem
	{
		public ToggleDebugMenuItem()
		{	super( RequestLogger.isDebugging() ? "Stop Debug Log" : "Start Debug Log" );
		}

		public void run()
		{
			if ( RequestLogger.isDebugging() )
			{
				RequestLogger.closeDebugLog();
				KoLMenuBar.this.debugMenuItem.setText( "Start Debug Log" );
			}
			else
			{
				RequestLogger.openDebugLog();
				KoLMenuBar.this.debugMenuItem.setText( "Stop Debug Log" );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for loading a script.
	 */

	private class LoadScriptMenuItem extends ThreadedMenuItem
	{
		private String scriptPath;

		public LoadScriptMenuItem()
		{	this( "Load script...", null );
		}

		public LoadScriptMenuItem( String scriptName, String scriptPath )
		{
			super( scriptName );
			this.scriptPath = scriptPath;
		}

		public void run()
		{
			String executePath = this.scriptPath;

			if ( this.scriptPath == null )
			{
				JFileChooser chooser = new JFileChooser( SCRIPT_LOCATION.getAbsolutePath() );
				int returnVal = chooser.showOpenDialog( null );

				if ( chooser.getSelectedFile() == null )
					return;

				if ( returnVal == JFileChooser.APPROVE_OPTION )
					executePath = chooser.getSelectedFile().getAbsolutePath();
			}

			if ( executePath == null )
				return;

			KoLmafia.forceContinue();
			CommandDisplayFrame.executeCommand( "call " + executePath );
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing frames.
	 */

	public class DisplayFrameMenuItem extends ThreadedMenuItem
	{
		private String frameClass;

		public DisplayFrameMenuItem( String title, String frameClass )
		{
			super( title );
			this.frameClass = frameClass;
		}

		public void run()
		{
			if ( this.frameClass.equals( "LicenseDisplay" ) )
			{
				Object [] parameters = new Object[4];
				parameters[0] = "KoLmafia: Copyright Notices";
				parameters[1] = new VersionDataPanel();
				parameters[2] = LICENSE_FILENAME;
				parameters[3] = LICENSE_NAME;

				SwingUtilities.invokeLater( new CreateFrameRunnable( LicenseDisplay.class, parameters ) );
			}
			else
			{
				KoLmafiaGUI.constructFrame( this.frameClass );
			}
		}
	}

	/**
	 * Internal class which opens the operating system's default
	 * browser to the given location.
	 */

	public class DisplayPageMenuItem extends ThreadedMenuItem
	{
		private String location;

		public DisplayPageMenuItem( String title, String location )
		{
			super( title );
			this.location = location;
		}

		public void run()
		{	StaticEntity.openSystemBrowser( this.location );
		}
	}

	public class RelayBrowserMenuItem extends ThreadedMenuItem
	{
		private String location;

		public RelayBrowserMenuItem( String title )
		{	this( title, null );
		}

		public RelayBrowserMenuItem( String title, String location )
		{
			super( title );
			this.location = location;
		}

		public void run()
		{
			if ( location == null )
				StaticEntity.getClient().openRelayBrowser();
			else
				StaticEntity.getClient().openRelayBrowser( this.location );
		}
	}

	/**
	 * Internal class used to invoke the given no-parameter
	 * method on the given object.  This is used whenever
	 * there is the need to invoke a method and the creation
	 * of an additional class is unnecessary.
	 */

	public class InvocationMenuItem extends ThreadedMenuItem
	{
		private Object object;
		private Method method;
		private String methodName;

		public InvocationMenuItem( String title, Object object, String methodName )
		{
			this( title, object == null ? null : object.getClass(), methodName );
			this.object = object;
		}

		public InvocationMenuItem( String title, Class c, String methodName )
		{
			super( title );
			this.methodName = methodName;

			if ( c == null )
				return;

			try
			{
				this.object = c;
				this.method = c.getMethod( methodName, NOPARAMS );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		public void run()
		{
			try
			{
				RequestThread.openRequestSequence();

				if ( this.method != null )
					this.method.invoke( this.object instanceof KoLmafia ? StaticEntity.getClient() : this.object, null );

				RequestThread.closeRequestSequence();
			}
			catch ( Exception e1 )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e1 );
			}
		}
	}

	/**
	 * An internal class used to handle requests to open a new frame
	 * using a local panel inside of the adventure frame.
	 */

	public class KoLPanelFrameMenuItem extends ThreadedMenuItem
	{
		private CreateFrameRunnable creator;

		public KoLPanelFrameMenuItem( String title, ActionPanel panel )
		{
			super( title );

			Object [] parameters = new Object[2];
			parameters[0] = title;
			parameters[1] = panel;

			this.creator = new CreateFrameRunnable( KoLPanelFrame.class, parameters );
		}

		public void run()
		{	SwingUtilities.invokeLater( this.creator );
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
			StaticEntity.getVersion(), VERSION_DATE, " ",
			"Copyright © 2005-2007 KoLmafia development team",
			"Berkeley Software Development (BSD) License",
			"http://kolmafia.sourceforge.net/",
			" ",
			"Current Running on " + System.getProperty( "os.name" ),
			"Local Directory is " + System.getProperty( "user.dir" ),
			"Using Java v" + System.getProperty( "java.runtime.version" )
		};

		public VersionDataPanel()
		{
			JPanel versionPanel = new JPanel( new BorderLayout( 20, 20 ) );
			versionPanel.add( new JLabel( JComponentUtilities.getImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.NORTH );

			JPanel labelPanel = new JPanel( new GridLayout( this.versionData.length, 1 ) );
			for ( int i = 0; i < this.versionData.length; ++i )
				labelPanel.add( new JLabel( this.versionData[i], JLabel.CENTER ) );

			versionPanel.add( labelPanel, BorderLayout.CENTER );

			JButton donateButton = new JButton( JComponentUtilities.getImage( "paypal.gif" ) );
			JComponentUtilities.setComponentSize( donateButton, 74, 31 );
			donateButton.addActionListener( new DonateButtonListener() );

			JPanel donatePanel = new JPanel();
			donatePanel.add( donateButton );

			JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
			centerPanel.add( versionPanel, BorderLayout.CENTER );
			centerPanel.add( donatePanel, BorderLayout.SOUTH );

			this.setLayout( new CardLayout( 20, 20 ) );
			this.add( centerPanel, "" );
		}

		private class DonateButtonListener extends ThreadedListener
		{
			public void run()
			{	StaticEntity.openSystemBrowser( "http://sourceforge.net/project/project_donations.php?group_id=126572" );
			}
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

		public JComponent constructMenuItem( Object o )
		{	return o instanceof JSeparator ? new JSeparator() : this.constructMenuItem( (File) o, "scripts" );
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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
				return null;
			}

			String name = pieces[ pieces.length - 1 ];
			String path = prefix + File.separator + name;

			if ( file.isDirectory() )
			{
				// Get a list of all the files
				File [] scriptList = file.listFiles( BACKUP_FILTER );

				//  Convert the list into a menu
				JMenu menu = new JMenu( name );

				// Iterate through the files.  Do this in two
				// passes to make sure that directories start
				// up top, followed by non-directories.

				boolean hasDirectories = false;

				for ( int i = 0; i < scriptList.length; ++i )
				{
					if ( scriptList[i].isDirectory() && shouldAddScript( scriptList[i] ) )
					{
						menu.add( this.constructMenuItem( scriptList[i], path ) );
						hasDirectories = true;
					}
				}

				for ( int i = 0; i < scriptList.length; ++i )
					if ( !scriptList[i].isDirectory() )
						menu.add( this.constructMenuItem( scriptList[i], path ) );

				// Return the menu
				return menu;
			}

			return new LoadScriptMenuItem( name, path );
		}

		public JComponent [] getHeaders()
		{
			JComponent [] headers = new JComponent[2];

			headers[0] = new LoadScriptMenuItem();
			headers[1] = new InvocationMenuItem( "Refresh menu", KoLFrame.class, "compileScripts" );

			return headers;
		}
	}

	public static final boolean shouldAddScript( File script )
	{
		if ( !script.isDirectory() )
			return true;

		File [] scriptList = script.listFiles( BACKUP_FILTER );
		if ( scriptList == null || scriptList.length == 0 )
			return false;

		for ( int i = 0; i < scriptList.length; ++i )
			if ( !shouldAddScript( scriptList[i] ) )
				return false;

		return true;
	}

	private class RequestMenuItem extends ThreadedMenuItem
	{
		private KoLRequest request;

		public RequestMenuItem( String title, KoLRequest request )
		{
			super( title );
			this.request = request;
		}

		public void run()
		{	RequestThread.postRequest( this.request );
		}
	}

	private class StopEverythingItem extends ThreadedMenuItem
	{
		public StopEverythingItem()
		{	super( "Stop Everything" );
		}

		public void run()
		{	RequestThread.declareWorldPeace();
		}
	}

	private class RefreshEverythingItem extends ThreadedMenuItem
	{
		public RefreshEverythingItem()
		{	super( "Refresh Session" );
		}

		public void run()
		{
			if ( KoLSettings.getUserProperty( "loginServerName" ).startsWith( "dev" ) )
				LoginRequest.executeTimeInRequest();
			else
				StaticEntity.getClient().refreshSession();
		}
	}

	private class LogoutMenuItem extends ThreadedMenuItem
	{
		public LogoutMenuItem()
		{	super( "Logout of KoL" );
		}

		public void run()
		{	RequestThread.postRequest( new LogoutRequest() );
		}
	}

	private static class EndSessionMenuItem extends ThreadedMenuItem
	{
		public EndSessionMenuItem()
		{	super( "Exit KoLmafia" );
		}

		public void run()
		{	System.exit(0);
		}
	}
}
