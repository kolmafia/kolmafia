/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.MailboxRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class BuffBotManager
{
	public static final int SAVEBOX = 0;
	public static final int DISPOSE = 1;

	private static final int REFUND_THRESHOLD = 4;

	private static int initialRestores = 0;
	private static boolean isInitializing = false;

	private static final ArrayList<KoLMailMessage> saveList = new ArrayList<KoLMailMessage>();
	private static final ArrayList<KoLMailMessage> deleteList = new ArrayList<KoLMailMessage>();
	private static final ArrayList<SendMailRequest> sendList = new ArrayList<SendMailRequest>();

	private static int messageDisposalSetting;
	private static String refundMessage;
	private static String thanksMessage;
	private static List<String> whiteList = new ArrayList<String>();

	private static final Map<Integer, Offering> buffCostMap = new TreeMap<Integer, Offering>();
	private static final SortedListModel<Offering> buffCostTable = new SortedListModel<Offering>();

	public static final Pattern MEAT_PATTERN =
		Pattern.compile( "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat" );
	public static final Pattern GIFT1_PATTERN =
		Pattern.compile( "<a class=nounder style='color: blue' href='showplayer.php\\?who=(\\d+)' target=mainpane>" );
	public static final Pattern GIFT2_PATTERN = Pattern.compile( "&gt;&gt;([^<]+)" );

	/**
	 * Resets the buffbot's internal variables and reloads the appropriate variables from memory.
	 */

	public static final void loadSettings()
	{
		BuffBotManager.isInitializing = true;

		MailManager.clearMailboxes();

		BuffBotManager.buffCostMap.clear();
		BuffBotManager.buffCostTable.clear();

		BuffBotManager.saveList.clear();
		BuffBotManager.deleteList.clear();
		BuffBotManager.sendList.clear();

		String[] currentBuff;
		BufferedReader reader =
			FileUtilities.getReader( new File( KoLConstants.BUFFBOT_LOCATION, KoLCharacter.baseUserName() + ".txt" ) );

		if ( reader == null )
		{
			BuffBotManager.isInitializing = false;
			BuffBotManager.saveBuffs();
			return;
		}

		// It's possible the person is starting from an older release
		// of KoLmafia.  If that's the case, reload the data from the
		// properties file, clear it out, and continue.

		while ( ( currentBuff = FileUtilities.readData( reader ) ) != null )
		{
			if ( currentBuff.length < 3 )
			{
				continue;
			}

			BuffBotManager.addBuff(
				SkillDatabase.getSkillName( StringUtilities.parseInt( currentBuff[ 0 ] ) ),
				StringUtilities.parseInt( currentBuff[ 1 ] ), StringUtilities.parseInt( currentBuff[ 2 ] ) );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		BuffBotManager.isInitializing = false;
	}

	/**
	 * Returns the table of costs for each buff managed by this buffbot.
	 */

	public static final LockableListModel<Offering> getBuffCostTable()
	{
		return BuffBotManager.buffCostTable;
	}

	/**
	 * An internal method which adds a buff to the list of available buffs. This also registers the buff inside of the
	 * list of available buffs.
	 */

	public static final void addBuff( final String skillName, final int price, final int castCount )
	{
		if ( price <= 0 || castCount <= 0 )
		{
			return;
		}

		Integer newPrice = IntegerPool.get( price );

		// Because the new concept allows multiple buffs
		// to have the same price, store things in a list.

		Offering castList = (Offering) BuffBotManager.buffCostMap.get( newPrice );

		// If this price has never existing before, go
		// ahead and add a new list to the data structure.

		if ( castList == null )
		{
			castList = new Offering( skillName, price, castCount );
			BuffBotManager.buffCostMap.put( newPrice, castList );
			BuffBotManager.buffCostTable.add( castList );
		}
		else
		{
			int skillId = SkillDatabase.getSkillId( skillName );
			int duration = Math.max( 5, SkillDatabase.getEffectDuration( skillId ) );

			castList.addBuff( skillName, castCount * duration );
			castList.updateFreeState();

			int index = BuffBotManager.buffCostTable.indexOf( castList );
			BuffBotManager.buffCostTable.fireContentsChanged( BuffBotManager.buffCostTable, index, index );
		}

		BuffBotManager.saveBuffs();
	}

	/**
	 * An internal method which removes the list of selected buffs from the current mappings.
	 */

	public static final void removeBuffs( final Object[] buffs )
	{
		Offering toRemove;
		boolean removedOne = false;

		for ( int i = 0; i < buffs.length; ++i )
		{
			if ( !BuffBotManager.buffCostTable.contains( buffs[ i ] ) )
			{
				continue;
			}

			removedOne = true;
			toRemove = (Offering) buffs[ i ];
			BuffBotManager.buffCostTable.remove( toRemove );
			BuffBotManager.buffCostMap.remove( IntegerPool.get( toRemove.getPrice() ) );
		}

		if ( removedOne )
		{
			BuffBotManager.saveBuffs();
		}
	}

	/**
	 * An internal method which saves the list of buffs into the user-specific settings file.
	 */

	private static final void saveBuffs()
	{
		if ( BuffBotManager.isInitializing )
		{
			return;
		}

		FileUtilities.downloadFile( "http://kolmafia.sourceforge.net/buffbot.xsl", new File(
			KoLConstants.BUFFBOT_LOCATION, "buffbot.xsl" ) );

		File datafile = new File( KoLConstants.BUFFBOT_LOCATION, KoLCharacter.baseUserName() + ".txt" );
		File xmlfile = new File( KoLConstants.BUFFBOT_LOCATION, KoLCharacter.baseUserName() + ".xml" );

		PrintStream settings = LogStream.openStream( datafile, true, "ISO-8859-1" );
		PrintStream document = LogStream.openStream( xmlfile, true, "ISO-8859-1" );

		document.println( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" );
		document.println( "<?xml-stylesheet type=\"text/xsl\" href=\"buffbot.xsl\"?>" );
		document.println();

		document.println( "<botdata>" );
		document.println( "<name>" + KoLCharacter.getUserName() + "</name>" );
		document.println( "<playerid>" + KoLCharacter.getUserId() + "</playerid>" );

		document.println( "<free-list>" );
		Offering currentCast;

		for ( int i = 0; i < BuffBotManager.buffCostTable.size(); ++i )
		{
			// First, append the buff to the setting string, then
			// print the buff to the XML tree.

			currentCast = (Offering) BuffBotManager.buffCostTable.get( i );
			settings.println( currentCast.toSettingString() );

			for ( int j = 0; j < currentCast.buffs.length; ++j )
			{
				document.println( "\t<buffdata>" );
				document.println( "\t\t<name>" + currentCast.buffs[ j ] + "</name>" );
				document.println( "\t\t<skillid>" + SkillDatabase.getSkillId( currentCast.buffs[ j ] ) + "</skillid>" );
				document.println( "\t\t<price>" + KoLConstants.COMMA_FORMAT.format( currentCast.price ) + "</price>" );
				document.println( "\t\t<turns>" + KoLConstants.COMMA_FORMAT.format( currentCast.turns[ j ] ) + "</turns>" );
				document.println( "\t\t<philanthropic>" + currentCast.free + "</philanthropic>" );
				document.println( "\t</buffdata>" );
			}
		}

		document.println( "</free-list>" );
		document.println( "<normal-list></normal-list>" );
		document.println( "</botdata>" );
		document.close();
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception (such as not enough MP to
	 * continue). On each pass, it gets all messages from the mailbox, then iterates on the mailbox.
	 */

	public static final void runBuffBot( final int iterations )
	{
		BuffBotHome.loadSettings();

		// Make sure that the buffbot is wearing the best
		// equipment they have available.

		UseSkillRequest.optimizeEquipment( 6003 );

		BuffBotHome.setBuffBotActive( true );
		BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot started." );

		BuffBotManager.messageDisposalSetting =
			StringUtilities.parseInt( Preferences.getString( "buffBotMessageDisposal" ) );
		BuffBotManager.whiteList = ClanManager.getWhiteList();

		BuffBotManager.refundMessage = Preferences.getString( "invalidBuffMessage" );
		BuffBotManager.thanksMessage = Preferences.getString( "thanksMessage" );
		BuffBotManager.initialRestores = Math.max( RecoveryManager.getRestoreCount(), 100 );

		String restoreItems = Preferences.getString( "mpAutoRecoveryItems" );

		PauseObject pauser = new PauseObject();
		boolean usingAdventures = restoreItems.indexOf( "rest" ) != -1;

		// The outer loop goes until user cancels, or
		// for however many iterations are needed.

		for ( int i = iterations; BuffBotHome.isBuffBotActive(); --i )
		{
			// If you run out of adventures and/or restores, then
			// check to see if you need to abort.

			if ( RecoveryManager.getRestoreCount() == 0 )
			{
				if ( !usingAdventures || KoLCharacter.getAdventuresLeft() == 0 )
				{
					if ( NPCStoreDatabase.contains( "magical mystery juice" ) )
					{
						AdventureResult restores =
							new AdventureResult( "magical mystery juice", BuffBotManager.initialRestores );
						BuffBotHome.setBuffBotActive( InventoryManager.retrieveItem( restores ) );
					}
					else
					{
						AdventureResult restores = new AdventureResult( "phonics down", BuffBotManager.initialRestores );
						BuffBotHome.setBuffBotActive( InventoryManager.retrieveItem( restores ) );
					}
				}
			}

			// If no abort happened due to lack of restores, then you
			// can proceed with the next iteration.

			BuffBotManager.runOnce();

			BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Message processing complete.  Buffbot is sleeping." );
			if ( BuffBotManager.initialRestores > 0 )
			{
				BuffBotHome.timeStampedLogEntry(
					BuffBotHome.NOCOLOR, "(" + RecoveryManager.getRestoreCount() + " mana restores remaining)" );
			}
			else if ( usingAdventures )
			{
				BuffBotHome.timeStampedLogEntry(
					BuffBotHome.NOCOLOR, "(" + KoLCharacter.getAdventuresLeft() + " adventures remaining)" );
			}

			if ( BuffBotHome.isBuffBotActive() )
			{
				BuffBotHome.setBuffBotActive( i > 1 );
			}

			if ( !BuffBotHome.isBuffBotActive() )
			{
				break;
			}

			// Sleep for a while and then try again (don't go
			// away for more than 1 second at a time to avoid
			// automatic re-enabling problems).

			for ( int j = 0; j < 60; ++j )
			{
				pauser.pause( 1000 );
			}
		}

		// After the buffbot is finished running, make sure
		// to reset the continue state.

		BuffBotHome.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot stopped." );
		BuffBotHome.setBuffBotActive( false );
	}

	public static final void runOnce()
	{
		MailManager.getMessages( "Inbox" ).clear();
		RequestThread.postRequest( new MailboxRequest( "Inbox" ) );

		while ( !BuffBotManager.deleteList.isEmpty() || !BuffBotManager.saveList.isEmpty() )
		{
			while ( !BuffBotManager.deleteList.isEmpty() )
			{
				Object[] messages = BuffBotManager.deleteList.toArray();
				BuffBotManager.deleteList.clear();
				MailManager.deleteMessages( "Inbox", messages );
			}

			if ( !BuffBotManager.saveList.isEmpty() )
			{
				Object[] messages = BuffBotManager.saveList.toArray();
				BuffBotManager.saveList.clear();
				MailManager.saveMessages( "Inbox", messages );
			}
		}
	}

	/**
	 * Queues the message to be sent later. Note that only one message can ever be queued. This ensures that thank-you
	 * messages do not result in refunds.
	 */

	private static final void queueOutgoingMessage( final String recipient, final String message,
		final AdventureResult result )
	{
		if ( BuffBotManager.sendList.isEmpty() )
		{
			BuffBotManager.sendList.add( new SendMailRequest( recipient, message, result ) );
		}
	}

	/**
	 * Queues an incoming message to be processed. This ensures that the message only appears on one list.
	 */

	public static final void queueIncomingMessage( final KoLMailMessage message, final boolean delete )
	{
		if ( !BuffBotManager.saveList.contains( message ) && !BuffBotManager.deleteList.contains( message ) )
		{
			if ( delete )
			{
				BuffBotManager.deleteList.add( message );
			}
			else
			{
				BuffBotManager.saveList.add( message );
			}
		}
	}

	/**
	 * Overrides/hides the message handling method in <code>MailManager</code>. Because this is a static final
	 * entity, this doesn't really matter, but it is convenient to have, from a style perspective.
	 */

	public static final KoLMailMessage addMessage( final String boxname, final String message )
	{
		KoLMailMessage success = MailManager.addMessage( boxname, message );

		if ( success == null || !BuffBotHome.isBuffBotActive() || !boxname.equals( "Inbox" ) )
		{
			return success;
		}

		try
		{
			BuffBotManager.processMessage( success );
			KoLmafia.forceContinue();
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

		if ( !BuffBotManager.sendList.isEmpty() )
		{
			SendMailRequest sending = (SendMailRequest) BuffBotManager.sendList.get( 0 );
			BuffBotHome.update(
				BuffBotHome.NOCOLOR,
				"Sending queued message to " + ContactManager.getPlayerName( sending.getRecipient() ) + "..." );
			RequestThread.postRequest( sending );

			BuffBotManager.sendList.clear();
		}

		return success;
	}

	/**
	 * Returns whether or not the given username exists on the current white list for restricted buffs.
	 */

	private static final boolean onWhiteList( final String userName )
	{
		return Collections.binarySearch( BuffBotManager.whiteList, userName.toLowerCase() ) > -1;
	}

	/**
	 * Sends a refund for the given amount to the given user with the appropriate reason attached.
	 */

	private static final void sendRefund( final String recipient, final String reason, final int amount )
	{
		if ( BuffBotManager.sendList.isEmpty() )
		{
			BuffBotManager.queueOutgoingMessage( recipient, reason, new AdventureResult( AdventureResult.MEAT, amount ) );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Queued refund message for [" + recipient + "]" );
		}
	}

	/**
	 * Checks to see if there's an attached donation by seeing if there's an image tag, with width of 30. Valentine
	 * images have width of 100 so we don't mark those as a false positive.
	 *
	 * @return <code>true</code> if there is a donation
	 */

	private static final boolean containsDonation( final KoLMailMessage message )
	{
		return message.getMessageHTML().indexOf( "You acquire" ) != -1;
	}

	/**
	 * Sends a thank you message to the given user, with the given message HTML quoted.
	 */

	private static final void sendThankYou( final String recipient, final String messageHTML )
	{
		if ( BuffBotManager.sendList.isEmpty() && !BuffBotManager.thanksMessage.equals( "" ) )
		{
			String reason =
				BuffBotManager.thanksMessage + KoLConstants.LINE_BREAK + KoLConstants.LINE_BREAK + ">" + messageHTML.replaceAll(
					"<.*?>", " " ).replaceAll( "[ ]+", " " );

			BuffBotManager.queueOutgoingMessage( recipient, reason, new AdventureResult( AdventureResult.MEAT, 0 ) );
			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Queued thank you for [" + recipient + "]" );
		}
	}

	private static final Offering extractRequest( final KoLMailMessage message, final int meatSent )
	{
		Offering castList = (Offering) BuffBotManager.buffCostMap.get( IntegerPool.get( meatSent ) );

		// If what is sent does not match anything in the buff table,
		// handle it.  Once it gets beyond this point, it is known to
		// be a valid buff request.

		if ( castList != null )
		{
			BuffBotManager.queueIncomingMessage( message, true );
			return castList;
		}

		if ( meatSent >= 100000 )
		{
			// If the amount of meat sent is extremely large,
			// and no buff matches that value, assume that it's
			// a donation and send a thank you note.

			BuffBotManager.sendThankYou( message.getSenderName(), message.getMessageHTML() );
			BuffBotManager.queueIncomingMessage( message, false );
			return null;
		}

		if ( meatSent != 0 )
		{
			// If the cast list is empty, and the meat sent was
			// not a donation, then the user is not receiving
			// any buffs.  Therefore, reset the variable.

			BuffBotManager.queueIncomingMessage( message, true );
			BuffBotHome.update(
				BuffBotHome.NONBUFFCOLOR,
				"Invalid amount (" + meatSent + " meat) received from " + message.getSenderName() );
			BuffBotManager.sendRefund(
				message.getSenderName(),
				KoLConstants.COMMA_FORMAT.format( meatSent ) + " meat is not a valid buff price.  " + BuffBotManager.refundMessage,
				meatSent );
			return null;
		}

		// If it gets this far, then it's an empty message.
		// Based on the user's settings, do something with
		// the message (save, delete, etc.)

		switch ( BuffBotManager.messageDisposalSetting )
		{
		case SAVEBOX:

			String messageText = message.getMessageHTML().replaceAll( "<.*?>", "" );
			boolean willDelete = messageText.length() < 10;

			BuffBotManager.queueIncomingMessage( message, willDelete );

			if ( willDelete )
			{
				BuffBotHome.update(
					BuffBotHome.NONBUFFCOLOR, "Deleting non-buff message from [" + message.getSenderName() + "]" );
			}
			else
			{
				BuffBotHome.update(
					BuffBotHome.NONBUFFCOLOR, "Saving non-buff message from [" + message.getSenderName() + "]" );
			}

			return null;

		case DISPOSE:

			BuffBotManager.queueIncomingMessage( message, true );
			BuffBotHome.update(
				BuffBotHome.NONBUFFCOLOR, "Deleting non-buff message from [" + message.getSenderName() + "]" );
			return null;

		default:

			BuffBotHome.update(
				BuffBotHome.NONBUFFCOLOR, "Ignoring non-buff message from [" + message.getSenderName() + "]" );
			return null;
		}
	}

	/**
	 * Utility method which processes the message that was received. This parses out any applicable buffs and sends any
	 * applicable thank you messages.
	 */

	private static final void processMessage( final KoLMailMessage message )
		throws Exception
	{
		// Now that you're guaranteed to be above the threshold,
		// go ahead and process the message.

		BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Received message from [" + message.getSenderName() + "]" );

		if ( BuffBotManager.containsDonation( message ) )
		{
			BuffBotManager.sendThankYou( message.getSenderName(), message.getMessageHTML() );
			BuffBotManager.queueIncomingMessage( message, false );
			return;
		}

		Matcher meatMatcher = BuffBotManager.MEAT_PATTERN.matcher( message.getMessageHTML() );
		int meatSent = meatMatcher.find() ? StringUtilities.parseInt( meatMatcher.group( 1 ) ) : 0;
		Offering castList = BuffBotManager.extractRequest( message, meatSent );

		if ( castList == null )
		{
			return;
		}

		// Ensure that every buff is handled.  In the event
		// that a buff was partially completed, pretend that
		// all buffs were completed.

		String requestor = message.getSenderName();
		String recipient = requestor;

		// If it's clear that the person is receiving a refund because they are
		// restricted due to failure, ignore this request.

		if ( !BuffBotHome.isPermitted( requestor ) )
		{
			return;
		}

		boolean isGiftBuff = false;

		Matcher giftMatcher = BuffBotManager.GIFT1_PATTERN.matcher( message.getMessageHTML() );
		if ( giftMatcher.find() )
		{
			isGiftBuff = true;
			recipient = giftMatcher.group( 1 );
		}
		else
		{
			giftMatcher = BuffBotManager.GIFT2_PATTERN.matcher( message.getMessageHTML() );
			if ( giftMatcher.find() )
			{
				isGiftBuff = true;
				recipient = giftMatcher.group( 1 ).trim();
			}
		}

		if ( isGiftBuff && castList.free )
		{
			return;
		}

		if ( BuffBotManager.executeBuff( castList, recipient, meatSent ) )
		{
			return;
		}

		int failureCount = BuffBotHome.getInstanceCount( 0, requestor ) + 1;
		BuffBotHome.addToRecipientList( 0, requestor );

		if ( UseSkillRequest.lastUpdate.startsWith( "Selected target cannot receive" ) )
		{
			BuffBotHome.denyFutureBuffs( requestor );
		}

		// Record the inability to buff inside of a separate
		// file which stores how many refunds were sent that day.

		if ( failureCount == BuffBotManager.REFUND_THRESHOLD + 1 )
		{
			BuffBotManager.sendRefund(
				requestor,
				"This message is to provide notification that you have already sent " + BuffBotManager.REFUND_THRESHOLD + " " + "buff requests which resulted in a refund to your account.  In order to preserve the integrity of this buffbot, from now until the next rollover begins, " + "all requests for once-per-day buffs and all buffs which which might result in a refund will instead be treated as donations.",
				0 );
		}
		else if ( failureCount < BuffBotManager.REFUND_THRESHOLD )
		{
			if ( !BuffBotHome.isPermitted( requestor ) )
			{
				BuffBotManager.sendRefund(
					requestor,
					"It has been determined that at some point during an attempt to buff you in the last 24 hours, you could not receive buffs.  " + "This could be either due to engaging in combat, having too many AT songs in your head, or ascending before receiving your buff.  As a result of this failure, " + "all of your requests are being refunded rather than processed in order to maintain throughput.  Apologies for the inconvenience.",
					meatSent );
			}
			else
			{
				BuffBotManager.sendRefund(
					requestor,
					"This buffbot was unable to process your request.  " + UseSkillRequest.lastUpdate + "  Please try again later." + KoLConstants.LINE_BREAK + KoLConstants.LINE_BREAK + BuffBotManager.refundMessage,
					meatSent );
			}
		}
	}

	private static final boolean executeBuff( final Offering buff, final String recipient, final int meatSent )
	{
		// If it's not a philanthropic buff, process the buff as
		// normal (no need to slow down to verify).

		if ( !buff.free )
		{
			return buff.castOnTarget( recipient );
		}

		// If it's not a philanthropic buff request, then go ahead
		// and check to see that it's okay to send it.

		switch ( Preferences.getInteger( "buffBotPhilanthropyType" ) )
		{

		case 0:

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Philanthropic buff request from " + recipient );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast #" + meatSent + " on " + recipient );
			UseSkillRequest.lastUpdate = "Philanthropic buffs temporarily disabled.";
			return false;

		case 1:

			int instanceCount = BuffBotHome.getInstanceCount( meatSent, recipient );
			if ( instanceCount == 0 || buff.casts.length == 1 && buff.getLowestBuffId() == SkillPool.ODE_TO_BOOZE && instanceCount == 1 )
			{
				break;
			}

			// This is a philanthropic buff and the user has already
			// requested it the maximum number of times alotted.  The
			// user will not be buffed.

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Philanthropy limit exceeded for " + recipient );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast #" + meatSent + " on " + recipient );

			UseSkillRequest.lastUpdate = "Philanthropy limit exceeded.";
			return false;

		case 2:

			if ( BuffBotManager.onWhiteList( recipient ) )
			{
				break;
			}

			// This is a restricted buff for a non-allowed user.
			// The user will not be buffed.

			BuffBotHome.update( BuffBotHome.NONBUFFCOLOR, "Received white list request from un-whitelisted player" );
			BuffBotHome.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast #" + meatSent + " on " + recipient );
			UseSkillRequest.lastUpdate = "Philanthropic buffs temporarily disabled.";
			return false;
		}

		// Under all other circumstances, you go ahead and
		// process the buff request.

		BuffBotHome.addToRecipientList( meatSent, recipient );
		return buff.castOnTarget( recipient );
	}

	public static class Offering
		implements Comparable<Offering>
	{
		private final String botName;
		private final int price;
		private boolean free;

		private int lowestBuffId;
		public String[] buffs;
		public int[] casts;
		public int[] turns;

		private boolean changed;
		private boolean useFullStrings;

		private final StringBuffer settingString;
		private final StringBuffer stringForm;

		public Offering( final String buffName, final int price, final int casts )
		{
			this( buffName, KoLCharacter.getUserName(), price, 0, false );
			int buffId = SkillDatabase.getSkillId( buffName );

			this.casts[ 0 ] = casts;
			this.turns[ 0 ] = casts * Math.max( 5, SkillDatabase.getEffectDuration( buffId ) );
			this.updateFreeState();

			this.useFullStrings = true;
			this.changed = true;
		}

		public void updateFreeState()
		{
			int totalCost = 0;

			for ( int i = 0; i < this.casts.length; ++i )
			{
				totalCost +=
					this.casts[ i ] * SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( this.buffs[ i ] ) );
			}

			this.free = this.price <= totalCost * 95 / MPRestoreItemList.getManaRestored( "magical mystery juice" );
		}

		public Offering( final String buffName, final String botName, final int price, final int turns,
			final boolean free )
		{
			this.buffs = new String[] { buffName };
			this.casts = new int[] { turns / 10 };
			this.turns = new int[] { turns };
			this.lowestBuffId = SkillDatabase.getSkillId( buffName );

			this.botName = botName;
			this.price = price;
			this.free = free;

			this.settingString = new StringBuffer();
			this.stringForm = new StringBuffer();

			this.useFullStrings = false;
			this.changed = true;
		}

		public String getBotName()
		{
			return this.botName;
		}

		public int getPrice()
		{
			return this.price;
		}

		public int[] getTurns()
		{
			return this.turns;
		}

		public int getLowestBuffId()
		{
			return this.lowestBuffId;
		}

		@Override
		public String toString()
		{
			if ( this.changed )
			{
				this.constructStringForm();
				this.changed = false;
			}

			return this.stringForm.toString();
		}

		public String toSettingString()
		{
			if ( this.changed )
			{
				this.constructStringForm();
				this.changed = false;
			}

			return this.settingString.toString();
		}

		public void addBuff( final String buffName, final int turns )
		{
			String[] tempNames = new String[ this.buffs.length + 1 ];
			int[] tempCasts = new int[ this.casts.length + 1 ];
			int[] tempTurns = new int[ this.turns.length + 1 ];

			System.arraycopy( this.buffs, 0, tempNames, 0, this.buffs.length );
			System.arraycopy( this.casts, 0, tempCasts, 0, this.buffs.length );
			System.arraycopy( this.turns, 0, tempTurns, 0, this.buffs.length );

			this.buffs = tempNames;
			this.casts = tempCasts;
			this.turns = tempTurns;

			int skillId = SkillDatabase.getSkillId( buffName );
			int duration = Math.max( 5, SkillDatabase.getEffectDuration( skillId ) );

			this.buffs[ this.buffs.length - 1 ] = buffName;
			this.casts[ this.casts.length - 1 ] = turns / duration;
			this.turns[ this.turns.length - 1 ] = turns;

			if ( skillId < this.lowestBuffId )
			{
				this.lowestBuffId = skillId;
			}

			this.changed = true;
		}

		public boolean castOnTarget( final String target )
		{
			for ( int i = 0; i < this.buffs.length; ++i )
			{
				BuffBotHome.recordBuff( target, this.buffs[ i ], this.casts[ i ], this.price );

				// Figure out how much MP the buff will take, and then identify
				// the number of casts per request that this character can handle.

				BuffBotHome.update(
					BuffBotHome.BUFFCOLOR,
					"Casting " + this.buffs[ i ] + ", " + this.casts[ i ] + " times on " + target + " for " + this.price + " meat... " );

				RequestThread.postRequest( UseSkillRequest.getInstance( this.buffs[ i ], target, this.casts[ i ] ) );

				if ( UseSkillRequest.lastUpdate.equals( "" ) )
				{
					BuffBotHome.update(
						BuffBotHome.BUFFCOLOR, " ---> Successfully cast " + this.buffs[ i ] + " on " + target );
				}
				else
				{
					BuffBotHome.update(
						BuffBotHome.ERRORCOLOR, " ---> Could not cast " + this.buffs[ i ] + " on " + target );
					return i != 0;
				}
			}

			return true;
		}

		private void constructStringForm()
		{
			this.stringForm.setLength( 0 );

			this.stringForm.append( KoLConstants.COMMA_FORMAT.format( this.price ) );
			this.stringForm.append( " meat for " );

			if ( this.turns.length == 1 )
			{
				this.stringForm.append( KoLConstants.COMMA_FORMAT.format( this.turns[ 0 ] ) );
				this.stringForm.append( " turns" );

				if ( this.useFullStrings )
				{
					this.stringForm.append( " of " );
					this.stringForm.append( this.buffs[ 0 ] );

					if ( this.free )
					{
						this.stringForm.append( " (once per day)" );
					}
				}
			}
			else
			{
				this.stringForm.insert( 0, "<html>" );

				this.stringForm.append( "a" );

				if ( this.useFullStrings && this.free )
				{
					this.stringForm.append( " Once-Per-Day" );
				}

				this.stringForm.append( " Buff Pack which includes:" );

				for ( int i = 0; i < this.buffs.length; ++i )
				{
					this.stringForm.append( "<br> - " );
					this.stringForm.append( KoLConstants.COMMA_FORMAT.format( this.turns[ i ] ) );
					this.stringForm.append( " turns of " );
					this.stringForm.append( this.buffs[ i ] );
				}

				this.stringForm.append( "</html>" );
			}


			this.settingString.setLength( 0 );

			for ( int i = 0; i < this.buffs.length; ++i )
			{
				this.settingString.append( SkillDatabase.getSkillId( this.buffs[ i ] ) );
				this.settingString.append( '\t' );
				this.settingString.append( this.price );
				this.settingString.append( '\t' );
				this.settingString.append( this.casts[ i ] );
				this.settingString.append( KoLConstants.LINE_BREAK );
			}

		}

		@Override
		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof Offering ) )
			{
				return false;
			}

			Offering off = (Offering) o;
			return this.botName.equalsIgnoreCase( off.botName ) && this.price == off.price && this.turns == off.turns && this.free == off.free;
		}

		@Override
		public int hashCode()
		{
			int hash = 0;
			hash = this.botName != null ? this.botName.hashCode() : 0;
			hash = 31 * hash + this.price;
			hash = 31 * hash + (this.free ? 1 : 0);
			hash = 31 * hash + Arrays.hashCode( this.turns );
			return hash;
		}

		public SendMailRequest toRequest()
		{
			return new SendMailRequest( this.botName, KoLConstants.DEFAULT_KMAIL, new AdventureResult(
				AdventureResult.MEAT, this.price ) );
		}

		public int compareTo( final Offering o )
		{
			if ( o == null || !( o instanceof Offering ) )
			{
				return -1;
			}

			Offering off = (Offering) o;

			// First, buffpacks should come before standard offerings
			if ( ( this.turns.length == 1 || off.turns.length == 1 ) && this.turns.length != off.turns.length )
			{
				return off.turns.length - this.turns.length;
			}

			// If a buffpack, compare price
			if ( this.turns.length > 1 && off.turns.length > 1 )
			{
				return this.price - off.price;
			}

			// Compare the Id of the lowest Id buffs
			if ( this.lowestBuffId != off.lowestBuffId )
			{
				return this.lowestBuffId - off.lowestBuffId;
			}

			// Next compare turns
			if ( this.turns[ 0 ] != off.turns[ 0 ] )
			{
				return this.turns[ 0 ] - off.turns[ 0 ];
			}

			// Next compare price
			if ( this.price != off.price )
			{
				return this.price - off.price;
			}

			// Then, compare the names of the bots
			return this.botName.compareToIgnoreCase( off.botName );
		}
	}
}
