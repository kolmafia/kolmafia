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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanFortuneRequest
	extends GenericRequest
{
	public enum Buff
	{
		FAMILIAR( "-1" ),
		ITEM( "-2" ),
		MEAT( "-3"),
		MUSCLE( "-4" ),
		MYSTICALITY( "-5" ),
		MOXIE( "-6" );

		private final String value;

		private Buff( String value )
		{
			this.value = value;
		}

		public String getValue()
		{
			return this.value;
		}
	}

	// preaction=lovetester
	// choice.php?whichchoice=1278&option=1
	// which: 1 (clanmate), -1 (susie), -2 (hagnk), -3 (meatsmith), -4 muscle -5 myst -6 moxie
	// whichid=clanmate
	// q1=food q2=character q3=word

	private static final Pattern USES_PATTERN = Pattern.compile( "clanmate (\\d) time" );

	public ClanFortuneRequest()
	{
		super( "choice.php" );
	}

	public ClanFortuneRequest( final Buff buff )
	{
		this( buff,
			Preferences.getString( "clanFortuneWord1" ),
			Preferences.getString( "clanFortuneWord2" ),
			Preferences.getString( "clanFortuneWord3" ) );
	}

	public ClanFortuneRequest( final Buff buff, final String word1, final String word2, final String word3 )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1278" );
		this.addFormField( "option", "1" );
		this.addFormField( "which", buff.getValue() );
		this.addFormField( "q1", word1 );
		this.addFormField( "q2", word2 );
		this.addFormField( "q3", word3 );
	}
	
	public ClanFortuneRequest( final String name )
	{
		this( name,
			Preferences.getString( "clanFortuneWord1" ),
			Preferences.getString( "clanFortuneWord2" ),
			Preferences.getString( "clanFortuneWord3" ) );
	}
	
	public ClanFortuneRequest( final String name, final String word1, final String word2, final String word3 )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1278" );
		this.addFormField( "option", "1" );
		this.addFormField( "which", "1" );
		this.addFormField( "whichid", name );
		this.addFormField( "q1", word1 );
		this.addFormField( "q2", word2 );
		this.addFormField( "q3", word3 );
	}
	// choice.php?whichchoice=1278&which=1&whichid=cheesefax&q1=food&q2=batman&q3=thick
	// choice.php?pwd&whichchoice=1278&option=1&which=1&whichid=cheesefax&q1=food&q2=batman&q3=thick

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}
	
	@Override
	public void run()
	{
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}
		RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.FORTUNE ) );
		super.run();
	}

	@Override
	public void processResults()
	{
		ClanFortuneRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( ( !urlString.startsWith( "choice.php" ) && !urlString.contains( "preaction=lovetester" ) ) || responseText == null )
		{
			return;
		}

		Preferences.setBoolean( "_clanFortuneBuffUsed", !responseText.contains( "resident of Seaside Town" ) );

		Matcher matcher = USES_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int usesLeft = StringUtilities.parseInt( matcher.group( 1 ) );
			Preferences.setInteger( "_clanFortuneConsultUses", 3 - usesLeft );
		}
		else
		{
			Preferences.setInteger( "_clanFortuneConsultUses", 3 );
		}
	}
	
	// You may consult Madame Zatara about your relationship with a resident of Seaside Town.

	// You may still consult Madame Zatara about your relationship with a clanmate 3 times today.
	// You may still consult Madame Zatara about your relationship with a clanmate 1 time today.
}
