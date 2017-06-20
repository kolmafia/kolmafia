/**
 * Copyright (c) 2005-2017, KoLmafia development team
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

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

public class KGBRequest
	extends GenericRequest
{
	public KGBRequest()
	{
		super( "place.php" );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		if ( action != null )
		{
			KGBRequest.countClicks( responseText );
		}
		if ( action != null && action.startsWith( "kgb_button" ) )
		{
			KGBRequest.updateEnchantments( responseText );
		}
	}

	public static final void countClicks( String responseText )
	{
		int startIndex = responseText.indexOf( "<br>Click" ) + 4;
		int endIndex = responseText.indexOf( "<br>", startIndex );
		String text = responseText.substring( startIndex, endIndex ).toLowerCase();
		int index = text.indexOf( "click" );
		int count = 0;
		while ( index != -1 )
		{
			count++;
			index = text.indexOf( "click", index + 5 );
		}
		Preferences.increment( "_kgbClicksUsed", count );
	}

	// <s>Monsters will be less attracted to you</s><br><br><b>+5 PvP Fights per day</b>
	private static final Pattern ENCHANT_PATTERN = Pattern.compile( "<s>(.*?)</s><br><br><b>(.*?)</b>" );

	private static final void updateEnchantments( final String responseText )
	{
		// A symphony of mechanical buzzing and whirring ensues, and your case seems to be... different somehow.
		if ( !responseText.contains( "symphony of mechanical" ) )
		{
			return;
		}
		Matcher matcher = KGBRequest.ENCHANT_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String oldEnchantment = matcher.group( 1 );
			String newEnchantment = matcher.group( 2 );
			ModifierList modList = Modifiers.getModifierList( "Item", ItemPool.KREMLIN_BRIEFCASE );
			String mod = Modifiers.parseModifier( oldEnchantment );
			ModifierList newModList = Modifiers.splitModifiers( mod );
			for ( Modifier modifier : newModList )
			{
				modList.removeModifier( modifier.getName() );
			}
			mod = Modifiers.parseModifier( newEnchantment );
			newModList = Modifiers.splitModifiers( mod );
			for ( Modifier modifier : newModList )
			{
				modList.addModifier( modifier );
			}
			Modifiers.overrideModifier( "Item:[" + ItemPool.KREMLIN_BRIEFCASE + "]", modList.toString() );
			KoLCharacter.recalculateAdjustments();
		}
	}
}