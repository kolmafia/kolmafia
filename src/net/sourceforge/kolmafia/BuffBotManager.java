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

	private static final int SLEEP_TIME = 1000;  // Sleep this much each time
	private static final int SLEEP_COUNT = 75;   // Sleep his many times

	private static Map buffCostMap = new TreeMap();
	private static int maxPhilanthropy = 0;
	private static int autoBuySetting = -1;
	private static LockableListModel buffCostTable = new LockableListModel();
	private static String [] whiteListArray = new String[0];

	public static final String MEAT_REGEX = "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat";

	/**
	 * Constructor for the <code>BuffBotManager</code> class.
	 */

	public static void reset( LockableListModel costTable )
	{
		buffCostMap.clear();
		buffCostTable.clear();
		buffCostTable.addAll( costTable );

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

	/**
	 * An internal method which adds a buff to the list of available
	 * buffs.  This also registers the buff inside of the list of
	 * available buffs.
	 */

	public static void addBuff( String skillName, int price, int castCount, boolean restricted, boolean philanthropic )
	{
		BuffBotCaster newCast = new BuffBotCaster( skillName, price, castCount, restricted, philanthropic );
		buffCostTable.add( newCast );
		Object oldCast = buffCostMap.put( new Integer( price ), newCast );

		if ( oldCast != null )
			buffCostTable.remove( oldCast );

		saveBuffs();
	}

	/**
	 * An internal method which removes the list of selected buffs
	 * from the current mappings.
	 */

	public static void removeBuffs( Object [] buffs )
	{
		BuffBotCaster toRemove;
		for ( int i = 0; i < buffs.length; ++i )
		{
			toRemove = (BuffBotCaster) buffs[i];
			buffCostTable.remove( toRemove );
			buffCostMap.remove( new Integer( toRemove.getPrice() ) );
		}

		saveBuffs();
	}

	private static void saveBuffs()
	{
		StringBuffer sellerSetting = new StringBuffer();
		BuffBotManager.BuffBotCaster currentCast;

		if ( buffCostTable.size() > 0 )
			sellerSetting.append( ((BuffBotManager.BuffBotCaster) buffCostTable.get(0)).toSettingString() );

		for ( int i = 1; i < buffCostTable.size(); ++i )
		{
			sellerSetting.append( ';' );
			sellerSetting.append( ((BuffBotManager.BuffBotCaster) buffCostTable.get(i)).toSettingString() );
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
			client.initializeChat();
		}

		String whiteListString = getProperty( "whiteList" ).toLowerCase();
		if ( whiteListString.indexOf( "$clan" ) != -1 )
			whiteListString = whiteListString.replaceFirst( "\\$clan", client.getClanManager().retrieveClanListAsCDL() );

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

		// The outer loop goes until user cancels

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

			if ( i != iterations )
			{
				if ( newMessages )
				{
					BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Message processing complete." );
					BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot is sleeping." );
					BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + client.getRestoreCount() + " mana restores remaining)" );
				}

				client.updateDisplay( DISABLED_STATE, "BuffBot is sleeping" );

				for ( int j = 0; j < SLEEP_COUNT; ++j )
					if ( BuffBotHome.isBuffBotActive() )
						KoLRequest.delay( SLEEP_TIME );
			}
			else if ( newMessages )
				BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + client.getRestoreCount() + " mana restores remaining)" );
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
			if ( !processMessage( (KoLMailMessage) messages.get( messages.size() - 1 ) ) )
			{
				client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!" );
				BuffBotHome.setBuffBotActive( false );
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Unable to process a buff message." );
			}
		}

		return success;
	}

	private static boolean onWhiteList(String userName)
	{	return Arrays.binarySearch(whiteListArray, userName.toLowerCase()) > -1;
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

	private static void stockRestores()
	{
		int currentRestores = -1;
		int calculatedRestores = client.getRestoreCount();

		if ( calculatedRestores <= autoBuySetting )
		{
			try
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
			catch ( Exception e )
			{
				client.updateDisplay( ERROR_STATE, "Could not find auto-stocking script." );
				return;
			}
		}
	}

	private static boolean processMessage( KoLMailMessage message )
	{
		// Check to see if the user should autobuy MP restores
		// in order to restock.

		stockRestores();

		// Now that you're guaranteed to be above the threshold,
		// go ahead and process the message.

		int meatSent = 0;
		BuffBotCaster buff;

		try
		{
			Matcher meatMatcher = Pattern.compile( MEAT_REGEX ).matcher( message.getMessageHTML() );

			if ( BuffBotManager.containsDonation( message ) )
			{
				sendThankYou( message.getSenderName(), message.getMessageHTML() );
				saveList.add( message );
			}

			if ( meatMatcher.find() )
			{
				meatSent = df.parse( meatMatcher.group(1) ).intValue();

				// Look for this amount in the buff table
				buff = (BuffBotCaster) buffCostMap.get( new Integer( meatSent ) );
				if ( buff != null )
				{
					if ( ( !buff.restricted && !buff.philanthropic ) || ( buff.restricted && onWhiteList(message.getSenderName()) ) ||
						( buff.philanthropic && BuffBotHome.getInstanceCount( meatSent, message.getSenderName() ) < maxPhilanthropy ) )
					{
						return executeBuff( buff, message, meatSent );
					}
					else if ( buff.restricted )
					{
						// This is a restricted buff for a non-allowed user.
						BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Request for restricted buff denied: from [" +
							message.getSenderName() + "] meat received: " + meatSent );

						sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );

						if ( !saveList.contains( message ) )
							deleteList.add( message );
						return true;
					}
					else
					{
						BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Repeated request: from [" +
								message.getSenderName() + "] meat received: " + meatSent );

						sendRefund( message.getSenderName(), "Sorry, this buff may only be requested " + maxPhilanthropy +
							(maxPhilanthropy == 1 ? " time" : " times") + " per day.", meatSent );

						if ( !saveList.contains( message ) )
							deleteList.add( message );

						return true;
					}
				}
				else if ( meatSent >= 100000 )
				{
					sendThankYou( message.getSenderName(), message.getMessageHTML() );
					if ( !saveList.contains( message ) )
						saveList.add( message );
					return true;
				}
				else
				{
					BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Meat received does not match anything in database: from [" +
						message.getSenderName() + "] meat received: " + meatSent );

					sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );

					if ( !saveList.contains( message ) )
						deleteList.add( message );

					return true;
				}
			}
		}
		catch( Exception e )
		{
			return false;
		}

		// Must not be a buff request message, so notify user and save/delete;
		// also check to see if it was an attempted scam

		detectScam( message, "You gain ([\\d,]+) Meat" );
		return true;
	}

	private static boolean executeBuff( BuffBotCaster buff, KoLMailMessage message, int meatSent )
	{
		double buffPercentRemaining = buff.castOnTarget( message.getSenderName() );
		int refundAmount = (int) ((double)meatSent * buffPercentRemaining);

		if ( refundAmount != 0 )
		{
			if ( client.permitsContinue() )
			{
				sendRefund( message.getSenderName(), "This buffbot has run out of the mana restoration items.  Please try again later.", refundAmount );

				if ( !saveList.contains( message ) )
					deleteList.add( message );

				return false;
			}
			else
			{
				// Can't send refund to ronin/hardcore, so don't attempt
				// to send the refund.

				if ( UseSkillRequest.lastUpdate.indexOf( "cannot receive" ) != -1 )
					refundAmount = 0;

				sendRefund( message.getSenderName(), "This buffbot was unable to process your request.  " +
					UseSkillRequest.lastUpdate + "  Please try again later.", refundAmount );

				if ( !saveList.contains( message ) )
					deleteList.add( message );

				return true;
			}
		}

		if ( buff.philanthropic )
			BuffBotHome.addToRecipientList( meatSent, message.getSenderName() );

		if ( !saveList.contains( message ) )
			deleteList.add( message );

		return true;
	}

	private static void detectScam( KoLMailMessage message, String scamString )
	{
		// For scam messages, leave the scam messages in
		// the player's inbox until further notice.

		Matcher scamMatcher = Pattern.compile( scamString ).matcher( message.getMessageHTML() );
		if ( scamMatcher.find() )
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Possible attempted scam message from [" + message.getSenderName() + "]" );

		// Now, mark for either save or delete the message,
		// or ignore the message, if applicable.

		else if ( messageDisposalSetting == SAVEBOX )
		{
			saveList.add( message );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Saving non-buff message from [" + message.getSenderName() + "]" );
		}
		else if ( messageDisposalSetting == DISPOSE )
		{
			deleteList.add( message );
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

	public static class BuffBotCaster
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

		public double castOnTarget( String target )
		{
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
