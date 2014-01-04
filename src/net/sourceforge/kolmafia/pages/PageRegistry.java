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

package net.sourceforge.kolmafia.pages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PageRegistry
{
	private static final Set<String> seenLocations = new HashSet<String>();
	private static final Map pagesByLocation = new HashMap();

	public static final boolean isGameAction( String path, String queryString )
	{
		if ( PageRegistry.isExternalLocation( path ) )
		{
			return false;
		}

		Page page = PageRegistry.getPage( path );

		if ( page == null )
		{
			return true;
		}

		return page.isGameAction( queryString );
	}

	private static synchronized Page getPage( String path )
	{

		if ( PageRegistry.seenLocations.contains( path ) )
		{
			return (Page) pagesByLocation.get( path );
		}

		PageRegistry.seenLocations.add( path );

		Class pageClass = null;

		try
		{
			String className = "net.sourceforge.kolmafia.pages." + path.substring( 0, path.length() - 4 );

			pageClass = Class.forName( className );
		}
		catch ( ClassNotFoundException e )
		{
		}

		if ( pageClass == null )
		{
			return null;
		}

		Page page = null;

		try
		{
			page = (Page) pageClass.newInstance();

			pagesByLocation.put( path, page );
		}
		catch ( Exception e )
		{
		}

		return page;
	}

	private static final boolean isExternalLocation( String path )
	{
		return path.length() == 0 || path.startsWith( "http:" ) || path.startsWith( "https:" ) || !path.endsWith( ".php" );
	}

}