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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import tab.CloseTabPaneEnhancedUI;

import com.informit.guides.JDnDList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class LoginFrame extends KoLFrame
{
	private static LoginFrame INSTANCE = null;

	private String username;
	private JComboBox servers;
	private JComponent usernameField;

	private JTextField proxyHost;
	private JTextField proxyPort;
	private JTextField proxyLogin;
	private JTextField proxyPassword;

	public LoginFrame()
	{
		super( VERSION_NAME + ": Login" );

		INSTANCE = this;
		this.tabs.addTab( "KoL Login", this.constructLoginPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );

		this.tabs.addTab( "Main Tabs", new StartupFramesPanel() );
		this.tabs.addTab( "Look & Feel", new UserInterfacePanel()  );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		this.tabs.addTab( "Connection", connectPanel );
		this.tabs.addTab( "Breakfast", breakfastPanel );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );

//		this.setResizable( false );
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( this.usernameField != null )
			this.usernameField.requestFocus();
	}

	public static boolean instanceExists()
	{	return INSTANCE != null;
	}

	public static void hideInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.setVisible( false );
	}

	public static void disposeInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.dispose();
	}

	public void dispose()
	{
		this.honorProxySettings();

		if ( !KoLDesktop.instanceExists() )
			System.exit(0);

		super.dispose();
		INSTANCE = null;
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getImage( StaticEntity.getProperty( "loginWindowLogo" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( new LoginPanel(), BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested
	 * inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel extends KoLPanel
	{
		private JPasswordField passwordField;

		private JCheckBox savePasswordCheckBox;
		private JCheckBox autoLoginCheckBox;
		private JCheckBox getBreakfastCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place
		 * for the users to input their login name and password.  This
		 * panel, because it is intended to be the content panel for
		 * status message updates, also has a status label.
		 */

		public LoginPanel()
		{
			super( "login", "relay" );

			boolean useTextField = saveStateNames.isEmpty();
			LoginFrame.this.usernameField = useTextField ? (JComponent) new JTextField() : (JComponent) new LoginNameComboBox();
			this.passwordField = new JPasswordField();

			this.savePasswordCheckBox = new JCheckBox();
			this.autoLoginCheckBox = new JCheckBox();
			this.getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( this.savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( this.autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( this.getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", LoginFrame.this.usernameField );
			elements[1] = new VerifiableElement( "Password: ", this.passwordField );

			this.setContent( elements );

			this.actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			this.actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = StaticEntity.getProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = StaticEntity.getProperty( "lastUsername" );
			else
				this.autoLoginCheckBox.setSelected( true );

			if ( LoginFrame.this.usernameField instanceof LoginNameComboBox )
				((LoginNameComboBox)LoginFrame.this.usernameField).setSelectedItem( autoLoginSetting );

			String passwordSetting = KoLmafia.getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				this.passwordField.setText( passwordSetting );
				this.savePasswordCheckBox.setSelected( true );
			}

			this.getBreakfastCheckBox.setSelected( StaticEntity.getBooleanProperty( "alwaysGetBreakfast" ) );
			this.getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			this.autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			this.savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			JComponentUtilities.addHotKey( LoginFrame.this.usernameField, KeyEvent.VK_ENTER, this.CONFIRM_LISTENER );

			try
			{
				String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
				this.setStatusMessage( holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}

		public void addListeners()
		{
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( LoginFrame.this.usernameField == null || this.passwordField == null )
				return;

			if ( this.savePasswordCheckBox == null || this.autoLoginCheckBox == null || this.getBreakfastCheckBox == null )
				return;

			super.setEnabled( isEnabled );

			LoginFrame.this.usernameField.setEnabled( isEnabled );
			this.passwordField.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "relayBrowserOnly", "false" );
			StaticEntity.setProperty( "alwaysGetBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.username = null;

			if ( LoginFrame.this.usernameField instanceof JTextField )
				LoginFrame.this.username = ((JTextField)LoginFrame.this.usernameField).getText();
			else if ( ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem() != null )
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem();
			else
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).currentMatch;


			String password = new String( this.passwordField.getPassword() );

			if ( LoginFrame.this.username == null || password == null || LoginFrame.this.username.equals("") || password.equals("") )
			{
				this.setStatusMessage( "Invalid login." );
				return;
			}

			if ( this.autoLoginCheckBox.isSelected() )
				StaticEntity.setProperty( "autoLogin", LoginFrame.this.username );
			else
				StaticEntity.setProperty( "autoLogin", "" );

			StaticEntity.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.honorProxySettings();
			RequestThread.postRequest( new LoginRequest( LoginFrame.this.username, password ) );

			if ( !KoLmafia.permitsContinue() )
				return;
			
			try
			{
				String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
				KoLmafia.updateDisplay( ENABLE_STATE, holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}

		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
				StaticEntity.setProperty( "relayBrowserOnly", "true" );

			StaticEntity.getClient().openRelayBrowser();
			LoginFrame.this.setVisible( false );
		}

		private class AutoLoginListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( LoginPanel.this.autoLoginCheckBox.isSelected() )
					LoginPanel.this.actionConfirmed();
				else
					StaticEntity.setProperty( "autoLogin", "" );
			}
		}

		private class GetBreakfastListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	StaticEntity.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( LoginPanel.this.getBreakfastCheckBox.isSelected() ) );
			}
		}

		private class RemovePasswordListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !LoginPanel.this.savePasswordCheckBox.isSelected() && LoginFrame.this.usernameField instanceof JComboBox )
				{
					String value = (String) saveStateNames.getSelectedItem();
					if ( value == null )
						return;

					saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					LoginPanel.this.passwordField.setText( "" );
				}

				StaticEntity.setProperty( "saveStateActive", String.valueOf( LoginPanel.this.savePasswordCheckBox.isSelected() ) );
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends MutableComboBox
		{
			public LoginNameComboBox()
			{	super( saveStateNames, true );
			}

			public void setSelectedItem( Object anObject )
			{
				super.setSelectedItem( anObject );
				this.setPassword();
			}

			public void findMatch( int keyCode )
			{
				super.findMatch( keyCode );
				this.setPassword();
			}

			private void setPassword()
			{
				if ( this.currentMatch == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				String password = KoLmafia.getSaveState( (String) this.currentMatch );
				if ( password == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				LoginPanel.this.passwordField.setText( password );
				LoginPanel.this.savePasswordCheckBox.setSelected( true );

				boolean breakfastSetting = StaticEntity.getGlobalProperty( (String) this.currentMatch, "getBreakfast" ).equals( "true" );
				LoginPanel.this.getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}

	private class ScriptPanel extends OptionsPanel
	{
		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		public ScriptPanel()
		{
			super( "Miscellaneous Scripts" );

			this.loginScript = new ScriptSelectPanel( new JTextField() );
			this.logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "On Login: ", this.loginScript );
			elements[1] = new VerifiableElement( "On Logout: ", this.logoutScript );
			elements[2] = new VerifiableElement();

			this.setContent( elements );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", this.loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", this.logoutScript.getText() );
		}

		public void actionCancelled()
		{
			String loginScript = StaticEntity.getProperty( "loginScript" );
			this.loginScript.setText( loginScript );

			String logoutScript = StaticEntity.getProperty( "logoutScript" );
			this.logoutScript.setText( logoutScript );
		}

	}

	private class BreakfastPanel extends JPanel implements ActionListener
	{
		private String breakfastType;
		private JCheckBox [] skillOptions;

		private JCheckBox grabClovers;
		private JCheckBox mushroomPlot;
		private JCheckBox rumpusRoom;
		private JCheckBox readManual;
		private JCheckBox loginRecovery;
		private JCheckBox pathedSummons;

		public BreakfastPanel( String title, String breakfastType )
		{
			super( new BorderLayout() );

			this.add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			JPanel centerPanel = new JPanel( new GridLayout( 4, 3 ) );

			this.loginRecovery = new JCheckBox( "enable auto-recovery" );
			this.loginRecovery.addActionListener( this );
			centerPanel.add( this.loginRecovery );

			this.pathedSummons = new JCheckBox( "honor path restrictions" );
			this.pathedSummons.addActionListener( this );
			centerPanel.add( this.pathedSummons );

			this.rumpusRoom = new JCheckBox( "visit clan rumpus room" );
			this.rumpusRoom.addActionListener( this );
			centerPanel.add( this.rumpusRoom );

			this.breakfastType = breakfastType;
			this.skillOptions = new JCheckBox[ UseSkillRequest.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				this.skillOptions[i] = new JCheckBox( UseSkillRequest.BREAKFAST_SKILLS[i].toLowerCase() );
				this.skillOptions[i].addActionListener( this );
				centerPanel.add( this.skillOptions[i] );
			}

			this.mushroomPlot = new JCheckBox( "plant mushrooms" );
			this.mushroomPlot.addActionListener( this );
			centerPanel.add( this.mushroomPlot );

			this.grabClovers = new JCheckBox( "get hermit clovers" );
			this.grabClovers.addActionListener( this );
			centerPanel.add( this.grabClovers );

			this.readManual = new JCheckBox( "read guild manual" );
			this.readManual.addActionListener( this );
			centerPanel.add( this.readManual );

			JPanel centerHolder = new JPanel( new BorderLayout() );
			centerHolder.add( centerPanel, BorderLayout.NORTH );

			JPanel centerContainer = new JPanel( new CardLayout( 10, 10 ) );
			centerContainer.add( centerHolder, "" );

			this.add( centerContainer, BorderLayout.CENTER );

			this.actionCancelled();
		}

		public void actionPerformed( ActionEvent e )
		{	this.actionConfirmed();
		}

		public void actionConfirmed()
		{
			StringBuffer skillString = new StringBuffer();

			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				if ( this.skillOptions[i].isSelected() )
				{
					if ( skillString.length() != 0 )
						skillString.append( "," );

					skillString.append( UseSkillRequest.BREAKFAST_SKILLS[i] );
				}
			}

			StaticEntity.setProperty( "breakfast" + this.breakfastType, skillString.toString() );
			StaticEntity.setProperty( "loginRecovery" + this.breakfastType, String.valueOf( this.loginRecovery.isSelected() ) );
			StaticEntity.setProperty( "pathedSummons" + this.breakfastType, String.valueOf( this.pathedSummons.isSelected() ) );
			StaticEntity.setProperty( "visitRumpus" + this.breakfastType, String.valueOf( this.rumpusRoom.isSelected() ) );
			StaticEntity.setProperty( "autoPlant" + this.breakfastType, String.valueOf( this.mushroomPlot.isSelected() ) );
			StaticEntity.setProperty( "grabClovers" + this.breakfastType, String.valueOf( this.grabClovers.isSelected() ) );
			StaticEntity.setProperty( "readManual" + this.breakfastType, String.valueOf( this.readManual.isSelected() ) );
		}

		public void actionCancelled()
		{
			String skillString = StaticEntity.getProperty( "breakfast" + this.breakfastType );
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
				this.skillOptions[i].setSelected( skillString.indexOf( UseSkillRequest.BREAKFAST_SKILLS[i] ) != -1 );

			this.loginRecovery.setSelected( StaticEntity.getBooleanProperty( "loginRecovery" + this.breakfastType ) );
			this.pathedSummons.setSelected( StaticEntity.getBooleanProperty( "pathedSummons" + this.breakfastType ) );
			this.rumpusRoom.setSelected( StaticEntity.getBooleanProperty( "visitRumpus" + this.breakfastType ) );
			this.mushroomPlot.setSelected( StaticEntity.getBooleanProperty( "autoPlant" + this.breakfastType ) );
			this.grabClovers.setSelected( StaticEntity.getBooleanProperty( "grabClovers" + this.breakfastType ) );
			this.readManual.setSelected( StaticEntity.getBooleanProperty( "readManual" + this.breakfastType ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private class StartupFramesPanel extends KoLPanel implements ListDataListener
	{
		private final String [][] FRAME_OPTIONS =
		{
			{ "Adventure", "AdventureFrame" },
			{ "Mini-Browser", "RequestFrame" },
			{ "Relay Server", "LocalRelayServer" },

			{ "Purchases", "MallSearchFrame" },
			{ "Graphical CLI", "CommandDisplayFrame" },

			{ "Player Status", "CharsheetFrame" },
			{ "Item Manager", "ItemManageFrame" },
			{ "Gear Changer", "GearChangeFrame" },

			{ "Store Manager", "StoreManageFrame" },
			{ "Museum Display", "MuseumFrame" },

			{ "Hall of Legends", "MeatManageFrame" },
			{ "Skill Casting", "SkillBuffFrame" },

			{ "Contact List", "ContactListFrame" },
			{ "Buffbot Manager", "BuffBotFrame" },
			{ "Purchase Buffs", "BuffRequestFrame" },

			{ "Flower Hunter", "FlowerHunterFrame" },
			{ "Mushroom Plot", "MushroomFrame" },
			{ "Familiar Trainer", "FamiliarTrainingFrame" },

			{ "IcePenguin Express", "MailboxFrame" },
			{ "KoLmafia Chat", "KoLMessenger" },
			{ "Recent Events", "EventsFrame" },

			{ "Clan Management", "ClanManageFrame" },
			{ "Farmer's Almanac", "CalendarFrame" },
			{ "Internal Database", "ExamineItemsFrame" },

			{ "Coin Toss Game", "MoneyMakingGameFrame" },
			{ "Preferences", "OptionsFrame" }
		};

		private boolean isRefreshing = false;
		private JComboBox usernameComboBox;

		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		private LockableListModel completeList = new LockableListModel();
		private LockableListModel startupList = new LockableListModel();
		private LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super( new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			this.usernameComboBox = new JComboBox( saveStateNames );
			this.loginScript = new ScriptSelectPanel( new JTextField() );
			this.logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Settings:  ", this.usernameComboBox );

			this.setContent( elements );

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3, 10, 10 ) );
			optionPanel.add( new LabeledScrollPanel( "Complete List", new JDnDList( this.completeList, false ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup as Window", new JDnDList( this.startupList ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup in Tabs", new JDnDList( this.desktopList ) ) );

			JTextArea message = new JTextArea(
				"These are the per-user settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

				"When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( DEFAULT_FONT );

			this.container.add( message, BorderLayout.NORTH );
			this.container.add( optionPanel, BorderLayout.SOUTH );
			this.actionCancelled();

			this.completeList.addListDataListener( this );
			this.startupList.addListDataListener( this );
			this.desktopList.addListDataListener( this );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", this.loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", this.logoutScript.getText() );

			this.actionCancelled();
		}

		public void actionCancelled()
		{
			this.isRefreshing = true;

			LoginFrame.this.username = (String) saveStateNames.getSelectedItem();
			if ( LoginFrame.this.username == null )
				LoginFrame.this.username = "";

			this.completeList.clear();
			this.startupList.clear();
			this.desktopList.clear();

			for ( int i = 0; i < this.FRAME_OPTIONS.length; ++i )
				this.completeList.add( this.FRAME_OPTIONS[i][0] );

			String frameString = StaticEntity.getGlobalProperty( LoginFrame.this.username, "initialFrames" );
			String desktopString = StaticEntity.getGlobalProperty( LoginFrame.this.username, "initialDesktop" );

			if ( frameString.equals( "" ) && desktopString.equals( "" ) )
			{
				frameString = StaticEntity.getGlobalProperty( "", "initialFrames" );
				desktopString = StaticEntity.getGlobalProperty( "", "initialDesktop" );
			}

			String [] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.startupList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
					{
						this.completeList.remove( this.FRAME_OPTIONS[j][0] );
						this.startupList.add( this.FRAME_OPTIONS[j][0] );
					}

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( !this.desktopList.contains( this.FRAME_OPTIONS[j][0] ) && this.FRAME_OPTIONS[j][1].equals( pieces[i] ) )
					{
						this.completeList.remove( this.FRAME_OPTIONS[j][0] );
						this.desktopList.add( this.FRAME_OPTIONS[j][0] );
					}

			this.isRefreshing = false;
			this.saveLayoutSettings();

			this.loginScript.setText( StaticEntity.getProperty( "loginScript" ) );
			this.logoutScript.setText( StaticEntity.getProperty( "logoutScript" ) );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( e.getSource() == this.startupList )
				this.desktopList.removeAll( this.startupList );

			if ( e.getSource() == this.desktopList )
				this.startupList.removeAll( this.desktopList );

			this.saveLayoutSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	this.saveLayoutSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( this.isRefreshing )
				return;

			StringBuffer frameString = new StringBuffer();
			StringBuffer desktopString = new StringBuffer();

			for ( int i = 0; i < this.startupList.size(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.startupList.get(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( frameString.length() != 0 ) frameString.append( "," );
						frameString.append( this.FRAME_OPTIONS[j][1] );
					}

			for ( int i = 0; i < this.desktopList.size(); ++i )
				for ( int j = 0; j < this.FRAME_OPTIONS.length; ++j )
					if ( this.desktopList.get(i).equals( this.FRAME_OPTIONS[j][0] ) )
					{
						if ( desktopString.length() != 0 ) desktopString.append( "," );
						desktopString.append( this.FRAME_OPTIONS[j][1] );
					}

			StaticEntity.setGlobalProperty( "", "initialFrames", frameString.toString() );
			StaticEntity.setGlobalProperty( "", "initialDesktop", desktopString.toString() );

			if ( saveStateNames.size() != 0 )
			{
				String username = (String) saveStateNames.getSelectedItem();
				if ( username == null )
					username = "";

				StaticEntity.setGlobalProperty( username, "initialFrames", frameString.toString() );
				StaticEntity.setGlobalProperty( username, "initialDesktop", desktopString.toString() );
			}
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	private class UserInterfacePanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private String [][] options =
			
			System.getProperty( "os.name" ).startsWith( "Win" ) ?
			
			new String [][]
			{
				{ "guiUsesOneWindow", "Restrict interface to a single window" },
				{ "useSystemTrayIcon", "Minimize main interface to system tray" },
				{ "addStatusBarToFrames", "Add a status line to independent windows" },
				{},
				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
			}
			
			:
				
			new String [][]
  			{
  				{ "guiUsesOneWindow", "Restrict interface to a single window" },
  				{ "addStatusBarToFrames", "Add a status line to independent windows" },
  				{},
  				{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
  				{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
  			};

		private JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( "", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );
	
			UIManager.LookAndFeelInfo [] installed = UIManager.getInstalledLookAndFeels();
			Object [] installedLooks = new Object[ installed.length ];

			for ( int i = 0; i < installedLooks.length; ++i )
				installedLooks[i] = installed[i].getClassName();

			this.looks = new JComboBox( installedLooks );

			this.toolbars = new JComboBox();
			this.toolbars.addItem( "Show global menus only" );
			this.toolbars.addItem( "Put toolbar along top of panel" );
			this.toolbars.addItem( "Put toolbar along bottom of panel" );
			this.toolbars.addItem( "Put toolbar along left of panel" );

			this.scripts = new JComboBox();
			this.scripts.addItem( "Do not show script bar on main interface" );
			this.scripts.addItem( "Put script bar after normal toolbar" );
			this.scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement [] elements = new VerifiableElement[3];

			elements[0] = new VerifiableElement( "Java L&F: ", this.looks );
			elements[1] = new VerifiableElement( "Toolbar: ", this.toolbars );
			elements[2] = new VerifiableElement( "Scripts: ", this.scripts );

			this.actionCancelled();
			this.setContent( elements );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			this.add( new InterfaceCheckboxPanel(), BorderLayout.CENTER );
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void actionConfirmed()
		{
			String lookAndFeel = (String) this.looks.getSelectedItem();
			if ( lookAndFeel != null )
				StaticEntity.setProperty( "swingLookAndFeel", lookAndFeel );

			StaticEntity.setProperty( "useToolbars", String.valueOf( this.toolbars.getSelectedIndex() != 0 ) );
			StaticEntity.setProperty( "scriptButtonPosition", String.valueOf( this.scripts.getSelectedIndex() ) );
			StaticEntity.setProperty( "toolbarPosition", String.valueOf( this.toolbars.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			this.looks.setSelectedItem( StaticEntity.getProperty( "swingLookAndFeel" ) );
			this.toolbars.setSelectedIndex( StaticEntity.getIntegerProperty( "toolbarPosition" ) );
			this.scripts.setSelectedIndex( StaticEntity.getIntegerProperty( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel extends OptionsPanel
		{
			private JLabel innerGradient, outerGradient;

			/**
			 * Constructs a new <code>windowsPanel</code>, containing a
			 * place for the users to select their desired server and for them
			 * to modify any applicable proxy settings.
			 */

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ UserInterfacePanel.this.options.length + 3 ];

				UserInterfacePanel.this.optionBoxes = new JCheckBox[ UserInterfacePanel.this.options.length ];
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					UserInterfacePanel.this.optionBoxes[i] = new JCheckBox();

				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					if ( UserInterfacePanel.this.options[i].length == 0 )
						elements[i] = new VerifiableElement();
					else
						elements[i] = new VerifiableElement( UserInterfacePanel.this.options[i][1], JLabel.LEFT, UserInterfacePanel.this.optionBoxes[i] );
				}

				elements[ UserInterfacePanel.this.options.length ] = new VerifiableElement();

				this.outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 1 ] = new VerifiableElement( "Change the outer portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.outerGradient );

				this.innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 2 ] = new VerifiableElement( "Change the inner portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, this.innerGradient );

				this.actionCancelled();
				this.setContent( elements );
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						StaticEntity.setProperty( UserInterfacePanel.this.options[i][0], String.valueOf( UserInterfacePanel.this.optionBoxes[i].isSelected() ) );

				super.actionConfirmed();
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
					if ( UserInterfacePanel.this.options[i].length > 0 )
						UserInterfacePanel.this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( UserInterfacePanel.this.options[i][0] ) );

				this.innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
				this.outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			public void setEnabled( boolean isEnabled )
			{
			}

			private class TabColorChanger extends LabelColorChanger
			{
				public TabColorChanger( String property )
				{	super( property );
				}

				public void applyChanges()
				{
					if ( this.property.equals( "innerTabColor" ) )
						CloseTabPaneEnhancedUI.selectedA = InterfaceCheckboxPanel.this.innerGradient.getBackground();
					else
						CloseTabPaneEnhancedUI.selectedB = InterfaceCheckboxPanel.this.outerGradient.getBackground();

					LoginFrame.this.tabs.repaint();
				}
			}
		}
	}

	private class ConnectionOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" },
			{ "ignoreLoadBalancer", "Ignore the KoL load balancer when trying to login" },
			{ "testSocketTimeout", "Allow socket timeouts for unstable connections" }
		};

		public ConnectionOptionsPanel()
		{
			super( "Connection Options", new Dimension( 20, 20 ), new Dimension( 380, 20 ) );

			LoginFrame.this.servers = new JComboBox();
			LoginFrame.this.servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			LoginFrame.this.servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			for ( int i = 2; i <= KoLRequest.SERVER_COUNT; ++i )
				LoginFrame.this.servers.addItem( "Attempt to use www" + i + ".kingdomofloathing.com" );

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ 2 + this.options.length ];

			elements[0] = new VerifiableElement( LoginFrame.this.servers );
			elements[1] = new VerifiableElement();

			for ( int i = 0; i < this.options.length; ++i )
				elements[i+2] = new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "defaultLoginServer", String.valueOf( LoginFrame.this.servers.getSelectedIndex() ) );
			for ( int i = 0; i < this.options.length; ++i )
				StaticEntity.setProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			LoginFrame.this.servers.setSelectedIndex( StaticEntity.getIntegerProperty( "defaultLoginServer" ) );
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( this.options[i][0] ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public void honorProxySettings()
	{
		StaticEntity.setProperty( "proxySet", String.valueOf(
				this.proxyHost.getText().trim().length() > 0 ) );

		StaticEntity.setProperty( "http.proxyHost", this.proxyHost.getText() );
		StaticEntity.setProperty( "http.proxyPort", this.proxyPort.getText() );
		StaticEntity.setProperty( "http.proxyUser", this.proxyLogin.getText() );
		StaticEntity.setProperty( "http.proxyPassword", this.proxyPassword.getText() );
	}

	/**
	 * This panel handles all of the things related to proxy
	 * options (if applicable).
	 */

	private class ProxyOptionsPanel extends LabeledKoLPanel
	{
		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel()
		{
			super( "Proxy Settings", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			LoginFrame.this.proxyHost = new JTextField();
			LoginFrame.this.proxyPort = new JTextField();
			LoginFrame.this.proxyLogin = new JTextField();
			LoginFrame.this.proxyPassword = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Host: ", LoginFrame.this.proxyHost );
			elements[1] = new VerifiableElement( "Port: ", LoginFrame.this.proxyPort );
			elements[2] = new VerifiableElement( "Login: ", LoginFrame.this.proxyLogin );
			elements[3] = new VerifiableElement( "Password: ", LoginFrame.this.proxyPassword );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			LoginFrame.this.proxyHost.setText( StaticEntity.getProperty( "http.proxyHost" ) );
			LoginFrame.this.proxyPort.setText( StaticEntity.getProperty( "http.proxyPort" ) );
			LoginFrame.this.proxyLogin.setText( StaticEntity.getProperty( "http.proxyUser" ) );
			LoginFrame.this.proxyPassword.setText( StaticEntity.getProperty( "http.proxyPassword" ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}
}
