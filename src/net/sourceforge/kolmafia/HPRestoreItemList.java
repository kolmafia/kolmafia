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
 * "AS IS" AND ANY EXPRESS OR IHPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IHPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEHPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
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
 * items which are available for use as HP buffers.
 */

public abstract class HPRestoreItemList extends StaticEntity
{
	public static final HPRestoreItem GALAKTIK = new HPRestoreItem( "doc galaktik", 1, 10 );
	public static final HPRestoreItem HOUSE = new HPRestoreItem( "rest at campsite", 1, -3 );

	private static Object [] restoreName = new Object[0];
	private static JCheckBox [] restoreCheckbox = new JCheckBox[0];
	private static SortedListModel list = new SortedListModel();

	public static void reset()
	{
		list.clear();
		list.add( GALAKTIK );
		list.add( HOUSE );

		// These restores maximize your current HP.  To
		// make sure they appear at the top, they have values
		// which do not reflect their market value.

		list.add( new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE, 0 ) );
		list.add( new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE, 0 ) );

		// These HP restores come from NPCs, so they have a
		// constant market value

		list.add( new HPRestoreItem( "Doc Galaktik's Pungent Unguent", 4, 30 ) );
		list.add( new HPRestoreItem( "Doc Galaktik's Ailment Ointment", 9, 60 ) );
		list.add( new HPRestoreItem( "Doc Galaktik's Restorative Balm", 14, 120 ) );
		list.add( new HPRestoreItem( "Doc Galaktik's Homeopathic Elixir", 19, 240 ) );

		// Non-standard items which can also be used for HP
		// recovery, but might be a little expensive.

		list.add( new HPRestoreItem( "cast", 17, 300 ) );
		list.add( new HPRestoreItem( "tiny house", 22, 400 ) );
		list.add( new HPRestoreItem( "phonics down", 48, 900 ) );
	}

	public static HPRestoreItem get( int index )
	{	return (HPRestoreItem) list.get( index );
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

		String HPRestoreSetting = getProperty( "hpRestoreItems" );

		for ( int i = 0; i < restoreName.length; ++i )
			if ( HPRestoreSetting.indexOf( restoreName[i].toString() ) != -1 )
				restoreCheckbox[i].setSelected( true );

		JScrollPane scrollArea = new JScrollPane( restorePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		return scrollArea;
	}

	public static void setProperty()
	{
		StringBuffer HPRestoreSetting = new StringBuffer();

		if ( restoreCheckbox != null )
		{
			for ( int i = 0; i < restoreCheckbox.length; ++i )
			{
				if ( restoreCheckbox[i].isSelected() )
				{
					HPRestoreSetting.append( restoreName[i].toString() );
					HPRestoreSetting.append( ';' );
				}
			}
		}

		setProperty( "hpRestoreItems", HPRestoreSetting.toString() );
	}

	public static class HPRestoreItem implements Comparable
	{
		private String itemName;
		private int hpPerUse;
		private int estimatedPrice;
		private double priceToHPRatio;
		private AdventureResult itemUsed;

		public HPRestoreItem( String itemName, int hpPerUse, int estimatedPrice )
		{
			this.itemName = itemName;
			this.hpPerUse = hpPerUse;
			this.estimatedPrice = estimatedPrice;

			this.priceToHPRatio = hpPerUse == 0 ? Double.MAX_VALUE : (double)estimatedPrice / (double)hpPerUse;
			this.itemUsed = new AdventureResult( itemName, 0 );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverHP()
		{
			if ( this == GALAKTIK )
			{
				(new GalaktikRequest( client, GalaktikRequest.HP )).run();
				return;
			}

			if ( this == HOUSE )
			{
				DEFAULT_SHELL.updateDisplay( "Resting at campground..." );
				(new CampgroundRequest( client, "rest" )).run();
				return;
			}

			int currentHP = KoLCharacter.getCurrentHP();
			int maximumHP = KoLCharacter.getMaximumHP();

			// Always buff as close to max HP as possible, in order to
			// go as easy on the server as possible.

			int hpShort = maximumHP - currentHP;
			int numberToUse = Math.min( (int) Math.ceil( hpShort / hpPerUse ), itemUsed.getCount( KoLCharacter.getInventory() ) );

			// Because there aren't many buffbots running anymore, it's
			// okay to use one less than is actually necessary.

			if ( numberToUse > 1 )
				--numberToUse;
			else
				numberToUse = 1;

			DEFAULT_SHELL.updateDisplay( "Consuming " + numberToUse + " " + itemName + "..." );
			(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof HPRestoreItem) || o == null )
				return -1;

			double ratioDifference = this.priceToHPRatio - ((HPRestoreItem)o).priceToHPRatio;
			return ratioDifference < 0.0 ? -1 : ratioDifference > 0.0 ? 1 : 0;
		}

		public String toString()
		{	return itemName;
		}
	}
}
