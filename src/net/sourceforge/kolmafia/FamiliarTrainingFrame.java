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
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;

// events
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.ImageIcon;

// utilities
import java.lang.Math;
import java.util.ArrayList;
import java.util.TreeSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.lang.ref.WeakReference;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to provide access to Xylpher's familiar training tool
 */

public class FamiliarTrainingFrame extends KoLFrame
{
	private static ChatBuffer results = new ChatBuffer( "Arena Tracker" );
	private static boolean stop = false;
	private FamiliarTrainingPanel training;

	public static final int BASE = 1;
	public static final int BUFFED = 2;
	public static final int TURNS = 3;

	// Familiar buffing skills and effects
	private static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0, true );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0, true );
	private static final AdventureResult GREEN_TONGUE = new AdventureResult( "Green Tongue", 0, true );
	private static final AdventureResult BLACK_TONGUE = new AdventureResult( "Black Tongue", 0, true );

	// Familiar buffing items
	private static final AdventureResult PITH_HELMET = new AdventureResult( "plexiglass pith helmet", 0, false );
	private static final AdventureResult LEAD_NECKLACE = new AdventureResult( "lead necklace", 0, false );
	private static final AdventureResult RAT_HEAD_BALLOON = new AdventureResult( "rat head balloon", 0, false );
	private static final int firstTinyPlastic = 969;
	private static final int lastTinyPlastic = 988;
	private static final int firstTinyPlasticCrimbo = 1377;
	private static final int lastTinyPlasticCrimbo = 1378;

	// An adventure
	private static final AdventureResult ADV = new AdventureResult( AdventureResult.ADV, -1 );

	public FamiliarTrainingFrame( KoLmafia client )
	{
		super( client, "Familiar Training Tool" );

		addWindowListener( new CloseFamiliarTrainerListener() );

		CardLayout cards = new CardLayout( 10, 10 );
		framePanel.setLayout( cards );

		JMenuBar menuBar = getJMenuBar();

		JMenu optionsMenu = new JMenu( "Trainer" );
		optionsMenu.add( new FileMenuItem() );
		optionsMenu.add( new LocalSettingChangeMenuItem( client, "Cast buffs during training", "castBuffsWhileTraining" ) );
		// optionsMenu.add( new LocalSettingChangeMenuItem( client, "Debug", "debugFamiliarTraining" ) );
		menuBar.add( optionsMenu, 0 );

		training = new FamiliarTrainingPanel();
		framePanel.add( training, "" );

		// Clear left over results from the buffer
		results.clearBuffer();

		// Enable the display after fetching opponents
		if ( client != null )
			client.enableDisplay();
	}

	/**
	 * Internal class to handle de-initializing the chat when
	 * the window is closed.  This helps stop constantly
	 * spamming the chat server with a request when nothing
	 * is being done with the replies.
	 */

	protected final class CloseFamiliarTrainerListener extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{	stop = true;
		}
	}

	private class FileMenuItem extends JMenuItem implements ActionListener
	{
		public FileMenuItem()
		{
			super( "Save transcript" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			JFileChooser chooser = new JFileChooser( "data" );
			int returnVal = chooser.showSaveDialog( FamiliarTrainingFrame.this );

			File output = chooser.getSelectedFile();

			if ( output == null )
				return;

			try
			{
				PrintWriter ostream = new PrintWriter( new FileOutputStream( output, true ), true );
				ostream.println( results.getBuffer().replaceAll( "<br>", LINE_BREAK) );
				ostream.close();
			}
			catch ( Exception ex )
			{
				ex.printStackTrace( KoLmafia.getLogStream() );
				ex.printStackTrace();
			}
		}
	}

	private class FamiliarTrainingPanel extends JPanel
	{
		FamiliarData familiar;
		int weight;

		// Components
		FamiliarPanel familiarPanel;
		OpponentsPanel opponentsPanel;
		ButtonPanel buttonPanel;
		ResultsPanel resultsPanel;
		ChangePanel changePanel;

		public FamiliarTrainingPanel()
		{
			existingPanels.add( new WeakReference( this ) );
			setLayout( new BorderLayout( 10, 10 ) );

			// Get current familiar & base weight
			familiar = KoLCharacter.getFamiliar();
			weight = familiar.getWeight();

			// Put current familiar on top
			familiarPanel = new FamiliarPanel();
			add( familiarPanel, BorderLayout.NORTH );

			// Put opponents on left
			opponentsPanel = new OpponentsPanel();
			add( opponentsPanel, BorderLayout.WEST );

			// Put buttons on right
			buttonPanel = new ButtonPanel();
			add( buttonPanel, BorderLayout.EAST );

			// Put results in center
			resultsPanel = new ResultsPanel();
			add( resultsPanel, BorderLayout.CENTER );

			// Put familiar changer on bottom
			changePanel = new ChangePanel();
			add( changePanel, BorderLayout.SOUTH );

			// Register a listener to keep it updated
			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new FamiliarRefresh() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );

			if ( buttonPanel != null )
				buttonPanel.setEnabled( isEnabled );
			if ( changePanel != null )
				changePanel.setEnabled( isEnabled );
		}

		private class FamiliarPanel extends JPanel
		{
			private JLabel familiarIcon;
			private JLabel familiarLabel;

			public FamiliarPanel()
			{
				familiarIcon = new JLabel( "", JLabel.LEFT );
				add( familiarIcon );

				familiarLabel = new JLabel( "", JLabel.CENTER );
				add( familiarLabel );

				// Set the icon and label
				refreshFamiliar();
			}

			private void refreshFamiliar()
			{
				if ( familiar == FamiliarData.NO_FAMILIAR )
				{
					familiarIcon.setIcon( null );
					familiarLabel.setText( "(no familiar)" );
				}
				else
				{
					familiarIcon.setIcon( FamiliarsDatabase.getFamiliarImage( familiar.getID() ) );

					String label = familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace();
					familiarLabel.setText( label );
				}
			}
		}

		private class OpponentsPanel extends JPanel
		{
			public OpponentsPanel()
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
				// Get current opponents
				LockableListModel opponents = CakeArenaManager.getOpponentList( client );

				int opponentCount = opponents.size();
				for ( int i = 0; i < opponentCount; ++i )
				{
					CakeArenaManager.ArenaOpponent opponent = (CakeArenaManager.ArenaOpponent)opponents.get( i );
					add( new OpponentLabel( opponent ) );
				}
			}

			private class OpponentLabel extends JPanel
			{
				private JLabel familiarIcon;
				private JPanel familiarLabel;

				public OpponentLabel( CakeArenaManager.ArenaOpponent opponent )
				{
					setLayout( new BorderLayout( 10, 10 ) );

					familiarLabel = new JPanel();
					familiarLabel.setLayout( new BoxLayout( familiarLabel, BoxLayout.Y_AXIS ) );

					String name = opponent.getName();
					familiarLabel.add( new JLabel( name, JLabel.LEFT ) );

					String race = opponent.getRace();
					familiarLabel.add( new JLabel( race, JLabel.LEFT ) );

					int weight = opponent.getWeight();
					familiarLabel.add( new JLabel( "(" + String.valueOf( weight ) + " lbs)", JLabel.LEFT ) );

					add( familiarLabel, BorderLayout.WEST );

					familiarIcon = new JLabel( FamiliarsDatabase.getFamiliarImage( opponent.getRace() ) );
					add( familiarIcon, BorderLayout.EAST );
				}
			}
		}

		private class ButtonPanel extends JPanel
		{
			JButton base;
			JButton buffed;
			JButton turns;
			JButton stop;
			// JButton debug;

			public ButtonPanel()
			{
				JPanel containerPanel = new JPanel( new GridLayout( 4, 1, 5, 5 ) );
				base = new JButton( "Base" );
				base.addActionListener( new BaseListener() );
				containerPanel.add( base );

				buffed = new JButton( "Buffed" );
				buffed.addActionListener( new BuffedListener() );
				containerPanel.add( buffed );

				turns = new JButton( "Turns" );
				turns.addActionListener( new TurnsListener() );
				containerPanel.add( turns );

				stop = new JButton( "Stop Training" );
				stop.addActionListener( new StopListener() );
				containerPanel.add( stop );

				// debug = new JButton( "Debug" );
				// debug.addActionListener( new DebugListener() );
				// this.add( debug );

				add( containerPanel );
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );

				if ( base != null )
					base.setEnabled( isEnabled );
				if ( buffed != null )
					buffed.setEnabled( isEnabled );
				if ( turns != null )
					turns.setEnabled( isEnabled );
			}

			private class BaseListener implements ActionListener, Runnable
			{
				public void actionPerformed( ActionEvent e )
				{	(new DaemonThread( this )).start();
				}

				public void run()
				{
					// Prompt for goal
					int goal = getQuantity( "Train up to what base weight?", 20, 20 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( client, goal, BASE );

					// Re-enable the display
					client.enableDisplay();
				}
			}

			private class BuffedListener implements ActionListener, Runnable
			{
				public void actionPerformed( ActionEvent e )
				{	(new DaemonThread( this )).start();
				}

				public void run()
				{
					// Prompt for goal
					int goal = getQuantity( "Train up to what buffed weight?", 48, 20 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( client, goal, BUFFED );

					// Re-enable the display
					client.enableDisplay();
				}
			}

			private class TurnsListener implements ActionListener, Runnable
			{
				public void actionPerformed( ActionEvent e )
				{	(new DaemonThread( this )).start();
				}

				public void run()
				{
					// Prompt for goal
					int goal = getQuantity( "Train for how many turns?", Integer.MAX_VALUE, 1 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( client, goal, TURNS );

					// Re-enable the display
					client.enableDisplay();
				}
			}

			private class StopListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	FamiliarTrainingFrame.stop = true;
				}
			}

			/*
			private class DebugListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	debug( client );
				}
			}
			*/
		}

		private class ResultsPanel extends JPanel
		{
			JEditorPane resultsDisplay;

			public ResultsPanel()
			{
				setLayout( new BorderLayout( 10, 10 ) );

				JEditorPane resultsDisplay = new JEditorPane();
				resultsDisplay.setEditable( false );
				results.setChatDisplay( resultsDisplay );

				JScrollPane scroller = new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
				JComponentUtilities.setComponentSize( scroller, 400, 400 );

				add( scroller, BorderLayout.CENTER );
			}
		}

		private class ChangePanel extends JPanel
		{
			private JComboBox familiars;
			private boolean isChanging = false;

			public ChangePanel()
			{
				familiars = new ChangeComboBox( KoLCharacter.getFamiliarList() );
				add( familiars );
			}

			public void setEnabled( boolean isEnabled )
			{
				super.setEnabled( isEnabled );

				if ( familiars != null )
					familiars.setEnabled( isEnabled );
			}

			private class ChangeComboBox extends JComboBox implements Runnable
			{
				public ChangeComboBox( LockableListModel selector )
				{
					super( selector );
					addActionListener( this );
				}

				public synchronized void actionPerformed( ActionEvent e )
				{
					if ( !isShowing() || isChanging || !isEnabled() )
						return;

					if ( e.paramString().endsWith( "=" ) )
						return;

					executeChange();
				}

				public synchronized void firePopupMenuWillBecomeInvisible()
				{
					super.firePopupMenuWillBecomeInvisible();

					if ( !isShowing() || isChanging || !isEnabled() )
						return;

					executeChange();
				}

				public synchronized void executeChange()
				{
					FamiliarData selection = (FamiliarData)getSelectedItem();

					if ( selection == null || selection == familiar )
						return;

					isChanging = true;
					(new DaemonThread( this )).start();
				}

				public void run()
				{
					FamiliarData selection = (FamiliarData)getSelectedItem();
					(new FamiliarRequest( client, selection )).run();
					isChanging = false;
					client.enableDisplay();
				}
			}
		}

		private class FamiliarRefresh implements Runnable
		{
			public void run()
			{
				FamiliarData data = KoLCharacter.getFamiliar();
				if ( data != familiar || weight != data.getWeight() )
				{
					familiar = data;
					weight = familiar.getWeight();
					familiarPanel.refreshFamiliar();
				}
			}
		}
	}

	private static boolean levelFamiliar( KoLmafia client, int goal, int type )
	{
		boolean buffs = client.getLocalBooleanProperty( "castBuffsWhileTraining" );
		boolean debug = client.getLocalBooleanProperty( "debugFamiliarTraining" );
		return levelFamiliar( client, goal, type, buffs, debug );
	}

	/**
	 * Utility method to level the current familiar by fighting the
	 * current arena opponents.
	 *
	 * @param	client	KoLmafia client
	 * @param	goal	Weight goal for the familiar
	 * @param	type	BASE, BUFF, or TURNS
	 * @param	buffs	true if should cast buffs during training
	 * @param	debug	true if we are debugging
	 */

	public static boolean levelFamiliar( KoLmafia client, int goal, int type, boolean buffs, boolean debug )
	{
		// Clear the output
		results.clearBuffer();

		// Permit training session to proceed
		stop = false;
		client.resetContinueState();

		// Get current familiar
		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			statusMessage( client, ERROR_STATE, "No familiar selected to train." );
			client.cancelRequest();
			return false;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus( client );

		// Identify the familiar we are training
		printFamiliar( status, goal, type );
		results.append( "<br>" );

		// Print available buffs and items and current buffs
		results.append( status.printCurrentBuffs() );
		results.append( status.printAvailableBuffs() );
		results.append( status.printCurrentEquipment() );
		results.append( status.printAvailableEquipment() );
		results.append( "<br>" );

		// Get opponent list
		LockableListModel opponents = CakeArenaManager.getOpponentList( client );

		// Print the opponents
		printOpponents( opponents );
		results.append( "<br>" );

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Let the battles begin!
		client.updateDisplay( DISABLE_STATE, "Starting training session." );

		// Iterate until we reach the goal
		while ( !goalMet( status, goal, type) )
		{
			// If user canceled, bail now
			if ( stop || !client.permitsContinue() )
			{
				statusMessage( client, ERROR_STATE, "Training session aborted." );
				client.cancelRequest();
				return false;
			}

			// Make sure you have an adventure left
			if ( KoLCharacter.getAdventuresLeft() < 1 )
			{
				statusMessage( client, ERROR_STATE, "Training stopped: out of adventures." );
				client.cancelRequest();
				return false;
			}

			// Make sure you have enough meat to pay for the contest
			if ( KoLCharacter.getAvailableMeat() < 100 )
			{
				statusMessage( client, ERROR_STATE, "Training stopped: out of meat." );
				client.cancelRequest();
				return false;
			}

			// Choose possible weights
			int [] weights = status.getWeights( buffs );

			if ( debug )
				printWeights( weights, buffs );

			// Choose next opponent
			CakeArenaManager.ArenaOpponent opponent = tool.bestOpponent( familiar.getID(), weights );

			if ( opponent == null )
			{
				statusMessage( client, ERROR_STATE, "Don't know how to train a " + familiar.getRace() + " yet." );
				client.cancelRequest();
				return false;
			}

			// Change into appropriate gear
			status.changeGear( tool.bestWeight(), buffs );
			if ( !client.permitsContinue() )
			{
				if ( buffs )
				{
					results.append( "Trying again without considering buffs...<br>" );
					client.resetContinueState();
					buffs = false;
					continue;
				}

				statusMessage( client, ERROR_STATE, "Training stopped: internal error." );
				client.cancelRequest();
				return false;
			}

			if ( debug )
				break;

			// Enter the contest
			fightMatch( client, status, tool, opponent );
		}

		statusMessage( client, NORMAL_STATE, "Training session completed." );
		return true;
	}

	/**
	 * Utility method to buff the current familiar to the specified weight
	 * or higher.
	 *
	 * @param	client	KoLmafia client
	 * @param	weight	Weight goal for the familiar
	 */

	public static boolean buffFamiliar( KoLmafia client, int weight )
	{
		// Get current familiar. If none, punt.
		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			client.updateDisplay( ERROR_STATE, "You don't have a familiar equipped." );
			client.cancelRequest();
			return false;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus( client );

		// Initially, allow buffs
		boolean buffs = true;

		// Try to buff and equip to reach goal
		while ( true )
		{
			// Find possible weights
			int [] weights = status.getWeights( buffs );

			// Examine list and see if it is possible
			int goal = 0;
			for (int i = 0; i < weights.length; ++i )
			{
				goal = weights[i];
				if ( goal >= weight )
					break;
			}

			// Punt if goal is not possible
			if ( goal < weight )
				break;

			// Change into appropriate gear
			status.changeGear( goal, buffs );

			// See if we succeeded
			if ( client.permitsContinue() )
				return true;

			// If we failed using only equipment, punt.
			if ( !buffs )
				break;

			// Perhaps we failed to cast a buff. Try again
			// using nothing but equipment.
			client.resetContinueState();
			buffs = false;
		}

		client.updateDisplay( ERROR_STATE, "Can't buff and equip familiar to reach " + weight + " lbs." );
		client.cancelRequest();
		return false;
	}

	private static void statusMessage( KoLmafia client, int state, String message )
	{
		results.append( message + "<br>" );
		client.updateDisplay( state, message );
	}

	private static void printFamiliar( FamiliarStatus status, int goal, int type )
	{
		FamiliarData familiar = status.getFamiliar();
		String name = familiar.getName();
		String race = familiar.getRace();
		int weight = familiar.getWeight();
		String hope = "";

		if ( type == BASE )
			hope = " to " + goal + " lbs. base weight";
		else if ( type == BUFFED )
			hope = " to " + goal + " lbs. buffed weight";
		else if ( type == TURNS )
			hope = " for " + goal + " turns";

		results.append( "Training " + name + " the " + weight + " lb. " + race + hope +	 ".<br>" );
	}

	private static void printOpponents( LockableListModel opponents )
	{
		results.append( "Opponents:<br>" );
		int opponentCount = opponents.size();
		for ( int i = 0; i < opponentCount; ++i )
		{
			CakeArenaManager.ArenaOpponent opponent = (CakeArenaManager.ArenaOpponent)opponents.get( i );
			String name = opponent.getName();
			String race = opponent.getRace();
			int weight = opponent.getWeight();
			results.append( name + " the " + weight + " lb. " + race + "<br>");
		}
	}

	private static boolean goalMet( FamiliarStatus status, int goal, int type )
	{
		switch (type)
		{
		case BASE:
			return ( status.baseWeight() >= goal );

		case BUFFED:
			return ( status.maxBuffedWeight() >= goal );

		case TURNS:
			return ( status.turnsUsed() >= goal );
		}

		return false;
	}

	private static void printWeights( int [] weights, boolean buffs )
	{
		StringBuffer text = new StringBuffer();

		text.append( "Possible familiar weights" );
		if ( buffs )
			text.append( " (including castable buffs)" );
		text.append( ":");
		for (int i = 0; i < weights.length; ++i )
		{
			text.append( ( i > 0 ) ? ", " : " ");
			text.append( weights[i] );
		}
		text.append( "<br>");

		results.append( text.toString() );
	}

	private static void printMatch( FamiliarStatus status, CakeArenaManager.ArenaOpponent opponent, FamiliarTool tool )
	{
		FamiliarData familiar = status.getFamiliar();
		int weight = tool.bestWeight();
		int diff = tool.difference();

		StringBuffer text = new StringBuffer();
		int round = ( status.turnsUsed() + 1);
		text.append( "Round " + round + ": " );
		text.append( familiar.getName() );
		text.append( " (" + weight + " lbs." );
		if ( diff != 0 )
		{
			text.append( "; optimum = " );
			text.append( weight - diff );
			text.append( " lbs." );
		}
		text.append( ") vs. " + opponent.getName() );
		text.append( " in the " + CakeArenaManager.getEvent( tool.bestMatch() ) );
		text.append( " event.<br>" );

		results.append( text.toString() );

		client.updateDisplay( DISABLE_STATE, "Round " + round + ": " + familiar.getName() + " vs. " + opponent.getName() + "..." );
	}

	private static void fightMatch( KoLmafia client, FamiliarStatus status, FamiliarTool tool, CakeArenaManager.ArenaOpponent opponent )
	{
		// If user aborted, bail now
		if ( !client.permitsContinue())
			return;

		// Tell the user about the match
		printMatch( status, opponent, tool );

		// Run the match
		KoLRequest request = new CakeArenaRequest( client, opponent.getID(), tool.bestMatch() );
		request.run();

		// Pass the response text to the FamiliarStatus to
		// add familiar items and deduct a turn.
		String text = status.processMatchResult( request.responseText );
		results.append( text );
	}

	/**
	 * A class to hold everything that can modify the weight of the
	 * current familiar:
	 *
	 *    available items and whether they are equipped
	 *    available skills and whether they are active
	 */
	private static class FamiliarStatus
	{
		// Client
		KoLmafia client;

		// The familiar we are tracking
		FamiliarData familiar;

		// How many turns we have trained it
		int turns;

		// Details about its special familiar item
		AdventureResult familiarItem ;
		int familiarItemWeight;

		// Available skills which affect weight
		boolean sympathyAvailable;
		boolean leashAvailable;
		boolean empathyAvailable;

		// Active effects which affect weight
		int leashActive;
		int empathyActive;
		int greenTongueActive;
		int blackTongueActive;

		// Currently equipped gear which affects weight
		AdventureResult hat;
		AdventureResult item;
		AdventureResult [] acc = new AdventureResult [3];

		// Available equipment which affects weight
		AdventureResult pithHelmet;
		AdventureResult specItem;
		int specWeight;
		AdventureResult leadNecklace;
		FamiliarData leadNecklaceOwner;
		AdventureResult ratHeadBalloon;
		FamiliarData ratHeadBalloonOwner;
		int tpCount;
		AdventureResult [] tp = new AdventureResult [3];

		// Weights
		TreeSet weights;

		// Gear sets
		ArrayList gearSets;

		public FamiliarStatus( KoLmafia client )
		{
			// Save client for later use
			this.client = client;

			// Find out which familiar we are working with
			familiar = KoLCharacter.getFamiliar();

			// Get details about the special item it can wear
			String name = FamiliarsDatabase.getFamiliarItem( familiar.getID() );
			familiarItem = new AdventureResult( name, 1, false );
			familiarItemWeight = FamiliarData.itemWeightModifier( familiarItem.getItemID() );

			// Check available skills
			checkSkills();

			// Check current equipment
			checkCurrentEquipment();

			// Check available equipment
			checkAvailableEquipment( KoLCharacter.getInventory() );

			// No turns have been used yet
			turns = 0;

			// Initialize set of weights
			weights = new TreeSet();

			// Initialize the list of GearSets
			gearSets = new ArrayList();
		}

		/*
		// Debug initializer
		public FamiliarStatus( KoLmafia client,
				       FamiliarData familiar,
				       boolean sympathyAvailable,
				       boolean leashAvailable,
				       boolean empathyAvailable,
				       int leashActive,
				       int empathyActive,
				       AdventureResult hat,
				       AdventureResult item,
				       AdventureResult acc1,
				       AdventureResult acc2,
				       AdventureResult acc3,
				       LockableListModel inventory )
		{
			// Save client for later use
			this.client = client;

			// Find out which familiar we are working with
			this.familiar = familiar;

			// Get details about the special item it can wear
			String name = FamiliarsDatabase.getFamiliarItem( familiar.getID() );
			familiarItem = new AdventureResult( name, 1, false );
			familiarItemWeight = FamiliarData.itemWeightModifier( familiarItem.getItemID() );

			// Check available skills
			this.sympathyAvailable = sympathyAvailable;
			this.leashAvailable = leashAvailable;
			this.empathyAvailable = empathyAvailable;
			this.leashActive = leashActive;
			this.empathyActive = empathyActive;
			this.greenTongueActive = 0;
			this.blackTongueActive = 0;

			// Check current equipment
			checkCurrentEquipment( hat, item, acc1, acc2, acc3 );

			// Check available equipment
			checkAvailableEquipment( inventory );

			// No turns have been used yet
			turns = 0;

			// Initialize set of weights
			weights = new TreeSet();

			// Initialize the list of GearSets
			gearSets = new ArrayList();
		}
		*/

		private void checkSkills()
		{
			// Look at skills to decide which ones are possible
			sympathyAvailable = KoLCharacter.hasAmphibianSympathy();
			empathyAvailable = KoLCharacter.hasSkill( "Empathy of the Newt" );
			leashAvailable = KoLCharacter.hasSkill( "Leash of Linguini" );

			// Look at effects to decide which ones are active;
			LockableListModel active = KoLCharacter.getEffects();
			empathyActive = EMPATHY.getCount( active );
			leashActive = LEASH.getCount( active );
			greenTongueActive = GREEN_TONGUE.getCount( active );
			blackTongueActive = BLACK_TONGUE.getCount( active );
		}

		private void checkCurrentEquipment()
		{	checkCurrentEquipment( KoLCharacter.getCurrentEquipment( KoLCharacter.HAT ),
					       KoLCharacter.getCurrentEquipment( KoLCharacter.FAMILIAR ),
					       KoLCharacter.getCurrentEquipment( KoLCharacter.ACCESSORY1 ),
					       KoLCharacter.getCurrentEquipment( KoLCharacter.ACCESSORY2 ),
					       KoLCharacter.getCurrentEquipment( KoLCharacter.ACCESSORY3 ) );
		}

		private void checkCurrentEquipment( AdventureResult hat,
						    AdventureResult item,
						    AdventureResult acc1,
						    AdventureResult acc2,
						    AdventureResult acc3 )
		{
			// Initialize equipment to default
			this.hat = null;
			this.item = null;
			this.acc[0] = null;
			this.acc[1] = null;
			this.acc[2] = null;

			pithHelmet = null;
			specItem = null;
			specWeight = 0;
			leadNecklace = null;
			leadNecklaceOwner = null;
			ratHeadBalloon = null;
			ratHeadBalloonOwner = null;
			tpCount = 0;

			// Check hat for pithiness
			if ( hat != null && hat.getName().equals( PITH_HELMET.getName() ) )
				this.hat = pithHelmet = PITH_HELMET;

			// Check current familiar item
			if ( item != null )
			{
				String name = item.getName();
				if ( name.equals( familiarItem.getName() ) )
				{
					this.item = specItem = familiarItem;
					specWeight = familiarItemWeight;
				}
				else if ( name.equals( LEAD_NECKLACE.getName() ) )
				{
					this.item = leadNecklace = LEAD_NECKLACE;
					leadNecklaceOwner = familiar;
				}
				else if ( name.equals( RAT_HEAD_BALLOON.getName() ) )
				{
					this.item = ratHeadBalloon = RAT_HEAD_BALLOON;
					ratHeadBalloonOwner = familiar;
				}
			}

			// Check accessories for tininess and plasticity
			checkAccessory( 0, acc1 );
			checkAccessory( 1, acc2 );
			checkAccessory( 2, acc3 );
		}

		private void checkAccessory( int index, AdventureResult accessory )
		{
			if ( isTinyPlasticItem( accessory ) )
			{
				acc[ index] = accessory;
				tp[ tpCount++ ] = accessory;
			}
		}

		public boolean isTinyPlasticItem( AdventureResult ar )
		{
			if ( ar != null )
			{
				int id = ar.getItemID();
				if ( id >= firstTinyPlastic && id <= lastTinyPlastic )
					return true;
				if ( id >= firstTinyPlasticCrimbo && id <= lastTinyPlasticCrimbo )
					return true;
				return false;
			}
			return false;
		}

		private void checkAvailableEquipment( LockableListModel inventory )
		{
			// If not wearing a pith helmet, search inventory
			if ( pithHelmet == null &&
			     PITH_HELMET.getCount( inventory ) > 0 &&
			     EquipmentDatabase.canEquip( "plexiglass pith helmet" ) )
				pithHelmet = PITH_HELMET;

			// If current familiar item is not the special item and
			// such an item affects weight, search inventory
			if ( familiarItemWeight != 0 &&
			     !familiarItem.equals( item ) &&
			     familiarItem.getCount( inventory ) > 0 )
			{
				specItem = familiarItem;
				specWeight = familiarItemWeight;
			}

			// If current familiar is not wearing a lead necklace,
			// search inventory
			if ( leadNecklace == null &&
			     LEAD_NECKLACE.getCount( inventory ) > 0 )
			{
				leadNecklace = LEAD_NECKLACE;
				leadNecklaceOwner = null;
			}

			// If current familiar is not wearing a rat head
			// balloon, search inventory
			if ( ratHeadBalloon == null &&
			     RAT_HEAD_BALLOON.getCount( inventory ) > 0)
			{
				ratHeadBalloon = RAT_HEAD_BALLOON;
				ratHeadBalloonOwner = null;
			}

			// If we don't have a lead necklace or a rat head
			// balloon, search other familiars; we'll steal it from
			// them if necessary
			if ( leadNecklace == null || ratHeadBalloon == null )
			{
				// Find first familiar with item
				LockableListModel familiars = KoLCharacter.getFamiliarList();
				for ( int i = 0; i < familiars.size(); ++i )
				{
					FamiliarData familiar = (FamiliarData)familiars.get(i);
					String item = familiar.getItem();
					if ( item == null )
						continue;

					if ( item.equals( "lead necklace") )
					{
						// We found a lead necklace
						if ( leadNecklace == null )
						{
							leadNecklace = LEAD_NECKLACE;
							leadNecklaceOwner = familiar;
						}

						// Continue looking for balloon
						if ( ratHeadBalloon == null )
							continue;

						// Both are available
						break;
					}

					if ( item.equals( "rat head balloon") )
					{
						// We found a balloon
						if ( ratHeadBalloon == null )
						{
							ratHeadBalloon = RAT_HEAD_BALLOON;
							ratHeadBalloonOwner = familiar;
						}

						// Continue looking for necklace
						if ( leadNecklace == null )
							continue;

						// Both are available
						break;
					}
				}
			}

			// If equipped with fewer than three tiny plastic items
			// equipped, search inventory for more
			for ( int i = firstTinyPlastic; i <= lastTinyPlastic; ++i )
				addTinyPlastic( i, inventory );

			// Check Tiny Plastic Crimbo objects
			for ( int i = firstTinyPlasticCrimbo; i <= lastTinyPlasticCrimbo; ++i )
				addTinyPlastic( i, inventory );
		}

		private void addTinyPlastic( int id, LockableListModel inventory )
		{
			if ( tpCount == 3 )
				return;

			AdventureResult ar = new AdventureResult( id, 1 );
			int count = ar.getCount( inventory );

			// Make a new one for each slot
			while ( count-- > 0 && tpCount < 3 )
				tp[ tpCount++ ] = new AdventureResult( id, 1 );
		}

		/**************************************************************/

		public int [] getWeights( boolean buffs )
		{
			// Clear the set of possible weights
			weights.clear();

			// Calculate base weight
			int weight = familiar.getWeight();
			if ( sympathyAvailable )
				weight += 5;
			if ( empathyActive > 0 )
				weight += 5;
			if ( leashActive > 0 )
				weight += 5;

			// One snowcone effect at a time
			if ( greenTongueActive > 0 || blackTongueActive > 0 )
				weight += 5;

			// Start with buffs
			getBuffWeights( weight, buffs );

			// Make an array to hold values
			Object [] vals = weights.toArray();
			int [] value = new int[ vals.length ];

			// Read Integers from the set and store ints
			for ( int i = 0; i < vals.length; ++i )
				value[i] = ((Integer)vals[i]).intValue();

			return value;
		}

		private void getBuffWeights( int weight, boolean buffs )
		{
			if ( buffs )
			{
				if ( empathyAvailable && empathyActive == 0 )
					getItemWeights( weight + 5 );

				if ( leashAvailable && leashActive == 0 )
					getItemWeights( weight + 5 );

				if ( empathyAvailable && leashAvailable &&
				     empathyActive == 0 && leashActive == 0 )
					getItemWeights( weight + 10 );
			}

			// Calculate item weights with no additional buffs
			getItemWeights( weight );
		}

		private void getItemWeights( int weight )
		{
			// If familiar specific item adds weight, calculate
			if ( specWeight != 0 )
				getAccessoryWeights( Math.max( 1, weight + specWeight ) );

			// If we have a lead necklace, use it
			if ( leadNecklace != null )
				getAccessoryWeights( weight + 3);

			// If we have a rat head balloon, use it
			if ( ratHeadBalloon != null )
				getAccessoryWeights( Math.max( 1, weight -3) );

			// Calculate Accessory Weights with no Familiar Items
			getAccessoryWeights( weight );
		}

		private void getAccessoryWeights( int weight )
		{
			// Calculate using variable #s of tiny plastic objects
			for ( int i = 0; i < tpCount; ++i )
				getHatWeights( weight + i + 1 );

			// Calculate Hat Weights with no accessories
			getHatWeights( weight );
		}

		private void getHatWeights( int weight )
		{
			// Add weight with helmet
			if ( pithHelmet != null )
				weights.add( new Integer( weight + 5 ) );

			// Add weight with no helmet
			weights.add( new Integer( Math.max( weight, 1 ) ) );
		}

		/**************************************************************/

		/*
		 * Change gear and cast buffs such that your familiar's
		 * modified weight is as specified.
		 */
		public void changeGear( int weight, boolean buffs )
		{
			// Make a GearSet describing what we have now
			GearSet current = new GearSet();

			// If we are already suitably equipped, stop now
			if ( weight == current.weight() )
				return;

			// Choose a new GearSet with desired weight
			GearSet next = chooseGearSet( current, weight, buffs );

			// If we couldn't pick one, that's an internal error
			if ( next == null || weight != next.weight() )
			{
				results.append( "Internal error: could not select gear set to achieve " + weight + " lbs.<br>" );
				if ( next == null )
					results.append( "No gear set found.<br>" );
				else
					results.append( "Selected gear set provides " + next.weight() + " lbs.<br>" );
				client.cancelRequest();
				return;
			}

			// Change into the new GearSet
			changeGear( current, next);
		}

		/*
		 * Debug: choose and print desired GearSet
		 */
		public void chooseGear( int weight, boolean buffs )
		{
			// Make a GearSet describing what we have now
			GearSet current = new GearSet();

			// If we are already suitably equipped, stop now
			if ( weight == current.weight() )
			{
				results.append( "Current gear is acceptable/<br>" );
				return;
			}

			// Choose a new GearSet with desired weight
			GearSet next = chooseGearSet( current, weight, buffs );

			if ( next == null )
			{
				results.append( "Could not find a gear set to achieve " + weight + " lbs.<br>" );
				return;
			}

			results.append( "Chosen gear set: " + next + " provides " + next.weight() + " lbs.<br>" );
		}

		/*
		 * Swap gear and cast buffs to match desired GearSet.
		 * Return false if failed to swap or buff
		 */

		public void changeGear( GearSet current, GearSet next )
		{
			swapItem( current.hat, next.hat, KoLCharacter.HAT );
			swapItem( current.item, next.item, KoLCharacter.FAMILIAR );
			swapItem( current.acc1, next.acc1, KoLCharacter.ACCESSORY1 );
			swapItem( current.acc2, next.acc2, KoLCharacter.ACCESSORY2 );
			swapItem( current.acc3, next.acc3, KoLCharacter.ACCESSORY3 );
			castNeededBuffs( current, next );
		}

		private void swapItem( AdventureResult current, AdventureResult next, int slot )
		{
			// Nothing to do if already wearing this item
			if ( current == next )
				return;

			// Take off the item we are wearing in this slot
			//
			// Note that we only notice items that affect familiar
			// weight. Luckily, EquipmentRequest will notice if
			// something else is in the slot and will remove it
			// first, if necessary
			if ( current != null )
			{
				results.append( "Taking off " + current.getName() + "<br>" );
				(new EquipmentRequest( client, EquipmentRequest.UNEQUIP, slot)).run();
				setItem( slot, null );
			}

			// Steal a lead necklace, if needed
			if ( next == leadNecklace && leadNecklaceOwner != null && leadNecklaceOwner != familiar )
			{
				results.append( "Stealing lead necklace from " + leadNecklaceOwner.getName() + " the " + leadNecklaceOwner.getRace() + "<br>" );
				stealFamiliarItem( leadNecklaceOwner );
				leadNecklaceOwner = familiar;
			}

			// Steal a rat head balloon necklace, if needed
			if ( next == ratHeadBalloon && ratHeadBalloonOwner != null && ratHeadBalloonOwner != familiar )
			{
				results.append( "Stealing rat head balloon from " + ratHeadBalloonOwner.getName() + " the " + ratHeadBalloonOwner.getRace() + "<br>" );
				stealFamiliarItem( ratHeadBalloonOwner );
				ratHeadBalloonOwner = familiar;
			}

			// Finally, equip the new item
			if ( next != null )
			{
				String name = next.getName();
				results.append( "Putting on " + name + "<br>" );
				(new EquipmentRequest( client, name, slot)).run();
				setItem( slot, next );
			}
		}

		private void setItem( int slot, AdventureResult item )
		{
			if ( slot == KoLCharacter.HAT )
				hat = item;
			else if ( slot == KoLCharacter.FAMILIAR )
				this.item = item;
			else if ( slot == KoLCharacter.ACCESSORY1 )
				acc[0] = item;
			else if ( slot == KoLCharacter.ACCESSORY2 )
				acc[1] = item;
			else if ( slot == KoLCharacter.ACCESSORY3 )
				acc[2] = item;
		}

		private void stealFamiliarItem( FamiliarData owner )
		{
			// Switch to other familiar
			(new FamiliarRequest( client, owner )).run();

			// Unequip item
			(new EquipmentRequest( client, EquipmentRequest.UNEQUIP, KoLCharacter.FAMILIAR )).run();

			// Equip original familiar
			(new FamiliarRequest( client, familiar )).run();
		}

		private void castNeededBuffs( GearSet current, GearSet next )
		{
			if ( next.leash && !current.leash )
			{
				// Cast Leash. Bail if can't.
				(new UseSkillRequest( client, "Leash of Linguini", null, 1 )).run();
				if ( !client.permitsContinue())
					return;

				// Remember it
				leashActive += 10;
			}

			if ( next.empathy && !current.empathy )
			{
				// Cast Empathy. Bail if can't.
				(new UseSkillRequest( client, "Empathy of the Newt", null, 1 )).run();
				if ( !client.permitsContinue())
					return;

				// Remember it
				empathyActive += 10;
			}
		}

		/*
		 * Choose a GearSet that gives the current familiar the desired
		 * weight. Of the (potentially many) possible such sets, return
		 * the one that requires the smallest number of changes from
		 * what is currently in effect.
		 */
		private GearSet chooseGearSet( GearSet current, int weight, boolean buffs )
		{
			// Clear out the accumulated list of GearSets
			gearSets.clear();

			// Start with buffs
			getBuffGearSets( weight, buffs );

			// Iterate over all the GearSets and choose the first
			// one which is closest to the current GearSet
			GearSet choice = null;
			int choiceSwaps = Integer.MAX_VALUE;
			int count = gearSets.size();
			for ( int i = 0; i < count; ++i )
			{
				GearSet gear = (GearSet)gearSets.get( i );
				int swaps = gear.compareTo( current );
				if ( swaps < choiceSwaps )
				{
					choice = gear;
					choiceSwaps = swaps;
				}
			}

			return choice;
		}

		private void getBuffGearSets( int weight, boolean buffs )
		{
			if ( leashActive > 0 && empathyActive > 0 )
				getHatGearSets( weight, true, true );
			else if ( leashActive > 0 )
			{
				getHatGearSets( weight, true, false );
				if ( buffs && empathyAvailable )
					getHatGearSets( weight, true, true );
			}
			else if ( empathyActive > 0 )
			{
				getHatGearSets( weight, false, true );
				if ( buffs && leashAvailable )
					getHatGearSets( weight, true, true );
			}
			else if ( buffs )
			{
				getHatGearSets( weight, false, false );
				if ( leashAvailable )
					getHatGearSets( weight, true, false );
				if ( empathyAvailable )
					getHatGearSets( weight, false, true );
				if ( leashAvailable && empathyAvailable)
					getHatGearSets( weight, true, true );
			}
			else
				getHatGearSets( weight, false, false );
		}

		private void getHatGearSets( int weight, boolean leash, boolean empathy )
		{
			if ( pithHelmet != null )
				getItemGearSets( weight, pithHelmet, leash, empathy );

			getItemGearSets( weight, null, leash, empathy );
		}

		private void getItemGearSets( int weight, AdventureResult hat, boolean leash, boolean empathy )
		{
			if ( specItem != null )
				getAccessoryGearSets( weight, specItem, hat, leash, empathy );
			if ( leadNecklace != null )
				getAccessoryGearSets( weight, leadNecklace, hat, leash, empathy );
			if ( ratHeadBalloon != null )
				getAccessoryGearSets( weight, ratHeadBalloon, hat, leash, empathy );
			getAccessoryGearSets( weight, null, hat, leash, empathy );
		}

		private void getAccessoryGearSets( int weight, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy )
		{
			// No matter how many Tiny Plastic Objects we have, a
			// configuration with none equipped is legal
			addGearSet( weight, null, null, null, item, hat, leash, empathy );
			if ( tpCount == 0 )
				return;

			// If we have at least one and it started out equipped,
			// then it might be in any of the three accessory
			// slots.
			addGearSet( weight, tp[0], null, null, item, hat, leash, empathy );
			addGearSet( weight, null, tp[0], null, item, hat, leash, empathy );
			addGearSet( weight, null, null, tp[0], item, hat, leash, empathy );
			if ( tpCount == 1)
				return;

			// If we have at least two and they were both equipped
			// when we came in, they'll be in one of the following
			// patterns.

			addGearSet( weight, tp[0], tp[1], null, item, hat, leash, empathy );
			addGearSet( weight, tp[0], null, tp[1], item, hat, leash, empathy );
			addGearSet( weight, null, tp[0], tp[1], item, hat, leash, empathy );

			// If one of the two was in the inventory, the first
			// could have been in any of the three accessory
			// slots. Add a pattern for where it was in the third
			// slot.

			addGearSet( weight, tp[1], null, tp[0], item, hat, leash, empathy );

			if ( tpCount == 2 )
				return;

			// If we have three and they were all equipped when we
			// came in, they'll be in the following pattern
			addGearSet( weight, tp[0], tp[1], tp[2], item, hat, leash, empathy );

			// If two of them were equipped and the third was in
			// the inventory, the following patterns are legal
			addGearSet( weight, tp[0], tp[2], tp[1], item, hat, leash, empathy );
			addGearSet( weight, tp[2], tp[0], tp[1], item, hat, leash, empathy );

			// If only one was equipped, based on which
			// two-accessory patterns are legal, the following
			// three-accessory patterns are also legal
			addGearSet( weight, tp[1], tp[2], tp[0], item, hat, leash, empathy );
		}

		private void addGearSet( int weight, AdventureResult acc1, AdventureResult acc2, AdventureResult acc3, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy )
		{
			if ( weight == gearSetWeight( acc1, acc2, acc3, item, hat, leash, empathy ) )
				gearSets.add( new GearSet( hat, item, acc1, acc2, acc3, leash, empathy ) );
		}

		private int gearSetWeight( AdventureResult acc1, AdventureResult acc2, AdventureResult acc3, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy )
		{
			int weight = familiar.getWeight();

			if ( sympathyAvailable )
				weight += 5;

			// One snowcone effect at a time
			if ( greenTongueActive > 0 || blackTongueActive > 0 )
				weight += 5;

			if ( hat == PITH_HELMET )
				weight += 5;

			if ( item == specItem )
				weight += specWeight;
			else if ( item == LEAD_NECKLACE )
				weight += 3;
			else if ( item == RAT_HEAD_BALLOON )
				weight -= 3;

			if ( isTinyPlasticItem( acc1 ) )
				weight += 1;
			if ( isTinyPlasticItem( acc2 ) )
				weight += 1;
			if ( isTinyPlasticItem( acc3 ) )
				weight += 1;

			if ( leash )
				weight += 5;
			if ( empathy )
				weight += 5;

			return Math.max( weight, 1 );
		}

		/**************************************************************/

		public String processMatchResult( String response )
		{
			// If the contest did not take place, bail now
			if ( response.indexOf( "You enter" ) == -1 )
				return "";

			Matcher matcher;
			StringBuffer text = new StringBuffer();

			// Find and report how much experience was gained
			matcher = Pattern.compile( "gains (\\d+) experience" ).matcher( response );
			String message;
			if ( matcher.find() )
				message = familiar.getName() + " gains " + matcher.group(1) + " experience" + ( response.indexOf( "gains a pound" ) != -1 ? " and a pound." : "." );

			else
				message = familiar.getName() + " lost.";
			statusMessage( client, NORMAL_STATE, message );

			// If a prize was won, report it
			matcher = Pattern.compile( "You acquire an item: <b>(.*?)</b>" ).matcher( response );
			if ( matcher.find() )
			{
				String prize = matcher.group(1);
				statusMessage( client, NORMAL_STATE, "You win a prize: " + prize + "." );
				if ( prize.equals( LEAD_NECKLACE.getName() ) )
				{
					if ( leadNecklace == null )
					{
						leadNecklace = LEAD_NECKLACE;
						leadNecklaceOwner = familiar;
					}
				}
				else if ( familiarItemWeight > 0 )
				{
					if ( specItem == null )
					{
						specItem = familiarItem;
						specWeight = familiarItemWeight;
					}
				}
			}

			// Tell KoLmafia that we've used an adventure
			client.processResult( ADV );

			// Increment count of turns in this training session
			turns++;

			// Decrement buffs
			if ( leashActive > 0 )
				leashActive--;
			if ( empathyActive > 0 )
				empathyActive--;
			if ( greenTongueActive > 0 )
				greenTongueActive--;
			if ( blackTongueActive > 0 )
				blackTongueActive--;

			text.append( "<br>" );
			return text.toString();
		}

		public FamiliarData getFamiliar()
		{	return familiar;
		}

		public int turnsUsed()
		{	return turns;
		}

		public int baseWeight()
		{	return familiar.getWeight();
		}

		public int maxBuffedWeight()
		{
			// Start with current base weight of familiar
			int weight = familiar.getWeight();

			// Add possible skills
			if ( sympathyAvailable )
				weight += 5;
			if ( leashAvailable )
				weight += 5;
			if ( empathyAvailable )
				weight += 5;

			// Add available familiar items
			if ( specWeight > 3 )
				weight += specWeight;
			else if (leadNecklace != null)
				weight += 3;

			// Add available tiny plastic items
			weight += tpCount;

			// Add pith helmet
			if ( pithHelmet != null )
				weight += 5;

			return weight;
		}

		public String printAvailableBuffs()
		{
			StringBuffer text = new StringBuffer();

			text.append( "Castable buffs:" );
			if ( empathyAvailable )
				text.append( " Empathy (+5)" );
			if ( leashAvailable )
				text.append( " Leash (+5)" );
			if ( !empathyAvailable && !leashAvailable )
				text.append( " None" );
			text.append( "<br>" );

			return text.toString();
		}

		public String printCurrentBuffs()
		{
			StringBuffer text = new StringBuffer();

			text.append( "Current buffs:" );
			if ( sympathyAvailable )
				text.append( " Sympathy (+5 permanent)" );
			if ( empathyActive > 0 )
				text.append( " Empathy (+5 for " + empathyActive + " turns)" );
			if ( leashActive > 0 )
				text.append( " Leash (+5 for " + leashActive + " turns)" );
			if ( greenTongueActive > 0 )
				text.append( " Green Tongue (+5 for " + greenTongueActive + " turns)" );
			if ( blackTongueActive > 0 )
				text.append( " Black Tongue (+5 for " + blackTongueActive + " turns)" );
			if ( !sympathyAvailable && empathyActive == 0 && leashActive == 0 && greenTongueActive == 0 && blackTongueActive == 0 )
				text.append( " None" );
			text.append( "<br>" );

			return text.toString();
		}

		public String printCurrentEquipment()
		{
			StringBuffer text = new StringBuffer();

			text.append( "Current equipment:" );
			if ( hat == PITH_HELMET)
				text.append( " plexiglass pith helmet (+5)" );
			if ( item == LEAD_NECKLACE )
				text.append( " lead necklace (+3)" );
			else if ( item == RAT_HEAD_BALLOON )
				text.append( " rat head balloon (-3)" );
			else if ( item != null )
				text.append( " " + specItem.getName() + " (+" + specWeight + ")" );
			for ( int i = 0; i < 3; ++i )
				if ( acc[i] != null )
					text.append( " " + acc[i].getName() + " (+1)" );
			text.append( "<br>" );

			return text.toString();
		}

		public String printAvailableEquipment()
		{
			StringBuffer text = new StringBuffer();

			text.append( "Available equipment:" );
			if ( pithHelmet != null )
				text.append( " plexiglass pith helmet (+5)" );
			if ( specItem != null )
				text.append( " " + specItem.getName() + " (+" + specWeight + ")" );
			if ( leadNecklace != null)
				text.append( " lead necklace (+3)" );
			if ( ratHeadBalloon != null)
				text.append( " rat head balloon (-3)" );
			for ( int i = 0; i < tpCount; ++i )
				text.append( " " + tp[i].getName() + " (+1)" );
			text.append( "<br>" );

			return text.toString();
		}

		private class GearSet
		{
			public AdventureResult hat;
			public AdventureResult item;
			public AdventureResult acc1;
			public AdventureResult acc2;
			public AdventureResult acc3;
			public boolean leash;
			public boolean empathy;

			public GearSet( AdventureResult hat,
					AdventureResult item,
					AdventureResult acc1,
					AdventureResult acc2,
					AdventureResult acc3,
					boolean leash,
					boolean empathy )
			{
				this.hat = hat;
				this.item = item;
				this.acc1 = acc1;
				this.acc2 = acc2;
				this.acc3 = acc3;
				this.leash = leash;
				this.empathy = empathy;
			}

			public GearSet()
			{
				this( FamiliarStatus.this.hat,
				      FamiliarStatus.this.item,
				      FamiliarStatus.this.acc[0],
				      FamiliarStatus.this.acc[1],
				      FamiliarStatus.this.acc[2],
				      FamiliarStatus.this.leashActive > 0,
				      FamiliarStatus.this.empathyActive > 0);
			}

			public int weight()
			{	return gearSetWeight( acc1, acc2, acc3, item, hat, leash, empathy );
			}

			public int compareTo( GearSet that )
			{
				int changes = 0;

				if ( this.hat != that.hat )
					changes++;
				if ( this.item != that.item )
					changes++;
				if ( this.acc1 != that.acc1 )
					changes++;
				if ( this.acc2 != that.acc2 )
					changes++;
				if ( this.acc3 != that.acc3 )
					changes++;
				if ( this.leash != that.leash )
					changes++;
				if ( this.empathy != that.empathy )
					changes++;

				return changes;
			}

			public String toString()
			{
				StringBuffer text = new StringBuffer();
				text.append( "(" );
				if ( hat == null )
					text.append( "null" );
				else
					text.append( hat.getItemID() );
				text.append( ", " );
				if ( item == null )
					text.append( "null" );
				else
					text.append( item.getItemID() );
				text.append( ", " );
				if ( acc1 == null )
					text.append( "null" );
				else
					text.append( acc1.getItemID() );
				text.append( ", " );
				if ( acc2 == null )
					text.append( "null" );
				else
					text.append( acc2.getItemID() );
				text.append( ", " );
				if ( acc3 == null )
					text.append( "null" );
				else
					text.append( acc3.getItemID() );
				text.append( ", " );
				text.append( leash );
				text.append( ", " );
				text.append( empathy );
				text.append( ")" );
				return text.toString();
			}
		}
	}

	// Debug methods

	/*
	private static LockableListModel debugOpponents = new LockableListModel();
	static
	{
		debugOpponents.add( new CakeArenaManager.ArenaOpponent( 1, "Dirty Pair", "Fuzzy Dice", 15 ) );
		debugOpponents.add( new CakeArenaManager.ArenaOpponent( 2, "Radi O'Kol", "Leprechaun", 10 ) );
		debugOpponents.add( new CakeArenaManager.ArenaOpponent( 3, "Captain Scapula", "Spooky Pirate Skeleton", 15 ) );
		debugOpponents.add( new CakeArenaManager.ArenaOpponent( 4, "Queso Ardilla", "Hovering Sombrero", 10 ));
		debugOpponents.add( new CakeArenaManager.ArenaOpponent( 5, "Optimus Pram", "MagiMechTech MicroMechaMech", 7 ) );
	}

	private static FamiliarData debugFamiliar = new FamiliarData( 19, "Creepy", 2, "skewer-mounted razor blade" );

	private static void debug( KoLmafia client )
	{
		LockableListModel inventory = new LockableListModel();
		inventory.add( new AdventureResult( "lead necklace", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic angry goat", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic stab bat", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic cocoabo", 1 ) );

		FamiliarStatus status = new FamiliarStatus( client,
							    debugFamiliar,
							    true,
							    true,
							    false,
							    0,
							    0,
							    null,
							    new AdventureResult( "skewer-mounted razor blade" ),
							    null,
							    null,
							    null,
							    inventory );

		// Identify the familiar we are training
		printFamiliar( status, 20, BASE );
		results.append( "<br>" );

		// Print available buffs and items and current buffs
		results.append( status.printCurrentBuffs() );
		results.append( status.printAvailableBuffs() );
		results.append( status.printCurrentEquipment() );
		results.append( status.printAvailableEquipment() );
		results.append( "<br>" );

		// Print the opponents
		printOpponents( debugOpponents );
		results.append( "<br>" );

		FamiliarTool tool = new FamiliarTool( debugOpponents );

		int [] weights = status.getWeights( false );
		printWeights( weights, false );

		CakeArenaManager.ArenaOpponent opponent = tool.bestOpponent( debugFamiliar.getID(), weights );

		printMatch( status, opponent, tool );

		int weight = tool.bestWeight();
		results.append( "Equipping to " + weight + " lbs.<br>" );
		status.chooseGear( weight, false );
	}
	*/


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
			setSelected( client == null || client.getLocalBooleanProperty( property ) );

			this.property = property;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	client.setLocalProperty( property, isSelected() );
		}
	}

	public static void main( String [] args )
	{	(new CreateFrameRunnable( FamiliarTrainingFrame.class )).run();
	}
}
