/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import javax.swing.JCheckBox;

public abstract class MPRestoreItemList extends StaticEntity
{
	private static final AdventureResult EXPRESS_CARD = new AdventureResult( ConsumeItemRequest.EXPRESS_CARD, 1 );
	private static boolean purchaseBasedSort = false;

	public static final MPRestoreItem EXPRESS = new MPRestoreItem( "Platinum Yendorian Express Card", Integer.MAX_VALUE, false );
	public static final MPRestoreItem SOFA = new MPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE, false );
	public static final MPRestoreItem CAMPGROUND = new MPRestoreItem( "rest at your campground", Integer.MAX_VALUE, false );
	public static final MPRestoreItem BEANBAG = new MPRestoreItem( "relax in your beanbag", Integer.MAX_VALUE, false );

	private static final MPRestoreItem GALAKTIK = new MPRestoreItem( "Galaktik's Fizzy Invigorating Tonic", 1, 17, false );
	public static final MPRestoreItem MYSTERY_JUICE = new MPRestoreItem( "magical mystery juice", Integer.MAX_VALUE, 100, true );
	public static final MPRestoreItem SELTZER = new MPRestoreItem( "Knob Goblin seltzer", 10, 80, true );

	public static final MPRestoreItem [] CONFIGURES = new MPRestoreItem []
	{
		EXPRESS, SOFA, BEANBAG, CAMPGROUND, GALAKTIK, new MPRestoreItem( "natural fennel soda", 100, false ),
		new MPRestoreItem( "bottle of Vangoghbitussin", 100, false ), new MPRestoreItem( "Monstar energy beverage", 75, false ),
		new MPRestoreItem( "carbonated soy milk", 75, false ), new MPRestoreItem( "carbonated water lily", 65, false ),
		new MPRestoreItem( "bottle of Monsieur Bubble", 56, true ), new MPRestoreItem( "ancient Magi-Wipes", 55, false ),
		new MPRestoreItem( "unrefined mountain stream syrup", 55, true ), new MPRestoreItem( "phonics down", 48, false ),
		new MPRestoreItem( "tonic water", 40, false ), new MPRestoreItem( "honey-dipped locust", 36, false ),
		new MPRestoreItem( "Marquis de Poivre soda", 35, false ), new MPRestoreItem( "blue paisley oyster egg", 33, false ),
		new MPRestoreItem( "blue polka-dot oyster egg", 33, false ), new MPRestoreItem( "blue striped oyster egg", 33, false ),
		new MPRestoreItem( "blue pixel potion", 28, true ), new MPRestoreItem( "Knob Goblin superseltzer", 27, true ),
		new MPRestoreItem( "Blatantly Canadian", 23, false ), new MPRestoreItem( "tiny house", 22, false ),
		new MPRestoreItem( "green pixel potion", 19, true ), new MPRestoreItem( "Dyspepsi-Cola", 12, true ),
		new MPRestoreItem( "Cloaca-Cola", 12, true ), new MPRestoreItem( "Mountain Stream soda", 8, true ),
		MYSTERY_JUICE, new MPRestoreItem( "black cherry soda", 10, 80, false ), SELTZER,
		new MPRestoreItem( "Cherry Cloaca Cola", 8, 80, true ), new MPRestoreItem( "soda water", 4, 70, false )
	};

	public static final void setPurchaseBasedSort( boolean purchaseBasedSort )
	{	MPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
	}

	public static final boolean contains( AdventureResult item )
	{
		for ( int i = 0; i < CONFIGURES.length; ++i )
			if ( CONFIGURES[i].itemUsed != null && CONFIGURES[i].itemUsed.equals( item ) )
				return true;

		return false;
	}

	public static final JCheckBox [] getCheckboxes()
	{
		String mpRestoreSetting = KoLSettings.getUserProperty( "mpAutoRecoveryItems" );
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
		private int purchaseCost;
		private boolean isCombatUsable;
		private AdventureResult itemUsed;

		public MPRestoreItem( String itemName, int mpPerUse, boolean isCombatUsable )
		{	this( itemName, mpPerUse, 0, isCombatUsable );
		}

		public MPRestoreItem( String itemName, int mpPerUse, int purchaseCost, boolean isCombatUsable )
		{
			this.itemName = itemName;
			this.mpPerUse = mpPerUse;
			this.purchaseCost = purchaseCost;
			this.isCombatUsable = isCombatUsable;

			if ( TradeableItemDatabase.contains( itemName ) )
				this.itemUsed = new AdventureResult( itemName, 1, false );
			else
				this.itemUsed = null;
		}

		public AdventureResult getItem()
		{	return this.itemUsed;
		}

		public boolean isSkill()
		{	return this.itemUsed == null && this != GALAKTIK;
		}

		public boolean isCombatUsable()
		{	return this.isCombatUsable;
		}

		public int compareTo( Object o )
		{
			MPRestoreItem mpi = (MPRestoreItem) o;

			float restoreAmount = (KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP());

			float leftRatio = restoreAmount / (this.getManaPerUse());
			float rightRatio = restoreAmount / (mpi.getManaPerUse());

			if ( purchaseBasedSort )
			{
				if ( this.purchaseCost != 0 || mpi.purchaseCost != 0 )
				{
					leftRatio = ((float) Math.ceil( leftRatio )) * this.purchaseCost;
					rightRatio = ((float) Math.ceil( rightRatio )) * mpi.purchaseCost;
				}
			}

			float ratioDifference = leftRatio - rightRatio;
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public void updateManaPerUse()
		{
			if ( this == SOFA )
			{
				// The restore rate on the rumpus room sofa changes
				// based on your current level.

				this.mpPerUse = KoLCharacter.getLevel() * 5 + 1;
			}
			else if ( this == MYSTERY_JUICE )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}
			else if ( this == GALAKTIK )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.purchaseCost = QuestLogRequest.galaktikCuresAvailable() ? 12 : 17;
			}
		}

		public int getManaPerUse()
		{	return Math.min( this.mpPerUse, KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() );
		}

		public void recoverMP( int needed, boolean purchase )
		{
			if ( !KoLmafia.permitsContinue() )
				return;

			if ( this == EXPRESS )
			{
				if ( KoLSettings.getBooleanProperty( "expressCardUsed" ) )
					return;

				if ( inventory.contains( EXPRESS_CARD ) )
				{
					RequestThread.postRequest( new ConsumeItemRequest( EXPRESS_CARD ) );
					return;
				}

				if ( !KoLCharacter.canInteract() )
					return;

				if ( ClanManager.getStash().isEmpty() )
					RequestThread.postRequest( new ClanStashRequest() );

				if ( !ClanManager.getStash().contains( EXPRESS_CARD ) )
					return;

				RequestThread.postRequest( new ClanStashRequest( new Object [] { EXPRESS_CARD }, ClanStashRequest.STASH_TO_ITEMS ) );
				RequestThread.postRequest( new ConsumeItemRequest( EXPRESS_CARD ) );
				RequestThread.postRequest( new ClanStashRequest( new Object [] { EXPRESS_CARD }, ClanStashRequest.ITEMS_TO_STASH ) );
				return;
			}

			if ( this == BEANBAG )
			{
				RequestThread.postRequest( new CampgroundRequest( "relax" ) );
				return;
			}

			if ( this == CAMPGROUND )
			{
				RequestThread.postRequest( new CampgroundRequest( "rest" ) );
				return;
			}

			if ( this == GALAKTIK )
			{
				if ( purchase && needed > KoLCharacter.getCurrentMP() )
				{
					RequestThread.postRequest( new GalaktikRequest( GalaktikRequest.MP,
						Math.min( needed - KoLCharacter.getCurrentMP(), KoLCharacter.getAvailableMeat() / this.purchaseCost ) ) );
				}

				return;
			}

			if ( this == MYSTERY_JUICE )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}

			int mpShort = needed - KoLCharacter.getCurrentMP();
			if ( mpShort <= 0 )
				return;

			int numberToUse = Math.max( (int) Math.floor( (float) mpShort / (float) this.getManaPerUse() ), 1 );

			if ( this == SOFA )
			{
				RequestThread.postRequest( (new ClanGymRequest( ClanGymRequest.SOFA )).setTurnCount( numberToUse ) );
				return;
			}

			int numberAvailable = this.itemUsed.getCount( inventory );

			// If you need to purchase, then calculate a better
			// purchasing strategy.

			if ( purchase && numberAvailable < numberToUse && NPCStoreDatabase.contains( this.itemUsed.getName() ) )
			{
				int numberToBuy = numberToUse;
				int unitPrice = TradeableItemDatabase.getPriceById( this.itemUsed.getItemId() ) * 2;

				if ( MoodSettings.isExecuting() )
				{
					// For purchases involving between battle checks,
					// buy at least as many as is needed to sustain
					// the entire check.

					mpShort = Math.max( mpShort, MoodSettings.getMaintenanceCost() - KoLCharacter.getCurrentMP() );
					numberToBuy = Math.max( (int) Math.floor( (float) mpShort / (float) this.getManaPerUse() ), 1 );
				}

				numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, numberToBuy );

				if ( !AdventureDatabase.retrieveItem( this.itemUsed.getInstance( numberToBuy ) ) )
					return;

				numberAvailable = this.itemUsed.getCount( inventory );
			}

			numberToUse = Math.min( numberAvailable, numberToUse );

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 || !KoLmafia.permitsContinue() )
				return;

			RequestThread.postRequest( new ConsumeItemRequest( this.itemUsed.getInstance( numberToUse ) ) );
		}

		public String toString()
		{	return this.itemName;
		}
	}
}
