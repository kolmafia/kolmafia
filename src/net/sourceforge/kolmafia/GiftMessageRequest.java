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

import java.util.ArrayList;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class GiftMessageRequest extends SendMessageRequest
{
	private int desiredCapacity;
	private String recipient, outsideMessage, insideMessage;
	private GiftWrapper wrappingType;
	private int maxCapacity, materialCost;
	private boolean isFromStorage;

	private static final LockableListModel PACKAGES = new LockableListModel();
	static
	{
		BufferedReader reader = KoLDatabase.getReader( "packages.dat" );
		String [] data;

		while ( (data = KoLDatabase.readData( reader )) != null )
			PACKAGES.add( new GiftWrapper( data[0], StaticEntity.parseInt( data[1] ), StaticEntity.parseInt( data[2] ), StaticEntity.parseInt( data[3] ) ) );
	}

	private static class GiftWrapper
	{
		private StringBuffer name;
		private int radio, maxCapacity, materialCost;

		public GiftWrapper( String name, int radio, int maxCapacity, int materialCost )
		{
			this.radio = radio;
			this.maxCapacity = maxCapacity;
			this.materialCost = materialCost;

			this.name = new StringBuffer();
			this.name.append( name );
			this.name.append( " - " );
			this.name.append( materialCost );
			this.name.append( " meat (" );
			this.name.append( maxCapacity );
			this.name.append( " item" );

			if ( maxCapacity > 1 )
				this.name.append( 's' );

			this.name.append( ')' );
		}

		public String toString()
		{	return name.toString();
		}
	}

	public GiftMessageRequest( String recipient, String outsideMessage, String insideMessage,
		int desiredCapacity, Object [] attachments )
	{
		this( recipient, outsideMessage, insideMessage, desiredCapacity, attachments, false );
	}

	public GiftMessageRequest( String recipient, String outsideMessage, String insideMessage,
		int desiredCapacity, Object [] attachments, boolean isFromStorage )
	{
		super( "town_sendgift.php", attachments );

		this.recipient = recipient;
		this.outsideMessage = outsideMessage;
		this.insideMessage = insideMessage;
		this.desiredCapacity = desiredCapacity;

		this.wrappingType = (GiftWrapper) PACKAGES.get( desiredCapacity );
		this.maxCapacity = this.wrappingType.maxCapacity;
		this.materialCost = this.wrappingType.materialCost;

		addFormField( "action", "Yep." );
		addFormField( "towho", this.recipient );
		addFormField( "note", this.outsideMessage );
		addFormField( "insidenote", this.insideMessage );
		addFormField( "whichpackage", String.valueOf( this.wrappingType.radio ) );

		// You can take from inventory (0) or Hagnks (1)
		addFormField( "fromwhere", isFromStorage ? "1" : "0" );

		if ( isFromStorage )
		{
			this.source = storage;
			this.destination = new ArrayList();
		}
	}

	protected int getCapacity()
	{	return maxCapacity;
	}

	protected boolean alwaysIndex()
	{	return true;
	}

	protected SendMessageRequest getSubInstance( Object [] attachments )
	{	return new GiftMessageRequest( recipient, outsideMessage, insideMessage, desiredCapacity, attachments, this.source == storage );
	}

	protected String getSuccessMessage()
	{	return "<td>Package sent.</td>";
	}

	protected String getItemField()
	{	return source == storage ? "hagnks_whichitem" : "whichitem";
	}

	protected String getQuantityField()
	{	return source == storage ? "hagnks_howmany" : "howmany";
	}

	protected String getMeatField()
	{	return source == storage ? "hagnks_sendmeat" : "sendmeat";
	}


	public static LockableListModel getPackages()
	{
		// Which packages are available depends on ascension count.
		// You start with two packages and receive an additional
		// package every three ascensions you complete.

		LockableListModel packages = new LockableListModel();
		int packageCount = Math.min( KoLCharacter.getAscensions() / 3 + 2, 11 );

		packages.addAll( PACKAGES.subList( 0, packageCount + 1 ) );
		return packages;
	}

	protected void processResults()
	{
		super.processResults();
		if ( responseText.indexOf( getSuccessMessage() ) != -1 && materialCost > 0 )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - materialCost ) );
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "town_sendgift.php" ) == -1 )
			return false;

		return processRequest( "send", urlString, storage, 0 );
	}

	protected String getStatusMessage()
	{	return "Sending package to " + KoLmafia.getPlayerName( getFormField( "towho" ) );
	}
}
