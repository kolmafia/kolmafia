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

public class MPRestoreItemList extends SortedListModel implements KoLConstants
{
	public final MPRestoreItem BEANBAG = new MPRestoreItem( "relax in beanbag", 80, -1 );
	public final MPRestoreItem HOUSE = new MPRestoreItem( "rest at campsite", 40, -1 );

	private KoLmafia client;
	private KoLSettings settings;
	private KoLCharacter characterData;

	private Object [] restoreName;
	private JCheckBox [] restoreCheckbox;

	public MPRestoreItemList( KoLmafia client )
	{
		// These MP restores come from NPCs, so they have a
		// constant market value

		this.client = client;
		this.settings = client == null ? GLOBAL_SETTINGS : client.getSettings();
		this.characterData = client == null ? new KoLCharacter( "" ) : client.getCharacterData();

		this.add( BEANBAG );
		this.add( HOUSE );

		this.add( new MPRestoreItem( "magical mystery juice", (int) (characterData.getLevel() * 1.5 + 4.0), 150 ) );
		this.add( new MPRestoreItem( "soda water", 4, 70 ) );

		// On the other hand, these MP restores have a fairly
		// arbitrary value and may be subject to arbitrary
		// inflation, based on player spending habits.

		this.add( new MPRestoreItem( "tiny house", 22, 400 ) );
		this.add( new MPRestoreItem( "phonics down", 48, 800 ) );
		this.add( new MPRestoreItem( "Knob Goblin superseltzer", 27, 900 ) );
		this.add( new MPRestoreItem( "Mountain Stream soda", 9, 120 ) );
		this.add( new MPRestoreItem( "Dyspepsi-Cola", 12, 250 ) );
		this.add( new MPRestoreItem( "Knob Goblin seltzer", 5, 80 ) );
		this.add( new MPRestoreItem( "green pixel potion", 15, 500 ) );
		this.add( new MPRestoreItem( "blue pixel potion", 19, 800 ) );
		this.add( new MPRestoreItem( "Blatantly Canadian", 24, 1000 ) );
	}

	public JScrollPane getDisplay()
	{
		this.restoreName = toArray();
		this.restoreCheckbox = new JCheckBox[ restoreName.length ];

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

		String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < restoreName.length; ++i )
			if ( mpRestoreSetting.indexOf( restoreName[i].toString() ) != -1 )
				restoreCheckbox[i].setSelected( true );

		JScrollPane scrollArea = new JScrollPane( restorePanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scrollArea, 240, 100 );

		return scrollArea;
	}

	public void setProperty()
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

		settings.setProperty( "buffBotMPRestore", mpRestoreSetting.toString() );
	}

	public class MPRestoreItem implements Comparable
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

		public void recoverMP( int mpNeeded )
		{
			if ( this == BEANBAG )
			{
				client.updateDisplay( DISABLED_STATE, "Relaxing in beanbag chair..." );
				(new CampgroundRequest( client, "relax" )).run();
				return;
			}

			if ( this == HOUSE )
			{
				client.updateDisplay( DISABLED_STATE, "Resting at campground..." );
				(new CampgroundRequest( client, "rest" )).run();
				return;
			}

			characterData = client.getCharacterData();
			int currentMP = characterData.getCurrentMP();
			int maximumMP = characterData.getMaximumMP();

			// Always buff as close to maxMP as possible, in order to
			// go as easy on the server as possible.

			// But, don't go too far over (thus wasting restorers)

			int mpShort = Math.max(maximumMP + 5 - mpPerUse, mpNeeded) - currentMP;
			int numberToUse = Math.min( 1 + ((mpShort - 1) / mpPerUse), itemUsed.getCount( client.getInventory() ) );

			if ( numberToUse > 0 )
			{
				client.updateDisplay( DISABLED_STATE, "Consuming " + numberToUse + " " + itemName + "s..." );
				(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
			}
		}

		public int compareTo( Object o )
		{
			if ( o instanceof MPRestoreItem || o == null )
				return -1;

			double ratioDifference = this.priceToMPRatio - ((MPRestoreItem)o).priceToMPRatio;
			return ratioDifference < 0 ? -1 : ratioDifference > 0 ? 1 : 0;
		}

		public String toString()
		{	return itemName;
		}
	}
}