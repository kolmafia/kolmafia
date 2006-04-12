/**
 * Copyright (c) 2006, KoLmafia development team
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;

public class AreaCombatData
{
	private boolean valid;
	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;

	public AreaCombatData( String data )
	{	this.valid = parse( data );
	}

	private boolean parse( String s )
	{
		int minHit = 0;
		int maxHit = 0;
		int minEvade = 0;
		int maxEvade = 0;

		// Hit: 10 Evade: 12-13

		StringTokenizer tokens = new StringTokenizer( s, " " );
		while ( tokens.hasMoreTokens() )
		{
			try
			{
				String option = tokens.nextToken();
				if ( option.equals( "Hit:" ) )
				{
					if ( !tokens.hasMoreTokens() )
						return false;
					String value = tokens.nextToken();
					int dash = value.indexOf( "-" );
					if ( dash != -1 )
					{
						minHit = Integer.parseInt( value.substring( 0, dash ) );
						maxHit = Integer.parseInt( value.substring( dash + 1 ) );
					}
					else
					{
						minHit = Integer.parseInt( value );
						maxHit = minHit;
					}
					continue;
				}

				if ( option.equals( "Evade:" ) )
				{
					if ( !tokens.hasMoreTokens() )
						return false;
					String value = tokens.nextToken();
					int dash = value.indexOf( "-" );
					if ( dash != -1 )
					{
						minEvade = Integer.parseInt( value.substring( 0, dash ) );
						maxEvade = Integer.parseInt( value.substring( dash + 1 ) );
					}
					else
					{
						minEvade = Integer.parseInt( value );
						maxEvade = minEvade;
					}
					continue;
				}
			}
			catch ( Exception e )
			{
			}

			return false;
		}

		this.minHit = minHit;
		this.maxHit = maxHit;
		this.minEvade = minEvade;
		this.maxEvade = maxEvade;
		return true;
	}

	public boolean valid()
	{	return valid;
	}

	public int minHit()
	{	return minHit;
	}

	public int maxHit()
	{	return maxHit;
	}

	public int minEvade()
	{	return minEvade;
	}

	public int maxEvade()
	{	return maxEvade;
	}
}
