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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

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
	 * associated with the given client.  When this frame is
	 * closed, it will attempt to return focus to the currently
	 * active frame; note that if this is done while the client
	 * is shuffling active frames, closing the window will not
	 * properly transfer focus.
	 */

	public OptionsFrame()
	{
		super( "Preferences" );
		tabs = new JTabbedPane();

		addTab( "Globals", new GlobalOptionsPanel() );
		addTab( "Items", new ItemOptionsPanel() );
		addTab( "Zones", new AreaOptionsPanel() );
		addTab( "Browser", new RelayOptionsPanel() );
		addTab( "Scriptbar", new ScriptButtonPanel() );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private void addTab( String name, JComponent panel )
	{
		JScrollPane scroller = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		tabs.add( name, scroller );
	}

	private class RelayOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAddsUseLinks", "Add [use] links when acquiring items" },
			{ "relayAddsCommandLineLinks", "Add gCLI tool links to chat launcher" },
			{ "relayAddsSimulatorLinks", "Add Ayvuir's Simulator of Loathing link" },
			{ "relayMovesManeuver", "Move moxious maneuver button into skills list" },
			{ "makeBrowserDecisions", "Browser modules automatically make decisions" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
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

	private class GlobalOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "showAllRequests", "Show requests in mini-browser" },
			{ "keepSessionLogs", "Maintain dated player session log" },
			{ "serverFriendly", "Use server-friendlier request speed" },
			{ "defaultToRelayBrowser", "Browser shortcut button loads relay browser" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public GlobalOptionsPanel()
		{
			super( "Global Options", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
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
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			super.actionConfirmed();

			if ( getProperty( "keepSessionLogs" ).equals( "true" ) )
				KoLmafia.openSessionStream();
			else
				KoLmafia.closeSessionStream();

			actionCancelled();
			KoLCharacter.refreshCalculatedLists();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getProperty( options[i][0] ).equals( "true" ) );
		}
	}

	private class ItemOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "showClosetDrivenCreations", "Get ingredients from closet if needed" },
			{ "createWithoutBoxServants", "Create without requiring a box servant" },
			{ "autoRepairBoxes", "Create and install new box servant after explosion" },
			{ "autoSatisfyChecks", "Allow mall purchases on conditions check" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ItemOptionsPanel()
		{
			super( "Item Options", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
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

			if ( getProperty( "keepSessionLogs" ).equals( "true" ) )
				KoLmafia.openSessionStream();
			else
				KoLmafia.closeSessionStream();

			actionCancelled();
			KoLCharacter.refreshCalculatedLists();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( getProperty( options[i][0] ).equals( "true" ) );

			if ( getProperty( "autoRepairBoxes" ).equals( "true" ) )
			{
				optionBoxes[3].setSelected( true );
				optionBoxes[3].setEnabled( false );
			}
			else
				optionBoxes[3].setEnabled( true );
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
			AdventureDatabase.refreshAdventureList();
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

	private class ScriptButtonPanel extends ItemManagePanel implements ListDataListener
	{
		private LockableListModel scriptList;

		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", "add new", "remove", new LockableListModel(), true, true );

			scriptList = (LockableListModel) elementList.getModel();
			scriptList.addListDataListener( this );

			String [] scriptList = getProperty( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
				this.scriptList.add( scriptList[i] );
		}

		public void actionConfirmed()
		{
			String rootPath = SCRIPT_DIRECTORY.getAbsolutePath();
			JFileChooser chooser = new JFileChooser( rootPath );
			int returnVal = chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null )
				return;

			if ( returnVal == JFileChooser.APPROVE_OPTION )
			{
				String scriptPath = chooser.getSelectedFile().getAbsolutePath();
				if ( scriptPath.startsWith( rootPath ) )
					scriptPath = scriptPath.substring( rootPath.length() + 1 );

				scriptList.add( "call " + scriptPath );
			}
		}

		public void actionCancelled()
		{	scriptList.remove( elementList.getSelectedIndex() );
		}

		public void intervalAdded( ListDataEvent e )
		{	saveSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	saveSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{	saveSettings();
		}

		private void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( scriptList.size() != 0 )
				settingString.append( (String) scriptList.get(0) );

			for ( int i = 1; i < scriptList.size(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) scriptList.get(i) );
			}

			setProperty( "scriptList", settingString.toString() );
		}
	}
}
