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
import java.io.FileOutputStream;
import java.io.PrintStream;

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

	private static ArrayList saveList = new ArrayList();
	private static ArrayList deleteList = new ArrayList();
	private static ArrayList sendList = new ArrayList();

	private static int messageDisposalSetting;
	private static String refundMessage;
	private static String thanksMessage;

	private static Map buffCostMap = new TreeMap();
	private static SortedListModel buffCostTable = new SortedListModel();
	private static String [] whiteListArray = new String[0];

	public static final String MEAT_REGEX = "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat";

	/**
	 * Resets the buffbot's internal variables and reloads the
	 * appropriate variables from memory.
	 */

	public static void reset()
	{
		KoLMailManager.reset();

		buffCostMap.clear();
		buffCostTable.clear();

		saveList.clear();
		deleteList.clear();
		sendList.clear();

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
	 * Returns the table of costs for each buff managed by
	 * this buffbot.
	 */

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

		// Do not allow the same buff type to appear on
		// the list more than once.

		int currentIndex = castList.indexOf( newCast );
		if ( currentIndex != -1 )
			buffCostTable.remove( castList.remove( currentIndex ) );

		castList.add( newCast );
		buffCostTable.add( newCast );

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

	/**
	 * An internal method which saves the list of buffs into the
	 * user-specific settings file.
	 */

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

		TreeMap buffNameMap = new TreeMap();
		StringBuffer currentString;

		for ( int i = 0; i < buffCostTable.size(); ++i )
		{
			currentCast = (BuffBotCaster) buffCostTable.get(i);
			if ( !buffNameMap.containsKey( currentCast.getBuffName() ) )
				buffNameMap.put( currentCast.getBuffName(), new StringBuffer() );

			currentString = (StringBuffer) buffNameMap.get( currentCast.getBuffName() );

			currentString.append( currentCast.getTurnCount() );
			currentString.append( '-' );
			currentString.append( currentCast.getPrice() );
			
			if ( currentCast.philanthropic )
				currentString.append( '*' );
			
			currentString.append( LINE_BREAK );
		}
		
		try
		{
			PrintStream buffList = new PrintStream( new FileOutputStream( new File( "buffs/" + KoLCharacter.getUsername() + "_KoLmafiaReadableDisplayCaseText.txt" ) ) );
			Object [] keys = buffNameMap.keySet().toArray();
			
			for ( int i = 0; i < keys.length; ++i )
			{
				buffList.println( keys[i] );
				buffList.println( buffNameMap.get( keys[i] ) );
			}
			
			buffList.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public static void runBuffBot( int iterations )
	{
		BuffBotHome.setBuffBotActive( true );
		DEFAULT_SHELL.updateDisplay( "Buffbot started." );
		BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Starting new session" );
		messageDisposalSetting = Integer.parseInt( getProperty( "buffBotMessageDisposal" ) );

		String whiteListString = getProperty( "whiteList" ).toLowerCase();
		if ( whiteListString.indexOf( "$clan" ) != -1 )
			whiteListString = whiteListString.replaceFirst( "\\$clan", ClanManager.retrieveClanListAsCDL() );

		whiteListArray = whiteListString.split( "\\s*,\\s*" );
		Arrays.sort( whiteListArray );

		refundMessage = client.getSettings().getProperty( "invalidBuffMessage" );
		thanksMessage = client.getSettings().getProperty( "thanksMessage" );

		// The outer loop goes until user cancels, or
		// for however many iterations are needed.

		for ( int i = iterations; BuffBotHome.isBuffBotActive() && i > 0; --i )
		{
			BuffBotManager.runOnce();

			BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Message processing complete.  Buffbot is sleeping." );
			BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + client.getRestoreCount() + " mana restores remaining)" );
			DEFAULT_SHELL.updateDisplay( "Buffbot is sleeping." );

			// Sleep for a while and then try again (don't go
			// away for more than 1 second at a time to avoid
			// automatic re-enabling problems).

			for ( int j = 0; i != 1 && j < 60; ++j )
				if ( BuffBotHome.isBuffBotActive() )
					KoLRequest.delay( 1000 );
		}

		// After the buffbot is finished running, make sure
		// to reset the continue state.

		BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot stopped." );
		DEFAULT_SHELL.updateDisplay( "Buffbot stopped." );
		BuffBotHome.setBuffBotActive( false );
	}

	public static void runOnce()
	{
		getMessages( "Inbox" ).clear();
		(new MailboxRequest( client, "Inbox" )).run();

		while ( !deleteList.isEmpty() || !saveList.isEmpty() )
		{
			while ( !deleteList.isEmpty() )
			{
				Object [] messages = deleteList.toArray();
				deleteList.clear();

				deleteMessages( "Inbox", messages );
			}

			if ( !saveList.isEmpty() )
			{
				Object [] messages = saveList.toArray();
				saveList.clear();

				saveMessages( messages );
			}
		}
	}

	/**
	 * Queues the message to be sent later.  Note that only one message
	 * can ever be queued.  This ensures that thank-you messages do not
	 * result in refunds.
	 */

	private static void queueOutgoingMessage( String recipient, String message, AdventureResult result )
	{
		if ( sendList.isEmpty() )
			sendList.add( new GreenMessageRequest( client, recipient, message, result ) );
	}

	/**
	 * Queues an incoming message to be processed.  This ensures that the
	 * message only appears on one list.
	 */

	protected static void queueIncomingMessage( KoLMailMessage message, boolean delete )
	{
		if ( !saveList.contains( message ) && !deleteList.contains( message ) )
		{
			if ( delete )
				deleteList.add( message );
			else
				saveList.add( message );
		}
	}

	/**
	 * Overrides/hides the message handling method in <code>KoLMailManager</code>.
	 * Because this is a static entity, this doesn't really matter, but it is
	 * convenient to have, from a style perspective.
	 */

	public static KoLMailMessage addMessage( String boxname, String message )
	{
		KoLMailMessage success = KoLMailManager.addMessage( boxname, message );

		if ( success == null || !BuffBotHome.isBuffBotActive() || !boxname.equals( "Inbox" ) )
			return success;

		try
		{
			processMessage( success );
			client.forceContinue();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
			return success;
		}

		// Abort the buffbot only when you run out of MP
		// restores -- otherwise, it's always okay to
		// continue using the buffbot.

		if ( client.getRestoreCount() == 0 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Unable to continue BuffBot!" );
			BuffBotHome.setBuffBotActive( false );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Unable to process a buff message." );
		}

		if ( !sendList.isEmpty() )
		{
			GreenMessageRequest sending = (GreenMessageRequest) sendList.get(0);
			BuffBotHome.update( BuffBotHome.NOCOLOR, "Sending queued message to " + KoLmafia.getPlayerName( sending.getRecipient() ) + "..." );
			sending.run();

			sendList.clear();
		}

		return success;
	}

	/**
	 * Returns whether or not the given username exists on the
	 * current white list for restricted buffs.
	 */

	private static boolean onWhiteList( String userName )
	{	return Arrays.binarySearch( whiteListArray, userName.toLowerCase() ) > -1;
	}

	/**
	 * Sends a refund for the given amount to the given user
	 * with the appropriate reason attached.
	 */

	private static void sendRefund( String recipient, String reason, int amount )
	{
		if ( sendList.isEmpty() )
		{
			queueOutgoingMessage( recipient, reason, new AdventureResult( AdventureResult.MEAT, amount ) );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Queued refund message for [" + recipient + "]" );
		}
	}

	/**
	 * Checks to see if there's an attached donation  by seeing if there's
	 * an image tag, with width of 30.  Valentine images have width of 100
	 * so we don't mark those as a false positive.
	 *
	 * @return	<code>true</code> if there is a donation
	 */

	private static boolean containsDonation( KoLMailMessage message )
	{    return message.getMessageHTML().replaceAll( MEAT_REGEX, "" ).indexOf( "width=30" ) != -1;
	}

	/**
	 * Sends a thank you message to the given user, with the given message
	 * HTML quoted.
	 */

	private static void sendThankYou( String recipient, String messageHTML )
	{
		if ( sendList.isEmpty() && !thanksMessage.equals( "" ) )
		{
			String reason = thanksMessage + LINE_BREAK + LINE_BREAK +
				">" + messageHTML.replaceAll( "<.*?>", " " ).replaceAll( "[ ]+", " " );

			queueOutgoingMessage( recipient, reason, new AdventureResult( AdventureResult.MEAT, 0 ) );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Queued thank you for [" + recipient + "]" );
		}
	}

	/**
	 * Utility method which processes the message that was
	 * received.  This parses out any applicable buffs and
	 * sends any applicable thank you messages.
	 */

	private static void processMessage( KoLMailMessage message ) throws Exception
	{
		// Now that you're guaranteed to be above the threshold,
		// go ahead and process the message.

		BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Received message from [" + message.getSenderName() + "]" );

		if ( containsDonation( message ) )
		{
			sendThankYou( message.getSenderName(), message.getMessageHTML() );
			queueIncomingMessage( message, false );
			return;
		}

		Matcher meatMatcher = Pattern.compile( MEAT_REGEX ).matcher( message.getMessageHTML() );
		int meatSent = meatMatcher.find() ? df.parse( meatMatcher.group(1) ).intValue() : 0;
		List castList = (List) buffCostMap.get( new Integer( meatSent ) );

		// If what is sent does not match anything in the buff table,
		// handle it.  Once it gets beyond this point, it is known to
		// be a valid buff request.

		if ( castList == null || castList.isEmpty() )
		{
			if ( meatSent >= 100000 )
			{
				// If the amount of meat sent is extremely large,
				// and no buff matches that value, assume that it's
				// a donation and send a thank you note.

				sendThankYou( message.getSenderName(), message.getMessageHTML() );
				queueIncomingMessage( message, false );
				return;
			}
			else if ( meatSent != 0 )
			{
				// If the cast list is empty, and the meat sent was
				// not a donation, then the user is not receiving
				// any buffs.  Therefore, reset the variable.

				queueIncomingMessage( message, true );
				BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Invalid amount (" + meatSent + " meat) received from " + message.getSenderName() );
				sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price.  " + refundMessage, meatSent );
				return;
			}

			// If it gets this far, then it's an empty message.
			// Based on the user's settings, do something with
			// the message (save, delete, etc.)

			switch ( messageDisposalSetting )
			{
				case SAVEBOX:
					queueIncomingMessage( message, message.getMessageHTML().trim().length() == 0 );
					BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Saving non-buff message from [" + message.getSenderName() + "]" );
					break;

				case DISPOSE:
					queueIncomingMessage( message, true );
					BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Deleting non-buff message from [" + message.getSenderName() + "]" );
					break;

				default:
					BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Ignoring non-buff message from [" + message.getSenderName() + "]" );
			}

			return;
		}

		// Ensure that every buff is handled.  In the event
		// that a buff was partially completed, pretend that
		// all buffs were completed.

		queueIncomingMessage( message, true );

		BuffBotCaster currentBuff;
		boolean receivedBuffs = false;
		boolean gavePhilanthropicBuff = false;

		for ( int i = 0; client.permitsContinue() && i < castList.size(); ++i )
		{
			currentBuff = (BuffBotCaster) castList.get(i);
			receivedBuffs |= executeBuff( currentBuff, message, meatSent );
			gavePhilanthropicBuff |= currentBuff.philanthropic;
		}

		if ( receivedBuffs && gavePhilanthropicBuff )
			BuffBotHome.addToRecipientList( meatSent, message.getSenderName() );

		if ( !receivedBuffs )
		{
			// Record the inability to buff inside of a separate
			// file which stores how many refunds were sent that day.

			BuffBotHome.addToRecipientList( 0, message.getSenderName() );
			int failureCount = BuffBotHome.getInstanceCount( 0, message.getSenderName() );

			if ( failureCount == 9 )
			{
				// Nine refunds in a single day is pretty bad.  So,
				// send a notification that they will no longer be
				// refunded for buffs cast today.

				sendRefund( message.getSenderName(), "This is a message to notify you that you have sent 9 requests which were refunded.  " +
					"To prevent the possibility of intentional sabatoge, this will be the last refund you will receive today.  Thanks for understanding.", meatSent );
			}
			else if ( failureCount < 9 )
			{
				// If the person sent something and received no buffs,
				// then make sure they're refunded.

				sendRefund( message.getSenderName(), "This buffbot was unable to process your request.  " + UseSkillRequest.lastUpdate +
					"  Please try again later." + LINE_BREAK + LINE_BREAK + refundMessage, meatSent );
			}
		}
	}

	private static boolean executeBuff( BuffBotCaster buff, KoLMailMessage message, int meatSent )
	{
		if ( buff.restricted && !onWhiteList( message.getSenderName() ) )
		{
			// This is a restricted buff for a non-allowed user.
			// The user will not be buffed.

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Received white list request from un-whitelisted player" );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast " + buff.getBuffName() + " on " + message.getSenderName() );
			UseSkillRequest.lastUpdate = df.format( meatSent ) + " meat is not a valid buff price.";
			return false;
		}
		else if ( buff.philanthropic && BuffBotHome.getInstanceCount( meatSent, message.getSenderName() ) > 0 )
		{
			// This is a philanthropic buff and the user has already
			// requested it the maximum number of times alotted.  The
			// user will not be buffed.

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Philanthropy limit exceeded for " + message.getSenderName() );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast " + buff.getBuffName() + " on " + message.getSenderName() );
			UseSkillRequest.lastUpdate = "This buff may only be requested once per day.";
			return false;
		}

		// Under all other circumstances, you go ahead and
		// process the buff request.

		return buff.castOnTarget( message.getSenderName() );
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
		private int turnCount;

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
			this.turnCount = buffID > 6000 ? castCount * 15 : castCount * 10;

			this.restricted = restricted;
			this.philanthropic = philanthropic;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "Cast " );
			stringForm.append( this.buffName );
			stringForm.append( ' ' );
			stringForm.append( castCount );
			stringForm.append( " time" );

			if ( castCount != 1 )
				stringForm.append( "s (" );
			
			stringForm.append( turnCount );
			stringForm.append( " turns) for " );
			stringForm.append( price );
			stringForm.append( " meat" );

			if ( restricted )
				stringForm.append( " (white list only)" );
			if ( philanthropic )
				stringForm.append( " (philanthropic)" );

			this.stringForm = stringForm.toString();
			this.settingString = buffID + ":" + price + ":" + castCount + ":" + restricted + ":" + philanthropic;
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof BuffBotCaster && buffID == ((BuffBotCaster)o).buffID;
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

		public boolean castOnTarget( String target )
		{
			++requestsThisSession;
			BuffBotHome.recordBuff( target, buffName, castCount, price );

			// Figure out how much MP the buff will take, and then identify
			// the number of casts per request that this character can handle.

			BuffBotHome.update( BuffBotHome.BUFFCOLOR, "Casting " + buffName + ", " + castCount + " times on " +
				target + " for " + price + " meat... " );

			(new UseSkillRequest( client, buffName, target, castCount )).run();

			if ( client.permitsContinue() )
			{
				BuffBotHome.update( BuffBotHome.BUFFCOLOR, " ---> Successfully cast " + buffName + " on " + target );
				return true;
			}
			else
			{
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast " + buffName + " on " + target );
				return false;
			}
		}

		public String getBuffName()
		{	return buffName;
		}

		public int getPrice()
		{	return price;
		}

		public int getTurnCount()
		{	return turnCount;
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
