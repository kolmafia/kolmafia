/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class FamiliarData
	implements Comparable
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern REGISTER_PATTERN =
		Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^\"]*?)\" class=hand onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (exp|experience)?, .*? kills?\\)(.*?)<(/tr|form)" );

	private static final Pattern DESCID_PATTERN = Pattern.compile( "descitem\\((.*?)\\)" );

	private static final Pattern LOCK_PATTERN = Pattern.compile( "familiar.php\\?action=lockequip.*'This Familiar Equipment is (Locked|Unlocked)'" );

	private final int id;

	private int weight;
	private final String name, race;
	private AdventureResult item;

	public FamiliarData( final int id )
	{
		this( id, "", 1, EquipmentRequest.UNEQUIP );
	}

	public FamiliarData( final int id, final String name, final int weight, final AdventureResult item )
	{
		this.id = id;
		this.name = name;
		this.race = id == -1 ? "(none)" : FamiliarDatabase.getFamiliarName( id );

		this.weight = weight;
		this.item = item;
	}

	private FamiliarData( final Matcher dataMatcher )
	{
		this.id = StringUtilities.parseInt( dataMatcher.group( 2 ) );
		this.race = dataMatcher.group( 4 );

		FamiliarDatabase.registerFamiliar( this.id, this.race );
		FamiliarDatabase.setFamiliarImageLocation( this.id, dataMatcher.group( 1 ) );

		int kills = StringUtilities.parseInt( dataMatcher.group( 5 ) );
		this.weight = Math.max( Math.min( 20, (int) Math.sqrt( kills ) ), 1 );

		this.name = dataMatcher.group( 3 );

		String itemData = dataMatcher.group( 7 );
		this.item = parseFamiliarItem( this.id, itemData );
	}

	private static final AdventureResult parseFamiliarItem( final int id, final String text )
	{
		if ( text.indexOf( "<img" ) == -1 )
		{
			return EquipmentRequest.UNEQUIP;
		}

		Matcher itemMatcher = DESCID_PATTERN.matcher( text );
		if ( !itemMatcher.find() )
		{
			return EquipmentRequest.UNEQUIP;
		}

		String itemName = ItemDatabase.getItemName( itemMatcher.group( 1 ) );
		if ( itemName == null )
		{
			return EquipmentRequest.UNEQUIP;
		}

		return ItemPool.get( itemName, 1 );
	}

	public static final void registerFamiliarData( final String searchText )
	{
		// Assume he has no familiar
		FamiliarData firstFamiliar = null;

		Matcher familiarMatcher = FamiliarData.REGISTER_PATTERN.matcher( searchText );
		while ( familiarMatcher.find() )
		{
			FamiliarData examinedFamiliar = KoLCharacter.addFamiliar( new FamiliarData( familiarMatcher ) );

			// First in the list might be equipped
			if ( firstFamiliar == null )
			{
				firstFamiliar = examinedFamiliar;
			}
		}

		// On the other hand, he may have familiars but none are equipped.
		if ( firstFamiliar == null || searchText.indexOf( "You do not currently have a familiar" ) != -1 )
		{
			firstFamiliar = FamiliarData.NO_FAMILIAR;
		}

		KoLCharacter.setFamiliar( firstFamiliar );
		EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, firstFamiliar.getItem() );
		FamiliarData.checkLockedItem( searchText );
	}

	public static final void checkLockedItem( final String searchText )
	{
		Matcher lockMatcher = FamiliarData.LOCK_PATTERN.matcher( searchText );
		boolean locked = false;

		if ( lockMatcher.find() )
		{
			locked = lockMatcher.group(1).equals( "Locked" );
		}

		EquipmentManager.lockFamiliarItem( locked );
	}

	public int getId()
	{
		return this.id;
	}

	public void setItem( final AdventureResult item )
	{
		if ( this.id < 1 )
		{
			return;
		}

		if ( this.item != null && item != null && this.item.getItemId() == item.getItemId() )
		{
			return;
		}

		if ( !KoLmafia.isRefreshing() && this.item != null && this.item != EquipmentRequest.UNEQUIP )
		{
			AdventureResult n = ItemPool.get( this.item.getItemId(), 1 );
			AdventureResult.addResultToList( KoLConstants.inventory, n );
		}

		if ( item != null && item != EquipmentRequest.UNEQUIP )
		{
			this.item = ItemPool.get( item.getItemId(), 1 );
		}
		else
		{
			this.item = item;
		}

		if ( !KoLmafia.isRefreshing() && item != null && item != EquipmentRequest.UNEQUIP )
		{
			AdventureResult o = ItemPool.get( item.getItemId(), -1 );
			AdventureResult.addResultToList( KoLConstants.inventory, o );
		}

		if ( !KoLmafia.isRefreshing() )
		{
			switch ( this.id )
			{
			case 82:
				// Mad Hatrack
				EquipmentManager.updateEquipmentList( EquipmentManager.HAT );
				break;

			case 92:
				// Disembodied Hand
				EquipmentManager.updateEquipmentList( EquipmentManager.WEAPON );
				EquipmentManager.updateEquipmentList( EquipmentManager.OFFHAND );
				break;

			default:
				// Everything else
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
				break;
			}
			EquipmentManager.lockFamiliarItem();
		}
	}

	public AdventureResult getItem()
	{
		return this.item == null ? EquipmentRequest.UNEQUIP : this.item;
	}

	public void setWeight( final int weight )
	{
		this.weight = weight;
	}

	public int getWeight()
	{
		return this.weight;
	}

	public int getModifiedWeight()
	{
		int weight = this.weight + KoLCharacter.getFamiliarWeightAdjustment();
		float percent = KoLCharacter.getFamiliarWeightPercentAdjustment() / 100.0f;

		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		return Math.max( 1, weight );
	}

	public static final int itemWeightModifier( final int itemId )
	{
		switch ( itemId )
		{
		case -1: // bogus item id
		case 0: // another bogus item id

		case ItemPool.SHOCK_COLLAR:
		case ItemPool.MOONGLASSES:
		case ItemPool.TAM_O_SHANTER:
		case ItemPool.TARGETING_CHIP:
		case ItemPool.ANNOYING_PITCHFORK:
		case ItemPool.GRAVY_MAYPOLE:
		case ItemPool.WAX_LIPS:
		case ItemPool.NOSE_BONE_FETISH:
		case ItemPool.TEDDY_SEWING_KIT:
		case ItemPool.MINIATURE_DORMOUSE:
		case ItemPool.WEEGEE_SQOUIJA:
		case ItemPool.TAM_O_SHATNER:
		case ItemPool.BADGER_BADGE:
		case ItemPool.TUNING_FORK:
		case ItemPool.CAN_OF_STARCH:
		case ItemPool.EVIL_TEDDY_SEWING_KIT:
		case ItemPool.ANCIENT_CAROLS:
		case ItemPool.FAMILIAR_DOPPELGANGER:
		case ItemPool.ANT_HOE:
		case ItemPool.ANT_RAKE:
		case ItemPool.ANT_PITCHFORK:
		case ItemPool.ANT_SICKLE:
		case ItemPool.ANT_PICK:
		case ItemPool.PLASTIC_BIB:
		case ItemPool.TEDDY_BORG_SEWING_KIT:
		case ItemPool.FISH_SCALER:
		case ItemPool.ORIGAMI_MAGAZINE:
		case ItemPool.ZEN_MOTORCYCLE:
		case ItemPool.IMITATION_WHETSTONE:
		case ItemPool.FISHY_WAND:
		case ItemPool.MOONTAN_LOTION:
			return 0;

		case ItemPool.TINY_COSTUME_WARDROBE:
			return KoLCharacter.getFamiliar().getId() == 40 ? 25 : 0;

		case ItemPool.LEAD_NECKLACE:
			return 3;

		case ItemPool.PUMPKIN_BUCKET:
		case ItemPool.MAYFLOWER_BOUQUET:
		case ItemPool.FIREWORKS:
			return 5;

		case ItemPool.RAT_BALLOON:
			return -3;

		case ItemPool.TOY_HOVERCRAFT:
			return -5;

		case ItemPool.DAS_BOOT:
			return -10;

		case ItemPool.BATHYSPHERE:
			return -20;

		case ItemPool.MAKEUP_KIT:
		case ItemPool.PARROT_CRACKER:
			return 15;

		case ItemPool.SNOOTY_DISGUISE:
		case ItemPool.GROUCHO_DISGUISE:
			return 11;

		case ItemPool.TINY_CELL_PHONE:
			return 10;

		case ItemPool.MINIATURE_ANTLERS:
			// Depends on moon phases, somehow. Not yet spaded.
			return 0;

		default:
			return 5;
		}
	}

	public String getName()
	{
		return this.name;
	}

	public String getRace()
	{
		return this.race;
	}

	public String getImageLocation()
	{
		String image = FamiliarDatabase.getFamiliarImageLocation( this.id );
		int index = image.lastIndexOf( "/" );
		return index == -1 ? image : image.substring( index + 1 );
	}

	public boolean trainable()
	{
		if ( this.id == -1 )
		{
			return false;
		}

		int skills[] = FamiliarDatabase.getFamiliarSkills( this.id );

		// If any skill is greater than 0, we can train in that event
		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[ i ] > 0 )
			{
				return true;
			}
		}

		return false;
	}

	public String toString()
	{
		return this.id == -1 ? "(none)" : this.race + " (" + this.getModifiedWeight() + " lbs)";
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof FamiliarData && this.id == ( (FamiliarData) o ).id;
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof FamiliarData ) ? 1 : this.compareTo( (FamiliarData) o );
	}

	public int compareTo( final FamiliarData fd )
	{
		return this.race.compareToIgnoreCase( fd.race );
	}

	/**
	 * Returns whether or not the familiar can equip the given familiar
	 * item.
	 */

	public boolean canEquip( final AdventureResult item )
	{
		if ( item == null )
		{
			return false;
		}

		if ( item == EquipmentRequest.UNEQUIP )
		{
			return true;
		}

		switch ( item.getItemId() )
		{
		case -1:
		case 0:
			return false;

		case ItemPool.LEAD_NECKLACE:
		case ItemPool.TAM_O_SHANTER:
		case ItemPool.ANNOYING_PITCHFORK:
		case ItemPool.GRAVY_MAYPOLE:
		case ItemPool.RAT_BALLOON:
		case ItemPool.WAX_LIPS:
		case ItemPool.TAM_O_SHATNER:
		case ItemPool.PUMPKIN_BUCKET:
		case ItemPool.FAMILIAR_DOPPELGANGER:
		case ItemPool.MAYFLOWER_BOUQUET:
		case ItemPool.ANT_HOE:
		case ItemPool.ANT_RAKE:
		case ItemPool.ANT_PITCHFORK:
		case ItemPool.ANT_SICKLE:
		case ItemPool.ANT_PICK:
		case ItemPool.FISH_SCALER:
		case ItemPool.ORIGAMI_MAGAZINE:
		case ItemPool.FIREWORKS:
		case ItemPool.BATHYSPHERE:
		case ItemPool.DAS_BOOT:
		case ItemPool.MINIATURE_ANTLERS:
		case ItemPool.FISHY_WAND:
		case ItemPool.TINY_COSTUME_WARDROBE:
			return this.id != 54 && this.id != 82;

		case ItemPool.SNOOTY_DISGUISE:
		case ItemPool.GROUCHO_DISGUISE:
			return this.id == 45 || this.id == 63 || this.id == 78;

		case ItemPool.FOIL_BOW:
		case ItemPool.FOIL_RADAR:
			return this.id == 77;

		case ItemPool.AQUAVIOLET_JUBJUB_BIRD:
		case ItemPool.CHARPUCE_JUBJUB_BIRD:
		case ItemPool.CRIMSILION_JUBJUB_BIRD:
			return this.id == 105;

		default:
			if ( this.id == 82 )
			{
				// Mad Hatrack can wear hats
				return ItemDatabase.getConsumptionType( item.getItemId() ) == KoLConstants.EQUIP_HAT;
			}

			if ( this.id == 92 )
			{
				// Disembodied Hand can equip one-handed weapons
				// Disembodied Hand cannot equip chefstaves
				int itemId = item.getItemId();
				return ( EquipmentDatabase.getHands( itemId ) == 1 &&
					 !EquipmentDatabase.getItemType( itemId ).equals( "chefstaff" ) );
			}
			
			return item.getName().equals( FamiliarDatabase.getFamiliarItem( this.id ) );
		}
	}

	public static boolean lockableItem( final AdventureResult item )
	{
		if ( item == null || item == EquipmentRequest.UNEQUIP )
		{
			return false;
		}

		switch ( item.getItemId() )
		{
		case ItemPool.LEAD_NECKLACE:
		case ItemPool.TAM_O_SHANTER:
		case ItemPool.ANNOYING_PITCHFORK:
		case ItemPool.GRAVY_MAYPOLE:
		case ItemPool.RAT_BALLOON:
		case ItemPool.WAX_LIPS:
		case ItemPool.TAM_O_SHATNER:
		case ItemPool.PUMPKIN_BUCKET:
		case ItemPool.FAMILIAR_DOPPELGANGER:
		case ItemPool.MAYFLOWER_BOUQUET:
		case ItemPool.ANT_HOE:
		case ItemPool.ANT_RAKE:
		case ItemPool.ANT_PITCHFORK:
		case ItemPool.ANT_SICKLE:
		case ItemPool.ANT_PICK:
		case ItemPool.FISH_SCALER:
		case ItemPool.ORIGAMI_MAGAZINE:
		case ItemPool.FIREWORKS:
		case ItemPool.BATHYSPHERE:
		case ItemPool.DAS_BOOT:
		case ItemPool.MINIATURE_ANTLERS:
			return true;
		}

		return false;
	}

	public boolean isCombatFamiliar()
	{
		if ( FamiliarDatabase.isCombatType( this.id ) )
		{
			return true;
		}

		if ( this.id == 66 )
		{
			return EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName().endsWith( "whip" ) || EquipmentManager.getEquipment(
				EquipmentManager.OFFHAND ).getName().endsWith( "whip" );
		}

		return false;
	}

	public static final DefaultListCellRenderer getRenderer()
	{
		return new FamiliarRenderer();
	}

	private static class FamiliarRenderer
		extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !( value instanceof FamiliarData ) || ( (FamiliarData) value ).id == -1 )
			{
				defaultComponent.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				defaultComponent.setText( KoLConstants.VERSION_NAME + ", the 0 lb. \"No Familiar Plz\" Placeholder" );

				defaultComponent.setVerticalTextPosition( SwingConstants.CENTER );
				defaultComponent.setHorizontalTextPosition( SwingConstants.RIGHT );
				return defaultComponent;
			}

			FamiliarData familiar = (FamiliarData) value;
			defaultComponent.setIcon( FamiliarDatabase.getFamiliarImage( familiar.id ) );
			defaultComponent.setText( familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace() );

			defaultComponent.setVerticalTextPosition( SwingConstants.CENTER );
			defaultComponent.setHorizontalTextPosition( SwingConstants.RIGHT );

			return defaultComponent;
		}
	}
}
