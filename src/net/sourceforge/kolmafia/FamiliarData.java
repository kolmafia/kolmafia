/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.ArrayList;
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
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarData
	implements Comparable<FamiliarData>
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	// <center>Current Familiar:<br><img src="http://images.kingdomofloathing.com/itemimages/jungman.gif" class=hand onClick='fam(165)'><br><b>Jung Grrl</b><br>20-pound Angry Jung Man (500 experience, 29,380 kills)<table><tr><td valign=center>Equipment:</td><td><img src="http://images.kingdomofloathing.com/itemimages/antsickle.gif" class=hand onClick='descitem(235040244)'></td><td valign=center>ant sickle <font size=1><a href='inv_equip.php?pwd=4438585275374d322da30a77b73cb7d5&action=unequip&type=familiarequip&terrarium=1'>[unequip]</a></font></td><td><a href='familiar.php?action=lockequip&pwd=4438585275374d322da30a77b73cb7d5'><img src="http://images.kingdomofloathing.com/itemimages/openpadlock.gif" class=hand title='This Familiar Equipment is Unlocked'></a></td><td valign=top><font size=-1><b><a class=nounder href='javascript:doc("famequiplock");'>?</a></b></font></td></tr></table><p><form name=rename action=familiar.php method=post><input type=hidden name=action value="rename"><input type=hidden name=pwd value='4438585275374d322da30a77b73cb7d5'>Change your Familiar's Name:<br><input class=text type=text size=40 maxlength=40 name=newname value="Jung Grrl"> <input class=button type=submit value="Rename"></form>

	private static final Pattern CURRENT_PATTERN =
		Pattern.compile( "Current Familiar:.*?</form>" );

	// <tr class="frow expired" data-stats="1" data-other="1"><td valign=center>&nbsp;</td><td valign=center><img src="http://images.kingdomofloathing.com/itemimages/crayongoth.gif" class="hand fam" onClick='fam(160)'></td><td valign=top style='padding-top: .45em;'><b>Raven 'Raven' Ravengrrl</b>, the 1-pound Artistic Goth Kid (0 exp, 32,443 kills) <font size="1"><br />&nbsp;&nbsp;&nbsp;&nbsp;<a class="fave" href="familiar.php?group=0&action=fave&famid=160&pwd=4438585275374d322da30a77b73cb7d5">[unfavorite]</a></font></td></tr>

	private static final Pattern FROW_PATTERN =
		Pattern.compile( "<tr class=\"frow .*?</tr>" );

	private static final Pattern FAMILIAR_PATTERN =
		Pattern.compile( ".*?<img src=\"http://images\\.kingdomofloathing\\.com/itemimages/([^\"]*?)\" class=(?:\"hand fam\"|hand) onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, .*? kills?\\)(.*?)<(?:/tr|form)" );

	private static final Pattern DESCID_PATTERN = Pattern.compile( "descitem\\((.*?)\\)" );
	private static final Pattern SHRUB_TOPPER_PATTERN = Pattern.compile( "span title=\"(.*?)-heavy" );
	private static final Pattern SHRUB_LIGHT_PATTERN = Pattern.compile( "Deals (.*?) damage" );

	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult DOPPELGANGER = ItemPool.get( ItemPool.FAMILIAR_DOPPELGANGER, 1 );
	public static final AdventureResult FIREWORKS = ItemPool.get( ItemPool.FIREWORKS, 1 );
	public static final AdventureResult FLOWER_BOUQUET = ItemPool.get( ItemPool.MAYFLOWER_BOUQUET, 1 );
	public static final AdventureResult ITTAH_BITTAH_HOOKAH = ItemPool.get( ItemPool.ITTAH_BITTAH_HOOKAH, 1 );
	public static final AdventureResult LEAD_NECKLACE = ItemPool.get( ItemPool.LEAD_NECKLACE, 1 );
	public static final AdventureResult LIFE_PRESERVER = ItemPool.get( ItemPool.MINI_LIFE_PRESERVER, 1 );
	public static final AdventureResult MOVEABLE_FEAST = ItemPool.get( ItemPool.MOVEABLE_FEAST, 1 );
	public static final AdventureResult PET_SWEATER = ItemPool.get( ItemPool.PET_SWEATER, 1 );
	public static final AdventureResult PUMPKIN_BUCKET = ItemPool.get( ItemPool.PUMPKIN_BUCKET, 1 );
	public static final AdventureResult RAT_HEAD_BALLOON = ItemPool.get( ItemPool.RAT_BALLOON, 1 );
	public static final AdventureResult SUGAR_SHIELD = ItemPool.get( ItemPool.SUGAR_SHIELD, 1 );

	public static final List<DropInfo> DROP_FAMILIARS = new ArrayList<DropInfo>();
	public static final List<FightInfo> FIGHT_FAMILIARS = new ArrayList<FightInfo>();

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
		String race = FamiliarDatabase.getFamiliarName( id );
		this.race = ( id == -1 || race == null ) ? "(none)" : race;
		this.beeware = this.race.contains( "b" ) || this.race.contains( "B" );

		this.weight = weight;
		this.item = item;
		this.feasted = false;
		this.charges = 0;
	}

	private FamiliarData( final Matcher dataMatcher )
	{
		this.id = StringUtilities.parseInt( dataMatcher.group( 2 ) );
		this.race = dataMatcher.group( 4 );
		this.beeware = this.race.contains( "b" ) || this.race.contains( "B" );

		String image = dataMatcher.group( 1 );
		FamiliarDatabase.registerFamiliar( this.id, this.race, image );

		this.update( dataMatcher );
	}

	private final void update( final Matcher dataMatcher )
	{
		this.name = dataMatcher.group( 3 );
		this.experience = StringUtilities.parseInt( dataMatcher.group( 5 ) );
		this.setWeight();
		String itemData = dataMatcher.group( 6 );
		this.item = FamiliarData.parseFamiliarItem( this.id, itemData );
		this.favorite = itemData.contains( "[unfavorite]" );
	}

	public static final void reset()
	{
		FamiliarData.loadDropFamiliars();
		FamiliarData.loadFightFamiliars();
		FamiliarData.checkShrub();
	}

	public final boolean canEquip()
	{
		// Familiars cannot be equipped by most Avatar classes
		if ( KoLCharacter.inAxecore() || KoLCharacter.isJarlsberg() || KoLCharacter.isSneakyPete() || KoLCharacter.isEd() )
		{
			return false;
		}

		// Familiars with a "B" in their race cannot be equipped in Beecore
		if ( KoLCharacter.inBeecore() && this.beeware )
		{
			return false;
		}

		// Unallowed familiars cannot be equipped
		if ( !StandardRequest.isAllowed( "Familiars", this.race ) )
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

		int itemId = getItem().getItemId();
		if ( itemId == ItemPool.MAYFLOWER_BOUQUET )
		{
			String modifierName = Modifiers.getModifierName( Modifiers.FAMILIAR_EXP );
			double itemModifier = Modifiers.getNumericModifier( "Item", itemId, modifierName );

			experienceModifier -= itemModifier;

			if ( responseText.contains( "offer some words of encouragement and support" ) )
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
		for ( int i = 0; i < splitTTPref.length; ++i )
		{
			String[] it = splitTTPref[ i ].split( ":" );
			if ( it.length == 2 )
			{
				if ( this.id == Integer.parseInt( it[ 0 ] ) )
				{
					int newCount = Integer.parseInt( it[ 1 ] ) + 1;
					if ( newCount >= 6 )
					{
						this.experience++ ;
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
			for ( FamiliarData familiar : KoLCharacter.getFamiliarList() )
			{
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
		if ( !text.contains( "<img" ) )
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
		FamiliarData current = FamiliarData.NO_FAMILIAR;

		if ( !responseText.contains( "You do not currently have a familiar" ) )
		{
			Matcher currentMatcher = FamiliarData.CURRENT_PATTERN.matcher( responseText );
			if ( currentMatcher.find() )
			{
				Matcher familiarMatcher = FamiliarData.FAMILIAR_PATTERN.matcher( currentMatcher.group() );
				if ( familiarMatcher.find() )
				{
					current = FamiliarData.registerFamiliar( familiarMatcher );
					// There's no indication of whether your current familiar is a
					// favorite or not.  Safest to assume it is:
					current.setFavorite( true );
				}
			}
		}

		Matcher frowMatcher = FamiliarData.FROW_PATTERN.matcher( responseText );
		while ( frowMatcher.find() )
		{
			String frow = frowMatcher.group();
			if ( frow.contains( "\"frow expired\"" ) )
			{
				continue;
			}

			Matcher familiarMatcher = FamiliarData.FAMILIAR_PATTERN.matcher( frow );
			if ( !familiarMatcher.find() )
			{
				continue;
			}

			FamiliarData familiar = FamiliarData.registerFamiliar( familiarMatcher );

			if ( frow.contains( "kick out of Crown of Thrones" ) )
			{
				KoLCharacter.setEnthroned( familiar );
			}
			else if ( frow.contains( "kick out of Buddy Bjorn" ) )
			{
				KoLCharacter.setBjorned( familiar );
			}
		}

		int currentId = current.getId();
		if ( currentId == FamiliarPool.REANIMATOR && currentId != KoLCharacter.getFamiliar().getId() )
		{
			// Visit chat to familiar page to get current parts
			KoLmafia.updateDisplay( "Getting current parts information for " + current.getName() + " the " + current.getRace() + "." );
			RequestThread.postRequest( new GenericRequest( "main.php?talktoreanimator=1" ) );
		}

		KoLCharacter.setFamiliar( current );
		EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, current.getItem() );
		FamiliarData.checkLockedItem( responseText );
	}

	private static final FamiliarData registerFamiliar( final Matcher matcher )
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
		return familiar;
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

	private static final Pattern LOCK_PATTERN = Pattern.compile( "familiar.php\\?action=lockequip.*'This Familiar Equipment is (Locked|Unlocked)'" );

	public static final void checkLockedItem( final String responseText )
	{
		Matcher lockMatcher = FamiliarData.LOCK_PATTERN.matcher( responseText );
		boolean locked = lockMatcher.find() && lockMatcher.group( 1 ).equals( "Locked" );

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
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
				break;

			case FamiliarPool.HAND:
				// Disembodied Hand
				EquipmentManager.updateEquipmentList( EquipmentManager.WEAPON );
				EquipmentManager.updateEquipmentList( EquipmentManager.OFFHAND );
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
				break;

			case FamiliarPool.SCARECROW:
				// Fancypants Scarecrow
				EquipmentManager.updateEquipmentList( EquipmentManager.PANTS );
				EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );
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

	public int getModifiedWeight( final boolean includeEquipment )
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
				Modifiers mods = Modifiers.getItemModifiers( item.getItemId() );
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
				Modifiers mods = Modifiers.getItemModifiers( item.getItemId() );
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
			weight = (int) Math.floor( weight + weight * ( percent / 100.0f ) );
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
		Modifiers mods = Modifiers.getItemModifiers( itemId );
		return mods == null ? 0 : (int) mods.get( Modifiers.FAMILIAR_WEIGHT );
	}

	public final int getUncappedWeight()
	{
		if ( this.id == FamiliarPool.HATRACK || this.id == FamiliarPool.SCARECROW )
		{
			return Math.max( Math.min( 20, (int) Math.sqrt( this.experience ) ), 1 );
		}
		return this.weight;
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
		return FamiliarDatabase.isUnderwaterType( this.id );
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

	public static class DropInfo
	{
		public final int id;
		public final AdventureResult dropItem;
		public final String dropName;
		public final String dropTracker;
		public final int dailyCap;

		public DropInfo( int id, int dropId, String dropName, String dropTracker, int dailyCap )
		{
			this.id = id;
			this.dropItem = dropId < 0 ? null : ItemPool.get( dropId );
			this.dropName = dropName;
			this.dropTracker = dropTracker;
			this.dailyCap = dailyCap;
		}

		public int dropsToday()
		{
			return Preferences.getInteger( this.dropTracker );
		}

		public boolean hasDropsLeft()
		{
			return this.dropsToday() < this.dailyCap;
		}
	}

	// TODO: (philosophical) Decide whether free fights count as
	// meta-drops, or if these should both extend from a base abstract
	// class for familiar counters.

	public static class FightInfo
		extends DropInfo
	{
		public FightInfo( int id, String dropTracker, int dailyCap )
		{
			super( id, -1, "combats", dropTracker, dailyCap );
		}

		public int fightsToday()
		{
			return this.dropsToday();
		}

		public int hasFightsLeft()
		{
			return this.dropsToday();
		}
	}

	private static final void loadFightFamiliars()
	{
		FIGHT_FAMILIARS.clear();

		FIGHT_FAMILIARS.add( new FightInfo( FamiliarPool.HIPSTER, "_hipsterAdv", 7 ) );
		FIGHT_FAMILIARS.add( new FightInfo( FamiliarPool.ARTISTIC_GOTH_KID, "_hipsterAdv", 7 ) );
		FIGHT_FAMILIARS.add( new FightInfo( FamiliarPool.MACHINE_ELF, "_machineTunnelsAdv", 5 ) );
	}

	private static final void loadDropFamiliars()
	{
		DROP_FAMILIARS.clear();

		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.PIXIE, ItemPool.ABSINTHE, "absinthe", "_absintheDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.SANDWORM, ItemPool.AGUA_DE_VIDA, "agua", "_aguaDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.BADGER, ItemPool.ASTRAL_MUSHROOM, "astral", "_astralDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.KLOOP, ItemPool.DEVILISH_FOLIO, "folio", "_kloopDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.LLAMA, ItemPool.GONG, "gong", "_gongDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.GROOSE, ItemPool.GROOSE_GREASE, "grease", "_grooseDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.TRON, ItemPool.GG_TOKEN, "token", "_tokenDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.ALIEN, ItemPool.TRANSPORTER_TRANSPONDER, "transponder", "_transponderDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.UNCONSCIOUS_COLLECTIVE, ItemPool.UNCONSCIOUS_COLLECTIVE_DREAM_JAR, "dream jar", "_dreamJarDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.ANGRY_JUNG_MAN, ItemPool.PSYCHOANALYTIC_JAR, "psycho jar", "_jungDrops", 1 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.GRIM_BROTHER, ItemPool.GRIM_FAIRY_TALE, "fairy tale", "_grimFairyTaleDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.GRIMSTONE_GOLEM, ItemPool.GRIMSTONE_MASK, "grim mask", "_grimstoneMaskDrops", 1 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.GALLOPING_GRILL, ItemPool.HOT_ASHES, "hot ashes", "_hotAshesDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.FIST_TURKEY, -1, "turkey booze", "_turkeyBooze", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.GOLDEN_MONKEY, ItemPool.POWDERED_GOLD, "powdered gold", "_powderedGoldDrops", 5 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.ADVENTUROUS_SPELUNKER, ItemPool.TALES_OF_SPELUNKING, "tales", "_spelunkingTalesDrops", 1 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.CARNIE, -1, "cotton candy", "_carnieCandyDrops", 10 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.SWORD_AND_MARTINI_GUY, ItemPool.MINI_MARTINI, "mini-martini", "_miniMartiniDrops", 6 ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.PUCK_MAN, ItemPool.POWER_PILL, "power pill", "_powerPillDrops", Math.min( 1 + KoLCharacter.getCurrentDays(), 11 ) ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.MS_PUCK_MAN, ItemPool.POWER_PILL, "power pill", "_powerPillDrops", Math.min( 1 + KoLCharacter.getCurrentDays(), 11 ) ) );
		DROP_FAMILIARS.add( new DropInfo( FamiliarPool.MACHINE_ELF, ItemPool.MACHINE_SNOWGLOBE, "snowglobe", "_snowglobeDrops", 1 ) );
	}

	public static DropInfo getDropInfo( int id )
	{
		for ( DropInfo info : DROP_FAMILIARS )
		{
			if ( info.id == id )
				return info;
		}

		return null;
	}

	public DropInfo getDropInfo()
	{
		return FamiliarData.getDropInfo( this.id );
	}

	public static String dropName( int id )
	{
		DropInfo drops = FamiliarData.getDropInfo( id );
		return drops == null ? null : drops.dropName;
	}

	public String dropName( )
	{
		return FamiliarData.dropName( this.id );
	}

	public static AdventureResult dropItem( int id )
	{
		DropInfo drops = FamiliarData.getDropInfo( id );
		return drops == null ? null : drops.dropItem;
	}

	public AdventureResult dropItem()
	{
		return FamiliarData.dropItem( this.id );
	}

	public static int dropsToday( int id )
	{
		DropInfo drops = FamiliarData.getDropInfo( id );
		return drops == null ? 0 : drops.dropsToday();
	}

	public int dropsToday()
	{
		return FamiliarData.dropsToday( this.id );
	}

	public static int dropDailyCap( int id )
	{
		DropInfo drops = FamiliarData.getDropInfo( id );
		return drops == null ? 0 : drops.dailyCap;
	}

	public int dropDailyCap()
	{
		return FamiliarData.dropDailyCap( this.id );
	}

	public static boolean hasDrop( int id )
	{
		return FamiliarData.getDropInfo( id ) != null;
	}

	public boolean hasDrop()
	{
		return FamiliarData.hasDrop( this.id );
	}

	public static boolean hasFights( int id )
	{
		return FamiliarData.getFightInfo( id ) != null;
	}

	public boolean hasFights()
	{
		return FamiliarData.hasFights( this.id );
	}

	public static FightInfo getFightInfo( int id )
	{
		for ( FightInfo info : FIGHT_FAMILIARS )
		{
			if ( info.id == id )
				return info;
		}
		return null;
	}

	public FightInfo getFightInfo()
	{
		return FamiliarData.getFightInfo( this.id );
	}

	public static int fightsToday( int id )
	{
		FightInfo fights = FamiliarData.getFightInfo( id );
		return fights == null ? 0 : fights.fightsToday();
	}

	public int fightsToday()
	{
		return FamiliarData.fightsToday( this.id );
	}

	public static int fightDailyCap( int id )
	{
		FightInfo fights = FamiliarData.getFightInfo( id );
		return fights == null ? 0 : fights.dailyCap;
	}

	public int fightDailyCap()
	{
		return FamiliarData.fightDailyCap( this.id );
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

	@Override
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

		String name = item.getName();

		switch ( this.id )
		{
		case -1:
			return false;

		case FamiliarPool.CHAMELEON:
			return false;

		case FamiliarPool.HATRACK:
			// Hatrack can wear Hats as well as familiar items, but not Crown of Thrones
			if ( itemId != ItemPool.HATSEAT && ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_HAT )
			{
				return true;
			}
			break;

		case FamiliarPool.HAND:
			// Disembodied Hand can't equip Mainhand only items or Single Equip items
			if ( !EquipmentDatabase.isMainhandOnly( itemId ) && !Modifiers.getBooleanModifier( "Item", name, "Single Equip" ) )
			{
				return true;
			}
			break;

		case FamiliarPool.SCARECROW:
			// Scarecrow can wear Pants as well as familiar items
			if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_PANTS )
			{
				return true;
			}
			break;
		}

		if ( itemId == FamiliarDatabase.getFamiliarItemId( this.id ) )
		{
			return true;
		}

		Modifiers mods = Modifiers.getItemModifiers( itemId );
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

		Modifiers mods = Modifiers.getItemModifiers( item.getItemId() );
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
		if ( KoLCharacter.inRaincore() && FamiliarData.availableItem( FamiliarData.LIFE_PRESERVER, steal ) )
		{
			// The miniature life preserver is only useful in a Heavy Rains run
			return FamiliarData.LIFE_PRESERVER;
		}

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
	 *
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

	public static final void checkShrub()
	{
		if ( KoLCharacter.findFamiliar( FamiliarPool.CRIMBO_SHRUB ) == null )
		{
			return;
		}

		GenericRequest request = new GenericRequest( "desc_familiar.php?which=189" );
		RequestThread.postRequest( request );
		String response = request.responseText;

		Matcher topperMatcher = SHRUB_TOPPER_PATTERN.matcher( response );
		if ( topperMatcher.find() )
		{
			Preferences.setString( "shrubTopper", topperMatcher.group( 1 ) );
		}
		else
		{
			Preferences.setString( "shrubTopper", KoLCharacter.mainStat().toString() );
			// If we didn't find this pattern, we won't find anything else either
			// The remaining values are either random or nothing
			Preferences.setString( "shrubLights", "" );
			Preferences.setString( "shrubGarland", "" );
			Preferences.setString( "shrubGifts", "" );
			return;
		}

		Matcher lightsMatcher = SHRUB_LIGHT_PATTERN.matcher( response );
		if ( lightsMatcher.find() )
		{
			Preferences.setString( "shrubLights", lightsMatcher.group( 1 ) );
		}

		if ( response.contains( "Restores Hit Points" ) )
		{
			Preferences.setString( "shrubGarland", "HP" );
		}
		else if ( response.contains( "PvP fights" ) )
		{
			Preferences.setString( "shrubGarland", "PvP" );
		}
		else if ( response.contains( "Prevents monsters" ) )
		{
			Preferences.setString( "shrubGarland", "blocking" );
		}

		if ( response.contains( "Blast foes" ) )
		{
			Preferences.setString( "shrubGifts", "yellow" );
		}
		else if ( response.contains( "Filled with Meat" ) )
		{
			Preferences.setString( "shrubGifts", "meat" );
		}
		else if ( response.contains( "Exchange random gifts" ) )
		{
			Preferences.setString( "shrubGifts", "gifts" );
		}
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
			defaultComponent.setText(
				familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace() );

			defaultComponent.setVerticalTextPosition( SwingConstants.CENTER );
			defaultComponent.setHorizontalTextPosition( SwingConstants.RIGHT );

			return defaultComponent;
		}
	}
}
