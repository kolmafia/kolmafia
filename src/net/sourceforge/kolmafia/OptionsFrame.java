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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;

// events
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.AbstractButton;
import javax.swing.Box;

// utilities
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * <p>Handles all of the customizable user options in <code>KoLmafia</code>.
 * This class presents all of the options that the user can customize
 * in their adventuring and uses the appropriate <code>KoLSettings</code>
 * in order to display them.  This class also uses <code>KoLSettings</code>
 * to record the user's preferences for upcoming sessions.</p>
 *
 * <p>If this class is accessed before login, it will modify global settings
 * ONLY, and if the character already has settings, any modification of
 * global settings will not modify their own.  Accessing this class after
 * login will result in modification of the character's own settings ONLY,
 * and will not modify any global settings.</p>
 *
 * <p>Proxy settings are a special exception to this rule - because the
 * Java Virtual Machine requires the proxy settings to be specified at
 * a global level, though the settings are changed appropriately on disk,
 * only the most recently loaded settings will be active on the current
 * instance of the JVM.  If separate characters need separate proxies,
 * they cannot be run in the same JVM instance.</p>
 */

public class OptionsFrame extends KoLFrame
{
	/**
	 * Constructs a new <code>OptionsFrame</code> that will be
	 * associated with the given StaticEntity.getClient().  When this frame is
	 * closed, it will attempt to return focus to the currently
	 * active frame; note that if this is done while the StaticEntity.getClient()
	 * is shuffling active frames, closing the window will not
	 * properly transfer focus.
	 *
	 * @param	StaticEntity.getClient()	The StaticEntity.getClient() to be associated with this <code>OptionsFrame</code>
	 */

	public OptionsFrame()
	{
		super( "Preferences" );
		tabs = new JTabbedPane();

		addTab( "General", new GeneralOptionsPanel() );
		addTab( "Chat Options", new ChatOptionsPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private void addTab( String name, JComponent panel )
	{
		JScrollPane scroller = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		tabs.add( name, scroller );
	}

	/**
	 * This panel handles all of the things related to login
	 * options, including which server to use for login and
	 * all other requests, as well as the user's proxy settings
	 * (if applicable).
	 */

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "synchronizeFightFrame", "Show requests in mini-browser" },
			{ "serverFriendly", "Use server-friendlier request speed" },
			{ "forceReconnect", "Automatically time-in on time-out" },
			{ "autoSatisfyChecks", "Automatically satisfy conditions on check" },
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( "General Options", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( getProperty( options[i][0] ).equals( "true" ) );
		}
	}

	/**
	 * Panel used for handling chat-related options and preferences,
	 * including font size, window management and maybe, eventually,
	 * coloring options for contacts.
	 */

	private class ChatOptionsPanel extends OptionsPanel
	{
		private JComboBox autoLogSelect;
		private JComboBox fontSizeSelect;
		private JComboBox chatStyleSelect;
		private JComboBox useTabSelect;
		private JComboBox popupSelect;
		private JComboBox eSoluSelect;

		public ChatOptionsPanel()
		{
			super( "Chat Preferences" );

			autoLogSelect = new JComboBox();
			autoLogSelect.addItem( "Do not log chat" );
			autoLogSelect.addItem( "Automatically log chat" );

			fontSizeSelect = new JComboBox();
			for ( int i = 1; i <= 7; ++i )
				fontSizeSelect.addItem( String.valueOf( i ) );

			chatStyleSelect = new JComboBox();
			chatStyleSelect.addItem( "All conversations separate" );
			chatStyleSelect.addItem( "Channels separate, blues combined" );
			chatStyleSelect.addItem( "Channels combined, blues separate" );

			useTabSelect = new JComboBox();
			useTabSelect.addItem( "Use windowed chat interface" );
			useTabSelect.addItem( "Use tabbed chat interface" );

			popupSelect = new JComboBox();
			popupSelect.addItem( "Display /friends and /who in chat display" );
			popupSelect.addItem( "Popup a window for /friends and /who" );

			eSoluSelect = new JComboBox();
			eSoluSelect.addItem( "Nameclick select bar only" );
			eSoluSelect.addItem( "eSolu scriptlet chat links (color)" );
			eSoluSelect.addItem( "eSolu scriptlet chat links (gray)" );

			VerifiableElement [] elements = new VerifiableElement[6];
			elements[0] = new VerifiableElement( "Chat Logs: ", autoLogSelect );
			elements[1] = new VerifiableElement( "Font Size: ", fontSizeSelect );
			elements[2] = new VerifiableElement( "Chat Style: ", chatStyleSelect );
			elements[3] = new VerifiableElement( "Tabbed Chat: ", useTabSelect );
			elements[4] = new VerifiableElement( "Contact List: ", popupSelect );
			elements[5] = new VerifiableElement( "eSolu Script: ", eSoluSelect );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			if ( autoLogSelect.getSelectedIndex() == 1 )
				KoLMessenger.initializeChatLogs();

			setProperty( "autoLogChat", String.valueOf( autoLogSelect.getSelectedIndex() == 1 ) );
			setProperty( "fontSize", (String) fontSizeSelect.getSelectedItem() );
			LimitedSizeChatBuffer.setFontSize( Integer.parseInt( (String) fontSizeSelect.getSelectedItem() ) );

			setProperty( "chatStyle", String.valueOf( chatStyleSelect.getSelectedIndex() ) );
			setProperty( "useTabbedChat", String.valueOf( useTabSelect.getSelectedIndex() ) );
			setProperty( "usePopupContacts", String.valueOf( popupSelect.getSelectedIndex() ) );
			setProperty( "eSoluScriptType", String.valueOf( eSoluSelect.getSelectedIndex() ) );

			super.actionConfirmed();
		}

		protected void actionCancelled()
		{
			autoLogSelect.setSelectedIndex( getProperty( "autoLogChat" ).equals( "true" ) ? 1 : 0 );
			fontSizeSelect.setSelectedItem( getProperty( "fontSize" ) );
			LimitedSizeChatBuffer.setFontSize( Integer.parseInt( getProperty( "fontSize" ) ) );

			chatStyleSelect.setSelectedIndex( Integer.parseInt( getProperty( "chatStyle" ) ) );
			useTabSelect.setSelectedIndex( Integer.parseInt( getProperty( "useTabbedChat" ) ) );
			popupSelect.setSelectedIndex( Integer.parseInt( getProperty( "usePopupContacts" ) ) );
			eSoluSelect.setSelectedIndex( Integer.parseInt( getProperty( "eSoluScriptType" ) ) );
		}
	}

	/**
	 * A generic panel which adds a label to the bottom of the KoLPanel
	 * to update the panel's status.  It also provides a thread which is
	 * guaranteed to be a daemon thread for updating the frame which
	 * also retrieves a reference to the StaticEntity.getClient()'s current settings.
	 */

	private abstract class OptionsPanel extends LabeledKoLPanel
	{
		public OptionsPanel()
		{	this( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( String panelTitle )
		{	this( panelTitle, new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
		}

		public OptionsPanel( Dimension left, Dimension right )
		{	this( null, left, right );
		}

		public OptionsPanel( String panelTitle, Dimension left, Dimension right )
		{	super( panelTitle, left, right );
		}

		public void setStatusMessage( int displayState, String message )
		{
		}

		protected void actionConfirmed()
		{
		}
	}

	private class AreaOptionsPanel extends OptionsPanel
	{
		private String [] zones;
		private JCheckBox [] options;

		public AreaOptionsPanel()
		{
			super( "Adventure List", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );

			zones = new String[ AdventureDatabase.ZONE_NAMES.size() ];
			options = new JCheckBox[ AdventureDatabase.ZONE_NAMES.size() + 2 ];

			for ( int i = 0; i < options.length; ++i )
				options[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ AdventureDatabase.ZONE_NAMES.size() + 3 ];

			elements[0] = new VerifiableElement( "Sort adventure list", JLabel.LEFT, options[0] );
			elements[1] = new VerifiableElement( "Show associated zone", JLabel.LEFT, options[1] );
			elements[2] = new VerifiableElement( " ", new JLabel( "" ) );

			String [] names = new String[ AdventureDatabase.ZONE_NAMES.keySet().size() ];
			AdventureDatabase.ZONE_NAMES.keySet().toArray( names );

			for ( int i = 0; i < names.length; ++i )
			{
				zones[i] = (String) AdventureDatabase.ZONE_NAMES.get( names[i] );
				elements[i+3] = new VerifiableElement( "Hide " + AdventureDatabase.ZONE_DESCRIPTIONS.get( names[i] ), JLabel.LEFT, options[i+2] );
			}

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "sortAdventures", String.valueOf( options[0].isSelected() ) );
			setProperty( "showAdventureZone", String.valueOf( options[1].isSelected() ) );

			StringBuffer areas = new StringBuffer();

			for ( int i = 2; i < options.length; ++i )
			{
				if ( options[i].isSelected() )
				{
					if ( areas.length() != 0 )
						areas.append( ',' );

					areas.append( zones[i-2] );
				}
			}

			setProperty( "zoneExcludeList", areas.toString() );
			super.actionConfirmed();

			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			if ( options[0].isSelected() )
				Collections.sort( adventureList );
		}

		protected void actionCancelled()
		{
			options[0].setSelected( getProperty( "sortAdventures" ).equals( "true" ) );
			options[1].setSelected( getProperty( "showAdventureZone" ).equals( "true" ) );

			String excluded = getProperty( "zoneExcludeList" );
			for ( int i = 0; i < zones.length; ++i )
				options[i+2].setSelected( excluded.indexOf( zones[i] ) != -1 );
		}
	}
}
