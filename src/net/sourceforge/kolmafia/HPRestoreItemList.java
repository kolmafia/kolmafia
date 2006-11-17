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

	public static class HPRestoreItem
	{
		private String itemName;
		private int hpPerUse;
		private AdventureResult itemUsed;

		public HPRestoreItem( String itemName, int hpPerUse )
		{
			this.itemName = itemName;
			this.hpPerUse = hpPerUse;

			if ( TradeableItemDatabase.contains( itemName ) )
				this.itemUsed = new AdventureResult( itemName, 1, false );
			else
				this.itemUsed = null;
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public int getHealthPerUse()
		{	return hpPerUse;
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
			int numberToUse = (int) Math.ceil( (float) hpShort / (float) hpPerUse );

			if ( ClassSkillsDatabase.contains( itemName ) )
			{
				if ( !KoLCharacter.hasSkill( itemName ) )
					numberToUse = 0;
			}
			else if ( TradeableItemDatabase.contains( itemName ) )
			{
				// In certain instances, you are able to buy more of
				// the given item from NPC stores, or from the mall.

				int numberAvailable = itemUsed.getCount( inventory );

				if ( purchase )
				{
					if ( this == HERBS )
						numberAvailable = belowMax < 20 || !NPCStoreDatabase.contains( HERBS.toString() ) ? 0 : 1;
					else if ( this == SCROLL && KoLCharacter.canInteract() )
						numberAvailable = 1;
					else if ( this == OINTMENT )
						numberAvailable = numberToUse;
				}

				numberToUse = Math.min( numberToUse, numberAvailable );
			}

			if ( numberToUse <= 0 )
				return;

			if ( ClassSkillsDatabase.contains( itemName ) )
				getClient().makeRequest( UseSkillRequest.getInstance( itemName, "", numberToUse ) );
			else
				getClient().makeRequest( new ConsumeItemRequest( itemUsed.getInstance( numberToUse ) ) );
		}

		public String toString()
		{	return itemName;
		}
	}
}
