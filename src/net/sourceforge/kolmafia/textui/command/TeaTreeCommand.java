/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.PottedTeaTreeRequest;
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest.PottedTea;

public class TeaTreeCommand
	extends AbstractCommand
{

	public TeaTreeCommand()
	{
		this.usage = " shake | [tea name] - Harvest random or specific tea";
	}

	@Override
	public void run( final String cmd, String parameter )
	{
		PottedTea tea = null;
		parameter = parameter.trim();

		if ( parameter.equals( "" ) )
		{
			KoLmafia.updateDisplay( "Harvest what?" );
			return;
		}

		if ( parameter.startsWith( "random" ) || parameter.startsWith( "shake" ) )
		{
			tea = null;
		}
		else
		{
			List<String> matchingNames = PottedTeaTreeRequest.getMatchingNames( parameter );
			if ( matchingNames.isEmpty() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to harvest " + parameter );
				return;
			}

			if ( matchingNames.size() > 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + parameter + "' is an ambiguous tea name " );
				return;
			}

			String name = matchingNames.get( 0 );

			tea = PottedTeaTreeRequest.canonicalNameToTea( name );
			if ( tea == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to harvest " + parameter );
				return;
			}
		}

		PottedTeaTreeRequest request = tea == null ? new PottedTeaTreeRequest() : new PottedTeaTreeRequest( tea );

		RequestThread.postRequest( request );
	}
}
