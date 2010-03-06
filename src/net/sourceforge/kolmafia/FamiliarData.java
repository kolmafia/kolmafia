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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarData
	implements Comparable
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern REGISTER_PATTERN =
		Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^\"]*?)\" class=hand onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, .*? kills?\\)(.*?)<(?:/tr|form)" );

	private static final Pattern DESCID_PATTERN = Pattern.compile( "descitem\\((.*?)\\)" );

	private static final Pattern LOCK_PATTERN = Pattern.compile( "familiar.php\\?action=lockequip.*'This Familiar Equipment is (Locked|Unlocked)'" );

	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult DOPPELGANGER = ItemPool.get( ItemPool.FAMILIAR_DOPPELGANGER, 1 );
	public static final AdventureResult FIREWORKS = ItemPool.get( ItemPool.FIREWORKS, 1 );
	public static final AdventureResult FLOWER_BOUQUET = ItemPool.get( ItemPool.MAYFLOWER_BOUQUET, 1 );
	public static final AdventureResult LEAD_NECKLACE = ItemPool.get( ItemPool.LEAD_NECKLACE, 1 );
	public static final AdventureResult MOVEABLE_FEAST = ItemPool.get( ItemPool.MOVEABLE_FEAST, 1 );
	public static final AdventureResult PUMPKIN_BUCKET = ItemPool.get( ItemPool.PUMPKIN_BUCKET, 1 );
	public static final AdventureResult RAT_HEAD_BALLOON = ItemPool.get( ItemPool.RAT_BALLOON, 1 );
	public static final AdventureResult SUGAR_SHIELD = ItemPool.get( ItemPool.SUGAR_SHIELD, 1 );

	private final int id;
	private final String race;
	private String name;
	private int experience;
	private int weight;
	private AdventureResult item;
	private boolean feasted;
	private boolean favorite;

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
		this.feasted = false;
	}

	private FamiliarData( final Matcher dataMatcher )
	{
		this.id = StringUtilities.parseInt( dataMatcher.group( 2 ) );
		this.race = dataMatcher.group( 4 );

		FamiliarDatabase.registerFamiliar( this.id, this.race );
		FamiliarDatabase.setFamiliarImageLocation( this.id, dataMatcher.group( 1 ) );

		this.update( dataMatcher );
	}

	public final void update( final Matcher dataMatcher )
	{
		this.name = dataMatcher.group( 3 );
		this.experience = StringUtilities.parseInt( dataMatcher.group( 5 ) );
		this.setWeight();
		String itemData = dataMatcher.group( 6 );
		this.item = FamiliarData.parseFamiliarItem( this.id, itemData );
		this.favorite = itemData.indexOf( "[unfavorite]" ) != -1;
	}

	public final void addExperience( final int exp )
	{
		this.experience += exp;
		this.setWeight();
	}

	private final void setWeight()
	{
		int max = this.id == FamiliarPool.STOCKING_MIMIC ? 100 : 20;
		this.weight = Math.max( Math.min( max, (int) Math.sqrt( this.experience ) ), 1 );
	}

	public final void checkWeight( final int weight, final boolean feasted )
	{
		// Sanity check: don't adjust NO_FAMILIAR
		if ( this.id == -1 )
		{
			return;
		}

		// Called from CharPaneRequest with KoL's idea of current
		// familiar's weight and "well-fed" status.
		this.feasted = feasted;

		int delta = weight - this.getModifiedWeight();
		if ( delta != 0 )
		{
			RequestLogger.printLine( "Adjusting familiar weight by " + delta + " pound" + ( delta == 1 ? "" : "s" ) );
			this.weight += delta;
		}
	}

	public final void setName( final String name )
	{
		this.name = name;
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

	public static final void registerFamiliarData( final String responseText )
	{
		// Assume he has no familiar
		FamiliarData first = FamiliarData.NO_FAMILIAR;

		Matcher matcher = FamiliarData.REGISTER_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String race = matcher.group( 4 );
			FamiliarData familiar = KoLCharacter.findFamiliar( race );
			if ( familiar == null )
			{
				// Add new familiar to list
				familiar = new FamiliarData( matcher );
				KoLCharacter.addFamiliar( familiar );
			}
			else
			{
				// Update existing familiar
				familiar.update( matcher );
			}

			// First in the list might be equipped
			if ( first == FamiliarData.NO_FAMILIAR )
			{
				first = familiar;
			}
		}

		// He may have familiars but none are equipped.
		if ( responseText.indexOf( "You do not currently have a familiar" ) != -1 )
		{
			first = FamiliarData.NO_FAMILIAR;
		}

		KoLCharacter.setFamiliar( first );
		EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, first.getItem() );
		FamiliarData.checkLockedItem( responseText );

		// If we discovered new familiars, write an override file
		if ( FamiliarDatabase.newFamiliars )
		{
			FamiliarDatabase.saveDataOverride();
		}
	}

	public static final void checkLockedItem( final String responseText )
	{
		Matcher lockMatcher = FamiliarData.LOCK_PATTERN.matcher( responseText );
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
			case FamiliarPool.HATRACK:
				// Mad Hatrack
				EquipmentManager.updateEquipmentList( EquipmentManager.HAT );
				break;

			case FamiliarPool.HAND:
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
                // Start with base weight of familiar
		int weight = this.weight;

		// Get current fixed and percent weight modifiers
		Modifiers current = KoLCharacter.getCurrentModifiers();
		float fixed = current.get( Modifiers.FAMILIAR_WEIGHT ) + current.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
		float percent = current.get( Modifiers.FAMILIAR_WEIGHT_PCT );

		// If this is not the current familiar, adjust modifiers to
		// reflect this familiar's current equipment.
		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( this != familiar )
		{
			// Subtract modifiers for current familiar's equipment
			AdventureResult item = familiar.getItem();
			if ( item != EquipmentRequest.UNEQUIP )
			{
				Modifiers mods = Modifiers.getModifiers( item.getName() );
				if ( mods != null )
				{
					fixed -= mods.get( Modifiers.FAMILIAR_WEIGHT );
					fixed -= mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					percent -= mods.get( Modifiers.FAMILIAR_WEIGHT_PCT );
				}
			}

			// Add modifiers for this familiar's equipment
			item = this.getItem();
			if ( item != EquipmentRequest.UNEQUIP )
			{
				Modifiers mods = Modifiers.getModifiers( item.getName() );
				if ( mods != null )
				{
					fixed += mods.get( Modifiers.FAMILIAR_WEIGHT );
					fixed += mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					percent += mods.get( Modifiers.FAMILIAR_WEIGHT_PCT );
				}
			}
		}

		// Add in fixed modifiers
		weight += (int) fixed;

                // Adjust by percent modifiers
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		// If the familiar is well-fed, it's 10 lbs. heavier
		if ( this.feasted )
		{
			weight += 10;
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
		case ItemPool.CONTACT_LENSES:
		case ItemPool.AMPHIBIOUS_TOPHAT:
		case ItemPool.BAG_OF_MANY_CONFECTIONS:
		case ItemPool.PRETTY_PINK_BOW:
		case ItemPool.SMILEY_FACE_STICKER:
		case ItemPool.FARFALLE_BOW_TIE:
		case ItemPool.JALAPENO_SLICES:
		case ItemPool.SOLAR_PANELS:
		case ItemPool.TINY_SOMBRERO:
			return 0;

		case ItemPool.TINY_COSTUME_WARDROBE:
			return KoLCharacter.getFamiliar().getId() == FamiliarPool.DOPPEL ? 25 : 0;

		case ItemPool.LEAD_NECKLACE:
			return 3;

		case ItemPool.PUMPKIN_BUCKET:
		case ItemPool.MAYFLOWER_BOUQUET:
		case ItemPool.FIREWORKS:
		case ItemPool.MOVEABLE_FEAST:
		case ItemPool.ITTAH_BITTAH_HOOKAH:
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
		case ItemPool.SUGAR_SHIELD:
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

	public boolean getFavorite()
	{
		return this.favorite;
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

	public boolean waterBreathing()
	{
		switch ( this.id )
		{
		case -1:	// No familiar
		case FamiliarPool.BARRRNACLE:
		case FamiliarPool.EMO_SQUID:
		case FamiliarPool.CUDDLEFISH:
		case FamiliarPool.CRAB:
		case FamiliarPool.DRAGONFISH:
		case FamiliarPool.CLOWNFISH:
		case FamiliarPool.LOBSTER:
		case FamiliarPool.GIBBERER:
		case FamiliarPool.GROUPIE:
		case FamiliarPool.URCHIN:
			return true;
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
		case ItemPool.SUGAR_SHIELD:
		case ItemPool.MOVEABLE_FEAST:
		case ItemPool.ITTAH_BITTAH_HOOKAH:
			return this.id != FamiliarPool.CHAMELEON &&
			       this.id != FamiliarPool.HATRACK;

		case ItemPool.SNOOTY_DISGUISE:
		case ItemPool.GROUCHO_DISGUISE:
			return this.id == FamiliarPool.PET_ROCK ||
			       this.id == FamiliarPool.TOOTHSOME_ROCK ||
			       this.id == FamiliarPool.BUDDY_BOX;

		case ItemPool.FOIL_BOW:
		case ItemPool.FOIL_RADAR:
			return this.id == FamiliarPool.PRESSIE;

		case ItemPool.AQUAVIOLET_JUBJUB_BIRD:
		case ItemPool.CHARPUCE_JUBJUB_BIRD:
		case ItemPool.CRIMSILION_JUBJUB_BIRD:
			return this.id == FamiliarPool.BANDER;

		case ItemPool.AMPHIBIOUS_TOPHAT:
			return this.id == FamiliarPool.DANCING_FROG;

		default:
			if ( this.id == FamiliarPool.HATRACK )
			{
				// Mad Hatrack can wear hats
				return ItemDatabase.getConsumptionType( item.getItemId() ) == KoLConstants.EQUIP_HAT;
			}

			if ( this.id == FamiliarPool.HAND )
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
		case ItemPool.FISHY_WAND:
		case ItemPool.TINY_COSTUME_WARDROBE:
		case ItemPool.SUGAR_SHIELD:
		case ItemPool.MOVEABLE_FEAST:
		case ItemPool.ITTAH_BITTAH_HOOKAH:
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

		if ( this.id == FamiliarPool.DANDY_LION )
		{
			return EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName().endsWith( "whip" ) ||
			       EquipmentManager.getEquipment( EquipmentManager.OFFHAND ).getName().endsWith( "whip" );
		}

		return false;
	}

	public final void findAndWearItem( boolean steal )
	{
		AdventureResult use = this.findGoodItem( steal );
		if ( use != null )
		{
			RequestThread.postRequest( new EquipmentRequest( use, EquipmentManager.FAMILIAR ) );
		}
	}

	public final AdventureResult findGoodItem( boolean steal )
	{
		if ( FamiliarData.availableItem( FamiliarData.PUMPKIN_BUCKET, steal ) )
		{
			return FamiliarData.PUMPKIN_BUCKET;
		}

		if ( FamiliarData.availableItem( FamiliarData.FIREWORKS, steal ) )
		{
			return FamiliarData.FIREWORKS;
		}

		if ( FamiliarData.availableItem( FamiliarData.FLOWER_BOUQUET, steal ) )
		{
			return FamiliarData.FLOWER_BOUQUET;
		}

		if ( FamiliarData.availableItem( FamiliarData.MOVEABLE_FEAST, steal ) )
		{
			return FamiliarData.MOVEABLE_FEAST;
		}

		int itemId = FamiliarDatabase.getFamiliarItemId( this.id );
		AdventureResult item = itemId > 0 ? ItemPool.get( itemId, 1 ) : null;
		if ( item != null && FamiliarData.availableItem( item, false ) )
		{
			return item;
		}

		if ( FamiliarData.availableItem( FamiliarData.LEAD_NECKLACE, steal ) )
		{
			return FamiliarData.LEAD_NECKLACE;
		}

		return null;
	}

	private static final boolean availableItem( AdventureResult item, boolean steal )
	{
		if ( item.getCount( KoLConstants.inventory ) > 0 )
		{
			return true;
		}

		if ( !steal )
		{
			return false;
		}

		FamiliarData current = KoLCharacter.getFamiliar();
		List familiars = KoLCharacter.getFamiliarList();
		int count = familiars.size();
		for ( int i = 0; i < count; ++i )
		{
			FamiliarData familiar = (FamiliarData) familiars.get( i );
			if ( !familiar.equals( current ) )
			{
				AdventureResult equipped = familiar.getItem();
				if ( equipped != null && equipped.equals( item ) )
				{
					return true;
				}
			}
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
