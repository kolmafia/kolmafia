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

// layout
import java.awt.Dimension;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Font;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// containers
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

// utilities
import java.util.Properties;
import java.text.ParseException;

/**
 * An extension of <code>KoLFrame</code> which acts as a buffbot in Kingdom of Loathing.
 * This retrieves all messages from the inbox, and, for those messages which are buff
 * requests, buffs the sender. It also maintains the MP level using phonics downs.
 */

public class BuffBotFrame extends KoLFrame {
    private JTabbedPane tabs;
    private JPanel buffoptions, mainBuff;
    private LockableListModel BuffCostTable = new LockableListModel();
    private JComboBox NonBuffMsgSave;
    private JComboBox MPRestoreSelect;
    private LimitedSizeChatBuffer buffbotLog;

    /**
     * A data class <CODE>buffDescriptor</CODE>to handle choices for each buff type.
     * Individual elements:
     * @buffCost Cost of this buff (1st column) in meat.
     * @buffCastCount Number of times to cast this buff (1st column)
     * @buffCost2 Cost of this buff (2nd column) in meat.
     * @buffCastCount2 Number of times to cast this buff (2nd column)
     * @buffCost2, @buffCount2txt Text versions of above
     */
    public class buffDescriptor {
        public int buffCost;
        public int buffCastCount, buffID;
        public String buffName;
        public JTextField buffCostTxt, buffCountTxt;
        public int buffCost2, buffCastCount2;
        public JTextField buffCost2Txt, buffCount2Txt;
        /**
         * Constructor for the @buffDescriptor class.
         * @param skillName String description of the buff
         * @param skillID KoL ID for this buff
         */
        public buffDescriptor(String skillName, int skillID){
            buffName = skillName;
            buffID = skillID;
            buffCastCount = buffCost = 0;
            buffCostTxt = new JTextField("0", 6);
            buffCountTxt = new JTextField("0", 5);
            buffCastCount2 = buffCost2 = 0;
            buffCost2Txt = new JTextField("0", 6);
            buffCount2Txt = new JTextField("0", 5);
        }
    }

    /**
     * Constructs a new <code>BuffBotFrame</code> and inserts all
     * of the necessary panels into a tabular layout for accessibility.
     *
     * @param	client	The client to be notified in the event of error.
     */

    public BuffBotFrame( KoLmafia client ) {
        super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
                " (BuffBot)", client );

        setResizable( false );

        //Initialize the display log buffer and the file log
        if (client == null){
            buffbotLog = new BuffBotHome(null).getLog();
        } else {
            client.initializeBuffBot();
            buffbotLog = client.getBuffBotLog();
        }

        tabs = new JTabbedPane();
        mainBuff = new MainBuffPanel();
        buffoptions = new BuffOptionsPanel();

        tabs.addTab( "Run BuffBot", mainBuff );
        tabs.addTab( "BuffBot Options", buffoptions );

        getContentPane().setLayout( new CardLayout( 5, 5 ) );
        getContentPane().add( tabs, " " );
        addWindowListener( new ReturnFocusAdapter() );
        setDefaultCloseOperation( HIDE_ON_CLOSE );

		addWindowListener( new DisableBuffBotAdapter() );

        /* addMenuBar();*/
    }

    //    	TODO private void addMenuBar()
    //	{
    //		JMenuBar menuBar = new JMenuBar();
    //		this.setJMenuBar( menuBar );
    //
    //		JMenu fileMenu = new JMenu("File");
    //		fileMenu.setMnemonic( KeyEvent.VK_F );
    //		menuBar.add( fileMenu );
    //
    //		JMenuItem refreshItem = new JMenuItem( "Refresh Lists", KeyEvent.VK_R );
    //		refreshItem.addActionListener( new ListRefreshListener() );
    //		fileMenu.add( refreshItem );
    //
    //		addHelpMenu( menuBar );
    //	}
    //
    /**
     * Auxilary method used to enable and disable a frame.  By default,
     * this attempts to toggle the enable/disable status on all tabs.
     *
     * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
     */

    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        mainBuff.setEnabled( isEnabled );
        buffoptions.setEnabled( isEnabled );
    }

    /**
     * Internal class used to handle everything related to
     * operating the buffbot. This is the <CODE>mainBuffPanel</CODE>
     */

    private class MainBuffPanel extends JPanel {

        private NonContentPanel buffbotResultsPanel;
        private JLabel buffbotStatusLabel;

        /**
         * Constructor for <CODE>MainBuffPanel</CODE>
         */
        public MainBuffPanel( ) {

            JPanel panel = new JPanel();
            panel.setLayout( new BorderLayout( 10, 10 ) );

            buffbotResultsPanel = new BuffBotResultsPanel( );

            panel.add( buffbotResultsPanel, BorderLayout.SOUTH );

            add( panel, " " );

        }



        public void setStatusMessage( String s ) {
            buffbotStatusLabel.setText( s );
        }

        public void setEnabled( boolean isEnabled ) {
            super.setEnabled( isEnabled );
            buffbotResultsPanel.setEnabled(isEnabled);
        }

        public void clear() {
        }

        public void requestFocus() {

        }


        /**
         * An internal class which represents the panel used for tallying the
         * results in the <code>BuffBotFrame</code>.  Note that all of the
         * tallying functionality is handled by the <code>LockableListModel</code>
         * provided, so this functions as a container for that list model.
         */

        private class BuffBotResultsPanel extends NonContentPanel {
            private JTextField BuffResultsField;
            private JEditorPane BuffResultsDisplay;


            private LockableListModel usableItems;

            /**
             * COnstructor for <CODE>BuffBotResults</CODE> class.
             */
            public BuffBotResultsPanel( ) {
                // TODO change the Stop to something useful - done, now test
                super( "Start", "Clear Display" );
                // Set content for "Label Preceeding & both disabled on click
                setContent( null, null, null, null, true, true );

				JEditorPane buffbotLogDisplay = new JEditorPane();
				buffbotLog.setChatDisplay( buffbotLogDisplay );

                JScrollPane scrollArea = new JScrollPane( buffbotLogDisplay,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

                add( JComponentUtilities.createLabel( "BuffBot Activities", JLabel.CENTER,
                        Color.black, Color.white ), BorderLayout.NORTH );

                JComponentUtilities.setComponentSize( scrollArea, 400, 200 );
                add( scrollArea, BorderLayout.WEST );
            }
            public void clear( ) {
            }

            /**
             * Action based on user pushing <B>Run</B>.
             */
            protected void actionConfirmed() {
                (new BuffBotRequestThread()).start();
            }

            /**
             * Action, based on user selecting <B>Clear Display</B>
             */
            protected void actionCancelled() {
                buffbotLog.clearBuffer();
            }
        }
        /**
         * In order to keep the user interface from freezing (or at
         * least appearing to freeze), this internal class is used
         * to actually make the adventuring requests.
         */

        private class BuffBotRequestThread extends Thread {

            public BuffBotRequestThread() {
                super( "BuffBot-Request-Thread" );

                setDaemon( true );

            }

            public void run() {
                LockableListModel costList = new LockableListModel();
                LockableListModel buffIndexList = new LockableListModel();

                int buffSkillIndex, buffCostIndex;
                buffDescriptor buffEntry;
                boolean duplicates;


                // First, make sure there is a valid set of buff parameters
                // TODO - make a tree map with cost as index for matching to meat amounts
                duplicates = false;
                for (buffSkillIndex = 0; buffSkillIndex < BuffCostTable.size(); buffSkillIndex++){
                    buffEntry = (buffDescriptor) BuffCostTable.get(buffSkillIndex);
                    if (buffEntry.buffCost > 0 && buffEntry.buffCastCount > 0){
                        //Make sure the buffcost is unique:
                        if(!costList.contains(buffEntry.buffCostTxt))
                            costList.add(buffEntry.buffCostTxt);
                        else
                            duplicates = true;
                    }
                    if (buffEntry.buffCost2 > 0 && buffEntry.buffCastCount2 > 0){
                        //Make sure the buffcost2 is also unique:
                        if(!costList.contains(buffEntry.buffCost2Txt))
                            costList.add(buffEntry.buffCost2Txt);
                        else
                            duplicates = true;
                    }
                }

                if (costList.isEmpty())
                    JOptionPane.showMessageDialog(null,"No Valid Buff Table Entries!");
                else if (duplicates)
                    JOptionPane.showMessageDialog(null,"Duplicate Buff Cost Entries!");
                else   {
                    client.updateDisplay( ENABLED_STATE, "Buffbotting started." );
                    buffbotLog.append("Starting a new session.<br>\n");
                    client.resetContinueState();

                    (new BuffBotManager(client, BuffCostTable)).runBuffBot();
                    buffbotLog.append("BuffBot Terminated.<br>\n");
					client.updateDisplay( ENABLED_STATE, "BuffBot stopped." );

                }
            }
        }

    }


    /**
     * Internal class used to handle everything related to
     * Buff Option management
     */

    private class BuffOptionsPanel extends JPanel {
        private BuffOptionMgr TopPanel;
        private BuffListPanel BottomPanel;


        public BuffOptionsPanel() {

            JPanel panel = new JPanel();
            panel.setLayout( new BorderLayout() );

            TopPanel = new BuffOptionMgr();
            panel.add(TopPanel,BorderLayout.NORTH);

            BottomPanel = new BuffListPanel();
            panel.add(BottomPanel,BorderLayout.SOUTH);

            add( panel, " " );
        }

        public void setEnabled( boolean isEnabled ) {
            super.setEnabled( isEnabled );
            TopPanel.setEnabled(isEnabled);
            BottomPanel.setEnabled(isEnabled);
        }

        private class BuffOptionMgr extends KoLPanel {
            protected Properties settings;

            public BuffOptionMgr( ) {
                super("Apply", "Restore", new Dimension( 120, 20 ),  new Dimension( 200, 20 ));


                settings = (client == null) ? System.getProperties() : client.getSettings();
                LockableListModel msgDisposalSetting = new LockableListModel();
                msgDisposalSetting.add( "Delete Non-buff requests");
                msgDisposalSetting.add( "Save Non-buff requests");
                NonBuffMsgSave = new JComboBox( msgDisposalSetting );

                LockableListModel MPRestoreChoices = new LockableListModel();
                MPRestoreChoices.add( "Phonics & Houses" );
                MPRestoreChoices.add( "Phonics Only");
                MPRestoreChoices.add( "Tiny Houses Only");

                MPRestoreSelect = new JComboBox( MPRestoreChoices );
                VerifiableElement [] elements = new VerifiableElement[2];
                elements[0] = new VerifiableElement( "Message Disposal", NonBuffMsgSave );
                elements[1] = new VerifiableElement( "MP Restore Options ", MPRestoreSelect);

                setContent( elements, null, null, null, true, true );
                clear();
            }

            public void setEnabled( boolean isEnabled ) {
                super.setEnabled( isEnabled );
                NonBuffMsgSave.setEnabled(isEnabled);
                MPRestoreSelect.setEnabled(isEnabled);

            }

            public void setStatusMessage(int status, String Msg){

            }

            public void clear(){
                (new LoadDefaultSettingsThread()).start();
            }

            public void actionCancelled(){
                clear();
            }

            protected void actionConfirmed(){
                (new StoreSettingsThread()).start();
            }
            private class LoadDefaultSettingsThread extends Thread {
                public void run() {
                    int buffSkillIndex;
                    buffDescriptor buffEntry;
                    String tempString;

                    String msgDisposalSetting = settings.getProperty( "NonBuffMsgSave" );
                    String MPRestoreSetting = settings.getProperty( "MPRestoreSelect" );

                    if ( msgDisposalSetting == null || msgDisposalSetting.equals( "false" ) )
                        NonBuffMsgSave.setSelectedIndex( 0 );
                    else
                        NonBuffMsgSave.setSelectedIndex( 1 );
                    if ( MPRestoreSetting == null )
                        MPRestoreSelect.setSelectedIndex( 0 );
                    else
                        MPRestoreSelect.setSelectedItem( MPRestoreSetting );

                    for (buffSkillIndex = 0; buffSkillIndex < BuffCostTable.size(); buffSkillIndex++){
                        buffEntry = (buffDescriptor) BuffCostTable.get(buffSkillIndex);
                        tempString = settings.getProperty( "BuffCost#"+buffEntry.buffID);
                        buffEntry.buffCostTxt.setText((tempString == null)? "0": tempString);
                        tempString = settings.getProperty( "BuffCount#"+buffEntry.buffID);
                        buffEntry.buffCountTxt.setText((tempString == null)? "0": tempString);
                        buffEntry.buffCost = Integer.parseInt(buffEntry.buffCostTxt.getText());
                        buffEntry.buffCastCount = Integer.parseInt(buffEntry.buffCountTxt.getText());

                        // now the same again for the #2 fields
                        tempString = settings.getProperty( "BuffCost2#"+buffEntry.buffID);
                        buffEntry.buffCost2Txt.setText((tempString == null)? "0": tempString);
                        tempString = settings.getProperty( "BuffCount2#"+buffEntry.buffID);
                        buffEntry.buffCount2Txt.setText((tempString == null)? "0": tempString);
                        buffEntry.buffCost2 = Integer.parseInt(buffEntry.buffCost2Txt.getText());
                        buffEntry.buffCastCount2 = Integer.parseInt(buffEntry.buffCount2Txt.getText());

                        BuffCostTable.set(buffSkillIndex, buffEntry);
                    }

                    setStatusMessage( ENABLED_STATE, "" );

                }
            }
            private class StoreSettingsThread extends Thread {
                public void run() {
                    int buffSkillIndex;
                    buffDescriptor buffEntry;

                    settings.setProperty( "NonBuffMsgSave", "" + (NonBuffMsgSave.getSelectedIndex() == 1) );
                    settings.setProperty( "MPRestoreSelect", "" + (MPRestoreSelect.getSelectedItem()) );
                    for (buffSkillIndex = 0; buffSkillIndex < BuffCostTable.size(); buffSkillIndex++){
                        buffEntry = (buffDescriptor) BuffCostTable.get(buffSkillIndex);
                        settings.setProperty( "BuffCost#"+buffEntry.buffID, buffEntry.buffCostTxt.getText());
                        settings.setProperty( "BuffCount#"+buffEntry.buffID, buffEntry.buffCountTxt.getText());
                        buffEntry.buffCost = Integer.parseInt(buffEntry.buffCostTxt.getText());
                        buffEntry.buffCastCount = Integer.parseInt(buffEntry.buffCountTxt.getText());

                        // Now do the #2 fields:
                        settings.setProperty( "BuffCost2#"+buffEntry.buffID, buffEntry.buffCost2Txt.getText());
                        settings.setProperty( "BuffCount2#"+buffEntry.buffID, buffEntry.buffCount2Txt.getText());
                        buffEntry.buffCost2 = Integer.parseInt(buffEntry.buffCost2Txt.getText());
                        buffEntry.buffCastCount2 = Integer.parseInt(buffEntry.buffCount2Txt.getText());
                        BuffCostTable.set(buffSkillIndex, buffEntry);
                    }

                    if ( settings instanceof KoLSettings )
                        ((KoLSettings)settings).saveSettings();
                    setStatusMessage( ENABLED_STATE, "Settings saved." );

                    KoLRequest.delay( 5000 );
                    setStatusMessage( ENABLED_STATE, "" );
                }
            }


        }
        private class BuffListPanel extends JPanel {


            public BuffListPanel( ) {
                LockableListModel skillset = new LockableListModel();
                String skill;
                int skillID;
                JLabel skillLabel;
                buffDescriptor buffEntry;

                this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
                JPanel panel = new JPanel();

                skillset = (client == null) ? new LockableListModel() : client.getCharacterData().getAvailableSkills();
//                if (client == null){// Dummy data for UI debug
//                    skillset.add("Cletus's Canticle of Celerity");
//                    skillset.add("Jackasses' Symphony of Destruction");
//                    skillset.add("The Power Ballad of the Arrowsmith");
//                    skillset.add("The Magical Mojomuscular Melody");
//                    skillset.add("Brawnee's Anthem of Absorption");
//                    skillset.add("The Psalm of Pointiness");
//                    skillset.add("Stevedave's Shanty of Superiority");
//                    skillset.add("The Ode to Booze");
//                }

                JPanel ltPanel = new JPanel(new GridLayout(0,1));
                JPanel rtPanel;
				rtPanel = new JPanel(new GridLayout(0,4,5,5));
				ltPanel.add(new JLabel("Buff Title",JLabel.CENTER));
				rtPanel.add(new JLabel("Cost #1"));
				rtPanel.add(new JLabel("# Casts"));
				rtPanel.add(new JLabel("Cost #2"));
				rtPanel.add(new JLabel("# Casts"));

				int skillIndex = 0;
				while ((skill = (String) skillset.get(skillIndex)) != null){
					skillID = ClassSkillsDatabase.getSkillID( skill.replaceFirst( "ñ", "&ntilde;" ) );
					if (ClassSkillsDatabase.isBuff( skillID )){
						buffEntry = new buffDescriptor(skill, skillID);
						skillLabel = new JLabel(skill,JLabel.TRAILING);
						skillLabel.setFont(new Font("Serif", Font.PLAIN, 10));

						ltPanel.add(skillLabel);
						rtPanel.add(buffEntry.buffCostTxt);
						rtPanel.add(buffEntry.buffCountTxt);
						rtPanel.add(buffEntry.buffCost2Txt);
						rtPanel.add(buffEntry.buffCount2Txt);
						panel.add(rtPanel);
						BuffCostTable.add(buffEntry);

					}

					skillIndex++;
				}
                // Special message for the non-buffers
				if (BuffCostTable.isEmpty()){
                    panel = new JPanel(new GridLayout(0,1,5,5));
                    panel.add(new JLabel("You have no buffing skills!"));
					add(panel);
				} else {
					//Force the left panel to have the same height as the right one:
					ltPanel.setPreferredSize(new Dimension(ltPanel.getPreferredSize().width, 5+rtPanel.getPreferredSize().height));
					panel.add(ltPanel);
					panel.add(rtPanel);
					add(panel);
				}
            }
        }
    }

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	private class DisableBuffBotAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null )
				(new DisableBuffBotThread()).start();
		}

		private class DisableBuffBotThread extends Thread
		{
			public DisableBuffBotThread()
			{	setDaemon( true );
			}

			public void run()
			{	client.deinitializeBuffBot();
			}
		}
	}

    /**
     * The main method used in the event of testing the way the
     * user interface looks.  This allows the UI to be tested
     * without having to constantly log in and out of KoL.
     */

    public static void main( String [] args ) {
        KoLFrame uitest = new BuffBotFrame( null);
        uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
    }
}


