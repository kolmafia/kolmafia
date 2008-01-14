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

import javax.swing.JCheckBox;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.GalaktikRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public abstract class HPRestoreItemList
	extends StaticEntity
{
	private static boolean purchaseBasedSort = false;

	public static final HPRestoreItem WALRUS = new HPRestoreItem( "Tongue of the Walrus", 35 );

	private static final HPRestoreItem SOFA = new HPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE );
	private static final HPRestoreItem CAMPGROUND = new HPRestoreItem( "rest at your campground", 40 );

	private static final HPRestoreItem GALAKTIK = new HPRestoreItem( "Galaktik's Curative Nostrum", 1, 10 );
	private static final HPRestoreItem HERBS =
		new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE, 100 );
	private static final HPRestoreItem OINTMENT = new HPRestoreItem( "Doc Galaktik's Ailment Ointment", 9, 60 );

	public static final HPRestoreItem SCROLL = new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE );
	private static final HPRestoreItem MASSAGE_OIL = new HPRestoreItem( "scented, massage oil", Integer.MAX_VALUE );
	private static final HPRestoreItem COCOON = new HPRestoreItem( "Cannelloni Cocoon", Integer.MAX_VALUE );

	public static final HPRestoreItem[] CONFIGURES = new HPRestoreItem[]
	{
		HPRestoreItemList.SOFA,
		HPRestoreItemList.CAMPGROUND,
		HPRestoreItemList.GALAKTIK,
		HPRestoreItemList.HERBS,
		HPRestoreItemList.SCROLL,
		HPRestoreItemList.MASSAGE_OIL,
		HPRestoreItemList.COCOON,
		new HPRestoreItem( "red pixel potion", 110 ),
		new HPRestoreItem( "really thick bandage", 109 ),
		new HPRestoreItem( "filthy poultice", 100 ),
		new HPRestoreItem( "gauze garter", 100 ),
		new HPRestoreItem( "bottle of Vangoghbitussin", 100 ),
		new HPRestoreItem( "ancient Magi-Wipes", 55 ),
		new HPRestoreItem( "phonics down", 48 ),
		new HPRestoreItem( "Disco Power Nap", 40 ),
		HPRestoreItemList.WALRUS,
		new HPRestoreItem( "honey-dipped locust", 36 ),
		new HPRestoreItem( "red paisley oyster egg", 33 ),
		new HPRestoreItem( "red polka-dot oyster egg", 33 ),
		new HPRestoreItem( "red striped oyster egg", 33 ),
		new HPRestoreItem( "tiny house", 22 ),
		new HPRestoreItem( "Disco Nap", 20 ),
		new HPRestoreItem( "Lasagna Bandages", 20 ),
		new HPRestoreItem( "green pixel potion", 19 ),
		new HPRestoreItem( "Doc Galaktik's Homeopathic Elixir", 19, 240 ),
		new HPRestoreItem( "cast", 17 ),
		new HPRestoreItem( "Tongue of the Otter", 15 ),
		new HPRestoreItem( "Doc Galaktik's Restorative Balm", 14, 120 ),
		HPRestoreItemList.OINTMENT,
		new HPRestoreItem( "forest tears", 7 ),
		new HPRestoreItem( "Doc Galaktik's Pungent Unguent", 3, 30 )
	};

	public static final void setPurchaseBasedSort( final boolean purchaseBasedSort )
	{
		HPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
	}

	public static final boolean contains( final AdventureResult item )
	{
		for ( int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( HPRestoreItemList.CONFIGURES[ i ].itemUsed != null && HPRestoreItemList.CONFIGURES[ i ].itemUsed.equals( item ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final JCheckBox[] getCheckboxes()
	{
		String hpRestoreSetting = KoLSettings.getUserProperty( "hpAutoRecoveryItems" );
		JCheckBox[] restoreCheckbox = new JCheckBox[ HPRestoreItemList.CONFIGURES.length ];

		for ( int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i )
		{
			restoreCheckbox[ i ] = new JCheckBox( HPRestoreItemList.CONFIGURES[ i ].toString() );
			restoreCheckbox[ i ].setSelected( hpRestoreSetting.indexOf( HPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static class HPRestoreItem
		implements Comparable
	{
		private final String restoreName;
		private int healthPerUse;
		private int purchaseCost;

		private int skillId;
		private AdventureResult itemUsed;

		public HPRestoreItem( final String restoreName, final int healthPerUse )
		{
			this( restoreName, healthPerUse, 0 );
		}

		public HPRestoreItem( final String restoreName, final int healthPerUse, final int purchaseCost )
		{
			this.restoreName = restoreName;
			this.healthPerUse = healthPerUse;
			this.purchaseCost = purchaseCost;

			if ( ItemDatabase.contains( restoreName ) )
			{
				this.itemUsed = new AdventureResult( restoreName, 1, false );
				this.skillId = -1;
			}
			else if ( SkillDatabase.contains( restoreName ) )
			{
				this.itemUsed = null;
				this.skillId = SkillDatabase.getSkillId( restoreName );
			}
			else
			{
				this.itemUsed = null;
				this.skillId = -1;
			}
		}

		public boolean isSkill()
		{
			return this.skillId != -1;
		}

		public AdventureResult getItem()
		{
			return this.itemUsed;
		}

		public void updateHealthPerUse()
		{
			if ( this == HPRestoreItemList.SOFA )
			{
				this.healthPerUse = KoLCharacter.getLevel() * 5 + 1;
			}
			else if ( this == HPRestoreItemList.GALAKTIK )
			{
				this.purchaseCost = QuestLogRequest.galaktikCuresAvailable() ? 6 : 10;
			}
		}

		public int getHealthPerUse()
		{
			return this.healthPerUse;
		}

		public int getHealthRestored()
		{
			return Math.min( this.healthPerUse, KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() );
		}

		public int compareTo( final Object o )
		{
			// Health restores are special because skills are preferred
			// over items, so test for that first.

			HPRestoreItem hpi = (HPRestoreItem) o;

			if ( this.itemUsed == null && hpi.itemUsed != null )
			{
				return -1;
			}
			if ( this.itemUsed != null && hpi.itemUsed == null )
			{
				return 1;
			}

			float restoreAmount = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			float leftRatio = restoreAmount / this.getHealthRestored();
			float rightRatio = restoreAmount / hpi.getHealthRestored();

			// If you're comparing skills, then you compare MP cost for
			// casting the skill, with more expensive skills coming later.

			if ( this.itemUsed == null && this.skillId > 0 )
			{
				leftRatio =
					(float) ( Math.ceil( leftRatio ) * SkillDatabase.getMPConsumptionById( this.skillId ) );
				rightRatio =
					(float) ( Math.ceil( rightRatio ) * SkillDatabase.getMPConsumptionById( hpi.skillId ) );
			}
			else if ( HPRestoreItemList.purchaseBasedSort )
			{
				if ( this.purchaseCost != 0 || hpi.purchaseCost != 0 )
				{
					leftRatio = (float) Math.ceil( leftRatio ) * this.purchaseCost;
					rightRatio = (float) Math.ceil( rightRatio ) * hpi.purchaseCost;
				}
			}

			float ratioDifference = leftRatio - rightRatio;
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public void recoverHP( final int needed, final boolean purchase )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			if ( this == HPRestoreItemList.CAMPGROUND )
			{
				RequestThread.postRequest( new CampgroundRequest( "rest" ) );
				return;
			}

			if ( this == HPRestoreItemList.GALAKTIK )
			{
				if ( purchase && needed > KoLCharacter.getCurrentHP() )
				{
					RequestThread.postRequest( new GalaktikRequest( GalaktikRequest.HP, Math.min(
						needed - KoLCharacter.getCurrentHP(), KoLCharacter.getAvailableMeat() / this.purchaseCost ) ) );
				}

				return;
			}

			// For all other instances, you will need to calculate
			// the number of times this technique must be used.

			int hpShort = needed - KoLCharacter.getCurrentHP();
			if ( hpShort <= 0 )
			{
				return;
			}

			int belowMax = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			int numberToUse = Math.max( (int) Math.floor( (float) hpShort / (float) this.getHealthRestored() ), 1 );

			if ( this == HPRestoreItemList.SOFA )
			{
				RequestThread.postRequest( ( new ClanRumpusRequest( ClanRumpusRequest.SOFA ) ).setTurnCount( numberToUse ) );
				return;
			}

			else if ( SkillDatabase.contains( this.restoreName ) )
			{
				if ( !KoLCharacter.hasSkill( this.restoreName ) )
				{
					numberToUse = 0;
				}
			}
			else if ( ItemDatabase.contains( this.restoreName ) )
			{
				// In certain instances, you are able to buy more of
				// the given item from NPC stores, or from the mall.

				int numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );

				if ( purchase && numberAvailable < numberToUse )
				{
					int numberToBuy = numberAvailable;
					int unitPrice = ItemDatabase.getPriceById( this.itemUsed.getItemId() ) * 2;

					if ( this == HPRestoreItemList.HERBS && NPCStoreDatabase.contains( this.itemUsed.getName() ) )
					{
						numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, 3 );
					}
					else if ( NPCStoreDatabase.contains( this.itemUsed.getName() ) )
					{
						numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, numberToUse );
					}

					if ( !AdventureDatabase.retrieveItem( this.itemUsed.getInstance( numberToBuy ) ) )
					{
						return;
					}

					numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );
				}

				numberToUse = Math.min( numberToUse, numberAvailable );
			}

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 || !KoLmafia.permitsContinue() )
			{
				return;
			}

			if ( SkillDatabase.contains( this.restoreName ) )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( this.restoreName, "", numberToUse ) );
			}
			else
			{
				RequestThread.postRequest( new UseItemRequest( this.itemUsed.getInstance( numberToUse ) ) );
			}
		}

		public String toString()
		{
			return this.restoreName;
		}
	}
}
