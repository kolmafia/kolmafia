/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;

public class UseSkillRequest extends KoLRequest implements Comparable
{
	private static final int WALRUS_TONGUE = 1010;
	protected static String lastUpdate = "";

	private int skillID;
	private String skillName;
	private String target;
	private int buffCount;

	/**
	 * Constructs a new <code>UseSkillRequest</code>.
	 * @param	client	The client to be notified of completion
	 * @param	skillName	The name of the skill to be used
	 * @param	target	The name of the target of the skill
	 * @param	buffCount	The number of times the target is affected by this skill
	 */

	public UseSkillRequest( KoLmafia client, String skillName, String target, int buffCount )
	{
		super( client, "skills.php" );
		addFormField( "action", "Skillz." );
		addFormField( "pwd" );

		this.skillID = ClassSkillsDatabase.getSkillID( skillName );
		this.skillName = ClassSkillsDatabase.getSkillName( skillID );
		addFormField( "whichskill", String.valueOf( skillID ) );

		if ( ClassSkillsDatabase.isBuff( skillID ) )
		{
			this.target = target;
			addFormField( "bufftimes", "" + buffCount );

			if ( target == null || target.trim().length() == 0 || target.equals( String.valueOf( KoLCharacter.getUserID() ) ) || target.equals( KoLCharacter.getUsername() ) )
			{
				this.target = "yourself";
				if ( KoLCharacter.getUserID() != 0 )
					addFormField( "targetplayer", String.valueOf( KoLCharacter.getUserID() ) );
				else
					addFormField( "specificplayer", KoLCharacter.getUsername() );
			}
			else
				addFormField( "specificplayer", target );
		}
		else
		{
			addFormField( "quantity", "" + buffCount );
			this.target = null;
		}

		this.buffCount = buffCount < 1 ? 1 : buffCount;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof UseSkillRequest) )
			return -1;

		int mpDifference = ClassSkillsDatabase.getMPConsumptionByID( skillID ) -
			ClassSkillsDatabase.getMPConsumptionByID( ((UseSkillRequest)o).skillID );

		return mpDifference != 0 ? mpDifference : skillName.compareToIgnoreCase( ((UseSkillRequest)o).skillName );
	}

	public String getSkillName()
	{	return skillName;
	}

	public String toString()
	{	return skillName + " (" + ClassSkillsDatabase.getMPConsumptionByID( skillID ) + " mp)";
	}

	public void run()
	{
		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		client.recoverMP( ClassSkillsDatabase.getMPConsumptionByID( skillID ) * buffCount );

		// If a continue is not permitted, then return from the attempt;
		// you don't have enough mana for the cast.

		if ( !client.permitsContinue() )
			return;

		if ( target == null || target.trim().length() == 0 )
			DEFAULT_SHELL.updateDisplay( "Casting " + skillName + "..." );
		else
			DEFAULT_SHELL.updateDisplay( "Casting " + skillName + " on " + target + "..." );

		super.run();

		// To minimize the amount of confusion, go ahead and restore mana
		// once the request is complete.

		client.recoverMP();
	}

	protected void processResults()
	{
		boolean encounteredError = false;

		// If a reply was obtained, check to see if it was a success message
		// Otherwise, try to figure out why it was unsuccessful.

		if ( responseText == null || responseText.trim().length() == 0 )
		{
			encounteredError = true;
			lastUpdate = "Encountered lag problems.";
		}
		else if ( responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "That skill is unavailable.";
		}
		else if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Not enough mana to continue.";
		}
		else if ( responseText.indexOf( "You can only conjure" ) != -1 ||
			  responseText.indexOf( "You can only scrounge up" ) != -1 ||
			  responseText.indexOf( "You can only summon" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "Summon limit exceeded.";
		}
		else if ( responseText.indexOf( "too many songs" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = target + " has 3 AT buffs already.";
		}
		else if ( responseText.indexOf( "casts left of the Smile of Mr. A" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = "You cannot cast that many smiles.";
		}
		else if ( responseText.indexOf( "Invalid target player" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = target + " is not a valid target.";
		}
		else if ( responseText.indexOf( "busy fighting" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = target + " is busy fighting.";
		}
		else if ( responseText.indexOf( "cannot currently" ) != -1 )
		{
			encounteredError = true;
			lastUpdate = target + " cannot receive buffs.";
		}
		else if ( responseText.indexOf( "accordion equipped" ) != -1 )
		{
			// "You need to have an accordion equipped or in your
			// inventory if you want to play that song."

			encounteredError = true;
			lastUpdate = "You need an accordion to play Accordion Thief songs.";
		}
		else
		{
			lastUpdate = "";
		}

		// Now that all the checks are complete, proceed
		// to determine how to update the user display.

		if ( encounteredError )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, lastUpdate );
		}
		else
		{
			if ( target == null || target.equals( "" ) )
				DEFAULT_SHELL.updateDisplay( skillName + " was successfully cast." );
			else
				DEFAULT_SHELL.updateDisplay( skillName + " was successfully cast on " + target + "." );

			// Tongue of the Walrus (1010) automatically
			// removes any beaten up.

			client.processResult( new AdventureResult( AdventureResult.MP, 0 - (ClassSkillsDatabase.getMPConsumptionByID( skillID ) * buffCount) ) );
			client.applyRecentEffects();

			if ( skillID == WALRUS_TONGUE )
			{
				int roundsBeatenUp = KoLAdventure.BEATEN_UP.getCount( KoLCharacter.getEffects() );
				if ( roundsBeatenUp != 0 )
					client.processResult( KoLAdventure.BEATEN_UP.getInstance( 0 - roundsBeatenUp ) );
			}

			super.processResults();
		}
	}

	public String getCommandForm( int iterations )
	{	return "cast " + buffCount + " " + skillName;
	}
}
