/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoAttackCommand
	extends AbstractCommand
{
	private static final GenericRequest AUTO_ATTACKER = new GenericRequest( "account.php?action=autoattack&ajax=1&pwd" );

	public AutoAttackCommand()
	{
		this.usage = " <skill> - set default attack method.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();

		if ( setAutoAttackSkill( parameters.toLowerCase() ) )
		{
			return;
		}

		if ( parameters.startsWith( "/" ) )
		{
			return;
		}

		if ( !ChatManager.chatLiterate() )
		{
			KoLmafia.updateDisplay( "Chat commands are not available for this user." );
			return;
		}

		ChatSender.executeMacro( "/aa " + parameters );
	}

	protected boolean setAutoAttackSkill( String attackName )
	{
		int skillId = -1;
		
		// Check to see if it's a known skill / attack
		
		if ( attackName.equals( "none" ) || attackName.indexOf( "disable" ) != -1 )
		{
			skillId = 0;
		}
		else if ( attackName.equals( "attack" ) || attackName.startsWith( "attack " ) )
		{
			skillId = 1;
		}
		else if ( !Character.isDigit( attackName.charAt( 0 ) ) )
		{
			List combatSkills = SkillDatabase.getSkillsByType( SkillDatabase.COMBAT );
			String skillName = SkillDatabase.getSkillName( attackName, combatSkills );

			if ( skillName != null )
			{
				skillId = SkillDatabase.getSkillId( skillName );
			}
		}
		else
		{
			skillId = StringUtilities.parseInt( attackName );
		}

		// If it's not something that KoLmafia recognizes, fall through to KoL chat's implementation

		if (   skillId == -1 || 
			   ( skillId > 1 && 
			     skillId < 7000 && 
			     !KoLCharacter.hasSkill( skillId ) ) )
		{
			return false;
		}

		if ( skillId != KoLCharacter.getAutoAttackAction() )
		{
			AutoAttackCommand.AUTO_ATTACKER.addFormField( "value", String.valueOf( skillId ) );
			RequestThread.postRequest( AutoAttackCommand.AUTO_ATTACKER );
		}
		
		return true;
	}
}
