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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.ImageIcon;

// utilities
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to provide access to Xylpher's familiar training tool
 */

public class FamiliarTrainingFrame extends KoLFrame
{
	private static ChatBuffer results = new ChatBuffer( "Arena Tracker" );
	private LockableListModel opponents;

	public FamiliarTrainingFrame( KoLmafia client )
	{
		super( client, "Familiar Training Tool" );

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		FamiliarTrainingPanel training = new FamiliarTrainingPanel();
		getContentPane().add( training, "" );
	}

	private class FamiliarTrainingPanel extends JPanel
	{
		FamiliarData familiar;

		// Components
		FamiliarPanel familiarPanel;
		OpponentsPanel opponentsPanel;
		ButtonPanel buttonPanel;
		ResultsPanel resultsPanel;

		public FamiliarTrainingPanel()
		{
			setLayout( new BorderLayout( 10, 10 ) );

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

				// Get current familiar
				familiar = KoLCharacter.getFamiliar();

				// Set the icon and label
				refreshFamiliar();

				// Register a listener to keep it updated
				KoLCharacter.addKoLCharacterListener( new KoLCharacterAdapter( new FamiliarRefresh() ) );
			}

			private void refreshFamiliar()
			{
				familiarIcon.setIcon( FamiliarsDatabase.getFamiliarImage( familiar.getID() ) );

				String label = familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace();
				familiarLabel.setText( label );
			}

			private class FamiliarRefresh implements Runnable
			{
				public void run()
				{
					FamiliarData data = KoLCharacter.getFamiliar();
					if ( data != familiar )
					{
						familiar = data;
						refreshFamiliar();
					}
				}
			}
		}

		private class OpponentsPanel extends JPanel
		{
			public OpponentsPanel()
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
				// Get current opponents 
				getOpponents( client );

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

			public ButtonPanel()
			{
				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
				base = new JButton( "Base" );
				base.addActionListener( new BaseListener() );
				this.add( base );

				buffed = new JButton( "Buffed" );
				buffed.addActionListener( new BuffedListener() );
				this.add( buffed );
			}

			private class BaseListener implements ActionListener, Runnable
			{
				public void actionPerformed( ActionEvent e )
				{	(new DaemonThread( this )).start();
				}

				public void run()
				{
					// Prompt for goal
					int goal = getGoal( "base" );

					// Level the familiar
					levelFamiliar( client, goal, true );
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
					int goal = getGoal( "buffed" );

					// Level the familiar
					levelFamiliar( client, goal, false );
				}
			}

			private int getGoal( String type )
			{
				return getQuantity( "Train up to what " + type + " weight?", 20, 20 );
			}
		}

		private class ResultsPanel extends JPanel
		{
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
	}

	private void getOpponents( KoLmafia client )
	{
		if ( CakeArenaManager.getOpponentList().isEmpty() )
			(new CakeArenaRequest( client )).run();
		opponents = CakeArenaManager.getOpponentList();
	}


	/**
	 * Utility method to level the current familiar by fighting the
	 * current arena opponents.
	 *
	 * @param	goal	Weight goal for the familiar
	 * @param	base	true if goal is base weight, false if buffed
	 */

	public void levelFamiliar( KoLmafia client, int goal, boolean base )
	{
		// Get current familiar;
		FamiliarData familiar = KoLCharacter.getFamiliar();

		// Get opponent list
		getOpponents( client );

		// Find available items

		// Clear the output
		results.clearBuffer();

		// Print some initial stuff
		String name = familiar.getName();
		String race = familiar.getRace();
		int weight = familiar.getWeight();

		results.append( "Training " + name + ", the " + weight + " lb. " + race + ".<br>" );
                results.append( "Goal: " + goal + " lbs. " + ( base ? "base" : "buffed" ) + " weight.<br>");
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( FamiliarTrainingFrame.class, parameters )).run();
	}
}
