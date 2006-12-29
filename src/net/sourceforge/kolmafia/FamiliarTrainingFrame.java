/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import java.awt.Component;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;

public class FamiliarTrainingFrame extends KoLFrame
{
	private static final Pattern PRIZE_PATTERN = Pattern.compile( "You acquire an item: <b>(.*?)</b>" );

	private static final Pattern CAGELOST_PATTERN = Pattern.compile( "You enter (.*?) against (.*?) in an Ultimate Cage Match.<p>(.*?\\1.*?\\.<p>)\\1 struggles for" );
	private static final Pattern HUNTLOST_PATTERN = Pattern.compile( "You enter (.*?) against (.*?) in a Scavenger Hunt.<p>(.*?\\1.*?\\.<p>)\\1 finds" );
	private static final Pattern COURSELOST_PATTERN = Pattern.compile( "You enter (.*?) against (.*?) in an Obstacle Course race.<p>(.*?\\1.*?\\.<p>)\\1 makes it through the obstacle course" );
	private static final Pattern HIdELOST_PATTERN = Pattern.compile( "You enter (.*?) against (.*?) in a game of Hide and Seek.<p>(.*?\\1.*?\\.<p>)\\1 manages to stay hidden" );

	private static ChatBuffer results = new ChatBuffer( "Arena Tracker" );
	private static boolean stop = false;

	private FamiliarTrainingPanel training;
	private KoLCharacterAdapter weightListener;

	public static final int BASE = 1;
	public static final int BUFFED = 2;
	public static final int TURNS = 3;
	public static final int LEARN = 4;

	// Familiars
	private static final int DODECAPEDE = 38;

	// Familiar buffing skills and effects
	public static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0, true );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0, true );
	private static final AdventureResult GREEN_TONGUE = new AdventureResult( "Green Tongue", 0, true );
	private static final AdventureResult BLACK_TONGUE = new AdventureResult( "Black Tongue", 0, true );
	private static final AdventureResult HEAVY_PETTING = new AdventureResult( "Heavy Petting", 0, true );

	// Familiar buffing items
	private static final AdventureResult BUFFING_SPRAY = new AdventureResult( 1512, 1 );
	private static final AdventureResult PITH_HELMET = new AdventureResult( 1231, 1 );
	private static final AdventureResult LEAD_NECKLACE = new AdventureResult( 865, 1 );
	private static final AdventureResult RAT_HEAD_BALLOON = new AdventureResult( 1218, 1 );
	private static final AdventureResult PUMPKIN_BASKET = new AdventureResult( 1971, 1 );

	private static final AdventureResult GREEN_SNOWCONE = new AdventureResult( 1413, 1 );
	private static final AdventureResult BLACK_SNOWCONE = new AdventureResult( 1417, 1 );

	private static final int firstTinyPlastic = 969;
	private static final int lastTinyPlastic = 988;
	private static final int firstTinyPlasticCrimbo = 1377;
	private static final int lastTinyPlasticCrimbo = 1378;

	// Available skills which affect weight
	private static boolean sympathyAvailable;
	private static boolean leashAvailable;
	private static boolean empathyAvailable;
	private static boolean heavyPettingAvailable;

	// Active effects which affect weight
	private static int leashActive;
	private static int empathyActive;
	private static int greenTongueActive;
	private static int blackTongueActive;
	private static int heavyPettingActive;

	public FamiliarTrainingFrame()
	{
		super( "Familiar Trainer" );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		training = new FamiliarTrainingPanel();
		framePanel.add( training, "" );

		// Clear left over results from the buffer
		results.clearBuffer();
	}

	public void dispose()
	{
		stop = true;
		KoLCharacter.removeCharacterListener( weightListener );

		super.dispose();
	}

	public static final ChatBuffer getResults()
	{	return results;
	}

	private class FamiliarTrainingPanel extends JPanel
	{
		private FamiliarData familiar;
		private JComboBox familiars;
		private JLabel winCount;
		private JLabel prizeCounter;
		private JLabel totalWeight;

		private OpponentsPanel opponentsPanel;
		private ButtonPanel buttonPanel;
		private ResultsPanel resultsPanel;

		public FamiliarTrainingPanel()
		{
			super( new BorderLayout( 10, 10 ) );
			existingPanels.add( new WeakReference( this ) );

			JPanel container = new JPanel( new BorderLayout( 10, 10 ) );

			// Get current familiar
			familiar = KoLCharacter.getFamiliar();

			// Put familiar changer on top
			familiars = new ChangeComboBox( KoLCharacter.getFamiliarList() );
			familiars.setRenderer( FamiliarData.getRenderer() );
			container.add( familiars, BorderLayout.NORTH );

			// Put results in center
			resultsPanel = new ResultsPanel();
			container.add( resultsPanel, BorderLayout.CENTER );
			add( container, BorderLayout.CENTER );

			// Put opponents on left
			opponentsPanel = new OpponentsPanel();
			add( opponentsPanel, BorderLayout.WEST );

			// Put buttons on right
			JPanel buttonContainer = new JPanel( new BorderLayout() );

			buttonPanel = new ButtonPanel();
			buttonContainer.add( buttonPanel, BorderLayout.NORTH );

			// List of counters at bottom
			JPanel counterPanel = new JPanel();
			counterPanel.setLayout( new GridLayout( 3, 1 ) );

			// First the win counter
			winCount = new JLabel( "", JLabel.CENTER );
			counterPanel.add( winCount );

			// Next the prize counter
			prizeCounter = new JLabel( "", JLabel.CENTER );
			counterPanel.add( prizeCounter );

			// Finally the total familiar weight
			totalWeight = new JLabel( "", JLabel.CENTER );
			counterPanel.add( totalWeight );

			// Make a refresher for the counters
			weightListener = new KoLCharacterAdapter( new TotalWeightRefresher() );
			KoLCharacter.addCharacterListener( weightListener );

			// Show the counters
			buttonContainer.add( counterPanel, BorderLayout.SOUTH );

			add( buttonContainer, BorderLayout.EAST );
		}

		private class TotalWeightRefresher implements Runnable
		{
			public TotalWeightRefresher()
			{	this.run();
			}

			public void run()
			{
				// Arena wins
				int arenaWins = KoLCharacter.getArenaWins();
				winCount.setText( arenaWins + " arena wins" );

				// Wins to next prize
				int nextPrize = 10 - ( arenaWins % 10 );
				prizeCounter.setText( nextPrize + " wins to next prize" );

				// Terrarium weight
				int totalTerrariumWeight = 0;

				FamiliarData [] familiarArray = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
				KoLCharacter.getFamiliarList().toArray( familiarArray );

				for ( int i = 0; i < familiarArray.length; ++i )
					if ( familiarArray[i].getWeight() != 1 )
						totalTerrariumWeight += familiarArray[i].getWeight();

				totalWeight.setText( totalTerrariumWeight + " lb. terrarium" );
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( buttonPanel == null || familiars == null )
				return;

			super.setEnabled( isEnabled );
			buttonPanel.setEnabled( isEnabled );
			familiars.setEnabled( isEnabled );
		}

		private class OpponentsPanel extends JPanel
		{
			public OpponentsPanel()
			{
				// Get current opponents
				LockableListModel opponents = CakeArenaManager.getOpponentList();
				int opponentCount = opponents.size();

				setLayout( new GridLayout( opponentCount, 1, 0, 20 ) );

				for ( int i = 0; i < opponentCount; ++i )
					add( new OpponentLabel( (ArenaOpponent) opponents.get(i) ) );
			}

			private class OpponentLabel extends JLabel
			{
				public OpponentLabel( ArenaOpponent opponent )
				{
					super( "<html><center>" + opponent.getName() + "<br>" + "(" + opponent.getWeight() + " lbs)</center></html>",
						FamiliarsDatabase.getFamiliarImage( opponent.getRace() ), JLabel.CENTER );

					setVerticalTextPosition( JLabel.BOTTOM );
					setHorizontalTextPosition( JLabel.CENTER );
				}
			}
		}

		private class ButtonPanel extends JPanel
		{
			private JButton matchup, base, buffed, turns, stop, save, changer, learn, equip;
			// private JButton debug;

			public ButtonPanel()
			{
				JPanel containerPanel = new JPanel( new GridLayout( 11, 1, 5, 5 ) );

				matchup = new DisplayFrameButton( "View Matchup", "CakeArenaFrame" );
				containerPanel.add( matchup );

				containerPanel.add( new JLabel() );

				base = new JButton( "Train Base Weight" );
				base.addActionListener( new BaseListener() );
				containerPanel.add( base );

				buffed = new JButton( "Train Buffed Weight" );
				buffed.addActionListener( new BuffedListener() );
				containerPanel.add( buffed );

				turns = new JButton( "Train for Set Turns" );
				turns.addActionListener( new TurnsListener() );
				containerPanel.add( turns );

				containerPanel.add( new JLabel() );

				stop = new JButton( "Stop Training" );
				stop.addActionListener( new StopListener() );
				containerPanel.add( stop );

				save = new JButton( "Save Transcript" );
				save.addActionListener( new SaveListener() );
				containerPanel.add( save );

				containerPanel.add( new JLabel() );

				learn = new JButton( "Learn Familiar Strengths" );
				learn.addActionListener( new LearnListener() );
				containerPanel.add( learn );

				equip = new JButton( "Equip All Familiars" );
				equip.addActionListener( new EquipAllListener() );
				containerPanel.add( equip );

				// debug = new JButton( "Debug" );
				// debug.addActionListener( new DebugListener() );
				// this.add( debug );

				add( containerPanel );
			}

			public void setEnabled( boolean isEnabled )
			{
				if ( base == null || buffed == null || turns == null || save == null || changer == null )
					return;

				super.setEnabled( isEnabled );
				base.setEnabled( isEnabled );
				buffed.setEnabled( isEnabled );
				turns.setEnabled( isEnabled );
				save.setEnabled( isEnabled );
				changer.setEnabled( isEnabled );
			}

			private class BaseListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					// Prompt for goal
					int goal = getQuantity( "Train up to what base weight?", 20, 20 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( goal, BASE );
					KoLmafia.enableDisplay();
				}
			}

			private class BuffedListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					// Prompt for goal
					int goal = getQuantity( "Train up to what buffed weight?", 48, 20 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( goal, BUFFED );
					KoLmafia.enableDisplay();
				}
			}

			private class TurnsListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					// Prompt for goal
					int goal = getQuantity( "Train for how many turns?", Integer.MAX_VALUE, 1 );

					// Quit if canceled
					if ( goal == 0 )
						return;

					// Level the familiar
					levelFamiliar( goal, TURNS );
					KoLmafia.enableDisplay();
				}
			}

			private class StopListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	FamiliarTrainingFrame.stop = true;
				}
			}

			private class SaveListener implements ActionListener
			{
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
						// This should not happen.  Therefore, print
						// a stack trace for debug purposes.

						StaticEntity.printStackTrace( ex );
					}
				}
			}

			private class LearnListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					if ( familiar == FamiliarData.NO_FAMILIAR )
						return;

					// Prompt for trials
					int trials = getQuantity( "How many trials per event per rank?", 20, 3 );

					// Quit if canceled
					if ( trials == 0 )
						return;

					// Nag dialog
					int turns = trials * 12;
					if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null, "This will take up to " + turns + " adventures and cost up to " + COMMA_FORMAT.format( turns * 100 ) + " meat. Are you sure?", "Familiar strength learner nag screen", JOptionPane.YES_NO_OPTION ) )
						return;

					// Learn familiar parameters
					int [] skills = learnFamiliarParameters( trials );

					// Save familiar parameters
					if ( skills != null )
					{
						int [] original = FamiliarsDatabase.getFamiliarSkills( familiar.getId() );
						boolean changed = false;

						for ( int i = 0; i < original.length; ++i )
							if ( skills[i] != original[i] )
							{
								changed = true;
								break;
							}

						if ( changed && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null, "Save arena parameters for the " + familiar.getRace() + "?", "Save arena skills?", JOptionPane.YES_NO_OPTION ) )
							FamiliarsDatabase.setFamiliarSkills( familiar.getRace(), skills );

						KoLmafia.updateDisplay( CONTINUE_STATE, "Learned skills are " + ( changed ? "different from" : "the same as" ) + " those in familiar database." );
					}

					KoLmafia.enableDisplay();
				}
			}

			private class EquipAllListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{
					FamiliarData current = KoLCharacter.getFamiliar();

					FamiliarData [] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
					KoLCharacter.getFamiliarList().toArray( familiars );

					for ( int i = 0; i < familiars.length; ++i )
					{
						String itemName = FamiliarsDatabase.getFamiliarItem( familiars[i].getId() );

						if ( itemName != null && !familiars[i].getItem().equals( itemName ) )
						{
							AdventureResult item = new AdventureResult( itemName, 1, false );
							if ( KoLCharacter.hasItem( item ) )
							{
								KoLRequest request = new KoLRequest( "familiar.php?pwd&action=equip&whichfam=" + familiars[i].getId() + "&whichitem=" + item.getItemId() );

								RequestThread.postRequest( request );
								RequestThread.postRequest( new FamiliarRequest( familiars[i] ) );
								RequestThread.postRequest( new EquipmentRequest( item, KoLCharacter.FAMILIAR ) );
							}
						}
					}

					RequestThread.postRequest( new FamiliarRequest( current ) );
					KoLmafia.enableDisplay();
				}
			}

			/*
			private class DebugListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	debug();
				}
			}
			*/
		}

		private class ResultsPanel extends JPanel
		{
			RequestPane resultsDisplay;

			public ResultsPanel()
			{
				setLayout( new BorderLayout( 10, 10 ) );

				RequestPane resultsDisplay = new RequestPane();
				JScrollPane scroller = results.setChatDisplay( resultsDisplay );
				JComponentUtilities.setComponentSize( scroller, 400, 400 );

				add( scroller, BorderLayout.CENTER );
			}
		}

		private class ChangeComboBox extends JComboBox
		{
			private boolean isChanging = false;
			public ChangeComboBox( LockableListModel selector )
			{
				super( selector );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( !isShowing() || isChanging || !isEnabled() )
					return;

				if ( e.paramString().endsWith( "=" ) )
					return;

				executeChange();
			}

			public void firePopupMenuWillBecomeInvisible()
			{
				super.firePopupMenuWillBecomeInvisible();

				if ( !isShowing() || isChanging || !isEnabled() )
					return;

				executeChange();
			}

			public void executeChange()
			{
				FamiliarData selection = (FamiliarData) getSelectedItem();

				if ( selection == null || selection == familiar )
					return;

				isChanging = true;
				RequestThread.postRequest( new FamiliarRequest( selection ) );
				isChanging = false;

				KoLmafia.updateDisplay( "Familiar changed." );
				familiar = KoLCharacter.getFamiliar();
			}
		}
	}

	private static boolean levelFamiliar( int goal, int type )
	{	return levelFamiliar( goal, type, type == BUFFED, StaticEntity.getBooleanProperty( "debugFamiliarTraining" ) );
	}

	/**
	 * Utility method to level the current familiar by fighting the
	 * current arena opponents.
	 *
	 * @param	goal	Weight goal for the familiar
	 * @param	type	BASE, BUFF, or TURNS
	 * @param	buffs	true if should cast buffs during training
	 * @param	debug	true if we are debugging
	 */

	public static boolean levelFamiliar( int goal, int type, boolean buffs, boolean debug )
	{
		// Clear the output
		results.clearBuffer();

		// Permit training session to proceed
		stop = false;

		// Get current familiar
		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			statusMessage( ERROR_STATE, "No familiar selected to train." );
			return false;
		}

		if ( !familiar.trainable() )
		{
			statusMessage( ERROR_STATE, "Don't know how to train a " + familiar.getRace() + " yet." );
			return false;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus();

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
		LockableListModel opponents = CakeArenaManager.getOpponentList();

		// Print the opponents
		printOpponents( opponents );
		results.append( "<br>" );

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Let the battles begin!
		KoLmafia.updateDisplay( "Starting training session..." );

		// Iterate until we reach the goal
		while ( !goalMet( status, goal, type ) )
		{
			// If user canceled, bail now
			if ( stop || !KoLmafia.permitsContinue() )
			{
				statusMessage( ERROR_STATE, "Training session aborted." );
				return false;
			}

			// Make sure you have an adventure left
			if ( KoLCharacter.getAdventuresLeft() < 1 )
			{
				statusMessage( ERROR_STATE, "Training stopped: out of adventures." );
				return false;
			}

			// Make sure you have enough meat to pay for the contest
			if ( KoLCharacter.getAvailableMeat() < 100 )
			{
				statusMessage( ERROR_STATE, "Training stopped: out of meat." );
				return false;
			}

			// Choose possible weights
			int [] weights = status.getWeights( buffs );

			if ( debug )
			{
				statusMessage( CONTINUE_STATE, "Assuming buffs: " + buffs );
				printWeights( weights, buffs );
			}

			// Choose next opponent
			ArenaOpponent opponent = tool.bestOpponent( familiar.getId(), weights );

			if ( opponent == null )
			{
				statusMessage( ERROR_STATE, "Couldn't choose a suitable opponent." );
				return false;
			}

			// Change into appropriate gear
			status.changeGear( tool.bestWeight(), buffs );
			if ( !KoLmafia.permitsContinue() )
			{
				if ( buffs )
				{
					results.append( "Trying again without considering buffs...<br>" );
					buffs = false;
					continue;
				}

				statusMessage( ERROR_STATE, "Training stopped: internal error." );
				return false;
			}

			if ( debug )
				break;

			// Enter the contest
			fightMatch( status, tool, opponent );
		}

		statusMessage( CONTINUE_STATE, "Training session completed." );
		return buffs ? buffFamiliar( goal ) : true;
	}

	/**
	 * Utility method to derive the arena parameters of the current
	 * familiar
	 *
	 * @param	trials	How many trials per event
	 */

	private static int [] learnFamiliarParameters( int trials )
	{
		// Clear the output
		results.clearBuffer();

		// Permit training session to proceed
		stop = false;

		// Get current familiar
		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			statusMessage( ERROR_STATE, "No familiar selected to train." );
			return null;
		}

		int events = 12 * trials;
		// Make sure you have enough adventures left
		if ( KoLCharacter.getAdventuresLeft() < events )
		{
			statusMessage( ERROR_STATE, "You need to have at least " + events + " adventures available." );
			return null;
		}

		// Make sure you have enough meat to pay for the contests
		if ( KoLCharacter.getAvailableMeat() < ( 100 * events ) )
		{
			statusMessage( ERROR_STATE, "You need to have at least " + COMMA_FORMAT.format( 100 * events ) + " meat available." );
			return null;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus();

		// Identify the familiar we are training
		printFamiliar( status, 0, LEARN );
		results.append( "<br>" );

		// Print available buffs and items and current buffs
		results.append( status.printCurrentBuffs() );
		results.append( status.printAvailableBuffs() );
		results.append( status.printCurrentEquipment() );
		results.append( status.printAvailableEquipment() );
		results.append( "<br>" );

		// Get opponent list
		LockableListModel opponents = CakeArenaManager.getOpponentList();

		// Print the opponents
		printOpponents( opponents );
		results.append( "<br>" );

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Let the battles begin!
		KoLmafia.updateDisplay( "Starting training session..." );

		// XP earned indexed by [event][rank]
		int [][] xp = new int[4][3];

		// Array of skills to test with
		int test [] = new int[4];

		// Array of contest suckage
		boolean suckage [] = new boolean[4];

		// Iterate for the specified number of trials
		for ( int trial = 0; trial < trials; ++trial )
		{
			// Iterate through the contests
			for ( int contest = 0; contest < 4; ++contest )
			{
				// Initialize test parameters
				test[0] = test[1] = test[2] = test[3] = 0;

				// Iterate through the ranks
				for ( int rank = 0; rank < 3; ++rank )
				{
					// Skip contests in which the familiar
					// sucks
					if ( suckage[contest] )
						continue;

					// If user canceled, bail now
					if ( stop || !KoLmafia.permitsContinue() )
					{
						statusMessage( ERROR_STATE, "Training session aborted." );
						return null;
					}

					// Initialize test parameters
					test[contest] = rank + 1;

					statusMessage( CONTINUE_STATE, CakeArenaManager.getEvent( contest + 1) + " rank " + ( rank + 1 ) + ": trial " + ( trial + 1 ) );

					// Choose possible weights
					int [] weights = status.getWeights( false );

					// Choose next opponent
					ArenaOpponent opponent = tool.bestOpponent( test, weights );

					if ( opponent == null )
					{
						statusMessage( ERROR_STATE, "Couldn't choose a suitable opponent." );
						return null;
					}

					int match = tool.bestMatch();
					if ( match != contest + 1 )
					{
						// Informative message only. Do not stop session.
						statusMessage( ERROR_STATE, "Internal error: Familiar Tool selected " + CakeArenaManager.getEvent( match ) + " rather than " + CakeArenaManager.getEvent( contest + 1 ) );
						// Use contest, even if with bad weight
						match = contest + 1;
					}

					// Change into appropriate gear
					status.changeGear( tool.bestWeight(), false );
					if ( !KoLmafia.permitsContinue() )
					{
						statusMessage( ERROR_STATE, "Training stopped: internal error." );
						return null;
					}

					// Enter the contest
					int trialXP = fightMatch( status, tool, opponent, match );

					if ( trialXP < 0 )
						suckage[contest] = true;
					else
						xp[contest][rank] += trialXP;
				}
			}
		}

		// Original skill rankings
		int [] original = FamiliarsDatabase.getFamiliarSkills( familiar.getId() );

		// Derived skill rankings
		int skills [] = new int[4];

		StringBuffer text = new StringBuffer();

		// Open the table
		text.append( "<table>" );

		// Table header
		text.append( "<tr>" );
		text.append( "<th>Contest</th>" );
		text.append( "<th>XP[1]</th>" );
		text.append( "<th>XP[2]</th>" );
		text.append( "<th>XP[3]</th>" );
		text.append( "<th>Original Rank</th>" );
		text.append( "<th>Derived Rank</th>" );
		text.append( "</tr>" );

		for ( int contest = 0; contest < 4; ++contest )
		{
			text.append( "<tr>" );
			text.append( "<td>" + CakeArenaManager.getEvent( contest + 1) + "</td>" );

			int bestXP = 0;
			int bestRank = 0;
			for ( int rank = 0; rank < 3; ++rank )
			{
				int rankXP = suckage[contest] ? 0 : xp[contest][rank];
				text.append( "<td align=center>" + rankXP + "</td>" );
				if ( rankXP > bestXP )
				{
					bestXP = rankXP;
					bestRank = rank + 1;
				}
			}
			text.append( "<td align=center>" + original[contest] + "</td>" );
			text.append( "<td align=center>" + bestRank + "</td>" );
			skills[contest] = bestRank;
			text.append( "</tr>" );
		}

		// Close the table
		text.append( "</table>" );

		results.append( "<br>Final results for " + familiar.getRace() + " with " + trials + " trials using " + status.turnsUsed() + " turns:<br><br>" );
		results.append( text.toString() );
		return skills;
	}

	/**
	 * Utility method to buff the current familiar to the specified weight
	 * or higher.
	 *
	 * @param	weight	Weight goal for the familiar
	 */

	public static boolean buffFamiliar( int weight )
	{
		// Get current familiar. If none, punt.

		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a familiar equipped." );
			return false;
		}

		// If the familiar is already heavy enough, nothing to do
		if ( familiar.getModifiedWeight() >= weight )
			return true;

		FamiliarStatus status = new FamiliarStatus();
		boolean needBuffs = false;
		int goal = 0;

		while ( goal < weight )
		{
			// Calculate possible weights
			int [] weights = status.getWeights( needBuffs );
			Arrays.sort( weights );

			// Find the first one which meets or exceeds goal
			for ( int i = 0; goal < weight && i < weights.length; ++i )
				goal = Math.max( goal, weights[i] );

			// If we found one, use it
			if ( goal >= weight )
			{
				// Change gear and buff up
				status.changeGear( goal, needBuffs );
				if ( familiar.getModifiedWeight() >= weight )
					return true;
			}

			// If we've already tried using buffs, give up now
			if ( needBuffs )
				break;

			// Try again with buffs
			needBuffs = true;
		}

		KoLmafia.updateDisplay( ERROR_STATE, "Can't buff and equip familiar to reach " + weight + " lbs." );
		return false;
	}

	private static void statusMessage( int state, String message )
	{
		if ( state == ERROR_STATE || message.endsWith( "lost." ) )
			results.append( "<font color=red>" + message + "</font><br>" );
		else if ( message.indexOf( "experience" ) != -1 )
			results.append( "<font color=green>" + message + "</font><br>" );
		else if ( message.indexOf( "prize" ) != -1 )
			results.append( "<font color=blue>" + message + "</font><br>" );
		else
			results.append( message + "<br>" );

		KoLmafia.updateDisplay( state, message );
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
		else if ( type == LEARN )
			hope = " to learn arena strengths";

		results.append( "Training " + name + " the " + weight + " lb. " + race + hope +	 ".<br>" );
	}

	private static void printOpponents( LockableListModel opponents )
	{
		results.append( "Opponents:<br>" );
		int opponentCount = opponents.size();
		for ( int i = 0; i < opponentCount; ++i )
		{
			ArenaOpponent opponent = (ArenaOpponent) opponents.get( i );
			String name = opponent.getName();
			String race = opponent.getRace();
			int weight = opponent.getWeight();
			results.append( name + " the " + weight + " lb. " + race + "<br>");
		}
	}

	private static boolean goalMet( FamiliarStatus status, int goal, int type )
	{
		switch ( type )
		{
		case BASE:
			return ( status.baseWeight() >= goal );

		case BUFFED:
		{
			int maxBuffedWeight = status.maxBuffedWeight( true );
			boolean goalReached = maxBuffedWeight >= goal;

			if ( goal != 20 )
				return goalReached;

			boolean hasTongue = false;
			for ( int i = 0; i < activeEffects.size(); ++i )
				hasTongue |= ((AdventureResult)activeEffects.get(i)).getName().toLowerCase().endsWith( "tongue" );

			if ( !hasTongue )
			{
				if ( maxBuffedWeight >= 15 && greenTongueActive == 0 && blackTongueActive == 0 )
				{
					if ( KoLCharacter.hasItem( GREEN_SNOWCONE ) )
					{
						DEFAULT_SHELL.executeLine( "use 1 green snowcone" );
						greenTongueActive = 20;
						return true;
					}

					if ( KoLCharacter.hasItem( BLACK_SNOWCONE ) && blackTongueActive == 0 )
					{
						DEFAULT_SHELL.executeLine( "use 1 black snowcone" );
						blackTongueActive = 20;
						return true;
					}
				}
			}

			if ( goalReached || !KoLCharacter.canInteract() )
				return goalReached;

			// If the player is currently out of Ronin, then they have
			// the option of automatically purchasing their familiar item,
			// pet-buffing spray, empathy and a green snowcone, which will
			// boost you straight to 20 pounds without any skills.

			int poundsNeeded = goal - maxBuffedWeight;

			// If you're out of Ronin, pet-buffing spray is still
			// available in the mall.  Check this option first.

			if ( !heavyPettingAvailable && heavyPettingActive == 0 )
			{
				poundsNeeded -= 5;
				heavyPettingAvailable = true;
				DEFAULT_SHELL.executeBuyCommand( "1 Knob Goblin pet-buffing spray" );
			}

			if ( poundsNeeded <= 0 )
				return true;

			// First, check their current familiar's
			// item. If it affects weight (positively) and
			// they don't have one, buy one in the mall

			if ( status.familiarItem != null && status.specItem != status.familiarItem && status.familiarItemWeight > 0 )
			{
				poundsNeeded -= status.familiarItemWeight;
				String familiarItem = status.familiarItem.getName();

				DEFAULT_SHELL.executeBuyCommand( "1 " + familiarItem );
				status.updateStatus();
			}

			if ( poundsNeeded <= 0 )
				return true;

			// Acquire tiny plastic items from the mall.  Randomly
			// choose one of the items using a random number generator.

			if ( poundsNeeded <= 3 - status.tpCount )
			{
				String plasticItem = TradeableItemDatabase.getItemName(
					firstTinyPlastic + RNG.nextInt( lastTinyPlastic - firstTinyPlastic ) );

				DEFAULT_SHELL.executeBuyCommand( poundsNeeded + " " + plasticItem );
				status.updateStatus();
				return true;
			}

			// Otherwise, if it gets this far, you definitely need
			// a snowcone; any other circumstance and you would have
			// had enough to get by.

			if ( !hasTongue && greenTongueActive == 0 )
			{
				DEFAULT_SHELL.executeBuyCommand( "1 green snowcone" );
				status.updateStatus();
				poundsNeeded -= 5;
			}

			// That's all that can be done.  Return whether or not
			// your goal is achievable.

			return poundsNeeded <= 0;
		}

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

	private static void printMatch( FamiliarStatus status, ArenaOpponent opponent, FamiliarTool tool, int match )
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
		text.append( " in the " + CakeArenaManager.getEvent( match ) );
		text.append( " event.<br>" );

		results.append( text.toString() );

		KoLmafia.updateDisplay( "Round " + round + ": " + familiar.getName() + " vs. " + opponent.getName() + "..." );
	}

	private static int fightMatch( FamiliarStatus status, FamiliarTool tool, ArenaOpponent opponent )
	{	return fightMatch( status, tool, opponent, tool.bestMatch() );
	}

	private static int fightMatch( FamiliarStatus status, FamiliarTool tool, ArenaOpponent opponent, int match )
	{
		// If user aborted, bail now
		if ( KoLmafia.refusesContinue() )
			return 0;

		// Tell the user about the match
		printMatch( status, opponent, tool, match );

		// Run the match
		KoLRequest request = new CakeArenaRequest( opponent.getId(), match );
		RequestThread.postRequest( request );

		// Pass the response text to the FamiliarStatus to
		// add familiar items and deduct a turn.
		int xp = earnedXP( request.responseText );
		status.processMatchResult( request.responseText, xp );

		// Return the amount of XP the familiar earned
		return badContest( request.responseText,match) ? -1 : xp;
	}

	private static int earnedXP( String response )
	{
		Matcher matcher = CakeArenaManager.WIN_PATTERN.matcher( response );
		return matcher.find() ? Integer.valueOf( matcher.group(1) ).intValue() : 0;
	}

	private static boolean badContest( String response, int match )
	{
		// Look for special "this familiar sucks" message. Note the
		// familiar can still win, even if such a message is present; a
		// match in which both familiars suck is given to either
		// contestant at random.

		Matcher matcher;
		switch ( match )
		{
		case 1: // "You enter Tort against Dirty Pair in an Ultimate
			// Cage Match.<p>Tort is a lover, not a
			// fighter.<p>Well, not really -- potatoes just suck at
			// this event.<p>Tort struggles for 3 rounds, but is
			// eventually knocked out.<p>Tort lost."
			matcher = CAGELOST_PATTERN.matcher( response );
			break;
		case 2: // "You enter Trot against Vine Vidi Vici in a
			// Scavenger Hunt.<p>Trot keeps getting distracted from
			// the hunt and randomly ramming into things.<p>Trot
			// finds 12 items from the list.<p>Vine Vidi Vici finds
			// 17 items.<p>Trot lost."
			matcher = HUNTLOST_PATTERN.matcher( response );
			break;
		case 3: // "You enter Gort against Pork Soda in an Obstacle
			// Course race.<p>Gort is too short to get over most of
			// the obstacles.<p>Gort makes it through the obstacle
			// course in 49 seconds.<p>Pork Soda takes 29
			// seconds. <p>Gort lost."
			matcher = COURSELOST_PATTERN.matcher( response );
			break;
		case 4: // "You enter Tot against Pork Soda in a game of Hide
			// and Seek.<p>Tot buzzes incessantly, making it very
			// difficult to remain concealed.<p>Tot manages to stay
			// hidden for 28 seconds.<p>Pork Soda stays hidden for
			// 53 seconds.<p>Tot lost."
			matcher = HIdELOST_PATTERN.matcher( response );
			break;
		default:
			return false;
		}

		return  matcher.find();
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
		// The familiar we are tracking
		FamiliarData familiar;

		// How many turns we have trained it
		int turns;

		// Details about its special familiar item
		AdventureResult familiarItem;
		int familiarItemWeight;

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

		AdventureResult pumpkinBasket;
		FamiliarData pumpkinBasketOwner;

		int tpCount;
		AdventureResult [] tp = new AdventureResult [3];

		// Weights
		TreeSet weights;

		// Gear sets
		ArrayList gearSets;

		public FamiliarStatus()
		{
			// Find out which familiar we are working with
			familiar = KoLCharacter.getFamiliar();

			// Get details about the special item it can wear
			String name = FamiliarsDatabase.getFamiliarItem( familiar.getId() );
			familiarItem = new AdventureResult( name, 1, false );
			familiarItemWeight = FamiliarData.itemWeightModifier( familiarItem.getItemId() );

			// No turns have been used yet
			turns = 0;

			// Initialize set of weights
			weights = new TreeSet();

			// Initialize the list of GearSets
			gearSets = new ArrayList();

			// Check skills and equipment
			updateStatus();
		}

		public void updateStatus()
		{
			// Check available skills
			checkSkills();

			// Check current equipment
			checkCurrentEquipment();

			// Check available equipment
			checkAvailableEquipment( inventory );
                }

		/*
		// Debug initializer
		public FamiliarStatus( KoLmafia StaticEntity.getClient(),
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
			// Savefor later use
			this.StaticEntity.getClient() = StaticEntity.getClient();

			// Find out which familiar we are working with
			this.familiar = familiar;

			// Get details about the special item it can wear
			String name = FamiliarsDatabase.getFamiliarItem( familiar.getId() );
			familiarItem = new AdventureResult( name, 1, false );
			familiarItemWeight = FamiliarData.itemWeightModifier( familiarItem.getItemId() );

			// Check available skills
			this.sympathyAvailable = sympathyAvailable;
			this.leashAvailable = leashAvailable;
			this.empathyAvailable = empathyAvailable;
			this.leashActive = leashActive;
			this.empathyActive = empathyActive;
			this.greenTongueActive = 0;
			this.blackTongueActive = 0;
			this.heavyPettingActive = 0;

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
			heavyPettingAvailable = inventory.contains( BUFFING_SPRAY ) || NPCStoreDatabase.contains( "Knob Goblin pet-buffing spray" );

			// Look at effects to decide which ones are active;
			LockableListModel active = activeEffects;
			empathyActive = EMPATHY.getCount( active );
			leashActive = LEASH.getCount( active );
			greenTongueActive = GREEN_TONGUE.getCount( active );
			blackTongueActive = BLACK_TONGUE.getCount( active );
			heavyPettingActive = HEAVY_PETTING.getCount( active );
		}

		private void checkCurrentEquipment()
		{
			checkCurrentEquipment( KoLCharacter.getEquipment( KoLCharacter.HAT ),
				KoLCharacter.getEquipment( KoLCharacter.FAMILIAR ),
				KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ),
				KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ),
				KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );
		}

		private void checkCurrentEquipment( AdventureResult hat, AdventureResult item,
			AdventureResult acc1, AdventureResult acc2, AdventureResult acc3 )
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
			pumpkinBasket = null;
			pumpkinBasketOwner = null;

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
				else if ( name.equals( PUMPKIN_BASKET.getName() ) )
				{
					this.item = pumpkinBasket = PUMPKIN_BASKET;
					pumpkinBasketOwner = familiar;
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
				int id = ar.getItemId();
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
			if ( familiarItem != item  &&
			     familiarItemWeight != 0 &&
			     familiarItem.getCount( inventory ) > 0 )
			{
				specItem = familiarItem;
				specWeight = familiarItemWeight;
			}

			// If current familiar is not wearing a pumpkin basket,
			// search inventory
			if ( pumpkinBasket == null && !KoLCharacter.isHardcore() &&
			     PUMPKIN_BASKET.getCount( inventory ) > 0 )
			{
				pumpkinBasket = PUMPKIN_BASKET;
				pumpkinBasketOwner = null;
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
			if ( leadNecklace == null || ratHeadBalloon == null || pumpkinBasket == null )
			{
				// Find first familiar with item
				LockableListModel familiars = KoLCharacter.getFamiliarList();
				for ( int i = 0; i < familiars.size(); ++i )
				{
					FamiliarData familiar = (FamiliarData)familiars.get(i);
					AdventureResult item = familiar.getItem();
					if ( item == null )
						continue;

					if ( item.equals( LEAD_NECKLACE ) )
					{
						// We found a lead necklace
						if ( leadNecklace == null )
						{
							leadNecklace = LEAD_NECKLACE;
							leadNecklaceOwner = familiar;
						}
					}

					if ( item.equals( RAT_HEAD_BALLOON ) )
					{
						// We found a balloon
						if ( ratHeadBalloon == null )
						{
							ratHeadBalloon = RAT_HEAD_BALLOON;
							ratHeadBalloonOwner = familiar;
						}
					}

					if ( item.equals( PUMPKIN_BASKET ) )
					{
						// We found a plastic pumpkin basket
						if ( pumpkinBasket == null )
						{
							pumpkinBasket = PUMPKIN_BASKET;
							pumpkinBasketOwner = familiar;
						}
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

			// Sympathy adds 5 lbs. except to a dodecapede, for
			// which it subtracts 5 lbs.
			if ( sympathyAvailable )
				weight += ( familiar.getId() == DODECAPEDE ) ? -5 : 5;
			if ( empathyActive > 0 )
				weight += 5;
			if ( leashActive > 0 )
				weight += 5;

			// One snowcone effect at a time
			if ( greenTongueActive > 0 || blackTongueActive > 0 )
				weight += 5;

			if ( heavyPettingActive > 0 )
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
				int overallModifier = 0;

				if ( empathyAvailable && empathyActive == 0 )
					overallModifier += 5;

				if ( leashAvailable && leashActive == 0 )
					overallModifier += 5;

				if ( heavyPettingAvailable && heavyPettingActive == 0 )
					overallModifier += 5;

				for ( int i = 5; i <= overallModifier; i += 5 )
					getItemWeights( weight + i );
			}

			// Calculate item weights with no additional buffs
			getItemWeights( weight );
		}

		private void getItemWeights( int weight )
		{
			// If familiar specific item adds weight, calculate
			if ( specWeight != 0 )
				getAccessoryWeights( weight + specWeight );

			// If we have a pumpkin basket, use it
			if ( pumpkinBasket != null )
				getAccessoryWeights( weight + 5 );

			// If we have a lead necklace, use it
			if ( leadNecklace != null )
				getAccessoryWeights( weight + 3 );

			// If we have a rat head balloon, use it
			if ( ratHeadBalloon != null )
				getAccessoryWeights( weight - 3 );

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
				weights.add( new Integer( Math.max( weight + 5, 1 ) ) );

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
			if ( next == null || weight < next.weight() )
			{
				statusMessage( ERROR_STATE, "Could not select gear set to achieve " + weight + " lbs." );

				if ( next == null )
					results.append( "No gear set found.<br>" );
				else
					results.append( "Selected gear set provides " + next.weight() + " lbs.<br>" );

				return;
			}

			// Change into the new GearSet
			changeGear( current, next );
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
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, slot ) );
				setItem( slot, null );
			}

			if ( next == pumpkinBasket && pumpkinBasketOwner != null && pumpkinBasketOwner != familiar )
			{
				results.append( "Stealing pumpkin basket from " + pumpkinBasketOwner.getName() + " the " + pumpkinBasketOwner.getRace() + "<br>" );
				RequestThread.postRequest( new EquipmentRequest( PUMPKIN_BASKET, KoLCharacter.FAMILIAR ) );
				pumpkinBasketOwner = familiar;
			}

			// Steal a lead necklace, if needed
			if ( next == leadNecklace && leadNecklaceOwner != null && leadNecklaceOwner != familiar )
			{
				results.append( "Stealing lead necklace from " + leadNecklaceOwner.getName() + " the " + leadNecklaceOwner.getRace() + "<br>" );
				RequestThread.postRequest( new EquipmentRequest( LEAD_NECKLACE, KoLCharacter.FAMILIAR ) );
				leadNecklaceOwner = familiar;
			}

			// Steal a rat head balloon necklace, if needed
			if ( next == ratHeadBalloon && ratHeadBalloonOwner != null && ratHeadBalloonOwner != familiar )
			{
				results.append( "Stealing rat head balloon from " + ratHeadBalloonOwner.getName() + " the " + ratHeadBalloonOwner.getRace() + "<br>" );
				RequestThread.postRequest( new EquipmentRequest( RAT_HEAD_BALLOON, KoLCharacter.FAMILIAR ) );
				ratHeadBalloonOwner = familiar;
			}

			// Finally, equip the new item
			if ( next != null )
			{
				results.append( "Putting on " + next.getName() + "...<br>" );
				RequestThread.postRequest( new EquipmentRequest( next, slot ) );
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

		private void castNeededBuffs( GearSet current, GearSet next )
		{
			if ( next.leash && !current.leash )
			{
				// Cast Leash. Bail if can't.

				DEFAULT_SHELL.executeLine( "cast Leash of Linguini" );
				if ( !UseSkillRequest.lastUpdate.equals( "" ) )
					return;

				// Remember it
				leashActive += 10;
			}

			if ( next.empathy && !current.empathy )
			{
				// Cast Empathy. Bail if can't.
				DEFAULT_SHELL.executeLine( "cast Empathy of the Newt" );
				if ( !UseSkillRequest.lastUpdate.equals( "" ) )
					return;

				// Remember it
				empathyActive += 10;
			}

			if ( next.spray && !current.spray )
			{
				DEFAULT_SHELL.executeLine( "use 1 Knob Goblin pet-buffing spray" );
				heavyPettingActive += 10;
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

			// Start without buffs
			getBuffGearSets( weight, false );

			// Iterate over all the GearSets and choose the first
			// one which is closest to the current GearSet
			if ( gearSets.isEmpty() && buffs )
				getBuffGearSets( weight, buffs );

			Collections.sort( gearSets );
			return gearSets.isEmpty() ? null : (GearSet) gearSets.get(0);
		}

		private void getBuffGearSets( int weight, boolean buffs )
		{
			// Handle base case of everything available right
			// now with no additional casting.

			getHatGearSets( weight, leashActive > 0, empathyActive > 0, heavyPettingActive > 0 );

			// Handle situations where you are allowed to cast
			// buffs on yourself during training.

			if ( buffs )
			{
				boolean addLeash = leashAvailable && leashActive == 0;
				boolean addEmpathy = empathyAvailable && empathyActive == 0;
				boolean addSpray = heavyPettingAvailable && heavyPettingActive == 0;

				if ( addLeash )
					getHatGearSets( weight, addLeash, empathyActive > 0, heavyPettingActive > 0 );
				if ( addEmpathy )
					getHatGearSets( weight, leashActive > 0, addEmpathy, heavyPettingActive > 0 );
				if ( addSpray )
					getHatGearSets( weight, leashActive > 0, empathyActive > 0, addSpray );

				if ( addLeash && addEmpathy )
					getHatGearSets( weight, addLeash, addEmpathy, heavyPettingActive > 0 );
				if ( addEmpathy && addSpray )
					getHatGearSets( weight, leashActive > 0, addEmpathy, addSpray );
				if ( addLeash && addSpray )
					getHatGearSets( weight, addLeash, empathyActive > 0, addSpray );

				if ( addLeash && addEmpathy && addSpray )
					getHatGearSets( weight, addLeash, addEmpathy, addSpray );
			}
		}

		private void getHatGearSets( int weight, boolean leash, boolean empathy, boolean spray )
		{
			if ( pithHelmet != null )
				getItemGearSets( weight, pithHelmet, leash, empathy, spray );

			getItemGearSets( weight, null, leash, empathy, spray );
		}

		private void getItemGearSets( int weight, AdventureResult hat, boolean leash, boolean empathy, boolean spray )
		{
			if ( specItem != null )
				getAccessoryGearSets( weight, specItem, hat, leash, empathy, spray );
			if ( pumpkinBasket != null )
				getAccessoryGearSets( weight, pumpkinBasket, hat, leash, empathy, spray );
			if ( leadNecklace != null )
				getAccessoryGearSets( weight, leadNecklace, hat, leash, empathy, spray );
			if ( ratHeadBalloon != null )
				getAccessoryGearSets( weight, ratHeadBalloon, hat, leash, empathy, spray );
			getAccessoryGearSets( weight, null, hat, leash, empathy, spray );
		}

		private void getAccessoryGearSets( int weight, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy, boolean spray )
		{
			// No matter how many Tiny Plastic Objects we have, a
			// configuration with none equipped is legal
			addGearSet( weight, null, null, null, item, hat, leash, empathy, spray );
			if ( tpCount == 0 )
				return;

			// If we have at least one and it started out equipped,
			// then it might be in any of the three accessory
			// slots.
			addGearSet( weight, tp[0], null, null, item, hat, leash, empathy, spray );
			addGearSet( weight, null, tp[0], null, item, hat, leash, empathy, spray );
			addGearSet( weight, null, null, tp[0], item, hat, leash, empathy, spray );
			if ( tpCount == 1)
				return;

			// If we have at least two and they were both equipped
			// when we came in, they'll be in one of the following
			// patterns.

			addGearSet( weight, tp[0], tp[1], null, item, hat, leash, empathy, spray );
			addGearSet( weight, tp[0], null, tp[1], item, hat, leash, empathy, spray );
			addGearSet( weight, null, tp[0], tp[1], item, hat, leash, empathy, spray );

			// If one of the two was in the inventory, the first
			// could have been in any of the three accessory
			// slots. Add a pattern for where it was in the third
			// slot.

			addGearSet( weight, tp[1], null, tp[0], item, hat, leash, empathy, spray );

			if ( tpCount == 2 )
				return;

			// If we have three and they were all equipped when we
			// came in, they'll be in the following pattern
			addGearSet( weight, tp[0], tp[1], tp[2], item, hat, leash, empathy, spray );

			// If two of them were equipped and the third was in
			// the inventory, the following patterns are legal
			addGearSet( weight, tp[0], tp[2], tp[1], item, hat, leash, empathy, spray );
			addGearSet( weight, tp[2], tp[0], tp[1], item, hat, leash, empathy, spray );

			// If only one was equipped, based on which
			// two-accessory patterns are legal, the following
			// three-accessory patterns are also legal
			addGearSet( weight, tp[1], tp[2], tp[0], item, hat, leash, empathy, spray );
		}

		private void addGearSet( int weight, AdventureResult acc1, AdventureResult acc2, AdventureResult acc3, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy, boolean spray )
		{
			if ( weight == gearSetWeight( acc1, acc2, acc3, item, hat, leash, empathy, spray ) )
				gearSets.add( new GearSet( hat, item, acc1, acc2, acc3, leash, empathy, spray ) );
		}

		private int gearSetWeight( AdventureResult acc1, AdventureResult acc2, AdventureResult acc3, AdventureResult item,  AdventureResult hat, boolean leash, boolean empathy, boolean spray )
		{
			int weight = familiar.getWeight();

			if ( sympathyAvailable )
				weight += ( familiar.getId() == DODECAPEDE ) ? -5 : 5;

			// One snowcone effect at a time
			if ( greenTongueActive > 0 || blackTongueActive > 0 )
				weight += 5;

			if ( hat == PITH_HELMET )
				weight += 5;

			if ( item == specItem )
				weight += specWeight;
			else if ( item == PUMPKIN_BASKET )
				weight += 5;
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
			if ( spray )
				weight += 5;

			return Math.max( weight, 1 );
		}

		/**************************************************************/

		public void processMatchResult( String response, int xp )
		{
			// If the contest did not take place, bail now
			if ( response.indexOf( "You enter" ) == -1 )
				return;

			// Find and report how much experience was gained
			String message;
			if ( xp > 0 )
				message = familiar.getName() + " gains " + xp + " experience" + ( response.indexOf( "gains a pound" ) != -1 ? " and a pound." : "." );

			else
				message = familiar.getName() + " lost.";

			statusMessage( CONTINUE_STATE, message );

			// If a prize was won, report it
			Matcher matcher = PRIZE_PATTERN.matcher( response );
			if ( matcher.find() )
			{
				String prize = matcher.group(1);
				statusMessage( CONTINUE_STATE, "You win a prize: " + prize + "." );
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
			if ( heavyPettingActive > 0 )
				heavyPettingActive--;
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

		public int maxBuffedWeight( boolean buffs )
		{
			// Start with current base weight of familiar
			int weight = familiar.getWeight();

			// Add possible skills
			if ( sympathyAvailable )
				weight += ( familiar.getId() == DODECAPEDE ) ? -5 : 5;

			if ( buffs )
			{
				if ( leashAvailable || leashActive > 0 )
					weight += 5;
				if ( empathyAvailable || empathyActive > 0 )
					weight += 5;
				if ( heavyPettingAvailable || heavyPettingActive > 0 )
					weight += 5;
			}

			// Add available familiar items
			if ( pumpkinBasket != null )
				weight += 5;
			else if ( specWeight > 3 )
				weight += specWeight;
			else if ( leadNecklace != null )
				weight += 3;

			// Add available tiny plastic items
			weight += tpCount;

			// Add pith helmet
			if ( pithHelmet != null )
				weight += 5;

			return Math.max( weight, 1 );
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
				text.append( " Sympathy (" + ( (familiar.getId() == DODECAPEDE) ? "-" : "+" ) + "5 permanent)" );
			if ( empathyActive > 0 )
				text.append( " Empathy (+5 for " + empathyActive + " turns)" );
			if ( leashActive > 0 )
				text.append( " Leash (+5 for " + leashActive + " turns)" );
			if ( greenTongueActive > 0 )
				text.append( " Green Tongue (+5 for " + greenTongueActive + " turns)" );
			if ( blackTongueActive > 0 )
				text.append( " Black Tongue (+5 for " + blackTongueActive + " turns)" );
			if ( heavyPettingActive > 0 )
				text.append( " Heavy Petting (+5 for " + heavyPettingActive + " turns)" );
			if ( !sympathyAvailable && empathyActive == 0 && leashActive == 0 && greenTongueActive == 0 && blackTongueActive == 0 && heavyPettingActive == 0 )
				text.append( " None" );
			text.append( "<br>" );

			return text.toString();
		}

		public String printCurrentEquipment()
		{
			StringBuffer text = new StringBuffer();

			text.append( "Current equipment:" );
			if ( hat == PITH_HELMET )
				text.append( " plexiglass pith helmet (+5)" );
			if ( item == PUMPKIN_BASKET )
				text.append( " plastic pumpkin basket (+5)" );
			else if ( item == LEAD_NECKLACE )
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
			if ( pumpkinBasket != null)
				text.append( " plastic pumpkin bucket (+5)" );
			if ( leadNecklace != null)
				text.append( " lead necklace (+3)" );
			if ( ratHeadBalloon != null)
				text.append( " rat head balloon (-3)" );
			for ( int i = 0; i < tpCount; ++i )
				text.append( " " + tp[i].getName() + " (+1)" );
			text.append( "<br>" );

			return text.toString();
		}

		private class GearSet implements Comparable
		{
			public AdventureResult hat;
			public AdventureResult item;
			public AdventureResult acc1;
			public AdventureResult acc2;
			public AdventureResult acc3;

			public boolean leash;
			public boolean empathy;
			public boolean spray;

			public GearSet( AdventureResult hat, AdventureResult item,
				AdventureResult acc1, AdventureResult acc2, AdventureResult acc3,
				boolean leash, boolean empathy, boolean spray )
			{
				this.hat = hat;
				this.item = item;
				this.acc1 = acc1;
				this.acc2 = acc2;
				this.acc3 = acc3;
				this.leash = leash;
				this.empathy = empathy;
				this.spray = spray;
			}

			public GearSet()
			{
				this( FamiliarStatus.this.hat, FamiliarStatus.this.item, acc[0], acc[1], acc[2],
					leashActive > 0, empathyActive > 0, heavyPettingActive > 0 );
			}

			public int weight()
			{	return gearSetWeight( acc1, acc2, acc3, item, hat, leash, empathy, spray );
			}

			public int compareTo( Object o )
			{
				// Keep in mind that all unequips are considered
				// better than equips, so unequips have a change
				// weight of 1.  All others vary from that.

				GearSet that = (GearSet) o;
				int changes = 0;

				// Plexiglass pith helmet is considered the
				// most ideal change, if it exists.

				if ( this.hat != that.hat )
					changes += that.item == null ? 1 : 5;

				// Pumpkin basket is also ideal, lead necklace
				// is less than ideal, standard item is ideal.

				if ( this.item != that.item )
					changes += that.item == null ? 1 : that.item == pumpkinBasket ? 5 : that.item == leadNecklace ? 10 : 5;

				// Tiny plastic accessory changes are preferred
				// over buffs, because they don't require MP.

				if ( this.acc1 != that.acc1 )
					changes += that.item == null ? 1 : 20;
				if ( this.acc2 != that.acc2 )
					changes += that.item == null ? 1 : 20;
				if ( this.acc3 != that.acc3 )
					changes += that.item == null ? 1 : 20;

				// Leash and empathy are probably the only two
				// buffs which are useful, so consider them next.

				if ( this.leash != that.leash )
					changes += that.item == null ? 1 : 80;
				if ( this.empathy != that.empathy )
					changes += that.item == null ? 1 : 80;

				// Pet-buffing spray should be considered an
				// absolute last resort, so make it last place.

				if ( this.spray != that.spray )
					changes += that.item == null ? 1 : 250;

				return changes;
			}

			public String toString()
			{
				StringBuffer text = new StringBuffer();
				text.append( "(" );
				if ( hat == null )
					text.append( "null" );
				else
					text.append( hat.getItemId() );
				text.append( ", " );
				if ( item == null )
					text.append( "null" );
				else
					text.append( item.getItemId() );
				text.append( ", " );
				if ( acc1 == null )
					text.append( "null" );
				else
					text.append( acc1.getItemId() );
				text.append( ", " );
				if ( acc2 == null )
					text.append( "null" );
				else
					text.append( acc2.getItemId() );
				text.append( ", " );
				if ( acc3 == null )
					text.append( "null" );
				else
					text.append( acc3.getItemId() );
				text.append( ", " );
				text.append( leash );
				text.append( ", " );
				text.append( empathy );
				text.append( ", " );
				text.append( spray );
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
		debugOpponents.add( new ArenaOpponent( 1, "Dirty Pair", "Fuzzy Dice", 15 ) );
		debugOpponents.add( new ArenaOpponent( 2, "Radi O'Kol", "Leprechaun", 10 ) );
		debugOpponents.add( new ArenaOpponent( 3, "Captain Scapula", "Spooky Pirate Skeleton", 15 ) );
		debugOpponents.add( new ArenaOpponent( 4, "Queso Ardilla", "Hovering Sombrero", 10 ));
		debugOpponents.add( new ArenaOpponent( 5, "Optimus Pram", "MagiMechTech MicroMechaMech", 7 ) );
	}

	private static FamiliarData debugFamiliar = new FamiliarData( 19, "Creepy", 2, "skewer-mounted razor blade" );

	private static void debug( KoLmafia)
	{
		inventory.add( new AdventureResult( "lead necklace", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic angry goat", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic stab bat", 1 ) );
		inventory.add( new AdventureResult( "tiny plastic cocoabo", 1 ) );

		FamiliarStatus status = new FamiliarStatus(
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

		ArenaOpponent opponent = tool.bestOpponent( debugFamiliar.getId(), weights );

		printMatch( status, opponent, tool, tool.bestMatch() );

		int weight = tool.bestWeight();
		results.append( "Equipping to " + weight + " lbs.<br>" );
		status.chooseGear( weight, false );
	}
	*/


	/**
	 * An internal class used to handle requests which resets a property
	 * for the duration of the current session.
	 */

	public class LocalSettingChanger extends JButton implements ActionListener
	{
		private String title;
		private String property;

		public LocalSettingChanger( String title, String property )
		{
			this.title = title;
			this.property = property;
			addActionListener( this );

			// Turn everything off and back on again
			// so that it's off to start.

			actionPerformed( null );
			actionPerformed( null );
		}

		public void actionPerformed( ActionEvent e )
		{
			boolean toggleValue = StaticEntity.getBooleanProperty( property );
			StaticEntity.setProperty( property, String.valueOf( toggleValue ) );

			if ( toggleValue )
				setText( "Turn Off " + title );
			else
				setText( "Turn On " +  title );
		}
	}
}
