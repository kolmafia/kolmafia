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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.TCRSDatabase;

public class TCRSCommand
	extends AbstractCommand
{
	public TCRSCommand()
	{
		this.usage = " load | save | derive | apply - handle item modifiers for Two Crazy Random Summerlist or manipulate your closet.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String[] split = parameters.split( " +" );
		String command = split[ 0 ];

		if ( !KoLCharacter.isCrazyRandomTwo() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You are not in a Two Crazy Random Summer run" );
			return;
		}

		String file = TCRSDatabase.filename();

		if ( command.equals( "load" ) )
		{
			if ( !TCRSDatabase.load() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "File " + file + " does not exist" );
			}
			else
			{
				KoLmafia.updateDisplay( "Read file " + file );
			}
			return;
		}

		if ( command.equals( "save" ) )
		{
			if ( !TCRSDatabase.save() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Could not save file " + file );
			}
			else
			{
				KoLmafia.updateDisplay( "Wrote file " + file );
			}
			return;
		}

		if ( command.equals( "derive" ) )
		{
			TCRSDatabase.derive( true );
			return;
		}

		if ( command.equals( "apply" ) )
		{
			return;
		}

		if ( command.equals( "reset" ) )
		{
			return;
		}
	}
}
