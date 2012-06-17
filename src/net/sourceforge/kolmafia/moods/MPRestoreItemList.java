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

package net.sourceforge.kolmafia.moods;

import java.util.HashMap;

import javax.swing.JCheckBox;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.textui.command.NunneryCommand;

public abstract class MPRestoreItemList
{
	private static boolean purchaseBasedSort = false;
	private static HashMap<String, MPRestoreItem> restoreByName = new HashMap<String, MPRestoreItem>();

	public static final MPRestoreItem EXPRESS =
		new MPRestoreItem( "Platinum Yendorian Express Card", Integer.MAX_VALUE, false );
	public static final MPRestoreItem SOFA = new MPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE, false );
	public static final MPRestoreItem CAMPGROUND =
		new MPRestoreItem( "rest at your campground", Integer.MAX_VALUE, false );
	public static final MPRestoreItem DISCOREST =
		new MPRestoreItem( "free disco rest", Integer.MAX_VALUE, false );

	private static final MPRestoreItem NUNS =
		new MPRestoreItem( "visit the nuns", 1000, false);
	private static final MPRestoreItem OSCUS =
		new MPRestoreItem( "Oscus's neverending soda", 250, false);
	private static final MPRestoreItem QUARK =
		new MPRestoreItem( "unstable quark + junk item", 100, false);
	private static final MPRestoreItem GALAKTIK =
		new MPRestoreItem( "Galaktik's Fizzy Invigorating Tonic", 1, 17, false );
	public static final MPRestoreItem MYSTERY_JUICE =
		new MPRestoreItem( "magical mystery juice", Integer.MAX_VALUE, 100, true );
	public static final MPRestoreItem SELTZER = new MPRestoreItem( "Knob Goblin seltzer", 10, 80, true );
	private static final MPRestoreItem MOTH =
		new MPRestoreItem( "delicious shimmering moth", 35, false );

	public static final MPRestoreItem[] CONFIGURES = new MPRestoreItem[]
	{
		MPRestoreItemList.EXPRESS,
		MPRestoreItemList.SOFA,
		MPRestoreItemList.CAMPGROUND,
		MPRestoreItemList.DISCOREST,
		MPRestoreItemList.GALAKTIK,
		MPRestoreItemList.NUNS,
		MPRestoreItemList.OSCUS,
		MPRestoreItemList.QUARK,
		new MPRestoreItem( "high-pressure seltzer bottle", 175, true ),
		new MPRestoreItem( "natural fennel soda", 100, false ),
		new MPRestoreItem( "bottle of Vangoghbitussin", 100, false ),
		new MPRestoreItem( "Monstar energy beverage", 75, false ),
		new MPRestoreItem( "carbonated soy milk", 75, false ),
		new MPRestoreItem( "carbonated water lily", 65, false ),
		new MPRestoreItem( "Nardz energy beverage", 65, false ),
		new MPRestoreItem( "blue pixel potion", 65, true ),
		new MPRestoreItem( "cotton candy bale", 61, false ),
		new MPRestoreItem( "bottle of Monsieur Bubble", 56, true ),
		new MPRestoreItem( "ancient Magi-Wipes", 55, false ),
		new MPRestoreItem( "unrefined mountain stream syrup", 55, true ),
		new MPRestoreItem( "cotton candy pillow", 51, false ),
		new MPRestoreItem( "phonics down", 48, false ),
		new MPRestoreItem( "elven magi-pack", 45, false ),
		new MPRestoreItem( "generic mana potion", 44, false ),
		new MPRestoreItem( "generic restorative potion", 44, false ),
		new MPRestoreItem( "tonic water", 40, false ),
		new MPRestoreItem( "cotton candy cone", 39, false ),
		new MPRestoreItem( "palm-frond fan", 37, false ),
		new MPRestoreItem( "Okee-Dokee soda", 37, false ),
		new MPRestoreItem( "honey-dipped locust", 36, false ),
		new MPRestoreItem( "Marquis de Poivre soda", 35, false ),
		MPRestoreItemList.MOTH,
		new MPRestoreItem( "green pixel potion", 35, true ),
		new MPRestoreItem( "blue paisley oyster egg", 33, false ),
		new MPRestoreItem( "blue polka-dot oyster egg", 33, false ),
		new MPRestoreItem( "blue striped oyster egg", 33, false ),
		new MPRestoreItem( "cotton candy plug", 28, false ),
		new MPRestoreItem( "Knob Goblin superseltzer", 27, true ),
		new MPRestoreItem( "Blatantly Canadian", 23, false ),
		new MPRestoreItem( "cotton candy skoshe", 22, false ),
		new MPRestoreItem( "tiny house", 22, false ),
		new MPRestoreItem( "cotton candy smidgen", 17, false ),
		new MPRestoreItem( "Dyspepsi-Cola", 12, true ),
		new MPRestoreItem( "Cloaca-Cola", 12, true ),
		new MPRestoreItem( "Diet Cloaca Cola", 8, true ),
		new MPRestoreItem( "cotton candy pinch", 12, false ),
		new MPRestoreItem( "sugar shard", 8, false ),
		new MPRestoreItem( "Mountain Stream soda", 8, true ),
		MPRestoreItemList.MYSTERY_JUICE,
		new MPRestoreItem( "black cherry soda", 10, 80, false ),
		MPRestoreItemList.SELTZER,
		new MPRestoreItem( "Cherry Cloaca Cola", 8, 80, true ),
		new MPRestoreItem( "soda water", 4, 70, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter I", 35, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter II", 35, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter III", 35, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter IV", 35, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter V", 35, false ),
		new MPRestoreItem( "Notes from the Elfpocalypse, Chapter VI", 35, false ),
		new MPRestoreItem( "sueling turtle", 15, false ),
		new MPRestoreItem( "unrefined Mountain Stream syrup", 55, true )
	};

	public static final void setPurchaseBasedSort( final boolean purchaseBasedSort )
	{
		MPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
	}

	public static int getManaRestored( String restoreName )
	{
		MPRestoreItem restoreItem = (MPRestoreItem) MPRestoreItemList.restoreByName.get( restoreName );
		return restoreItem == null ? Integer.MIN_VALUE : restoreItem.manaPerUse;
	}

	public static void updateManaRestored()
	{
		MPRestoreItemList.CAMPGROUND.manaPerUse = MPRestoreItemList.DISCOREST.manaPerUse =
			KoLCharacter.getRestingMP();
		MPRestoreItemList.SOFA.manaPerUse = KoLCharacter.getLevel() * 5 + 1;
		MPRestoreItemList.MYSTERY_JUICE.manaPerUse = (int) ( KoLCharacter.getLevel() * 1.5f + 4.0f );
		MPRestoreItemList.GALAKTIK.purchaseCost = QuestLogRequest.galaktikCuresAvailable() ? 12 : 17;
	}

	public static final boolean contains( final AdventureResult item )
	{
		return restoreByName.containsKey( item.getName() );
	}

	public static final JCheckBox[] getCheckboxes()
	{
		String mpRestoreSetting = Preferences.getString( "mpAutoRecoveryItems" );
		JCheckBox[] restoreCheckbox = new JCheckBox[ MPRestoreItemList.CONFIGURES.length ];

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			restoreCheckbox[ i ] = new JCheckBox( MPRestoreItemList.CONFIGURES[ i ].toString() );
			restoreCheckbox[ i ].setSelected( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static final void updateCheckboxes( final JCheckBox[] restoreCheckbox )
	{
		String mpRestoreSetting = Preferences.getString( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			restoreCheckbox[ i ].setSelected( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 );
		}
	}

	public static class MPRestoreItem
		implements Comparable
	{
		private final String itemName;
		private int manaPerUse;
		private int purchaseCost;
		private final boolean isCombatUsable;
		private AdventureResult itemUsed;

		public MPRestoreItem( final String itemName, final int manaPerUse, final boolean isCombatUsable )
		{
			this( itemName, manaPerUse, 0, isCombatUsable );
		}

		public MPRestoreItem( final String itemName, final int manaPerUse, final int purchaseCost, final boolean isCombatUsable )
		{
			this.itemName = itemName;
			this.manaPerUse = manaPerUse;
			this.purchaseCost = purchaseCost;
			this.isCombatUsable = isCombatUsable;

			MPRestoreItemList.restoreByName.put( itemName, this );

			if ( ItemDatabase.contains( itemName ) )
			{
				this.itemUsed = ItemPool.get( itemName, 1 );
			}
			else
			{
				this.itemUsed = null;
			}
		}

		public AdventureResult getItem()
		{
			return this.itemUsed;
		}

		public boolean isSkill()
		{
			return this.itemUsed == null && this != MPRestoreItemList.GALAKTIK;
		}

		public boolean isCombatUsable()
		{
			return this.isCombatUsable;
		}

		public int compareTo( final Object o )
		{
			MPRestoreItem mpi = (MPRestoreItem) o;

			float restoreAmount = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();

			float leftRatio = restoreAmount / this.getManaRestored();
			float rightRatio = restoreAmount / mpi.getManaRestored();

			if ( MPRestoreItemList.purchaseBasedSort )
			{
				if ( this.purchaseCost != 0 || mpi.purchaseCost != 0 )
				{
					leftRatio = (float) Math.ceil( leftRatio ) * this.purchaseCost;
					rightRatio = (float) Math.ceil( rightRatio ) * mpi.purchaseCost;
				}
			}

			float ratioDifference = leftRatio - rightRatio;
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public int getManaRestored()
		{
			return Math.min( this.manaPerUse, KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() );
		}

		public boolean usableInCurrentPath()
		{
			if ( this.itemUsed == null || !KoLCharacter.inBeecore() )
			{
				return true;
			}
			String name = this.itemUsed.getName();
			return name.indexOf( "b" ) == -1 && name.indexOf( "B" ) == -1 ;
		}

		public void recoverMP( final int needed, final boolean purchase )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			if ( this == MPRestoreItemList.MOTH && !KoLConstants.activeEffects.contains(
				EffectPool.get( EffectPool.FORM_OF_BIRD ) ) )
			{
				return;
			}

			if ( this == MPRestoreItemList.EXPRESS )
			{
				if ( Preferences.getBoolean( "expressCardUsed" ) )
				{
					return;
				}

				AdventureResult EXPRESS_CARD = this.getItem();

				if ( KoLConstants.inventory.contains( EXPRESS_CARD ) )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( EXPRESS_CARD ) );
					return;
				}

				if ( !KoLCharacter.canInteract() )
				{
					return;
				}

				if ( ClanManager.getStash().isEmpty() )
				{
					RequestThread.postRequest( new ClanStashRequest() );
				}

				if ( !ClanManager.getStash().contains( EXPRESS_CARD ) )
				{
					return;
				}

				RequestThread.postRequest( new ClanStashRequest(
					new Object[] { EXPRESS_CARD }, ClanStashRequest.STASH_TO_ITEMS ) );
				RequestThread.postRequest( UseItemRequest.getInstance( EXPRESS_CARD ) );
				RequestThread.postRequest( new ClanStashRequest(
					new Object[] { EXPRESS_CARD }, ClanStashRequest.ITEMS_TO_STASH ) );
				return;
			}

			if ( this == MPRestoreItemList.CAMPGROUND )
			{
				RequestThread.postRequest( new CampgroundRequest( "rest" ) );
				return;
			}

			if ( this == MPRestoreItemList.DISCOREST )
			{
				int freerests = 0;
				if ( KoLCharacter.hasSkill( "Disco Nap" ) ) ++freerests;
				if ( KoLCharacter.hasSkill( "Disco Power Nap" ) ) freerests += 2;
				if ( KoLCharacter.hasSkill( "Executive Narcolepsy" ) ) ++freerests;
				if ( Preferences.getInteger( "timesRested" ) >= freerests ) return;

				RequestThread.postRequest( new CampgroundRequest( "rest" ) );
				return;
			}

			if ( this == MPRestoreItemList.GALAKTIK )
			{
				if ( purchase && needed > KoLCharacter.getCurrentMP() )
				{
					RequestThread.postRequest( new GalaktikRequest( GalaktikRequest.MP, Math.min(
						needed - KoLCharacter.getCurrentMP(), KoLCharacter.getAvailableMeat() / this.purchaseCost ) ) );
				}

				return;
			}

			if ( this == MPRestoreItemList.NUNS )
			{
				if ( Preferences.getInteger( "nunsVisits" ) >= 3 ) return;
				String side = Preferences.getString( "sidequestNunsCompleted" );
				if ( !side.equals( "fratboy" ) ) return;	// no MP for hippies!
				if ( KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 1000 )
				{
					// don't waste this limited resource on small restores
					return;
				}

				NunneryCommand.visit( "mp" );
				return;
			}

			if ( this == MPRestoreItemList.QUARK )
			{
				if ( ItemPool.get( ItemPool.UNSTABLE_QUARK, 1 ).getCount(
					KoLConstants.inventory ) < 1 )
				{
					return;
				}

				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "quark", "" );
				return;
			}

			if ( this == MPRestoreItemList.OSCUS )
			{
				if ( Preferences.getBoolean( "oscusSodaUsed" ) ) return;
				if ( KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 250 )
				{
					// don't waste this once-a-day item on small restores
					return;
				}
			}

			if ( this == MPRestoreItemList.MYSTERY_JUICE )
			{
				this.manaPerUse = (int) ( KoLCharacter.getLevel() * 1.5 + 4.0 );
			}

			int mpShort = needed - KoLCharacter.getCurrentMP();
			if ( mpShort <= 0 )
			{
				return;
			}

			int numberToUse = Math.max( (int) Math.floor( (float) mpShort / (float) this.getManaRestored() ), 1 );

			if ( this == MPRestoreItemList.SOFA )
			{
				RequestThread.postRequest( ( new ClanRumpusRequest( ClanRumpusRequest.SOFA ) ).setTurnCount( numberToUse ) );
				return;
			}

			int numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );

			// If you need to purchase, then calculate a better
			// purchasing strategy.

			if ( purchase && numberAvailable < numberToUse && NPCStoreDatabase.contains( this.itemUsed.getName() ) )
			{
				int numberToBuy = numberToUse;
				int unitPrice = ItemDatabase.getPriceById( this.itemUsed.getItemId() ) * 2;

				if ( MoodManager.isExecuting() )
				{
					// For purchases involving between battle checks,
					// buy at least as many as is needed to sustain
					// the entire check.

					mpShort = Math.max( mpShort, MoodManager.getMaintenanceCost() - KoLCharacter.getCurrentMP() );
					numberToBuy = Math.max( (int) Math.floor( (float) mpShort / (float) this.getManaRestored() ), 1 );
				}

				numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, numberToBuy );

				// We may need to switch outfits to buy the
				// recovery item, but make sure we are wearing
				// our original outfit before consuming it.

				SpecialOutfit.createImplicitCheckpoint();
				boolean success = InventoryManager.retrieveItem( this.itemUsed.getInstance( numberToBuy ) );
				SpecialOutfit.restoreImplicitCheckpoint();

				if ( !success )
				{
					return;
				}

				numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );
			}

			numberToUse = Math.min( numberAvailable, numberToUse );

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 || !KoLmafia.permitsContinue() )
			{
				return;
			}

			RequestThread.postRequest( UseItemRequest.getInstance( this.itemUsed.getInstance( numberToUse ) ) );
		}

		@Override
		public String toString()
		{
			return this.itemName;
		}
	}
}
