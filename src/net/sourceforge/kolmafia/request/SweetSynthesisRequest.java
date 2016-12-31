/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SweetSynthesisRequest
	extends GenericRequest
{
	// runskillz.php?action=Skillz&whichskill=166&targetplayer=XXXXX&quantity=1
	// choice.php?a=XXXX&b=YYYY&whichchoice=1217&option=1

	private static final Pattern ITEMID1_PATTERN = Pattern.compile( "[?&]a=([\\d]+)" );
	private static final Pattern ITEMID2_PATTERN = Pattern.compile( "[?&]b=([\\d]+)" );

	final int itemId1;
	final int itemId2;

	public SweetSynthesisRequest( final int itemId1, final int itemId2 )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1217" );
		this.addFormField( "option", "1" );
		this.addFormField( "a", String.valueOf( itemId1 ) );
		this.addFormField( "b", String.valueOf( itemId1 ) );
		this.itemId1 = itemId1;
		this.itemId2 = itemId2;
	}

	private static final int extractItemId( final String urlString, Pattern pattern )
	{
		Matcher matcher = pattern.matcher( urlString );
		return matcher.find() ?
			StringUtilities.parseInt( matcher.group( 1 ) ):
			0;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// Check that you have spleen available
		if ( KoLCharacter.getSpleenUse() >= KoLCharacter.getSpleenLimit() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		if ( !ItemDatabase.isCandyItem( this.itemId1 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item '" + this.itemId1 + "' is not candy" );
			return;
		}

		if ( !ItemDatabase.isCandyItem( this.itemId2 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item '" + this.itemId2 + "' is not candy" );
			return;
		}

		// Acquire the first candy
		if ( !InventoryManager.retrieveItem( this.itemId1, 1, true, false ) )
		{
			return;
		}

		// Acquire the second candy
		if ( !InventoryManager.retrieveItem( this.itemId2, 1, true, false ) )
		{
			return;
		}

		// Run the skill
		GenericRequest skillRequest = new GenericRequest( "runskillz.php" );
		skillRequest.addFormField( "action", "Skillz" );
		skillRequest.addFormField( "whichskill", String.valueOf( SkillPool.SWEET_SYNTHESIS ) );
		skillRequest.addFormField( "targetplayer", KoLCharacter.getPlayerId() );
		skillRequest.addFormField( "quantity", "1" );

		skillRequest.run();

		String responseText = skillRequest.responseText;

		// No response because of I/O error
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// Now run the choice
		super.run();

		responseText = this.responseText;

		// This should have been caught above
		if ( responseText.contains( "Your spleen has already taken enough abuse for one day." ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		// ChoiceManager will invoke our postChoice1 method
	}

	public static void postChoice1( final String urlString, final String responseText )
	{
		if ( responseText.contains( "Your spleen has already taken enough abuse for one day." ) )
		{
			return;
		}

		// We just used 1 spleen
		KoLCharacter.setSpleenUse( KoLCharacter.getSpleenUse() + 1 );
		KoLCharacter.updateStatus();

		// If we gain an effect, extract the items we used and remove them from inventory
		if ( responseText.contains( "You acquire an effect" ) )
		{
			int itemId1 = SweetSynthesisRequest.extractItemId( urlString, ITEMID1_PATTERN );
			int itemId2 = SweetSynthesisRequest.extractItemId( urlString, ITEMID2_PATTERN );
			ResultProcessor.processItem( itemId1, -1 );
			ResultProcessor.processItem( itemId2, -1 );
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1217 )
		{
			return false;
		}

		int itemId1 = SweetSynthesisRequest.extractItemId( urlString, ITEMID1_PATTERN );
		int itemId2 = SweetSynthesisRequest.extractItemId( urlString, ITEMID2_PATTERN );

		if ( itemId1 == 0 || itemId2 == 0)
		{
			return false;
		}

		String name1 = ItemDatabase.getDataName( itemId1 );
		String name2 = ItemDatabase.getDataName( itemId2 );

		String message = "synthesize " + name1 + ", " + name2;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
