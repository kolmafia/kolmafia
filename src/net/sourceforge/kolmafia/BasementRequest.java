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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.net.URLEncoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BasementRequest extends AdventureRequest
{
	private static final float SAFETY_MARGIN = 1.08f;

	private static int basementLevel = 0;
	private static float basementTestValue = 0;
	private static float basementTestCurrent = 0;

	private static String basementTestString = "";
	private static String gauntletString = "";

	private static int actualBoost = 0;
	private static int primaryBoost = 0;
	private static int secondaryBoost = 0;

	private static float averageResistanceNeeded = 0.0f;
	private static int element1 = -1, element2 = -1;
	private static int vulnerability = 0;
	private static int goodelement = -1;
	private static AdventureResult goodphial = null;
	private static AdventureResult goodeffect = null;
	private static int badelement1 = -1, badelement2 = -1, badelement3 = -1;
	private static AdventureResult badeffect1 = null, badeffect2 = null, badeffect3 = null;

	private static ArrayList desirableEffects = new ArrayList();

	private static float resistance1, resistance2;
	private static float expected1, expected2;

	private static String lastResponseText = "";
	private static String basementErrorMessage = null;

	private static final AdventureResult MUS_EQUAL = new AdventureResult( "Stabilizing Oiliness", 1, true );
	private static final AdventureResult MYS_EQUAL = new AdventureResult( "Expert Oiliness", 1, true );
	private static final AdventureResult MOX_EQUAL = new AdventureResult( "Slippery Oiliness", 1, true );

	private static final AdventureResult ASTRAL_SHELL = new AdventureResult( "Astral Shell", 1, true );
	private static final AdventureResult ELEMENTAL_SPHERE = new AdventureResult( "Elemental Saucesphere", 1, true );
	private static final AdventureResult BLACK_PAINT = new AdventureResult( "Red Door Syndrome", 1, true );

	private static final AdventureResult HOT_PHIAL = new AdventureResult( 1637, 1 );
	private static final AdventureResult COLD_PHIAL = new AdventureResult( 1638, 1 );
	private static final AdventureResult SPOOKY_PHIAL = new AdventureResult( 1639, 1 );
	private static final AdventureResult STENCH_PHIAL = new AdventureResult( 1640, 1 );
	private static final AdventureResult SLEAZE_PHIAL = new AdventureResult( 1641, 1 );

	public static final AdventureResult MAX_HOT = new AdventureResult( "Fireproof Lips", 1, true );
	public static final AdventureResult MAX_COLD = new AdventureResult( "Fever from the Flavor", 1, true );
	public static final AdventureResult MAX_SPOOKY = new AdventureResult( "Hyphemariffic", 1, true );
	public static final AdventureResult MAX_STENCH = new AdventureResult( "Can't Smell Nothin'", 1, true );
	public static final AdventureResult MAX_SLEAZE = new AdventureResult( "Hyperoffended", 1, true );

	private static final AdventureResult HOT_FORM = new AdventureResult( "Hotform", 1, true );
	private static final AdventureResult COLD_FORM = new AdventureResult( "Coldform", 1, true );
	private static final AdventureResult SPOOKY_FORM = new AdventureResult( "Spookyform", 1, true );
	private static final AdventureResult STENCH_FORM = new AdventureResult( "Stenchform", 1, true );
	private static final AdventureResult SLEAZE_FORM = new AdventureResult( "Sleazeform", 1, true );

	private static final Pattern BASEMENT_PATTERN = Pattern.compile( "Level ([\\d,]+)" );

	public static final AdventureResult [] ELEMENT_FORMS = new AdventureResult [] { HOT_FORM, COLD_FORM, SPOOKY_FORM, STENCH_FORM, SLEAZE_FORM };
	public static final AdventureResult [] ELEMENT_PHIALS = new AdventureResult [] { HOT_PHIAL, COLD_PHIAL, SPOOKY_PHIAL, STENCH_PHIAL, SLEAZE_PHIAL };

	public static final FamiliarData [] POSSIBLE_FAMILIARS = new FamiliarData [] {
		new FamiliarData( 18 ), // Hovering Sombrero
		new FamiliarData( 72 ), // Exotic Parrot
		new FamiliarData( 43 ), // Temporal Riftlet
		new FamiliarData( 50 ), // Wild Hare
		new FamiliarData( 53 ), // Astral Badger
		new FamiliarData( 70 ), // Green Pixie
	};

	/**
	 * Constructs a new <code>/code> which executes an
	 * adventure in Fernswarthy's Basement by posting to the provided form,
	 * notifying the givenof results (or errors).
	 *
	 * @param	adventureName	The name of the adventure location
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureId	The identifier for the adventure to be executed
	 */

	public BasementRequest( String adventureName )
	{
		super( adventureName, "basement.php", "0" );
	}

	public void run()
	{
		// Clear the data flags and probe the basement to see what we have.

		data.clear();
		super.run();

		// Load up the data variables and switch outfits if it's a fight.

		lastResponseText = responseText;
		checkBasement();

		// If we know we can't pass the test, give an error and bail out now.

		if ( basementErrorMessage != null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, basementErrorMessage );
			return;
		}

		// Decide which action to set. If it's a stat reward, always
		// boost prime stat.
		this.addFormField( "action", getBasementAction( this.responseText ) );

		// Attempt to pass the test.

		super.run();

		// Handle redirection

		if ( this.responseCode != 200 )
		{
			// If it was a fight and we won, good.

			if ( FightRequest.INSTANCE.responseCode == 200 && FightRequest.lastResponseText.indexOf( "<!--WINWINWIN-->" ) != -1 )
				return;

			// Otherwise ... what is this? Refetch the page and see if we passed test.

			this.data.clear();
			super.run();
		}

		// See what basement level we are on now and fail if we've not advanced.

		Matcher levelMatcher = BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() || basementLevel == StaticEntity.parseInt( levelMatcher.group(1) ) )
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to pass basement test." );
	}

	public static final String getBasementAction( String text )
	{
		if ( text.indexOf( "Got Silk?" ) != -1 )
			return KoLCharacter.isMoxieClass() ? "1" : "2";
		if ( text.indexOf( "Save the Dolls" ) != -1 )
			return KoLCharacter.isMysticalityClass() ? "1" : "2";
		if ( text.indexOf( "Take the Red Pill" ) != -1 )
			return KoLCharacter.isMuscleClass() ? "1" : "2";
		return "1";
	}

	public static final int getBasementLevel()
	{	return basementLevel;
	}

	public static final String getBasementLevelSummary()
	{
		if ( basementTestString.equals( "None" ) || basementTestString.startsWith( "Monster" ) )
			return "";

		if ( basementTestString.equals( "Elemental Resist" ) )
		{
			return basementTestString + " Test: " +
				COMMA_FORMAT.format( resistance1 ) + "% " + ( vulnerability == 1 ? "(vulnerable) " : "" ) + MonsterDatabase.elementNames[ element1 ] + " (" +
				COMMA_FORMAT.format( expected1 ) + " hp), " +
				COMMA_FORMAT.format( resistance2 ) + "% " + ( vulnerability == 2 ? "(vulnerable) " : "" ) + MonsterDatabase.elementNames[ element2 ] + " (" +
				COMMA_FORMAT.format( expected2 ) + " hp)";
		}

		if ( basementTestString.startsWith( "Encounter" ) )
			return basementTestString;

		if ( basementTestString.equals( "Maximum HP" ) )
			return basementTestString + " Test: " + COMMA_FORMAT.format( basementTestCurrent ) + " current, " + gauntletString + " needed";

		return basementTestString + " Test: " + COMMA_FORMAT.format( basementTestCurrent ) + " current, " +
			COMMA_FORMAT.format( basementTestValue ) + " needed";
	}

	public static final String getRequirement()
	{
		if ( basementTestString.equals( "Elemental Resist" ) )
		{
			return "<u>" + basementTestString + "</u><br/>Current: " +
				COMMA_FORMAT.format( resistance1 ) + "% " + ( vulnerability == 1 ? "(vulnerable) " : "" ) + MonsterDatabase.elementNames[ element1 ] + " (" +
				COMMA_FORMAT.format( expected1 ) + " hp), " +
				COMMA_FORMAT.format( resistance2 ) + "% " + ( vulnerability == 2 ? "(vulnerable) " : "" ) + MonsterDatabase.elementNames[ element2 ] + " (" +
				COMMA_FORMAT.format( expected2 ) + " hp)</br>" +
				"Needed: " + COMMA_FORMAT.format( averageResistanceNeeded ) + "% average resistance or " + goodeffect.getName();
		}

		if ( basementTestString.startsWith( "Monster" ) )
		{
			int index = basementTestString.indexOf( ": " );
			if ( index == -1 )
				return "";

			return "<u>Monster</u><br/>" + basementTestString.substring( index + 2 );
		}

		if ( basementTestString.equals( "Maximum HP" ) )
		{
			return "<u>" + basementTestString + "</u><br/>" +
				"Current: " + COMMA_FORMAT.format( basementTestCurrent ) + "<br/>" +
				"Needed: " + gauntletString;
		}

		return "<u>" + basementTestString + "</u><br/>" +
			"Current: " + COMMA_FORMAT.format( basementTestCurrent ) + "<br/>" +
			"Needed: " + COMMA_FORMAT.format( basementTestValue );
	}

	private static final void changeBasementOutfit( String name )
	{
		Object currentTest;
		String currentTestString;

		List available = KoLCharacter.getCustomOutfits();
		for ( int i = 0; i < available.size(); ++i )
		{
			currentTest = available.get(i);
			currentTestString = currentTest.toString().toLowerCase();

			if ( currentTestString.indexOf( name ) != -1 )
			{
				RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) currentTest ) );
				return;
			}
		}
	}

	private static final boolean checkForElementalTest( boolean autoSwitch, String responseText )
	{
		if ( responseText.indexOf( "<b>Peace, Bra!</b>" ) != -1 )
		{
			element1 = MonsterDatabase.STENCH;
			element2 = MonsterDatabase.SLEAZE;

			goodelement = element2;
			goodphial = SLEAZE_PHIAL;
			goodeffect = SLEAZE_FORM;

			// Stench is vulnerable to Sleaze
			badelement1 = MonsterDatabase.STENCH;
			badeffect1 = STENCH_FORM;

			// Spooky is vulnerable to Stench
			badelement2 = MonsterDatabase.SPOOKY;
			badeffect2 = SPOOKY_FORM;

			// Hot is vulnerable to Sleaze and Stench
			badelement3 = MonsterDatabase.HEAT;
			badeffect3 = HOT_FORM;
		}
		else if ( responseText.indexOf( "<b>Singled Out</b>" ) != -1 )
		{
			element1 = MonsterDatabase.COLD;
			element2 = MonsterDatabase.SLEAZE;

			goodelement = element1;
			goodphial = COLD_PHIAL;
			goodeffect = COLD_FORM;

			// Sleaze is vulnerable to Cold
			badelement1 = MonsterDatabase.SLEAZE;
			badeffect1 = SLEAZE_FORM;

			// Stench is vulnerable to Cold
			badelement2 = MonsterDatabase.STENCH;
			badeffect2 = STENCH_FORM;

			// Hot is vulnerable to Sleaze
			badelement3 = MonsterDatabase.HEAT;
			badeffect3 = HOT_FORM;
		}
		else if ( responseText.indexOf( "<b>Still Better than Pistachio</b>" ) != -1 )
		{
			element1 = MonsterDatabase.STENCH;
			element2 = MonsterDatabase.HEAT;

			goodelement = element1;
			goodphial = STENCH_PHIAL;
			goodeffect = STENCH_FORM;

			// Cold is vulnerable to Hot
			badelement1 = MonsterDatabase.COLD;
			badeffect1 = COLD_FORM;

			// Spooky is vulnerable to Hot
			badelement2 = MonsterDatabase.SPOOKY;
			badeffect2 = SPOOKY_FORM;

			// Hot is vulnerable to Stench
			badelement3 = MonsterDatabase.HEAT;
			badeffect3 = HOT_FORM;
		}
		else if ( responseText.indexOf( "<b>Unholy Writes</b>" ) != -1 )
		{
			element1 = MonsterDatabase.HEAT;
			element2 = MonsterDatabase.SPOOKY;

			goodelement = element1;
			goodphial = HOT_PHIAL;
			goodeffect = HOT_FORM;

			// Cold is vulnerable to Spooky
			badelement1 = MonsterDatabase.COLD;
			badeffect1 = COLD_FORM;

			// Spooky is vulnerable to Hot
			badelement2 = MonsterDatabase.SPOOKY;
			badeffect2 = SPOOKY_FORM;

			// Sleaze is vulnerable to Spooky
			badelement3 = MonsterDatabase.SLEAZE;
			badeffect3 = SLEAZE_FORM;
		}
		else if ( responseText.indexOf( "<b>The Unthawed</b>" ) != -1 )
		{
			element1 = MonsterDatabase.COLD;
			element2 = MonsterDatabase.SPOOKY;

			goodelement = element2;
			goodphial = SPOOKY_PHIAL;
			goodeffect = SPOOKY_FORM;

			// Cold is vulnerable to Spooky
			badelement1 = MonsterDatabase.COLD;
			badeffect1 = COLD_FORM;

			// Stench is vulnerable to Cold
			badelement2 = MonsterDatabase.STENCH;
			badeffect2 = STENCH_FORM;

			// Sleaze is vulnerable to Cold
			badelement3 = MonsterDatabase.SLEAZE;
			badeffect3 = SLEAZE_FORM;
		}
		else
		{
			// Not a known elemental test
			return false;
		}

		// Add the only beneficial elemental form for this test

		if ( !activeEffects.contains( goodeffect ) )
			desirableEffects.add( goodeffect );

		addDesiredEqualizer();

		// Add effects that resist the specific elements being tested
		addDesirableEffects( Modifiers.getPotentialChanges( Modifiers.elementalResistance( element1 ) ) );
		addDesirableEffects( Modifiers.getPotentialChanges( Modifiers.elementalResistance( element2 ) ) );

		// Add some effects that resist all elements
		if ( !activeEffects.contains( ASTRAL_SHELL ) )
			desirableEffects.add( ASTRAL_SHELL );

		if ( !activeEffects.contains( ELEMENTAL_SPHERE ) )
			desirableEffects.add( ELEMENTAL_SPHERE );

		if ( !activeEffects.contains( BLACK_PAINT ) )
			desirableEffects.add( BLACK_PAINT );

		actualBoost = Modifiers.HP;
		primaryBoost = Modifiers.MUS_PCT;
		secondaryBoost = Modifiers.MUS;

		if ( canHandleElementTest( autoSwitch, false ) )
			return true;

		if ( !autoSwitch )
			return true;

		changeBasementOutfit( "element" );
		canHandleElementTest( autoSwitch, true );
		return true;
	}

	private static final boolean canHandleElementTest( boolean autoSwitch, boolean switchedOutfits )
	{
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// total elemental damage is roughly 4.48 * x^1.4.  Assume the worst-case.

		float damage1 = (((float) Math.pow( basementLevel, 1.4 )) * 4.48f + 8.0f) * SAFETY_MARGIN;
		float damage2 = damage1;

		resistance1 = KoLCharacter.getElementalResistance( element1 );
		resistance2 = KoLCharacter.getElementalResistance( element2 );

		if ( activeEffects.contains( goodeffect ) )
		{
			if ( element1 == goodelement )
				resistance1 = 100.0f;
			else
				resistance2 = 100.0f;
		}

		vulnerability = 0;

		// If you have an elemental form which gives you vulnerability
		// to an element, you retain your elemental resistance (as
		// shown on the Character Sheet), but damage taken seems to be
		// quadrupled.
		if ( activeEffects.contains( badeffect1 ) || activeEffects.contains( badeffect2 ) || activeEffects.contains( badeffect3 ) )
		{
			if ( element1 == badelement1 || element1 == badelement2 || element1 == badelement3 )
			{
				vulnerability = 1;
				damage1 *= 4;
			}
			else
			{
				vulnerability = 2;
				damage2 *= 4;
			}
		}

		expected1 = Math.max( 1.0f, damage1 * ( (100.0f - resistance1) / 100.0f ) );
		expected2 = Math.max( 1.0f, damage2 * ( (100.0f - resistance2) / 100.0f ) );

		// If you can survive the current elemental test even without a phial,
		// then don't bother with any extra buffing.

		basementTestString = "Elemental Resist";
		averageResistanceNeeded = Math.max( 0, (int) Math.ceil( 100.0f * (1.0f - KoLCharacter.getMaximumHP() / (damage1 + damage2) )) );

		basementTestCurrent = KoLCharacter.getMaximumHP();
		basementTestValue = expected1 + expected2;

		if ( expected1 + expected2 < KoLCharacter.getCurrentHP() )
			return true;

		if ( expected1 + expected2 < KoLCharacter.getMaximumHP() )
		{
			if ( autoSwitch )
				StaticEntity.getClient().recoverHP( (int) (expected1 + expected2) );

			return KoLmafia.permitsContinue();
		}

		// If you already have the right phial effect, check to see if
		// it's sufficient.

		if ( activeEffects.contains( goodeffect ) )
			return false;

		// If you haven't switched outfits yet, it's possible that a simple
		// outfit switch will be sufficient to buff up.

		if ( !switchedOutfits )
			return false;

		// If you can't survive the test, even after an outfit switch, then
		// automatically fail.

		if ( expected1 >= expected2 )
		{
			if ( 1.0f + expected2 >= KoLCharacter.getMaximumHP() )
			{
				basementErrorMessage = "You must have at least " + basementTestValue + "% elemental resistance.";
				return false;
			}
		}
		else
		{
			if ( 1.0f + expected1 >= KoLCharacter.getMaximumHP() )
			{
				basementErrorMessage = "You must have at least " + basementTestValue + "% elemental resistance.";
				return false;
			}
		}

		if ( !autoSwitch )
		{
			basementErrorMessage = "You must have at least " + basementTestValue + "% elemental resistance.";
			return false;
		}

		// You can survive, but you need an elemental phial in order to do
		// so.  Go ahead and save it.

		for ( int i = 0; i < ELEMENT_FORMS.length; ++i )
			if ( activeEffects.contains( ELEMENT_FORMS[i] ) )
				(new UneffectRequest( ELEMENT_FORMS[i] )).run();

		(new ConsumeItemRequest( goodphial )).run();
		float damage = ( expected1 >= expected2 ) ? expected2 : expected1;
		StaticEntity.getClient().recoverHP( (int) (1.0f + damage) );

		return KoLmafia.permitsContinue();
	}

	private static final AdventureResult getDesiredEqualizer()
	{
		if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
			return MUS_EQUAL;

		if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
			return MYS_EQUAL;

		return MOX_EQUAL;
	}

	private static final void addDesiredEqualizer()
	{
		AdventureResult equalizer = getDesiredEqualizer();
		if ( !activeEffects.contains( equalizer ) )
			desirableEffects.add( equalizer );
	}

	private static final boolean checkForStatTest( boolean autoSwitch, String responseText )
	{
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// stat requirement is x^1.4 + 2.  Assume the worst-case.

		float statRequirement = ((float) Math.pow( basementLevel, 1.4 ) + 2.0f) * SAFETY_MARGIN;

		if ( responseText.indexOf( "Lift 'em" ) != -1 || responseText.indexOf( "Push it Real Good" ) != -1 || responseText.indexOf( "Ring that Bell" ) != -1 )
		{
			basementTestString = "Buffed Muscle";
			basementTestCurrent = KoLCharacter.getAdjustedMuscle();
			basementTestValue = (int) statRequirement;

			actualBoost = Modifiers.MUS;
			primaryBoost = Modifiers.MUS_PCT;
			secondaryBoost = Modifiers.MUS;

			addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "muscle" );

				if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
					basementErrorMessage = "You must have at least " + basementTestValue + " muscle.";
			}

			return true;
		}

		if ( responseText.indexOf( "Gathering:  The Magic" ) != -1 || responseText.indexOf( "Mop the Floor" ) != -1 || responseText.indexOf( "'doo" ) != -1 )
		{
			basementTestString = "Buffed Mysticality";
			basementTestCurrent = KoLCharacter.getAdjustedMysticality();
			basementTestValue = (int) statRequirement;

			actualBoost = Modifiers.MYS;
			primaryBoost = Modifiers.MYS_PCT;
			secondaryBoost = Modifiers.MYS;

			addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "mysticality" );

				if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
					basementErrorMessage = "You must have at least " + basementTestValue + " mysticality.";
			}

			return true;
		}

		if ( responseText.indexOf( "Don't Wake the Baby" ) != -1 || responseText.indexOf( "Grab a cue" ) != -1 || responseText.indexOf( "Smooth Moves" ) != -1 )
		{
			basementTestString = "Buffed Moxie";
			basementTestCurrent = KoLCharacter.getAdjustedMoxie();
			basementTestValue = (int) statRequirement;

			actualBoost = Modifiers.MOX;
			primaryBoost = Modifiers.MOX_PCT;
			secondaryBoost = Modifiers.MOX;

			addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "moxie" );

				if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
					basementErrorMessage = "You must have at least " + basementTestValue + " moxie.";
			}

			return true;
		}

		return false;
	}

	private static final boolean checkForDrainTest( boolean autoSwitch, String responseText )
	{

		if ( responseText.indexOf( "Grab the Handles" ) != -1 )
		{
			// According to
			// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
			// drain requirement is 1.67 * x^1.4 Assume worst-case.

			float drainRequirement = (float) Math.pow( basementLevel, 1.4 ) * 1.67f * SAFETY_MARGIN;

			basementTestString = "Maximum MP";
			basementTestCurrent = KoLCharacter.getMaximumMP();
			basementTestValue = (int) drainRequirement;

			actualBoost = Modifiers.MP;
			primaryBoost = Modifiers.MYS_PCT;
			secondaryBoost = Modifiers.MYS;

			addDesiredEqualizer();

			if ( KoLCharacter.getMaximumMP() < drainRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "mpdrain" );

				if ( KoLCharacter.getMaximumMP() < drainRequirement )
				{
					basementErrorMessage = "Insufficient mana to continue.";
					return true;
				}
			}

			if ( autoSwitch )
				StaticEntity.getClient().recoverMP( (int) drainRequirement );

			return true;
		}

		if ( responseText.indexOf( "Run the Gauntlet Gauntlet" ) != -1 )
		{
			// According to starwed at
			// http://forums.kingdomofloathing.com/viewtopic.php?t=83342&start=201
			// drain requirement is 10.0 * x^1.4. Assume worst-case.

			float drainRequirement = (float) Math.pow( basementLevel, 1.4 ) * 10.0f * SAFETY_MARGIN;

			basementTestString = "Maximum HP";
			basementTestCurrent = KoLCharacter.getMaximumHP();

			actualBoost = Modifiers.HP;
			primaryBoost = Modifiers.MUS_PCT;
			secondaryBoost = Modifiers.MUS;

			addDesiredEqualizer();

			float damageAbsorb = 1.0f - ((((float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f )) - 1.0f) / 10.0f);
			float healthRequirement = drainRequirement * damageAbsorb;

			basementTestValue = (int) healthRequirement;
			gauntletString =((int)drainRequirement) + " * " + FLOAT_FORMAT.format( damageAbsorb ) + " (" +
				KoLCharacter.getDamageAbsorption() + " DA) = " + COMMA_FORMAT.format( healthRequirement );

			if ( KoLCharacter.getMaximumHP() < healthRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "gauntlet" );

				damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
				healthRequirement = drainRequirement * damageAbsorb;
				basementTestValue = (int) healthRequirement;

				if ( KoLCharacter.getMaximumHP() < healthRequirement )
				{
					basementErrorMessage = "Insufficient health to continue.";
					return true;
				}
			}

			if ( autoSwitch )
				StaticEntity.getClient().recoverHP( (int) healthRequirement );

			return true;
		}

		return false;
	}

	private static final boolean checkForReward( String responseText )
	{
		if ( responseText.indexOf( "De Los Dioses" ) != -1 )
		{
			basementTestString = "Encounter: De Los Dioses";
			return true;
		}

		if ( responseText.indexOf( "The Dusk Zone" ) != -1 )
		{
			basementTestString = "Encounter: The Dusk Zone";
			return true;
		}

		if ( responseText.indexOf( "Giggity Bobbity Boo!" ) != -1 )
		{
			basementTestString = "Encounter: Giggity Bobbity Boo!";
			return true;
		}

		if ( responseText.indexOf( "No Good Deed" ) != -1 )
		{
			basementTestString = "Encounter: No Good Deed";
			return true;
		}

		if ( responseText.indexOf( "Fernswarthy's Basement, Level 500" ) != -1 )
		{
			basementTestString = "Encounter: Fernswarthy's Basement, Level 500";
			return true;
		}

		if ( responseText.indexOf( "Got Silk?" ) != -1 )
		{
			basementTestString = "Encounter: Got Silk?/Leather is Betther";
			return true;
		}

		if ( responseText.indexOf( "Save the Dolls" ) != -1 )
		{
			basementTestString = "Encounter: Save the Dolls/Save the Cardboard";
			return true;
		}

		if ( responseText.indexOf( "Take the Red Pill" ) != -1 )
		{
			basementTestString = "Encounter: Take the Red Pill/Take the Blue Pill";
			return true;
		}

		return false;
	}

	private static final String monsterLevelString()
	{
		float level = 2.0f * (float)Math.pow( basementLevel, 1.4 ) + KoLCharacter.getMonsterLevelAdjustment();
		return "Monster: Attack/Defense = " + (int)level;
	}

	private static final boolean checkForMonster( String responseText )
	{
		if ( responseText.indexOf( "Don't Fear the Ear" ) != -1 )
		{
			// The Beast with n Ears
			basementTestString = monsterLevelString();
			return true;
		}

		if ( responseText.indexOf( "Commence to Pokin" ) != -1 )
		{
			// The Beast with n Eyes
			basementTestString = monsterLevelString();
			return true;
		}

		if ( responseText.indexOf( "Stone Golem" ) != -1 )
		{
			// A n Stone Golem
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "Hydra" ) != -1 )
		{
			// A n-Headed Hydra
			basementTestString = monsterLevelString();
			return true;
		}

		if ( responseText.indexOf( "Toast the Ghost" ) != -1 )
		{
			// The Ghost of Fernswarthy's n great-grandfather
			basementTestString = monsterLevelString() + "<br>Physically resistant";
			return true;
		}

		if ( responseText.indexOf( "Bottles of Beer on a Golem" ) != -1 )
		{
			// N Bottles of Beer on a Golem
			basementTestString = monsterLevelString() + "<br>Blocks most spells";
			return true;
		}

		if ( responseText.indexOf( "Collapse That Waveform" ) != -1 )
		{
			// A n-Dimensional Horror
			basementTestString = monsterLevelString() + "<br>Blocks physical attacks";
			return true;
		}

		return false;
	}

	private static final void newBasementLevel( String responseText )
	{
		basementErrorMessage = null;
		basementTestString = "None";
		basementTestValue = 0;

		element1 = -1; element2 = -1;
		vulnerability = 0;

		goodelement = -1;
		goodphial = null;
		goodeffect = null;

		badeffect1 = null; badeffect2 = null; badeffect3 = null;
		badelement1 = -1; badelement2 = -1; badelement3 = -1;

		Matcher levelMatcher = BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() )
			return;

		basementLevel = StaticEntity.parseInt( levelMatcher.group(1) );
	}

	public static final void checkBasement()
	{
		KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( "basement.ash" ) );
		if ( interpreter != null )
			interpreter.execute( "main", new String[] { String.valueOf( basementLevel ), lastResponseText,
				String.valueOf( basementTestCurrent >= basementTestValue ) } );

		checkBasement( true, lastResponseText );
	}

	private static final boolean checkBasement( boolean autoSwitch, String responseText )
	{
		lastResponseText = responseText;

		desirableEffects.clear();
		newBasementLevel( responseText );

		if ( checkForReward( responseText ) )
			return false;

		if ( checkForElementalTest( autoSwitch, responseText ) )
			return true;

		if ( checkForStatTest( autoSwitch, responseText ) )
			return true;

		if ( checkForDrainTest( autoSwitch, responseText ) )
			return true;

		if ( !checkForMonster( responseText ) )
			// Presumably, it's a reward room
			return false;

		basementTestCurrent = 0;
		basementTestValue = 0;

		addDesiredEqualizer();

		actualBoost = Modifiers.HP;
		primaryBoost = Modifiers.MUS_PCT;
		secondaryBoost = Modifiers.MUS;

		if ( autoSwitch )
			changeBasementOutfit( "damage" );

		return true;
	}

	private static final void getDesiredEffects( ArrayList sourceList, ArrayList targetList )
	{
		Iterator it = sourceList.iterator();

		while ( it.hasNext() )
		{
			AdventureResult effect = (AdventureResult) it.next();
			if ( !wantEffect( effect ) )
				continue;

			DesiredEffect addition = new DesiredEffect( effect.getName() );

			if ( !targetList.contains( addition ) )
				targetList.add( addition );
		}
	}

	private static final void addDesirableEffects( ArrayList sourceList )
	{
		Iterator it = sourceList.iterator();

		while ( it.hasNext() )
		{
			AdventureResult effect = (AdventureResult) it.next();
			if ( wantEffect( effect ) && !desirableEffects.contains( effect ) )
				desirableEffects.add( effect );
		}
	}

	private static final boolean wantEffect( AdventureResult effect )
	{
		String action = MoodSettings.getDefaultAction( "lose_effect", effect.getName() );
		if ( action.equals( "" ) )
			return false;

		if ( action.startsWith( "cast" ) )
		{
			if ( !KoLCharacter.hasSkill( UneffectRequest.effectToSkill( effect.getName() ) ) )
				return false;
		}

		return true;
	}

	private static final ArrayList getDesiredEffects()
	{
		ArrayList targetList = new ArrayList();

		getDesiredEffects( desirableEffects, targetList );

		getDesiredEffects( Modifiers.getPotentialChanges( primaryBoost ), targetList );
		getDesiredEffects( Modifiers.getPotentialChanges( secondaryBoost ), targetList );

		if ( actualBoost == Modifiers.HP )
		{
			getDesiredEffects( Modifiers.getPotentialChanges( Modifiers.HP_PCT ), targetList );
			getDesiredEffects( Modifiers.getPotentialChanges( Modifiers.HP ), targetList );
		}
		else if ( actualBoost == Modifiers.MP )
		{
			getDesiredEffects( Modifiers.getPotentialChanges( Modifiers.MP_PCT ), targetList );
			getDesiredEffects( Modifiers.getPotentialChanges( Modifiers.MP ), targetList );
		}

		Collections.sort( targetList );
		return targetList;
	}

	public static final void decorate( StringBuffer buffer )
	{
		boolean hasCheck = checkBasement( false, buffer.toString() );

		if ( buffer.indexOf( "Got Silk?" ) != -1 )
		{
			addBasementChoiceSpoilers( buffer, "Moxie", "Muscle" );
			return;
		}

		if ( buffer.indexOf( "Save the Dolls" ) != -1 )
		{
			addBasementChoiceSpoilers( buffer, "Mysticality", "Moxie" );
			return;
		}

		if ( buffer.indexOf( "Take the Red Pill" ) != -1 )
		{
			addBasementChoiceSpoilers( buffer, "Muscle", "Mysticality" );
			return;
		}

		buffer.insert( buffer.indexOf( "</head>" ), "<script language=\"Javascript\" src=\"/basement.js\"></script></head>" );

		StringBuffer changes = new StringBuffer();
		changes.append( "<table>" );
		changes.append( "<tr><td><select id=\"gear\" style=\"width: 400px\"><option value=\"none\">- change your equipment -</option>" );

		// Add outfits

		SpecialOutfit outfit;
		for ( int i = 0; i < KoLCharacter.getCustomOutfits().size(); ++i )
		{
			outfit = (SpecialOutfit) KoLCharacter.getCustomOutfits().get(i);

			changes.append( "<option value=\"outfit+" );

			try
			{
				changes.append( URLEncoder.encode( outfit.getName(), "UTF-8" ) );
			}
			catch ( Exception e )
			{
				changes.append( StaticEntity.globalStringReplace( outfit.getName(), " ", "+" ) );
			}

			changes.append( "\">outfit " );
			changes.append( outfit.getName().substring(8) );
			changes.append( "</option>" );
		}

		for ( int i = 0; i < POSSIBLE_FAMILIARS.length; ++i )
		{
			if ( !KoLCharacter.getFamiliarList().contains( POSSIBLE_FAMILIARS[i] ) )
				continue;

			changes.append( "<option value=\"familiar+" );
			changes.append( StaticEntity.globalStringReplace( POSSIBLE_FAMILIARS[i].getRace(), " ", "+" ) );
			changes.append( "\">familiar " );
			changes.append( POSSIBLE_FAMILIARS[i].getRace() );
			changes.append( "</option>" );
		}

		changes.append( "</select></td><td>&nbsp;</td><td valign=top align=left><input type=\"button\" value=\"exec\" onClick=\"changeBasementGear();\"></td></tr>" );

		// Add effects

		ArrayList listedEffects = getDesiredEffects();

		if ( !listedEffects.isEmpty() )
		{
			String computeFunction = "computeNetBoost(" + ((int) basementTestCurrent) + "," + ((int) basementTestValue) + ");";

			String modifierName = Modifiers.getModifierName( actualBoost );
			modifierName = StaticEntity.globalStringDelete( modifierName, "Maximum " ).toLowerCase();

			changes.append( "<tr><td><select onChange=\"" );
			changes.append( computeFunction );
			changes.append( "\" id=\"potion\" style=\"width: 400px\" multiple size=5>" );

			if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
				changes.append( "<option value=0>use 1 scroll of drastic healing (hp restore)</option>" );

			if ( KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			{
				changes.append( "<option value=0" );

				if ( KoLCharacter.getFullness() == KoLCharacter.getFullnessLimit() )
					changes.append( " disabled" );

				changes.append( ">eat 1 Jumbo Dr. Lucifer (mp restore)</option>" );
			}

			for ( int i = 0; i < listedEffects.size(); ++i )
				appendBasementEffect( changes, (DesiredEffect) listedEffects.get(i) );

			changes.append( "</select></td><td>&nbsp;</td><td valign=top align=left>" );
			changes.append( "<input type=\"button\" value=\"exec\" onClick=\"changeBasementEffects();\">" );
			changes.append( "<br/><br/><font size=-1><nobr id=\"changevalue\">" );
			changes.append( (int) basementTestCurrent );
			changes.append( "</nobr><br/><nobr id=\"changetarget\">" );
			changes.append( (int) basementTestValue );
			changes.append( "</nobr></td></tr>" );
		}

		changes.append( "</table>" );
		buffer.insert( buffer.indexOf( "</center><blockquote>" ), changes.toString() );

		if ( hasCheck )
		{
			String checkString = getRequirement();
			buffer.insert( buffer.lastIndexOf( "</b>" ) + 4, "<br/>" );
			buffer.insert( buffer.lastIndexOf( "<img" ), "<table><tr><td>" );
			buffer.insert( buffer.indexOf( ">", buffer.lastIndexOf( "<img" ) ) + 1, "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><font id=\"spoiler\" size=2>" +
				checkString + "</font></td></tr></table>" );
		}
	}

	private static final void appendBasementEffect( StringBuffer changes, DesiredEffect effect )
	{
		changes.append( "<option value=" );
		changes.append( effect.effectiveBoost );

		if ( effect.action.startsWith( "chew" ) && KoLCharacter.getSpleenUse() == KoLCharacter.getSpleenLimit() )
			changes.append( " disabled" );

		changes.append( ">" );

		if ( !effect.itemAvailable )
			changes.append( "acquire and " );

		changes.append( effect.action );
		changes.append( " (" );

		if ( effect.computedBoost == 0.0f )
		{
			if ( effect.name.equals( MUS_EQUAL.getName() ) || effect.name.equals( MYS_EQUAL.getName() ) || effect.name.equals( MOX_EQUAL.getName() ))
			{
				changes.append( "stat equalizer" );
			}
			else
			{
				boolean isImmunity = false;
				for ( int j = 0; j < ELEMENT_FORMS.length; ++j )
					isImmunity |= effect.name.equals( ELEMENT_FORMS[j].getName() );

				if ( isImmunity )
					changes.append( "element immunity" );
				else
					changes.append( "element resist" );
			}
		}
		else
		{
			changes.append( "+" );
			changes.append( COMMA_FORMAT.format( effect.effectiveBoost ) );
		}

		changes.append( ")</option>" );
	}

	private static final void addBasementChoiceSpoilers( StringBuffer buffer, String choice1, String choice2 )
	{
		String text = buffer.toString();
		buffer.setLength(0);

		int index1 = 0, index2;

		// Add first choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice1 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add second choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice2 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Append remainder of buffer
		buffer.append( text.substring( index1 ) );
	}

	private static class DesiredEffect implements Comparable
	{
		private String name, action;
		private int computedBoost;
		private int effectiveBoost;
		private boolean itemAvailable;

		public DesiredEffect( String name )
		{
			this.name = name;

			this.computedBoost = (int) Math.ceil( computeBoost() );
			this.effectiveBoost = this.computedBoost > 0.0f ? this.computedBoost : 0 - this.computedBoost;

			this.action = this.computedBoost < 0 ? "uneffect " + name :
				MoodSettings.getDefaultAction( "lose_effect", name );

			this.itemAvailable = true;

			if ( this.action.startsWith( "use" ) )
			{
				AdventureResult item = KoLmafiaCLI.getFirstMatchingItem( action.substring(4).trim(), false );
				if ( item == null || !KoLCharacter.hasItem( item ) )
					this.itemAvailable = false;
			}
		}

		public boolean equals( Object o )
		{
			return o instanceof DesiredEffect && this.name.equals( ((DesiredEffect)o).name );
		}

		public int compareTo( Object o )
		{
			if ( this.effectiveBoost == 0.0f )
				return ((DesiredEffect)o).effectiveBoost != 0.0f ? -1 : this.name.compareToIgnoreCase( ((DesiredEffect)o).name );

			if ( ((DesiredEffect)o).effectiveBoost == 0.0f )
				return 1;

			if ( this.effectiveBoost != ((DesiredEffect)o).effectiveBoost )
				return this.effectiveBoost > ((DesiredEffect)o).effectiveBoost ? -1 : 1;

			return this.name.compareToIgnoreCase( ((DesiredEffect)o).name );
		}

		public float computeBoost()
		{
			Modifiers m = Modifiers.getModifiers( this.name );
			if ( m == null )
				return 0.0f;

			float base = getEqualizedStat();
			float boost = m.get( secondaryBoost ) + m.get( primaryBoost ) * base / 100.0f;

			if ( actualBoost == Modifiers.HP )
			{
				if ( KoLCharacter.isMuscleClass() )
					boost *= 1.5f;

				boost += m.get( Modifiers.HP ) + m.get( Modifiers.HP_PCT ) * base / 100.0f;
			}

			if ( actualBoost == Modifiers.MP )
			{
				if ( KoLCharacter.isMysticalityClass() )
					boost *= 1.5f;

				boost += m.get( Modifiers.MP ) + m.get( Modifiers.MP_PCT ) * base / 100.0f;
			}

			return boost;
		}

		public float getEqualizedStat()
		{
			float currentStat = 0.0f;
			switch ( primaryBoost )
			{
			case Modifiers.MUS_PCT:
				currentStat = KoLCharacter.getBaseMuscle();
				break;
			case Modifiers.MYS_PCT:
				currentStat = KoLCharacter.getBaseMysticality();
				break;
			case Modifiers.MOX_PCT:
				currentStat = KoLCharacter.getBaseMoxie();
				break;
			default:
				return Modifiers.getModifiers( this.name ).get( primaryBoost );
			}

			if ( activeEffects.contains( MUS_EQUAL ) )
				currentStat = Math.max( KoLCharacter.getBaseMuscle(), currentStat );
			if ( activeEffects.contains( MYS_EQUAL ) )
				currentStat = Math.max( KoLCharacter.getBaseMysticality(), currentStat );
			if ( activeEffects.contains( MOX_EQUAL ) )
				currentStat = Math.max( KoLCharacter.getBaseMoxie(), currentStat );

			return currentStat;
		}
	}
}
