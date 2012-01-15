/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPriceDatabase
	extends KoLDatabase
{
	private static final PriceArray prices = new PriceArray();
	private static final HashSet updated = new HashSet();
	private static final HashSet submitted = new HashSet();
	private static int modCount = 0;
	static
	{
		updatePrices( "mallprices.txt", false );
		updatePrices( "mallprices.txt", true );
		MallPriceDatabase.modCount = 0;
	}

	private static int updatePrices( String filename, boolean allowOverride )
	{
		BufferedReader reader = DataUtilities.getReader(
			UtilityConstants.DATA_DIRECTORY, filename, allowOverride );

		String line = FileUtilities.readLine( reader );
		if ( line == null )
		{
			RequestLogger.printLine( "(file not found)" );
			return 0;
		}

		if ( StringUtilities.parseInt( line ) != KoLConstants.MALLPRICES_VERSION )
		{
			RequestLogger.printLine( "(incompatible price file format)" );
			return 0;
		}

		String[] data;
		int count = 0;
		long now = System.currentTimeMillis() / 1000L;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 3 )
			{
				continue;
			}

			int id = StringUtilities.parseInt( data[ 0 ] );
			long timestamp = Math.min( now, Long.parseLong( data[ 1 ] ) );
			int price = StringUtilities.parseInt( data[ 2 ] );
			if ( id < 1 || id > ItemDatabase.maxItemId() ||
				price < 1 || price > 999999999 || timestamp <= 0 )
			{	// Something's fishy with this file...
				return count;
			}

			if ( !ItemDatabase.isTradeable( id ) ) continue;
			Price p = MallPriceDatabase.prices.get( id );
			if ( p == null )
			{
				MallPriceDatabase.prices.set( id, new Price( price, timestamp ) );
				++count;
				++MallPriceDatabase.modCount;
			}
			else if ( timestamp > p.timestamp )
			{
				p.price = price;
				p.timestamp = timestamp;
				++count;
				++MallPriceDatabase.modCount;
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		return count;
	}

	public static void updatePrices( String filename )
	{
		if ( filename.length() == 0 )
		{
			RequestLogger.printLine( "No URL or filename specified." );
			return;
		}

		if ( filename.startsWith( "http://" ) )
		{
			if ( MallPriceDatabase.updated.contains( filename ) )
			{
				RequestLogger.printLine( "Already updated from " + filename + " in this session." );
				return;
			}
			MallPriceDatabase.updated.add( filename );
		}
		int count = MallPriceDatabase.updatePrices( filename, true );
		if ( count > 0 )
		{
			MallPriceDatabase.writePrices();
			ConcoctionDatabase.refreshConcoctions( true );
		}
		RequestLogger.printLine( count + " price" + ( count != 1 ? "s" : "" ) +
			" updated from " + filename );
	}

	public static void recordPrice( int itemId, int price )
	{
		long timestamp = System.currentTimeMillis() / 1000L;
		Price p = MallPriceDatabase.prices.get( itemId );
		if ( p == null )
		{
			MallPriceDatabase.prices.set( itemId, new Price( price, timestamp ) );
		}
		else
		{
			p.price = price;
			p.timestamp = timestamp;
		}
		++MallPriceDatabase.modCount;
		MallPriceDatabase.writePrices();
	}

	private static void writePrices()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "mallprices.txt" );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.MALLPRICES_VERSION );

		for ( int i = 1; i < MallPriceDatabase.prices.size(); ++i )
		{
			Price p = MallPriceDatabase.prices.get( i );
			if ( p == null ) continue;
			writer.println( i + "\t" + p.timestamp + "\t" + p.price );
		}

		writer.close();
	}

	public static void submitPrices( String url )
	{
		if ( url.length() == 0 )
		{
			RequestLogger.printLine( "No URL specified." );
			return;
		}

		if ( MallPriceDatabase.modCount == 0 )
		{
			RequestLogger.printLine( "You have no updated price data to submit." );
			return;
		}
		if ( MallPriceDatabase.submitted.contains( url ) )
		{
			RequestLogger.printLine( "Already submitted to " + url + " in this session." );
			return;
		}

		try
		{
			HttpURLConnection con = (HttpURLConnection)
				new URL( url ).openConnection();
			con.setDoInput( true );
			con.setDoOutput( true );
			con.setRequestProperty( "Content-Type",
			"multipart/form-data; boundary=--blahblahfishcakes" );
			con.setRequestMethod( "POST" );
			OutputStream o = con.getOutputStream();
			BufferedWriter w = new BufferedWriter( new OutputStreamWriter( o ) );
			w.write( "----blahblahfishcakes\r\n" );
			w.write( "Content-Disposition: form-data; name=\"upload\"; filename=\"mallprices.txt\"\r\n\r\n" );

			BufferedReader reader = DataUtilities.getReader(
				UtilityConstants.DATA_DIRECTORY, "mallprices.txt", true );
			String line;
			while ( (line = FileUtilities.readLine( reader )) != null )
			{
				w.write( line );
				w.write( '\n' );
			}
			w.write( "\r\n----blahblahfishcakes--\r\n" );
			w.flush();
			o.close();

			InputStream i = con.getInputStream();
			int responseCode = con.getResponseCode();
			String response = "";
			if ( i != null )
			{
				response = new BufferedReader( new InputStreamReader( i ) ).readLine();
				i.close();
			}
			if ( responseCode == 200 )
			{
				RequestLogger.printLine( "Success: " + response );
				MallPriceDatabase.submitted.add( url );
			}
			else
			{
				RequestLogger.printLine( "Error " + responseCode + ": " + response );
			}
		}
		catch ( Exception e )
		{
			RequestLogger.printLine( "Submission failed: " + e );
			return;
		}
	}

	public static int getPrice( int itemId )
	{
		Price p = MallPriceDatabase.prices.get( itemId );
		return p == null ? 0 : p.price;
	}

	// Return age of price data, in fractional days
	public static float getAge( int itemId )
	{
		Price p = MallPriceDatabase.prices.get( itemId );
		long now = System.currentTimeMillis() / 1000L;
		return p == null ? Float.POSITIVE_INFINITY :
			(now - p.timestamp) / 86400.0f;
	}

	private static class Price
	{
		int price;
		long timestamp;

		public Price( int price, long timestamp )
		{
			this.price = price;
			this.timestamp = timestamp;
		}
	}

	/**
	 * Internal class which functions exactly an array of Prices, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	public static class PriceArray
	{
		private final ArrayList internalList = new ArrayList();

		public Price get( final int index )
		{
			return index < 0 || index >= this.internalList.size() ? null : (Price) this.internalList.get( index );
		}

		public void set( final int index, final Price value )
		{
			for ( int i = this.internalList.size(); i <= index; ++i )
			{
				this.internalList.add( null );
			}

			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
