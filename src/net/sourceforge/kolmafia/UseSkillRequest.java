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

public class UseSkillRequest extends KoLRequest
{
	private int consumedMP;
	private String target;
	private String skillName;

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
		addFormField( "pwd", client.getPasswordHash() );

		this.skillName = skillName;
		int skillID = ClassSkillsDatabase.getSkillID( skillName.replaceFirst( "ñ", "&ntilde;" ) );
		addFormField( "whichskill", "" + skillID );

		if ( ClassSkillsDatabase.isBuff( skillID ) )
		{
			addFormField( "bufftimes", "" + buffCount );

			if ( target == null || target.trim().length() == 0 )
			{
				if ( client.getCharacterData().getUserID() != 0 )
					addFormField( "targetplayer", "" + client.getCharacterData().getUserID() );
				else
					addFormField( "specificplayer", client.getLoginName() );
			}
			else
				addFormField( "specificplayer", target );
		}
		else
			addFormField( "quantity", "" + buffCount );

		this.target = target;
		this.consumedMP = ClassSkillsDatabase.getMPConsumptionByID( skillID ) * buffCount;
	}

	public void run()
	{
		if ( target == null || target.trim().length() == 0 )
			updateDisplay( DISABLED_STATE, "Casting " + skillName + "..." );
		else
			updateDisplay( DISABLED_STATE, "Casting " + skillName + " on " + target );

		super.run();

		// If it does not notify you that you didn't have enough mana points,
		// then the skill was successfully used.

		if ( replyContent == null || replyContent.indexOf( "You don't have enough" ) != -1 )
		{
			updateDisplay( ERROR_STATE, "You don't have enough mana." );
			return;
		}
		else if ( replyContent.indexOf( "You can only conjure" ) != -1 )
		{
			updateDisplay( ERROR_STATE, "Summon limited exceeded." );
			return;
		}
		else if ( replyContent.indexOf( "Invalid target" ) != -1 )
		{
			updateDisplay( ERROR_STATE, "Invalid target: " + target );
			return;
		}

		client.processResult( new AdventureResult( AdventureResult.MP, 0 - consumedMP ) );

		processResults( replyContent.replaceFirst(
			"</b><br>\\(duration: ", " (" ).replaceFirst( " Adventures", "" ) );

 		client.applyRecentEffects();
		updateDisplay( ENABLED_STATE, skillName + " was successfully cast." );
	}
}