/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CakeArenaManager;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.FamiliarTool;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.StyledChatBuffer;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.button.DisplayFrameButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.StatusPanel;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

public class FamiliarTrainingFrame
	extends GenericFrame
{
	private static final Pattern PRIZE_PATTERN =
		Pattern.compile( "You've earned a prize from the Arena Goodies Sack!.*You acquire an item: <b>(.*?)</b>" );
	private static final Pattern STEAL_PATTERN =
		Pattern.compile( "She also drops an item from her mouth.*You acquire an item: <b>(.*?)</b>" );
	private static final Pattern CAGELOST_PATTERN =
		Pattern.compile( "You enter (.*?) against (.*?) in an Ultimate Cage Match.<p>.*?(?:\\1|this turtle|Grouper groupies|Chauvinist Pigs).*?[.!]<p>\\1 struggles for" );
	private static final Pattern HUNTLOST_PATTERN =
		Pattern.compile( "You enter (.*?) against (.*?) in a Scavenger Hunt.<p>.*?\\1.*?[.!]<p>\\1 finds" );
	private static final Pattern COURSELOST_PATTERN =
		Pattern.compile( "You enter (.*?) against (.*?) in an Obstacle Course race.<p>.*?(?:\\1|Urchins).*?[.!]<p>\\1 makes it through the obstacle course" );
	private static final Pattern HIdELOST_PATTERN =
		Pattern.compile( "You enter (.*?) against (.*?) in a game of Hide and Seek.<p>.*?\\1.*?[.!]<p>\\1 manages to stay hidden" );

	private static final StyledChatBuffer results = new StyledChatBuffer( "", "blue", false );

	private static int losses = 0;
	private static boolean stop = false;

	private final FamiliarTrainingPanel training;
	private KoLCharacterAdapter weightListener;

	public static final int BASE = 1;
	public static final int BUFFED = 2;
	public static final int TURNS = 3;
	public static final int LEARN = 4;

	// Familiar buffing skills and effects
	public static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0, true );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0, true );
	private static final AdventureResult BESTIAL_SYMPATHY = new AdventureResult( "Bestial Sympathy", 0, true );
	private static final AdventureResult BLACK_TONGUE = new AdventureResult( "Black Tongue", 0, true );
	private static final AdventureResult GREEN_GLOW = new AdventureResult( "Healthy Green Glow", 0, true );
	private static final AdventureResult GREEN_HEART = new AdventureResult( "Heart of Green", 0, true );
	private static final AdventureResult GREEN_TONGUE = new AdventureResult( "Green Tongue", 0, true );
	private static final AdventureResult HEAVY_PETTING = new AdventureResult( "Heavy Petting", 0, true );
	private static final AdventureResult WORST_ENEMY = new AdventureResult( "Man's Worst Enemy", 0, true );

	// Familiar buffing items
	private static final AdventureResult PITH_HELMET = new AdventureResult( 1231, 1 );
	private static final AdventureResult CRUMPLED_FEDORA = new AdventureResult( 3328, 1 );

	private static final AdventureResult BUFFING_SPRAY = new AdventureResult( 1512, 1 );
	private static final AdventureResult GREEN_SNOWCONE = ItemPool.get( ItemPool.GREEN_SNOWCONE, 1 );
	private static final AdventureResult BLACK_SNOWCONE = ItemPool.get( ItemPool.BLACK_SNOWCONE, 1 );
	private static final AdventureResult GREEN_CANDY = new AdventureResult( 2309, 1 );
	private static final AdventureResult HALF_ORCHID = new AdventureResult( 2546, 1 );
	private static final AdventureResult SPIKY_COLLAR = new AdventureResult( 2667, 1 );

	private static final AdventureResult BAR_WHIP = new AdventureResult( 2455, 1 );
	private static final int[] tinyPlasticNormal = new int[]
	{
		969,
		970,
		971,
		972,
		973,
		974,
		975,
		976,
		977,
		978,
		979,
		980,
		981,
		982,
		983,
		984,
		985,
		986,
		987,
		988
	};
	private static final int[] tinyPlasticCrimbo = new int[]
	{
		1377,
		1378,
		2201,
		2202
	};

	// Available skills which affect weight
	private static boolean sympathyAvailable;
	private static boolean leashAvailable;
	private static boolean empathyAvailable;

	// Available effects which affect weight
	private static boolean bestialAvailable;
	private static boolean blackConeAvailable;
	private static boolean greenConeAvailable;
	private static boolean greenHeartAvailable;
	private static boolean heavyPettingAvailable;
	private static boolean worstEnemyAvailable;

	// Active effects which affect weight
	private static int leashActive;
	private static int empathyActive;
	private static int bestialActive;
	private static int blackTongueActive;
	private static int greenGlowActive;
	private static int greenHeartActive;
	private static int greenTongueActive;
	private static int heavyPettingActive;
	private static int worstEnemyActive;

	public FamiliarTrainingFrame()
	{
		super( "Familiar Trainer" );

		this.training = new FamiliarTrainingPanel();
		this.setCenterComponent( this.training );

		// Clear left over results from the buffer
		FamiliarTrainingFrame.results.clear();
	}

	@Override
	public void updateDisplayState( final MafiaState displayState )
	{
		this.training.setEnabled( displayState != MafiaState.CONTINUE );
	}

	@Override
	public void dispose()
	{
		FamiliarTrainingFrame.stop = true;
		super.dispose();
	}

	public static final ChatBuffer getResults()
	{
		return FamiliarTrainingFrame.results;
	}

	private class FamiliarTrainingPanel
		extends JPanel
	{
		private FamiliarData familiar;
		private final JComboBox familiars;
		private final JLabel winCount;
		private final JLabel prizeCounter;
		private final JLabel totalWeight;

		private final OpponentsPanel opponentsPanel;
		private final ButtonPanel buttonPanel;
		private final ResultsPanel resultsPanel;

		public FamiliarTrainingPanel()
		{
			super( new BorderLayout( 10, 10 ) );

			JPanel container = new JPanel( new BorderLayout( 10, 10 ) );

			// Get current familiar
			this.familiar = KoLCharacter.getFamiliar();

			// Put familiar changer on top
			this.familiars = new ChangeComboBox( KoLCharacter.getFamiliarList() );
			this.familiars.setRenderer( FamiliarData.getRenderer() );
			container.add( this.familiars, BorderLayout.NORTH );

			// Put results in center
			this.resultsPanel = new ResultsPanel();
			container.add( this.resultsPanel, BorderLayout.CENTER );
			this.add( container, BorderLayout.CENTER );

			// Put opponents on left
			this.opponentsPanel = new OpponentsPanel();
			this.add( this.opponentsPanel, BorderLayout.WEST );

			// Put buttons on right
			JPanel buttonContainer = new JPanel( new BorderLayout() );

			this.buttonPanel = new ButtonPanel();
			buttonContainer.add( this.buttonPanel, BorderLayout.NORTH );

			// List of counters at bottom
			JPanel counterPanel = new JPanel();
			counterPanel.setLayout( new GridLayout( 3, 1 ) );

			// First the win counter
			this.winCount = new JLabel( "", JLabel.CENTER );
			counterPanel.add( this.winCount );

			// Next the prize counter
			this.prizeCounter = new JLabel( "", JLabel.CENTER );
			counterPanel.add( this.prizeCounter );

			// Finally the total familiar weight
			this.totalWeight = new JLabel( "", JLabel.CENTER );
			counterPanel.add( this.totalWeight );

			// Make a refresher for the counters
			FamiliarTrainingFrame.this.weightListener = new KoLCharacterAdapter( new TotalWeightRefresher() );
			KoLCharacter.addCharacterListener( FamiliarTrainingFrame.this.weightListener );

			// Show the counters
			buttonContainer.add( counterPanel, BorderLayout.SOUTH );

			this.add( buttonContainer, BorderLayout.EAST );

			this.add( new StatusPanel(), BorderLayout.SOUTH );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( this.buttonPanel == null || this.familiars == null )
			{
				return;
			}

			super.setEnabled( isEnabled );
			this.buttonPanel.setEnabled( isEnabled );
			this.familiars.setEnabled( isEnabled );
		}

		private class TotalWeightRefresher
			implements Runnable
		{
			public TotalWeightRefresher()
			{
				this.run();
			}

			public void run()
			{
				// Arena wins
				int arenaWins = KoLCharacter.getArenaWins();
				FamiliarTrainingPanel.this.winCount.setText( arenaWins + " arena wins" );

				// Wins to next prize
				int nextPrize = 5 - arenaWins % 5;
				FamiliarTrainingPanel.this.prizeCounter.setText( nextPrize + " wins to next prize" );

				// Terrarium weight
				int totalTerrariumWeight = 0;

				FamiliarData[] familiarArray = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
				KoLCharacter.getFamiliarList().toArray( familiarArray );

				for ( int i = 0; i < familiarArray.length; ++i )
				{
					if ( familiarArray[ i ].getWeight() != 1 )
					{
						totalTerrariumWeight += familiarArray[ i ].getWeight();
					}
				}

				FamiliarTrainingPanel.this.totalWeight.setText( totalTerrariumWeight + " lb. terrarium" );
			}
		}

		private class OpponentsPanel
			extends JPanel
		{
			public OpponentsPanel()
			{
				// Get current opponents
				LockableListModel opponents = CakeArenaManager.getOpponentList();
				int opponentCount = opponents.size();

				this.setLayout( new GridLayout( opponentCount, 1, 0, 20 ) );

				for ( int i = 0; i < opponentCount; ++i )
				{
					this.add( new OpponentLabel( (ArenaOpponent) opponents.get( i ) ) );
				}
			}

			private class OpponentLabel
				extends JLabel
			{
				public OpponentLabel( final ArenaOpponent opponent )
				{
					super(
						"<html><center>" + opponent.getName() + "<br>" + "(" + opponent.getWeight() + " lbs)</center></html>",
						FamiliarDatabase.getFamiliarImage( opponent.getRace() ), JLabel.CENTER );

					this.setVerticalTextPosition( JLabel.BOTTOM );
					this.setHorizontalTextPosition( JLabel.CENTER );
				}
			}
		}

		private class ButtonPanel
			extends JPanel
		{
			private final JButton matchup, base, buffed, turns, stop, save;
			// private JButton debug;
			final JButton learn, equip;

			public ButtonPanel()
			{
				JPanel containerPanel = new JPanel( new GridLayout( 11, 1, 5, 5 ) );

				this.matchup = new DisplayFrameButton( "View Matchup", "CakeArenaFrame" );
				containerPanel.add( this.matchup );

				containerPanel.add( new JLabel() );

				this.base = new JButton( "Train Base Weight" );
				this.base.addActionListener( new BaseListener() );
				containerPanel.add( this.base );

				this.buffed = new JButton( "Train Buffed Weight" );
				this.buffed.addActionListener( new BuffedListener() );
				containerPanel.add( this.buffed );

				this.turns = new JButton( "Train for Set Turns" );
				this.turns.addActionListener( new TurnsListener() );
				containerPanel.add( this.turns );

				containerPanel.add( new JLabel() );

				this.stop = new JButton( "Stop Training" );
				this.stop.addActionListener( new StopListener() );
				containerPanel.add( this.stop );

				this.save = new JButton( "Save Transcript" );
				this.save.addActionListener( new SaveListener() );
				containerPanel.add( this.save );

				containerPanel.add( new JLabel() );

				this.learn = new JButton( "Learn Familiar Strengths" );
				this.learn.addActionListener( new LearnListener() );
				containerPanel.add( this.learn );

				this.equip = new JButton( "Equip All Familiars" );
				this.equip.addActionListener( new EquipAllListener() );
				containerPanel.add( this.equip );

				// debug = new JButton( "Debug" );
				// debug.addActionListener( new DebugListener() );
				// this.add( debug );

				this.add( containerPanel );
			}

			@Override
			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );

				if ( this.matchup != null )
				{
					this.matchup.setEnabled( isEnabled );
				}
				if ( this.base != null )
				{
					this.base.setEnabled( isEnabled );
				}
				if ( this.buffed != null )
				{
					this.buffed.setEnabled( isEnabled );
				}
				if ( this.turns != null )
				{
					this.turns.setEnabled( isEnabled );
				}
				// this.stop is always enabled
				if ( this.save != null )
				{
					this.save.setEnabled( isEnabled );
				}
				if ( this.learn != null )
				{
					this.learn.setEnabled( isEnabled );
				}
				if ( this.equip != null )
				{
					this.equip.setEnabled( isEnabled );
				}
			}

			private class BaseListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					// Prompt for goal
					Integer value = InputFieldUtilities.getQuantity( "Train up to what base weight?", 20, 20 );
					int goal = ( value == null ) ? 0 : value.intValue();

					// Quit if canceled
					if ( goal == 0 )
					{
						return;
					}

					// Level the familiar

					FamiliarTrainingFrame.levelFamiliar( goal, FamiliarTrainingFrame.BASE );
				}
			}

			private class BuffedListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					// Prompt for goal
					Integer value = InputFieldUtilities.getQuantity( "Train up to what buffed weight?", 48, 20 );
					int goal = ( value == null ) ? 0 : value.intValue();

					// Quit if canceled
					if ( goal == 0 )
					{
						return;
					}

					// Level the familiar

					FamiliarTrainingFrame.levelFamiliar( goal, FamiliarTrainingFrame.BUFFED );
				}
			}

			private class TurnsListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					// Prompt for goal
					Integer value = InputFieldUtilities.getQuantity( "Train for how many turns?", Integer.MAX_VALUE, 1 );
					int goal = ( value == null ) ? 0 : value.intValue();

					// Quit if canceled
					if ( goal == 0 )
					{
						return;
					}

					// Level the familiar

					FamiliarTrainingFrame.levelFamiliar( goal, FamiliarTrainingFrame.TURNS );
				}
			}

			private class StopListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					FamiliarTrainingFrame.stop = true;
				}
			}

			private class SaveListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					JFileChooser chooser = new JFileChooser( "data" );
					chooser.showSaveDialog( FamiliarTrainingFrame.this );

					File output = chooser.getSelectedFile();

					if ( output == null )
					{
						return;
					}

					try
					{
						PrintStream ostream = LogStream.openStream( output, false );
						ostream.println( FamiliarTrainingFrame.results.getHTMLContent().replaceAll(
							"<br>", KoLConstants.LINE_BREAK ) );
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

			private class LearnListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					if ( FamiliarTrainingPanel.this.familiar == FamiliarData.NO_FAMILIAR )
					{
						return;
					}

					// Prompt for trials
					Integer value = InputFieldUtilities.getQuantity( "How many trials per event per rank?", 20, 3 );
					int trials = ( value == null ) ? 0 : value.intValue();

					// Quit if canceled
					if ( trials == 0 )
					{
						return;
					}

					// Nag dialog
					int turns = trials * 12;
					if ( !InputFieldUtilities.confirm( "This will take up to " + turns + " adventures and cost up to " + KoLConstants.COMMA_FORMAT.format( turns * 100 ) + " meat. Are you sure?" ) )
					{
						return;
					}

					// Learn familiar parameters

					int[] skills = FamiliarTrainingFrame.learnFamiliarParameters( trials );

					// Save familiar parameters

					if ( skills != null )
					{
						int[] original =
							FamiliarDatabase.getFamiliarSkills( FamiliarTrainingPanel.this.familiar.getId() );
						boolean changed = false;

						for ( int i = 0; i < original.length; ++i )
						{
							if ( skills[ i ] != original[ i ] )
							{
								changed = true;
								break;
							}
						}

						if ( changed && InputFieldUtilities.confirm( "Save arena parameters for the " + FamiliarTrainingPanel.this.familiar.getRace() + "?" ) )
						{
							FamiliarDatabase.setFamiliarSkills( FamiliarTrainingPanel.this.familiar.getRace(), skills );
						}

						KoLmafia.updateDisplay(
							MafiaState.CONTINUE,
							"Learned skills are " + ( changed ? "different from" : "the same as" ) + " those in familiar database." );
					}
				}
			}

			private class EquipAllListener
				extends ThreadedListener
			{
				private final ArrayList<AdventureResult> closetItems = new ArrayList<AdventureResult>();
				private final ArrayList<AdventureResult> storageItems = new ArrayList<AdventureResult>();
				private final ArrayList<GenericRequest> requests = new ArrayList<GenericRequest>();

				@Override
				protected void execute()
				{
					KoLmafia.updateDisplay( "Equipping familiars..." );

					FamiliarData current = KoLCharacter.getFamiliar();

					FamiliarData[] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
					KoLCharacter.getFamiliarList().toArray( familiars );

					for ( int i = 0; i < familiars.length; ++i )
					{
						this.equipFamiliar( familiars[ i ] );
					}

					// If nothing to do, do nothing!

					if ( this.requests.size() == 0 )
					{
						return;
					}

					if ( closetItems.size() > 0 )
					{
						AdventureResult[] array = new AdventureResult[ this.closetItems.size() ];
						this.closetItems.toArray( array );
						RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, array ) );
					}

					if ( storageItems.size() > 0 )
					{
						AdventureResult[] array = new AdventureResult[ this.storageItems.size() ];
						this.storageItems.toArray( array );
						RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, array ) );
					}

					GenericRequest[] array = new GenericRequest[ this.requests.size() ];
					this.requests.toArray( array );

					for ( int i = 0; i < array.length; ++i )
					{
						RequestThread.postRequest( array[ i ] );
					}

					RequestThread.postRequest( new FamiliarRequest( current ) );

					// Leave list empty for next time and
					// allow garbage collection.

					this.closetItems.clear();
					this.storageItems.clear();
					this.requests.clear();
				}

				private void equipFamiliar( FamiliarData familiar )
				{
					String itemName = FamiliarDatabase.getFamiliarItem( familiar.getId() );

					if ( itemName == null || itemName.equals( "" ) )
					{
						return;
					}

					if ( familiar.getItem().getName().equals( itemName ) )
					{
						return;
					}

					AdventureResult item = new AdventureResult( itemName, 1, false );
					if ( item.getCount( KoLConstants.inventory ) > 0 )
					{
						// Use one from inventory
					}
					else if ( item.getCount( KoLConstants.closet ) > 0 )
					{
						// Use one from the closet
						this.closetItems.add( item );
					}
					else if ( item.getCount( KoLConstants.storage ) > 0 )
					{
						// Use one from storage
						this.storageItems.add( item );
					}
					else
					{
						return;
					}

					GenericRequest req;
					if ( KoLCharacter.getFamiliar().equals( familiar ) )
					{
						req = new EquipmentRequest( item );
					}
					else
					{
						req = new FamiliarRequest( familiar, item );
					}
					this.requests.add( req );

					return;
				}
			}

			/*
			 * private class DebugListener extends ThreadedListener { public void run() { debug(); } }
			 */
		}

		private class ResultsPanel
			extends JPanel
		{
			RequestPane resultsDisplay;

			public ResultsPanel()
			{
				this.setLayout( new BorderLayout( 10, 10 ) );

				RequestPane resultsDisplay = new RequestPane();
				JScrollPane scroller = FamiliarTrainingFrame.results.addDisplay( resultsDisplay );
				JComponentUtilities.setComponentSize( scroller, 400, 400 );

				this.add( scroller, BorderLayout.CENTER );
			}
		}

		private class ChangeComboBox
			extends JComboBox
		{
			public ChangeComboBox( final LockableListModel selector )
			{
				super( selector );
				this.addActionListener( new ChangeComboBoxListener() );
			}

			private class ChangeComboBoxListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					FamiliarData selection = (FamiliarData) ChangeComboBox.this.getSelectedItem();

					if ( selection == null || selection == FamiliarTrainingPanel.this.familiar )
					{
						return;
					}

					RequestThread.postRequest( new FamiliarRequest( selection ) );
					FamiliarTrainingPanel.this.familiar = KoLCharacter.getFamiliar();
				}
			}
		}
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	private static final boolean levelFamiliar( final int goal, final int type )
	{
		return FamiliarTrainingFrame.levelFamiliar( goal, type, Preferences.getBoolean( "debugFamiliarTraining" ) );
	}

	/**
	 * Utility method to level the current familiar by fighting the current arena opponents.
	 *
	 * @param goal Weight goal for the familiar
	 * @param type BASE, BUFF, or TURNS
	 * @param buffs true if should cast buffs during training
	 * @param debug true if we are debugging
	 */

	public static final boolean levelFamiliar( final int goal, final int type, final boolean debug )
	{
		// Clear the output
		FamiliarTrainingFrame.results.clear();

		// Permit training session to proceed
		FamiliarTrainingFrame.stop = false;
		KoLmafia.forceContinue();

		// Get current familiar
		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "No familiar selected to train." );
			return false;
		}

		if ( !familiar.trainable() )
		{
			FamiliarTrainingFrame.statusMessage(
				MafiaState.ERROR, "Don't know how to train a " + familiar.getRace() + " yet." );
			return false;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus();

		// Identify the familiar we are training
		FamiliarTrainingFrame.printFamiliar( status, goal, type );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Print available buffs and items and current buffs
		FamiliarTrainingFrame.results.append( status.printCurrentBuffs() );
		FamiliarTrainingFrame.results.append( status.printAvailableBuffs() );
		FamiliarTrainingFrame.results.append( status.printCurrentEquipment() );
		FamiliarTrainingFrame.results.append( status.printAvailableEquipment() );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Get opponent list
		LockableListModel opponents = CakeArenaManager.getOpponentList();

		// Print the opponents
		FamiliarTrainingFrame.printOpponents( opponents );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Save your current outfit
		SpecialOutfit.createImplicitCheckpoint();

		// Let the battles begin!

		KoLmafia.updateDisplay( "Starting training session..." );

		// Iterate until we reach the goal
		FamiliarTrainingFrame.losses = 0;
		while ( !FamiliarTrainingFrame.goalMet( status, goal, type ) && FamiliarTrainingFrame.losses < 5 )
		{
			// If user canceled, bail now
			if ( FamiliarTrainingFrame.stop || !KoLmafia.permitsContinue() )
			{
				FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "Training session aborted.", true );
				return false;
			}

			// Make sure you have an adventure left
			if ( KoLCharacter.getAdventuresLeft() < 1 )
			{
				FamiliarTrainingFrame.statusMessage(
					MafiaState.ERROR, "Training stopped: out of adventures.", true );
				return false;
			}

			// Make sure you have enough meat to pay for the contest
			if ( KoLCharacter.getAvailableMeat() < 100 )
			{
				FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "Training stopped: out of meat.", true );
				return false;
			}

			// Switch to the required familiar
			if ( KoLCharacter.getFamiliar() != familiar )
			{
				RequestThread.postRequest( new FamiliarRequest( familiar ) );
			}

			// Choose possible weights
			int[] weights = status.getWeights();

			// Choose next opponent
			ArenaOpponent opponent = tool.bestOpponent( familiar.getId(), weights );

			if ( opponent == null )
			{
				FamiliarTrainingFrame.statusMessage(
					MafiaState.ERROR, "Couldn't choose a suitable opponent.", true );
				return false;
			}

			// Change into appropriate gear
			status.changeGear( tool.bestWeight() );
			if ( !KoLmafia.permitsContinue() )
			{
				FamiliarTrainingFrame.statusMessage(
					MafiaState.ERROR, "Training stopped: internal error.", true );
				return false;
			}

			if ( debug )
			{
				break;
			}

			// Enter the contest
			if ( FamiliarTrainingFrame.fightMatch( status, tool, opponent ) <= 0 )
			{
				++FamiliarTrainingFrame.losses;
			}
			else
			{
				FamiliarTrainingFrame.losses = 0;
			}
		}

		if ( FamiliarTrainingFrame.losses >= 5 )
		{
			FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "Too many consecutive losses.", true );
			return false;
		}

		// Done training. Restore original outfit
		SpecialOutfit.restoreImplicitCheckpoint();

		if ( familiar.getId() != FamiliarPool.CHAMELEON )
		{
			// Find and wear an appropriate item
			familiar.findAndWearItem( false );
		}

		boolean result = type == FamiliarTrainingFrame.BUFFED ? FamiliarTrainingFrame.buffFamiliar( goal ) : true;

		FamiliarTrainingFrame.statusMessage( MafiaState.CONTINUE, "Training session completed." );
		return result;
	}

	/**
	 * Utility method to derive the arena parameters of the current familiar
	 *
	 * @param trials How many trials per event
	 */

	private static final int[] learnFamiliarParameters( final int trials )
	{
		// Clear the output
		FamiliarTrainingFrame.results.clear();

		// Permit training session to proceed
		FamiliarTrainingFrame.stop = false;

		// Get current familiar

		if ( KoLCharacter.getFamiliar() == FamiliarData.NO_FAMILIAR )
		{
			FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "No familiar selected to train." );
			return null;
		}

		int events = 12 * trials;
		// Make sure you have enough adventures left
		if ( KoLCharacter.getAdventuresLeft() < events )
		{
			FamiliarTrainingFrame.statusMessage(
				MafiaState.ERROR, "You need to have at least " + events + " adventures available." );
			return null;
		}

		// Make sure you have enough meat to pay for the contests
		if ( KoLCharacter.getAvailableMeat() < 100 * events )
		{
			FamiliarTrainingFrame.statusMessage(
				MafiaState.ERROR,
				"You need to have at least " + KoLConstants.COMMA_FORMAT.format( 100 * events ) + " meat available." );
			return null;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus();

		// Identify the familiar we are training
		FamiliarTrainingFrame.printFamiliar( status, trials, FamiliarTrainingFrame.LEARN );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Print available buffs and items and current buffs
		FamiliarTrainingFrame.results.append( status.printCurrentBuffs() );
		FamiliarTrainingFrame.results.append( status.printAvailableBuffs() );
		FamiliarTrainingFrame.results.append( status.printCurrentEquipment() );
		FamiliarTrainingFrame.results.append( status.printAvailableEquipment() );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Get opponent list
		LockableListModel opponents = CakeArenaManager.getOpponentList();

		// Print the opponents
		FamiliarTrainingFrame.printOpponents( opponents );
		FamiliarTrainingFrame.results.append( "<br>" );

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Save your current outfit
		SpecialOutfit.createImplicitCheckpoint();

		// Let the battles begin!
		KoLmafia.updateDisplay( "Starting training session..." );

		// XP earned indexed by [event][rank]
		int[][] xp = new int[ 4 ][ 3 ];

		// Array of skills to test with
		int[] test = new int[ 4 ];

		// Array of contest suckage

		int[] skills = new int[ 4 ];
		boolean[] suckage = new boolean[ 4 ];

		// Iterate for the specified number of trials
		for ( int trial = 1; trial <= trials && KoLmafia.permitsContinue(); ++trial )
		{
			skills = FamiliarTrainingFrame.learnFamiliarParameters( trial, status, tool, xp, test, suckage );
		}

		// Done training. Restore original outfit
		SpecialOutfit.restoreImplicitCheckpoint();

		return skills;
	}

	private static final int[] learnFamiliarParameters( final int trial, final FamiliarStatus status,
		final FamiliarTool tool, final int[][] xp, final int[] test, final boolean[] suckage )
	{
		// Iterate through the contests
		for ( int contest = 0; contest < 4; ++contest )
		{
			// Initialize test parameters
			test[ 0 ] = test[ 1 ] = test[ 2 ] = test[ 3 ] = 0;

			// Iterate through the ranks
			for ( int rank = 0; rank < 3; ++rank )
			{
				// Skip contests in which the familiar sucks
				if ( suckage[ contest ] )
				{
					continue;
				}

				// If user canceled, bail now
				if ( FamiliarTrainingFrame.stop || !KoLmafia.permitsContinue() )
				{
					FamiliarTrainingFrame.printTrainingResults( trial, status, xp, suckage );
					FamiliarTrainingFrame.statusMessage( MafiaState.ERROR, "Training session aborted.", true );
					return null;
				}

				// Initialize test parameters
				test[ contest ] = rank + 1;

				FamiliarTrainingFrame.statusMessage(
					MafiaState.CONTINUE,
					CakeArenaManager.getEvent( contest + 1 ) + " rank " + ( rank + 1 ) + ": trial " + trial );

				// Choose possible weights
				int[] weights = status.getWeights();

				// Choose next opponent
				ArenaOpponent opponent = tool.bestOpponent( test, weights );

				if ( opponent == null )
				{
					FamiliarTrainingFrame.printTrainingResults( trial, status, xp, suckage );
					FamiliarTrainingFrame.statusMessage(
						MafiaState.ERROR, "Couldn't choose a suitable opponent.", true );
					return null;
				}

				int match = tool.bestMatch();
				if ( match != contest + 1 )
				{
					// Informative message only. Do not stop session.
					FamiliarTrainingFrame.statusMessage(
						MafiaState.ERROR,
						"Internal error: Selected " + CakeArenaManager.getEvent( match ) + " rather than " + CakeArenaManager.getEvent( contest + 1 ) );
					// Use contest, even if with bad weight
					match = contest + 1;
				}

				// Change into appropriate gear
				status.changeGear( tool.bestWeight() );
				if ( !KoLmafia.permitsContinue() )
				{
					FamiliarTrainingFrame.printTrainingResults( trial, status, xp, suckage );
					FamiliarTrainingFrame.statusMessage(
						MafiaState.ERROR, "Training stopped: internal error.", true );
					return null;
				}

				// Enter the contest
				int trialXP = FamiliarTrainingFrame.fightMatch( status, tool, opponent, match );

				if ( trialXP < 0 )
				{
					suckage[ contest ] = true;
				}
				else
				{
					xp[ contest ][ rank ] += trialXP;
				}
			}
		}

		return FamiliarTrainingFrame.printTrainingResults( trial, status, xp, suckage );
	}

	private static int[] printTrainingResults( final int trial, final FamiliarStatus status, final int[][] xp,
		final boolean[] suckage )
	{
		// Original skill rankings
		int[] original = FamiliarDatabase.getFamiliarSkills( KoLCharacter.getFamiliar().getId() );

		// Derived skill rankings
		int skills[] = new int[ 4 ];

		StringBuilder text = new StringBuilder();

		text.append( "<br>Results for " + KoLCharacter.getFamiliar().getRace() + " after " + trial + " trials using " + status.turnsUsed() + " turns:<br><br>" );

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
			text.append( "<td>" + CakeArenaManager.getEvent( contest + 1 ) + "</td>" );

			int bestXP = 0;
			int bestRank = 0;
			for ( int rank = 0; rank < 3; ++rank )
			{
				int rankXP = suckage[ contest ] ? 0 : xp[ contest ][ rank ];
				text.append( "<td align=center>" + rankXP + "</td>" );
				if ( rankXP > bestXP )
				{
					bestXP = rankXP;
					bestRank = rank + 1;
				}
			}

			skills[ contest ] = bestRank;

			text.append( "<td align=center>" + original[ contest ] + "</td>" );
			text.append( "<td align=center>" + bestRank + "</td>" );
			text.append( "</tr>" );
		}

		// Close the table
		text.append( "</table><br><br>" );

		FamiliarTrainingFrame.results.append( text.toString() );

		return skills;
	}

	/**
	 * Utility method to buff the current familiar to the specified weight or higher.
	 *
	 * @param weight Weight goal for the familiar
	 */

	public static final boolean buffFamiliar( final int weight )
	{
		// Get current familiar. If none, punt.

		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a familiar equipped." );
			return false;
		}

		// If the familiar is already heavy enough, nothing to do
		if ( familiar.getModifiedWeight() >= weight )
		{
			return true;
		}

		FamiliarStatus status = new FamiliarStatus();
		int[] weights = status.getWeights();
		Arrays.sort( weights );

		status.changeGear( weights[ weights.length - 1 ] );
		if ( familiar.getModifiedWeight() >= weight )
		{
			return true;
		}

		if ( !InventoryManager.hasItem( FamiliarData.PUMPKIN_BUCKET ) &&
		     !InventoryManager.hasItem( FamiliarData.FLOWER_BOUQUET ) &&
		     !InventoryManager.hasItem( FamiliarData.FIREWORKS ) &&
		     !InventoryManager.hasItem( FamiliarData.SUGAR_SHIELD ) &&
		     status.familiarItemWeight != 0 &&
		     !InventoryManager.hasItem( status.familiarItem ) &&
		     KoLCharacter.canInteract() )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "buy 1 " + status.familiarItem.getName() );
			RequestThread.postRequest( new EquipmentRequest( status.familiarItem ) );

			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.leashAvailable && FamiliarTrainingFrame.leashActive == 0 )
		{
			RequestThread.postRequest( UseSkillRequest.getInstance( "leash of linguini", 1 ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.empathyAvailable && FamiliarTrainingFrame.empathyActive == 0 )
		{
			RequestThread.postRequest( UseSkillRequest.getInstance( "empathy of the newt", 1 ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		// Add on a green heart first, if you know the difference is
		// less than three.

		if ( FamiliarTrainingFrame.greenHeartAvailable && FamiliarTrainingFrame.greenHeartActive == 0 && familiar.getModifiedWeight() + 3 >= weight )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.GREEN_CANDY ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.bestialAvailable && FamiliarTrainingFrame.bestialActive == 0 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.HALF_ORCHID ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.heavyPettingAvailable && FamiliarTrainingFrame.heavyPettingActive == 0 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.BUFFING_SPRAY ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.greenConeAvailable && FamiliarTrainingFrame.greenTongueActive == 0 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.GREEN_SNOWCONE ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( !FamiliarTrainingFrame.greenConeAvailable && FamiliarTrainingFrame.greenTongueActive == 0 && FamiliarTrainingFrame.blackConeAvailable && FamiliarTrainingFrame.blackTongueActive == 0 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.BLACK_SNOWCONE ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		if ( FamiliarTrainingFrame.worstEnemyAvailable && FamiliarTrainingFrame.worstEnemyActive == 0 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( FamiliarTrainingFrame.SPIKY_COLLAR ) );
			if ( familiar.getModifiedWeight() >= weight )
			{
				return true;
			}
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "Can't buff and equip familiar to reach " + weight + " lbs." );
		return false;
	}

	private static final void statusMessage( final MafiaState state, final String message )
	{
		FamiliarTrainingFrame.statusMessage( state, message, false );
	}

	private static final void statusMessage( final MafiaState state, final String message, boolean restoreOutfit )
	{
		if ( restoreOutfit )
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}

		if ( state == MafiaState.ERROR || message.endsWith( "lost." ) )
		{
			FamiliarTrainingFrame.results.append( "<font color=red>" + message + "</font><br>" );
		}
		else if ( message.indexOf( "experience" ) != -1 )
		{
			FamiliarTrainingFrame.results.append( "<font color=green>" + message + "</font><br>" );
		}
		else if ( message.indexOf( "prize" ) != -1 )
		{
			FamiliarTrainingFrame.results.append( "<font color=blue>" + message + "</font><br>" );
		}
		else
		{
			FamiliarTrainingFrame.results.append( message + "<br>" );
		}

		KoLmafia.updateDisplay( state, message );
	}

	private static final void printFamiliar( final FamiliarStatus status, final int goal, final int type )
	{
		FamiliarData familiar = status.getFamiliar();
		String name = familiar.getName();
		String race = familiar.getRace();
		int weight = familiar.getWeight();
		String hope = "";

		if ( type == FamiliarTrainingFrame.BASE )
		{
			hope = " to " + goal + " lbs. base weight";
		}
		else if ( type == FamiliarTrainingFrame.BUFFED )
		{
			hope = " to " + goal + " lbs. buffed weight";
		}
		else if ( type == FamiliarTrainingFrame.TURNS )
		{
			hope = " for " + goal + " turns";
		}
		else if ( type == FamiliarTrainingFrame.LEARN )
		{
			hope = " for " + goal + " iterations to learn arena strengths";
		}

		FamiliarTrainingFrame.results.append( "Training " + name + " the " + weight + " lb. " + race + hope + ".<br>" );
	}

	private static final void printOpponents( final LockableListModel opponents )
	{
		FamiliarTrainingFrame.results.append( "Opponents:<br>" );
		int opponentCount = opponents.size();
		for ( int i = 0; i < opponentCount; ++i )
		{
			ArenaOpponent opponent = (ArenaOpponent) opponents.get( i );
			String name = opponent.getName();
			String race = opponent.getRace();
			int weight = opponent.getWeight();
			FamiliarTrainingFrame.results.append( name + " the " + weight + " lb. " + race + "<br>" );
		}
	}

	private static final boolean goalMet( final FamiliarStatus status, final int goal, final int type )
	{
		switch ( type )
		{
		case BASE:
			return status.baseWeight() >= goal;

		case BUFFED:
			return status.maxWeight( true ) >= goal;

		case TURNS:
			return status.turnsUsed() >= goal;
		}

		return false;
	}

	private static final void printMatch( final FamiliarStatus status, final ArenaOpponent opponent,
		final FamiliarTool tool, final int match )
	{
		FamiliarData familiar = status.getFamiliar();
		int weight = tool.bestWeight();
		int diff = tool.difference();

		StringBuilder text = new StringBuilder();
		int round = status.turnsUsed() + 1;
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

		FamiliarTrainingFrame.results.append( text.toString() );

		KoLmafia.updateDisplay( "Round " + round + ": " + familiar.getName() + " vs. " + opponent.getName() + "..." );
	}

	private static final int fightMatch( final FamiliarStatus status, final FamiliarTool tool,
		final ArenaOpponent opponent )
	{
		return FamiliarTrainingFrame.fightMatch( status, tool, opponent, tool.bestMatch() );
	}

	private static final int fightMatch( final FamiliarStatus status, final FamiliarTool tool,
		final ArenaOpponent opponent, final int match )
	{
		// If user aborted, bail now
		if ( KoLmafia.refusesContinue() )
		{
			return 0;
		}

		// Tell the user about the match
		FamiliarTrainingFrame.printMatch( status, opponent, tool, match );

		// Run the match
		GenericRequest request = new CakeArenaRequest( opponent.getId(), match );
		RequestThread.postRequest( request );

		// Pass the response text to the FamiliarStatus to
		// add familiar items and deduct a turn.
		int xp = CakeArenaManager.earnedXP( request.responseText );
		status.processMatchResult( request.responseText, xp );

		// Return the amount of XP the familiar earned
		return FamiliarTrainingFrame.badContest( request.responseText, match ) ? -1 : xp;
	}

	private static final boolean badContest( final String response, final int match )
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
			matcher = FamiliarTrainingFrame.CAGELOST_PATTERN.matcher( response );
			break;
		case 2: // "You enter Trot against Vine Vidi Vici in a
			// Scavenger Hunt.<p>Trot keeps getting distracted from
			// the hunt and randomly ramming into things.<p>Trot
			// finds 12 items from the list.<p>Vine Vidi Vici finds
			// 17 items.<p>Trot lost."
			matcher = FamiliarTrainingFrame.HUNTLOST_PATTERN.matcher( response );
			break;
		case 3: // "You enter Gort against Pork Soda in an Obstacle
			// Course race.<p>Gort is too short to get over most of
			// the obstacles.<p>Gort makes it through the obstacle
			// course in 49 seconds.<p>Pork Soda takes 29
			// seconds. <p>Gort lost."
			matcher = FamiliarTrainingFrame.COURSELOST_PATTERN.matcher( response );
			break;
		case 4: // "You enter Tot against Pork Soda in a game of Hide
			// and Seek.<p>Tot buzzes incessantly, making it very
			// difficult to remain concealed.<p>Tot manages to stay
			// hidden for 28 seconds.<p>Pork Soda stays hidden for
			// 53 seconds.<p>Tot lost."
			matcher = FamiliarTrainingFrame.HIdELOST_PATTERN.matcher( response );
			break;
		default:
			return false;
		}

		return matcher.find();
	}

	/**
	 * A class to hold everything that can modify the weight of the current familiar: available items and whether they
	 * are equipped available skills and whether they are active
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
		AdventureResult weapon;
		AdventureResult offhand;

		AdventureResult[] acc = new AdventureResult[ 3 ];

		// Available equipment which affects weight
		AdventureResult specItem;
		int specWeight;

		boolean pithHelmet;
		boolean crumpledFedora;
		boolean leadNecklace;
		boolean ratHeadBalloon;
		boolean bathysphere;
		boolean dasBoot;
		boolean pumpkinBucket;
		boolean flowerBouquet;
		boolean boxFireworks;
		boolean sugarShield;
		boolean doppelganger;

		int tpCount;
		int whipCount;
		AdventureResult[] tp = new AdventureResult[ 3 ];

		// Weights
		TreeSet<Integer> weights;

		// Gear sets
		ArrayList<GearSet> gearSets;

		public FamiliarStatus()
		{
			// Find out which familiar we are working with
			this.familiar = KoLCharacter.getFamiliar();

			// Get details about the special item it can wear
			String name = FamiliarDatabase.getFamiliarItem( this.familiar.getId() );
			this.familiarItem = new AdventureResult( name, 1, false );
			this.familiarItemWeight = FamiliarData.itemWeightModifier( this.familiarItem.getItemId() );

			// No turns have been used yet
			this.turns = 0;

			// Initialize set of weights
			this.weights = new TreeSet<Integer>();

			// Initialize the list of GearSets
			this.gearSets = new ArrayList<GearSet>();

			// Check skills and equipment
			this.updateStatus();
		}

		public void updateStatus()
		{
			// Check available skills
			this.checkSkills();

			// Check current equipment
			this.checkCurrentEquipment();

			// Check available equipment
			this.checkAvailableEquipment( KoLConstants.inventory );
		}

		private void checkSkills()
		{
			// Look at skills to decide which ones are possible
			FamiliarTrainingFrame.sympathyAvailable = KoLCharacter.hasAmphibianSympathy();
			FamiliarTrainingFrame.empathyAvailable =
				KoLCharacter.hasSkill( "Empathy of the Newt" ) && UseSkillRequest.hasTotem();
			FamiliarTrainingFrame.leashAvailable = KoLCharacter.hasSkill( "Leash of Linguini" );

			FamiliarTrainingFrame.bestialAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.HALF_ORCHID );
			FamiliarTrainingFrame.blackConeAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.BLACK_SNOWCONE );
			FamiliarTrainingFrame.greenConeAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.GREEN_SNOWCONE );
			FamiliarTrainingFrame.greenHeartAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.GREEN_CANDY );
			FamiliarTrainingFrame.heavyPettingAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.BUFFING_SPRAY ) || NPCStoreDatabase.contains( "Knob Goblin pet-buffing spray" );
			FamiliarTrainingFrame.worstEnemyAvailable =
				KoLCharacter.canInteract() || InventoryManager.hasItem( FamiliarTrainingFrame.SPIKY_COLLAR );

			// Look at effects to decide which ones are active;
			FamiliarTrainingFrame.empathyActive = FamiliarTrainingFrame.EMPATHY.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.leashActive = FamiliarTrainingFrame.LEASH.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.bestialActive =
				FamiliarTrainingFrame.BESTIAL_SYMPATHY.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.blackTongueActive =
				FamiliarTrainingFrame.BLACK_TONGUE.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.greenGlowActive =
				FamiliarTrainingFrame.GREEN_GLOW.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.greenHeartActive =
				FamiliarTrainingFrame.GREEN_HEART.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.greenTongueActive =
				FamiliarTrainingFrame.GREEN_TONGUE.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.heavyPettingActive =
				FamiliarTrainingFrame.HEAVY_PETTING.getCount( KoLConstants.activeEffects );
			FamiliarTrainingFrame.worstEnemyActive =
				FamiliarTrainingFrame.WORST_ENEMY.getCount( KoLConstants.activeEffects );
		}

		private void checkCurrentEquipment()
		{
			this.checkCurrentEquipment(
				EquipmentManager.getEquipment( EquipmentManager.WEAPON ),
				EquipmentManager.getEquipment( EquipmentManager.OFFHAND ),
				EquipmentManager.getEquipment( EquipmentManager.HAT ),
				EquipmentManager.getEquipment( EquipmentManager.FAMILIAR ),
				EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ),
				EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ),
				EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ) );
		}

		private void checkCurrentEquipment( final AdventureResult weapon, final AdventureResult offhand,
			final AdventureResult hat, final AdventureResult item, final AdventureResult acc1,
			final AdventureResult acc2, final AdventureResult acc3 )
		{
			// Initialize equipment to default
			this.weapon = null;
			this.offhand = null;
			this.hat = null;
			this.item = null;

			this.specItem = null;
			this.specWeight = 0;

			this.pithHelmet = false;
			this.crumpledFedora = false;
			this.leadNecklace = false;
			this.ratHeadBalloon = false;
			this.bathysphere = false;
			this.dasBoot = false;
			this.pumpkinBucket = false;
			this.flowerBouquet = false;
			this.boxFireworks = false;
			this.sugarShield = false;
			this.doppelganger = false;

			this.whipCount = 0;
			this.tpCount = 0;

			// Check hat for pithiness
			if ( hat.getItemId() == FamiliarTrainingFrame.PITH_HELMET.getItemId() )
			{
				this.pithHelmet = true;
				this.hat = FamiliarTrainingFrame.PITH_HELMET;
			}

			if ( hat.getItemId() == FamiliarTrainingFrame.CRUMPLED_FEDORA.getItemId() )
			{
				this.crumpledFedora = true;
				this.hat = FamiliarTrainingFrame.CRUMPLED_FEDORA;
			}

			if ( weapon != null && weapon.getItemId() == FamiliarTrainingFrame.BAR_WHIP.getItemId() )
			{
				++this.whipCount;
				this.weapon = FamiliarTrainingFrame.BAR_WHIP;
			}

			if ( offhand != null && offhand.getItemId() == FamiliarTrainingFrame.BAR_WHIP.getItemId() )
			{
				++this.whipCount;
				this.offhand = FamiliarTrainingFrame.BAR_WHIP;
			}

			// Check current familiar item
			if ( item != null )
			{
				int itemId = item.getItemId();

				if ( itemId == this.familiarItem.getItemId() )
				{
					this.item = this.specItem = this.familiarItem;
					this.specWeight = this.familiarItemWeight;
				}

				if ( itemId == FamiliarData.PUMPKIN_BUCKET.getItemId() )
				{
					this.pumpkinBucket = true;
					this.item = FamiliarData.PUMPKIN_BUCKET;
				}

				if ( itemId == FamiliarData.FLOWER_BOUQUET.getItemId() )
				{
					this.flowerBouquet = true;
					this.item = FamiliarData.FLOWER_BOUQUET;
				}

				if ( itemId == FamiliarData.FIREWORKS.getItemId() )
				{
					this.boxFireworks = true;
					this.item = FamiliarData.FIREWORKS;
				}

				if ( itemId == FamiliarData.SUGAR_SHIELD.getItemId() )
				{
					this.sugarShield = true;
					this.item = FamiliarData.SUGAR_SHIELD;
				}

				if ( itemId == FamiliarData.LEAD_NECKLACE.getItemId() )
				{
					this.leadNecklace = true;
					this.item = FamiliarData.LEAD_NECKLACE;
				}

				if ( itemId == FamiliarData.RAT_HEAD_BALLOON.getItemId() )
				{
					this.ratHeadBalloon = true;
					this.item = FamiliarData.RAT_HEAD_BALLOON;
				}

				if ( itemId == ItemPool.BATHYSPHERE )
				{
					this.bathysphere = true;
					this.item = FamiliarData.BATHYSPHERE;
				}

				if ( itemId == ItemPool.DAS_BOOT )
				{
					this.dasBoot = true;
					this.item = FamiliarData.DAS_BOOT;
				}

				if ( itemId == FamiliarData.DOPPELGANGER.getItemId() )
				{
					this.doppelganger = true;
					this.item = FamiliarData.DOPPELGANGER;
				}
			}

			// Check accessories for tininess and plasticity

			this.checkAccessory( 0, acc1 );
			this.checkAccessory( 1, acc2 );
			this.checkAccessory( 2, acc3 );
		}

		private void checkAccessory( final int index, final AdventureResult accessory )
		{
			if ( this.isTinyPlasticItem( accessory ) )
			{
				this.acc[ index ] = accessory;
				this.tp[ this.tpCount++ ] = accessory;
			}
		}

		public boolean isTinyPlasticItem( final AdventureResult ar )
		{
			if ( ar == null )
			{
				return false;
			}

			int id = ar.getItemId();
			for ( int i = 0; i < FamiliarTrainingFrame.tinyPlasticNormal.length; ++i )
			{
				if ( id == FamiliarTrainingFrame.tinyPlasticNormal[ i ] )
				{
					return true;
				}
			}

			for ( int i = 0; i < FamiliarTrainingFrame.tinyPlasticCrimbo.length; ++i )
			{
				if ( id == FamiliarTrainingFrame.tinyPlasticCrimbo[ i ] )
				{
					return true;
				}
			}

			return false;
		}

		private void checkAvailableEquipment( final LockableListModel inventory )
		{
			// If not wearing a pith helmet, search inventory
			this.pithHelmet |=
				FamiliarTrainingFrame.PITH_HELMET.getCount( inventory ) > 0 && EquipmentManager.canEquip( "plexiglass pith helmet" );

			// If not wearing a crumpled fedora helmet, search inventory
			this.crumpledFedora |=
				FamiliarTrainingFrame.CRUMPLED_FEDORA.getCount( inventory ) > 0 && EquipmentManager.canEquip( "crumpled felt fedora" );

			// If current familiar item is not the special item and
			// such an item affects weight, search inventory
			if ( this.familiarItem != this.item && this.familiarItemWeight != 0 && this.familiarItem.getCount( inventory ) > 0 )
			{
				this.specItem = this.familiarItem;
				this.specWeight = this.familiarItemWeight;
			}

			if ( !KoLCharacter.isHardcore() )
			{
				// If current familiar is not wearing a pumpkin bucket,
				// search inventory
				this.pumpkinBucket |= FamiliarData.PUMPKIN_BUCKET.getCount( inventory ) > 0;

				// If current familiar is not wearing a Mayflower bouquet,
				// search inventory
				this.flowerBouquet |= FamiliarData.FLOWER_BOUQUET.getCount( inventory ) > 0;

				// If current familiar is not wearing a box of fireworks,
				// search inventory
				this.boxFireworks |= FamiliarData.FIREWORKS.getCount( inventory ) > 0;
			}

			// If current familiar is not wearing a sugar shield,
			// search inventory
			this.sugarShield |= FamiliarData.SUGAR_SHIELD.getCount( inventory ) > 0;

			// If current familiar is not wearing a lead necklace,
			// search inventory
			this.leadNecklace |= FamiliarData.LEAD_NECKLACE.getCount( inventory ) > 0;

			// If current familiar is not wearing a rat head
			// balloon, search inventory
			this.ratHeadBalloon |= FamiliarData.RAT_HEAD_BALLOON.getCount( inventory ) > 0;

			// If current familiar is not wearing a bathysphere,
			// search inventory
			this.bathysphere |= FamiliarData.BATHYSPHERE.getCount( inventory ) > 0;

			// If current familiar is not wearing das boot
			// search inventory
			this.dasBoot |= FamiliarData.DAS_BOOT.getCount( inventory ) > 0;

			// If current familiar is not wearing a doppel,
			// search inventory
			this.doppelganger |= FamiliarData.DOPPELGANGER.getCount( inventory ) > 0;

			this.whipCount =
				Math.min(
					KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) ? 2 : 1,
					this.whipCount + FamiliarTrainingFrame.BAR_WHIP.getCount( inventory ) );

			// If equipped with fewer than three tiny plastic items
			// equipped, search inventory for more
			for ( int i = 0; i < FamiliarTrainingFrame.tinyPlasticNormal.length; ++i )
			{
				this.addTinyPlastic( FamiliarTrainingFrame.tinyPlasticNormal[ i ] );
			}

			// Check Tiny Plastic Crimbo objects
			for ( int i = 0; i < FamiliarTrainingFrame.tinyPlasticCrimbo.length; ++i )
			{
				this.addTinyPlastic( FamiliarTrainingFrame.tinyPlasticCrimbo[ i ] );
			}

			// If we're not training a chameleon and we don't have
			// a lead necklace or a rat head balloon, search other
			// familiars; we'll steal it from them if necessary

			if ( this.familiar.getId() == FamiliarPool.CHAMELEON ||
			     this.leadNecklace && this.ratHeadBalloon &&
			     this.pumpkinBucket && this.flowerBouquet &&
			     this.boxFireworks && this.sugarShield &&
			     this.bathysphere && this.dasBoot )
			{
				return;
			}

			// Find first familiar with item

			LockableListModel familiars = KoLCharacter.getFamiliarList();
			for ( int i = 0; i < familiars.size(); ++i )
			{
				FamiliarData familiar = (FamiliarData) familiars.get( i );
				AdventureResult item = familiar.getItem();

				if ( item == null )
				{
					continue;
				}

				this.leadNecklace |= item.getItemId() == FamiliarData.LEAD_NECKLACE.getItemId();
				this.ratHeadBalloon |= item.getItemId() == FamiliarData.RAT_HEAD_BALLOON.getItemId();
				this.bathysphere |= item.getItemId() == ItemPool.BATHYSPHERE;
				this.dasBoot |= item.getItemId() == ItemPool.DAS_BOOT;
				this.pumpkinBucket |= item.getItemId() == FamiliarData.PUMPKIN_BUCKET.getItemId();
				this.flowerBouquet |= item.getItemId() == FamiliarData.FLOWER_BOUQUET.getItemId();
				this.doppelganger |= item.getItemId() == FamiliarData.DOPPELGANGER.getItemId();
				this.boxFireworks |= item.getItemId() == FamiliarData.FIREWORKS.getItemId();
				this.sugarShield |= item.getItemId() == FamiliarData.SUGAR_SHIELD.getItemId();
			}
		}

		private void addTinyPlastic( final int id )
		{
			if ( this.tpCount == 3 )
			{
				return;
			}

			AdventureResult ar = new AdventureResult( id, 1 );
			int count = ar.getCount( KoLConstants.inventory );

			// Make a new one for each slot
			while ( count-- > 0 && this.tpCount < 3 )
			{
				this.tp[ this.tpCount++ ] = new AdventureResult( id, 1 );
			}
		}

		/** *********************************************************** */

		public int[] getWeights()
		{
			// Clear the set of possible weights
			this.weights.clear();

			// Calculate base weight
			int weight = this.familiar.getWeight();

			// Sympathy adds 5 lbs. except to a dodecapede, for
			// which it subtracts 5 lbs.
			if ( FamiliarTrainingFrame.sympathyAvailable )
			{
				weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
			}
			if ( FamiliarTrainingFrame.empathyActive > 0 )
			{
				weight += 5;
			}
			if ( FamiliarTrainingFrame.leashActive > 0 )
			{
				weight += 5;
			}

			// One snowcone effect at a time
			if ( FamiliarTrainingFrame.greenTongueActive > 0 || FamiliarTrainingFrame.blackTongueActive > 0 )
			{
				weight += 5;
			}

			if ( FamiliarTrainingFrame.bestialActive > 0 )
			{
				weight += 3;
			}

			if ( FamiliarTrainingFrame.greenGlowActive > 0 )
			{
				weight += 10;
			}

			if ( FamiliarTrainingFrame.greenHeartActive > 0 )
			{
				weight += 3;
			}

			if ( FamiliarTrainingFrame.heavyPettingActive > 0 )
			{
				weight += 5;
			}

			if ( FamiliarTrainingFrame.worstEnemyActive > 0 )
			{
				weight += 5;
			}

			this.getItemWeights( weight );

			// Make an array to hold values
			Object[] vals = this.weights.toArray();
			int[] value = new int[ vals.length ];

			// Read Integers from the set and store ints
			for ( int i = 0; i < vals.length; ++i )
			{
				value[ i ] = ( (Integer) vals[ i ] ).intValue();
			}

			return value;
		}

		private void getItemWeights( final int weight )
		{
			// Get current familiar
			FamiliarData familiar = KoLCharacter.getFamiliar();

			// Calculate Accessory Weights with no Familiar Items
			this.getAccessoryWeights( weight );

			// Only consider familiar items if current familiar is
			// not a chameleon and you have no doppelganger
			if ( familiar.getId() == FamiliarPool.CHAMELEON || this.doppelganger )
			{
				return;
			}

			// If familiar specific item adds weight, calculate
			if ( this.specWeight != 0 )
			{
				this.getAccessoryWeights( weight + this.specWeight );
			}

			// If we have a sugar shield, use it
			if ( this.sugarShield )
			{
				this.getAccessoryWeights( weight + 10 );
			}

			// If we have a pumpkin bucket, use it
			if ( this.pumpkinBucket )
			{
				this.getAccessoryWeights( weight + 5 );
			}

			// If we have a Mayflower bouquet, use it
			if ( this.flowerBouquet )
			{
				this.getAccessoryWeights( weight + 5 );
			}

			// If we have a little box of fireworks, use it
			if ( this.boxFireworks )
			{
				this.getAccessoryWeights( weight + 5 );
			}

			// If we have a lead necklace, use it
			if ( this.leadNecklace )
			{
				this.getAccessoryWeights( weight + 3 );
			}

			// If we have a rat head balloon, use it
			if ( this.ratHeadBalloon )
			{
				this.getAccessoryWeights( weight - 3 );
			}

			// If we have das boot, use it
			if ( this.dasBoot )
			{
				this.getAccessoryWeights( weight - 10 );
			}

			// If we have a bathysphere, use it
			if ( this.bathysphere )
			{
				this.getAccessoryWeights( weight - 20 );
			}
		}

		private void getAccessoryWeights( final int weight )
		{
			// Calculate using variable #s of tiny plastic objects
			for ( int i = 0; i <= this.tpCount; ++i )
			{
				this.getWhipWeights( weight + i );
			}
		}

		private void getWhipWeights( final int weight )
		{
			// Calculate using variable #s of whips
			for ( int i = 0; i <= this.whipCount; ++i )
			{
				this.getHatWeights( weight + i * 2 );
			}
		}

		private void getHatWeights( final int weight )
		{
			// Add weight with helmet
			if ( this.pithHelmet )
			{
				this.weights.add( IntegerPool.get( Math.max( weight + 5, 1 ) ) );
			}

			// Add weight with fedora
			if ( this.crumpledFedora )
			{
				this.weights.add( IntegerPool.get( Math.max( weight + 10, 1 ) ) );
			}

			// Add weight with no helmet
			this.weights.add( IntegerPool.get( Math.max( weight, 1 ) ) );
		}

		/** *********************************************************** */

		/*
		 * Change gear and cast buffs such that your familiar's modified weight is as specified.
		 */
		public void changeGear( final int weight )
		{
			// Make a GearSet describing what we have now
			GearSet current = new GearSet();

			if ( this.doppelganger )
			{
				RequestThread.postRequest( new EquipmentRequest(
					FamiliarData.DOPPELGANGER, EquipmentManager.FAMILIAR ) );
			}

			// If we are already suitably equipped, stop now
			if ( weight == current.weight() )
			{
				return;
			}

			// Choose a new GearSet with desired weight
			GearSet next = this.chooseGearSet( current, weight );

			// If we couldn't pick one, that's an internal error
			if ( next == null || weight < next.weight() )
			{
				FamiliarTrainingFrame.statusMessage(
					MafiaState.ERROR, "Could not select gear set to achieve " + weight + " lbs." );

				if ( next == null )
				{
					FamiliarTrainingFrame.results.append( "No gear set found.<br>" );
				}
				else
				{
					FamiliarTrainingFrame.results.append( "Selected gear set provides " + next.weight() + " lbs.<br>" );
				}

				return;
			}

			// Change into the new GearSet
			this.changeGear( current, next );
		}

		/*
		 * Debug: choose and print desired GearSet
		 */
		public void chooseGear( final int weight )
		{
			// Make a GearSet describing what we have now
			GearSet current = new GearSet();

			// If we are already suitably equipped, stop now
			if ( weight == current.weight() )
			{
				FamiliarTrainingFrame.results.append( "Current gear is acceptable/<br>" );
				return;
			}

			// Choose a new GearSet with desired weight
			GearSet next = this.chooseGearSet( current, weight );

			if ( next == null )
			{
				FamiliarTrainingFrame.results.append( "Could not find a gear set to achieve " + weight + " lbs.<br>" );
				return;
			}

			FamiliarTrainingFrame.results.append( "Chosen gear set: " + next + " provides " + next.weight() + " lbs.<br>" );
		}

		/*
		 * Swap gear and cast buffs to match desired GearSet. Return false if failed to swap or buff
		 */

		public void changeGear( final GearSet current, final GearSet next )
		{
			this.swapItem( current.weapon, next.weapon, EquipmentManager.WEAPON );
			this.swapItem( current.offhand, next.offhand, EquipmentManager.OFFHAND );
			this.swapItem( current.hat, next.hat, EquipmentManager.HAT );
			this.swapItem( current.item, next.item, EquipmentManager.FAMILIAR );
			this.swapItem( current.acc1, next.acc1, EquipmentManager.ACCESSORY1 );
			this.swapItem( current.acc2, next.acc2, EquipmentManager.ACCESSORY2 );
			this.swapItem( current.acc3, next.acc3, EquipmentManager.ACCESSORY3 );
		}

		private void swapItem( final AdventureResult current, final AdventureResult next, final int slot )
		{
			// Nothing to do if already wearing this item
			if ( current == next )
			{
				return;
			}

			// EquipmentRequest will notice if something else is in
			// the slot and will remove it first, if necessary
			//
			// Therefore, we can simply equip the new item.

			if ( next != null )
			{
				FamiliarTrainingFrame.results.append( "Putting on " + next.getName() + "<br>" );
				RequestThread.postRequest( new EquipmentRequest( next, slot ) );
				this.setItem( slot, next );
			}
			else if ( current != null )
			{
				FamiliarTrainingFrame.results.append( "Taking off " + current.getName() + "<br>" );
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, slot ) );
				this.setItem( slot, null );
			}
		}

		private void setItem( final int slot, final AdventureResult item )
		{
			switch ( slot )
			{
			case EquipmentManager.WEAPON:
				this.weapon = item;
				break;
			case EquipmentManager.OFFHAND:
				this.offhand = item;
				break;
			case EquipmentManager.HAT:
				this.hat = item;
				break;
			case EquipmentManager.FAMILIAR:
				this.item = item;
				break;
			case EquipmentManager.ACCESSORY1:
				this.acc[ 0 ] = item;
				break;
			case EquipmentManager.ACCESSORY2:
				this.acc[ 1 ] = item;
				break;
			case EquipmentManager.ACCESSORY3:
				this.acc[ 2 ] = item;
				break;
			}
		}

		/*
		 * Choose a GearSet that gives the current familiar the desired weight. Of the (potentially many) possible such
		 * sets, return the one that requires the smallest number of changes from what is currently in effect.
		 */
		private GearSet chooseGearSet( final GearSet current, final int weight )
		{
			// Clear out the accumulated list of GearSets

			this.gearSets.clear();
			this.getHatGearSets( weight );

			// Iterate over all the GearSets and choose the first
			// one which is closest to the current GearSet

			Collections.sort( this.gearSets );
			return this.gearSets.isEmpty() ? null : (GearSet) this.gearSets.get( 0 );
		}

		private void getHatGearSets( final int weight )
		{
			if ( this.pithHelmet )
			{
				this.getItemGearSets( weight, FamiliarTrainingFrame.PITH_HELMET );
			}

			if ( this.crumpledFedora )
			{
				this.getItemGearSets( weight, FamiliarTrainingFrame.CRUMPLED_FEDORA );
			}

			this.getItemGearSets( weight, null );
		}

		private void getItemGearSets( final int weight, final AdventureResult hat )
		{
			// If it's a comma chameleon or we have a doppelganger,
			// don't look at familiar items.

			if ( KoLCharacter.getFamiliar().getId() == FamiliarPool.CHAMELEON )
			{
				this.getAccessoryGearSets( weight, null, hat );
				return;
			}

			if ( this.doppelganger )
			{
				this.getAccessoryGearSets( weight, FamiliarData.DOPPELGANGER, hat );
				return;
			}

			if ( this.specItem != null )
			{
				this.getAccessoryGearSets( weight, this.specItem, hat );
			}
			if ( this.pumpkinBucket )
			{
				this.getAccessoryGearSets( weight, FamiliarData.PUMPKIN_BUCKET, hat );
			}
			if ( this.flowerBouquet )
			{
				this.getAccessoryGearSets( weight, FamiliarData.FLOWER_BOUQUET, hat );
			}
			if ( this.boxFireworks )
			{
				this.getAccessoryGearSets( weight, FamiliarData.FIREWORKS, hat );
			}
			if ( this.sugarShield )
			{
				this.getAccessoryGearSets( weight, FamiliarData.SUGAR_SHIELD, hat );
			}
			if ( this.leadNecklace )
			{
				this.getAccessoryGearSets( weight, FamiliarData.LEAD_NECKLACE, hat );
			}
			if ( this.ratHeadBalloon )
			{
				this.getAccessoryGearSets( weight, FamiliarData.RAT_HEAD_BALLOON, hat );
			}
			if ( this.bathysphere )
			{
				this.getAccessoryGearSets( weight, FamiliarData.BATHYSPHERE, hat );
			}
			if ( this.dasBoot )
			{
				this.getAccessoryGearSets( weight, FamiliarData.DAS_BOOT, hat );
			}

			this.getAccessoryGearSets( weight, null, hat );
		}

		private void getAccessoryGearSets( final int weight, final AdventureResult item, final AdventureResult hat )
		{
			// No matter how many Tiny Plastic Objects we have, a
			// configuration with none equipped is legal
			this.addGearSet( weight, null, null, null, item, hat );
			if ( this.tpCount == 0 )
			{
				return;
			}

			// If we have at least one and it started out equipped,
			// then it might be in any of the three accessory
			// slots.
			this.addGearSet( weight, this.tp[ 0 ], null, null, item, hat );
			this.addGearSet( weight, null, this.tp[ 0 ], null, item, hat );
			this.addGearSet( weight, null, null, this.tp[ 0 ], item, hat );
			if ( this.tpCount == 1 )
			{
				return;
			}

			// If we have at least two and they were both equipped
			// when we came in, they'll be in one of the following
			// patterns.

			this.addGearSet( weight, this.tp[ 0 ], this.tp[ 1 ], null, item, hat );
			this.addGearSet( weight, this.tp[ 0 ], null, this.tp[ 1 ], item, hat );
			this.addGearSet( weight, null, this.tp[ 0 ], this.tp[ 1 ], item, hat );

			// If one of the two was in the inventory, the first
			// could have been in any of the three accessory
			// slots. Add a pattern for where it was in the third
			// slot.

			this.addGearSet( weight, this.tp[ 1 ], null, this.tp[ 0 ], item, hat );

			if ( this.tpCount == 2 )
			{
				return;
			}

			// If we have three and they were all equipped when we
			// came in, they'll be in the following pattern
			this.addGearSet( weight, this.tp[ 0 ], this.tp[ 1 ], this.tp[ 2 ], item, hat );

			// If two of them were equipped and the third was in
			// the inventory, the following patterns are legal
			this.addGearSet( weight, this.tp[ 0 ], this.tp[ 2 ], this.tp[ 1 ], item, hat );
			this.addGearSet( weight, this.tp[ 2 ], this.tp[ 0 ], this.tp[ 1 ], item, hat );

			// If only one was equipped, based on which
			// two-accessory patterns are legal, the following
			// three-accessory patterns are also legal
			this.addGearSet( weight, this.tp[ 1 ], this.tp[ 2 ], this.tp[ 0 ], item, hat );
		}

		private void addGearSet( final int weight, final AdventureResult acc1, final AdventureResult acc2,
			final AdventureResult acc3, final AdventureResult item, final AdventureResult hat )
		{
			int gearWeight = this.gearSetWeight( null, null, acc1, acc2, acc3, item, hat );
			if ( weight == gearWeight )
			{
				this.gearSets.add( new GearSet( null, null, acc1, acc2, acc3, item, hat ) );
			}

			if ( this.whipCount > 0 )
			{
				gearWeight = this.gearSetWeight( FamiliarTrainingFrame.BAR_WHIP, null, acc1, acc2, acc3, item, hat );
				if ( weight == gearWeight )
				{
					this.gearSets.add( new GearSet( FamiliarTrainingFrame.BAR_WHIP, null, acc1, acc2, acc3, item, hat ) );
				}
			}

			if ( this.whipCount > 1 )
			{
				gearWeight =
					this.gearSetWeight(
						FamiliarTrainingFrame.BAR_WHIP, FamiliarTrainingFrame.BAR_WHIP, acc1, acc2, acc3, item, hat );
				if ( weight == gearWeight )
				{
					this.gearSets.add( new GearSet(
						FamiliarTrainingFrame.BAR_WHIP, FamiliarTrainingFrame.BAR_WHIP, acc1, acc2, acc3, item, hat ) );
				}
			}
		}

		private int gearSetWeight( final AdventureResult weapon, final AdventureResult offhand,
			final AdventureResult acc1, final AdventureResult acc2, final AdventureResult acc3,
			final AdventureResult item, final AdventureResult hat )
		{
			int weight = this.familiar.getWeight();

			if ( hat == FamiliarTrainingFrame.CRUMPLED_FEDORA )
			{
				weight += 10;
			}
			else if ( hat == FamiliarTrainingFrame.PITH_HELMET )
			{
				weight += 5;
			}

			if ( FamiliarTrainingFrame.sympathyAvailable )
			{
				weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
			}

			if ( FamiliarTrainingFrame.leashActive > 0 )
			{
				weight += 5;
			}
			if ( FamiliarTrainingFrame.empathyActive > 0 )
			{
				weight += 5;
			}
			if ( FamiliarTrainingFrame.bestialActive > 0 )
			{
				weight += 3;
			}
			if ( FamiliarTrainingFrame.greenGlowActive > 0 )
			{
				weight += 10;
			}
			if ( FamiliarTrainingFrame.greenHeartActive > 0 )
			{
				weight += 3;
			}
			if ( FamiliarTrainingFrame.greenTongueActive > 0 || FamiliarTrainingFrame.blackTongueActive > 0 )
			{
				weight += 5;
			}
			if ( FamiliarTrainingFrame.heavyPettingActive > 0 )
			{
				weight += 5;
			}
			if ( FamiliarTrainingFrame.worstEnemyActive > 0 )
			{
				weight += 5;
			}

			if ( item == FamiliarData.DOPPELGANGER )
			{
				;
			}
			else if ( item == this.specItem )
			{
				weight += this.specWeight;
			}
			else if ( item == FamiliarData.SUGAR_SHIELD )
			{
				weight += 10;
			}
			else if ( item == FamiliarData.PUMPKIN_BUCKET )
			{
				weight += 5;
			}
			else if ( item == FamiliarData.FLOWER_BOUQUET )
			{
				weight += 5;
			}
			else if ( item == FamiliarData.FIREWORKS )
			{
				weight += 5;
			}
			else if ( item == FamiliarData.LEAD_NECKLACE )
			{
				weight += 3;
			}
			else if ( item == FamiliarData.RAT_HEAD_BALLOON )
			{
				weight -= 3;
			}
			else if ( item == FamiliarData.BATHYSPHERE )
			{
				weight -= 20;
			}
			else if ( item == FamiliarData.DAS_BOOT )
			{
				weight -= 10;
			}

			if ( weapon == FamiliarTrainingFrame.BAR_WHIP )
			{
				weight += 2;
			}
			if ( offhand == FamiliarTrainingFrame.BAR_WHIP )
			{
				weight += 2;
			}

			if ( this.isTinyPlasticItem( acc1 ) )
			{
				weight += 1;
			}
			if ( this.isTinyPlasticItem( acc2 ) )
			{
				weight += 1;
			}
			if ( this.isTinyPlasticItem( acc3 ) )
			{
				weight += 1;
			}

			return Math.max( weight, 1 );
		}

		/** *********************************************************** */

		public void processMatchResult( final String response, final int xp )
		{
			// If the contest did not take place, bail now
			if ( response.indexOf( "You enter" ) == -1 )
			{
				return;
			}

			// Find and report how much experience was gained
			String message;
			if ( xp > 0 )
			{
				message =
					this.familiar.getName() + " gains " + xp + " experience" + ( response.indexOf( "gains a pound" ) != -1 ? " and a pound." : "." );
			}
			else
			{
				message = this.familiar.getName() + " lost.";
			}

			FamiliarTrainingFrame.statusMessage( MafiaState.CONTINUE, message );

			// If a prize was won, report it
			Matcher prizeMatcher = FamiliarTrainingFrame.PRIZE_PATTERN.matcher( response );
			Matcher stealMatcher = FamiliarTrainingFrame.STEAL_PATTERN.matcher( response );
			String prize = null;

			if ( prizeMatcher.find() )
			{
				prize = prizeMatcher.group( 1 );
				FamiliarTrainingFrame.statusMessage( MafiaState.CONTINUE, "You win a prize: " + prize + "." );
			}
			else if ( stealMatcher.find() )
			{
				prize = stealMatcher.group( 1 );
				FamiliarTrainingFrame.statusMessage(
					MafiaState.CONTINUE, "Your familiar steals an item: " + prize + "." );
			}

			if ( prize != null )
			{
				if ( prize.equals( FamiliarData.LEAD_NECKLACE.getName() ) )
				{
					this.leadNecklace = true;
				}
				else if ( this.familiarItemWeight > 0 )
				{
					if ( this.specItem == null )
					{
						this.specItem = this.familiarItem;
						this.specWeight = this.familiarItemWeight;
					}
				}
			}

			// Increment count of turns in this training session
			this.turns++ ;

			// Decrement buffs
			if ( FamiliarTrainingFrame.leashActive > 0 )
			{
				FamiliarTrainingFrame.leashActive-- ;
			}
			if ( FamiliarTrainingFrame.empathyActive > 0 )
			{
				FamiliarTrainingFrame.empathyActive-- ;
			}
			if ( FamiliarTrainingFrame.bestialActive > 0 )
			{
				FamiliarTrainingFrame.bestialActive-- ;
			}
			if ( FamiliarTrainingFrame.blackTongueActive > 0 )
			{
				FamiliarTrainingFrame.blackTongueActive-- ;
			}
			if ( FamiliarTrainingFrame.greenGlowActive > 0 )
			{
				FamiliarTrainingFrame.greenHeartActive-- ;
			}
			if ( FamiliarTrainingFrame.greenHeartActive > 0 )
			{
				FamiliarTrainingFrame.greenHeartActive-- ;
			}
			if ( FamiliarTrainingFrame.greenTongueActive > 0 )
			{
				FamiliarTrainingFrame.greenTongueActive-- ;
			}
			if ( FamiliarTrainingFrame.heavyPettingActive > 0 )
			{
				FamiliarTrainingFrame.heavyPettingActive-- ;
			}
			if ( FamiliarTrainingFrame.worstEnemyActive > 0 )
			{
				FamiliarTrainingFrame.worstEnemyActive-- ;
			}
		}

		public FamiliarData getFamiliar()
		{
			return this.familiar;
		}

		public int turnsUsed()
		{
			return this.turns;
		}

		public int baseWeight()
		{
			return this.familiar.getWeight();
		}

		public int maxWeight( final boolean buffs )
		{
			// Start with current base weight of familiar
			int weight = this.familiar.getWeight();

			// Add possible skills
			if ( FamiliarTrainingFrame.sympathyAvailable )
			{
				weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
			}

			if ( buffs )
			{
				if ( FamiliarTrainingFrame.leashAvailable || FamiliarTrainingFrame.leashActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.empathyAvailable || FamiliarTrainingFrame.empathyActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.bestialAvailable || FamiliarTrainingFrame.bestialActive > 0 )
				{
					weight += 3;
				}
				if ( FamiliarTrainingFrame.greenConeAvailable || FamiliarTrainingFrame.greenTongueActive > 0 || FamiliarTrainingFrame.blackConeAvailable || FamiliarTrainingFrame.blackTongueActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.greenGlowActive > 0 )
				{
					weight += 10;
				}
				if ( FamiliarTrainingFrame.greenHeartAvailable || FamiliarTrainingFrame.greenHeartActive > 0 )
				{
					weight += 3;
				}
				if ( FamiliarTrainingFrame.heavyPettingAvailable || FamiliarTrainingFrame.heavyPettingActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.worstEnemyAvailable || FamiliarTrainingFrame.worstEnemyActive > 0 )
				{
					weight += 5;
				}
			}
			else
			{
				if ( FamiliarTrainingFrame.leashActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.empathyActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.bestialActive > 0 )
				{
					weight += 3;
				}
				if ( FamiliarTrainingFrame.greenGlowActive > 0 )
				{
					weight += 10;
				}
				if ( FamiliarTrainingFrame.greenHeartActive > 0 )
				{
					weight += 3;
				}
				if ( FamiliarTrainingFrame.greenTongueActive > 0 || FamiliarTrainingFrame.blackTongueActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.heavyPettingActive > 0 )
				{
					weight += 5;
				}
				if ( FamiliarTrainingFrame.worstEnemyActive > 0 )
				{
					weight += 5;
				}
			}

			// Add available familiar items
			if ( this.pumpkinBucket )
			{
				weight += 5;
			}
			else if ( this.flowerBouquet )
			{
				weight += 5;
			}
			else if ( this.boxFireworks )
			{
				weight += 5;
			}
			else if ( this.sugarShield )
			{
				weight += 10;
			}
			else if ( this.specWeight > 3 )
			{
				weight += this.specWeight;
			}
			else if ( this.leadNecklace )
			{
				weight += 3;
			}

			// Add available tiny plastic items
			weight += this.tpCount;
			weight += 2 * this.whipCount;

			// Add crumpled fedora
			if ( this.crumpledFedora )
			{
				weight += 10;
			}
			else if ( this.pithHelmet )
			{
				weight += 5;
			}

			return Math.max( weight, 1 );
		}

		public String printAvailableBuffs()
		{
			StringBuilder text = new StringBuilder();

			text.append( "Castable buffs:" );
			if ( FamiliarTrainingFrame.empathyAvailable )
			{
				text.append( " Empathy (+5)" );
			}
			if ( FamiliarTrainingFrame.leashAvailable )
			{
				text.append( " Leash (+5)" );
			}
			if ( !FamiliarTrainingFrame.empathyAvailable && !FamiliarTrainingFrame.leashAvailable )
			{
				text.append( " None" );
			}
			text.append( "<br>" );

			return text.toString();
		}

		public String printCurrentBuffs()
		{
			StringBuilder text = new StringBuilder();

			text.append( "Current buffs:" );
			if ( FamiliarTrainingFrame.sympathyAvailable )
			{
				text.append( " Sympathy (" + ( this.familiar.getId() == FamiliarPool.DODECAPEDE ? "-" : "+" ) + "5 permanent)" );
			}
			if ( FamiliarTrainingFrame.empathyActive > 0 )
			{
				text.append( " Empathy (+5 for " + FamiliarTrainingFrame.empathyActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.leashActive > 0 )
			{
				text.append( " Leash (+5 for " + FamiliarTrainingFrame.leashActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.bestialActive > 0 )
			{
				text.append( " Bestial Sympathy (+3 for " + FamiliarTrainingFrame.bestialActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.blackTongueActive > 0 )
			{
				text.append( " Black Tongue (+5 for " + FamiliarTrainingFrame.blackTongueActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.greenGlowActive > 0 )
			{
				text.append( " Healthy Green Glow (+10 for " + FamiliarTrainingFrame.greenGlowActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.greenHeartActive > 0 )
			{
				text.append( " Heart of Green (+3 for " + FamiliarTrainingFrame.greenHeartActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.greenTongueActive > 0 )
			{
				text.append( " Green Tongue (+5 for " + FamiliarTrainingFrame.greenTongueActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.heavyPettingActive > 0 )
			{
				text.append( " Heavy Petting (+5 for " + FamiliarTrainingFrame.heavyPettingActive + " turns)" );
			}
			if ( FamiliarTrainingFrame.worstEnemyActive > 0 )
			{
				text.append( " Man's Worst Enemy (+5 for " + FamiliarTrainingFrame.worstEnemyActive + " turns)" );
			}

			if ( !FamiliarTrainingFrame.sympathyAvailable && FamiliarTrainingFrame.empathyActive == 0 && FamiliarTrainingFrame.leashActive == 0 && FamiliarTrainingFrame.bestialActive == 0 && FamiliarTrainingFrame.blackTongueActive == 0 && FamiliarTrainingFrame.greenGlowActive == 0 && FamiliarTrainingFrame.greenHeartActive == 0 && FamiliarTrainingFrame.greenTongueActive == 0 && FamiliarTrainingFrame.heavyPettingActive == 0 && FamiliarTrainingFrame.worstEnemyActive == 0 )
			{
				text.append( " None" );
			}

			text.append( "<br>" );

			return text.toString();
		}

		public String printCurrentEquipment()
		{
			StringBuilder text = new StringBuilder();

			text.append( "Current equipment:" );

			if ( this.weapon == FamiliarTrainingFrame.BAR_WHIP )
			{
				text.append( " bar whip (+2)" );
			}
			if ( this.offhand == FamiliarTrainingFrame.BAR_WHIP )
			{
				text.append( " bar whip (+2)" );
			}

			if ( this.hat == FamiliarTrainingFrame.CRUMPLED_FEDORA )
			{
				text.append( " crumpled felt fedora (+10)" );
			}

			if ( this.hat == FamiliarTrainingFrame.PITH_HELMET )
			{
				text.append( " plexiglass pith helmet (+5)" );
			}

			if ( this.item == FamiliarData.DOPPELGANGER )
			{
				text.append( " " + FamiliarData.DOPPELGANGER.getName() + " (+0)" );
			}
			else if ( this.item == FamiliarData.PUMPKIN_BUCKET )
			{
				text.append( " " + FamiliarData.PUMPKIN_BUCKET.getName() + " (+5)" );
			}
			else if ( this.item == FamiliarData.FLOWER_BOUQUET )
			{
				text.append( " " + FamiliarData.FLOWER_BOUQUET.getName() + " (+5)" );
			}
			else if ( this.item == FamiliarData.FIREWORKS )
			{
				text.append( " " + FamiliarData.FIREWORKS.getName() + " (+5)" );
			}
			else if ( this.item == FamiliarData.SUGAR_SHIELD )
			{
				text.append( " " + FamiliarData.SUGAR_SHIELD.getName() + " (+10)" );
			}
			else if ( this.item == FamiliarData.LEAD_NECKLACE )
			{
				text.append( " " + FamiliarData.LEAD_NECKLACE.getName() + " (+3)" );
			}
			else if ( this.item == FamiliarData.RAT_HEAD_BALLOON )
			{
				text.append( " " + FamiliarData.RAT_HEAD_BALLOON.getName() + " (-3)" );
			}
			else if ( this.item == FamiliarData.BATHYSPHERE )
			{
				text.append( " " + FamiliarData.BATHYSPHERE.getName() + " (-20)" );
			}
			else if ( this.item == FamiliarData.DAS_BOOT )
			{
				text.append( " " + FamiliarData.DAS_BOOT.getName() + " (-10)" );
			}
			else if ( this.item != null )
			{
				text.append( " " + this.specItem.getName() + " (+" + this.specWeight + ")" );
			}

			for ( int i = 0; i < 3; ++i )
			{
				if ( this.acc[ i ] != null )
				{
					text.append( " " + this.acc[ i ].getName() + " (+1)" );
				}
			}
			text.append( "<br>" );

			return text.toString();
		}

		public String printAvailableEquipment()
		{
			StringBuilder text = new StringBuilder();

			text.append( "Available equipment:" );

			for ( int i = 0; i < this.whipCount; ++i )
			{
				text.append( " bar whip (+2)" );
			}

			if ( this.hat == FamiliarTrainingFrame.CRUMPLED_FEDORA )
			{
				text.append( " crumpled felt fedora (+10)" );
			}

			if ( this.hat == FamiliarTrainingFrame.PITH_HELMET )
			{
				text.append( " plexiglass pith helmet (+5)" );
			}

			if ( this.doppelganger )
			{
				text.append( " flaming familiar doppelg&auml;nger (+0)" );
			}
			else
			{
				if ( this.crumpledFedora )
				{
					text.append( " crumpled felt fedora (+10)" );
				}
				if ( this.pithHelmet )
				{
					text.append( " plexiglass pith helmet (+5)" );
				}
				if ( this.specItem != null )
				{
					text.append( " " + this.specItem.getName() + " (+" + this.specWeight + ")" );
				}
				if ( this.sugarShield )
				{
					text.append( " sugar shield (+10)" );
				}
				if ( this.pumpkinBucket )
				{
					text.append( " plastic pumpkin bucket (+5)" );
				}
				if ( this.flowerBouquet )
				{
					text.append( " mayflower bouquet (+5)" );
				}
				if ( this.boxFireworks )
				{
					text.append( " little box of fireworks (+5)" );
				}
				if ( this.leadNecklace )
				{
					text.append( " lead necklace (+3)" );
				}
				if ( this.ratHeadBalloon )
				{
					text.append( " rat head balloon (-3)" );
				}
				if ( this.bathysphere )
				{
					text.append( " little bitty bathysphere (-20)" );
				}
				if ( this.dasBoot )
				{
					text.append( " das boot (-10)" );
				}
			}

			for ( int i = 0; i < this.tpCount; ++i )
			{
				text.append( " " + this.tp[ i ].getName() + " (+1)" );
			}

			text.append( "<br>" );

			return text.toString();
		}

		private class GearSet
			implements Comparable<GearSet>
		{
			public AdventureResult weapon;
			public AdventureResult offhand;
			public AdventureResult hat;
			public AdventureResult item;
			public AdventureResult acc1;
			public AdventureResult acc2;
			public AdventureResult acc3;

			public GearSet()
			{
				this(
					FamiliarStatus.this.weapon, FamiliarStatus.this.offhand, FamiliarStatus.this.acc[ 0 ],
					FamiliarStatus.this.acc[ 1 ], FamiliarStatus.this.acc[ 2 ], FamiliarStatus.this.item,
					FamiliarStatus.this.hat );
			}

			public GearSet( final AdventureResult weapon, final AdventureResult offhand, final AdventureResult acc1,
				final AdventureResult acc2, final AdventureResult acc3, final AdventureResult item,
				final AdventureResult hat )
			{
				this.weapon = weapon;
				this.offhand = offhand;
				this.acc1 = acc1;
				this.acc2 = acc2;
				this.acc3 = acc3;
				this.item = item;
				this.hat = hat;
			}

			public int weight()
			{
				return FamiliarStatus.this.gearSetWeight(
					this.weapon, this.offhand, this.acc1, this.acc2, this.acc3, this.item, this.hat );
			}

			public int compareTo( final GearSet o )
			{
				// Keep in mind that all unequips are considered
				// better than equips, so unequips have a change
				// weight of 1.	 All others vary from that.

				GearSet that = (GearSet) o;
				int changes = 0;

				// Crumpled felt fedora is considered the
				// most ideal change, if it exists.

				if ( this.hat != that.hat )
				{
					changes += that.item == null ? 1 : 10;
				}

				// Pumpkin bucket is also ideal, lead necklace
				// is less than ideal, standard item is ideal.

				if ( this.item != that.item )
				{
					changes +=
						that.item == null ? 1 : that.item.getItemId() == FamiliarData.LEAD_NECKLACE.getItemId() ? 10 : 5;
				}

				if ( this.weapon != that.weapon )
				{
					changes += 15;
				}

				if ( this.offhand != that.offhand )
				{
					changes += 10;
				}

				// Tiny plastic accessory changes are expensive
				// because they involve frequent changes.

				if ( this.acc1 != that.acc1 )
				{
					changes += 20;
				}
				if ( this.acc2 != that.acc2 )
				{
					changes += 20;
				}
				if ( this.acc3 != that.acc3 )
				{
					changes += 20;
				}

				return changes;
			}

			@Override
			public String toString()
			{
				StringBuilder text = new StringBuilder();
				text.append( "(" );
				if ( this.weapon == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.weapon.getItemId() );
				}
				text.append( ", " );
				if ( this.offhand == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.offhand.getItemId() );
				}
				text.append( ", " );
				if ( this.hat == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.hat.getItemId() );
				}
				text.append( ", " );
				if ( this.item == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.item.getItemId() );
				}
				text.append( ", " );
				if ( this.acc1 == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.acc1.getItemId() );
				}
				text.append( ", " );
				if ( this.acc2 == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.acc2.getItemId() );
				}
				text.append( ", " );
				if ( this.acc3 == null )
				{
					text.append( "null" );
				}
				else
				{
					text.append( this.acc3.getItemId() );
				}

				text.append( ")" );
				return text.toString();
			}
		}
	}

	/**
	 * An internal class used to handle requests which resets a property for the duration of the current session.
	 */

	public class LocalSettingChanger
		extends JButton
		implements ActionListener
	{
		private final String title;
		private final String property;

		public LocalSettingChanger( final String title, final String property )
		{
			super( title );

			this.title = title;
			this.property = property;

			// Turn everything off and back on again
			// so that it's off to start.

			this.actionPerformed( null );
			this.actionPerformed( null );

			this.addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			boolean toggleValue = Preferences.getBoolean( this.property );
			Preferences.setBoolean( this.property, toggleValue );

			if ( toggleValue )
			{
				this.setText( "Turn Off " + this.title );
			}
			else
			{
				this.setText( "Turn On " + this.title );
			}
		}
	}
}
