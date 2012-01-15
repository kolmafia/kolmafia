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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.FriarRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FriarBlessingCommand
	extends AbstractCommand
{
	public FriarBlessingCommand()
	{
		this.usage = " [blessing] food | familiar | booze - get daily blessing.";
	}

	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = null;

		if ( split.length == 2 && split[ 0 ].equals( "blessing" ) )
		{
			command = split[ 1 ];
		}
		else if ( split.length == 1 && !split[ 0 ].equals( "" ) )
		{
			command = split[ 0 ];
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
			return;
		}

		int action = 0;

		if ( Character.isDigit( command.charAt( 0 ) ) )
		{
			action = StringUtilities.parseInt( command );
		}
		else
		{
			for ( int i = 0; i < FriarRequest.BLESSINGS.length; ++i )
			{
				if ( command.equalsIgnoreCase( FriarRequest.BLESSINGS[ i ] ) )
				{
					action = i + 1;
					break;
				}
			}
		}

		if ( action < 1 || action > 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: friars [blessing] food|familiar|booze" );
			return;
		}

		RequestThread.postRequest( new FriarRequest( action ) );
	}
}
