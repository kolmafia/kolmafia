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

public class AdventureRequest extends KoLRequest
{
	private String adventureName;
	private String formSource;
	private String adventureId;

	private static int basementLevel = 0;
	private static float basementTestValue = 0;
	private static float basementTestCurrent = 0;
	private static String basementTestString = "";

	private static int element1 = -1, element2 = -1;
	private static int goodelement = -1;
	private static AdventureResult goodphial = null;
	private static AdventureResult goodeffect = null;
	private static int badelement1 = -1, badelement2 = -1, badelement3 = -1;
	private static AdventureResult badphial1 = null, badphial2 = null, badphial3 = null;
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

	private static final AdventureResult SKELETON_KEY = new AdventureResult( 642, 1 );

	public static final AdventureResult ABRIDGED = new AdventureResult( 534, -1 );
	public static final AdventureResult BRIDGE = new AdventureResult( 535, -1 );
	public static final AdventureResult DODECAGRAM = new AdventureResult( 479, -1 );
	public static final AdventureResult CANDLES = new AdventureResult( 480, -1 );
	public static final AdventureResult BUTTERKNIFE = new AdventureResult( 481, -1 );


	/**
	 * Constructs a new <code>AdventureRequest</code> which executes the
	 * adventure designated by the given Id by posting to the provided form,
	 * notifying the givenof results (or errors).
	 *
	 * @param	adventureName	The name of the adventure location
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureId	The identifer for the adventure to be executed
	 */

	public AdventureRequest( String adventureName, String formSource, String adventureId )
	{
		super( formSource );
		this.adventureName = adventureName;
		this.formSource = formSource;
		this.adventureId = adventureId;

		// The adventure Id is all you need to identify the adventure;
		// posting it in the form sent to adventure.php will handle
		// everything for you.

		if ( formSource.equals( "adventure.php" ) )
		{
			this.addFormField( "snarfblat", adventureId );
		}
		else if ( formSource.equals( "shore.php" ) )
		{
			this.addFormField( "whichtrip", adventureId );
			this.addFormField( "pwd" );
		}
		else if ( formSource.equals( "casino.php" ) )
		{
			this.addFormField( "action", "slot" );
			this.addFormField( "whichslot", adventureId );
		}
		else if ( formSource.equals( "knob.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "king", "Yep." );
		}
		else if ( formSource.equals( "mountains.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "orcs", "1" );
		}
		else if ( formSource.equals( "friars.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "action", "ritual" );
		}
		else if ( formSource.equals( "lair6.php" ) )
		{
			this.addFormField( "place", adventureId );
		}
		else if ( !formSource.equals( "dungeon.php" ) && !formSource.equals( "basement.php" ) && !formSource.equals( "rats.php" ) )
		{
			this.addFormField( "action", adventureId );
		}
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void run()
	{
		// Prevent the request from happening if they attempted
		// to cancel in the delay period.

		if ( !KoLmafia.permitsContinue() )
			return;

		if ( this.formSource.equals( "mountains.php" ) )
		{
			VISITOR.constructURLString( "mountains.php" ).run();
			if ( VISITOR.responseText.indexOf( "value=80" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "The Orc Chasm has already been bridged." );
				return;
			}
		}

		if ( this.formSource.equals( "shore.php" ) )
		{
			if ( KoLCharacter.getAdventuresLeft() < 2 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				return;
			}
		}

		delay();

		if ( this.formSource.equals( "dungeon.php" ) || this.formSource.equals( "basement.php" ) )
			this.data.clear();
		else if ( this.formSource.equals( "adventure.php" ) && this.adventureId.equals( "120" ) )
			addFormField( "subsnarfblat", String.valueOf( RNG.nextInt( 4 ) + 1 ) );

		super.run();

		if ( this.formSource.equals( "dungeon.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "action", "Yep." );

			if ( this.responseText.indexOf( "Locked Door" ) != -1 && SKELETON_KEY.getCount( inventory ) + SKELETON_KEY.getCount( closet ) > 1 )
			{
				StaticEntity.getClient().processResult( SKELETON_KEY.getInstance( -1 ) );
				this.addFormField( "option", "2" );
			}
			else
				this.addFormField( "option", "1" );

			super.run();
		}
		else if ( this.formSource.equals( "basement.php" ) )
		{
			handleBasement();
		}
		else
		{
			useMarmotClover( this.formSource, this.responseText );
		}

		if ( !containsUpdate && !RequestFrame.sidebarFrameExists() )
			CharpaneRequest.getInstance().run();
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
			badphial1 = STENCH_PHIAL;
			badeffect1 = STENCH_FORM;

			// Spooky is vulnerable to Stench
			badelement2 = MonsterDatabase.SPOOKY;
			badphial2 = SPOOKY_PHIAL;
			badeffect2 = SPOOKY_FORM;

			// Hot is vulnerable to Sleaze and Stench
			badelement3 = MonsterDatabase.HEAT;
			badphial3 = HOT_PHIAL;
			badeffect2 = HOT_FORM;
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
			badphial1 = SLEAZE_PHIAL;
			badeffect1 = SLEAZE_FORM;

			// Stench is vulnerable to Cold
			badelement2 = MonsterDatabase.STENCH;
			badphial2 = STENCH_PHIAL;
			badeffect2 = STENCH_FORM;

			// Hot is vulnerable to Sleaze
			badelement3 = MonsterDatabase.HEAT;
			badphial3 = HOT_PHIAL;
			badeffect2 = HOT_FORM;
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
			badphial1 = COLD_PHIAL;
			badeffect1 = COLD_FORM;

			// Spooky is vulnerable to Hot
			badelement2 = MonsterDatabase.SPOOKY;
			badphial2 = SPOOKY_PHIAL;
			badeffect2 = SPOOKY_FORM;

			// Hot is vulnerable to Stench
			badelement3 = MonsterDatabase.HEAT;
			badphial3 = HOT_PHIAL;
			badeffect2 = HOT_FORM;
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
			badphial1 = COLD_PHIAL;
			badeffect1 = COLD_FORM;

			// Spooky is vulnerable to Hot
			badelement2 = MonsterDatabase.SPOOKY;
			badphial2 = SPOOKY_PHIAL;
			badeffect2 = SPOOKY_FORM;

			// Sleaze is vulnerable to Spooky
			badelement3 = MonsterDatabase.SLEAZE;
			badphial3 = SLEAZE_PHIAL;
			badeffect2 = SLEAZE_FORM;
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
			badphial1 = COLD_PHIAL;
			badeffect1 = COLD_FORM;

			// Stench is vulnerable to Cold
			badelement2 = MonsterDatabase.STENCH;
			badphial2 = STENCH_PHIAL;
			badeffect2 = STENCH_FORM;

			// Sleaze is vulnerable to Cold
			badelement3 = MonsterDatabase.SLEAZE;
			badphial3 = SLEAZE_PHIAL;
			badeffect2 = SLEAZE_FORM;
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
		if ( responseText.indexOf( "The Beast with" ) != -1 )
		{
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "Stone Golem" ) != -1 )
		{
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "Hydra" ) != -1 )
		{
			basementTestString = "Monster";
			return true;
		}

		if ( responseText.indexOf( "The Ghost of Fernswarthy" ) != -1 )
		{
			basementTestString = "Monster: physically resistant";
			return true;
		}

		if ( responseText.indexOf( "Bottles of Beer on a Golem" ) != -1 )
		{
			basementTestString = "Monster: blocks most spells";
			return true;
		}

		if ( responseText.indexOf( "Dimensional Horror" ) != -1 )
		{
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
                goodelement = badelement1 = badelement2 = badelement3 = -1;
		goodphial = badphial1 = badphial2 = badphial3 = null;
		goodeffect = badeffect1 = badeffect2 = badeffect3 = null;

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

	public void processResults()
	{
		// Sometimes, there's no response from the server.
		// In this case, skip and continue onto the next one.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		// The hedge maze should always result in you getting
		// a fight redirect.  If this is not the case, then
		// if the hedge maze is not complete, use up all their
		// pieces first, then go adventuring.

		if ( this.formSource.equals( "lair3.php" ) )
		{
			if ( KoLCharacter.hasItem( SorceressLair.HEDGE_KEY ) && KoLCharacter.hasItem( SorceressLair.PUZZLE_PIECE ) )
				KoLmafia.updateDisplay( PENDING_STATE, "Unexpected hedge maze puzzle state." );

			return;
		}

		if ( this.formSource.equals( "dungeon.php" ) && this.responseText.indexOf( "You have reached the bottom of today's Dungeon" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Daily dungeon completed." );
			return;
		}

		// The sorceress fight should always result in you getting
		// a fight redirect.

		if ( this.formSource.equals( "lair6.php" ) )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "The sorceress has already been defeated." );
			return;
		}

		if ( this.formSource.equals( "cyrpt.php" ) )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You shouldn't be here." );
			return;
		}

		// If you haven't unlocked the orc chasm yet,
		// try doing so now.

		if ( this.adventureId.equals( "80" ) && this.responseText.indexOf( "You shouldn't be here." ) != -1 )
		{
			AdventureRequest bridge = new AdventureRequest( "Bridge the Orc Chasm", "mountains.php", "" );
			bridge.run();

			if ( KoLmafia.permitsContinue() )
				this.run();

			return;
		}

		// We're missing an item, haven't been given a quest yet, or otherwise
		// trying to go somewhere not allowed.

		if ( this.responseText.indexOf( "You shouldn't be here" ) != -1 || this.responseText.indexOf( "not yet be accessible" ) != -1 || this.responseText.indexOf( "You can't get there" ) != -1 || this.responseText.indexOf( "Seriously.  It's locked." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		if ( this.responseText.indexOf( "in the regular dimension now" ) != -1 )
		{
			// "You're in the regular dimension now, and don't
			// remember how to get back there."
			KoLmafia.updateDisplay( PENDING_STATE, "You are no longer Half-Astral." );
			return;
		}

		if ( this.responseText.indexOf( "into the spectral mists" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "No one may know of its coming or going." );
			return;
		}

		if ( this.responseText.indexOf( "temporal rift in the plains has closed" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "The temporal rift has closed." );
			return;
		}

		// Cold protection is required for the area.  This only happens at
		// the peak.  Abort and notify.

		if ( this.responseText.indexOf( "need some sort of protection" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You need cold protection." );
			return;
		}

		// Stench protection is required for the area.	This only
		// happens at the Guano Junction.  Abort and notify.

		if ( this.responseText.indexOf( "need stench protection to adventure here" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You need stench protection." );
			return;
		}

		// This is a server error. Hope for the
		// best and repeat the request.

		if ( this.responseText.indexOf( "No adventure data exists for this location" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Server error.  Please wait and try again." );
			return;
		}

		if ( this.responseText.indexOf( "You must have at least" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Your stats are too low for this location." );
			return;
		}

		// Cobb's Knob King's Chamber: if you've already
		// defeated the goblin king, go into pending state.

		if ( this.formSource.equals( "knob.php" ) && this.responseText.indexOf( "You've already slain the Goblin King" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You already defeated the Goblin King." );
			return;
		}

		// The Haert of the Cyrpt: if you've already defeated
		// the bonerdagon, go into pending state.

		if ( this.formSource.equals( "cyrpt.php" ) && this.responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You already defeated the Bonerdagon." );
			return;
		}

		if ( this.responseText.indexOf( "already undefiled" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Cyrpt area cleared." );
			return;
		}

		// Nothing more to do in this area

		if ( this.formSource.equals( "adventure.php" ) && this.responseText.indexOf( "adventure.php" ) == -1 && this.responseText.indexOf( "You acquire" ) == -1 )
		{
			if ( !KoLmafia.isAutoStop( this.encounter ) )
				KoLmafia.updateDisplay( PENDING_STATE, "Nothing more to do here." );

			return;
		}

		// The Orc Chasm (pre-bridge)

		if ( this.formSource.equals( "mountains.php" ) )
		{
			// If there's no link to the valley beyond, put down a
			// brIDGE

			if ( this.responseText.indexOf( "value=80" ) == -1 )
			{
				// If you have an unabridged dictionary in your
				// inventory, visit the untinkerer
				// automatically and repeat the request.

				if ( KoLCharacter.hasItem( ABRIDGED ) )
				{
					(new UntinkerRequest( ABRIDGED.getItemId() )).run();
					this.run();
					return;
				}

				// Otherwise, the player is unable to cross the
				// orc chasm at this time.

				KoLmafia.updateDisplay( ERROR_STATE, "You can't cross the Orc Chasm." );
				return;
			}

			if ( this.responseText.indexOf( "the path to the Valley is clear" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You have bridged the Orc Chasm." );
				StaticEntity.getClient().processResult( BRIDGE );
			}

			return;
		}

		// If you're at the casino, each of the different slot
		// machines deducts meat from your tally

		if ( this.formSource.equals( "casino.php" ) )
		{
			if ( this.adventureId.equals( "1" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			else if ( this.adventureId.equals( "2" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( this.adventureId.equals( "11" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		}

		if ( this.adventureId.equals( "70" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		else if ( this.adventureId.equals( "71" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );

		// Shore Trips cost 500 meat each; handle
		// the processing here.

		if ( this.formSource.equals( "shore.php" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );

		// Trick-or-treating requires a costume;
		// notify the user of this error.

		if ( this.formSource.equals( "trickortreat.php" ) && this.responseText.indexOf( "without a costume" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You must wear a costume." );
			return;
		}
	}

	public static final String registerEncounter( KoLRequest request )
	{
		String urlString = request.getURLString();
		if ( !(request instanceof AdventureRequest) && !containsEncounter( urlString, request.responseText ) )
			return "";

		if ( urlString.indexOf( "fight.php" ) != -1 )
		{
			int spanIndex = request.responseText.indexOf( "<span id='monname" ) + 1;
			spanIndex = request.responseText.indexOf( ">", spanIndex ) + 1;

			if ( spanIndex == 0 )
				return "";

			int endSpanIndex = request.responseText.indexOf( "</span>", spanIndex );
			if ( endSpanIndex == -1 )
				return "";

			String encounter = request.responseText.substring( spanIndex, endSpanIndex );
			encounter = CombatSettings.encounterKey( encounter, false );

			RequestLogger.printLine( "Encounter: " + encounter );
			RequestLogger.updateSessionLog( "Encounter: " + encounter );
			StaticEntity.getClient().registerEncounter( encounter, "Combat" );

			return encounter;
		}

		if ( urlString.startsWith( "dungeon.php" ) || urlString.startsWith( "basement.php" ) )
		{
			return "";
		}

		int boldIndex = request.responseText.indexOf( "Results:</b>" ) + 1;
		boldIndex = request.responseText.indexOf( "<b>", boldIndex ) + 3;

		if ( boldIndex == 2 )
			return "";

		int endBoldIndex = request.responseText.indexOf( "</b>", boldIndex );

		if ( endBoldIndex == -1 )
			return "";

		String encounter = request.responseText.substring( boldIndex, endBoldIndex );
		if ( encounter.equals( "" ) )
			return "";

		RequestLogger.printLine( "Encounter: " + encounter );
		RequestLogger.updateSessionLog( "Encounter: " + encounter );

		registerDemonName( encounter, request.responseText );

		if ( !urlString.startsWith( "choice.php" ) || urlString.indexOf( "option" ) == -1 )
			StaticEntity.getClient().registerEncounter( encounter, "Noncombat" );
		else
			StaticEntity.getClient().recognizeEncounter( encounter );

		return encounter;
	}

	private static final Object [][] demons = {
		{ "Summoning Chamber",
		  Pattern.compile( "Did you say your name was (.*?)\\?" ),
		},
		{ "Hoom Hah",
		  Pattern.compile( "(.*?)! \\1, cooooome to meeeee!" )
		},
		{ "Every Seashell Has a Story to Tell If You're Listening",
		  Pattern.compile( "Hello\\? Is (.*?) there\\?" )
		},
		{ "Leavesdropping",
		  Pattern.compile( "(.*?), we call you! \\1, come to us!" )
		},
		{ "These Pipes... Aren't Clean!",
		  Pattern.compile( "Blurgle. (.*?). Gurgle. By the way," )
		},
	};

	public static final void registerDemonName( String encounter, String responseText )
	{
		for ( int i = 0; i < demons.length; ++i )
		{
			String name = (String)demons[i][0];
			if ( name == null || !name.equals( encounter ) )
				continue;

			Pattern pattern = (Pattern)demons[i][1];
			Matcher matcher = pattern.matcher( responseText );
			if ( !matcher.find() )
				return;

			String demon = matcher.group(1);
			RequestLogger.printLine( "Demon name: " + demon );
			RequestLogger.updateSessionLog( "Demon name: " + demon );
			StaticEntity.setProperty( "demonName" + (i + 1), demon );
			return;
		}
	}

	private static final boolean containsEncounter( String formSource, String responseText )
	{
		// The first round is unique in that there is no
		// data fields.  Therefore, it will equal fight.php
		// exactly every single time.

		if ( formSource.startsWith( "fight.php" ) )
			return FightRequest.getCurrentRound() == 0;

		// All other adventures can be identified via their
		// form data and the place they point to.

		else if ( formSource.startsWith( "adventure.php" ) )
			return true;
		else if ( formSource.startsWith( "cave.php" ) )
			return formSource.indexOf( "end" ) != -1;
		else if ( formSource.startsWith( "shore.php" ) )
			return formSource.indexOf( "whichtrip" ) != -1;
		else if ( formSource.startsWith( "knob.php" ) )
			return formSource.indexOf( "king" ) != -1;
		else if ( formSource.startsWith( "cyrpt.php" ) )
			return formSource.indexOf( "action" ) != -1;
		else if ( formSource.startsWith( "rats.php" ) )
			return true;
		else if ( formSource.startsWith( "choice.php" ) )
			return responseText.indexOf( "choice.php" ) != -1;
		else if ( formSource.startsWith( "palinshelves.php" ) )
			return responseText.indexOf( "palinshelves.php" ) != -1;

		// It is not a known adventure.  Therefore,
		// do not log the encounter yet.

		return false;
	}

	public int getAdventuresUsed()
	{	return 1;
	}

	public String toString()
	{	return this.adventureName;
	}

	public static final void handleServerRedirect( String redirectLocation )
	{
		if ( redirectLocation.indexOf( "main.php" ) != -1 )
			return;

		VISITOR.constructURLString( redirectLocation );

		if ( redirectLocation.indexOf( "palinshelves.php" ) != -1 )
		{
			VISITOR.run();
			VISITOR.constructURLString( "palinshelves.php?action=placeitems&whichitem1=2259&whichitem2=2260&whichitem3=493&whichitem4=2261" ).run();
			return;
		}

		if ( redirectLocation.indexOf( "tiles.php" ) != -1 )
		{
			handleDvoraksRevenge( VISITOR );
			return;
		}

		FightFrame.showRequest( VISITOR );
		KoLmafia.updateDisplay( ABORT_STATE, "Unknown adventure type encountered." );
	}

	public static final void handleDvoraksRevenge( KoLRequest request )
	{
		StaticEntity.getClient().registerEncounter( "Dvorak's Revenge", "Noncombat" );
		RequestLogger.printLine( "Encounter: Dvorak's Revenge" );
		RequestLogger.updateSessionLog( "Encounter: Dvorak's Revenge" );

		request.run();
		request.constructURLString( "tiles.php?action=jump&whichtile=4" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=6" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=3" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=5" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=7" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=6" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=3" ).run();
	}

	public static final boolean useMarmotClover( String location, String responseText )
	{
		if ( location.startsWith( "barrels.php" ) || location.startsWith( "shore.php" ) )
		{
			if ( !StaticEntity.getBooleanProperty( "cloverProtectActive" ) )
				return false;

			if ( responseText.indexOf( "ten-leaf clover" ) == -1 )
				return false;

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );
			return true;
		}

		if ( !location.startsWith( "adventure.php" ) )
			return false;

		if ( !StaticEntity.getBooleanProperty( "cloverProtectActive" ) )
			return false;

		if ( responseText.indexOf( "notice a ten-leaf clover" ) == -1 || responseText.indexOf( "puff of smoke" ) != -1 )
			return false;

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );
		return true;
	}
}
