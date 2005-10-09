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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Arrays;

import java.io.File;
import java.io.FileInputStream;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * Container class for <code>BuffBotManager</code>
 * Provides all aspects of BuffBot execution.
 */

public abstract class BuffBotManager extends KoLMailManager implements KoLConstants
{
	public static final int SAVEBOX = 0;
	public static final int DISPOSE = 1;

	protected static ArrayList saveList = new ArrayList();
	protected static ArrayList deleteList = new ArrayList();

	private static int messageDisposalSetting;
	private static boolean useChatBasedBuffBot;
	private static String refundMessage;
	private static String thanksMessage;

	private static Map buffCostMap = new TreeMap();
	private static int maxPhilanthropy = 0;
	private static int autoBuySetting = -1;
	private static SortedListModel buffCostTable = new SortedListModel();
	private static String [] whiteListArray = new String[0];

	public static final String MEAT_REGEX = "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat";

	public static void reset()
	{
		KoLMailManager.reset();

		buffCostMap.clear();
		buffCostTable.clear();

		saveList.clear();
		deleteList.clear();

		String [] soldBuffs = getProperty( "buffBotCasting" ).split( ";" );
		if ( soldBuffs[0].length() > 0 )
		{
			for ( int i = 0; i < soldBuffs.length; ++i )
			{
				String [] currentBuff = soldBuffs[i].split( ":" );
				if ( currentBuff.length == 4 )
				{
					addBuff( ClassSkillsDatabase.getSkillName( Integer.parseInt( currentBuff[0] ) ), Integer.parseInt( currentBuff[1] ),
						Integer.parseInt( currentBuff[2] ), currentBuff[3].equals( "true" ), false );
				}
				else if ( currentBuff.length == 5 )
				{
					addBuff( ClassSkillsDatabase.getSkillName( Integer.parseInt( currentBuff[0] ) ), Integer.parseInt( currentBuff[1] ),
						Integer.parseInt( currentBuff[2] ), currentBuff[3].equals( "true" ), currentBuff[4].equals( "true" ) );
				}
			}
		}
	}

	public static SortedListModel getBuffCostTable()
	{	return buffCostTable;
	}

	/**
	 * An internal method which adds a buff to the list of available
	 * buffs.  This also registers the buff inside of the list of
	 * available buffs.
	 */

	public static void addBuff( String skillName, int price, int castCount, boolean restricted, boolean philanthropic )
	{
		Integer newPrice = new Integer( price );
		BuffBotCaster newCast = new BuffBotCaster( skillName, price, castCount, restricted, philanthropic );

		buffCostTable.add( newCast );

		// Because the new concept allows multiple buffs
		// to have the same price, store things in a list.

		List castList = (List) buffCostMap.get( newPrice );

		// If this price has never existing before, go
		// ahead and add a new list to the data structure.

		if ( castList == null )
		{
			castList = new ArrayList();
			buffCostMap.put( newPrice, castList );
		}

		castList.add( newCast );
		saveBuffs();
	}

	/**
	 * An internal method which removes the list of selected buffs
	 * from the current mappings.
	 */

	public static void removeBuffs( Object [] buffs )
	{
		List castList;
		BuffBotCaster toRemove;

		for ( int i = 0; i < buffs.length; ++i )
		{
			toRemove = (BuffBotCaster) buffs[i];
			buffCostTable.remove( toRemove );

			castList = (List) buffCostMap.get( new Integer( toRemove.getPrice() ) );
			castList.remove( toRemove );
		}

		saveBuffs();
	}

	private static void saveBuffs()
	{
		StringBuffer sellerSetting = new StringBuffer();
		BuffBotCaster currentCast;

		if ( buffCostTable.size() > 0 )
			sellerSetting.append( ((BuffBotCaster) buffCostTable.get(0)).toSettingString() );

		for ( int i = 1; i < buffCostTable.size(); ++i )
		{
			sellerSetting.append( ';' );
			sellerSetting.append( ((BuffBotCaster) buffCostTable.get(i)).toSettingString() );
		}

		setProperty( "buffBotCasting", sellerSetting.toString() );
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public static synchronized void runBuffBot( int iterations )
	{
		boolean newMessages = false;
		BuffBotHome.setBuffBotActive( true );
		client.updateDisplay( DISABLED_STATE, "Buffbot started." );
		BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Starting new session" );

		maxPhilanthropy = Integer.parseInt( getProperty( "maxPhilanthropy" ) );
		useChatBasedBuffBot = getProperty( "useChatBasedBuffBot" ).equals( "true" );
		messageDisposalSetting = Integer.parseInt( getProperty( "buffBotMessageDisposal" ) );

		if ( useChatBasedBuffBot )
		{
			iterations = 1;
			KoLMessenger.initialize();
		}

		String whiteListString = getProperty( "whiteList" ).toLowerCase();
		if ( whiteListString.indexOf( "$clan" ) != -1 )
			whiteListString = whiteListString.replaceFirst( "\\$clan", ClanManager.retrieveClanListAsCDL() );

		whiteListArray = whiteListString.split( "\\s*,\\s*" );
		Arrays.sort( whiteListArray );

		refundMessage = client.getSettings().getProperty( "invalidBuffMessage" );
		thanksMessage = client.getSettings().getProperty( "thanksMessage" );

		autoBuySetting = -1;

		try
		{
			autoBuySetting = df.parse( client.getSettings().getProperty( "autoBuySetting" ) ).intValue();
		}
		catch ( Exception e )
		{
		}

		// The outer loop goes until user cancels, or
		// for however many iterations are needed.

		for ( int i = 1; BuffBotHome.isBuffBotActive() && i <= iterations; ++i )
		{
			// Request the inbox for the user.  Each call
			// to add message will trigger the actual
			// buffing attempt sequence, so all that
			// needs to be done is clear the lists and
			// initiate the mailbox request.

			newMessages = false;
			((List)mailboxes.get( "Inbox" )).clear();
			deleteList.clear();  saveList.clear();
			(new MailboxRequest( client, "Inbox" )).run();

			// Do all the deletes and saves now that all
			// the buffbot activity has been processed.

			if ( !deleteList.isEmpty() )
			{
				newMessages = true;
				deleteMessages( "Inbox", deleteList.toArray() );
			}

			if ( !saveList.isEmpty() )
			{
				newMessages = true;
				saveMessages( saveList.toArray() );
			}

			// Otherwise sleep for a while and then try again
			// (don't go away for more than 1 second at a time
			// to avoid re-enabling problems).

			if ( newMessages )
			{
				BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Message processing complete.  Buffbot is sleeping." );
				BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + client.getRestoreCount() + " mana restores remaining)" );
			}

			client.updateDisplay( DISABLED_STATE, "Buffbot is sleeping." );

			if ( i != iterations )
				for ( int j = 0; j < 75; ++j )
					if ( BuffBotHome.isBuffBotActive() )
						KoLRequest.delay( 1000 );
		}

		if ( !useChatBasedBuffBot )
		{
			BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot stopped." );
			client.updateDisplay( ENABLED_STATE, "Buffbot stopped." );
			BuffBotHome.setBuffBotActive( false );
		}
	}

	private static void sendEmptyMessageNotice( KoLMailMessage message )
	{
		BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Sending empty message notice to [" + message.getSenderName() + "]" );
		(new GreenMessageRequest( client, message.getSenderName(), "Your message contained no meat or items.", new AdventureResult( AdventureResult.MEAT, 0 ) )).run();
	}

	public static boolean addMessage( String boxname, String message )
	{
		boolean success = KoLMailManager.addMessage( boxname, message );
		if ( success && boxname.equals( "Inbox" ) )
		{
			client.resetContinueState();
			LockableListModel messages = getMessages( "Inbox" );

			try
			{
				if ( !processMessage( (KoLMailMessage) messages.get( messages.size() - 1 ) ) )
				{
					client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!" );
					BuffBotHome.setBuffBotActive( false );
					BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Unable to process a buff message." );
				}
			}
			catch ( Exception e )
			{
				// If an exception occurs during
				// the message processing, go ahead
				// and ignore it.

			}
		}

		return success;
	}

	private static boolean onWhiteList( String userName )
	{	return Arrays.binarySearch( whiteListArray, userName.toLowerCase() ) > -1;
	}

	private static void sendRefund( String recipient, String reason, int amount )
	{	sendRefund( recipient, reason, new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	private static void sendRefund( String recipient, String reason, AdventureResult refund )
	{
		(new GreenMessageRequest( client, recipient, reason, refund )).run();
		BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Sent refund to [" + recipient + "], " + refund.toString() );
	}

	/**
	 * Checks to see if there's an attached donation  by seeing if there's
	 * an image tag, with width of 30.  Valentine images have width of 100
	 * so we don't mark those as a false positive.
	 *
	 * @return	<code>true</code> if there is a donation
	 */

	private static boolean containsDonation( KoLMailMessage message )
	{
	    return message.getMessageHTML().replaceAll( MEAT_REGEX, "" ).indexOf( "width=30" ) != -1;
	}

	private static void sendThankYou( String recipient, String messageHTML )
	{
		if ( !thanksMessage.equals("") )
		{
			String reason = thanksMessage +
				LINE_BREAK + LINE_BREAK +
				">" + messageHTML.replaceAll( "<.*?>", " " ).replaceAll( "[ ]+", " " );

			(new GreenMessageRequest( client, recipient, reason, new AdventureResult( AdventureResult.MEAT, 0 ) )).run();
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Sent thank you to [" + recipient + "]" );
		}
		else
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Thank you message NOT sent to [" + recipient + "]" );

	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	private static void stockRestores() throws Exception
	{
		int currentRestores = -1;
		int calculatedRestores = client.getRestoreCount();

		if ( calculatedRestores <= autoBuySetting )
		{
			while ( BuffBotHome.isBuffBotActive() && calculatedRestores <= autoBuySetting && currentRestores != calculatedRestores )
			{
				currentRestores = calculatedRestores;
				client.updateDisplay( DISABLED_STATE, "Executing auto-stocking script..." );

				String scriptPath = getProperty( "autoStockScript" ) ;
				File autoStockScript = new File( scriptPath );

				if ( autoStockScript.exists() )
					(new KoLmafiaCLI( client, new FileInputStream( autoStockScript ) )).listenForCommands();
				else
				{
					client.updateDisplay( ERROR_STATE, "Could not find auto-stocking script." );
					return;
				}

				calculatedRestores = client.getRestoreCount();
			}

			if ( currentRestores == client.getRestoreCount() )
			{
				client.updateDisplay( ERROR_STATE, "Auto-stocking script failed to buy restores." );
				return;
			}
		}
	}

	private static boolean processMessage( KoLMailMessage message ) throws Exception
	{
		// Check to see if the user should autobuy MP restores
		// in order to restock.

		stockRestores();

		// Now that you're guaranteed to be above the threshold,
		// go ahead and process the message.

		if ( containsDonation( message ) )
		{
			sendThankYou( message.getSenderName(), message.getMessageHTML() );
			saveList.add( message );
			return true;
		}

		Matcher meatMatcher = Pattern.compile( MEAT_REGEX ).matcher( message.getMessageHTML() );
		int meatSent = meatMatcher.find() ? df.parse( meatMatcher.group(1) ).intValue() : 0;

		// Look for this amount in the buff table, and use up all the applicable
		// buffs at the given price.

		List castList = (List) buffCostMap.get( new Integer( meatSent ) );
		for ( int i = 0; castList != null && i < castList.size() && executeBuff( (BuffBotCaster) castList.get(i), message, meatSent ); ++i );

		// Must not be a buff request message, so notify user and save/delete;
		// also check to see if it was an attempted scam

		detectScam( message, "You gain ([\\d,]+) Meat" );
		return true;
	}

	private static boolean executeBuff( BuffBotCaster buff, KoLMailMessage message, int meatSent )
	{
		if ( buff != null )
		{
			if ( buff.restricted && !onWhiteList( message.getSenderName() ) )
			{
				// This is a restricted buff for a non-allowed user.
				// Update the log to show the request attempt and
				// send a message to the user indicating that this
				// is not a valid buff price.

				BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Request for restricted buff denied: from [" +
					message.getSenderName() + "] meat received: " + meatSent );

				sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );

				queueMessage( message, true );
				return true;
			}
			else if ( buff.philanthropic && BuffBotHome.getInstanceCount( meatSent, message.getSenderName() ) >= maxPhilanthropy )
			{
				if ( meatSent != 0 )
				{
					// This is a philanthropic buff request, but the
					// user already requested the buff previously.
					// Notify the user that this buff can only be
					// requested once per day.

					BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Repeated request: from [" +
						message.getSenderName() + "] meat received: " + meatSent );

					sendRefund( message.getSenderName(), "Sorry, this buff may only be requested " + maxPhilanthropy +
						(maxPhilanthropy == 1 ? " time" : " times") + " per day.", meatSent );

					queueMessage( message, true );
					return true;
				}
			}
			else
			{
				// Under all other circumstances, you go ahead and
				// process the buff request.

				client.resetContinueState();

				double buffPercentRemaining = buff.castOnTarget( message.getSenderName() );

				if ( buffPercentRemaining != 0.0 )
				{
					// Can't send refund to ronin/hardcore, so don't attempt
					// to send the refund.

					if ( UseSkillRequest.lastUpdate.endsWith( " cannot receive buffs." ) )
						buffPercentRemaining = 0.0;

					// If there was no update message, that means all the buffs
					// were successful, but the buffbot ran out of MP.

					if ( UseSkillRequest.lastUpdate.equals( "" ) )
						UseSkillRequest.lastUpdate = "Ran out of MP restores.";

					sendRefund( message.getSenderName(), "This buffbot was unable to process your request.  " +
						UseSkillRequest.lastUpdate + "  Please try again later.", (int) Math.floor( (double)meatSent * buffPercentRemaining ) );

					queueMessage( message, true );
					return true;
				}

				if ( buff.philanthropic )
					BuffBotHome.addToRecipientList( meatSent, message.getSenderName() );

				queueMessage( message, true );
				return true;

			}
		}
		else if ( meatSent >= 100000 )
		{
			// If the amount of meat sent is extremely large,
			// and no buff matches that value, assume that it's
			// a donation and send a thank you note.

			sendThankYou( message.getSenderName(), message.getMessageHTML() );
			queueMessage( message, false );
			return true;
		}
		else if ( meatSent != 0 )
		{
			// If the amount of meat is non-zero, then you can
			// go ahead and send a refund message.

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Meat received does not match anything in database: from [" +
				message.getSenderName() + "] meat received: " + meatSent );

			sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );
			queueMessage( message, true );
			return true;
		}

		// If it gets this far, then it's an empty message.  The
		// scam detection will automatically fix the problem.

		return true;
	}

	protected static void queueMessage( KoLMailMessage message, boolean delete )
	{
		if ( !saveList.contains( message ) && !deleteList.contains( message ) )
			(delete ? deleteList : saveList).add( message );
	}

	private static void detectScam( KoLMailMessage message, String scamString )
	{
		// For scam messages, leave the scam messages in
		// the player's inbox until further notice.

		Matcher scamMatcher = Pattern.compile( scamString ).matcher( message.getMessageHTML() );
		if ( scamMatcher.find() )
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Possible attempted scam message from [" + message.getSenderName() + "]" );

		// Now, mark for either save or delete the message,
		// or ignore the message, if applicable.

		else if ( messageDisposalSetting == SAVEBOX )
		{
			queueMessage( message, false );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Saving non-buff message from [" + message.getSenderName() + "]" );
		}
		else if ( messageDisposalSetting == DISPOSE )
		{
			queueMessage( message, true );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Deleting non-buff message from [" + message.getSenderName() + "]" );
		}
		else
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Ignoring non-buff message from [" + message.getSenderName() + "]" );
	}

	/**
	 * An internal class used to represent a single instance of casting a
	 * buff.  This is used to manage buffs inside of the BuffBotManager
	 * class by simply calling a method after a simple lookup.
	 */

	public static class BuffBotCaster implements Comparable
	{
		private int price;
		private int buffID;
		private String buffName;
		private int castCount;

		private String target;
		private boolean restricted;
		private boolean philanthropic;
		private String stringForm;
		private String settingString;

		private int requestsThisSession;

		public BuffBotCaster( String buffName, int price, int castCount, boolean restricted, boolean philanthropic )
		{
			this.buffID = ClassSkillsDatabase.getSkillID( buffName );
			this.buffName = buffName;
			this.price = price;
			this.castCount = castCount;

			this.restricted = restricted;
			this.philanthropic = philanthropic;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "Cast " );
			stringForm.append( this.buffName );
			stringForm.append( ' ' );
			stringForm.append( castCount );
			stringForm.append( " time" );

			if ( castCount != 1 )
				stringForm.append( 's' );

			stringForm.append( " for " );
			stringForm.append( price );
			stringForm.append( " meat" );

			if ( restricted )
				stringForm.append( " (white list only)" );
			if ( philanthropic )
				stringForm.append( " (philanthropic)" );

			this.stringForm = stringForm.toString();
			this.settingString = buffID + ":" + price + ":" + castCount + ":" + restricted + ":" + philanthropic;
		}

		public int compareTo( Object o )
		{	return o == null || !(o instanceof BuffBotCaster) ? - 1 : compareTo( (BuffBotCaster) o );
		}

		public int compareTo( BuffBotCaster bbc )
		{
			if ( price != bbc.price )
				return price - bbc.price;

			if ( restricted && !bbc.restricted )
				return -1;

			if ( !restricted && bbc.restricted )
				return 1;

			if ( philanthropic && !bbc.philanthropic )
				return -1;

			if ( !philanthropic && bbc.philanthropic )
				return 1;

			return 0;
		}

		public double castOnTarget( String target )
		{
			client.resetContinueState();

			++requestsThisSession;
			BuffBotHome.recordBuff( target, buffName, castCount, price );

			// Figure out how much MP the buff will take, and then identify
			// the number of casts per request that this character can handle.

			int totalCasts = castCount;
			double mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );
			double maximumMP = KoLCharacter.getMaximumMP();

			double currentMP;
			int currentCast, mpPerEvent;

			if ( price > 0 )
				BuffBotHome.update( BuffBotHome.BUFFCOLOR, "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + price + " meat... " );
			else
				BuffBotHome.update( BuffBotHome.BUFFCOLOR, "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + (-price) + " tiny houses... " );

			while ( totalCasts > 0 )
			{
				currentMP = (double) KoLCharacter.getCurrentMP();
				currentCast = Math.min( totalCasts, (int) (maximumMP / mpPerCast) );
				mpPerEvent = (int) (mpPerCast * currentCast);
				if ( !client.recoverMP( mpPerEvent ) )
					return (double)totalCasts / (double)castCount;

				(new UseSkillRequest( client, buffName, target, currentCast )).run();

				if ( !client.permitsContinue() )
				{
					BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast " + buffName + " on " + target );
					return (double)totalCasts / (double)castCount;
				}

				totalCasts -= currentCast;
				BuffBotHome.update( BuffBotHome.BUFFCOLOR, " ---> Successfully cast " + buffName + " " + currentCast + " times" );
			}

			return 0.0;
		}

		public int getPrice()
		{	return price;
		}

		public int getCastCount()
		{	return castCount;
		}

		public String toString()
		{	return stringForm;
		}

		public String toSettingString()
		{	return settingString;
		}

		public int getRequestsThisSession()
		{	return requestsThisSession;
		}
	}
}
