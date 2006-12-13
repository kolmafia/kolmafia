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
import javax.swing.JCheckBox;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as MP buffers.
 */

public abstract class MPRestoreItemList extends StaticEntity
{
	public static final MPRestoreItem SOFA = new MPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE );
	public static final MPRestoreItem CAMPGROUND = new MPRestoreItem( "rest at your campground", Integer.MAX_VALUE );
	public static final MPRestoreItem BEANBAG = new MPRestoreItem( "relax in your beanbag", Integer.MAX_VALUE );

	private static final MPRestoreItem GALAKTIK = new MPRestoreItem( "Galaktik's Fizzy Invigorating Tonic", Integer.MAX_VALUE );
	private static final MPRestoreItem MYSTERY_JUICE = new MPRestoreItem( "magical mystery juice", Integer.MAX_VALUE );
	public static final MPRestoreItem SELTZER = new MPRestoreItem( "Knob Goblin seltzer", 10 );

	public static final MPRestoreItem [] CONFIGURES = new MPRestoreItem []
	{
		BEANBAG, CAMPGROUND, GALAKTIK, new MPRestoreItem( "bottle of Vangoghbitussin", 100 ),
		new MPRestoreItem( "bottle of Monsieur Bubble", 56 ), new MPRestoreItem( "unrefined mountain stream syrup", 55 ),
		new MPRestoreItem( "phonics down", 48 ), new MPRestoreItem( "tonic water", 40 ), new MPRestoreItem( "Marquis de Poivre soda", 35 ),
		new MPRestoreItem( "blue paisley oyster egg", 33 ), new MPRestoreItem( "blue polka-dot oyster egg", 33 ),
		new MPRestoreItem( "blue striped oyster egg", 33 ), new MPRestoreItem( "blue pixel potion", 28 ),
		new MPRestoreItem( "Knob Goblin superseltzer", 27 ), new MPRestoreItem( "maple syrup", 25 ),
		new MPRestoreItem( "Blatantly Canadian", 23 ), new MPRestoreItem( "tiny house", 22 ),
		new MPRestoreItem( "green pixel potion", 19 ), new MPRestoreItem( "Dyspepsi-Cola", 12 ),
		new MPRestoreItem( "Cloaca-Cola", 12 ), new MPRestoreItem( "Mountain Stream soda", 8 ), MYSTERY_JUICE,
		SELTZER, new MPRestoreItem( "Cherry Cloaca Cola", 8 ), new MPRestoreItem( "soda water", 4 )
	};

	public static boolean contains( AdventureResult item )
	{
		for ( int i = 0; i < CONFIGURES.length; ++i )
			if ( CONFIGURES[i].itemUsed != null && CONFIGURES[i].itemUsed.equals( item ) )
				return true;

		return false;
	}

	public static JCheckBox [] getCheckboxes()
	{
		String mpRestoreSetting = getProperty( "mpAutoRecoveryItems" );
		JCheckBox [] restoreCheckbox = new JCheckBox[ CONFIGURES.length ];

		for ( int i = 0; i < CONFIGURES.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox( CONFIGURES[i].toString() );
			restoreCheckbox[i].setSelected( mpRestoreSetting.indexOf( CONFIGURES[i].toString().toLowerCase() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static class MPRestoreItem implements Comparable
	{
		private String itemName;
		private int mpPerUse;
		private AdventureResult itemUsed;

		public MPRestoreItem( String itemName, int mpPerUse )
		{
			this.itemName = itemName;
			this.mpPerUse = mpPerUse;

			if ( TradeableItemDatabase.contains( itemName ) )
				this.itemUsed = new AdventureResult( itemName, 1, false );
			else
				this.itemUsed = null;
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public int compareTo( Object o )
		{
			float restoreAmount = (float) (KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP());
			float ratioDifference = (restoreAmount / ((float) getManaPerUse())) - (restoreAmount / ((float) ((MPRestoreItem)o).getManaPerUse()));
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public int getManaPerUse()
		{
			if ( this == SOFA )
			{
				// The restore rate on the rumpus room sofa changes
				// based on your current level.

				this.mpPerUse = (int) KoLCharacter.getLevel() * 5 + 1;
			}
			else if ( this == MYSTERY_JUICE )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}

			return mpPerUse;
		}

		public void recoverMP( int needed, boolean purchase )
		{
			if ( this == BEANBAG )
			{
				(new CampgroundRequest( "relax" )).run();
				return;
			}

			if ( this == CAMPGROUND )
			{
				(new CampgroundRequest( "rest" )).run();
				return;
			}

			if ( this == GALAKTIK )
			{
				DEFAULT_SHELL.executeLine( "galaktik mp" );
				return;
			}

			if ( this == MYSTERY_JUICE )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}

			int mpShort = needed - KoLCharacter.getCurrentMP();
			int numberToUse = (int) Math.ceil( (float) mpShort / (float) getManaPerUse() );

			if ( this == SOFA )
			{
				(new ClanGymRequest( ClanGymRequest.SOFA )).setTurnCount( numberToUse ).run();
				return;
			}

			int numberAvailable = itemUsed.getCount( inventory );

			// For purchases, attempt to reduce the number of times
			// you buy items by a factor of three.

			if ( purchase && numberAvailable < numberToUse && NPCStoreDatabase.contains( itemUsed.getName() ) )
			{
				AdventureDatabase.retrieveItem( itemUsed.getInstance(
					Math.min( KoLCharacter.getAvailableMeat() / (TradeableItemDatabase.getPriceById( itemUsed.getItemId() ) * 2), numberToUse * 3 ) ) );

				numberAvailable = itemUsed.getCount( inventory );
			}

			numberToUse = Math.min( numberAvailable, numberToUse );

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 )
				return;

			getClient().makeRequest( new ConsumeItemRequest( itemUsed.getInstance( numberToUse ) ) );
		}

		public String toString()
		{	return itemName;
		}
	}
}
