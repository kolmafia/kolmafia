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

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// containers
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;

// utilities
import java.util.Properties;
import java.text.ParseException;
import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

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
	private BuffBotHome buffbotLog;

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

		// Initialize the display log buffer and the file log
		if ( client == null )
		{
			BuffBotHome buffbogLog = new BuffBotHome(null);
			buffbogLog.initialize();
		}
		else
		{
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
		getContentPane().add( tabs, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );

		addWindowListener( new DisableBuffBotAdapter() );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenuItem adventureMenuItem = new JMenuItem( "" );
		adventureMenuItem.addActionListener( new ToggleVisibility( adventureMenuItem ) );

		addStatusMenu( menuBar ).add( adventureMenuItem, 0 );

		addPeopleMenu( menuBar );
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
		mainBuff.setEnabled( isEnabled );
		buffOptions.setEnabled( isEnabled );
		whiteList.setEnabled( isEnabled );
	}

	/**
	 * Internal class used to handle everything related to
	 * operating the buffbot. This is the <CODE>mainBuffPanel</CODE>
	 */

	private class MainBuffPanel extends LabeledScrollPanel
	{
		/**
		 * Constructor for <CODE>MainBuffPanel</CODE>
		 */

		public MainBuffPanel()
		{
			super( "BuffBot Activities", "Start", "Stop", new JEditorPane() );
			JEditorPane buffbotLogDisplay = (JEditorPane) getScrollComponent();
			buffbotLog.setChatDisplay(buffbotLogDisplay  );
			buffbotLogDisplay.setEditable( false );
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
			buffbotLog.timeStampedLogEntry( "BuffBot Terminated.<br>" );
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
				if ( client.isBuffBotActive() )
					return;

				buffbotLog.timeStampedLogEntry( "<b>Starting a new session.</b><br>" );
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

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );
			centerPanel.add( Box.createVerticalStrut( 40 ) );
			centerPanel.add( new BuffListPanel() );
			add( centerPanel, BorderLayout.CENTER );
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
		private JComboBox buffBotModeSelect;
		private JComboBox messageDisposalSelect;
		private WhiteListEntry whiteListEntry;

		private JTextArea whiteListEditor;

		private Object [] availableRestores;
		private JCheckBox [] restoreCheckbox;

		public WhiteListPanel()
		{
			super( "Apply", "Restore", new Dimension( 120, 20 ),  new Dimension( 240, 20 ));
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout() );

			LockableListModel buffBotModeChoices = new LockableListModel();
			buffBotModeChoices.add( "Accept meat for buffs" );
			buffBotModeChoices.add( "Accept tiny houses for buffs" );
			buffBotModeSelect = new JComboBox( buffBotModeChoices );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Auto-save non-buff requests" );
			messageDisposalChoices.add( "Auto-delete non-buff requests" );
			messageDisposalChoices.add( "Do nothing with non-buff requests" );
			messageDisposalSelect = new JComboBox( messageDisposalChoices );

			availableRestores = currentManager == null ? new Object[0] : currentManager.getMPRestoreItemList().toArray();
			restoreCheckbox = new JCheckBox[ availableRestores.length ];

			JPanel checkboxPanel = new JPanel();
			checkboxPanel.setLayout( new GridLayout( restoreCheckbox.length, 1 ) );

			for ( int i = 0; i < restoreCheckbox.length; ++i )
			{
				restoreCheckbox[i] = new JCheckBox();
				checkboxPanel.add( restoreCheckbox[i] );
			}

			JPanel labelPanel = new JPanel();
			labelPanel.setLayout( new GridLayout( availableRestores.length, 1 ) );
			for ( int i = 0; i < availableRestores.length; ++i )
				labelPanel.add( new JLabel( availableRestores[i].toString(), JLabel.LEFT ) );

			JPanel restorePanel = new JPanel();
			restorePanel.setLayout( new BorderLayout( 0, 0 ) );
			restorePanel.add( checkboxPanel, BorderLayout.WEST );
			restorePanel.add( labelPanel, BorderLayout.CENTER );

			JScrollPane scrollArea = new JScrollPane( restorePanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( scrollArea, 240, 100 );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Buff Bot Mode: ", buffBotModeSelect );
			elements[1] = new VerifiableElement( "Message Disposal: ", messageDisposalSelect );
			elements[2] = new VerifiableElement( "MP Restore Items: ", scrollArea );

			setContent( elements );
			(new LoadDefaultSettingsThread()).start();
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			whiteListEntry = new WhiteListEntry();
			add( whiteListEntry, BorderLayout.CENTER );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			messageDisposalSelect.setEnabled( isEnabled );
			buffBotModeSelect.setEnabled( isEnabled );
			whiteListEntry.setEnabled( isEnabled );
			for ( int i = 0; i < restoreCheckbox.length; ++i )
				restoreCheckbox[i].setEnabled( isEnabled );
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
				setLayout( new BorderLayout() );

				whiteListEditor = new JTextArea();
				whiteListEditor.setEditable( true );
				whiteListEditor.setLineWrap( true );
				whiteListEditor.setWrapStyleWord( true );

				JScrollPane scrollArea = new JScrollPane( whiteListEditor,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

				add( JComponentUtilities.createLabel( "White List (please separate names with commas):", JLabel.CENTER,
						Color.black, Color.white ), BorderLayout.NORTH );

				JComponentUtilities.setComponentSize( scrollArea, 400, 200 );
				add( scrollArea, BorderLayout.CENTER );
			}
		}

		private class LoadDefaultSettingsThread extends Thread
		{
			public void run()
			{
				String itemBasedBuffingSetting = settings.getProperty( "buffBotItemBasedBuffing" );
				String messageDisposalSetting = settings.getProperty( "buffBotMessageDisposal" );
				String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );
				String whiteListSetting = settings.getProperty( "whiteList" );

				if ( mpRestoreSetting != null )
					for ( int i = 0; i < availableRestores.length; ++i )
						if ( mpRestoreSetting.indexOf( availableRestores[i].toString() ) != -1 )
							restoreCheckbox[i].setSelected( true );

				messageDisposalSelect.setSelectedIndex( messageDisposalSetting == null ? 0 : Integer.parseInt( messageDisposalSetting ) );
				buffBotModeSelect.setSelectedIndex( (itemBasedBuffingSetting == null || itemBasedBuffingSetting.equals( "false" )) ? 0 : 1 );


				if ( whiteListSetting != null )
					whiteListEditor.setText( whiteListSetting );

				setStatusMessage( ENABLED_STATE, "Settings loaded." );
			}
		}

		private class StoreSettingsThread extends Thread
		{
			public void run()
			{
				settings.setProperty( "buffBotItemBasedBuffing", "" + (buffBotModeSelect.getSelectedIndex() == 1) );
				settings.setProperty( "buffBotMessageDisposal", "" + messageDisposalSelect.getSelectedIndex() );

				StringBuffer mpRestoreSetting = new StringBuffer();
				for ( int i = 0; i < restoreCheckbox.length; ++i )
					if ( restoreCheckbox[i].isSelected() )
					{
						mpRestoreSetting.append( availableRestores[i].toString() );
						mpRestoreSetting.append( ';' );
					}
				settings.setProperty( "buffBotMPRestore", mpRestoreSetting.toString() );

				String[] whiteListString = whiteListEditor.getText().split("\\s*,\\s*");
				java.util.Arrays.sort( whiteListString );

				whiteListEditor.setText( whiteListString[0] );
				for (int i = 1; i < whiteListString.length; i++)
					if (!whiteListString[i].equals(""))
						whiteListEditor.append( ", " + whiteListString[i] );
				settings.setProperty( "whiteList", whiteListEditor.getText() );

				if ( settings instanceof KoLSettings )
					((KoLSettings)settings).saveSettings();

				JOptionPane.showMessageDialog( null, "Settings have been saved!" );
			}
		}
	}

	private class ToggleVisibility implements ActionListener
	{
		private JMenuItem toggleItem;
		private boolean isVisible;

		public ToggleVisibility( JMenuItem toggleItem )
		{
			this.toggleItem = toggleItem;
			toggleItem.setText( "Hide Main" );
			this.isVisible = true;
		}

		public void actionPerformed( ActionEvent e )
		{
			this.isVisible = !this.isVisible;
			toggleItem.setText( isVisible ? "Hide Main" : "Seek Main" );
			if ( client != null && client instanceof KoLmafiaGUI )
				((KoLmafiaGUI)client).setVisible( this.isVisible );
		}
	}

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	private class DisableBuffBotAdapter extends WindowAdapter
	{
		public void windowClosing( WindowEvent e )
		{
			if ( client != null )
				(new DisableBuffBotThread()).start();
			else
				System.exit(0);
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
	 * An internal class which creates a panel which displays
	 * a generic scroll pane.  Note that the code for this
	 * frame was lifted from the ActionVerifyPanel found in
	 * the Spellcast package.
	 */

	private abstract class LabeledScrollPanel extends JPanel
	{
		private JComponent scrollComponent;
		private JPanel buttonPanel;

		public LabeledScrollPanel( String title, String confirmedText, String cancelledText, JComponent scrollComponent )
		{
			this.scrollComponent = scrollComponent;

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout() );

			centerPanel.add( JComponentUtilities.createLabel( title, JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			centerPanel.add( new JScrollPane( scrollComponent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText );

			JPanel actualPanel = new JPanel();
			actualPanel.setLayout( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );
			actualPanel.add( buttonPanel, BorderLayout.EAST );

			setLayout( new CardLayout( 10, 10 ) );
			add( actualPanel, "" );
		}

		public JComponent getScrollComponent()
		{	return scrollComponent;
		}

		protected abstract void actionConfirmed();
		protected abstract void actionCancelled();

		public void setEnabled( boolean isEnabled )
		{	buttonPanel.setEnabled( isEnabled );
		}

		private class VerifyButtonPanel extends JPanel
		{
			private JButton confirmedButton;
			private JButton cancelledButton;

			public VerifyButtonPanel( String confirmedText, String cancelledText )
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

				// add the "confirmed" button
				confirmedButton = new JButton( confirmedText );
				confirmedButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionConfirmed();
						}
					} );

				addButton( confirmedButton );
				add( Box.createVerticalStrut( 4 ) );

				// add the "cancelled" button
				cancelledButton = new JButton( cancelledText );
				cancelledButton.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							actionCancelled();
						}
					} );
				addButton( cancelledButton );

				JComponentUtilities.setComponentSize( this, 80, 100 );
			}

			private void addButton( JButton buttonToAdd )
			{
				JPanel container = new JPanel();
				container.setLayout( new GridLayout() );
				container.add( buttonToAdd );
				container.setMaximumSize( new Dimension( Integer.MAX_VALUE, 24 ) );
				add( container );
			}

			public void setEnabled( boolean isEnabled )
			{
				confirmedButton.setEnabled( isEnabled );
				//removing this, so that cancelledButton stays enabled
				//cancelledButton.setEnabled( isEnabled );
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
