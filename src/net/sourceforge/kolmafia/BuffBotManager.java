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

	private static final int SLEEP_TIME = 1000;       // Sleep this much each time
	private static final int SHORT_SLEEP_COUNT = 75;  // This many times
	private static final int LONG_SLEEP_COUNT = 300;  // This many times for slot needs

	private Map buffCostMap;
	private MPRestoreItemList mpRestoreItemList;
	private LockableListModel buffCostTable;
	private String [] whiteListArray;

	private static final String BUFFCOLOR = "<font color=green>";
	private static final String NONBUFFCOLOR = "<font color=blue>";
	private static final String ERRORCOLOR = "<font color=red>";
	private static final String ENDCOLOR = "</font>";

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
	}

	/**
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public synchronized void runBuffBot()
	{
		client.updateDisplay( DISABLED_STATE, "Buffbot Starting" );

		// need to make sure the MP is up to date
		(new CharsheetRequest( client )).run();

		// get all current buffbot settings
		messageDisposalSetting = settings.getProperty( "buffBotMessageDisposal" ) == null ? 0 :
			Integer.parseInt( settings.getProperty( "buffBotMessageDisposal" ) );

		mpRestoreSetting = settings.getProperty( "buffBotMPRestore" ) == null ? "tiny house" :
			settings.getProperty( "buffBotMPRestore" );

		whiteListArray = settings.getProperty("whiteList") == null ? new String[0] :
			settings.getProperty("whiteList").toLowerCase().split("\\s*,\\s*");
		Arrays.sort(whiteListArray);

		// The outer loop goes until user cancels
		while( client.isBuffBotActive() )
		{
			// First, retrieve all messages in the mailbox (If there are any)
			if ( client != null )
				(new MailboxRequest( client, "Inbox" )).run();

			// Next process each message in the Inbox
			Object [] inbox = getMessages( "Inbox" ).toArray();
			deleteList.clear();  saveList.clear();
			if (inbox.length > 0) buffbotLog.timeStampedLogEntry("Mail received.<br>\n");
			for ( int i = inbox.length - 1; client.isBuffBotActive() && i >= 0; --i )
			{
				client.resetContinueState();
				if ( !processMessage( (KoLMailMessage) inbox[i] ) )
				{
					client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!" );
					client.setBuffBotActive( false );
					buffbotLog.append( ERRORCOLOR + "Unable to process a buff message." + ENDCOLOR + "<br>\n" );
				}
			}

			// Do all the deletes and saves

			if ( !deleteList.isEmpty() )
				deleteMessages( "Inbox", deleteList.toArray() );
			if ( !saveList.isEmpty() )
				saveMessages( saveList.toArray() );

			// Otherwise sleep for a while and then try again
			// (don't go away for more than 1 second at a time

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

	private boolean onWhiteList(String userName)
	{	return Arrays.binarySearch(whiteListArray, userName.toLowerCase()) > -1;
	}

	private void sendRefund( String recipient, String reason, int amount )
	{
		(new GreenMessageRequest( client, recipient, reason, new AdventureResult( AdventureResult.MEAT, amount ) )).run();
		buffbotLog.append( NONBUFFCOLOR + "Sent refund to [" + recipient + "] meat sent: " + amount + ENDCOLOR + "<br>\n");
	}

	private boolean processMessage( KoLMailMessage message )
	{
		int meatSent = 0;
		BuffBotCaster buff;

		try
		{
			Matcher meatMatcher = Pattern.compile( "<img src=\"http://images.kingdomofloathing.com/itemimages/meat.gif\" height=30 width=30 alt=\"Meat\">You gain ([\\d,]+) Meat" ).matcher( message.getMessageHTML() );
			if ( meatMatcher.find() )
			{
				meatSent = df.parse( meatMatcher.group(1) ).intValue();

				// Look for this amount in the buff table
				buff = (BuffBotCaster) buffCostMap.get( new Integer( meatSent ) );
				if ( buff != null )
				{
					// See if this is a restricted buff, and the sender qualifies
					if ((!buff.restricted) || onWhiteList(message.getSenderName()))
					{
						// We have a genuine buff request, so do it!
						if ( !buff.castOnTarget( message.getSenderName(), meatSent ))
						{
							if ( client.permitsContinue() )
							{
								sendRefund( message.getSenderName(), "We're sorry.  The Buffbot you have dialed has gone insane and thinks it has run out of MP restores.  Please try again later.", meatSent );
								deleteList.add( message );
								return false;
							}
							else
							{
								sendRefund( message.getSenderName(), "The Buffbot is not currently able to process your request.  Please try again later.", meatSent );
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
								message.getSenderName() + "] meat received: " + meatSent + ENDCOLOR + "<br>\n");

						sendRefund( message.getSenderName(), "Sorry, this buff is white-list restricted.  Please try a different buff.", meatSent );
						deleteList.add( message );
						return true;
					}
				}
				else
				{
					buffbotLog.append( NONBUFFCOLOR + "Meat received does not match anything in database: from [" +
							message.getSenderName() + "] meat received: " + meatSent + ENDCOLOR + "<br>\n");

					sendRefund( message.getSenderName(), df.format( meatSent ) + " meat is not a valid buff price.", meatSent );
					deleteList.add( message );
					return true;
				}
			}
		}
		catch( Exception e )
		{
			return false;
		}

		// Must not be a buff request message, so notify user and save/delete
		// check to see if it was an attempted scam
		Matcher meatMatcher = Pattern.compile( "You gain ([\\d,]+) Meat" ).matcher( message.getMessageHTML() );
		if ( meatMatcher.find() )
			buffbotLog.append( NONBUFFCOLOR + "Possible attempted scam message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>\n");
		else
			buffbotLog.append( NONBUFFCOLOR + "Received non-buff message from [" + message.getSenderName() + "]" + ENDCOLOR + "<br>\n");

		buffbotLog.append( NONBUFFCOLOR + "Action: " + (messageDisposalSetting == INBOX ? "ignore" :
			messageDisposalSetting == SAVEBOX ? "save" : "delete") + ENDCOLOR + "<br>\n");

		// Now, mark for either save or delete the message.

		if ( messageDisposalSetting == SAVEBOX )
			saveList.add( message );
		else if ( messageDisposalSetting == DISPOSE )
			deleteList.add( message );

		return true;
	}

	private boolean recoverMP( int mpNeeded )
	{
		if ( characterData.getCurrentMP() >= mpNeeded )
			return true;

		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			String itemName = mpRestoreItemList.get(i).toString();
			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
			{
				if ( itemName.equals( mpRestoreItemList.BEANBAG.toString() ) )
				{
					while ( characterData.getAdventuresLeft() > 0 )
					{
						mpRestoreItemList.BEANBAG.recoverMP(mpNeeded);
						if ( characterData.getCurrentMP() >= mpNeeded )
							return true;
					}
				}
				else
				{
					AdventureResult item = new AdventureResult( itemName, 0 );
					while ( inventory.contains( item ) )
					{
						((MPRestoreItemList.MPRestoreItem)mpRestoreItemList.get(i)).recoverMP(mpNeeded);
						if ( characterData.getCurrentMP() >= mpNeeded )
							return true;
					}
				}
			}
		}

		buffbotLog.append( ERRORCOLOR + "Unable to acquire enough MP!" + ENDCOLOR + "<br>\n");
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
			this.buffID = ClassSkillsDatabase.getSkillID( buffName.replaceFirst( "ñ", "&ntilde;" ) );
			this.buffName = buffName.replaceFirst("&ntilde;","ñ");
			this.price = price;
			this.castCount = castCount;

			this.restricted = restricted;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "Cast " );
			stringForm.append( this.buffName );
			stringForm.append( ' ' );
			stringForm.append( castCount );
			stringForm.append( " times for " );
			stringForm.append( price );
			stringForm.append( " meat" );

			if ( restricted )
				stringForm.append( " (white list only)" );

			this.stringForm = stringForm.toString();
			this.settingString = buffID + ":" + price + ":" + castCount + ":" + restricted;
		}

		public boolean castOnTarget( String target, int price )
		{
			// Figure out how much MP the buff will take, and then identify
			// the number of casts per request that this character can handle.

			int totalCasts = castCount;

			double maximumMP = characterData.getMaximumMP();
			double mpPerCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );

			double currentMP;
			int currentCast, mpPerEvent;

			buffbotLog.append( BUFFCOLOR + "Casting " + buffName + ", " + castCount + " times on "
					+ target + " for " + price + " meat... "+ ENDCOLOR + "<br>\n");
			while ( totalCasts > 0 )
			{
				currentCast = Math.min(totalCasts, (int) (maximumMP/mpPerCast) );
				mpPerEvent = (int) (mpPerCast * currentCast);
				currentMP = (double) characterData.getCurrentMP();
				if ( !recoverMP( mpPerEvent ) )
					return false;

				(new UseSkillRequest( client, buffName, target, currentCast )).run();
				totalCasts -= currentCast;

				if ( !client.permitsContinue() )
				{
					buffbotLog.append( ERRORCOLOR + " ---> " + target + " had too many buffs." + ENDCOLOR + "<br>\n");
					return false;
				}

				buffbotLog.append( BUFFCOLOR + " ---> Successfully cast: " + buffName + ", " + currentCast + " times." + ENDCOLOR + "<br>\n");
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
			this.add( new MPRestoreItem( "magical mystery juice", characterData.getLevel() + 4, 150 ) );
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
				int numberToUse = 1 + ((mpShort - 1) / mpPerUse);
				int itemIndex = client.getInventory().indexOf( itemUsed );
				if  ( itemIndex > -1 )
				{
					numberToUse = Math.min(numberToUse, ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
					if (numberToUse > 0)
					{
						buffbotLog.append("Consuming " + numberToUse + " " + itemName + "s.<br>\n");
						(new ConsumeItemRequest( client, ConsumeItemRequest.CONSUME_MULTIPLE,
								new AdventureResult( itemUsed.getItemID(), numberToUse ) )).run();
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
