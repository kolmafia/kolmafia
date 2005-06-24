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
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
import javax.swing.JButton;

// utilities
import java.util.Iterator;
import java.util.Properties;
import java.text.ParseException;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.ActionPanel;
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
	private InvalidBuffPanel invalidBuff;

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
		super( "KoLmafia: BuffBot", client );

		settings = (client == null) ? System.getProperties() : client.getSettings();
		buffCostTable = new LockableListModel();

		if ( client != null )
		{
			client.initializeBuffBot();
			currentManager = new BuffBotManager( client, buffCostTable );
			client.setBuffBotManager( currentManager );
		}

		// Initialize the display log buffer and the file log
		buffbotLog = client == null ? new BuffBotHome( null ) : client.getBuffBotLog();

		JTabbedPane tabs = new JTabbedPane();
		mainBuff = new MainBuffPanel();
		buffOptions = new BuffOptionsPanel();
		whiteList = new WhiteListPanel();
		invalidBuff = new InvalidBuffPanel();

		tabs.addTab( "Run BuffBot", mainBuff );
		tabs.addTab( "Edit Buff List", buffOptions );
		tabs.addTab( "Change Settings", whiteList );
		tabs.addTab( "Reply Messages", invalidBuff );

		addCompactPane();
		getContentPane().add( tabs, BorderLayout.CENTER );

		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );

		addWindowListener( new DisableBuffBotAdapter() );
		addMenuBar();
	}

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenuItem adventureMenuItem = new JMenuItem( "", KeyEvent.VK_M );
		adventureMenuItem.addActionListener( new ToggleVisibility( adventureMenuItem ) );

		JMenuItem statisticsMenuItem = new JMenuItem( "Session Stats", KeyEvent.VK_S );
		statisticsMenuItem.addActionListener( new ShowStatisticsListener() );

		JMenu statusMenu = addStatusMenu( menuBar );
		statusMenu.add( adventureMenuItem, 0 );
		statusMenu.add( statisticsMenuItem, 1 );

		addPeopleMenu( menuBar );
		addHelpMenu( menuBar );
	}

	private class ShowStatisticsListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			StringBuffer statBuffer = new StringBuffer();
			statBuffer.append( "Buff Request Frequency:\n" );

			Iterator costIterator = buffCostTable.iterator();
			statBuffer.append( costIterator.hasNext() ? "\n" : "No buff statistics available." );

			BuffBotManager.BuffBotCaster currentCast;
			while ( costIterator.hasNext() )
			{
				currentCast = (BuffBotManager.BuffBotCaster) costIterator.next();
				statBuffer.append( currentCast.toString() );
				statBuffer.append( "\n  - Requested " );
				statBuffer.append( currentCast.getRequestsThisSession() );
				statBuffer.append( " time" );

				if ( currentCast.getRequestsThisSession() != 1 )
					statBuffer.append( 's' );

				statBuffer.append( " this session\n\n" );
			}

			StatisticsFrame frame = new StatisticsFrame( client, statBuffer.toString() );
			frame.pack();  frame.setVisible( true );  frame.requestFocus();
		}

		private class StatisticsFrame extends KoLFrame
		{
			public StatisticsFrame( KoLmafia client, String statistics )
			{
				super( "KoLmafia: Buffbot Statistics", client );

				JTextArea content = new JTextArea( 12, 32 );
				JScrollPane scroller = new JScrollPane( content, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
				content.setText( statistics );

				getContentPane().setLayout( new CardLayout( 10, 10 ) );
				getContentPane().add( scroller, "" );
			}
		}
	}

	/**
	 * Auxilary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		if ( mainBuff != null )
			mainBuff.setEnabled( isEnabled );
		if ( buffOptions != null )
			buffOptions.setEnabled( isEnabled );
		if ( whiteList != null )
			whiteList.setEnabled( isEnabled );
		if ( invalidBuff != null )
			invalidBuff.setEnabled( isEnabled );
	}

	/**
	 * Internal class used to handle everything related to
	 * operating the buffbot.
	 */

	private class MainBuffPanel extends LabeledScrollPanel
	{
		/**
		 * Constructor for <code>MainBuffPanel</code>
		 */

		public MainBuffPanel()
		{
			super( "BuffBot Activities", "start", "stop", new JList() );

			buffbotLog.setFrame( BuffBotFrame.this );
			JList buffbotLogDisplay = (JList) getScrollComponent();
			buffbotLogDisplay.setCellRenderer( BuffBotHome.getBuffMessageRenderer() );
			if ( client != null )
				buffbotLogDisplay.setModel( buffbotLog.getMessages() );
		}

		/**
		 * Action based on user pushing <b>Start</b>.
		 */

		protected void actionConfirmed()
		{
			if ( client.isBuffBotActive() )
				return;

			client.resetContinueState();
			client.setBuffBotActive( true );
			currentManager.runBuffBot(-1);
		}

		/**
		 * Action, based on user selecting <b>Stop</b>
		 */

		protected void actionCancelled()
		{
			client.setBuffBotActive( false );
			buffbotLog.updateStatus("BuffBot stopped by user.");
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot options management
	 */

	private class BuffOptionsPanel extends NonContentPanel
	{
		private JCheckBox restrictBox;
		private JCheckBox singletonBox;
		private JComboBox skillSelect;
		private JList buffListDisplay;
		private JTextField priceField, countField;

		public BuffOptionsPanel()
		{
			super( "Add", "Remove", new Dimension( 60, 20 ),  new Dimension( 240, 20 ));
			UseSkillRequest skill;

			LockableListModel skillSet = (client == null) ? new LockableListModel() : client.getCharacterData().getAvailableSkills();
			LockableListModel buffSet = new LockableListModel();
			for (int i = 0; (skill = (UseSkillRequest) skillSet.get(i)) != null; ++i )
				if (ClassSkillsDatabase.isBuff( ClassSkillsDatabase.getSkillID( skill.getSkillName() ) ))
					buffSet.add( skill );

			skillSelect = new JComboBox( buffSet );

			priceField = new JTextField();
			countField = new JTextField();
			restrictBox = new JCheckBox();
			singletonBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Buff: ", skillSelect );
			elements[1] = new VerifiableElement( "Price: ", priceField );
			elements[2] = new VerifiableElement( "Casts: ", countField );
			elements[3] = new VerifiableElement( "Wlist?", restrictBox );
			elements[4] = new VerifiableElement( "Plimi?", singletonBox );
			setContent( elements );
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );
			centerPanel.add( Box.createVerticalStrut( 10 ) );
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
				client.getBuffBotManager().addBuff( ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName(),
					df.parse( priceField.getText() ).intValue(), df.parse( countField.getText() ).intValue(),
						restrictBox.isSelected(), singletonBox.isSelected() );
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
				setLayout( new BorderLayout() );
				setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
				add( JComponentUtilities.createLabel( "Active Buffing List", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				buffListDisplay = new JList( buffCostTable );
				buffListDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				buffListDisplay.setVisibleRowCount( 5 );

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
		private JTextField maxPhilanthropyField;
		private JComboBox buffBotModeSelect;
		private JComboBox messageDisposalSelect;
		private WhiteListEntry whiteListEntry;

		private JTextArea whiteListEditor;

		private Object [] availableRestores;
		private JCheckBox [] restoreCheckbox;

		public WhiteListPanel()
		{
			super( "apply", "defaults", new Dimension( 60, 20 ),  new Dimension( 240, 20 ));
			JPanel panel = new JPanel();
			panel.setLayout( new BorderLayout() );

			maxPhilanthropyField = new JTextField();

			LockableListModel buffBotModeChoices = new LockableListModel();
			buffBotModeChoices.add( "Use buff cost list" );
			buffBotModeChoices.add( "Use tiny house mode" );
			buffBotModeSelect = new JComboBox( buffBotModeChoices );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Auto-save non-requests" );
			messageDisposalChoices.add( "Auto-delete non-requests" );
			messageDisposalChoices.add( "Do nothing to non-requests" );
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
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( scrollArea, 240, 100 );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "P-Limit: ", maxPhilanthropyField );
			elements[1] = new VerifiableElement( "Buffmode: ", buffBotModeSelect );
			elements[2] = new VerifiableElement( "Messages: ", messageDisposalSelect );
			elements[3] = new VerifiableElement( "Restores: ", scrollArea );

			setContent( elements );
			actionCancelled();
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
		{
			settings.setProperty( "maxPhilanthropy", maxPhilanthropyField.getText() );
			settings.setProperty( "buffBotItemBasedBuffing", String.valueOf( buffBotModeSelect.getSelectedIndex() == 1 ) );
			settings.setProperty( "buffBotMessageDisposal", String.valueOf( messageDisposalSelect.getSelectedIndex() ) );

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

		public void actionCancelled()
		{
			String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

			for ( int i = 0; i < availableRestores.length; ++i )
				if ( mpRestoreSetting.indexOf( availableRestores[i].toString() ) != -1 )
					restoreCheckbox[i].setSelected( true );

			maxPhilanthropyField.setText( settings.getProperty( "maxPhilanthropy" ) );
			messageDisposalSelect.setSelectedIndex( Integer.parseInt( settings.getProperty( "buffBotMessageDisposal" ) ) );
			buffBotModeSelect.setSelectedIndex( settings.getProperty( "itemBasedBuffing" ).equals( "true" ) ? 1 : 0 );
			whiteListEditor.setText( settings.getProperty( "whiteList" ) );

			setStatusMessage( ENABLED_STATE, "Settings loaded." );
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
						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

				add( JComponentUtilities.createLabel( "White List (separate names with commas):", JLabel.CENTER,
						Color.black, Color.white ), BorderLayout.NORTH );

				JComponentUtilities.setComponentSize( scrollArea, 300, 120 );
				add( scrollArea, BorderLayout.CENTER );
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
			{
				if ( client != null )
					client.setBuffBotActive( false );
				client.updateDisplay( ENABLED_STATE, "Buffbot deactivated." );
			}
			else
				System.exit(0);
		}
	}

	private class InvalidBuffPanel extends ActionPanel
	{
		private JTextArea invalidPriceMessage, thanksMessage;
		private JPanel buttonPanel;

		public InvalidBuffPanel()
		{
			invalidPriceMessage = new JTextArea();
			thanksMessage = new JTextArea();

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(2,1,0,10));

			JPanel centerTopPanel = new JPanel();
			centerTopPanel.setLayout( new BorderLayout() );
			centerTopPanel.add( JComponentUtilities.createLabel( "Invalid Buff Price Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			centerTopPanel.add( new JScrollPane( invalidPriceMessage, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			JPanel centerBottomPanel = new JPanel();
			centerBottomPanel.setLayout( new BorderLayout() );
			centerBottomPanel.add( JComponentUtilities.createLabel( "Donation Thanks Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			centerBottomPanel.add( new JScrollPane( thanksMessage, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );

			centerPanel.add(centerTopPanel);
			centerPanel.add(centerBottomPanel);
			buttonPanel = new VerifyButtonPanel( "save", "defaults" );

			JPanel actualPanel = new JPanel();
			actualPanel.setLayout( new BorderLayout( 20, 10 ) );
			actualPanel.add( centerPanel, BorderLayout.CENTER );
			actualPanel.add( buttonPanel, BorderLayout.EAST );

			setLayout( new CardLayout( 10, 10 ) );
			add( actualPanel, "" );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			if ( client != null )
			{
				client.getSettings().setProperty( "invalidBuffMessage", invalidPriceMessage.getText() );
				client.getSettings().setProperty( "thanksMessage", thanksMessage.getText() );
				client.getSettings().saveSettings();
			}

			JOptionPane.showMessageDialog( null, "Settings have been saved!" );
		}

		public void actionCancelled()
		{
			if ( client != null )
			{
				invalidPriceMessage.setText( client.getSettings().getProperty( "invalidBuffMessage" ) );
				thanksMessage.setText( client.getSettings().getProperty( "thanksMessage" ) );
			}
		}

		public void setEnabled( boolean isEnabled )
		{	buttonPanel.setEnabled( isEnabled );
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new BuffBotFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
