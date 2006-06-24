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
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class GiftMessageRequest extends SendMessageRequest
{
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

	public GiftMessageRequest( KoLmafia client, String recipient, String outsideMessage, String insideMessage,
		Object wrappingType, Object [] attachments, int meatAttachment )
	{
		this( client, recipient, outsideMessage, insideMessage, wrappingType, attachments, meatAttachment, false );
	}

	public GiftMessageRequest( KoLmafia client, String recipient, String outsideMessage, String insideMessage,
		Object wrappingType, Object [] attachments, int meatAttachment, boolean isFromStorage )
	{
		super( client, "town_sendgift.php", attachments, meatAttachment );
		addFormField( "pwd" );
		addFormField( "action", "Yep." );
		addFormField( "towho", recipient );
		addFormField( "note", outsideMessage );
		addFormField( "insidenote", insideMessage );

		this.recipient = KoLmafia.getPlayerID( recipient );
		this.outsideMessage = outsideMessage;
		this.insideMessage = insideMessage;

		this.wrappingType = (GiftWrapper) wrappingType;
		this.maxCapacity = this.wrappingType.maxCapacity;
		this.materialCost = this.wrappingType.materialCost;

		addFormField( "whichpackage", String.valueOf( this.wrappingType.radio ) );
		addFormField( isFromStorage ? "hagnks_sendmeat" : "sendmeat", String.valueOf( this.meatAttachment ) );

		// You can take from inventory (0) or Hagnks (1)
		addFormField( "fromwhere", isFromStorage ? "1" : "0" );

		if ( isFromStorage )
		{
			this.source = KoLCharacter.getStorage();
			this.whichField = "hagnks_whichitem";
			this.quantityField = "hagnks_howmany";
		}
	}

	protected int getCapacity()
	{	return maxCapacity;
	}

	protected boolean alwaysIndex()
	{	return true;
	}

	protected void repeat( Object [] attachments )
	{	(new GiftMessageRequest( client, recipient, outsideMessage, insideMessage, wrappingType, attachments, 0, this.source == KoLCharacter.getStorage() )).run();
	}

	protected String getSuccessMessage()
	{	return "<td>Package sent.</td>";
	}

	public static LockableListModel getPackages()
	{
		// Which packages are available depends on ascension count.
		// You start with two packages and receive an additional
		// package every three ascensions you complete.

		LockableListModel packages = new LockableListModel();
		int packageCount = Math.min( KoLCharacter.getAscensions() / 3 + 2, 11 );

		packages.addAll( PACKAGES.subList( 0, packageCount ) );
		return packages;
	}

	protected void processResults()
	{
		if ( responseText.indexOf( getSuccessMessage() ) != -1 && materialCost > 0 )
			client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - materialCost ) );

		super.processResults();
	}
}
