/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TrophyRequest
	extends GenericRequest
{
	private static final Pattern TROPHY_PATTERN = Pattern.compile(
		"<td><img[^>]*? src=\"http://images.kingdomofloathing.com/(.+?)\".*?<td[^>]*?>(.+?)<.*?name=public(\\d+)\\s*(checked)?\\s*>",
		Pattern.DOTALL );
	private ArrayList trophies;

	// Get current trophies.
	public TrophyRequest()
	{
		this( null );
	}

	// Rearrange trophies.
	public TrophyRequest( ArrayList trophies )
	{
		super( "trophies.php" );
		this.trophies = trophies;
	}

	@Override
	public void run()
	{
		if ( this.trophies == null )
		{
			super.run();
			this.trophies = new ArrayList();

			if ( this.responseText == null )
			{
				return;
			}

			Matcher m = TrophyRequest.TROPHY_PATTERN.matcher( this.responseText );
			while ( m.find() )
			{
				this.trophies.add( new Trophy( m.group( 1 ), m.group( 2 ),
					StringUtilities.parseInt( m.group( 3 ) ),
					m.group( 4 ) != null ) );
			}
			return;
		}

		this.addFormField( "action", "Yup." );
		Iterator i = this.trophies.iterator();
		while ( i.hasNext() )
		{
			Trophy t = (Trophy) i.next();
			if ( t.visible )
			{
				this.addFormField( "public" + t.id, "on" );
			}
		}
		super.run();

		// Multiple trophy moving only works via GET, not POST.
		StringBuffer buf = new StringBuffer( "trophies.php?moveall=yes" );
		i = this.trophies.iterator();
		int pos = 1;
		while ( i.hasNext() )
		{
			Trophy t = (Trophy) i.next();
			buf.append( "&trophy" );
			buf.append( t.id );
			buf.append( "=" );
			buf.append( pos++ );
		}

		this.constructURLString( "blah", false );	// clear out cached URL data
		this.constructURLString( buf.toString(), false );

		super.run();
	}

	public ArrayList getTrophies()
	{
		return this.trophies;
	}

	public static class Trophy
	{
		public final String filename;
		public final String name;
		public final int id;
		public boolean visible;

		public Trophy( String filename, String name, int id, boolean visible )
		{
			this.filename = filename;
			this.name = name;
			this.id = id;
			this.visible = visible;
		}
	}
}
