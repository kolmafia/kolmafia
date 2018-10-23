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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LatteRequest
	extends GenericRequest
{
	private static final Pattern REFILL_PATTERN = Pattern.compile( "You've got <b>(\\d+)</b> refill" );
	private static final Pattern LINE_PATTERN = Pattern.compile( "<tr style=.*?</tr>", Pattern.DOTALL );
	private static final Pattern INPUT_PATTERN = Pattern.compile( "name=\\\"l(\\d)\\\" (?:|checked) value=\\\"(.*?)\\\">\\s(.*?)\\s</td>" );
	private static final Pattern RESULT_PATTERN = Pattern.compile( "You get your mug filled with a delicious (.*?) Latte (.*?)\\.</span>" );

	private static final String [][] LATTE = new String[][]
	{
		// Ingredient, Location, First group name, second group name, third group name, modifier string, discovery text, radio 1, radio 2, radio 3
		{ "ancient", "The Mouldering Mansion", "Ancient exotic spiced", "ancient/spicy", "with ancient spice", "Spooky Damage: 50", "urn full of ancient spices" },
		{ "basil", "The Overgrown Lot", "Basil and", "basil", "with basil", "HP Regen Min: 5, HP Regen Max: 5", "clump of wild basil" },
		{ "belgian", "Whitey's Grove", "Belgian vanilla", "Belgian vanilla", "with a shot of Belgian vanilla", "Muscle Percent: 20, Mysticality Percent: 20, Moxie Percent: 20", "a large vanilla bean pod" },
		{ "bug-thistle", "The Bugbear Pen", "Bug-thistle", "bug-thistle", "with a sprig of bug-thistle", "Mysticality: 20", "patch of bug-thistle" },
		{ "butternut", "Madness Bakery", "Butternutty", "butternut-spice", "with butternut", "Spell Damage: 10", "find a butternut squash" },
		{ "cajun", "The Black Forest", "Cajun", "cajun spice", "with cajun spice", "Meat Drop: 40", "Cayenne you add this to the menu" },
		{ "chalk", "The Haunted Billiards Room", "Blue chalk and", "blue chalk", "with blue chalk", "Cold Damage: 25", "box of blue chalk cubes" },
		{ "cinnamon", null, "Cinna-", "cinnamon", "with a shake of cinnamon", "Experience (Moxie): 1, Moxie Percent: 5, Pickpocket Chance: 5", null },
		{ "carrot", "The Dire Warren", "Carrot", "carrot", "with carrot", "Item Drop: 20", "bunch of carrots" },
		{ "carrrdamom", "Barrrney's Barrr", "Carrrdamom-scented", "carrrdamom", "with carrrdamom", "MP Regen Min: 4, MP Regen Max: 6", "A carrrdamom" },
		{ "chili", "The Haunted Kitchen", "Chili", "chili seeds", "with a kick", "Hot Resistance: 3", "jar of chili seeds" },
		{ "cloves", "The Sleazy Back Alley", "Cloven", "cloves", "with a puff of cloves", "Stench Resistance: 3", "little tin of ground cloves" },
		{ "coal", "The Haunted Boiler Room", "Coal-boiled", "coal", "with a lump of hot coal", "Hot Damage: 25", "brazier of burning coals" },
		{ "cocoa", "The Icy Peak", "Cocoa", "cocoa powder", "mocha loca", "Cold Resistance: 3", "packet of cocoa powder" },
		{ "diet", "Battlefield (No Uniform)", "Diet", "diet soda", "with diet soda syrup", "Initiative: 50", "jug of diet soda syrup" },
		{ "dwarf", "Itznotyerzitz Mine", "Dwarf creamed", "dwarf cream", "with dwarf cream", "Muscle: 30", "milking a stalactite" },
		{ "dyspepsi", "TBC", "Dyspepsi-flavored", "Dyspepsi", "with a shot of Dyspepsi syrup", "Initiative: 25", "TBC" },
		{ "filth", "The Feeding Chamber", "Filthy", "filth milk", "with filth milk", "Damage Reduction: 20", "filth milk" },
		{ "flour", "The Road to the White Citadel", "Floured", "white flour", "dusted with flour", "Sleaze Resistance: 3", "bag of all-purpose flour" },
		{ "fungus", "The Fungal Nethers", "Fungal", "fungus", "with fungal scrapings", "Maximum MP: 30", "patch of mushrooms" },
		{ "grass", "The Hidden Park", "Fresh grass and", "fresh grass", "with fresh-cut grass", "Experience: 3", "pile of fresh lawn clippings" },
		{ "greasy", "Cobb's Knob Barracks", "Extra-greasy", "hot sausage", "with extra gristle", "Muscle Percent: 50", "big greasy sausage" },
		{ "healing", "The Daily Dungeon", "Extra-healthy", "health potion", "with a shot of healing elixir", "HP Regen Min: 10, HP Regen Max: 20", "jug full of red syrup" },
		{ "hellion", "The Dark Neck of the Woods", "Hellish", "hellion", "with hellion", "PvP Fights: 6", "small pile of hellion cubes" },
		{ "greek", "Wartime Frat House (Hippy Disguise)", "Greek spice", "greek spice", "with greek spice", "Sleaze Damage: 25", "ΣΠΙΣΗΣ" },
		{ "grobold", "The Old Rubee Mine", "Grobold rum and", "grobold rum", "with a shot of grobold rum", "Sleaze Damage: 25", "stash of grobold rum" },
		{ "guarna", "The Bat Hole Entrance", "Guarna and", "guarna", "infused with guarna", "Adventures: 4", "patch of guarana plants" },
		{ "gunpowder", "1st Floor, Shiawase-Mitsuhama Building", "Gunpowder and", "gunpowder", "with gunpowder", "Weapon Damage: 50", "jar of gunpowder" },
		{ "hobo", "Hobopolis Town Square", "Hobo-spiced", "hobo spice", "with hobo spice", "Damage Absorption: 50", "Hobo Spices" },
		{ "ink", "The Haunted Library", "Inky", "ink", "with ink", "Combat Rate: -5", "large bottle of india ink" },
		{ "kombucha", "Wartime Hippy Camp (Frat Disguise)", "Kombucha-infused", "kombucha", "with a kombucha chaser", "Stench Damage: 25", "bottle of homemade kombucha" },
		{ "lihc", "The Defiled Niche", "Lihc-licked", "lihc saliva", "with lihc spit", "Spooky Damage: 25", "collect some of the saliva in a jar" },
		{ "lizard", "The Arid, Extra-Dry Desert", "Lizard milk and", "lizard milk", "with lizard milk", "MP Regen Min: 5, MP Regen Max: 15", "must be lizard milk" },
		{ "mega", "Cobb's Knob Laboratory", "Super-greasy", "mega sausage", "with super gristle", "Moxie Percent: 50", "biggest sausage you've ever seen" },
		{ "mold", "The Unquiet Garves", "Moldy", "grave mold", "with grave mold", "Spooky Damage: 20", "covered with mold" },
		{ "msg", "The Briniest Deepests", "MSG-Laced", "MSG", "with flavor", "Critical Hit Percent: 15", "pure MSG from the ocean" },
		{ "noodles", "The Haunted Pantry", "Carb-loaded", "macaroni", "with extra noodles", "Maximum HP: 20", "espresso-grade noodles" },
		{ "norwhal", "The Ice Hole", "Norwhal milk and", "norwhal milk", "with norwhal milk", "Maximum HP Percent: 200", "especially Nordic flavor to it" },
		{ "oil", "The Old Landfill", "Motor oil and", "motor oil", "with motor oil", "Sleaze Damage: 20", "puddle of old motor oil" },
		{ "paint", "The Haunted Gallery", "Oil-paint and", "oil paint", "with oil paint", "Cold Damage: 5, Hot Damage: 5, Sleaze Damage: 5, Spooky Damage: 5, Stench Damage: 5", "large painter's pallette" },
		{ "paradise", "The Stately Pleasure Dome", "Paradise milk", "paradise milk", "with milk of paradise", "Muscle: 20, Mysticality: 20, Moxie: 20", "Milk of Paradise" },
		{ "pumpkin", null, "Autumnal", "pumpkin spice", "with a hint of autumn", "Experience (Mysticality): 1, Mysticality Percent: 5, Spell Damage: 5", null },
		{ "rawhide", "The Spooky Forest", "Rawhide", "rawhide", "with rawhide", "Familiar Weight: 5", "stash of rawhide dog chews" },
		{ "rock", "The Brinier Deepers", "Extra-salty", "rock salt", "with rock salt", "Critical Hit Percent: 10", "large salt deposits" },
		{ "salt", "The Briny Deeps", "Salted", "salt", "with salt", "Critical Hit Percent: 5", "distill some of the salt" },
		{ "sandalwood", "Noob Cave", "Sandalwood-infused", "sandalwood splinter", "with sandalwood splinters", "Muscle: 5, Mysticality: 5, Moxie: 5", "made of sandalwood" },
		{ "sausage", "Cobb's Knob Kitchens", "Greasy", "sausage", "with gristle", "Mysticality Percent: 50", "full of sausages" },
		{ "space", "The Hole in the Sky", "Space pumpkin and", "space pumpkin", "with space pumpkin juice", "Muscle: 10, Mysticality: 10, Moxie: 10", "some kind of brown powder" },
		{ "squash", "The Copperhead Club", "Spaghetti-squashy", "spaghetti squash spice", "with extra squash", "Spell Damage: 20", "steal a spaghetti squash" },
		{ "squamous", "The Caliginous Abyss", "Squamous-salted", "squamous", "with squamous salt", "Spooky Resistance: 3", "break off a shard" },
		{ "teeth", "The VERY Unquiet Garves", "Teeth", "teeth", "with teeth in it", "Spooky Damage: 25, Weapon Damage: 25", "handful of loose teeth" },
		{ "vanilla", null, "Vanilla", "vanilla", "with a shot of vanilla", "Experience (Muscle): 1, Muscle Percent: 5, Weapon Damage: 5", null },
		{ "venom", "The Middle Chamber", "Envenomed", "asp venom", "with extra poison", "Weapon Damage: 25", "wring the poison out of it into a jar" },
		{ "vitamins", "The Dark Elbow of the Woods", "Fortified", "vitamin", "enriched with vitamins", "Experience (familiar): 3", "specifically vitamins G, L, P, and W" },
		{ "wing", "The Dark Heart of the Woods", "Hot wing and", "hot wing", "with a hot wing in it", "Combat Rate: 5", "plate of hot wings" },
	};

	private static final int INGREDIENT = 0;
	private static final int LOCATION = 1;
	private static final int FIRST = 2;
	private static final int SECOND = 3;
	private static final int THIRD = 4;
	private static final int MOD = 5;
	private static final int DISCOVER = 6;

	private static String[][] radio = new String[LATTE.length][3];

	public LatteRequest()
	{
		super( "choice.php" );
	}

	public static void refill( final String first, final String second, final String third )
	{
		int[] ingredients = new int[3];
		for ( int i = 0; i < 3; ++i )
		{
			ingredients[i] = -1;
		}
		for ( int i = 0; i < LATTE.length; ++i )
		{
			if ( first.equals( LATTE[i][INGREDIENT] ) )
			{
				ingredients[0] = i;
			}
			if ( second.equals( LATTE[i][INGREDIENT] ) )
			{
				ingredients[1] = i;
			}
			if ( third.equals( LATTE[i][INGREDIENT] ) )
			{
				ingredients[2] = i;
			}
		}

		// Check unlocks and choice options
		GenericRequest request = new GenericRequest( "main.php?latte=1", false ) ;
		RequestThread.postRequest( request );
		LatteRequest.parseChoiceOptions( request.responseText );
		String l1 = radio[ingredients[0]][0];
		String l2 = radio[ingredients[1]][1];
		String l3 = radio[ingredients[2]][2];

		boolean checksOk = true;
		for ( int i = 0; i < 3; ++i )
		{
			if ( ingredients[i] == -1 )
			{
				String message = null;
				switch ( i )
				{
				case 0:
					message = "Cannot find ingredient " + first + ". Use 'latte unlocked' to see available ingredients.";
					break;
				case 1:
					message = "Cannot find ingredient " + second + ". Use 'latte unlocked' to see available ingredients.";
					break;
				case 2:
					message = "Cannot find ingredient " + third + ". Use 'latte unlocked' to see available ingredients.";
					break;
				}
				KoLmafia.updateDisplay( MafiaState.ERROR, message );
				checksOk = false;
			}
			else if ( !Preferences.getString( "latteUnlocks" ).contains( LATTE[ingredients[i]][INGREDIENT] ) )
			{
				String message = "Ingredient " + LATTE[ingredients[i]][INGREDIENT] + " is not unlocked. Use 'latte unlocks' to see how to unlock it.";
				KoLmafia.updateDisplay( MafiaState.ERROR, message );
				checksOk = false;
			}
		}
		if ( !checksOk )
		{
			return;
		}

		// Refill Latte
		String message = "Filling mug with " + LATTE[ingredients[0]][FIRST] + " " + LATTE[ingredients[1]][SECOND] + " Latte " + LATTE[ingredients[2]][THIRD] + ".";
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		GenericRequest requestNew = new GenericRequest( "choice.php?whichchoice=1329&option=1&l1=" + l1 + "&l2=" + l2 + "&l3=" + l3 ) ;
		RequestThread.postRequest( requestNew );
	}

	public static final void listUnlocks( final boolean all )
	{
		StringBuilder output = new StringBuilder();
		output.append( "<table border=2 cols=3>" );
		output.append( "<tr>" );
		output.append( "<th>Ingredient</th>" );
		output.append( "<th>Unlock</th>" );
		output.append( "<th>Modifier</th>" );
		output.append( "</tr>" );

		for ( int i = 0; i < LATTE.length; ++i )
		{
			boolean unlocked = Preferences.getString( "latteUnlocks" ).contains( LATTE[i][INGREDIENT] );
			if ( all || unlocked )
			{
				output.append( "<tr>" );
				output.append( "<td>" );
				output.append( LATTE[i][INGREDIENT] );
				output.append( "</td>" );
				output.append( "<td>" );
				if ( unlocked )
				{
					output.append( "unlocked" );
				}
				else
				{
					output.append( "Unlock in ");
					output.append( LATTE[i][LOCATION] );
				}
				output.append( "</td>" );
				output.append( "<td>" );
				output.append( LATTE[i][MOD] );
				output.append( "</td>" );
				output.append( "</tr>" );
			}
		}
		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
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
		if ( matcher.find() )
		{
			String first = null;
			String second = null;
			String third = null;
			String[] mods = new String[3];
			String start = matcher.group( 1 ).trim();
			String middle = null;
			String end = matcher.group( 2 ).trim();
			
			for ( int i = 0; i < LATTE.length; ++i )
			{
				if ( start.startsWith( LATTE[i][FIRST] ) )
				{
					mods[0] = LATTE[i][MOD];
					first = LATTE[i][FIRST];
					middle = start.replace( LATTE[i][FIRST], "" ).trim();
					break;
				}
			}

			for ( int i = 0; i < LATTE.length; ++i )
			{
				if ( middle.equals( LATTE[i][SECOND] ) )
				{
					mods[1] = LATTE[i][MOD];
					second = LATTE[i][SECOND];
					if ( third != null )
					{
						break;
					}
				}
				if ( end.equals( LATTE[i][THIRD] ) )
				{
					mods[2] = LATTE[i][MOD];
					third = LATTE[i][THIRD];
					if ( second != null )
					{
						break;
					}
				}
			}

			String message = "Filled your mug with " + first + " " + second + " Latte " + third + ".";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );

			ModifierList modList = new ModifierList();
			for ( int i = 0 ; i < 3 ; ++i )
			{
				ModifierList addModList = Modifiers.splitModifiers( mods[i] );
				for ( Modifier modifier : addModList )
				{
					modList.addToModifier( modifier );
				}
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

	public static final void parseFight( final String location, final String responseText )
	{
		if ( location == null || responseText == null )
		{
			return;
		}

		for ( int i = 0; i < LATTE.length; ++i )
		{
			if ( LATTE[i][LOCATION] == null )
			{
				continue;
			}
			if ( location.equals( LATTE[i][LOCATION] ) )
			{
				if ( responseText.contains( LATTE[i][DISCOVER] ) )
				{
					String pref = Preferences.getString( "latteUnlocks" );
					if ( !pref.contains( LATTE[i][INGREDIENT] ) )
					{
						Preferences.setString( "latteUnlocks", pref + "," + LATTE[i][INGREDIENT] );
						String message = "Unlocked " + LATTE[i][INGREDIENT] + " for Latte.";
						RequestLogger.printLine( message );
						RequestLogger.updateSessionLog( message );
					}
				}
				break;
			}
		}		
	}

	public static final void parseVisitChoice( final String text )
	{
		Matcher matcher = LatteRequest.REFILL_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			Preferences.setInteger( "_latteRefillsUsed", 3 - StringUtilities.parseInt( matcher.group( 1 ) ) );
		}
		matcher = LatteRequest.LINE_PATTERN.matcher( text );
		StringBuilder unlocks = new StringBuilder();
		while ( matcher.find() )
		{
			String line = matcher.group( 0 );
			Matcher lineMatcher = LatteRequest.INPUT_PATTERN.matcher( line );
			if ( lineMatcher.find() )
			{
				String description = lineMatcher.group( 3 ).trim();
				for ( int i = 0; i < LATTE.length; ++i )
				{
					if ( description.equals( LATTE[i][FIRST] ) )
					{
						if ( !line.contains( "&Dagger;" ) )
						{
							if ( unlocks.length() != 0 )
							{
								unlocks.append( "," );
							}
							unlocks.append( LATTE[i][INGREDIENT] );
						}
						break;
					}
				}
			}
		}
		Preferences.setString( "latteUnlocks", unlocks.toString() );
	}

	public static final void parseChoiceOptions( final String text )
	{
		Matcher matcher = LatteRequest.REFILL_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			Preferences.setInteger( "_latteRefillsUsed", 3 - StringUtilities.parseInt( matcher.group( 1 ) ) );
		}
		matcher = LatteRequest.LINE_PATTERN.matcher( text );
		StringBuilder unlocks = new StringBuilder();
		while ( matcher.find() )
		{
			String line = matcher.group( 0 );
			Matcher lineMatcher = LatteRequest.INPUT_PATTERN.matcher( line );
			while ( lineMatcher.find() )
			{
				int button = StringUtilities.parseInt( lineMatcher.group( 1 ) );
				String value = lineMatcher.group( 2 );
				String description = lineMatcher.group( 3 ).trim();
				for ( int i = 0; i < LATTE.length; ++i )
				{
					if ( description.equals( LATTE[i][button+1] ) )
					{
						radio[i][button-1] = value;
						if ( button == 1 && !line.contains( "&Dagger;" ) )
						{
							if ( unlocks.length() != 0 )
							{
								unlocks.append( "," );
							}
							unlocks.append( LATTE[i][INGREDIENT] );
						}
						break;
					}
				}
			}
		}
		Preferences.setString( "latteUnlocks", unlocks.toString() );
	}
}
