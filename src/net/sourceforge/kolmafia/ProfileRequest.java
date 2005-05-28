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
	private String cleanHTML;

	private String playerName;
	private String playerID;
	private int playerLevel;
	private int currentMeat;
	private int turnsPlayed;
	private String classType;

	private Date lastLogin;
	private String food, drink;
	private String pvpRank;

	private String muscle, mysticism, moxie;
	private String title, rank, karma;

	public ProfileRequest( KoLmafia client, String playerName )
	{
		super( client, "showplayer.php" );
		addFormField( "who", client.getPlayerID( playerName ) );

		this.cleanHTML = "";
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

		int secondTableIndex = responseText.indexOf( "</table><table>" );

		// This is a massive replace which makes the profile easier to
		// parse and re-represent inside of editor panes.

		this.cleanHTML = responseText.substring( responseText.indexOf( "</b>" ) + 4, secondTableIndex ).replaceAll(
			"<td", " <td" ).replaceAll( "<tr", "<br><tr" ).replaceAll( "</?[ctplhi].*?>", "" ).replaceAll(
			"[ ]+", " " ).replaceAll( "(<br> )+", "<br> " ) + "<br>" +
				responseText.substring( secondTableIndex, responseText.lastIndexOf( "send" ) ).replaceAll(
				"<td", " <td" ).replaceAll( "<tr", "<br><tr" ).replaceAll( "</?[tplh].*?>", "" ).replaceAll(
				"[ ]+", " " ).replaceAll( "(<br> )+", "<br> " ).replaceAll( "<[cC]enter>.*?</center>", "" ).replaceAll(
				"onClick=\'.*?\'", "" ).replaceFirst( "<br> Familiar:", "" ).replaceFirst(
				"</b>,", "</b><br>" ).replaceFirst( "<b>\\(</b>.*?<b>\\)</b>", "<br>" ).replaceFirst(
				"<b>Ranking:", "<b>PVP Ranking:" ).replaceFirst( "<br>", "" );

		// This completes the retrieval of the player profile.
		// Fairly straightforward, but really ugly-looking.
		// Now, parsing data related to the player.

		try
		{
			StringTokenizer st = new StringTokenizer( cleanHTML.replaceAll( "><", "" ), "<>" );
			String token = st.nextToken();

			while ( !token.startsWith( "Level" ) )
				token = st.nextToken();

			this.playerLevel = Integer.parseInt( token.substring( 6 ) );
			st.nextToken();

			KoLCharacter data = new KoLCharacter( playerName );
			data.setClassName( st.nextToken().trim() );
			this.classType = data.getClassType();

			while ( !st.nextToken().startsWith( "Meat" ) );
			st.nextToken();

			this.currentMeat = df.parse( st.nextToken().trim() ).intValue();

			while ( !st.nextToken().startsWith( "Turns" ) );
			st.nextToken();

			this.turnsPlayed = df.parse( st.nextToken().trim() ).intValue();

			while ( !st.nextToken().startsWith( "Last" ) );
			st.nextToken();

			this.lastLogin = sdf.parse( st.nextToken().trim() );

			if ( cleanHTML.indexOf( ">Favorite Food" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Favorite" ) );
				st.nextToken();
				this.food = st.nextToken().trim();
			}
			else
				this.food = "none";

			if ( cleanHTML.indexOf( ">Favorite Booze" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "Favorite" ) );
				st.nextToken();
				this.drink = st.nextToken().trim();
			}
			else
				this.drink = "none";

			if ( cleanHTML.indexOf( ">PVP Ranking" ) != -1 )
			{
				while ( !st.nextToken().startsWith( "PVP Ranking" ) );
				st.nextToken();
				this.pvpRank = st.nextToken().trim();
			}
			else
				this.pvpRank = "&nbsp;";
		}
		catch ( Exception e )
		{
		}
	}

	public String getCleanHTML()
	{	return cleanHTML;
	}

	public String getPlayerName()
	{	return playerName;
	}

	public String getPlayerID()
	{	return playerID;
	}

	public String getClassType()
	{	return classType;
	}

	public int getPlayerLevel()
	{	return playerLevel;
	}

	public int getCurrentMeat()
	{	return currentMeat;
	}

	public int getTurnsPlayed()
	{	return turnsPlayed;
	}

	public Date getLastLogin()
	{	return lastLogin;
	}

	public String getLastLoginAsString()
	{	return sdf.format( lastLogin );
	}

	public String getFood()
	{	return food;
	}

	public String getDrink()
	{	return drink;
	}

	public String getPvpRank()
	{	return pvpRank;
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
}
