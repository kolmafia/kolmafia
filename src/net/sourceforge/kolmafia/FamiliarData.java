/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class FamiliarData implements KoLConstants, Comparable
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern REGISTER_PATTERN =
		Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^\"]*?)\" class=hand onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (exp|experience)?, .*? kills?\\)(.*?)<(/tr|form)" );

	private int id, weight;
	private String name, race;
	private AdventureResult item;

	public FamiliarData( int id )
	{	this( id, "", 1, EquipmentRequest.UNEQUIP );
	}

	public FamiliarData( int id, String name, int weight, AdventureResult item )
	{
		this.id = id;
		this.name = name;
		this.race = id == -1 ? EquipmentRequest.UNEQUIP.toString() : FamiliarsDatabase.getFamiliarName( id );

		this.weight = weight;
		this.item = item;
	}

	private FamiliarData( Matcher dataMatcher )
	{
		this.id = StaticEntity.parseInt( dataMatcher.group(2) );
		this.race = dataMatcher.group(4);

		FamiliarsDatabase.registerFamiliar( id, race );
		FamiliarsDatabase.setFamiliarImageLocation( id, dataMatcher.group(1) );

		int kills = StaticEntity.parseInt( dataMatcher.group(5) );
		this.weight = Math.max( Math.min( 20, (int) Math.sqrt( kills ) ), 1 );

		this.name = dataMatcher.group(3);

		String itemData = dataMatcher.group(7);

		this.item = itemData.indexOf( "<img" ) == -1 ? EquipmentRequest.UNEQUIP :
			itemData.indexOf( "tamo.gif" ) != -1 ? new AdventureResult( "lucky Tam O'Shanter", 1, false ) :
			itemData.indexOf( "omat.gif" ) != -1 ? new AdventureResult( "lucky Tam O'Shatner", 1, false ) :
			itemData.indexOf( "maypole.gif" ) != -1 ? new AdventureResult( "miniature gravy-covered maypole", 1, false ) :
			itemData.indexOf( "waxlips.gif" ) != -1 ? new AdventureResult( "wax lips", 1, false ) :
			itemData.indexOf( "pitchfork.gif" ) != -1 ? new AdventureResult( "annoying pitchfork", 1, false ) :
			itemData.indexOf( "lnecklace.gif" ) != -1 ? new AdventureResult( "lead necklace", 1, false ) :
			itemData.indexOf( "ratbal.gif" ) != -1 ? new AdventureResult( "rat head balloon", 1, false ) :
			itemData.indexOf( "punkin.gif" ) != -1 ? new AdventureResult( "plastic pumpkin bucket", 1, false ) :
			new AdventureResult( FamiliarsDatabase.getFamiliarItem( this.id ), 1, false );
	}

	public static final void registerFamiliarData( String searchText )
	{
		// Assume he has no familiar
		FamiliarData firstFamiliar = null;

		Matcher familiarMatcher = REGISTER_PATTERN.matcher( searchText );
		while ( familiarMatcher.find() )
		{
			FamiliarData examinedFamiliar = KoLCharacter.addFamiliar( new FamiliarData( familiarMatcher ) );

			// First in the list might be equipped
			if ( firstFamiliar == null )
				firstFamiliar = examinedFamiliar;
		}

		// On the other hand, he may have familiars but none are equipped.
		if ( firstFamiliar == null || searchText.indexOf( "You do not currently have a familiar" ) != -1 )
			firstFamiliar = NO_FAMILIAR;

		KoLCharacter.setFamiliar( firstFamiliar );
		KoLCharacter.setEquipment( KoLCharacter.FAMILIAR, firstFamiliar.getItem() );


		KoLCharacter.recalculateAdjustments();
	}

	public int getId()
	{	return id;
	}

	public void setItem( AdventureResult item )
	{	this.item = item;
	}

	public AdventureResult getItem()
	{	return item == null ? EquipmentRequest.UNEQUIP : item;
	}

	public void setWeight( int weight )
	{	this.weight = weight;
	}

	public int getWeight()
	{	return weight;
	}

	public int getModifiedWeight()
	{
		// Start with base weight
		int total = weight;

		// Add in adjustment due to equipment, skills, and effects
		if ( id == 38 )
			total += KoLCharacter.getDodecapedeWeightAdjustment();
		else
			total += KoLCharacter.getFamiliarWeightAdjustment();

		// Finally, add in effect of current equipment
		total += itemWeightModifier( item.getItemId() );

		return total;
	}

	public static int itemWeightModifier( int itemId )
	{
		switch ( itemId )
		{
		case -1:	// bogus item id
		case 0:		// another bogus item id
		case 856:	// shock collar
		case 857:	// moonglasses
		case 1040:	// lucky Tam O'Shanter
		case 1102:	// targeting chip
		case 1116:	// annoying pitchfork
		case 1152:	// miniature gravy-covered maypole
		case 1260:	// wax lips
		case 1264:	// tiny nose-bone fetish
		case 1419:	// teddy bear sewing kit
		case 1489:	// miniature dormouse
		case 1537:	// weegee sqouija
		case 1539:	// lucky Tam O'Shatner
		case 1623:	// badger badge
		case 1928:	// tuning fork
		case 2084:	// can of starch
		case 2147:	// evil teddy bear sewing kit
		case 2191:	// giant book of ancient carols
		case 2225:	// flaming familiar doppelg&auml;nger
			return 0;

		case 865:	// lead necklace
			return 3;

		case 1971:  // plastic pumpkin bucket
			return 5;

		case 1218:	// rat head balloon
			return -3;

		case 1243:	// toy six-seater hovercraft
			return -5;

		case 1305:	// tiny makeup kit
			return 15;

		case 1526:	// pet rock "Snooty" disguise
		case 1678:	// pet rock "Groucho" disguise
			return 11;

		default:
			return 5;
		}
	}

	public String getName()
	{	return name;
	}

	public String getRace()
	{	return race;
	}

	public boolean trainable()
	{
		if ( id == -1)
			return false;

		int skills[] = FamiliarsDatabase.getFamiliarSkills( id );

		// If any skill is greater than 0, we can train in that event
		for ( int i = 0; i < skills.length; ++i )
			if ( skills[i] > 0 )
				return true;
		return false;
	}

	public String toString()
	{	return id == -1 ? EquipmentRequest.UNEQUIP.toString() : race + " (" + getModifiedWeight() + " lbs)";
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof FamiliarData && id == ((FamiliarData)o).id;
	}

	public int compareTo( Object o )
	{	return o == null || !(o instanceof FamiliarData) ? 1 : compareTo( (FamiliarData)o );
	}

	public int compareTo( FamiliarData fd )
	{	return race.compareToIgnoreCase( fd.race );
	}

	/**
	 * Returns whether or not the familiar can equip the given
	 * familiar item.
	 */

	public boolean canEquip( AdventureResult item )
	{
		if ( item == null )
			return false;

		if ( item == EquipmentRequest.UNEQUIP )
			return true;

		switch ( item.getItemId() )
		{
		case -1:
			return false;

		case 865:   // lead necklace
		case 1040:  // lucky Tam O'Shanter
		case 1116:  // annoying pitchfork
		case 1152:  // miniature gravy-covered maypole
		case 1218:  // rat head balloon
		case 1260:  // wax lips
		case 1539:  // lucky Tam O'Shatner
		case 1971:  // plastic pumpkin bucket
		case 2225:  // flaming familiar doppelg&auml;nger
			// Can these items be equipped by a Comma Chameleon?
			return true;

		default:
			return item.getName().equals( FamiliarsDatabase.getFamiliarItem( id ) );
		}
	}

	public boolean isThiefFamiliar()
	{
		switch ( id )
		{
		case 16: // Cocoabo
		case 27: // Hanukkimbo Dreidl
		case 48: // Ninja Pirate Zombie Robot
			return true;

		default:
			return false;
		}
	}

	public boolean isCombatFamiliar()
	{
		switch ( id )
		{
		case 1:  // Mosquito
		case 4:  // Angry Goat
		case 5:  // Sabre-Toothed Lime
		case 6:  // Fuzzy Dice
		case 7:  // Spooky Pirate Skeleton
		case 9:  // Howling Balloon Monkey
		case 10: // Stab Bat
		case 16: // Cocoabo
		case 17: // Star Starfish
		case 19: // Ghost Pickle on a Stick
		case 21: // Whirling Maple Leaf
		case 24: // Jill-O-Lantern
		case 25: // Hand Turkey
		case 26: // Crimbo Elf
		case 27: // Hanukkimbo Dreidl
		case 28: // Baby Yeti
		case 29: // Feather Boa Constrictor
		case 31: // Personal Raincloud
		case 32: // Clockwork Grapefruit
		case 33: // MagimechTech MicroMechaMech
		case 34: // Flaming Gravy Fairy
		case 35: // Frozen Gravy Fairy
		case 36: // Stinky Gravy Fairy
		case 37: // Spooky Gravy Fairy
		case 38: // Inflatable Dodecapede
		case 40: // Doppelshifter
		case 44: // Sweet Nutcracker
		case 46: // Snowy Owl
		case 48: // Ninja Pirate Zombie Robot
		case 49: // Sleazy Gravy Fairy
		case 51: // Wind-up Chattering Teeth
		case 52: // Spirit Hobo
		case 54: // Comma Chameleon
		case 55: // Misshapen Animal Skeleton
		case 56: // Scary Death Orb
			return true;

		default:

			// pumpkin basket makes a non-combat familiar
			// into a combat familiar.

			return false;
		}
	}


	public static DefaultListCellRenderer getRenderer()
	{	return new FamiliarRenderer();
	}

	private static class FamiliarRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			JLabel defaultComponent = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof FamiliarData) || ((FamiliarData)value).id == -1 )
			{
				defaultComponent.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				defaultComponent.setText( VERSION_NAME + ", the 0 lb. \"No Familiar Plz\" Placeholder" );

				defaultComponent.setVerticalTextPosition( JLabel.CENTER );
				defaultComponent.setHorizontalTextPosition( JLabel.RIGHT );
				return defaultComponent;
			}

			FamiliarData familiar = (FamiliarData) value;
			defaultComponent.setIcon( FamiliarsDatabase.getFamiliarImage( familiar.id ) );
			defaultComponent.setText( familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace() );

			defaultComponent.setVerticalTextPosition( JLabel.CENTER );
			defaultComponent.setHorizontalTextPosition( JLabel.RIGHT );

			return defaultComponent;
		}
	}
}
