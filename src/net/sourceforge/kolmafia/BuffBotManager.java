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

/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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


/**
 * Container class for <code>BuffBotManager</code>
 * Provides all aspects of BuffBot execution.
 * @author raney53
 */
public class BuffBotManager {
    private KoLMailManager BBmailbox;
    private KoLmafia client;
    private Properties settings;
    private LockableListModel buffCostTable;
    private boolean saveNonBuffmsgs;
    private String MPRestoreSetting;
    private KoLCharacter me;
    private BuffBotHome.BuffBotLog BBLog;
    private static final int BBSLEEPTIME = 1000; // Sleep this much each time
    private static final int BBSLEEPCOUNT = 60;  // This many times

    
    /**
     * Constructor for the <code>BuffBotManager</code> class.
     */
    public BuffBotManager(KoLmafia client, 
            LockableListModel buffCostTable){
        //for now, use the KoL mail manager
        this.BBmailbox = (client == null) ? new KoLMailManager() : client.getMailManager(); 
        this.client = client;
        this.buffCostTable = buffCostTable;

        settings = (client == null) ? System.getProperties() : client.getSettings();
        String tempStr = settings.getProperty( "NonBuffMsgSave" );
        this.saveNonBuffmsgs = settings.getProperty( "NonBuffMsgSave" ).equals("true");
        this.MPRestoreSetting = settings.getProperty( "MPRestoreSelect" );
        client.updateDisplay( KoLFrame.DISABLED_STATE, "Buffbot Starting" );
        me =  client.getCharacterData();
        BBLog = client.BBHome.getBBLog();
       
    }
    
    /**
     * This is the main BuffBot method. 
     * It loops until the user cancels, or an exception (such as not enough MP to continue).
     * 
     * On each pass, it gets all messages from the mailbox, then iterates on the mailbox.
     */
    public void runBuffBot( ){
        KoLMailMessage firstmsg;
        
        
        // A temporary Kluge to force the main adventure panel to be selected
        client.BBHome.getAdventureTabs().setSelectedIndex(0);
        // The outer loop goes until user cancels
        while( client.permitsContinue() ){
         
            //First, retrieve all messages in the mailbox (If there are any)
            if ( client != null ){
//                BBLog.BBLogEntry("Checking the mail.");
                MailboxRequest MBRequest = new MailboxRequest( client, "Inbox" );
                MBRequest.run();
                if (client.permitsContinue()) 
					client.updateDisplay( KoLFrame.ENABLED_STATE, "");
				else return;
            }
            
            //Next process each message in the Inbox
            LockableListModel inbox = BBmailbox.getMessages("Inbox");
            while (inbox.size() > 0){
                firstmsg = (KoLMailMessage) inbox.get( 0 );
                // determine if this is a buff request (and if so, which one)
                // if it is a buff request, cast the buff,
                // otherwise either save it or delete it.
                if (!BBProcessMessage(firstmsg)){
                    client.updateDisplay( KoLFrame.ENABLED_STATE, "Unable to continue BuffBot!");
                    client.cancelRequest();
                    BBLog.BBLogEntry("Unable to process a buff message.");
                    return;
                }
                
                // clear it out of the inbox
                inbox.remove(firstmsg);
                
            }
            
                       
            // otherwise sleep for a while and then try again
            // (don't go away for more than 1 second    
            for(int i = 1 ; i <= BBSLEEPCOUNT; i = i + 1)
                if (client.permitsContinue())
                    KoLRequest.delay(BBSLEEPTIME);
                else return;
        }
    }
    
    boolean BBProcessMessage(KoLMailMessage myMsg ){
        String meatSentTxt, myMsgHTML;
        int meatSent;
        BuffBotFrame.buffDescriptor buffEntry;
        boolean buffRequestFound = false;
        
        myMsgHTML = myMsg.getMessageHTML().replaceAll(",","");
		Matcher meatMatcher = Pattern.compile( ">You gain \\d+ Meat" ).matcher( myMsgHTML );
        if (meatMatcher.find()){
            meatSentTxt = meatMatcher.group();
            meatSentTxt = meatSentTxt;
            Matcher meatAmtMatcher = Pattern.compile("\\d+").matcher( meatSentTxt);
            if (meatAmtMatcher.find()){
                meatSentTxt = meatAmtMatcher.group();
                meatSent = Integer.parseInt(meatSentTxt);
            }
            else //This should never happen
                meatSent = -1;
            
            //look for this amount in the buff table
            int myIndex = 0;
            while ((myIndex < buffCostTable.size()) && !buffRequestFound){
                buffEntry = (BuffBotFrame.buffDescriptor) buffCostTable.get(myIndex);
                // Look for a match of both buffCost and buffCost2 
                if (meatSent == buffEntry.buffCost){
                    // We have a genuine buff request, so do it!
                    buffRequestFound = true;
                    if (! castThatBuff(myMsg.getSenderName(), buffEntry, buffEntry.buffCastCount)){
                        return false;
                    }
                }
                else if (meatSent == buffEntry.buffCost2) {
                    // We have a genuine buff request, so do it!
                    buffRequestFound = true;
                    if (! castThatBuff(myMsg.getSenderName(), buffEntry, buffEntry.buffCastCount2)){
                        return false;
                    }
                }
                myIndex++;
            }
            
        }
        //Now, either save or delete the message.
        String msgDisp = ((!buffRequestFound) && saveNonBuffmsgs ? "save" : "delete");
        if (!buffRequestFound) {
            BBLog.BBLogEntry("Received non-buff message from [" + myMsg.getSenderName() + "] Action: " + msgDisp);
        }
        (new dropMsgRequest(client, msgDisp, myMsg)).run();
        return true;
    }

    private boolean castThatBuff(String bufftarget, BuffBotFrame.buffDescriptor buffEntry,
                                int num2cast){
        // Figure out how much MP the buff will take,
        // and then identify the number of casts per request that this
        // character can handle.
        
        int buffID = buffEntry.buffID;
        int totalCasts = num2cast;
        int MPperCast = ClassSkillsDatabase.getMPConsumptionByID( buffID );
        int castsPerEvent = Math.min(totalCasts, (me.getMaximumMP())/(MPperCast));
        int MPperEvent = MPperCast * castsPerEvent;
        
        while (totalCasts > 0){
            if (me.getCurrentMP() < MPperEvent){
                //time to buff up
                if (! recoverMP(MPperEvent)) return false;
            }
            (new castBuffRequest(client, buffEntry.buffName, bufftarget, castsPerEvent)).run();
            totalCasts = totalCasts - castsPerEvent;
        }
        BBLog.BBLogEntry("Cast " + buffEntry.buffName + ", " + num2cast + " times on " + bufftarget + ".");
        return true;
    }
    
    private boolean recoverMP(int MPNeeded){
        final int phonicsMP = 46, tinyhouseMP = 20, beanbagMP = 80;
        int num2use;
        AdventureResult itemUsed;
        
        int currentMP = me.getCurrentMP();
        int maxMP = me.getMaximumMP();
        int MPShort = maxMP - currentMP;
        
        //First try resting in the beanbag chair
        // TODO - implement beanbag chair recovery
        
        // try to get there using phonics downs 
        // always buff as close to maxMP as possible, in order to
        //        go as easy on the server as possible
        if (MPRestoreSetting.equals("Phonics & Houses") | MPRestoreSetting.equals("Phonics Only")){
            num2use = 1 + ((MPShort - 1) / phonicsMP);
            itemUsed = new AdventureResult( "phonics down", 0 - num2use);
            int itemIndex = client.getInventory().indexOf(itemUsed  );
            if  ( itemIndex > -1 ){
                num2use = Math.min(num2use, ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
                if (num2use > 0){
                    BBLog.BBLogEntry("Consuming " + num2use + " phonics downs.");
                    (new ConsumeItemRequest( client, ConsumeItemRequest.CONSUME_MULTIPLE, new AdventureResult( itemUsed.getItemID(), num2use ) )).run();
                    currentMP = me.getCurrentMP();
                    if (currentMP >= MPNeeded) return true;
                }
            }
        }
        // try to get there using tiny houses
        if (MPRestoreSetting.equals("Phonics & Houses") | MPRestoreSetting.equals("Tiny Houses Only")){
            MPShort = maxMP - currentMP;
            num2use = 1 + ((MPShort - 1) / tinyhouseMP);
            itemUsed = new AdventureResult( "tiny house", 0 - num2use);
            int itemIndex = client.getInventory().indexOf( itemUsed );
            if  ( itemIndex > -1 ){
                num2use = Math.min(num2use, ((AdventureResult)client.getInventory().get( itemIndex )).getCount() );
                if (num2use > 0){
                    BBLog.BBLogEntry("Consuming " + num2use + " tiny houses.");
                    (new ConsumeItemRequest( client, ConsumeItemRequest.CONSUME_MULTIPLE, new AdventureResult( itemUsed.getItemID(), num2use ) )).run();
                    if (me.getCurrentMP() >= MPNeeded) return true;
                }
            }
        }
        
        BBLog.BBLogEntry("Unable to acquire enough MP!");
        return false;
    }
    private class castBuffRequest extends KoLRequest {
        private int consumedMP;
        private String target;
        private String buffName;
        
        /**   Creates a new instance of <code>castBuffRequest</code>
         *    to do a single instance of a buff
         *    skillName and target are validated by the caller
         * @param	client	The client to be notified of completion
         * @param	buffName	The name of the buff to be used
         * @param	target	The name of the target of the skill
         * @param	buffCount	The number of times the target is affected by this skill
         */
        public castBuffRequest(KoLmafia client, String buffName, String target, int buffCount ) {
            super( client, "skills.php");
            addFormField( "action", "Skillz." );
            addFormField( "pwd", client.getPasswordHash() );
            
            this.buffName = buffName;
            this.target = target;
            int skillID = ClassSkillsDatabase.getSkillID( buffName.replaceFirst( "ñ", "&ntilde;" ) );
            addFormField( "whichskill", "" + skillID );
            addFormField( "bufftimes", "" + buffCount );
            addFormField( "specificplayer", target );
            this.consumedMP = ClassSkillsDatabase.getMPConsumptionByID( skillID ) * buffCount;
            
        }
        
        public void run(){
            updateDisplay( KoLFrame.DISABLED_STATE, "Casting " + buffName + " on user: " + target);
            super.run();
            
            // If it does not notify you that you didn't have enough mana points,
            // then the skill was successfully used.
            
            if ( replyContent == null || replyContent.indexOf( "You don't have enough" ) != -1 ) {
                updateDisplay( KoLFrame.ENABLED_STATE, "You don't have enough mana." );
                client.cancelRequest();
                return;
            } else if ( replyContent.indexOf( "Invalid target" ) != -1 ) {
                updateDisplay( KoLFrame.ENABLED_STATE, "Invalid target: " + target );
                client.cancelRequest();
                return;
            }
            
            client.processResult( new AdventureResult( AdventureResult.MP, 0 - consumedMP ) );
            processResults( replyContent.replaceFirst(
                    "</b><br>\\(duration: ", " (" ).replaceFirst( " Adventures", "" ) );
            client.applyRecentEffects();
                        
        }
        
    }
    
   private class dropMsgRequest extends KoLRequest {
       String msgID, msgDisp, msgSender;
               
        public dropMsgRequest(KoLmafia client, String msgDisp, 
                KoLMailMessage myMsg){
            super(client, "messages.php");
            
            this.msgDisp = msgDisp;
            addFormField( "action", msgDisp);
            addFormField( "pwd", client.getPasswordHash() );
            msgID = myMsg.getMessageID();
            addFormField( msgID, "on");
			msgSender = myMsg.getSenderName();
        }
        
        public void run(){
            updateDisplay( KoLFrame.DISABLED_STATE, "Removing message from " + msgSender + ", dispensation =" + msgDisp);
            super.run();
                        
            if ( replyContent == null || replyContent.indexOf( "Invalid" ) != -1 ) {
                updateDisplay( KoLFrame.ENABLED_STATE, "Unable to drop" + msgID );
                BBLog.BBLogEntry("Unable to drop" + msgID);
                client.cancelRequest();
                return;
            }
            updateDisplay( KoLFrame.DISABLED_STATE, "");
        }
        
    }
   

}
