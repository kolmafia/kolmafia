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
	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );

	public CouncilFrame()
	{	super( "Council of Loathing", new KoLRequest( "council.php", true ) );
	}

	public boolean hasSideBar()
	{	return false;
	}

	public String getDisplayHTML( String responseText )
	{
		return super.getDisplayHTML( responseText ).replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" ).replaceFirst(
			"table width=95%", "table width=100%" );
	}

	public static void handleQuestChange( String location, String responseText )
	{
		if ( location.startsWith( "council.php" ) )
			handleCouncilChange( responseText );
		else if ( location.startsWith( "guild.php" ) )
			handleGuildChange( location, responseText );
		else if ( location.startsWith( "friars.php" ) )
			handleFriarsChange( responseText );
		else if ( location.startsWith( "trapper.php" ) )
			handleTrapperChange( responseText );

	}

	private static void handleGuildChange( String location, String responseText )
	{
		if ( location.indexOf( "paco" ) != -1 && KoLCharacter.hasItem( KoLmafia.SATCHEL ) )
			StaticEntity.getClient().processResult( KoLmafia.SATCHEL.getNegation() );
	}

	private static void handleFriarsChange( String responseText )
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

	private static void handleTrapperChange( String responseText )
	{
		Matcher oreMatcher = ORE_PATTERN.matcher( responseText );
		if ( oreMatcher.find() )
			StaticEntity.setProperty( "trapperOre", oreMatcher.group(1) + " ore" );

		// If you receive items from the trapper, then you
		// lose some items already in your inventory.

		if ( responseText.indexOf( "You acquire" ) == -1 )
			return;

		if ( responseText.indexOf( "crossbow" ) != -1 || responseText.indexOf( "staff" ) != -1 || responseText.indexOf( "sword" ) != -1 )
		{
			if ( responseText.indexOf( "asbestos" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "asbestos ore", -3, false ) );
			else if (responseText.indexOf( "linoleum" ) != -1 )
				StaticEntity.getClient().processResult( new AdventureResult( "linoleum ore", -3, false ) );
			else
				StaticEntity.getClient().processResult( new AdventureResult( "chrome ore", -3, false ) );
		}
		else if ( responseText.indexOf( "goat cheese pizza" ) != -1 )
		{
			StaticEntity.getClient().processResult( new AdventureResult( "goat cheese", -6, false ) );
		}
	}

	private static void handleCouncilChange( String responseText )
	{
		if ( responseText.indexOf( "500" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "mosquito larva", -1, false ) );
		if ( responseText.indexOf( "batskin belt" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		if ( responseText.indexOf( "dragonbone belt buckle" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( "skull of the bonerdagon", -1, false ) );
	}
}
