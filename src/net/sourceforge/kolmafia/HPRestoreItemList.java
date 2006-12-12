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
import javax.swing.JCheckBox;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as HP buffers.
 */

public abstract class HPRestoreItemList extends StaticEntity
{
	public static final HPRestoreItem WALRUS = new HPRestoreItem( "Tongue of the Walrus", 35 );

	private static final HPRestoreItem SOFA = new HPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE );
	private static final HPRestoreItem CAMPGROUND = new HPRestoreItem( "rest at your campground", Integer.MAX_VALUE );
	private static final HPRestoreItem GALAKTIK = new HPRestoreItem( "Galaktik's Curative Nostrum", Integer.MAX_VALUE );
	private static final HPRestoreItem HERBS = new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE );
	private static final HPRestoreItem SCROLL = new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE );
	private static final HPRestoreItem OINTMENT = new HPRestoreItem( "Doc Galaktik's Ailment Ointment", 9 );

	public static final HPRestoreItem [] CONFIGURES = new HPRestoreItem []
	{
		CAMPGROUND, GALAKTIK, HERBS, SCROLL, new HPRestoreItem( "Cannelloni Cocoon", Integer.MAX_VALUE ),
		new HPRestoreItem( "phonics down", 48 ), new HPRestoreItem( "Disco Power Nap", 40 ), WALRUS,
		new HPRestoreItem( "red paisley oyster egg", 33 ), new HPRestoreItem( "red polka-dot oyster egg", 33 ),
		new HPRestoreItem( "red striped oyster egg", 33 ), new HPRestoreItem( "red pixel potion", 27 ),
		new HPRestoreItem( "maple syrup", 25 ), new HPRestoreItem( "tiny house", 22 ), new HPRestoreItem( "Disco Nap", 20 ),
		new HPRestoreItem( "Lasagna Bandages", 24 ), new HPRestoreItem( "green pixel potion", 19 ),
		new HPRestoreItem( "Doc Galaktik's Homeopathic Elixir", 19 ), new HPRestoreItem( "cast", 17 ),
		new HPRestoreItem( "Tongue of the Otter", 15 ), new HPRestoreItem( "Doc Galaktik's Restorative Balm", 14 ),
		OINTMENT, new HPRestoreItem( "forest tears", 7 ), new HPRestoreItem( "Doc Galaktik's Pungent Unguent", 3 )
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
		String hpRestoreSetting = getProperty( "hpAutoRecoveryItems" );
		JCheckBox [] restoreCheckbox = new JCheckBox[ CONFIGURES.length ];

		for ( int i = 0; i < CONFIGURES.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox( CONFIGURES[i].toString() );
			restoreCheckbox[i].setSelected( hpRestoreSetting.indexOf( CONFIGURES[i].toString().toLowerCase() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static class HPRestoreItem implements Comparable
	{
		private String restoreName;
		private int hpPerUse;

		private int skillId;
		private AdventureResult itemUsed;

		public HPRestoreItem( String restoreName, int hpPerUse )
		{
			this.restoreName = restoreName;
			this.hpPerUse = hpPerUse;

			if ( TradeableItemDatabase.contains( restoreName ) )
			{
				this.itemUsed = new AdventureResult( restoreName, 1, false );
				this.skillId = -1;
			}
			else
			{
				this.itemUsed = null;
				this.skillId = ClassSkillsDatabase.getSkillId( restoreName );
			}
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public int getHealthPerUse()
		{
			if ( this == SOFA )
			{
				// The restore rate on the rumpus room sofa changes
				// based on your current level.

				this.hpPerUse = (int) KoLCharacter.getLevel() * 5 + 1;
			}

			return hpPerUse;
		}

		public int compareTo( Object o )
		{
			// Health restores are special because skills are preferred
			// over items, so test for that first.

			HPRestoreItem hpi = (HPRestoreItem) o;

			if ( itemUsed == null && hpi.itemUsed != null )
				return -1;
			if ( itemUsed != null && hpi.itemUsed == null )
				return 1;

			float restoreAmount = (float) (KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP());
			float leftRatio = restoreAmount / ((float) getHealthPerUse());
			float rightRatio = restoreAmount / ((float) hpi.getHealthPerUse());

			// If you're comparing skills, then you compare MP cost for
			// casting the skill, with more expensive skills coming later.

			if ( itemUsed == null )
			{
				leftRatio = (float) (Math.ceil( leftRatio ) * (double) ClassSkillsDatabase.getMPConsumptionById( skillId ));
				rightRatio = (float) (Math.ceil( rightRatio ) * (double) ClassSkillsDatabase.getMPConsumptionById( hpi.skillId ));
			}

			float ratioDifference = leftRatio - rightRatio;
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public void recoverHP( int needed, boolean purchase )
		{
			if ( this == CAMPGROUND )
			{
				(new CampgroundRequest( "rest" )).run();
				return;
			}

			if ( this == GALAKTIK )
			{
				DEFAULT_SHELL.executeLine( "galaktik hp" );
				return;
			}

			// For all other instances, you will need to calculate
			// the number of times this technique must be used.

			int hpShort = needed - KoLCharacter.getCurrentHP();
			int belowMax = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			int numberToUse = (int) Math.ceil( (float) hpShort / (float) getHealthPerUse() );

			if ( this == SOFA )
			{
				(new ClanGymRequest( ClanGymRequest.SOFA )).setTurnCount( numberToUse ).run();
				return;
			}

			else if ( ClassSkillsDatabase.contains( restoreName ) )
			{
				if ( !KoLCharacter.hasSkill( restoreName ) )
					numberToUse = 0;
			}
			else if ( TradeableItemDatabase.contains( restoreName ) )
			{
				// In certain instances, you are able to buy more of
				// the given item from NPC stores, or from the mall.

				int numberAvailable = itemUsed.getCount( inventory );

				if ( purchase && numberAvailable < numberToUse )
				{
					if ( this == HERBS && NPCStoreDatabase.contains( itemUsed.getName() ) )
					{
						// If you need to buy herbs, make sure you buy enough
						// to fill your spleen.

						AdventureDatabase.retrieveItem( itemUsed.getInstance(
							Math.min( KoLCharacter.getAvailableMeat() / 100, KoLCharacter.hasSkill( "Spleen of Steel" ) ? 20 : 15 ) ) );
					}
					else if ( this == SCROLL && KoLCharacter.canInteract() )
					{
						// If you're using scrolls of drastic healing, then
						// make sure you have a little surplus.

						AdventureDatabase.retrieveItem( itemUsed.getInstance( 10 ) );
					}
					else if ( this == OINTMENT )
					{
						// For ointment, attempt to reduce the number of times
						// you buy ointment by a factor of three.

						AdventureDatabase.retrieveItem( itemUsed.getInstance(
							Math.min( KoLCharacter.getAvailableMeat() / 60, numberToUse * 3 ) ) );
					}

					numberAvailable = itemUsed.getCount( inventory );
				}

				numberToUse = Math.min( numberToUse, numberAvailable );
			}

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 )
				return;

			if ( ClassSkillsDatabase.contains( restoreName ) )
				getClient().makeRequest( UseSkillRequest.getInstance( restoreName, "", numberToUse ) );
			else
				getClient().makeRequest( new ConsumeItemRequest( itemUsed.getInstance( numberToUse ) ) );
		}

		public String toString()
		{	return restoreName;
		}
	}
}
