/**
 * Copyright (c) 2005, KoLmafia development team
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
import java.util.StringTokenizer;

public class FamiliarData implements Comparable
{
	private int id, weight;
	private String race, item;

	public FamiliarData( int id )
	{
		this.id = id;
		this.race = FamiliarsDatabase.getFamiliarName( id );
		this.weight = 0;
		this.item = "none";
	}

	public FamiliarData( int id, String html )
	{
		this.id = id;
		StringTokenizer parsedContent = new StringTokenizer( html, "<>" );
		String token = parsedContent.nextToken();

		while ( !token.startsWith( ", " ) )
			token = parsedContent.nextToken();

		this.weight = Integer.parseInt( (new StringTokenizer( token.substring( 6 ), " ()", true )).nextToken() );
		this.race = FamiliarsDatabase.getFamiliarName( id );
		this.item = html.indexOf( "<img" ) == -1 ? "none" :
			html.indexOf( "tamo.gif" ) != -1 ? "lucky Tam O'Shanter" : html.indexOf( "lnecklace.gif" ) != -1 ? "lead necklace" :
				FamiliarsDatabase.getFamiliarItem( id );
	}

	public FamiliarData( int id, int weight )
	{
		this.id = id;
		this.weight = weight;
		this.race = FamiliarsDatabase.getFamiliarName( id );
	}

	public int getID()
	{	return id;
	}

	public String getItem()
	{	return item;
	}

	public int getWeight()
	{	return weight;
	}

	public String getRace()
	{	return race;
	}

	public String toString()
	{	return race + " (" + weight + " lbs)";
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof FamiliarData && id == ((FamiliarData)o).id;
	}

	public int compareTo( Object o )
	{	return o == null || !(o instanceof FamiliarData) ? 1 : compareTo( (FamiliarData)o );
	}

	public int compareTo( FamiliarData fd )
	{	return race.compareToIgnoreCase( fd.race );
	}
}