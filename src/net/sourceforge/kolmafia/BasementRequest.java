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

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BasementRequest extends AdventureRequest
{
	private static int basementLevel = 0;
	private static float basementTestValue = 0;
	private static float basementTestCurrent = 0;
	private static String basementTestString = "";

	private static int element1 = -1, element2 = -1;
	private static int goodelement = -1;
	private static AdventureResult goodphial = null;
	private static AdventureResult goodeffect = null;
	private static int badelement1 = -1, badelement2 = -1, badelement3 = -1;
	private static AdventureResult badeffect1 = null, badeffect2 = null, badeffect3 = null;

	private static float resistance1, resistance2;
	private static float expected1, expected2;

	private static String basementErrorMessage = null;

	private static final AdventureResult HOT_PHIAL = new AdventureResult( 1637, 1 );
	private static final AdventureResult COLD_PHIAL = new AdventureResult( 1638, 1 );
	private static final AdventureResult SPOOKY_PHIAL = new AdventureResult( 1639, 1 );
	private static final AdventureResult STENCH_PHIAL = new AdventureResult( 1640, 1 );
	private static final AdventureResult SLEAZE_PHIAL = new AdventureResult( 1641, 1 );

	private static final AdventureResult HOT_FORM = new AdventureResult( "Hotform", 1, true );
	private static final AdventureResult COLD_FORM = new AdventureResult( "Coldform", 1, true );
	private static final AdventureResult SPOOKY_FORM = new AdventureResult( "Spookyform", 1, true );
	private static final AdventureResult STENCH_FORM = new AdventureResult( "Stenchform", 1, true );
	private static final AdventureResult SLEAZE_FORM = new AdventureResult( "Sleazeform", 1, true );

	private static final Pattern BASEMENT_PATTERN = Pattern.compile( "Level ([\\d,]+)" );
	private static final AdventureResult [] ELEMENT_FORMS = new AdventureResult [] { HOT_FORM, COLD_FORM, SPOOKY_FORM, STENCH_FORM, SLEAZE_FORM };

	/**
	 * Constructs a new <code>BasementRequest</code> which executes an
	 * adventure in Fernswarthy's Basement by posting to the provided form,
	 * notifying the givenof results (or errors).
	 *
	 * @param	adventureName	The name of the adventure location
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureId	The identifer for the adventure to be executed
	 */

	public BasementRequest()
	{
		super( "Fernswarthy's Basement", "basement.php", "0" );
	}

	public void run()
	{
		data.clear();
		super.run();
		handleBasement();
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
				COMMA_FORMAT.format( resistance1 ) + "% " + MonsterDatabase.elementNames[ element1 ] + " (" +
				COMMA_FORMAT.format( expected1 ) + " hp), " +
				COMMA_FORMAT.format( resistance2 ) + "% " + MonsterDatabase.elementNames[ element2 ] + " (" +
				COMMA_FORMAT.format( expected2 ) + " hp)";
		}

		if ( basementTestString.startsWith( "Encounter" ) )
			return basementTestString;

		return basementTestString + " Test: " + COMMA_FORMAT.format( basementTestCurrent ) + " current, " +
			COMMA_FORMAT.format( basementTestValue ) + " needed";
	}

	public static final String getRequirement()
	{
		if ( basementTestString.equals( "Elemental Resist" ) )
		{
			return "<u>" + basementTestString + "</u><br/>Current: " +
				COMMA_FORMAT.format( resistance1 ) + "% " + MonsterDatabase.elementNames[ element1 ] + " (" +
				COMMA_FORMAT.format( expected1 ) + " hp), " +
				COMMA_FORMAT.format( resistance2 ) + "% " + MonsterDatabase.elementNames[ element2 ] + " (" +
				COMMA_FORMAT.format( expected2 ) + " hp)</br>" +
				"Needed: " + COMMA_FORMAT.format( basementTestValue ) + "% average resistance or " + goodeffect.getName();
		}

		if ( basementTestString.startsWith( "Monster" ) )
		{
			int index = basementTestString.indexOf( ": " );
			if ( index == -1 )
				return "";
			return "<u>Monster</u>: " + basementTestString.substring( index + 2 );
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
			element1 = MonsterDatabase.SPOOKY;
			element2 = MonsterDatabase.COLD;

			goodelement = element1;
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

		float damage1 = (((float) Math.pow( basementLevel, 1.4 )) * 4.48f + 8.0f) * 1.05f;
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

		if ( activeEffects.contains( badeffect1 ) ||
		     activeEffects.contains( badeffect2 ) ||
		     activeEffects.contains( badeffect3 ) )
		{
			if ( element1 == badelement1 || element1 == badelement2 || element1 == badelement3 )
			{
				damage1 *= 2;
				resistance1 /= 2;
			}
			else
			{
				damage2 *= 2;
				resistance2 /= 2;
			}
		}

		expected1 = Math.max( 1.0f, damage1 * ( (100.0f - resistance1) / 100.0f ) );
		expected2 = Math.max( 1.0f, damage2 * ( (100.0f - resistance2) / 100.0f ) );

		// If you can survive the current elemental test even without a phial,
		// then don't bother with any extra buffing.

		basementTestString = "Elemental Resist";
		basementTestCurrent = Math.min( resistance1, resistance2 );
		basementTestValue = Math.max( 0, (int) Math.ceil( 100.0f * (1.0f - KoLCharacter.getMaximumHP() / (damage1 + damage2) )) );

		if ( expected1 + expected2 < KoLCharacter.getCurrentHP() )
			return true;

		if ( expected1 + expected2 < KoLCharacter.getMaximumHP() )
		{
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

	private static final boolean checkForStatTest( boolean autoSwitch, String responseText )
	{
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// stat requirement is x^1.4 + 2.  Assume the worst-case.

		float statRequirement = ((float) Math.pow( basementLevel, 1.4 ) + 2.0f) * 1.05f;

		if ( responseText.indexOf( "Lift 'em" ) != -1 || responseText.indexOf( "Push it Real Good" ) != -1 || responseText.indexOf( "Ring that Bell" ) != -1 )
		{
			basementTestString = "Buffed Muscle";
			basementTestCurrent = KoLCharacter.getAdjustedMuscle();
			basementTestValue = (int) statRequirement;

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
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// drain requirement is 1.67 * x^1.4  Assume the worst-case.

		float drainRequirement = (float) Math.pow( basementLevel, 1.4 ) * 1.67f * 1.05f;

		if ( responseText.indexOf( "Grab the Handles" ) != -1 )
		{
			basementTestString = "Maximum MP";
			basementTestCurrent = KoLCharacter.getMaximumMP();
			basementTestValue = (int) drainRequirement;

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

			StaticEntity.getClient().recoverMP( (int) drainRequirement );
			return true;
		}

		if ( responseText.indexOf( "Run the Gauntlet Gauntlet" ) != -1 )
		{
			basementTestString = "Maximum HP";
			basementTestCurrent = KoLCharacter.getMaximumHP();
			basementTestValue = (int) drainRequirement;

			float damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
			float healthRequirement = drainRequirement * damageAbsorb;

			if ( KoLCharacter.getMaximumHP() < healthRequirement )
			{
				if ( autoSwitch )
					changeBasementOutfit( "gauntlet" );

				damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
				healthRequirement = drainRequirement * damageAbsorb;

				if ( KoLCharacter.getMaximumHP() < healthRequirement )
				{
					basementErrorMessage = "Insufficient health to continue.";
					return true;
				}
			}

			StaticEntity.getClient().recoverHP( (int) healthRequirement );
			return true;
		}

		return false;
	}

	private static final boolean checkForStatReward( String responseText )
	{
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

	private static final boolean checkForMonster( String responseText )
	{
		if ( responseText.indexOf( "Don't Fear the Ear" ) != -1 )
		{
			// The Beast with n Ears
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "Commence to Pokin" ) != -1 )
		{
			// The Beast with n Eyes
			basementTestString = "Monster";
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
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "Toast the Ghost" ) != -1 )
		{
			// The Ghost of Fernswarthy's n great-grandfather
			basementTestString = "Monster: physically resistant";
			return true;
		}

		if ( responseText.indexOf( "Bottles of Beer on a Golem" ) != -1 )
		{
			// N Bottles of Beer on a Golem
			basementTestString = "Monster: blocks most spells";
			return true;
		}

		if ( responseText.indexOf( "Collapse That Waveform" ) != -1 )
		{
			// A n-Dimensional Horror
			basementTestString = "Monster: blocks physical attacks";
			return true;
		}

		return false;
	}

	public static final void newBasementLevel( String responseText )
	{
		basementErrorMessage = null;
		basementTestString = "None";
		basementTestValue = 0;

		element1 = -1; element2 = -1;

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

	public static final boolean checkBasement( boolean autoSwitch, String responseText )
	{
		newBasementLevel( responseText );

		if ( checkForStatReward( responseText ) )
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

		if ( autoSwitch )
			changeBasementOutfit( "damage" );

		return true;
	}

	private void handleBasement()
	{
		checkBasement( true, responseText );

		if ( this.responseText.indexOf( "Got Silk?" ) != -1 )
		{
			this.addFormField( "action", KoLCharacter.isMoxieClass() ? "1" : "2" );
		}
		else if ( this.responseText.indexOf( "Save the Dolls" ) != -1 )
		{
			this.addFormField( "action", KoLCharacter.isMysticalityClass() ? "1" : "2" );
		}
		else if ( this.responseText.indexOf( "Take the Red Pill" ) != -1 )
		{
			this.addFormField( "action", KoLCharacter.isMuscleClass() ? "1" : "2" );
		}
		else
		{
			this.addFormField( "action", "1" );
		}

		if ( basementErrorMessage != null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, basementErrorMessage );
			return;
		}

		super.run();

		if ( this.responseCode != 200 )
		{
			if ( FightRequest.INSTANCE.responseCode == 200 && FightRequest.INSTANCE.responseText.indexOf( "<!--WINWINWIN-->" ) != -1 )
				return;

			this.data.clear();
			super.run();
		}

		Matcher levelMatcher = BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() || basementLevel == StaticEntity.parseInt( levelMatcher.group(1) ) )
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to pass basement test." );
	}
}
