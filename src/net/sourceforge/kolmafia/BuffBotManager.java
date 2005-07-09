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
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Arrays;

/**
 * Container class for <code>BuffBotManager</code>
 * Provides all aspects of BuffBot execution.
 */

public class BuffBotManager extends KoLMailManager implements KoLConstants
{
	public static final int SAVEBOX = 0;
	public static final int DISPOSE = 1;
	public static final int INBOX = 2;

	private KoLmafia client;
	private KoLCharacter characterData;
	private List inventory;
	private KoLSettings settings;

	protected ArrayList saveList;
	protected ArrayList deleteList;

	private String mpRestoreSetting;
	private int messageDisposalSetting;
	private boolean useChatBasedBuffBot;
	private BuffBotHome buffbotLog;
	private String refundMessage;
	private String thanksMessage;

	private static final int SLEEP_TIME = 1000;       // Sleep this much each time
	private static final int SHORT_SLEEP_COUNT = 75;  // This many times
	private static final int LONG_SLEEP_COUNT = 300;  // This many times for slot needs

	private Map buffCostMap;
	private int maxPhilanthropy;
	private int autoBuySetting;
	private MPRestoreItemList mpRestoreItemList;
	private LockableListModel buffCostTable;
	private String [] whiteListArray;

	public static final String MEAT_REGEX = "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat";

	/**
	 * Constructor for the <code>BuffBotManager</code> class.
	 */

	public BuffBotManager( KoLmafia client, LockableListModel buffCostTable )
	{
		super( client );
		this.client = client;

		this.buffCostMap = new TreeMap();
		this.buffCostTable = buffCostTable;

		this.settings = (client == null) ? new KoLSettings() : client.getSettings();
		this.characterData =  client.getCharacterData();
		this.mpRestoreItemList = new MPRestoreItemList();
		buffbotLog = client.getBuffBotLog();

		saveList = new ArrayList();
		deleteList = new ArrayList();

		String [] soldBuffs = settings.getProperty( "buffBotCasting" ).split( ";" );
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

	public void addBuff( String skillName, int price, int castCount, boolean restricted, boolean philanthropic )
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

	public void removeBuffs( Object [] buffs )
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

	private void saveBuffs()
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

		settings.setProperty( "buffBotCasting", sellerSetting.toString() );
		settings.saveSettings();
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public synchronized void runBuffBot( int iterations )
	{
		boolean newMessages = false;
		client.setBuffBotActive( true );
		client.updateDisplay( DISABLED_STATE, "Buffbot started." );
		buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Starting new session" );

		this.settings = (client == null) ? new KoLSettings() : client.getSettings();
		this.characterData =  client.getCharacterData();
		this.inventory = client == null ? new LockableListModel() : client.getInventory();

		maxPhilanthropy = Integer.parseInt( settings.getProperty( "maxPhilanthropy" ) );
		mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		useChatBasedBuffBot = settings.getProperty( "useChatBasedBuffBot" ).equals( "true" );
		messageDisposalSetting = Integer.parseInt( settings.getProperty( "buffBotMessageDisposal" ) );

		if ( useChatBasedBuffBot )
		{
			if ( messageDisposalSetting == INBOX )
				messageDisposalSetting = SAVEBOX;

			iterations = 1;
			client.initializeChat();
		}

		String whiteListString = settings.getProperty( "whiteList" ).toLowerCase();
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

		int sleepCount = messageDisposalSetting == INBOX ? LONG_SLEEP_COUNT : SHORT_SLEEP_COUNT;

		for ( int i = 1; client.isBuffBotActive() && i <= iterations; ++i )
		{
			// Request the inbox for the user.  Each call
			// to add message will trigger the actual
			// buffing attempt sequence, so all that
			// needs to be done is clear the lists and
			// initiate the mailbox request.

			newMessages = false;
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
					buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Message processing complete." );
					buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot is sleeping." );
					buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + getRestoreCount() + " mana restores remaining)" );
				}

				client.updateDisplay( DISABLED_STATE, "BuffBot is sleeping" );

				for ( int j = 0; j < sleepCount; ++j )
					if ( client.isBuffBotActive() )
						KoLRequest.delay( SLEEP_TIME );
			}
			else if ( newMessages )
				buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "(" + getRestoreCount() + " mana restores remaining)" );
		}

		if ( !useChatBasedBuffBot )
		{
			buffbotLog.timeStampedLogEntry( BuffBotHome.NOCOLOR, "Buffbot stopped." );
			client.updateDisplay( ENABLED_STATE, "Buffbot stopped." );
			client.setBuffBotActive( false );
		}
	}

	private void sendEmptyMessageNotice( KoLMailMessage message )
	{
		buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Sending empty message notice to [" + message.getSenderName() + "]" );
		(new GreenMessageRequest( client, message.getSenderName(), "Your message contained no meat or items.", new AdventureResult( AdventureResult.MEAT, 0 ) )).run();
	}

	public boolean addMessage( String boxname, String message )
	{
		boolean success = super.addMessage( boxname, message );
		if ( success && boxname.equals( "Inbox" ) )
		{
			client.resetContinueState();
			LockableListModel messages = getMessages( "Inbox" );
			if ( !processMessage( (KoLMailMessage) messages.get( messages.size() - 1 ) ) )
			{
				client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!" );
				client.setBuffBotActive( false );
				buffbotLog.update( BuffBotHome.ERRORCOLOR, "Unable to process a buff message." );
			}
		}

		return success;
	}

	private boolean onWhiteList(String userName)
	{	return Arrays.binarySearch(whiteListArray, userName.toLowerCase()) > -1;
	}

	private void sendRefund( String recipient, String reason, int amount )
	{	sendRefund( recipient, reason, new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	private void sendRefund( String recipient, String reason, AdventureResult refund )
	{
		(new GreenMessageRequest( client, recipient, reason, refund )).run();
		buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Sent refund to [" + recipient + "], " + refund.toString() );
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

	private void sendThankYou( String recipient, String messageHTML )
	{
		if ( !thanksMessage.equals("") )
		{
			String reason = thanksMessage +
				System.getProperty( "line.separator" ) + System.getProperty( "line.separator" ) +
				">" + messageHTML.replaceAll( "<.*?>", " " ).replaceAll( "[ ]+", " " );

			(new GreenMessageRequest( client, recipient, reason, new AdventureResult( AdventureResult.MEAT, 0 ) )).run();
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Sent thank you to [" + recipient + "]" );
		}
		else
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Thank you message NOT sent to [" + recipient + "]" );

	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	private void stockRestores()
	{
		if ( getRestoreCount() <= autoBuySetting )
		{
			try
			{
				int currentRestores = -1;

				while ( client.isBuffBotActive() && getRestoreCount() <= autoBuySetting && currentRestores != getRestoreCount() )
				{
					currentRestores = getRestoreCount();
					client.updateDisplay( DISABLED_STATE, "Executing auto-stocking script..." );
					(new KoLmafiaCLI( client, settings.getProperty( "autoStockScript" ) )).listenForCommands();
				}

				if ( currentRestores == getRestoreCount() )
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

	private boolean processMessage( KoLMailMessage message )
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
						( buff.philanthropic && buffbotLog.getInstanceCount( meatSent, message.getSenderName() ) < maxPhilanthropy ) )
					{
						return executeBuff( buff, message, meatSent );
					}
					else if ( buff.restricted )
					{
						// This is a restricted buff for a non-allowed user.
						buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Request for restricted buff denied: from [" +
							message.getSenderName() + "] meat received: " + meatSent );

						sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );

						if ( !saveList.contains( message ) )
							deleteList.add( message );
						return true;
					}
					else
					{
						buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Repeated request: from [" +
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
					buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Meat received does not match anything in database: from [" +
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

	private boolean executeBuff( BuffBotCaster buff, KoLMailMessage message, int meatSent )
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
				String refundReason = "This buffbot was unable to process your request.  " +
					UseSkillRequest.lastUpdate + "  Please try again later.";
				if (UseSkillRequest.lastUpdate.indexOf("cannot receive")!=-1)
					refundAmount = 0; //can't send refund to ronin/hardcore
				sendRefund( message.getSenderName(), refundReason, refundAmount );

				if ( !saveList.contains( message ) )
					deleteList.add( message );
				return true;
			}

		}

		if ( buff.philanthropic )
			buffbotLog.addToRecipientList( meatSent, message.getSenderName() );

		if ( !saveList.contains( message ) )
			deleteList.add( message );

		return true;
	}

	private void detectScam( KoLMailMessage message, String scamString )
	{
		// For scam messages, leave the scam messages in
		// the player's inbox until further notice.

		Matcher scamMatcher = Pattern.compile( scamString ).matcher( message.getMessageHTML() );
		if ( scamMatcher.find() )
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Possible attempted scam message from [" + message.getSenderName() + "]" );

		// Now, mark for either save or delete the message,
		// or ignore the message, if applicable.

		else if ( messageDisposalSetting == SAVEBOX )
		{
			saveList.add( message );
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Saving non-buff message from [" + message.getSenderName() + "]" );
		}
		else if ( messageDisposalSetting == DISPOSE )
		{
			deleteList.add( message );
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Deleting non-buff message from [" + message.getSenderName() + "]" );
		}
		else
			buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Ignoring non-buff message from [" + message.getSenderName() + "]" );
	}


	private boolean recoverMP( int mpNeeded )
	{
		if ( characterData.getCurrentMP() >= mpNeeded )
			return true;

		int previousMP = -1;
		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			MPRestoreItemList.MPRestoreItem restorer = (MPRestoreItemList.MPRestoreItem) mpRestoreItemList.get(i);
			String itemName = restorer.toString();

			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
			{
				if ( itemName.equals( mpRestoreItemList.BEANBAG.toString() ) )
				{
					while ( characterData.getAdventuresLeft() > 0 && characterData.getCurrentMP() > previousMP )
					{
						previousMP = characterData.getCurrentMP();
 						restorer.recoverMP( mpNeeded );
 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;

						if ( characterData.getCurrentMP() == previousMP )
						{
							buffbotLog.update( BuffBotHome.ERRORCOLOR, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( client )).run();
						}
 					}
				}
				else
				{
					AdventureResult item = new AdventureResult( itemName, 0 );
 					while ( inventory.contains( item ) && characterData.getCurrentMP() > previousMP )
 					{
 						previousMP = characterData.getCurrentMP();
 						restorer.recoverMP( mpNeeded );
 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;

						if ( characterData.getCurrentMP() == previousMP )
						{
							buffbotLog.update( BuffBotHome.ERRORCOLOR, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( client )).run();
						}
 					}
				}
			}
		}

		buffbotLog.update( BuffBotHome.ERRORCOLOR, "Unable to acquire enough MP!" );
		return false;
	}

	public List getMPRestoreItemList()
	{	return mpRestoreItemList;
	}

	public int getRestoreCount()
	{
		int restoreCount = 0;

		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			MPRestoreItemList.MPRestoreItem restorer = (MPRestoreItemList.MPRestoreItem) mpRestoreItemList.get(i);
			String itemName = restorer.toString();

			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
				restoreCount += restorer.itemUsed.getCount( client.getInventory() );
		}

		return restoreCount;
	}

	/**
	 * An internal class used to represent a single instance of casting a
	 * buff.  This is used to manage buffs inside of the BuffBotManager
	 * class by simply calling a method after a simple lookup.
	 */

	public class BuffBotCaster
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

			// Figure out how much MP the buff will take, and then identify
			// the number of casts per request that this character can handle.

			int totalCasts = castCount;
			double mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );
			double maximumMP = characterData.getMaximumMP();

			double currentMP;
			int currentCast, mpPerEvent;

			if ( price > 0 )
				buffbotLog.update( BuffBotHome.BUFFCOLOR, "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + price + " meat... " );
			else
				buffbotLog.update( BuffBotHome.BUFFCOLOR, "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + (-price) + " tiny houses... " );

			while ( totalCasts > 0 )
			{
				currentMP = (double) characterData.getCurrentMP();
				currentCast = Math.min( totalCasts, (int) (maximumMP / mpPerCast) );
				mpPerEvent = (int) (mpPerCast * currentCast);
				if ( !recoverMP( mpPerEvent ) )
					return (double)totalCasts / (double)castCount;

				(new UseSkillRequest( client, buffName, target, currentCast )).run();

				if ( !client.permitsContinue() )
				{
					buffbotLog.update( BuffBotHome.ERRORCOLOR, " ---> Could not cast " + buffName + " on " + target );
					return (double)totalCasts / (double)castCount;
				}

				totalCasts -= currentCast;
				buffbotLog.update( BuffBotHome.BUFFCOLOR, " ---> Successfully cast " + buffName + " " + currentCast + " times" );
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

	/**
	 * Internal class used as a holder class to hold all of the
	 * items which are available for use as MP buffers.
	 */

	private class MPRestoreItemList extends SortedListModel
	{
		public final MPRestoreItem BEANBAG = new MPRestoreItem( "rest in beanbag chair", 80, -1 );

		public MPRestoreItemList()
		{
			// These MP restores come from NPCs, so they have a
			// constant market value

			this.add( BEANBAG );
			this.add( new MPRestoreItem( "magical mystery juice", (int) (characterData.getLevel() * 1.5 + 4.0), 150 ) );
			this.add( new MPRestoreItem( "soda water", 4, 70 ) );

			// On the other hand, these MP restores have a fairly
			// arbitrary value and may be subject to arbitrary
			// inflation, based on player spending habits.

			this.add( new MPRestoreItem( "tiny house", 22, 400 ) );
			this.add( new MPRestoreItem( "phonics down", 48, 800 ) );
			this.add( new MPRestoreItem( "Knob Goblin superseltzer", 27, 900 ) );
			this.add( new MPRestoreItem( "Mountain Stream soda", 9, 120 ) );
			this.add( new MPRestoreItem( "Dyspepsi-Cola", 12, 250 ) );
			this.add( new MPRestoreItem( "Knob Goblin seltzer", 5, 80 ) );
			this.add( new MPRestoreItem( "green pixel potion", 15, 500 ) );
			this.add( new MPRestoreItem( "blue pixel potion", 19, 800 ) );
			this.add( new MPRestoreItem( "Blatantly Canadian", 24, 1000 ) );
		}

		public class MPRestoreItem implements Comparable
		{
			private String itemName;
			private int mpPerUse;
			private int estimatedPrice;
			private double priceToMPRatio;
			private AdventureResult itemUsed;

			public MPRestoreItem( String itemName, int mpPerUse, int estimatedPrice )
			{
				this.itemName = itemName;
				this.mpPerUse = mpPerUse;
				this.estimatedPrice = estimatedPrice;

				this.priceToMPRatio = (double)estimatedPrice / (double)mpPerUse;
				this.itemUsed = new AdventureResult( itemName, 0 );
			}

			public void recoverMP( int mpNeeded )
			{
				if ( this == BEANBAG )
				{
					buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Relaxing in my beanbag chair." );
					(new KoLAdventure( client, "campground.php", "relax", "Campsite: To the Beanbag!" )).run();
					return;
				}

				KoLCharacter characterData = client.getCharacterData();
				int currentMP = characterData.getCurrentMP();
				int maximumMP = characterData.getMaximumMP();

				// always buff as close to maxMP as possible, in order to
				//        go as easy on the server as possible
				// But, don't go too far over (thus wasting restorers)
				int mpShort = Math.max(maximumMP + 5 - mpPerUse, mpNeeded) - currentMP;
				int numberToUse = Math.min( 1 + ((mpShort - 1) / mpPerUse), itemUsed.getCount( client.getInventory() ) );

				if ( numberToUse > 0 )
				{
					buffbotLog.update( BuffBotHome.NONBUFFCOLOR, "Consuming " + numberToUse + " " + itemName + "s." );
					(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
				}
			}

			public int compareTo( Object o )
			{
				if ( o instanceof MPRestoreItem || o == null )
					return -1;

				double ratioDifference = this.priceToMPRatio - ((MPRestoreItem)o).priceToMPRatio;
				return ratioDifference < 0 ? -1 : ratioDifference > 0 ? 1 : 0;
			}

			public String toString()
			{	return itemName;
			}
		}
	}
}
