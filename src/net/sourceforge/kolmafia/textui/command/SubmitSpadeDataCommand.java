/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.SendMailRequest;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SubmitSpadeDataCommand
	extends AbstractCommand
{
	public SubmitSpadeDataCommand()
	{
		this.usage = " [prices <URL>] - submit automatically gathered data.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.startsWith( "prices" ) )
		{
			MallPriceDatabase.submitPrices( parameters.substring( 6 ).trim() );
			return;
		}

		String[] data = Preferences.getString( "spadingData" ).split( "\\|" );
		if ( data.length < 3 )
		{
			KoLmafia.updateDisplay( "No spading data has been collected yet. " + "Please try again later." );
			return;
		}
		for ( int i = 0; i < data.length - 2; i += 3 )
		{
			String contents = data[ i ];
			String recipient = data[ i + 1 ];
			String explanation = data[ i + 2 ];
			if ( InputFieldUtilities.confirm( "Would you like to send the data \"" + contents + "\" to " + recipient + "?\nThis information will be used " + explanation ) )
			{
				RequestThread.postRequest( new SendMailRequest( recipient, contents ) );
			}
		}
		Preferences.setString( "spadingData", "" );
	}
}
