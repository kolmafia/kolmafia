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

// utilities
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	private KoLmafia client;
	private KoLCharacter characterData;
	private Properties settings;

	private ArrayList saveList;
	private ArrayList deleteList;

	private String mpRestoreSetting;
	private boolean messageDisposalSetting;
	private LimitedSizeChatBuffer buffbotLog;

	private static final int SLEEP_TIME = 1000;       // Sleep this much each time
	private static final int SHORT_SLEEP_COUNT = 75;  // This many times
	private static final int LONG_SLEEP_COUNT = 300;  // This many times for slot needs
	private static final int TINYHOUSEMP = 20;
	private static final int PHONICSMP = 46;

	private Map costMap;
	private LockableListModel buffCostTable;
	private String[] whiteListArray;

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
		this.buffCostTable = buffCostTable;
		settings = (client == null) ? System.getProperties() : client.getSettings();

		this.costMap = new TreeMap();

		characterData =  client.getCharacterData();
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
		Object oldCast = costMap.put( new Integer( price ), newCast );

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
			costMap.remove( new Integer( toRemove.getPrice() ) );
		}
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
			this.buffID = ClassSkillsDatabase.getSkillID( buffName );
			this.buffName = buffName;
			this.price = price;
			this.castCount = castCount;

			this.restricted = restricted;

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "Cast " );
			stringForm.append( buffName );
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

			while ( totalCasts > 0 )
			{
				currentCast = Math.min(totalCasts, (int) (maximumMP/mpPerCast) );
				mpPerEvent = (int) (mpPerCast * currentCast);
				currentMP = (double) characterData.getCurrentMP();
				if ( !recoverMP( mpPerEvent ) )
						return false;
				(new UseSkillRequest( client, buffName, target, currentCast )).run();
				totalCasts -= currentCast;
			}

			buffbotLog.append( BUFFCOLOR + "Cast " + buffName + ", " + castCount + " times on " 
					+ target + " for " + price + " meat. "+ ENDCOLOR + "<br>\n");
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
	 * This is the main BuffBot method. It loops until the user cancels, or an exception
	 * (such as not enough MP to continue).  On each pass, it gets all messages from the
	 * mailbox, then iterates on the mailbox.
	 */

	public void runBuffBot()
	{
		// need to make sure the MP is up to date
		(new CharsheetRequest( client )).run();
		
		// get all current buffbot settings
		messageDisposalSetting = settings.getProperty( "buffBotMessageDisposal" ) != null &&
			settings.getProperty( "buffBotMessageDisposal" ).equals("true");
		mpRestoreSetting = settings.getProperty( "buffBotMPRestore" ) == null ? "Phonics & Houses" :
			settings.getProperty( "buffBotMPRestore" );
		whiteListArray = settings.getProperty("whiteList").toLowerCase().split("\\s*,\\s*");
		
		client.updateDisplay( DISABLED_STATE, "Buffbot Starting" );

		// The outer loop goes until user cancels
		while( client.isBuffBotActive() )
		{
			// First, retrieve all messages in the mailbox (If there are any)
			if ( client != null )
				(new MailboxRequest( client, "Inbox" )).run();

			// Next process each message in the Inbox
			Object [] inbox = getMessages("Inbox").toArray();
			deleteList.clear();  saveList.clear();

			for ( int i = inbox.length - 1; i >= 0; --i )
			{
				if ( !processMessage( (KoLMailMessage) inbox[i] ) )
				{
					client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!");
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
			for( int i = 0; i < SHORT_SLEEP_COUNT; ++i )
				if ( client.isBuffBotActive() )
					KoLRequest.delay( SLEEP_TIME );
		}
	}
	
	private boolean onWhiteList(String userName)
	{
		return (Arrays.binarySearch(whiteListArray, userName) > -1);
	}
	
	private boolean processMessage( KoLMailMessage message )
	{
		Integer meatSent = 0;
		BuffBotCaster buff;
		boolean buffFound = false;

		try
		{
			Matcher meatMatcher = Pattern.compile( ">You gain ([\\d,]+) Meat" ).matcher( message.getMessageHTML() );
			if ( meatMatcher.find() )
			{
				meatSent = new Integer( df.parse( meatMatcher.group(1) ).intValue() );

				// Look for this amount in the buff table
				buff = (BuffBotCaster) costMap.get( meatSent );
				if ( buff != null )
				{
					//See if this is a restricted buff, and the sender qualifies
					if ((!buff.restricted) || onWhiteList(message.getSenderName())) 
					{
						// We have a genuine buff request, so do it!
						if (!buff.castOnTarget( message.getSenderName(), meatSent ))
							return false;
						deleteList.add( message );
						return true;
					}
					else // this is a restricted buff for a non-allowed user.
					{
						//TODO offer a refund option for this (instead of just save/delete)
						buffbotLog.append( NONBUFFCOLOR + "Request for resricted buff denied: from [" + 
								message.getSenderName() + "] meat received: " + meatSent + ENDCOLOR + "<br>\n");
						buffbotLog.append( NONBUFFCOLOR + "Action: " + (messageDisposalSetting ? "save" : "delete") + ENDCOLOR + "<br>\n");
						if ( messageDisposalSetting )
							saveList.add( message );
						else
							deleteList.add( message );
						return true;
					}
				}
			}
		}
		catch( Exception e )
		{
			return false;
		}
		
		// Must not be a buff request message, so notify user and save/delete
		String meatString = (meatSent > 0) ? " (meat recvd: " + meatSent + ")" : "";
		buffbotLog.append( NONBUFFCOLOR + "Received non-buff message from [" + message.getSenderName() + "]" + meatString + ENDCOLOR + "<br>\n");
		buffbotLog.append( NONBUFFCOLOR + "Action: " + (messageDisposalSetting ? "save" : "delete") + ENDCOLOR + "<br>\n");

		// Now, mark for either save or delete the message.
		if ( messageDisposalSetting )
			saveList.add( message );
		else
			deleteList.add( message );
				
		return true;
	}

	private boolean recoverMP( int mpNeeded )
	{
		if ( characterData.getCurrentMP() >= mpNeeded )
			return true;

		// First try resting in the beanbag chair
		// TODO - implement beanbag chair recovery

		// TODO Compute the optimal use of Tiny Houses or Phonics first
		// try to get there using tiny houses

		if ( mpRestoreSetting.equals( "Phonics & Houses" ) || mpRestoreSetting.equals( "Tiny Houses Only" ) )
		{
			AdventureResult item = new AdventureResult( "tiny house", 0 );
			while ( client.getInventory().contains( item ) )
			{
				useRestoreItem( "tiny house", TINYHOUSEMP );
				if ( characterData.getCurrentMP() >= mpNeeded )
					return true;
			}
		}

		// try to get there using phonics downs
		if ( mpRestoreSetting.equals( "Phonics & Houses" ) || mpRestoreSetting.equals( "Phonics Only" ) )
		{
			AdventureResult item = new AdventureResult( "phonics down", 0 );
			while ( client.getInventory().contains( item ) )
			{
				useRestoreItem( "phonics down", PHONICSMP );
				if ( characterData.getCurrentMP() >= mpNeeded )
					return true;
			}
		}

		buffbotLog.append( ERRORCOLOR + "Unable to acquire enough MP!" + ENDCOLOR + "<br>\n");
		return false;
	}

	private void useRestoreItem( String itemName, int mpPerUse )
	{
		int currentMP = characterData.getCurrentMP();
		int maximumMP = characterData.getMaximumMP();

		// Always buff as close to maxMP as possible, in order to
		// go as easy on the server as possible.  But, don't go
		// too far over (thus wasting restorers).

		int mpNeeded = maximumMP - currentMP + 5;
		int numberToUse = 1 + ((mpNeeded - 1) / mpPerUse);

		AdventureResult itemUsed = new AdventureResult( itemName, 0 );
		int itemIndex = client.getInventory().indexOf( itemUsed );

		if  ( itemIndex > -1 )
		{
			numberToUse = Math.min( numberToUse, ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
			if ( numberToUse > 0 )
			{
				buffbotLog.append( "Consuming " + numberToUse + " " + itemName + "s.<br>\n" );
				(new ConsumeItemRequest( client, ConsumeItemRequest.CONSUME_MULTIPLE,
					new AdventureResult( itemUsed.getItemID(), numberToUse ) )).run();
			}
		}
	}
}
