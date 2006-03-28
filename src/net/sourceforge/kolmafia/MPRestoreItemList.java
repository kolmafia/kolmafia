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

import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as MP buffers.
 */

public abstract class MPRestoreItemList extends StaticEntity
{
	public static final MPRestoreItem BEANBAG = new MPRestoreItem( "relax in beanbag", 1, -3 );
	public static final MPRestoreItem HOUSE = new MPRestoreItem( "rest at campsite", 1, -2 );
	public static final MPRestoreItem MYSTERY = new MPRestoreItem( "magical mystery juice", 1, -1 );

	public static final MPRestoreItem GALAKTIK = new MPRestoreItem( "doc galaktik", 1, 20 );

	private static Object [] restoreName = new Object[0];
	private static JCheckBox [] restoreCheckbox = new JCheckBox[0];
	private static SortedListModel list = new SortedListModel();

	public static void reset()
	{
		list.clear();
		list.add( GALAKTIK );
		list.add( BEANBAG );
		list.add( HOUSE );
		list.add( MYSTERY );

		// These MP restores come from NPCs, so they have a
		// constant market value

		list.add( new MPRestoreItem( "soda water", 4, 70 ) );

		// On the other hand, these MP restores have a fairly
		// arbitrary value and may be subject to arbitrary
		// inflation, based on player spending habits.

		list.add( new MPRestoreItem( "tiny house", 22, 400 ) );
		list.add( new MPRestoreItem( "phonics down", 48, 900 ) );
		list.add( new MPRestoreItem( "Knob Goblin superseltzer", 27, 1000 ) );
		list.add( new MPRestoreItem( "Mountain Stream soda", 8, 120 ) );
		list.add( new MPRestoreItem( "Dyspepsi-Cola", 12, 250 ) );
		list.add( new MPRestoreItem( "Knob Goblin seltzer", 10, 100 ) );
		list.add( new MPRestoreItem( "blatantly Canadian", 22, 800 ) );
	}

	public static MPRestoreItem get( int index )
	{	return (MPRestoreItem) list.get( index );
	}

	public static int size()
	{	return list.size();
	}

	public static JScrollPane getDisplay()
	{
		restoreName = list.toArray();
		restoreCheckbox = new JCheckBox[ restoreName.length ];

		JPanel checkboxPanel = new JPanel();
		checkboxPanel.setLayout( new GridLayout( restoreCheckbox.length, 1 ) );

		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox();
			checkboxPanel.add( restoreCheckbox[i] );
		}

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new GridLayout( restoreName.length, 1 ) );
		for ( int i = 0; i < restoreName.length; ++i )
			labelPanel.add( new JLabel( restoreName[i].toString(), JLabel.LEFT ) );

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BorderLayout( 0, 0 ) );
		restorePanel.add( checkboxPanel, BorderLayout.WEST );
		restorePanel.add( labelPanel, BorderLayout.CENTER );

		String mpRestoreSetting = getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < restoreName.length; ++i )
			if ( mpRestoreSetting.indexOf( restoreName[i].toString() ) != -1 )
				restoreCheckbox[i].setSelected( true );

		JScrollPane scrollArea = new JScrollPane( restorePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		return scrollArea;
	}

	public static void setProperty()
	{
		StringBuffer mpRestoreSetting = new StringBuffer();

		if ( restoreCheckbox != null )
		{
			for ( int i = 0; i < restoreCheckbox.length; ++i )
			{
				if ( restoreCheckbox[i].isSelected() )
				{
					mpRestoreSetting.append( restoreName[i].toString() );
					mpRestoreSetting.append( ';' );
				}
			}
		}

		setProperty( "buffBotMPRestore", mpRestoreSetting.toString() );
	}

	public static class MPRestoreItem implements Comparable
	{
		private String itemName;
		private int mpPerUse;
		private int estimatedPrice;
		private double priceToMPRatio;
		private AdventureResult itemUsed;

		public MPRestoreItem( String itemName, int mpPerUse, int estimatedPrice )
		{
			this.itemName = itemName;
			this.mpPerUse = mpPerUse;
			this.estimatedPrice = estimatedPrice;

			this.priceToMPRatio = (double)estimatedPrice / (double)mpPerUse;
			this.itemUsed = new AdventureResult( itemName, 0 );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverMP()
		{
			if ( this == GALAKTIK )
			{
				(new GalaktikRequest( client, GalaktikRequest.MP )).run();
				return;
			}

			if ( this == BEANBAG )
			{
				DEFAULT_SHELL.updateDisplay( "Relaxing in beanbag chair..." );
				(new CampgroundRequest( client, "relax" )).run();
				return;
			}

			if ( this == HOUSE )
			{
				DEFAULT_SHELL.updateDisplay( "Resting at campground..." );
				(new CampgroundRequest( client, "rest" )).run();
				return;
			}

			if ( this == MYSTERY )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}

			int currentMP = KoLCharacter.getCurrentMP();
			int maximumMP = KoLCharacter.getMaximumMP();

			// Always buff as close to max MP as possible, in order to
			// go as easy on the server as possible.

			int mpShort = maximumMP - currentMP;
			int numberToUse = (int) Math.ceil( mpShort / mpPerUse );
			
			if ( StaticEntity.getProperty( "autoSatisfyChecks" ).equals( "false" ) )
				numberToUse = Math.min( numberToUse, itemUsed.getCount( KoLCharacter.getInventory() ) );

			// Because there aren't many buffbots running anymore, it's
			// okay to use one less than is actually necessary.

			if ( numberToUse < 1 )
				numberToUse = 1;

			DEFAULT_SHELL.updateDisplay( "Consuming " + numberToUse + " " + itemName + "..." );
			(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof MPRestoreItem) || o == null )
				return -1;

			double ratioDifference = this.priceToMPRatio - ((MPRestoreItem)o).priceToMPRatio;
			return ratioDifference < 0.0 ? -1 : ratioDifference > 0.0 ? 1 : 0;
		}

		public String toString()
		{	return itemName;
		}
	}
}
