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
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

public class ProfileRequest extends KoLRequest
{
	private static final Pattern DATA_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>" );
	private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat( "MMMM d, yyyy", Locale.US );
	private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat( "yyyy-MM-dd", Locale.US );

	private String playerName;
	private String playerID;
	private Integer playerLevel;
	private String restriction;
	private Integer currentMeat;
	private Integer turnsPlayed, currentRun;
	private String classType;

	private Date created, lastLogin;
	private String food, drink;
	private Integer ascensionCount, pvpRank, karma;

	private Integer muscle, mysticism, moxie;
	private String title, rank;

	private String clanName;
	private int equipmentPower;

	public ProfileRequest( String playerName )
	{
		super( "showplayer.php" );

		if ( playerName.startsWith( "#" ) )
		{
			this.playerID = playerName.substring(1);
			this.playerName = KoLmafia.getPlayerName( playerID );
		}
		else
		{
			this.playerName = playerName;
			this.playerID = KoLmafia.getPlayerID( playerName );
		}

		addFormField( "who", playerID );

		this.muscle = new Integer(0);
		this.mysticism = new Integer(0);
		this.moxie = new Integer(0);
		this.karma = new Integer(0);
	}

	/**
	 * Internal method used to refresh the fields of the profile
	 * request based on the response text.  This should be called
	 * after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		// Nothing to refresh if no text
		if  ( responseText.length() == 0 )
			return;

		// This is a massive replace which makes the profile easier to
		// parse and re-represent inside of editor panes.

		String cleanHTML = responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" );
		StringTokenizer st = new StringTokenizer( cleanHTML, "\n" );

		String token = st.nextToken();
		while ( !token.startsWith( "Level" ) )
			token = st.nextToken();

		// It's possible that the player recently ascended and therefore
		// there's no data on the character.  Default the values and
		// return, if this is the case.

		if ( token.length() == 6 )
		{
			this.playerLevel = new Integer( 0 );
			this.classType = "Recent Ascension";
			this.currentMeat = new Integer( 0 );
			this.ascensionCount = new Integer( 0 );
			this.turnsPlayed = new Integer( 0 );
			this.created = new Date();
			this.lastLogin = new Date();
			this.food = "none";
			this.drink = "none";
			this.pvpRank = new Integer( 0 );
			return;
		}

		this.playerLevel = Integer.valueOf( token.substring(5).trim() );
		this.classType = KoLCharacter.getClassType( st.nextToken().trim() );

		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 && cleanHTML.indexOf( "\nPath" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Path" ) );
			this.restriction = st.nextToken().trim();
		}
		else
			this.restriction = "No-Path";

		while ( !st.nextToken().startsWith( "Meat" ) );
		this.currentMeat = new Integer( StaticEntity.parseInt( st.nextToken().trim() ) );

		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Ascensions" ) );
			st.nextToken();
			this.ascensionCount = new Integer( StaticEntity.parseInt( st.nextToken().trim() ) );
		}
		else
			this.ascensionCount = new Integer( 0 );

		while ( !st.nextToken().startsWith( "Turns" ) );
		this.turnsPlayed = new Integer( StaticEntity.parseInt( st.nextToken().trim() ) );

		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Turns" ) );
			this.currentRun = new Integer( StaticEntity.parseInt( st.nextToken().trim() ) );
		}
		else
			this.currentRun = turnsPlayed;

		String dateString = null;
		while ( !st.nextToken().startsWith( "Account" ) );
		try
		{
			dateString = st.nextToken().trim();
			this.created = INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
			this.created = new Date();
		}

		while ( !st.nextToken().startsWith( "Last" ) );

		try
		{
			dateString = st.nextToken().trim();
			this.lastLogin = INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
			this.lastLogin = this.created;
		}

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
			this.pvpRank = new Integer( StaticEntity.parseInt( st.nextToken().trim() ) );
		}
		else
			this.pvpRank = new Integer( 0 );

		this.equipmentPower = 0;
		if ( cleanHTML.indexOf( "\nEquipment" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Equipment" ) );

			String currentItem;
			while ( EquipmentDatabase.contains( currentItem = st.nextToken() ) )
			{
				switch ( TradeableItemDatabase.getConsumptionType( currentItem ) )
				{
					case ConsumeItemRequest.EQUIP_HAT:
					case ConsumeItemRequest.EQUIP_PANTS:

						this.equipmentPower += EquipmentDatabase.getPower( currentItem );
						break;
				}
			}
		}
	}

	/**
	 * Static method used by the clan manager in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static ProfileRequest getInstance( String playerName, String playerID, String playerLevel, String responseText, String rosterRow )
	{
		ProfileRequest instance = new ProfileRequest( playerName );
		instance.playerID = playerID;

		// First, initialize the level field for the
		// current player.

		instance.playerLevel = Integer.valueOf( playerLevel );

		// Next, refresh the fields for this player.
		// The response text should be copied over
		// before this happens.

		instance.responseText = responseText;
		instance.refreshFields();

		// Next, parse out all the data in the
		// row of the detail roster table.

		Matcher dataMatcher = DATA_PATTERN.matcher( rosterRow );

		// The name of the player occurs in the first
		// field of the table.  Because you already
		// know the name of the player, this can be
		// arbitrarily skipped.

		dataMatcher.find();

		// The player's three primary stats appear in
		// the next three fields of the table.

		dataMatcher.find( dataMatcher.end() );
		instance.muscle = new Integer( StaticEntity.parseInt( dataMatcher.group(1) ) );

		dataMatcher.find( dataMatcher.end() );
		instance.mysticism = new Integer( StaticEntity.parseInt( dataMatcher.group(1) ) );

		dataMatcher.find( dataMatcher.end() );
		instance.moxie = new Integer( StaticEntity.parseInt( dataMatcher.group(1) ) );

		// The next field contains the total power,
		// and since this is calculated, it can be
		// skipped in data retrieval.

		dataMatcher.find();

		// The next two fields contain the title and
		// rank within the clan for the player.

		dataMatcher.find( dataMatcher.end() );
		instance.title = dataMatcher.group(1);

		dataMatcher.find( dataMatcher.end() );
		instance.rank = dataMatcher.group(1);

		// The last field contains the total karma
		// accumulated by this player.

		dataMatcher.find( dataMatcher.end() );
		instance.karma = new Integer( StaticEntity.parseInt( dataMatcher.group(1) ) );

		return instance;
	}

	/**
	 * Static method used by the flower hunter in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static ProfileRequest getInstance( String playerName, String playerID, String clanName, Integer playerLevel, String classType, Integer pvpRank )
	{
		ProfileRequest instance = new ProfileRequest( playerName );
		instance.playerID = playerID;
		instance.playerLevel = playerLevel;
		instance.clanName = clanName == null ? "" : clanName;
		instance.classType = classType;
		instance.pvpRank = pvpRank;

		return instance;
	}

	public String getPlayerName()
	{	return playerName;
	}

	public String getPlayerID()
	{	return playerID;
	}

	public String getClanName()
	{	return clanName;
	}

	public void initialize()
	{
		if ( responseText == null )
			this.run();
	}

	public String getRestriction()
	{
		initialize();
		return restriction;
	}

	public String getClassType()
	{
		if ( classType == null )
			initialize();

		return classType;
	}

	public Integer getPlayerLevel()
	{
		if ( playerLevel == null || playerLevel.intValue() == 0 )
			initialize();

		return playerLevel;
	}

	public Integer getCurrentMeat()
	{
		initialize();
		return currentMeat;
	}

	public Integer getTurnsPlayed()
	{
		initialize();
		return turnsPlayed;
	}

	public Integer getCurrentRun()
	{
		initialize();
		return currentRun;
	}

	public Date getLastLogin()
	{
		initialize();
		return lastLogin;
	}

	public Date getCreation()
	{
		initialize();
		return created;
	}

	public String getCreationAsString()
	{
		initialize();
		return OUTPUT_FORMAT.format( created );
	}

	public String getLastLoginAsString()
	{
		initialize();
		return OUTPUT_FORMAT.format( lastLogin );
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

	public Integer getPvpRank()
	{
		if ( pvpRank == null || pvpRank.intValue() == 0 )
			initialize();

		return pvpRank;
	}

	public Integer getMuscle()
	{	return muscle;
	}

	public Integer getMysticism()
	{	return mysticism;
	}

	public Integer getMoxie()
	{	return moxie;
	}

	public Integer getPower()
	{	return new Integer( muscle.intValue() + mysticism.intValue() + moxie.intValue() );
	}

	public Integer getEquipmentPower()
	{	return new Integer( equipmentPower );
	}

	public String getTitle()
	{	return title;
	}

	public String getRank()
	{	return rank;
	}

	public Integer getKarma()
	{	return karma;
	}

	public Integer getAscensionCount()
	{
		initialize();
		return ascensionCount;
	}

	protected void processResults()
	{
		responseText.replaceFirst( "http://www[23]?\\.kingdomofloathing\\.com/ascensionhistory\\.php?back=self&who=([\\d]+)", "../ascensions/$1.htm" );
		refreshFields();
	}

	protected boolean mayChangeCreatables()
	{	return false;
	}
}
