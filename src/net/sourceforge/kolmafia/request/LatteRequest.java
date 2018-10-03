/**
 * Copyright (c) 2005-2018, KoLmafia development team
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
import net.sourceforge.kolmafia.Modifiers.ModifierList;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class LatteRequest
	extends GenericRequest
{
	private static final Pattern RESULT_PATTERN = Pattern.compile( "You get your mug filled with a (.*?)\\.</span>" );

	public LatteRequest()
	{
		super( "choice.php" );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=1329" ) )
		{
			return;
		}

		if ( !urlString.contains( "option=1" ) )
		{
			return;
		}

		// Find Latte result
		Matcher matcher = RESULT_PATTERN.matcher( responseText );
		ModifierList modList = new ModifierList();
		if ( matcher.find() )
		{
			String latte = matcher.group( 1 );
			if ( latte.contains( "Autumnal" ) || latte.contains( "pumpkin spice" ) || latte.contains( "hint of autumn" ) )
			{
				modList.addToModifier( "Experience (Mysticality)", "1" );
				modList.addToModifier( "Mysticality Percent", "5" );
				modList.addToModifier( "Spell Damage", "5" );
			}
			if ( latte.contains( "Cinna-" ) || latte.contains( "cinnamon" ) )
			{
				modList.addToModifier( "Experience (Moxie)", "1" );
				modList.addToModifier( "Moxie Percent", "5" );
				modList.addToModifier( "Pickpocket Chance", "5" );
			}
			if ( latte.contains( "Vanilla" ) || latte.contains( "vanilla" ) )
			{
				modList.addToModifier( "Experience (Muscle)", "1" );
				modList.addToModifier( "Muscle Percent", "5" );
				modList.addToModifier( "Weapon Damage", "5" );
			}
			if ( latte.contains( "Ancient exotic spiced" ) || latte.contains( "ancient/spicy" ) || latte.contains( "ancient spice" ) )
			{
				modList.addToModifier( "Spooky Damage", "50" );
			}
			if ( latte.contains( "Basil" ) || latte.contains( "basil" ) )
			{
				modList.addToModifier( "HP Regen Min", "5" );
				modList.addToModifier( "HP Regen Max", "5" );
			}
			if ( latte.contains( "Belgian vanilla" ) )
			{
				modList.addToModifier( "Muscle Percent", "20" );
				modList.addToModifier( "Mysticality Percent", "20" );
				modList.addToModifier( "Moxie Percent", "20" );
			}
			if ( latte.contains( "Blue chalk" ) || latte.contains( "blue chalk" ) )
			{
				modList.addToModifier( "Cold Damage", "25" );
			}
			if ( latte.contains( "bug-thistle" ) || latte.contains( "Bug-thistle" ) )
			{
				modList.addToModifier( "Mysticality", "20" );
			}
			if ( latte.contains( "Butternutty" ) || latte.contains( "butternut" ) )
			{
				modList.addToModifier( "Spell Damage", "10" );
			}
			if ( latte.contains( "Cajun" ) || latte.contains( "cajun" ) )
			{
				modList.addToModifier( "Meat Drop", "40" );
			}
			if ( latte.contains( "Carb-loaded" ) || latte.contains( "macaroni" ) || latte.contains( "extra noodles" ) )
			{
				modList.addToModifier( "Maximum HP", "20" );
			}
			if ( latte.contains( "Carrot" ) || latte.contains( "carrot" ) )
			{
				modList.addToModifier( "Item Drop", "20" );
			}
			if ( latte.contains( "Carrrdamom" ) || latte.contains( "carrrdamom" ) )
			{
				modList.addToModifier( "MP Regen Min", "4" );
				modList.addToModifier( "MP Regen Max", "6" );
			}
			if ( latte.contains( "Chili" ) || latte.contains( "chili seeds" ) || latte.contains( "kick" ) )
			{
				modList.addToModifier( "Hot Resistance", "3" );
			}
			if ( latte.contains( "Cloven" ) || latte.contains( "cloves" ) )
			{
				modList.addToModifier( "Stench Resistance", "3" );
			}
			if ( latte.contains( "Coal-boiled" ) || latte.contains( "coal" ) )
			{
				modList.addToModifier( "Hot Damage", "25" );
			}
			if ( latte.contains( "Cocoa" ) || latte.contains( "cocoa powder" ) || latte.contains( "mocha loca" ) )
			{
				modList.addToModifier( "Cold Resistance", "3" );
			}
			if ( latte.contains( "Diet" ) || latte.contains( "diet soda" ) )
			{
				modList.addToModifier( "Initiative", "50" );
			}
			if ( latte.contains( "Dyspepsi" ) )
			{
				modList.addToModifier( "Initiative", "25" );
			}
			if ( latte.contains( "Envenomed" ) || latte.contains( "asp venom" ) || latte.contains( "extra poison" ) )
			{
				modList.addToModifier( "Weapon Damage", "25" );
			}
			if ( latte.contains( "Extra-greasy" ) || latte.contains( "hot sausage" ) || latte.contains( "extra gristle" ) )
			{
				modList.addToModifier( "Muscle Percent", "50" );
			}
			if ( latte.contains( "Extra-healthy" ) || latte.contains( "health potion" ) || latte.contains( "shot of healing elixir" ) )
			{
				modList.addToModifier( "HP Regen Min", "10" );
				modList.addToModifier( "HP Regen Max", "20" );
			}
			if ( latte.contains( "Extra-salty" ) || latte.contains( "rock salt" ) )
			{
				modList.addToModifier( "Critical Hit Percent", "10" );
			}
			if ( latte.contains( "Filthy" ) || latte.contains( "filth milk" ) )
			{
				modList.addToModifier( "Damage Reduction", "20" );
			}
			if ( latte.contains( "Floured" ) || latte.contains( "white flour" ) || latte.contains( "dusted with flour" ) )
			{
				modList.addToModifier( "Sleaze Resistance", "3" );
			}
			if ( latte.contains( "Fortified" ) || latte.contains( "vitamin" ) )
			{
				modList.addToModifier( "Experience (familiar)", "3" );
			}
			if ( latte.contains( "Fresh grass" ) || latte.contains( "fresh grass" ) || latte.contains( "fresh-cut grass" ) )
			{
				modList.addToModifier( "Experience", "3" );
			}
			if ( latte.contains( "Fungal" ) || latte.contains( "fungus" ) || latte.contains( "fungal scrapings" ) )
			{
				modList.addToModifier( "Maximum MP", "30" );
			}
			if ( latte.contains( "Greasy" ) || ( latte.contains( "sausage" ) && !latte.contains( "mega sausage" ) ) || latte.contains( "gristle" ) )
			{
				modList.addToModifier( "Mysticality Percent", "50" );
			}
			if ( latte.contains( "Greek spice" ) || latte.contains( "greek spice" ) )
			{
				modList.addToModifier( "Sleaze Damage", "25" );
			}
			if ( latte.contains( "Grobold rum" ) || latte.contains( "grobold rum" ) )
			{
				modList.addToModifier( "Sleaze Damage", "25" );
			}
			if ( latte.contains( "Guarna" ) || latte.contains( "guarna" ) )
			{
				modList.addToModifier( "Adventures", "4" );
			}
			if ( latte.contains( "Gunpowder" ) || latte.contains( "gunpowder" ) )
			{
				modList.addToModifier( "Weapon Damage", "50" );
			}
			if ( latte.contains( "Hellish" ) || latte.contains( "hellion" ) )
			{
				modList.addToModifier( "PvP Fights", "6" );
			}
			if ( latte.contains( "Hobo-spiced" ) || latte.contains( "hobo spice" ) )
			{
				modList.addToModifier( "Damage Absorption", "50" );
			}
			if ( latte.contains( "Hot wing" ) || latte.contains( "hot wing" ) )
			{
				modList.addToModifier( "Combat Rate", "5" );
			}
			if ( latte.contains( "Inky" ) || latte.contains( " ink" ) )
			{
				modList.addToModifier( "Combat Rate", "-5" );
			}
			if ( latte.contains( "Kombucha-infused" ) || latte.contains( "kombucha" ) )
			{
				modList.addToModifier( "Stench Damage", "25" );
			}
			if ( latte.contains( "Lihc-licked" ) || latte.contains( "lihc saliva" ) || latte.contains( "lihc spit" ) )
			{
				modList.addToModifier( "Spooky Damage", "25" );
			}
			if ( latte.contains( "Lizard milk" ) || latte.contains( "lizard milk" ) )
			{
				modList.addToModifier( "MP Regen Min", "5" );
				modList.addToModifier( "MP Regen Max", "15" );
			}
			if ( latte.contains( "Moldy" ) || latte.contains( "grave mold" ) )
			{
				modList.addToModifier( "Spooky Damage", "20" );
			}
			if ( latte.contains( "Motor oil" ) || latte.contains( "motor oil" ) )
			{
				modList.addToModifier( "Sleaze Damage", "20" );
			}
			if ( latte.contains( "MSG" ) || latte.contains( "with flavor" ) )
			{
				modList.addToModifier( "Critical Hit Percent", "15" );
			}
			if ( latte.contains( "Norwhal milk" ) || latte.contains( "norwhal milk" ) )
			{
				modList.addToModifier( "Maximum HP Percent", "200" );
			}
			if ( latte.contains( "Oil-paint" ) || latte.contains( "oil paint" ) )
			{
				modList.addToModifier( "Cold Damage", "5" );
				modList.addToModifier( "Hot Damage", "5" );
				modList.addToModifier( "Sleaze Damage", "5" );
				modList.addToModifier( "Spooky Damage", "5" );
				modList.addToModifier( "Stench Damage", "5" );
			}
			if ( latte.contains( "Paradise milk" ) || latte.contains( "paradise milk" ) || latte.contains( "milk of paradise" ) )
			{
				modList.addToModifier( "Muscle", "20" );
				modList.addToModifier( "Mysticality", "20" );
				modList.addToModifier( "Moxie", "20" );
			}
			if ( latte.contains( "Rawhide" ) || latte.contains( "rawhide" ) )
			{
				modList.addToModifier( "Familiar Weight", "5" );
			}
			if ( latte.contains( "Salted" ) || latte.contains( "salt" ) )
			{
				modList.addToModifier( "Critical Hit Percent", "5" );
			}
			if ( latte.contains( "Sandalwood-infused" ) || latte.contains( "sandalwood splinter" ) )
			{
				modList.addToModifier( "Muscle", "5" );
				modList.addToModifier( "Mysticality", "5" );
				modList.addToModifier( "Moxie", "5" );
			}
			if ( latte.contains( "Space pumpkin" ) || latte.contains( "space pumpkin" ) )
			{
				modList.addToModifier( "Muscle", "10" );
				modList.addToModifier( "Mysticality", "10" );
				modList.addToModifier( "Moxie", "10" );
			}
			if ( latte.contains( "Spaghetti-squashy" ) || latte.contains( "spaghetti squash spice" ) || latte.contains( "extra squash" ) )
			{
				modList.addToModifier( "Spell Damage", "20" );
			}
			if ( latte.contains( "Squamous-salted" ) || latte.contains( "squamous" ) )
			{
				modList.addToModifier( "Spooky Resistance", "3" );
			}
			if ( latte.contains( "Super-greasy" ) || latte.contains( "mega sausage" ) || latte.contains( "super gristle" ) )
			{
				modList.addToModifier( "Moxie Percent", "50" );
			}
			if ( latte.contains( "Teeth" ) || latte.contains( "teeth" ) )
			{
				modList.addToModifier( "Spooky Damage", "25" );
				modList.addToModifier( "Weapon Damage", "25" );
			}

			Preferences.setString( "latteModifier", modList.toString() );
			Modifiers.overrideModifier( "Item:[" + ItemPool.LATTE_MUG + "]", modList.toString() );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			Preferences.increment( "_latteRefillsUsed", 1, 3, false );
			Preferences.setBoolean( "_latteBanishUsed", false );
			Preferences.setBoolean( "_latteCopyUsed", false );
			Preferences.setBoolean( "_latteDrinkUsed", false );
		}
	}
}
