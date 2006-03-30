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
import javax.swing.SpringLayout;

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
import java.lang.ref.WeakReference;
import com.sun.java.forums.SpringUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A Frame to provide access to supported buffbots
 */

public class BuffRequestFrame extends KoLFrame
{
	public BuffRequestFrame()
	{
		super( "Purchase Buffs" );

		CardLayout cards = new CardLayout( 10, 10 );
		framePanel.setLayout( cards );

		// Configure buffbot offerings
		BuffBotDatabase.configureBuffBots();

		BuffRequestPanel buffs = new BuffRequestPanel();
		JScrollPane scroller = new JScrollPane( buffs, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 600, 480 );
		framePanel.add( scroller, "" );

		StaticEntity.getClient().enableDisplay();
	}

	private class BuffRequestPanel extends JPanel
	{
		BuffRequestBox [] boxes;

		public BuffRequestPanel()
		{
			super( new SpringLayout() );
			existingPanels.add( new WeakReference( this ) );

			// Add a panel for each available buff
			int buffCount = BuffBotDatabase.buffCount();
			boxes = new BuffRequestBox[buffCount];

			for ( int i = 0; i < buffCount; ++i )
				boxes[i] = new BuffRequestBox( i );

			SpringUtilities.makeCompactGrid( this, buffCount, 3, 5, 5, 5, 5 );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );

			if ( boxes == null )
				return;

			for ( int i = 0; i < boxes.length; ++i )
				if ( boxes[i] != null )
					boxes[i].setEnabled( isEnabled );
		}

		private class BuffRequestBox
		{
			private int index;
			JComboBox selects;
			JButton button;

			public BuffRequestBox( int index )
			{
				this.index = index;

				// Make a combo box and fill it with offerings
				selects = new JComboBox();

				int count = BuffBotDatabase.getBuffOfferingCount( index );
				for (int j = 0; j < count; ++j )
				{
					String label = BuffBotDatabase.getBuffLabel( index, j );
					selects.addItem( label );
				}

				// Label the box with the Skill name
				String name = BuffBotDatabase.getBuffAbbreviation( index );
				JLabel label = new JLabel( name, JLabel.RIGHT );

				// Add a button to purchase this buff
				button = new JButton( "Buy" );
				button.addActionListener( new BuyBuffListener() );

				BuffRequestPanel.this.add( label );
				BuffRequestPanel.this.add( selects );
				BuffRequestPanel.this.add( button );
			}

			public void setEnabled( boolean isEnabled )
			{
				if ( selects != null )
					selects.setEnabled( isEnabled );
				if ( button != null )
					button.setEnabled( isEnabled );
			}

			private class BuyBuffListener extends ListeningRunnable
			{
				public void run()
				{
					selects.requestFocus();

					String buff = BuffBotDatabase.getBuffName( index );
					int selection = selects.getSelectedIndex();
					String bot = BuffBotDatabase.getBuffBot( index, selection );
					int price = BuffBotDatabase.getBuffPrice( index, selection );
					int turns = BuffBotDatabase.getBuffTurns( index, selection );

					DEFAULT_SHELL.updateDisplay( "Buying " + turns + " turns of " + buff + " from " + bot );
					(new GreenMessageRequest( StaticEntity.getClient(), bot, VERSION_NAME, new AdventureResult( AdventureResult.MEAT, price ), false )).run();
					DEFAULT_SHELL.updateDisplay( "Buff request complete." );
				}
			}
		}
	}
}
