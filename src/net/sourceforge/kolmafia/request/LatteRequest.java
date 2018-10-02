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
	private static final Pattern urlPattern = Pattern.compile( "m=(\\d)&e=(\\d)&s1=(.*)?&s2=(.*)?&s3=(.*)" );

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

		// Not matching the URL, because urgh.
		ModifierList modList = new ModifierList();
		if ( responseText.contains( "Autumnal" ) || responseText.contains( "pumpkin spice" ) || responseText.contains( "hint of autumn" ) )
		{
			modList.addModifier( "Experience (Mysticality)", "1" );
			modList.addModifier( "Mysticality Percent", "5" );
			modList.addModifier( "Spell Damage", "5" );
		}
		if ( responseText.contains( "Cinna-" ) || responseText.contains( "cinnamon" ) )
		{
			modList.addModifier( "Experience (Moxie)", "1" );
			modList.addModifier( "Moxie Percent", "5" );
			modList.addModifier( "Pickpocket Chance", "5" );
		}
		if ( responseText.contains( "Vanilla" ) || responseText.contains( "vanilla" ) )
		{
			modList.addModifier( "Experience (Muscle)", "1" );
			modList.addModifier( "Muscle Percent", "5" );
			modList.addModifier( "Weapon Damage", "5" );
		}
		if ( responseText.contains( "Ancient exotic spiced" ) || responseText.contains( "ancient/spicy" ) || responseText.contains( "ancient spice" ) )
		{
			modList.addModifier( "Spooky Damage", "50" );
		}
		if ( responseText.contains( "Basil" ) || responseText.contains( "basil" ) )
		{
			modList.addModifier( "HP Regen Min", "5" );
			modList.addModifier( "HP Regen Max", "5" );
		}
		if ( responseText.contains( "Belgian vanilla" ) )
		{
			modList.addModifier( "Muscle Percent", "20" );
			modList.addModifier( "Mysticality Percent", "20" );
			modList.addModifier( "Moxie Percent", "20" );
		}
		if ( responseText.contains( "Blue chalk" ) || responseText.contains( "blue chalk" ) )
		{
			modList.addModifier( "Cold Damage", "25" );
		}
		if ( responseText.contains( "bug-thistle" ) || responseText.contains( "Bug-thistle" ) )
		{
			modList.addModifier( "Mysticality", "20" );
		}
		if ( responseText.contains( "Butternutty" ) || responseText.contains( "butternut" ) )
		{
			modList.addModifier( "Spell Damage", "10" );
		}
		if ( responseText.contains( "Cajun" ) || responseText.contains( "cajun" ) )
		{
			modList.addModifier( "Meat Drop", "40" );
		}
		if ( responseText.contains( "Carb-loaded" ) || responseText.contains( "macaroni" ) || responseText.contains( "extra noodles" ) )
		{
			modList.addModifier( "Maximum HP", "20" );
		}
		if ( responseText.contains( "Carrot" ) || responseText.contains( "carrot" ) )
		{
			modList.addModifier( "Item Drop", "20" );
		}
		if ( responseText.contains( "Carrrdamom" ) || responseText.contains( "carrrdamom" ) )
		{
			modList.addModifier( "MP Regen Min", "4" );
			modList.addModifier( "MP Regen Max", "6" );
		}
		if ( responseText.contains( "Chili" ) || responseText.contains( "chili seeds" ) || responseText.contains( "kick" ) )
		{
			modList.addModifier( "Hot Resistance", "3" );
		}
		if ( responseText.contains( "Cloven" ) || responseText.contains( "cloves" ) )
		{
			modList.addModifier( "Stench Resistance", "3" );
		}
		if ( responseText.contains( "Coal-boiled" ) || responseText.contains( "coal" ) )
		{
			modList.addModifier( "Hot Damage", "25" );
		}
		if ( responseText.contains( "Cocoa" ) || responseText.contains( "cocoa powder" ) || responseText.contains( "mocha loca" ) )
		{
			modList.addModifier( "Cold Resistance", "3" );
		}
		if ( responseText.contains( "Diet" ) || responseText.contains( "diet soda" ) )
		{
			modList.addModifier( "Initiative", "50" );
		}
		if ( responseText.contains( "Dyspepsi" ) )
		{
			modList.addModifier( "Initiative", "25" );
		}
		if ( responseText.contains( "Envenomed" ) || responseText.contains( "asp venom" ) || responseText.contains( "extra poison" ) )
		{
			modList.addModifier( "Weapon Damage", "25" );
		}
		if ( responseText.contains( "Extra-greasy" ) || responseText.contains( "hot sausage" ) || responseText.contains( "extra gristle" ) )
		{
			modList.addModifier( "Muscle Percent", "50" );
		}
		if ( responseText.contains( "Extra-healthy" ) || responseText.contains( "health potion" ) || responseText.contains( "shot of healing elixir" ) )
		{
			modList.addModifier( "HP Regen Min", "10" );
			modList.addModifier( "HP Regen Max", "20" );
		}
		if ( responseText.contains( "Extra-salty" ) || responseText.contains( "rock salt" ) )
		{
			modList.addModifier( "Critical Hit Percent", "10" );
		}
		if ( responseText.contains( "Filthy" ) || responseText.contains( "filth milk" ) )
		{
			modList.addModifier( "Damage Reduction", "20" );
		}
		if ( responseText.contains( "Floured" ) || responseText.contains( "white flour" ) || responseText.contains( "dusted with flour" ) )
		{
			modList.addModifier( "Sleaze Resistance", "3" );
		}
		if ( responseText.contains( "Fortified" ) || responseText.contains( "vitamin" ) )
		{
			modList.addModifier( "Experience (familiar)", "3" );
		}
		if ( responseText.contains( "Fresh grass" ) || responseText.contains( "fresh grass" ) || responseText.contains( "fresh-cut grass" ) )
		{
			modList.addModifier( "Experience", "3" );
		}
		if ( responseText.contains( "Fungal" ) || responseText.contains( "fungus" ) || responseText.contains( "fungal scrapings" ) )
		{
			modList.addModifier( "Maximum MP", "30" );
		}
		if ( responseText.contains( "Greasy" ) || ( responseText.contains( "sausage" ) && !responseText.contains( "mega sausage" ) ) || responseText.contains( "gristle" ) )
		{
			modList.addModifier( "Mysticality Percent", "50" );
		}
		if ( responseText.contains( "Greek spice" ) || responseText.contains( "greek spice" ) )
		{
			modList.addModifier( "Sleaze Damage", "25" );
		}
		if ( responseText.contains( "Grobold rum" ) || responseText.contains( "grobold rum" ) )
		{
			modList.addModifier( "Sleaze Damage", "25" );
		}
		if ( responseText.contains( "Guarna" ) || responseText.contains( "guarna" ) )
		{
			modList.addModifier( "Adventures", "4" );
		}
		if ( responseText.contains( "Gunpowder" ) || responseText.contains( "gunpowder" ) )
		{
			modList.addModifier( "Weapon Damage", "50" );
		}
		if ( responseText.contains( "Hellish" ) || responseText.contains( "hellion" ) )
		{
			modList.addModifier( "PvP Fights", "6" );
		}
		if ( responseText.contains( "Hobo-spiced" ) || responseText.contains( "hobo spice" ) )
		{
			modList.addModifier( "Damage Absorption", "50" );
		}
		if ( responseText.contains( "Hot wing" ) || responseText.contains( "hot wing" ) )
		{
			modList.addModifier( "Combat Rate", "5" );
		}
		if ( responseText.contains( "Inky" ) || responseText.contains( " ink" ) )
		{
			modList.addModifier( "Combat Rate", "-5" );
		}
		if ( responseText.contains( "Kombucha-infused" ) || responseText.contains( "kombucha" ) )
		{
			modList.addModifier( "Stench Damage", "25" );
		}
		if ( responseText.contains( "Lihc-licked" ) || responseText.contains( "lihc saliva" ) || responseText.contains( "lihc spit" ) )
		{
			modList.addModifier( "Spooky Damage", "25" );
		}
		if ( responseText.contains( "Lizard milk" ) || responseText.contains( "lizard milk" ) )
		{
			modList.addModifier( "MP Regen Min", "5" );
			modList.addModifier( "MP Regen Max", "15" );
		}
		if ( responseText.contains( "Moldy" ) || responseText.contains( "grave mold" ) )
		{
			modList.addModifier( "Spooky Damage", "20" );
		}
		if ( responseText.contains( "Motor oil" ) || responseText.contains( "motor oil" ) )
		{
			modList.addModifier( "Sleaze Damage", "20" );
		}
		if ( responseText.contains( "MSG" ) || responseText.contains( "with flavor" ) )
		{
			modList.addModifier( "Critical Hit Percent", "15" );
		}
		if ( responseText.contains( "Norwhal milk" ) || responseText.contains( "norwhal milk" ) )
		{
			modList.addModifier( "Maximum HP Percent", "200" );
		}
		if ( responseText.contains( "Oil-paint" ) || responseText.contains( "oil paint" ) )
		{
			modList.addModifier( "Cold Damage", "5" );
			modList.addModifier( "Hot Damage", "5" );
			modList.addModifier( "Sleaze Damage", "5" );
			modList.addModifier( "Spooky Damage", "5" );
			modList.addModifier( "Stench Damage", "5" );
		}
		if ( responseText.contains( "Paradise milk" ) || responseText.contains( "paradise milk" ) || responseText.contains( "milk of paradise" ) )
		{
			modList.addModifier( "Muscle", "20" );
			modList.addModifier( "Mysticality", "20" );
			modList.addModifier( "Moxie", "20" );
		}
		if ( responseText.contains( "Rawhide" ) || responseText.contains( "rawhide" ) )
		{
			modList.addModifier( "Familiar Weight", "5" );
		}
		if ( responseText.contains( "Salted" ) || responseText.contains( "salt" ) )
		{
			modList.addModifier( "Critical Hit Percent", "5" );
		}
		if ( responseText.contains( "Sandalwood-infused" ) || responseText.contains( "sandalwood splinter" ) )
		{
			modList.addModifier( "Muscle", "5" );
			modList.addModifier( "Mysticality", "5" );
			modList.addModifier( "Moxie", "5" );
		}
		if ( responseText.contains( "Space pumpkin" ) || responseText.contains( "space pumpkin" ) )
		{
			modList.addModifier( "Muscle", "10" );
			modList.addModifier( "Mysticality", "10" );
			modList.addModifier( "Moxie", "10" );
		}
		if ( responseText.contains( "Spaghetti-squashy" ) || responseText.contains( "spaghetti squash spice" ) || responseText.contains( "extra squash" ) )
		{
			modList.addModifier( "Spell Damage", "20" );
		}
		if ( responseText.contains( "Squamous-salted" ) || responseText.contains( "squamous" ) )
		{
			modList.addModifier( "Spooky Resistance", "3" );
		}
		if ( responseText.contains( "Super-greasy" ) || responseText.contains( "mega sausage" ) || responseText.contains( "super gristle" ) )
		{
			modList.addModifier( "Moxie Percent", "50" );
		}
		if ( responseText.contains( "Teeth" ) || responseText.contains( "teeth" ) )
		{
			modList.addModifier( "Weapon Damage", "25" );
		}

		Preferences.setString( "_latteModifier", modList.toString() );
		Modifiers.overrideModifier( "Item:[" + ItemPool.LATTE_MUG + "]", modList.toString() );
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}
}
