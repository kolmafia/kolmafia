/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.Iterator;
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

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.Type69Request;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarData
	implements Comparable<FamiliarData>
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern REGISTER_PATTERN =
		Pattern.compile( "<img(?<!:</td><td><img) src=\"http://images\\.kingdomofloathing\\.com/itemimages/([^\"]*?)\" class=(?:\"hand fam\"|hand) onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, .*? kills?\\)(.*?)<(?:/tr|form)" );

	private static final Pattern DESCID_PATTERN = Pattern.compile( "descitem\\((.*?)\\)" );

	private static final Pattern LOCK_PATTERN = Pattern.compile( "familiar.php\\?action=lockequip.*'This Familiar Equipment is (Locked|Unlocked)'" );

	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult DOPPELGANGER = ItemPool.get( ItemPool.FAMILIAR_DOPPELGANGER, 1 );
	public static final AdventureResult FIREWORKS = ItemPool.get( ItemPool.FIREWORKS, 1 );
	public static final AdventureResult FLOWER_BOUQUET = ItemPool.get( ItemPool.MAYFLOWER_BOUQUET, 1 );
	public static final AdventureResult ITTAH_BITTAH_HOOKAH = ItemPool.get( ItemPool.ITTAH_BITTAH_HOOKAH, 1 );
	public static final AdventureResult LEAD_NECKLACE = ItemPool.get( ItemPool.LEAD_NECKLACE, 1 );
	public static final AdventureResult MOVEABLE_FEAST = ItemPool.get( ItemPool.MOVEABLE_FEAST, 1 );
	public static final AdventureResult PET_SWEATER = ItemPool.get( ItemPool.PET_SWEATER, 1 );
	public static final AdventureResult PUMPKIN_BUCKET = ItemPool.get( ItemPool.PUMPKIN_BUCKET, 1 );
	public static final AdventureResult RAT_HEAD_BALLOON = ItemPool.get( ItemPool.RAT_BALLOON, 1 );
	public static final AdventureResult SUGAR_SHIELD = ItemPool.get( ItemPool.SUGAR_SHIELD, 1 );

	private final int id;
	private final String race;
	private boolean beeware;
	private String name;
	private int experience;
	private int weight;
	private AdventureResult item;
	private boolean feasted;
	private boolean favorite;
	private int charges;

	public FamiliarData( final int id )
	{
		this( id, "", 1, EquipmentRequest.UNEQUIP );
	}

	public FamiliarData( final int id, final String name, final int weight, final AdventureResult item )
	{
		this.id = id;
		this.name = name;
		this.race = id == -1 ? "(none)" : FamiliarDatabase.getFamiliarName( id );
		this.beeware = this.race != null && this.race.contains( "b" ) || this.race.contains( "B" );

		this.weight = weight;
		this.item = item;
		this.feasted = false;
		this.charges = 0;
	}

	private FamiliarData( final Matcher dataMatcher )
	{
		this.id = StringUtilities.parseInt( dataMatcher.group( 2 ) );
		this.race = dataMatcher.group( 4 );
		this.beeware = this.race.indexOf( "b" ) != -1 || this.race.indexOf( "B" ) != -1;

		String image = dataMatcher.group( 1 );
		FamiliarDatabase.registerFamiliar( this.id, this.race, image );

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

	public final boolean canEquip()
	{
		// Familiars cannot be equipped in when you are an Avatar of Boris
		if ( KoLCharacter.inAxecore() || KoLCharacter.isJarlsberg() || KoLCharacter.isSneakyPete() )
		{
			return false;
		}

		// Familiars with a "B" in their race cannot be equipped in Beecore
		if ( KoLCharacter.inBeecore() && this.beeware )
		{
			return false;
		}

		// Unallowed familiars cannot be equipped
		if ( !Type69Request.isAllowed( "Familiars", this.race ) )
		{
			return false;
		}

		return true;
	}

	public final int getTotalExperience()
	{
		return this.experience;
	}

	public final void addCombatExperience( String responseText )
	{
		if ( this.id == FamiliarPool.STOCKING_MIMIC ) 
		{
			// Doesn't automatically gain experience from winning a combat
			return;
		}

		double experienceModifier = KoLCharacter.currentNumericModifier( Modifiers.FAMILIAR_EXP );

		if ( getItem().getItemId() == ItemPool.MAYFLOWER_BOUQUET )
		{
			String itemName = getItem().getName();
			String modifierName = Modifiers.getModifierName( Modifiers.FAMILIAR_EXP );
			double itemModifier = Modifiers.getNumericModifier( itemName, modifierName );

			experienceModifier -= itemModifier;

			if ( responseText.indexOf( "offer some words of encouragement and support" ) != -1 )
			{
				experienceModifier += 3;
			}
		}

		this.experience += 1 + experienceModifier;
		
		if ( KoLCharacter.hasSkill( "Testudinal Teachings" ) )
		{
			this.addTestTeachExperience();
		}

		this.setWeight();
	}

	public final void addNonCombatExperience( int exp )
	{
		this.experience += exp;

		this.setWeight();
	}

	public final void addTestTeachExperience()
	{
		String rawTTPref = Preferences.getString( "testudinalTeachings" );
		String[] splitTTPref = rawTTPref.split( "\\|" );

		// Check Familiar Testudinal Teachings experience	
		for ( int i = 0 ; i < splitTTPref.length ; ++i )
		{
			String[] it = splitTTPref[ i ].split( ":" );
			if ( it.length == 2 )
			{
				if ( this.id == Integer.parseInt( it[ 0 ] ) )
				{
					int newCount = Integer.parseInt( it[ 1 ] ) + 1;
					if ( newCount >= 6 )
					{
						this.experience++;
						newCount = 0;
					}
					String newTTProperty = it[ 0 ] + ":" + String.valueOf( newCount );
					String newTTPref = StringUtilities.globalStringReplace( rawTTPref,
						splitTTPref[ i ], newTTProperty );
					Preferences.setString( "testudinalTeachings", newTTPref );
					return;
				}					
			}
		}
		
		// Familiar not found, so add it
		String delimiter = "";
		if ( rawTTPref.length() > 0 )
		{
			delimiter = "|";
		}
		String newTTPref = rawTTPref + delimiter + String.valueOf( this.id ) + ":1";
		Preferences.setString( "testudinalTeachings", newTTPref );
	}
	
	public final void recognizeCombatUse()
	{
		int singleFamiliarRun = getSingleFamiliarRun();

		if ( singleFamiliarRun == 0 )
		{
			Preferences.setInteger( "singleFamiliarRun", this.id );
		}
		else if ( this.id != singleFamiliarRun )
		{
			Preferences.setInteger( "singleFamiliarRun", -1 );
		}
	}

	public final boolean isUnexpectedFamiliar()
	{
		if ( this.id == -1 && KoLCharacter.getCurrentRun() == 0 )
		{
			return true;
		}

		int singleFamiliarRun = getSingleFamiliarRun();

		return singleFamiliarRun > 0 && this.id != singleFamiliarRun;
	}

	public static final int getSingleFamiliarRun()
	{
		int singleFamiliarRun = Preferences.getInteger( "singleFamiliarRun" );

		if ( singleFamiliarRun == 0 )
		{
			Iterator familiarIterator = KoLCharacter.getFamiliarList().iterator();

			while ( familiarIterator.hasNext() )
			{
				FamiliarData familiar = (FamiliarData) familiarIterator.next();

				if ( familiar.getTotalExperience() != 0 )
				{
					if ( singleFamiliarRun != 0 )
					{
						singleFamiliarRun = -1;
						break;
					}

					singleFamiliarRun = familiar.getId();
				}
			}

			Preferences.setInteger( "singleFamiliarRun", singleFamiliarRun );
		}

		return singleFamiliarRun;
	}

	private final void setWeight()
	{
		int max = this.id == FamiliarPool.STOCKING_MIMIC ? 100 : 20;
		this.weight = Math.max( Math.min( max, (int) Math.sqrt( this.experience ) ), 1 );
	}

	public final void checkWeight( final int weight, final boolean feasted )
	{
		// Called from CharPaneRequest with KoL's idea of current familiar's weight and "well-fed" status.
		// This does NOT include "hidden" weight modifiers

		// Sanity check: don't adjust NO_FAMILIAR
		if ( this.id == -1 )
		{
			return;
		}

		this.feasted = feasted;

		// Get modified weight excluding hidden weight modifiers
		int delta = weight - this.getModifiedWeight( false, true );
		if ( delta != 0 )
		{
			// The following is informational, not an error, but it confuses people, so don't print it.
			// RequestLogger.printLine( "Adjusting familiar weight by " + delta + " pound" + ( delta == 1 ? "" : "s" ) );
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
		FamiliarData hatseat = null;
		FamiliarData buddy = null;

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

			if ( matcher.group( 6 ).contains( "kick out of Crown of Thrones" ) )
			{
				hatseat = familiar;
			}
			else if ( matcher.group( 6 ).contains( "kick out of Buddy Bjorn" ) )
			{
				buddy = familiar;
			}

			// First in the list might be equipped
			if ( first == FamiliarData.NO_FAMILIAR )
			{
				first = familiar;
			}
		}

		// He may have familiars but none are equipped.
		if ( responseText.contains( "You do not currently have a familiar" ) )
		{
			first = FamiliarData.NO_FAMILIAR;
		}
		else if ( first != FamiliarData.NO_FAMILIAR )
		{	// There's no indication of whether your current familiar is a
			// favorite or not.  Safest to assume it is:
			first.setFavorite( true );
		}

		KoLCharacter.setFamiliar( first );
		EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, first.getItem() );
		FamiliarData.checkLockedItem( responseText );
		if ( hatseat != null ) KoLCharacter.setEnthroned( hatseat );
		if ( buddy != null ) KoLCharacter.setBjorned( buddy );
	}

	public static final FamiliarData registerFamiliar( final int id, final int experience )
	{
		if ( id == 0 )
		{
			return FamiliarData.NO_FAMILIAR;
		}

		FamiliarData familiar = KoLCharacter.findFamiliar( id );
		if ( familiar == null )
		{
			// Add new familiar to list
			familiar = new FamiliarData( id );
			KoLCharacter.addFamiliar( familiar );
		}

		familiar.experience = experience;
		familiar.setWeight();
		return familiar;
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

	public boolean getFeasted()
	{
		return this.feasted;
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

			case FamiliarPool.SCARECROW:
				// Fancypants Scarecrow
				EquipmentManager.updateEquipmentList( EquipmentManager.PANTS );
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
		return this.getModifiedWeight( true, true );
	}

	public int getModifiedWeight( final boolean includeEquipment)
	{
		return this.getModifiedWeight( true, includeEquipment );
	}

	private int getModifiedWeight( final boolean includeHidden, final boolean includeEquipment )
	{
		// Start with base weight of familiar
		int weight = this.weight;

		// Get current fixed and percent weight modifiers
		Modifiers current = KoLCharacter.getCurrentModifiers();
		double fixed = current.get( Modifiers.FAMILIAR_WEIGHT );
		double hidden = current.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
		double percent = current.get( Modifiers.FAMILIAR_WEIGHT_PCT );

		FamiliarData familiar = KoLCharacter.getFamiliar();

		// If this is not the current familiar or we are not
		// considering equipment, subtract weight granted by equipment
		if ( this != familiar || !includeEquipment )
		{
			// Subtract modifiers for current familiar's equipment
			AdventureResult item = familiar.getItem();
			if ( item != EquipmentRequest.UNEQUIP )
			{
				Modifiers mods = Modifiers.getModifiers( item.getName() );
				if ( mods != null )
				{
					fixed -= mods.get( Modifiers.FAMILIAR_WEIGHT );
					hidden -= mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					percent -= mods.get( Modifiers.FAMILIAR_WEIGHT_PCT );
				}
			}
		}

		// If this is not the current familiar and we are considering
		// equipment, add weight granted by equipment.
		if ( this != familiar && includeEquipment )
		{
			// Add modifiers for this familiar's equipment
			item = this.getItem();
			if ( item != EquipmentRequest.UNEQUIP )
			{
				Modifiers mods = Modifiers.getModifiers( item.getName() );
				if ( mods != null )
				{
					fixed += mods.get( Modifiers.FAMILIAR_WEIGHT );
					hidden += mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					percent += mods.get( Modifiers.FAMILIAR_WEIGHT_PCT );
				}
			}
		}

		// Add in fixed modifiers
		weight += (int) fixed;

		// If want to include hidden modifiers, do so now
		if ( includeHidden )
		{
			weight += (int) hidden;
		}

		// Adjust by percent modifiers
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * (percent / 100.0f) );
		}

		// If the familiar is well-fed, it's 10 lbs. heavier
		if ( this.feasted )
		{
			weight += 10;
		}

		// check if the familiar has a weight cap
		int cap = (int) current.get( Modifiers.FAMILIAR_WEIGHT_CAP );
		int cappedWeight = ( cap == 0 ) ? weight : Math.min( weight, cap );

		return Math.max( 1, cappedWeight );
	}

	public static final int itemWeightModifier( final int itemId )
	{
		Modifiers mods = Modifiers.getModifiers( ItemDatabase.getItemName( itemId ) );
		return mods == null ? 0 : (int) mods.get( Modifiers.FAMILIAR_WEIGHT );
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

	public void setFavorite( boolean favor )
	{
		this.favorite = favor;
	}

	public String getImageLocation()
	{
		String image = FamiliarDatabase.getFamiliarImageLocation( this.id );
		int index = image.lastIndexOf( "/" );
		return index == -1 ? image : image.substring( index + 1 );
	}

	public void setCharges( int charges )
	{
		this.charges = charges;
	}

	public int getCharges()
	{
		return this.charges;
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
		Modifiers mods = Modifiers.getModifiers( "fam:" + this.getRace() );
		return mods != null && mods.getBoolean( Modifiers.UNDERWATER_FAMILIAR );
	}

	public boolean canCarry()
	{
		switch ( this.id )
		{
		case FamiliarPool.DOPPEL:
		case FamiliarPool.CHAMELEON:
		case FamiliarPool.HATRACK:
		case FamiliarPool.HAND:
		case FamiliarPool.SCARECROW:
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return this.id == -1 ? "(none)" : this.race + " (" + this.getModifiedWeight() + " lbs)";
	}

	@Override
	public boolean equals( final Object o )
	{
		return o != null && o instanceof FamiliarData && this.id == ( (FamiliarData) o ).id;
	}

	@Override
	public int hashCode()
	{
		return this.id;
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

		int itemId = item.getItemId();
		if ( itemId <= 0 )
		{
			return false;
		}

		switch ( this.id )
		{
		case -1:
			return false;

		case FamiliarPool.CHAMELEON:
			return false;

		case FamiliarPool.HATRACK:
			return itemId != ItemPool.HATSEAT && ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_HAT;

		case FamiliarPool.HAND:
			// Disembodied Hand can't equip Mainhand only items
			if ( !EquipmentDatabase.isMainhandOnly( itemId ) )
			{
				return true;
			}
			break;

		case FamiliarPool.SCARECROW:
			return ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_PANTS;
		}

		if ( itemId == FamiliarDatabase.getFamiliarItemId( this.id ) )
		{
			return true;
		}

		String name = item.getName();

		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return false;
		}
		if ( mods.getBoolean( Modifiers.GENERIC ) )
		{
			return true;
		}

		String others = mods.getString( Modifiers.EQUIPS_ON );
		if ( others == null || others.equals( "" ) )
		{
			return false;
		}
		String[] pieces = others.split( "\\s*\\|\\s*" );
		for ( int i = pieces.length - 1; i >= 0; --i )
		{
			if ( pieces[ i ].equals( this.getRace() ) )
			{
				return true;
			}
		}
		return false;
	}

	public static boolean lockableItem( final AdventureResult item )
	{
		if ( item == null || item == EquipmentRequest.UNEQUIP )
		{
			return false;
		}

		Modifiers mods = Modifiers.getModifiers( item.getName() );
		return mods != null && mods.getBoolean( Modifiers.GENERIC );
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
		if ( FamiliarData.availableItem( FamiliarData.PET_SWEATER, steal ) )
		{
			return FamiliarData.PET_SWEATER;
		}

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

		if ( FamiliarData.availableItem( FamiliarData.ITTAH_BITTAH_HOOKAH, steal ) )
		{
			return FamiliarData.ITTAH_BITTAH_HOOKAH;
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

	/**
	 * Calculates the number of combats with a Slimeling required for the
	 * nth slime stack in an ascension to drop.
	 * @param n the number of the slime stack (reset to zero on ascension)
	 * @return the number of combats
	 */
	public static int getSlimeStackTurns( final int n )
	{
		return n * ( n + 1 ) / 2;
	}

	public static final DefaultListCellRenderer getRenderer()
	{
		return new FamiliarRenderer();
	}

	private static class FamiliarRenderer
		extends DefaultListCellRenderer
	{
		@Override
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
