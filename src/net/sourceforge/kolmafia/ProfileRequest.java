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

import java.util.Date;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;

public class ProfileRequest extends KoLRequest
{
	private static final SimpleDateFormat sdf = new SimpleDateFormat( "MMMM d, yyyy" );

	private String playerName;
	private String playerID;
	private int playerLevel;
	private int currentMeat;
	private int turnsPlayed;
	private String classType;

	private Date lastLogin;
	private String food, drink;
	private String ascensionCount, pvpRank;

	private String muscle, mysticism, moxie;
	private String title, rank, karma;

	public ProfileRequest( KoLmafia client, String playerName )
	{
		super( client, "showplayer.php" );
		addFormField( "who", client.getPlayerID( playerName ) );

		this.playerName = playerName;
		this.playerID = client.getPlayerID( playerName );

		this.muscle = "0";
		this.mysticism = "0";
		this.moxie = "0";
		this.karma = "0";
	}

	public void run()
	{
		super.run();

		try
		{
			// This is a massive replace which makes the profile easier to
			// parse and re-represent inside of editor panes.

			String cleanHTML = responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" );
			StringTokenizer st = new StringTokenizer( cleanHTML, "\n" );

			String token = st.nextToken();
			while ( !token.startsWith( "Level" ) )
				token = st.nextToken();

			this.playerLevel = Integer.parseInt( token.substring( 6 ) );

			KoLCharacter data = new KoLCharacter( playerName );
			data.setClassName( st.nextToken().trim() );
			this.classType = data.getClassType();

			while ( !st.nextToken().startsWith( "Meat" ) );
			this.currentMeat = df.parse( st.nextToken().trim() ).intValue();

			if ( cleanHTML.indexOf( "\nAscensions" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Ascensions" ) );
				st.nextToken();
				this.ascensionCount = st.nextToken().trim();
			}
			else
				this.ascensionCount = "0";

			while ( !st.nextToken().startsWith( "Turns" ) );
			this.turnsPlayed = df.parse( st.nextToken().trim() ).intValue();

			while ( !st.nextToken().startsWith( "Last" ) );
			this.lastLogin = sdf.parse( st.nextToken().trim() );

			if ( cleanHTML.indexOf( "\nFavorite Food" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Favorite" ) );
				this.food = st.nextToken().trim();
			}
			else
				this.food = "none";

			if ( cleanHTML.indexOf( "\nFavorite Booze" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Favorite" ) );
				this.drink = st.nextToken().trim();
			}
			else
				this.drink = "none";

			if ( cleanHTML.indexOf( "\nRanking" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Ranking" ) );
				this.pvpRank = st.nextToken().trim();
			}
			else
				this.pvpRank = "&nbsp;";
		}
		catch ( Exception e )
		{
		}
	}

	public String getPlayerName()
	{	return playerName;
	}

	public String getPlayerID()
	{	return playerID;
	}

	public void initialize()
	{
		if ( responseText == null )
			this.run();
	}

	public String getClassType()
	{
		initialize();
		return classType;
	}

	public void setPlayerLevel( int playerLevel )
	{	this.playerLevel = playerLevel;
	}

	public int getPlayerLevel()
	{	return playerLevel;
	}

	public int getCurrentMeat()
	{
		initialize();
		return currentMeat;
	}

	public int getTurnsPlayed()
	{
		initialize();
		return turnsPlayed;
	}

	public Date getLastLogin()
	{
		initialize();
		return lastLogin;
	}

	public String getLastLoginAsString()
	{
		initialize();
		return sdf.format( lastLogin );
	}

	public String getFood()
	{
		initialize();
		return food;
	}

	public String getDrink()
	{
		initialize();
		return drink;
	}

	public String getPvpRank()
	{
		initialize();
		return pvpRank;
	}

	public void setMuscle( String muscle )
	{	this.muscle = muscle;
	}

	public String getMuscle()
	{	return muscle;
	}

	public void setMysticism( String mysticism )
	{	this.mysticism = mysticism;
	}

	public String getMysticism()
	{	return mysticism;
	}

	public void setMoxie( String moxie )
	{	this.moxie = moxie;
	}

	public String getMoxie()
	{	return moxie;
	}

	public String getPower()
	{	return String.valueOf( Integer.parseInt( muscle ) + Integer.parseInt( mysticism ) + Integer.parseInt( moxie ) );
	}

	public void setTitle( String title )
	{	this.title = title;
	}

	public String getTitle()
	{	return title;
	}

	public void setRank( String rank )
	{	this.rank = rank;
	}

	public String getRank()
	{	return rank;
	}

	public void setKarma( String karma )
	{	this.karma = karma;
	}

	public String getKarma()
	{	return karma;
	}

	public String getAscensionCount()
	{
		initialize();
		return ascensionCount;
	}
}
