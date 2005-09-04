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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FamiliarData implements KoLConstants, Comparable
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern SEARCH_PATTERN =
		Pattern.compile( "<img src=\"http://images.kingdomofloathing.com/itemimages/familiar(\\d+).*?</b>.*?\\d+ pound (.*?) \\(([\\d,]+) kills?\\)(.*?)<(/tr|form)" );

	private static int weightModifier;
	private static int dodecaModifier;
	private static KoLCharacter owner;

	private static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0 );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0 );

	private int id, weight;
	private String race, item;

	public FamiliarData( int id )
	{
		this.id = id;
		this.race = id == -1 ? EquipmentRequest.UNEQUIP : FamiliarsDatabase.getFamiliarName( id );

		this.weight = 0;
		this.item = EquipmentRequest.UNEQUIP;
	}

	private FamiliarData( KoLmafia client, Matcher dataMatcher )
	{
		try
		{
			int kills = df.parse( dataMatcher.group(3) ).intValue();
			this.weight = Math.max( Math.min( 20, (int) Math.sqrt( kills ) ), 1 );
		}
		catch ( Exception e )
		{
			// If an exception is thrown, that means it was not
			// possible to parse the kills.  Set the weight to
			// zero pounds in this case.

			this.weight = 0;
		}

		this.id = Integer.parseInt( dataMatcher.group(1) );
		this.race = dataMatcher.group(2);

		if ( !FamiliarsDatabase.contains( this.race ) )
			FamiliarsDatabase.registerFamiliar( this.id, this.race );

		this.id = FamiliarsDatabase.getFamiliarID( this.race );

		String itemData = dataMatcher.group(4);

		this.item = itemData.indexOf( "<img" ) == -1 ? EquipmentRequest.UNEQUIP :
			itemData.indexOf( "tamo.gif" ) != -1 ? "lucky tam o'shanter" :
			itemData.indexOf( "maypole.gif" ) != -1 ? "miniature gravy-covered maypole" :
			itemData.indexOf( "waxlips.gif" ) != -1 ? "wax lips" :
			itemData.indexOf( "pitchfork.gif" ) != -1 ? "annoying pitchfork" :
			itemData.indexOf( "lnecklace.gif" ) != -1 ? "lead necklace" :
			FamiliarsDatabase.getFamiliarItem( this.id ).toLowerCase();
	}

	public static final void registerFamiliarData( KoLmafia client, String searchText )
	{
		KoLCharacter characterData = client.getCharacterData();

		FamiliarData firstFamiliar = null;

		Matcher familiarMatcher = SEARCH_PATTERN.matcher( searchText );

		while ( familiarMatcher.find() )
		{
			FamiliarData examinedFamiliar = characterData.addFamiliar( new FamiliarData( client, familiarMatcher ) );

			if ( firstFamiliar == null )
				firstFamiliar = examinedFamiliar;
		}

		// If he really has a familiar, first one parsed is it.

		if ( searchText.indexOf( "You do not currently have a familiar" ) != -1 )
			firstFamiliar = null;

		characterData.setFamiliar( firstFamiliar );
	}

	public int getID()
	{	return id;
	}

	public void setItem( String item )
	{	this.item = item;
	}

	public String getItem()
	{	return item;
	}

	public void setWeight( int weight )
	{	this.weight = weight;
	}

	public int getWeight()
	{	return weight;
	}

	public String getRace()
	{	return race;
	}

	public String toString()
	{
		if ( id == -1 )
			return EquipmentRequest.UNEQUIP;

		if ( id == 38 )
			return race + " (" + Math.max( weight + dodecaModifier, 1 ) + " lbs)";

		return race + " (" + (weight + weightModifier) + " lbs)";
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

	public boolean canEquip( String item )
	{
		switch ( TradeableItemDatabase.getItemID( item ) )
		{
			case -1:
				return false;

			case 865:
			case 1040:
			case 1116:
			case 1152:
			case 1260:
				return true;

			default:
				return item.equals( KoLDatabase.getCanonicalName( FamiliarsDatabase.getFamiliarItem( id ) ) );
		}
	}

	public static void setOwner( KoLCharacter owner )
	{
		FamiliarData.owner = owner;
		updateWeightModifier();
	}

	/**
	 * Calculates the amount of additional weight that is present
	 * due to buffs and related things.
	 */

	public static void updateWeightModifier()
	{
		weightModifier = 0;

		// First update the weight changes due to the
		// accessories the character is wearing

		int [] accessoryID = new int[3];
		accessoryID[0] = TradeableItemDatabase.getItemID( owner.getEquipment( KoLCharacter.ACCESSORY1 ) );
		accessoryID[1] = TradeableItemDatabase.getItemID( owner.getEquipment( KoLCharacter.ACCESSORY2 ) );
		accessoryID[2] = TradeableItemDatabase.getItemID( owner.getEquipment( KoLCharacter.ACCESSORY3 ) );

		for ( int i = 0; i < 3; ++i )
			if ( accessoryID[i] > 968 && accessoryID[i] < 989 )
				++weightModifier;

		// Next, update the weight due to the accessory
		// that the familiar is wearing

		dodecaModifier = weightModifier;

		switch ( TradeableItemDatabase.getItemID( owner.getFamiliarItem() ) )
		{
			case -1:
			case 1040:
			case 1152:
			case 1260:

				break;

			case 865:

				weightModifier += 3;
				dodecaModifier += 3;
				break;

			default:

				weightModifier += 5;
				dodecaModifier -= 5;
				break;
		}

		// Empathy and Leash of Linguini each add five pounds.
		// The passive "Amphibian Sympathy" skill does too.

		if ( owner.getEffects().contains( EMPATHY ) )
		{
			weightModifier += 5;
			dodecaModifier -= 5;
		}


		if ( owner.getEffects().contains( LEASH ) )
		{
			weightModifier += 5;
			dodecaModifier -= 5;
		}

		if ( owner.hasAmphibianSympathy() )
		{
			weightModifier += 5;
			dodecaModifier -= 5;
		}
	}
}
