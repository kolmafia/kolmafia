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
import java.awt.FlowLayout;

// events
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to provide access to Xylpher's familiar training tool
 */

public class FamiliarTrainingFrame extends KoLFrame
{
	private static ChatBuffer results = new ChatBuffer( "Arena Tracker" );
	private FamiliarTrainingPanel training;

	private static final String [] events =
	{
		"Ultimate Cage Match",
		"Scavenger Hunt",
		"Obstacle Course",
		"Hide and Seek"
	};

	private static final int BASE = 1;
	private static final int BUFFED = 2;
	private static final int TURNS = 3;

	// Familiar buffing skills
	private static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0, true );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0, true );

	// Familiar buffing items
	private static final AdventureResult PITH_HELMET = new AdventureResult( "plexiglass pith helmet", 0, false );
	private static final AdventureResult LEAD_NECKLACE = new AdventureResult( "lead necklace", 0, false );
	private static final AdventureResult RAT_HEAD_BALLOON = new AdventureResult( "rat head balloon", 0, false );
	private static final int firstTinyPlastic = 969;
	private static final int lastTinyPlastic = 988;

	public FamiliarTrainingFrame( KoLmafia client )
	{
		super( client, "Familiar Training Tool" );

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		JMenu optionsMenu = new JMenu( "File" );
		optionsMenu.add( new FileMenuItem() );
		menuBar.add( optionsMenu );

		training = new FamiliarTrainingPanel();
		getContentPane().add( training, "" );
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( training != null )
			training.setEnabled( isEnabled );
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
			KoLCharacter.addKoLCharacterListener( new KoLCharacterAdapter( new FamiliarRefresh() ) );
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

			public ButtonPanel()
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
				base = new JButton( "Base" );
				base.addActionListener( new BaseListener() );
				this.add( base );

				buffed = new JButton( "Buffed" );
				buffed.addActionListener( new BuffedListener() );
				this.add( buffed );

				turns = new JButton( "Turns" );
				turns.addActionListener( new TurnsListener() );
				this.add( turns );
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

					// Level the familiar
					levelFamiliar( client, goal, BASE );
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

					// Level the familiar
					levelFamiliar( client, goal, BUFFED );
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

					// Level the familiar
					levelFamiliar( client, goal, TURNS );
				}
			}
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

	/**
	 * Utility method to level the current familiar by fighting the
	 * current arena opponents.
	 *
	 * @param	goal	Weight goal for the familiar
	 * @param	base	true if goal is base weight, false if buffed
	 */

	public static void levelFamiliar( KoLmafia client, int goal, int type )
	{
		// Clear the output
		results.clearBuffer();

		// Get current familiar
		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			results.append( "No familiar selected to train.<br>" );
			return;
		}

		// Get the status of current familiar
		FamiliarStatus status = new FamiliarStatus( client, false );

		// Identify the familiar we are training
		String name = familiar.getName();
		String race = familiar.getRace();
		int weight = familiar.getWeight();

		results.append( "Training " + name + ", the " + weight + " lb. " + race + ".<br>" );

		if ( goalMet( status, goal, type) )
		{
			results.append( "Goal already met.<br>" );
			return;
		}

		// Print available buffs and items and current buffs

		// Choose possible weights
		int [] weights = new int[1];
		weights[0] = familiar.getModifiedWeight();

		// Get opponent list
		LockableListModel opponents = CakeArenaManager.getOpponentList( client );

		// List the opponents

		// Make a Familiar Tool
		FamiliarTool tool = new FamiliarTool( opponents );

		// Let the battles begin!
		int id = familiar.getID();
		results.append( "<br>" );

		// Select initial battle
		{
			int opp = tool.bestOpponent( id, weights );
			CakeArenaManager.ArenaOpponent opponent = (CakeArenaManager.ArenaOpponent)opponents.get( opp );

			if ( opponent == null )
			{
				results.append( "Can't determine an appropriate opponent.<br>");
				return;
			}

			int match = tool.bestMatch();
			String event = events[match];

			int famweight = tool.bestWeight();
			int diff = tool.difference();

			results.append( "Match: " + name + " (" + famweight + " lbs) vs. " + opponent.getName() + " in the " + event + "<br>" );
		}
	}

	private static boolean goalMet( FamiliarStatus status, int goal, int type )
	{
		switch ( type )
		{
		case BASE:
			results.append( "Goal: " + goal + " lbs. " + " base weight.<br>");
			if ( status.baseWeight() >= goal )
				return true;
			break;

		case BUFFED:
			results.append( "Goal: " + goal + " lbs. " + " buffed weight.<br>");
			if ( status.maxBuffedWeight() >= goal )
				return true;
			break;

		case TURNS:
			results.append( "Goal: train for " + goal + " turn" + ( ( goal > 1) ? "s<br>" : "<br>" ) );
			break;
		}

		return false;
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

		// Currently equipped gear which affects weight
		AdventureResult hat;
		AdventureResult item;
		int itemWeight;
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

		public FamiliarStatus( KoLmafia client, boolean refresh )
		{
			// If requested, refresh the character status to ensure
			// accuracy of available skills, currently cast buffs,
			// meat, adventures remaining, and equipment.

			if ( refresh )
			{
				(new CharsheetRequest( client )).run();
				client.updateDisplay( ENABLED_STATE, "Status updated." );
			}

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
			checkAvailableEquipment();
		}

		private void checkSkills()
		{
			// Look at skills to decide which ones are possible
			sympathyAvailable = KoLCharacter.hasAmphibianSympathy();
			empathyAvailable = KoLCharacter.hasSkill( EMPATHY.getName() );
			leashAvailable = KoLCharacter.hasSkill( LEASH.getName() );

			// Look at effects to decide which ones are active;
			LockableListModel active = KoLCharacter.getEffects();
			empathyActive = EMPATHY.getCount( active );
			leashActive = LEASH.getCount( active );
		}

		private void checkCurrentEquipment()
		{
			// Initialize defaults
			pithHelmet = null;
			itemWeight = 0;
			specItem = null;
			specWeight = 0;
			leadNecklace = null;
			leadNecklaceOwner = null;
			ratHeadBalloon = null;
			ratHeadBalloonOwner = null;
			tpCount = 0;

			// Check hat for pithiness
			hat = KoLCharacter.getCurrentEquipment( KoLCharacter.HAT );

			if ( hat != null && hat.equals( PITH_HELMET ) )
				pithHelmet = hat;

			// Check current familiar item
			item = KoLCharacter.getCurrentEquipment( KoLCharacter.FAMILIAR );
			if ( item != null )
			{
				itemWeight = FamiliarData.itemWeightModifier( item.getItemID() );
				if ( item.equals( familiarItem ) )
				{
					specItem = item;
					specWeight = itemWeight;
				}
				else if ( item.equals( LEAD_NECKLACE) )
				{
					leadNecklace = item;
					leadNecklaceOwner = familiar;
				}
				else if ( item.equals( RAT_HEAD_BALLOON) )
				{
					ratHeadBalloon = item;
					ratHeadBalloonOwner = familiar;
				}
			}

			// Check accessories for tininess and plasticity
			checkAccessory( 0, KoLCharacter.ACCESSORY1 );
			checkAccessory( 1, KoLCharacter.ACCESSORY2 );
			checkAccessory( 2, KoLCharacter.ACCESSORY3 );
		}

		private void checkAccessory( int index, int type )
		{
			AdventureResult accessory = KoLCharacter.getCurrentEquipment( type );
			acc[ index] = accessory;
			int id = accessory.getItemID();
			if ( id >= firstTinyPlastic && id <= lastTinyPlastic )
				tp[ tpCount++ ] = accessory;
		}

		private void checkAvailableEquipment()
		{
			// If not wearing a pith helmet, search inventory
			if ( hat == null )
			{
			}

			// If current familiar item not the special item and
			// such an item affects weight, search inventory
			if ( familiarItemWeight != 0 && !familiarItem.equals( item ) )
			{
			}

			// If familiar not wearing lead necklace, search
			// inventory and other familiars
			if ( leadNecklace == null )
			{
			}

			// If familiar not wearing rat head balloon, search
			// inventory and other familiars
			if ( ratHeadBalloon == null )
			{
			}

			// If equipped with fewer than three tiny plastic items
			// equipped, search inventory for more
			if ( tpCount < 3 )
			{
			}
		}

		public int baseWeight()
		{	return familiar.getWeight();
		}


		private int maxBuffedWeight()
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
			if ( specItem != null)
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
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( FamiliarTrainingFrame.class, parameters )).run();
	}
}
