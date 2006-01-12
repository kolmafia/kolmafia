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
		Pattern.compile( "<img src=\"http://images.kingdomofloathing.com/itemimages/.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) kills?\\)(.*?)<(/tr|form)" );

	private static int weightModifier;
	private static int dodecaModifier;

	private static final AdventureResult EMPATHY = new AdventureResult( "Empathy", 0 );
	private static final AdventureResult LEASH = new AdventureResult( "Leash of Linguini", 0 );

	private int id, weight;
	private String name, race, item;

	public FamiliarData( int id )
	{       this( id, "", 1, EquipmentRequest.UNEQUIP );
	}

	public FamiliarData( int id, String name, int weight, String item )
	{
		this.id = id;
		this.name = name;
		this.race = id == -1 ? EquipmentRequest.UNEQUIP : FamiliarsDatabase.getFamiliarName( id );

		this.weight = weight;
		this.item = item;
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

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			this.weight = 0;
		}

		this.name = dataMatcher.group(1);
		this.race = dataMatcher.group(2);
		this.id = FamiliarsDatabase.getFamiliarID( this.race );
		FamiliarsDatabase.downloadFamiliarImage( this.id );

		if ( !FamiliarsDatabase.contains( this.race ) )
			FamiliarsDatabase.registerFamiliar( this.id, this.race );

		String itemData = dataMatcher.group(4);

		this.item = itemData.indexOf( "<img" ) == -1 ? EquipmentRequest.UNEQUIP :
			itemData.indexOf( "tamo.gif" ) != -1 ? "lucky tam o'shanter" :
			itemData.indexOf( "maypole.gif" ) != -1 ? "miniature gravy-covered maypole" :
			itemData.indexOf( "waxlips.gif" ) != -1 ? "wax lips" :
			itemData.indexOf( "pitchfork.gif" ) != -1 ? "annoying pitchfork" :
			itemData.indexOf( "lnecklace.gif" ) != -1 ? "lead necklace" :
			itemData.indexOf( "ratbal.gif" ) != -1 ? "rat head balloon" :
			FamiliarsDatabase.getFamiliarItem( this.id ).toLowerCase();
	}

	public static final void registerFamiliarData( KoLmafia client, String searchText )
	{
		// Assume he has no familiar
		FamiliarData firstFamiliar = null;

		// Examine all the familiars in the list
		Matcher familiarMatcher = SEARCH_PATTERN.matcher( searchText );
		while ( familiarMatcher.find() )
		{
			FamiliarData examinedFamiliar = KoLCharacter.addFamiliar( new FamiliarData( client, familiarMatcher ) );

			// First in the list might be equipped
			if ( firstFamiliar == null )
				firstFamiliar = examinedFamiliar;
		}

		// On the other hand, he may have familiars but none are equipped.
		if ( firstFamiliar == null || searchText.indexOf( "You do not currently have a familiar" ) != -1 )
			firstFamiliar = NO_FAMILIAR;

		KoLCharacter.setFamiliar( firstFamiliar );
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

	public int getModifiedWeight()
	{
		int modifiedWeight = weight;

		if ( id == 38 )
			modifiedWeight += dodecaModifier;
		else
			modifiedWeight += weightModifier;

		modifiedWeight += itemWeightModifier( TradeableItemDatabase.getItemID( getItem() ) );

		return modifiedWeight;
	}

	public static int itemWeightModifier( int itemID )
	{
		switch ( itemID )
		{
		case -1:	// bogus item ID
		case 856:	// shock collar
		case 857:	// moonglasses
		case 1040:	// lucky Tam O'Shanter
		case 1102:	// targeting chip
		case 1116:	// annoying pitchfork
		case 1152:	// miniature gravy-covered maypole
		case 1260:	// wax lips
		case 1419:	// teddy bear sewing kit
			return 0;

		case 865:	// lead necklace
			return 3;

		case 1218:	// rat head balloon
			return -3;

		case 1243:	// toy six-seater hovercraft
			return -5;

		case 1305:	// tiny makeup kit
			return 15;

		default:
			if ( TradeableItemDatabase.getConsumptionType( itemID ) == ConsumeItemRequest.EQUIP_FAMILIAR )
				return 5;
			return 0;
		}
	}

	public String getName()
	{	return name;
	}

	public String getRace()
	{	return race;
	}

	public String toString()
	{	return id == -1 ? EquipmentRequest.UNEQUIP : race + " (" + getModifiedWeight() + " lbs)";
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
			case 1218:
			case 1260:
				return true;

			default:
				return item.equals( KoLDatabase.getCanonicalName( FamiliarsDatabase.getFamiliarItem( id ) ) );
		}
	}

	public static void reset()
	{	updateWeightModifier();
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
		accessoryID[0] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ) );
		accessoryID[1] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ) );
		accessoryID[2] = TradeableItemDatabase.getItemID( KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ) );

		for ( int i = 0; i < 3; ++i )
		{
			if ( accessoryID[i] > 968 && accessoryID[i] < 989 )
				++weightModifier;
			else if ( accessoryID[i] >= 1377 && accessoryID[i] <= 1378 )
				++weightModifier;
		}

		// Plexiglass Pith Helmet adds +5 pounds if equipped.

                if ( TradeableItemDatabase.getItemID( KoLCharacter.getCurrentEquipmentName( KoLCharacter.HAT ) ) == 1231 )
			weightModifier += 5;

		// Next, update the weight due to the accessory
		// that the familiar is wearing

		dodecaModifier = weightModifier;

		// Empathy and Leash of Linguini each add five pounds.
		// The passive "Amphibian Sympathy" skill does too.

		if ( KoLCharacter.getEffects().contains( EMPATHY ) )
		{
			weightModifier += 5;
			dodecaModifier += 5;
		}


		if ( KoLCharacter.getEffects().contains( LEASH ) )
		{
			weightModifier += 5;
			dodecaModifier += 5;
		}

		if ( KoLCharacter.hasAmphibianSympathy() )
		{
			weightModifier += 5;
			dodecaModifier -= 5;
		}
	}
}
