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

/**
 * Container class for <code>BuffBotManager</code>
 * Provides all aspects of BuffBot execution.
 */

public class BuffBotManager extends KoLMailManager implements KoLConstants
{
	private KoLmafia client;
	private Properties settings;
	private LockableListModel buffCostTable;
	private boolean saveNonBuffmsgs;
	private String MPRestoreSetting;
	private KoLCharacter me;
	private LimitedSizeChatBuffer buffbotLog;
	private static final int BBSLEEPTIME = 1000; // Sleep this much each time
	private static final int BBSLEEPCOUNT = 75;  // This many times
	private static final int BBSLEEPCOUNTLONG = 300;  // This many times for slot needs
	private	ArrayList deleteList, saveList;
	
	private static final String BUFFCOLOR = "<font color = \"darkgreen\">";
	private static final String NONBUFFCOLOR = "<font color = \"darkblue\">";
	private static final String ERRORCOLOR = "<font color = \"darkblue\">";
	private static final String ENDCOLOR = "</font>";
	
	/**
	 * Constructor for the <code>BuffBotManager</code> class.
	 */
	
	public BuffBotManager(KoLmafia client,
			LockableListModel buffCostTable)
	{
		
		super( client );
		this.client = client;
		this.buffCostTable = buffCostTable;
		
		settings = (client == null) ? System.getProperties() : client.getSettings();
		String tempStr = settings.getProperty( "NonBuffMsgSave" );
		this.saveNonBuffmsgs = settings.getProperty( "NonBuffMsgSave" ).equals("true");
		this.MPRestoreSetting = settings.getProperty( "MPRestoreSelect" );
		client.updateDisplay( DISABLED_STATE, "Buffbot Starting" );
		me =  client.getCharacterData();
		buffbotLog = client.getBuffBotLog();
		
	}
	
	/**
	 * This is the main BuffBot method.
	 * It loops until the user cancels, or an exception (such as not enough MP to continue).
	 *
	 * On each pass, it gets all messages from the mailbox, then iterates on the mailbox.
	 */
	public void runBuffBot( )
	{
		KoLMailMessage firstmsg;
		
		//Now, make sure the MP is up to date:
		(new CharsheetRequest( client )).run();
		// The outer loop goes until user cancels
		while( client.isBuffBotActive() )
		{
			
			//First, retrieve all messages in the mailbox (If there are any)
			if ( client != null )
				(new MailboxRequest( client, "Inbox" )).run();
			
			//Next process each message in the Inbox
			LockableListModel inbox = getMessages("Inbox");
			deleteList = new ArrayList();
			saveList = new ArrayList();
			while (inbox.size() > 0)
			{
				firstmsg = (KoLMailMessage) inbox.get( 0 );
				// determine if this is a buff request (and if so, which one)
				// if it is a buff request, cast the buff,
				// otherwise either save it or delete it.
				if (!processMessage(firstmsg))
				{
					client.updateDisplay( ENABLED_STATE, "Unable to continue BuffBot!");
					client.cancelRequest();
					client.setBuffBotActive(false);
					buffbotLog.append(ERRORCOLOR + "Unable to process a buff message.</font><br>\n");
				}
				
				// clear it out of the inbox
				inbox.remove(firstmsg);
				
			}
			// do all the deletes and saves
			if (!deleteList.isEmpty())
				(new MailboxRequest( client, "Inbox", deleteList.toArray(), "delete" )).run();
			if (!saveList.isEmpty()) 
				(new MailboxRequest( client, "Inbox", saveList.toArray(), "save" )).run();
			// otherwise sleep for a while and then try again
			// (don't go away for more than 1 second at a time
			client.updateDisplay(DISABLED_STATE, "BuffBot is sleeping");
			for(int i = 1 ; i <= BBSLEEPCOUNT; i = i + 1)
				if (client.isBuffBotActive())
					KoLRequest.delay(BBSLEEPTIME);
		}
	}
	
	private boolean processMessage(KoLMailMessage myMsg )
	{
		int meatSent;
		BuffBotFrame.BuffDescriptor buffEntry;
		boolean buffRequestFound = false;
		
		try
		{
			Matcher meatMatcher = Pattern.compile( ">You gain ([\\d,]+) Meat" ).matcher( myMsg.getMessageHTML() );
			if (meatMatcher.find())
			{
				meatSent = df.parse( meatMatcher.group(1) ).intValue();
				
				//look for this amount in the buff table
				for ( int i = 0; i < buffCostTable.size() && !buffRequestFound; ++i)
				{
					buffEntry = (BuffBotFrame.BuffDescriptor) buffCostTable.get(i);
					// Look for a match of both buffCost and buffCost2
					if (meatSent == buffEntry.buffCost)
					{
						// We have a genuine buff request, so do it!
						buffRequestFound = true;
						if (! castThatBuff(myMsg.getSenderName(), buffEntry, buffEntry.buffCastCount))
						{
							return false;
						}
					}
					else if (meatSent == buffEntry.buffCost2)
					{
						// We have a genuine buff request, so do it!
						buffRequestFound = true;
						if (! castThatBuff(myMsg.getSenderName(), buffEntry, buffEntry.buffCastCount2))
						{
							return false;
						}
					}
				}
				
			}
		}
		catch( Exception e )
		{}
		
		//Now, mark for either save or delete the message.
		String msgDisp = ((!buffRequestFound) && saveNonBuffmsgs ? "save" : "delete");
		if (!buffRequestFound)
		{
			buffbotLog.append( NONBUFFCOLOR + "Received non-buff message from [" + myMsg.getSenderName() + "]</font><br>\n");
			buffbotLog.append( NONBUFFCOLOR + "Action: " + msgDisp + "</font><br>\n");
		}
		if (msgDisp == "save") saveList.add(myMsg);
		else deleteList.add(myMsg);
		
		return true;
	}
	
	private boolean castThatBuff(String bufftarget, BuffBotFrame.BuffDescriptor buffEntry,
			int num2cast)
	{
		// Figure out how much MP the buff will take,
		// and then identify the number of casts per request that this
		// character can handle.
		int castsPerEvent, MPperEvent ;
		
		int buffID = buffEntry.buffID;
		int totalCasts = num2cast;
		int MPperCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );
		
		while (totalCasts > 0)
		{
			castsPerEvent = Math.min(totalCasts, (me.getMaximumMP())/(MPperCast));
			MPperEvent = MPperCast * castsPerEvent;
			if (me.getCurrentMP() < MPperEvent)
			{
				//time to buff up
				if (! recoverMP(MPperEvent)) return false;
			}
			(new UseSkillRequest(client, buffEntry.buffName, bufftarget, castsPerEvent)).run();
			totalCasts = totalCasts - castsPerEvent;
		}
		buffbotLog.append( BUFFCOLOR + "Cast " + buffEntry.buffName + ", " + num2cast + " times on " + bufftarget + ".</font><br>\n");
		return true;
	}
	
	private boolean recoverMP(int MPNeeded)
	{
		final int PHONICSMP = 46, TINYHOUSEMP = 20, BEANBAGMP = 80;
		int num2use, MPShort;
		AdventureResult itemUsed;
		
		int currentMP;
		//First try resting in the beanbag chair
		// TODO - implement beanbag chair recovery
		
		// TODO Coompute the optimal use of Tiny Houses or Phonics first
		// try to get there using tiny houses
		if (MPRestoreSetting.equals("Phonics & Houses") | MPRestoreSetting.equals("Tiny Houses Only"))
		{	useRestoralItem("tiny house", MPNeeded, TINYHOUSEMP);
			currentMP = me.getCurrentMP();
			if (currentMP >= MPNeeded) return true;
		}
		
		// try to get there using phonics downs
		if (MPRestoreSetting.equals("Phonics & Houses") | MPRestoreSetting.equals("Phonics Only"))
		{	useRestoralItem("phonics down", MPNeeded, PHONICSMP);
			currentMP = me.getCurrentMP();
			if (currentMP >= MPNeeded) return true;
		}
		buffbotLog.append( ERRORCOLOR + "Unable to acquire enough MP!</font><br>\n");
		return false;
	}
	void useRestoralItem(String itemName, int MPNeeded, int MPperuse)
	{
		
		int num2use, MPShort;
		AdventureResult itemUsed;
		int currentMP = me.getCurrentMP();
		int maxMP = me.getMaximumMP();
		// always buff as close to maxMP as possible, in order to
		//        go as easy on the server as possible
		// But, don't go too far over (thus wasting restorers)
		MPShort = Math.max(maxMP + 5 - MPperuse, MPNeeded) - currentMP;
		num2use = 1 + ((MPShort - 1) / MPperuse);
		itemUsed = new AdventureResult( itemName, 0 - num2use);
		int itemIndex = client.getInventory().indexOf(itemUsed  );
		if  ( itemIndex > -1 )
		{
			num2use = Math.min(num2use, ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
			if (num2use > 0)
			{
				buffbotLog.append("Consuming " + num2use + " " + itemName + "s.<br>\n");
				(new ConsumeItemRequest( client, ConsumeItemRequest.CONSUME_MULTIPLE, new AdventureResult( itemUsed.getItemID(), num2use ) )).run();
			}
		}
		
		
	}
}
