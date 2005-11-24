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
import java.awt.GridLayout;
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
import javax.swing.JComboBox;
import javax.swing.JScrollPane;

// utilities
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	public BuffRequestFrame( KoLmafia client )
	{
		super( client, "Buff Requests" );
		getContentPane().setLayout( new BorderLayout() );

		BuffRequestPanel buffs = new BuffRequestPanel();
		JScrollPane scroller = new JScrollPane( buffs, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 600, 600 );
		getContentPane().add( scroller, BorderLayout.CENTER );
	}

	public boolean isEnabled()
	{	return true;
	}

	private class BuffRequestPanel extends JPanel
	{
		public BuffRequestPanel()
		{
			super();

			// Vertically stacked panels within this one
			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

			// Add a panel for each available buff
			int buffCount = BuffBotDatabase.buffCount();
			for ( int i = 0; i < buffCount; ++i )
				this.add( new BuffRequestBox( i ) );
		}

		private class BuffRequestBox extends JPanel
		{
			private int index;
			JComboBox selects;

			public BuffRequestBox( int index)
			{
				super();
				setLayout( new BorderLayout( 10, 10 ) );

				this.index = index;
				Object [] buff = BuffBotDatabase.getBuff( index );

				ArrayList pairs = (ArrayList)buff[2];

				selects = new JComboBox();
				for (int j = 0; j < pairs.size(); ++j )
				{
					Object [] pair = (Object [])pairs.get( j );
					int price = ((Integer)pair[0]).intValue();
					int turns = ((Integer)pair[1]).intValue();
					String label = turns + " turns for " + price + " meat";
					selects.addItem( label );
				}

				// Now add the controls to the pane

				// Label the box with the Skill name
				int skill = ((Integer)buff[1]).intValue();
				String skillName = ClassSkillsDatabase.getSkillName( skill );
				JLabel label = new JLabel( skillName, JLabel.RIGHT );
				this.add( label, BorderLayout.WEST );

				// Add the combo box of available buffs
				this.add( selects, BorderLayout.CENTER );

				// Add a button to purchase this buff
				JButton button = new JButton( "Buy" );
				button.addActionListener( new BuyBuffListener() );
				this.add( button, BorderLayout.EAST );
			}

			private class BuyBuffListener implements ActionListener, Runnable
			{
				public void actionPerformed( ActionEvent e )
				{	(new DaemonThread( this )).start();
				}

				public void run()
				{
					int selection = selects.getSelectedIndex();
					String bot = BuffBotDatabase.getBuffBot( index );
					int price = BuffBotDatabase.getBuffPrice( index, selection );
					String buff = BuffBotDatabase.getBuffName( index );
					int turns = BuffBotDatabase.getBuffTurns( index, selection );
					client.updateDisplay( DISABLED_STATE, "Buying " + turns + " turns of " + buff + " from " + bot );
					(new GreenMessageRequest( client, bot, "Buff me, baby!", new AdventureResult( AdventureResult.MEAT, price ) )).run();
					client.updateDisplay( ENABLED_STATE, "Buff request complete." );
				}
			}
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( BuffRequestFrame.class, parameters )).run();
	}
}
