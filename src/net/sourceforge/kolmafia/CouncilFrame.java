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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CouncilFrame extends RequestFrame
{
	public static final KoLRequest COUNCIL_VISIT = new KoLRequest( "council.php" );
	public static final AdventureResult YETI_FUR = new AdventureResult( 388, 1 );

	private static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );

	public CouncilFrame()
	{
		super( "Council of Loathing" );

		COUNCIL_VISIT.responseText = null;
		this.displayRequest( COUNCIL_VISIT );
	}

	public boolean hasSideBar()
	{	return false;
	}

	public String getDisplayHTML( String responseText )
	{
		return super.getDisplayHTML( responseText ).replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" ).replaceFirst(
			"table width=95%", "table width=100%" );
	}

	public static final void handleQuestChange( String location, String responseText )
	{
		if ( location.startsWith( "council" ) )
			handleCouncilChange( responseText );
		else if ( location.startsWith( "guild" ) )
			handleGuildChange( location, responseText );
		else if ( location.startsWith( "friars" ) )
			handleFriarsChange( responseText );
		else if ( location.startsWith( "trapper" ) )
			handleTrapperChange( location, responseText );
		else if ( location.startsWith( "bhh" ) )
			handleBountyChange( location, responseText );
		else if ( location.startsWith( "manor3" ) && location.indexOf( "action=summon" ) != -1 )
			AdventureRequest.registerDemonName( "Summoning Chamber", responseText );
		else if ( location.startsWith( "adventure" ) && location.indexOf( "=84" ) != -1 )
			handleSneakyPeteChange( responseText );
	}

	private static final void handleBountyChange( String location, String responseText )
	{
		if ( location.indexOf( "takebounty" ) != -1 )
		{
			Matcher idMatcher = ITEMID_PATTERN.matcher( location );
			if ( !idMatcher.find() )
				return;

			KoLSettings.setUserProperty( "currentBountyItem", idMatcher.group(1) );
			String itemName = TradeableItemDatabase.getItemName( StaticEntity.parseInt( idMatcher.group(1) ) );
			AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( itemName ) );
			return;
		}

		if ( location.indexOf( "action=abandonbounty" ) == -1 && responseText.indexOf( "You acquire" ) == -1 )
			return;

		int itemId = KoLSettings.getIntegerProperty( "currentBountyItem" );
		if ( itemId == 0 )
			return;

		AdventureResult item = new AdventureResult( itemId, 1 );
		StaticEntity.getClient().processResult( item.getInstance( 0 - item.getCount( inventory ) ) );
		KoLSettings.setUserProperty( "currentBountyItem", "0" );
	}

	private static final void handleSneakyPeteChange( String responseText )
	{
		if ( KoLCharacter.hasEquipped( KoLmafia.NOVELTY_BUTTON ) && responseText.indexOf( "You hand him your button and take his glowstick" ) != -1 )
		{
			if ( KoLCharacter.hasEquipped( KoLmafia.NOVELTY_BUTTON, KoLCharacter.ACCESSORY1 ) )
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY1, EquipmentRequest.UNEQUIP );
			else if ( KoLCharacter.hasEquipped( KoLmafia.NOVELTY_BUTTON, KoLCharacter.ACCESSORY2 ) )
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY2, EquipmentRequest.UNEQUIP );
			else
				KoLCharacter.setEquipment( KoLCharacter.ACCESSORY3, EquipmentRequest.UNEQUIP );

			// Maintain session tally: "unequip" the button and
			// discard it.

			AdventureResult.addResultToList( inventory, KoLmafia.NOVELTY_BUTTON );
			StaticEntity.getClient().processResult( KoLmafia.NOVELTY_BUTTON.getNegation() );
		}
	}

	private static final void handleGuildChange( String location, String responseText )
	{
		if ( location.indexOf( "paco" ) != -1 && KoLCharacter.hasItem( KoLmafia.SATCHEL ) )
			StaticEntity.getClient().processResult( KoLmafia.SATCHEL.getNegation() );
	}

	private static final void handleFriarsChange( String responseText )
	{
		// "Thank you, Adventurer."

		if ( responseText.indexOf( "Thank you" ) != -1 )
		{
			StaticEntity.getClient().processResult( AdventureRequest.DODECAGRAM );
			StaticEntity.getClient().processResult( AdventureRequest.CANDLES );
			StaticEntity.getClient().processResult( AdventureRequest.BUTTERKNIFE );

			KoLmafia.updateDisplay( PENDING_STATE, "Taint cleansed." );
		}
	}

	private static final void handleTrapperChange( String location, String responseText )
	{
		if ( location.indexOf( "max=on" ) != -1 )
		{
			int furCount = YETI_FUR.getCount( inventory );
			StaticEntity.getClient().processResult( YETI_FUR.getInstance( 0 - furCount ) );
			return;
		}

		if ( location.indexOf( "qty" ) != -1 )
		{
			Matcher qtyMatcher = QTY_PATTERN.matcher( location );
			if ( qtyMatcher.find() )
			{
				int furCount = Math.min( YETI_FUR.getCount( inventory ), StaticEntity.parseInt( qtyMatcher.group(1) ) );
				StaticEntity.getClient().processResult( YETI_FUR.getInstance( 0 - furCount ) );
			}

			return;
		}

		Matcher oreMatcher = ORE_PATTERN.matcher( responseText );
		if ( oreMatcher.find() )
			KoLSettings.setUserProperty( "trapperOre", oreMatcher.group(1) + " ore" );

		// If you receive items from the trapper, then you
		// lose some items already in your inventory.

		if ( responseText.indexOf( "You acquire" ) == -1 )
			return;

		if ( responseText.indexOf( "goat cheese pizza" ) != -1 )
		{
			StaticEntity.getClient().processResult( new AdventureResult( "goat cheese", -6, false ) );
			return;
		}

		if ( responseText.indexOf( "crossbow" ) == -1 && responseText.indexOf( "staff" ) == -1 && responseText.indexOf( "sword" ) == -1 )
			return;

		if ( responseText.indexOf( "asbestos" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "asbestos ore", -3, false ) );
		else if (responseText.indexOf( "linoleum" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "linoleum ore", -3, false ) );
		else
			StaticEntity.getClient().processResult( new AdventureResult( "chrome ore", -3, false ) );

		if ( KoLmafia.isAdventuring() )
			unlockGoatlet();
	}

	public static final void unlockGoatlet()
	{
		if ( !EquipmentDatabase.hasOutfit( 8 ) )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "You need a mining outfit to continue." );
			return;
		}

		if ( EquipmentDatabase.isWearingOutfit( 8 ) )
		{
			(new AdventureRequest( "Goatlet", "adventure.php", "60" )).run();
			return;
		}

		SpecialOutfit.createImplicitCheckpoint();
		(new EquipmentRequest( EquipmentDatabase.getOutfit( 8 ))).run();
		(new AdventureRequest( "Goatlet", "adventure.php", "60" )).run();
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void handleCouncilChange( String responseText )
	{
		KoLSettings.setUserProperty( "lastCouncilVisit", String.valueOf( KoLCharacter.getLevel() ) );

		if ( responseText.indexOf( "500" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "mosquito larva", -1, false ) );
		if ( responseText.indexOf( "batskin belt" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		if ( responseText.indexOf( "dragonbone belt buckle" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "skull of the bonerdagon", -1, false ) );
	}
}
