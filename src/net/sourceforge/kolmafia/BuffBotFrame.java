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

/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Font;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// containers
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.border.EtchedBorder;

import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

// utilities
import java.util.Properties;
import java.text.ParseException;

/**
 * An extension of <code>KoLFrame</code> which acts as a buffbot in Kingdom of Loathing.
 * This retrieves all messages from the inbox, and, for those messages which are buff
 * requests, buffs the sender. It also maintains the MP level using phonics downs.
 */

public class BuffBotFrame extends KoLFrame
{
	private Properties settings;
	private BuffBotManager currentManager;

	private BuffOptionsPanel buffOptions;
	private MainBuffPanel mainBuff;
	private WhiteListPanel whiteList;

	private LockableListModel buffCostTable;
	private LimitedSizeChatBuffer buffbotLog;

	/**
	 * Constructs a new <code>BuffBotFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 *
	 * @param	client	The client to be notified in the event of error.
	 */

	public BuffBotFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
				" (BuffBot)", client );

		settings = (client == null) ? System.getProperties() : client.getSettings();
		buffCostTable = new LockableListModel();

		if ( client != null )
		{
			client.initializeBuffBot();
			currentManager = new BuffBotManager( client, buffCostTable );
			client.setBuffBotManager( currentManager );
		}

		setResizable( false );

		// Initialize the display log buffer and the file log
		if ( client == null )
		{
			BuffBotHome home = new BuffBotHome(null);
			home.initialize();
			buffbotLog = home.getLog();
		}
		else
		{
			client.initializeBuffBot();
			buffbotLog = client.getBuffBotLog();
		}

		JTabbedPane tabs = new JTabbedPane();
		mainBuff = new MainBuffPanel();
		buffOptions = new BuffOptionsPanel();
		whiteList = new WhiteListPanel();

		tabs.addTab( "Run BuffBot", mainBuff );
		tabs.addTab( "Configure Buffs", buffOptions );
		tabs.addTab( "Change Settings", whiteList );

		getContentPane().setLayout( new CardLayout( 5, 5 ) );
		getContentPane().add( tabs, " " );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		addWindowListener( new DisableBuffBotAdapter() );

		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * Auxilary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		mainBuff.setEnabled( isEnabled );
		buffOptions.setEnabled( isEnabled );
	}

	/**
	 * Internal class used to handle everything related to
	 * operating the buffbot. This is the <CODE>mainBuffPanel</CODE>
	 */

	private class MainBuffPanel extends NonContentPanel
	{
		/**
		 * Constructor for <CODE>MainBuffPanel</CODE>
		 */

		public MainBuffPanel()
		{
			super( "Start", "Stop" );
			setContent( null );

			JEditorPane buffbotLogDisplay = new JEditorPane();
			buffbotLog.setChatDisplay( buffbotLogDisplay );
			buffbotLogDisplay.setEditable( false );

			JScrollPane scrollArea = new JScrollPane( buffbotLogDisplay,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			add( JComponentUtilities.createLabel( "BuffBot Activities", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

			JComponentUtilities.setComponentSize( scrollArea, 400, 200 );
			add( scrollArea, BorderLayout.WEST );
		}

		/**
		 * Action based on user pushing <b>Start</b>.
		 */

		protected void actionConfirmed()
		{	(new BuffBotRequestThread()).start();
		}

		/**
		 * Action, based on user selecting <b>Stop</b>
		 */

		protected void actionCancelled()
		{
			buffbotLog.append( "BuffBot Terminated.<br>\n\n\n" );
			client.setBuffBotActive(false);
			client.updateDisplay( ENABLED_STATE, "BuffBot stopped." );
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the request to start the buffbot.
		 */

		private class BuffBotRequestThread extends Thread
		{
			public BuffBotRequestThread()
			{
				super( "BuffBot-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				client.updateDisplay( ENABLED_STATE, "Buffbotting started." );
				buffbotLog.append( "<b>Starting a new session.</b><br>\n" );
				client.resetContinueState();
				client.setBuffBotActive( true );
				currentManager.runBuffBot();
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot options management
	 */

	private class BuffOptionsPanel extends NonContentPanel
	{
		private JCheckBox restrictBox;
		private JComboBox skillSelect;
		private JList buffListDisplay;
		private JTextField priceField, countField;

		public BuffOptionsPanel()
		{
			super( "Add Buff", "Remove Buff", new Dimension( 100, 20 ),  new Dimension( 240, 20 ));
			int skillID;
			String skill;
			
			LockableListModel skillSet = (client == null) ? new LockableListModel() :client.getCharacterData().getAvailableSkills();
			LockableListModel buffSet = new LockableListModel();
			for (int i = 0; (skill = (String) skillSet.get(i)) != null; ++i )
			{
				skill = (String) skillSet.get(i);
				skillID = ClassSkillsDatabase.getSkillID( skill.replaceFirst( "ñ", "&ntilde;" ) );
				if (ClassSkillsDatabase.isBuff( skillID ))
					buffSet.add(skill);
			}
			skillSelect = new JComboBox( buffSet );

			priceField = new JTextField();
			countField = new JTextField();
			restrictBox = new JCheckBox();
			restrictBox.setEnabled( false );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Buff Name: ", skillSelect );
			elements[1] = new VerifiableElement( "Buff Price: ", priceField );
			elements[2] = new VerifiableElement( "Cast Count: ", countField );
			elements[3] = new VerifiableElement( "White List Only?", restrictBox );
			setContent( elements );
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BoxLayout( southPanel, BoxLayout.Y_AXIS ) );
			southPanel.add( Box.createVerticalStrut( 40 ) );
			southPanel.add( new BuffListPanel() );
			add( southPanel, BorderLayout.SOUTH );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			skillSelect.setEnabled( isEnabled );
			buffListDisplay.setEnabled( isEnabled );
			priceField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			try
			{
				client.getBuffBotManager().addBuff( (String) skillSelect.getSelectedItem(),
					df.parse( priceField.getText() ).intValue(), df.parse( countField.getText() ).intValue(), restrictBox.isSelected() );
			}
			catch ( Exception e )
			{
			}
		}

		public void actionCancelled()
		{	client.getBuffBotManager().removeBuffs( buffListDisplay.getSelectedValues() );
		}

		private class BuffListPanel extends JPanel
		{
			public BuffListPanel()
			{
				String sellerSetting = settings.getProperty( "buffBotCasting" );
				if ( sellerSetting != null )
				{
					String [] soldBuffs = sellerSetting.split( "[;:]" );
					for ( int i = 0; i < soldBuffs.length; ++i )
						currentManager.addBuff( ClassSkillsDatabase.getSkillName( Integer.parseInt( soldBuffs[i] ) ),
							Integer.parseInt( soldBuffs[++i] ), Integer.parseInt( soldBuffs[++i] ), soldBuffs[++i].equals("true") );
				}

				setLayout( new BorderLayout() );
				setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
				add( JComponentUtilities.createLabel( "Active Buffing List", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				buffListDisplay = new JList( buffCostTable.getMirrorImage() );
				buffListDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				buffListDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
				buffListDisplay.setVisibleRowCount( 11 );

				add( new JScrollPane( buffListDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot White List management
	 */

	private class WhiteListPanel extends NonContentPanel
	{
		private JComboBox mpRestoreSelect;
		private JComboBox messageDisposalSelect;
		private WhiteListEntry listPanel;

		public WhiteListPanel()
		{
			super( "Apply", "Restore", new Dimension( 120, 20 ),  new Dimension( 200, 20 ));
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout() );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Delete Non-buff requests");
			messageDisposalChoices.add( "Save Non-buff requests");
			messageDisposalSelect = new JComboBox( messageDisposalChoices );

			LockableListModel mpRestoreChoices = new LockableListModel();
			mpRestoreChoices.add( "Phonics & Houses" );
			mpRestoreChoices.add( "Phonics Only" );
			mpRestoreChoices.add( "Tiny Houses Only" );
			mpRestoreSelect = new JComboBox( mpRestoreChoices );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Message Disposal", messageDisposalSelect );
			elements[1] = new VerifiableElement( "MP Restore Options ", mpRestoreSelect );

			setContent( elements );
			listPanel = new WhiteListEntry();
			add( listPanel, BorderLayout.SOUTH );

			(new LoadDefaultSettingsThread()).start();
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			mpRestoreSelect.setEnabled( isEnabled );
			messageDisposalSelect.setEnabled( isEnabled );
			listPanel.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{	(new StoreSettingsThread()).start();
		}

		public void actionCancelled()
		{	(new LoadDefaultSettingsThread()).start();
		}

		private class WhiteListEntry extends JPanel
		{
			public WhiteListEntry()
			{
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				JPanel panel = new JPanel();
				panel.add(new JLabel("Watch this space for the White List implementation"));
				add(panel);
			}
		}

		private class LoadDefaultSettingsThread extends Thread
		{
			public void run()
			{
				String messageDisposalSetting = settings.getProperty( "buffBotMessageDisposal" );
				String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

				messageDisposalSelect.setSelectedIndex( messageDisposalSetting == null || messageDisposalSetting.equals( "false" ) ? 0 : 1 );
				mpRestoreSelect.setSelectedItem( mpRestoreSetting == null ? "Phonics & Houses" : mpRestoreSetting );

				setStatusMessage( ENABLED_STATE, "" );
			}
		}

		private class StoreSettingsThread extends Thread
		{
			public void run()
			{
				settings.setProperty( "buffBotMessageDisposal", "" + (messageDisposalSelect.getSelectedIndex() == 1) );
				settings.setProperty( "buffBotMPRestore", mpRestoreSelect.getSelectedItem().toString() );

				if ( settings instanceof KoLSettings )
					((KoLSettings)settings).saveSettings();

				setStatusMessage( ENABLED_STATE, "Settings saved." );
				KoLRequest.delay( 5000 );
				setStatusMessage( ENABLED_STATE, "" );
			}
		}
	}

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	private class DisableBuffBotAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null )
				(new DisableBuffBotThread()).start();
		}

		private class DisableBuffBotThread extends Thread
		{
			public DisableBuffBotThread()
			{	setDaemon( true );
			}

			public void run()
			{
				if ( client != null )
					client.deinitializeBuffBot();
				client.updateDisplay( ENABLED_STATE, "Buffbot deactivated." );
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new BuffBotFrame( null);
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
