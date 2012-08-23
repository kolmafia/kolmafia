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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordHashRequest
	extends GenericRequest
{
	private static final Pattern HASH_PATTERN_1 = Pattern.compile( "name=[\"\']?pwd[\"\']? value=[\"\']([^\"\']+)[\"\']" );
	private static final Pattern HASH_PATTERN_2 = Pattern.compile( "pwd=([^&]+)" );
	private static final Pattern HASH_PATTERN_3 = Pattern.compile( "pwd = \"([^\"]+)\"" );

	public PasswordHashRequest( final String location )
	{
		super( location );
	}

	@Override
	public void processResults()
	{
		super.processResults();

		if ( !GenericRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		PasswordHashRequest.updatePasswordHash( this.responseText );
	}
	
	public static void updatePasswordHash( String responseText )
	{
		Matcher pwdmatch = PasswordHashRequest.HASH_PATTERN_1.matcher( responseText );
		if ( pwdmatch.find() )
		{
			GenericRequest.setPasswordHash( pwdmatch.group( 1 ) );
			return;
		}

		pwdmatch = PasswordHashRequest.HASH_PATTERN_2.matcher( responseText );
		if ( pwdmatch.find() )
		{
			GenericRequest.setPasswordHash( pwdmatch.group( 1 ) );
			return;
		}		

		pwdmatch = PasswordHashRequest.HASH_PATTERN_3.matcher( responseText );
		if ( pwdmatch.find() )
		{
			GenericRequest.setPasswordHash( pwdmatch.group( 1 ) );
			return;
		}		
	}
}
