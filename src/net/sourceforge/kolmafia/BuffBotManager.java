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

import java.util.Properties;
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
	private Properties settings;

	private ArrayList saveList;
	private ArrayList deleteList;

	private String mpRestoreSetting;
	private int messageDisposalSetting;
	private BuffBotHome buffbotLog;
	private String refundMessage;

	private static final int SLEEP_TIME = 1000;       // Sleep this much each time
	private static final int SHORT_SLEEP_COUNT = 75;  // This many times
	private static final int LONG_SLEEP_COUNT = 300;  // This many times for slot needs

	private Map buffCostMap;
	private boolean itemBasedBuffing;
	private MPRestoreItemList mpRestoreItemList;
	private LockableListModel buffCostTable;
	private String [] whiteListArray;

	private static final String BUFFCOLOR = "<font color=green>";
	private static final String NONBUFFCOLOR = "<font color=blue>";
	private static final String ERRORCOLOR = "<font color=red>";
	private static final String ENDCOLOR = "</font>";
	private static final String MEAT_REGEX = "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat";

	/**
	 * Constructor for the <code>BuffBotManager</code> class.
	 */

	public BuffBotManager( KoLmafia client, LockableListModel buffCostTable )
	{
		super( client );
		this.client = client;

		settings = (client == null) ? System.getProperties() : client.getSettings();

		this.buffCostMap = new TreeMap();
		this.buffCostTable = buffCostTable;
		this.inventory = client == null ? new LockableListModel() : client.getInventory();

		this.characterData =  client.getCharacterData();
		this.mpRestoreItemList = new MPRestoreItemList();
		buffbotLog = client.getBuffBotLog();

		saveList = new ArrayList();
		deleteList = new ArrayList();
	}

	/**
	 * An internal method which adds a buff to the list of available
	 * buffs.  This also registers the buff inside of the list of
	 * available buffs.
	 */

	public void addBuff( String skillName, int price, int castCount, boolean restricted )
	{
		BuffBotCaster newCast = new BuffBotCaster( skillName, price, castCount, restricted );
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
		if ( settings instanceof KoLSettings )
			((KoLSettings)settings).saveSettings();
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public synchronized void runBuffBot(int runIterations)
	{
		boolean newMessages = false;
		client.updateDisplay( DISABLED_STATE, "Buffbot Starting" );

		// need to make sure the MP is up to date
		(new CharsheetRequest( client )).run();

		// get all current buffbot settings

		itemBasedBuffing = settings.getProperty( "buffBotItemBasedBuffing" ) == null ? false :
			settings.getProperty( "buffBotItemBasedBuffing" ).equals( "true" );

		messageDisposalSetting = settings.getProperty( "buffBotMessageDisposal" ) == null ? SAVEBOX :
			Integer.parseInt( settings.getProperty( "buffBotMessageDisposal" ) );

		mpRestoreSetting = settings.getProperty( "buffBotMPRestore" ) == null ? "tiny house" :
			settings.getProperty( "buffBotMPRestore" );

		whiteListArray = settings.getProperty("whiteList") == null ? new String[0] :
			settings.getProperty("whiteList").toLowerCase().split("\\s*,\\s*");
		Arrays.sort(whiteListArray);

		refundMessage = client.getSettings().getProperty( "invalidBuffMessage" ) == null ? "" :
			System.getProperty( "line.separator" ) + System.getProperty( "line.separator" ) +
				client.getSettings().getProperty( "invalidBuffMessage" );

		// The outer loop goes until user cancels

		while( client.isBuffBotActive() && runIterations-- != 0)
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

			if ( newMessages )
			{
				buffbotLog.timeStampedLogEntry( "Message processing complete.<br>");
				buffbotLog.timeStampedLogEntry( "Buffbot is sleeping.<br>");
			}

			client.updateDisplay( DISABLED_STATE, "BuffBot is sleeping" );

			if ( messageDisposalSetting == INBOX )
			{
				for ( int i = 0; i < LONG_SLEEP_COUNT; ++i )
					if ( client.isBuffBotActive() )
						KoLRequest.delay( SLEEP_TIME );
			}
			else
			{
				for ( int i = 0; i < SHORT_SLEEP_COUNT; ++i )
					if ( client.isBuffBotActive() )
						KoLRequest.delay( SLEEP_TIME );
			}
		}
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
				buffbotLog.append( ERRORCOLOR + "Unable to process a buff message." + ENDCOLOR + "<br>" );
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
		buffbotLog.append( NONBUFFCOLOR + "Sent refund to [" + recipient + "], " + refund.toString() + ENDCOLOR + "<br>");
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
	    Matcher donationMatcher = Pattern.compile( "<img src=\"http://images.kingdomofloathing.com/.*width=30").matcher(
			message.getMessageHTML().replaceAll( MEAT_REGEX, "") );
	    return donationMatcher.find();
	}

	private void sendThankYou( String recipient, String messageHTML )
	{
		String reason = "Thank you very much for your generosity! Your donation is greatly appreciated. " +
			"If this was not intended as a donation, please contact the maintainer of this buffbot.\n\n" +
			"&gt;  " + messageHTML;

		(new GreenMessageRequest( client, recipient, reason, new AdventureResult( AdventureResult.MEAT, 0 ) )).run();
		buffbotLog.append( NONBUFFCOLOR + "Sent thank you to [" + recipient + "] " + ENDCOLOR + "<br>");
	}

	private boolean processMessage( KoLMailMessage message )
	{	return itemBasedBuffing ? findTinyHouse( message ) : findMeat( message );
	}

	private boolean findMeat( KoLMailMessage message )
	{
		int meatSent = 0;
		BuffBotCaster buff;

		try
		{
			Matcher meatMatcher = Pattern.compile( MEAT_REGEX ).matcher( message.getMessageHTML() );
			if ( meatMatcher.find() )
			{
				if ( containsDonation( message ) )
					sendThankYou( message.getSenderName(), message.getMessageHTML() );

				meatSent = df.parse( meatMatcher.group(1) ).intValue();

				// Look for this amount in the buff table
				buff = (BuffBotCaster) buffCostMap.get( new Integer( meatSent ) );
				if ( buff != null )
				{
					// See if this is a restricted buff, and the sender qualifies
					if ((!buff.restricted) || onWhiteList(message.getSenderName()))
					{
						// We have a genuine buff request, so do it!
						if ( !buff.castOnTarget( message.getSenderName() ))
						{
							if ( client.permitsContinue() )
							{
								sendRefund( message.getSenderName(), "This buffbot has run out of the mana restoration items.  Please try again later.", meatSent );
								deleteList.add( message );
								return false;
							}
							else
							{
								sendRefund( message.getSenderName(), "This buffbot was unable to process your request.  Please try again later.", meatSent );
								deleteList.add( message );
								return true;
							}

						}

						deleteList.add( message );
						return true;
					}
					else
					{
						// This is a restricted buff for a non-allowed user.
						buffbotLog.append( NONBUFFCOLOR + "Request for restricted buff denied: from [" +
								message.getSenderName() + "] meat received: " + meatSent + ENDCOLOR + "<br>");

						sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );
						deleteList.add( message );
						return true;
					}
				}
				else
				{
					buffbotLog.append( NONBUFFCOLOR + "Meat received does not match anything in database: from [" +
							message.getSenderName() + "] meat received: " + meatSent + ENDCOLOR + "<br>");

					sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price." + refundMessage, meatSent );
					deleteList.add( message );
					return true;
				}
			}
			else
			{
				if ( containsDonation( message ) )
					sendThankYou( message.getSenderName(), message.getMessageHTML() );

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

	private boolean findTinyHouse( KoLMailMessage message )
	{
		int housesSent = 0;
		BuffBotCaster buff;

		try
		{
			String messageContent = message.getMessageHTML();

			// First, test for multiple houses being sent
			// to do a buff request

			Matcher houseMatcher = Pattern.compile( "<b>tiny house \\(([\\d,]+)\\)" ).matcher( messageContent );
			if ( houseMatcher.find() )
				housesSent = df.parse( houseMatcher.group(1) ).intValue();

			// It's possible the user sent only one house!
			// In which case, you test for just that.

			else if ( messageContent.indexOf( "<b>tiny house" ) != -1 )
				housesSent = 1;

			if ( housesSent > 0 )
			{
				LockableListModel skills = characterData.getAvailableSkills();

				for ( int i = 0; i < skills.size(); ++i )
				{
					if ( messageContent.toLowerCase().indexOf( ((UseSkillRequest)skills.get(i)).getSkillName().replaceFirst( "ñ", "n" ).toLowerCase() ) != -1 )
					{
						int castCount = housesSent * 20 / ClassSkillsDatabase.getMPConsumptionByID(
							ClassSkillsDatabase.getSkillID( ((UseSkillRequest)skills.get(i)).getSkillName() ) );

						if ( !(new BuffBotCaster( ((UseSkillRequest)skills.get(i)).getSkillName(), -housesSent, castCount, false )).castOnTarget( message.getSenderName() ) )
						{
							sendRefund( message.getSenderName(), "This buffbot was unable to process your request.  Please try again later.",
								new AdventureResult( "tiny house", housesSent ) );

							deleteList.add( message );
							return true;
						}

						// If it gets this far, then the buff was successfully
						// cast!  Therefore, delete the message and return.

						deleteList.add( message );
						return true;
					}
				}

				// If it gets this far, that means the user forgot
				// to specify a buff!  Therefore, send them a refund.

				sendRefund( message.getSenderName(), "No buff was specified.  Please try again.", new AdventureResult( "tiny house", housesSent ) );
				deleteList.add( message );
				return true;
			}
		}
		catch( Exception e )
		{
			return false;
		}

		// Must not be a buff request message, so notify user and save/delete;
		// also check to see if it was an attempted scam

		detectScam( message, "You acquire some items: tiny house" );
		return true;
	}

	private void detectScam( KoLMailMessage message, String scamString )
	{
		// For scam messages, leave the scam messages in
		// the player's inbox until further notice.

		Matcher scamMatcher = Pattern.compile( scamString ).matcher( message.getMessageHTML() );
		if ( scamMatcher.find() )
			buffbotLog.append( NONBUFFCOLOR + "Ignoring possible attempted scam message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>");

		// Now, mark for either save or delete the message,
		// or ignore the message, if applicable.

		else if ( messageDisposalSetting == SAVEBOX )
		{
			saveList.add( message );
			buffbotLog.append( NONBUFFCOLOR + "Saving non-buff message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>");
		}
		else if ( messageDisposalSetting == DISPOSE )
		{
			deleteList.add( message );
			buffbotLog.append( NONBUFFCOLOR + "Deleting non-buff message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>");
		}
		else
			buffbotLog.append( NONBUFFCOLOR + "Ignoring non-buff message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>");
	}


	private boolean recoverMP( int mpNeeded )
	{
		boolean keepTrying;
		int previousMP;
		
		if ( characterData.getCurrentMP() >= mpNeeded )
			return true;

		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			String itemName = mpRestoreItemList.get(i).toString();

			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
			{
				if ( itemName.equals( mpRestoreItemList.BEANBAG.toString() ) )
				{
 					keepTrying = (characterData.getAdventuresLeft() > 0);
					while ( keepTrying )
					{
						previousMP = characterData.getCurrentMP();
 						mpRestoreItemList.BEANBAG.recoverMP(mpNeeded);
 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;
						keepTrying = (characterData.getAdventuresLeft() > 0) &
								( characterData.getCurrentMP() > previousMP );
 					}
				}
				else
				{
					AdventureResult item = new AdventureResult( itemName, 0 );
					keepTrying = ( inventory.contains( item ) );
 					while ( keepTrying )
 					{
 						previousMP = characterData.getCurrentMP();
 						((MPRestoreItemList.MPRestoreItem)mpRestoreItemList.get(i)).recoverMP(mpNeeded);
 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;
						keepTrying = inventory.contains( item ) & 
								( characterData.getCurrentMP() > previousMP );
 					}
				}
			}
		}

		buffbotLog.append( ERRORCOLOR + "Unable to acquire enough MP!" + ENDCOLOR + "<br>");
		return false;
	}

	public List getMPRestoreItemList()
	{	return mpRestoreItemList;
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
		private String stringForm;
		private String settingString;

		public BuffBotCaster( String buffName, int price, int castCount, boolean restricted )
		{
			// string may come from either the id lookup database or the skill list, so
			//		need to be prepared for either orientation of ñ
			this.buffID = ClassSkillsDatabase.getSkillID( buffName );
			this.buffName = buffName;
			this.price = price;
			this.castCount = castCount;

			this.restricted = restricted;

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

			this.stringForm = stringForm.toString();
			this.settingString = buffID + ":" + price + ":" + castCount + ":" + restricted;
		}

		public boolean castOnTarget( String target )
		{
			// Figure out how much MP the buff will take, and then identify
			// the number of casts per request that this character can handle.

			int totalCasts = castCount;
			double mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );
			double maximumMP = characterData.getMaximumMP();

			double currentMP;
			int currentCast, mpPerEvent;

			if (price > 0)
				buffbotLog.append( BUFFCOLOR + "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + price + " meat... "+ ENDCOLOR + "<br>");
			else
				buffbotLog.append( BUFFCOLOR + "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + (-price) + " tiny houses... "+ ENDCOLOR + "<br>");
			while ( totalCasts > 0 )
			{
				currentMP = (double) characterData.getCurrentMP();
				currentCast = Math.min( totalCasts, (int) (maximumMP / mpPerCast) );
				mpPerEvent = (int) (mpPerCast * currentCast);
				if ( !recoverMP( mpPerEvent ) )
					return false;

				(new UseSkillRequest( client, buffName, target, currentCast )).run();
				totalCasts -= currentCast;

				if ( !client.permitsContinue() )
				{
					buffbotLog.append( ERRORCOLOR + " ---> Could not cast " + buffName + " on " + target + ENDCOLOR + "<br>");
					return false;
				}

				buffbotLog.append( BUFFCOLOR + " ---> Successfully cast " + buffName + " " + currentCast + " times" + ENDCOLOR + "<br>");
			}

			return true;
		}

		public int getPrice()
		{	return price;
		}

		public String toString()
		{	return stringForm;
		}

		public String toSettingString()
		{	return settingString;
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
			this.add( new MPRestoreItem( "magical mystery juice", characterData.getLevel() + 2, 150 ) );
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
					buffbotLog.append("Relaxing in my beanbag chair." + "<br>");
					(new CampgroundRequest( client, "relax" )).run();
					return;
				}

				KoLCharacter characterData = client.getCharacterData();
				int currentMP = characterData.getCurrentMP();
				int maximumMP = characterData.getMaximumMP();

				// always buff as close to maxMP as possible, in order to
				//        go as easy on the server as possible
				// But, don't go too far over (thus wasting restorers)
				int mpShort = Math.max(maximumMP + 5 - mpPerUse, mpNeeded) - currentMP;
				int itemIndex = client.getInventory().indexOf( itemUsed );
				if  ( itemIndex > -1 )
				{
					int numberToUse = Math.min(1 + ((mpShort - 1) / mpPerUse), ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
					if (numberToUse > 0)
					{
						buffbotLog.append("Consuming " + numberToUse + " " + itemName + "s.<br>");
						(new ConsumeItemRequest( client, new AdventureResult( itemUsed.getItemID(), numberToUse ) )).run();
					}
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
