/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

public class AddCustomDeedsPanel
	extends JPanel
{
	public static CardLayoutSelectorPanel selectorPanel;

	private JTextField boolField1;
	private JTextField boolField2;
	private JTextField boolField3;
	private JTextField multiField1;
	private JTextField multiField2;
	private JTextField multiField3;
	private JTextField multiField4;
	private JTextField itemField1;
	private JTextField itemField2;
	private JTextField itemField3;
	private JTextField skillField1;
	private JTextField skillField2;
	private JTextField skillField3;
	private JTextField skillField4;
	private JTextField textField;

	private static final int BOOL_FIELD_1 = 0;
	private static final int BOOL_FIELD_2 = 1;
	private static final int BOOL_FIELD_3 = 2;
	private static final int MULTI_FIELD_1 = 3;
	private static final int MULTI_FIELD_2 = 4;
	private static final int MULTI_FIELD_3 = 5;
	private static final int MULTI_FIELD_4 = 6;
	private static final int ITEM_FIELD_1 = 7;
	private static final int ITEM_FIELD_2 = 8;
	private static final int ITEM_FIELD_3 = 9;
	private static final int SKILL_FIELD_1 = 10;
	private static final int SKILL_FIELD_2 = 11;
	private static final int SKILL_FIELD_3 = 12;
	private static final int SKILL_FIELD_4 = 13;
	private static final int TEXT_FIELD = 14;

	private JLabel boolLabel1;
	private JLabel boolLabel2;
	private JLabel boolLabel3;
	private JLabel multiLabel1;
	private JLabel multiLabel2;
	private JLabel multiLabel3;
	private JLabel multiLabel4;
	private JLabel itemLabel1;
	private JLabel itemLabel2;
	private JLabel itemLabel3;
	private JLabel skillLabel1;
	private JLabel skillLabel2;
	private JLabel skillLabel3;
	private JLabel skillLabel4;

	private static final int BOOL_LABEL_1 = 0;
	private static final int BOOL_LABEL_2 = 1;
	private static final int BOOL_LABEL_3 = 2;
	private static final int MULTI_LABEL_1 = 3;
	private static final int MULTI_LABEL_2 = 4;
	private static final int MULTI_LABEL_3 = 5;
	private static final int MULTI_LABEL_4 = 6;
	private static final int ITEM_LABEL_1 = 7;
	private static final int ITEM_LABEL_2 = 8;
	private static final int ITEM_LABEL_3 = 9;
	private static final int SKILL_LABEL_1 = 10;
	private static final int SKILL_LABEL_2 = 11;
	private static final int SKILL_LABEL_3 = 12;
	private static final int SKILL_LABEL_4 = 13;

	private ThreadedButton boolButton;
	private ThreadedButton multiButton;
	private ThreadedButton itemButton;
	private ThreadedButton skillButton;
	private ThreadedButton textDeedButton;
	private ThreadedButton addTextButton;

	private JTextArea textArea;
	private ArrayList textDeed = new ArrayList();

	public AddCustomDeedsPanel()
	{
		buildCustomDeed();
	}

	private void buildCustomDeed()
	{
		AddCustomDeedsPanel.selectorPanel = new CardLayoutSelectorPanel( "", "ABCDEFGHIJKLM" );
		AddCustomDeedsPanel.selectorPanel.addCategory( "Custom Deeds" );

		addBooleanPrefDeed();
		addMultiPrefDeed();
		addBooleanItemDeed();
		addSkillDeed();
		addTextDeed();
	}

	private void addBooleanPrefDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		this.boolField1 = new JTextField( 25 );
		this.boolField2 = new JTextField( 25 );
		this.boolField3 = new JTextField( 25 );
		this.boolLabel1 = new JLabel( "required" );
		this.boolLabel1.setToolTipText( "The text to display on the button." );
		this.boolLabel2 = new JLabel( "required" );
		this.boolLabel2.setToolTipText( "The preference that the button will track. "
			+ "You must provide a boolean preference." );
		this.boolLabel3 = new JLabel( "(optional)" );
		this.boolLabel3.setToolTipText( "If a command is not specified, defaults to displayText." );
		this.boolButton = new ThreadedButton( "add deed" );

		this.boolField1.getDocument().addDocumentListener( new BoolField1Listener() );
		this.boolField2.getDocument().addDocumentListener( new BoolField2Listener() );
		this.boolButton.addActionListener( new BooleanPrefActionListener() );

		title.add( new JLabel( "Adding Boolean Preference." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "command:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( this.boolField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( this.boolField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( this.boolField3, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( this.boolLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( this.boolLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( this.boolLabel3, c );

		c.gridx = 0;
		c.gridy = 4;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 4;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( this.boolButton, c );
		this.boolButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Boolean Pref", panel );
	}

	private void addMultiPrefDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		this.multiField1 = new JTextField( 25 );
		this.multiField2 = new JTextField( 25 );
		this.multiField3 = new JTextField( 25 );
		this.multiField4 = new JTextField( 25 );
		this.multiLabel1 = new JLabel( "required" );
		this.multiLabel1.setToolTipText( "The text to display on the button." );
		this.multiLabel2 = new JLabel( "required" );
		this.multiLabel2.setToolTipText( "The preference that the button will track. "
			+ "You must provide an integer preference." );
		this.multiLabel3 = new JLabel( "required" );
		this.multiLabel3.setToolTipText( "The command that the button will execute.  Required." );
		this.multiLabel4 = new JLabel( "required" );
		this.multiLabel4
			.setToolTipText( "Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
		this.multiButton = new ThreadedButton( "add deed" );

		this.multiField1.getDocument().addDocumentListener( new MultiField1Listener() );
		this.multiField2.getDocument().addDocumentListener( new MultiField2Listener() );
		this.multiField3.getDocument().addDocumentListener( new MultiField3Listener() );
		this.multiField4.getDocument().addDocumentListener( new MultiField4Listener() );
		this.multiButton.addActionListener( new MultiActionListener() );

		title.add( new JLabel( "Adding Multi Preference." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "command:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxUses:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( this.multiField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( this.multiField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( this.multiField3, c );
		c.gridx = 1;
		c.gridy = 4;
		textPanel.add( this.multiField4, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( this.multiLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( this.multiLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( this.multiLabel3, c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( this.multiLabel4, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( this.multiButton, c );
		this.multiButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Multi Pref", panel );
	}

	private void addBooleanItemDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		this.itemField1 = new JTextField( 25 );
		this.itemField2 = new JTextField( 25 );
		this.itemField3 = new JTextField( 25 );
		this.itemLabel1 = new JLabel( "required" );
		this.itemLabel1.setToolTipText( "The text to display on the button." );
		this.itemLabel2 = new JLabel( "required" );
		this.itemLabel2.setToolTipText( "The preference that the button will track. "
			+ "You must provide a boolean preference." );
		this.itemLabel3 = new JLabel( "(optional)" );
		this.itemLabel3
			.setToolTipText( "If an item is not specified, defaults to displayText.  Uses fuzzy matching." );
		this.itemButton = new ThreadedButton( "add deed" );

		this.itemField1.getDocument().addDocumentListener( new ItemField1Listener() );
		this.itemField2.getDocument().addDocumentListener( new ItemField2Listener() );
		this.itemField3.getDocument().addDocumentListener( new ItemField1Listener() );
		// listener 1 sets the state of both label1 and label3
		this.itemButton.addActionListener( new ItemPrefActionListener() );

		title.add( new JLabel( "Adding Boolean Item." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "item:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( this.itemField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( this.itemField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( this.itemField3, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( this.itemLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( this.itemLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( this.itemLabel3, c );

		c.gridx = 0;
		c.gridy = 4;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 4;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( this.itemButton, c );
		this.itemButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Item", panel );
	}

	private void addSkillDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		this.skillField1 = new JTextField( 25 );
		this.skillField2 = new JTextField( 25 );
		this.skillField3 = new JTextField( 25 );
		this.skillField4 = new JTextField( 25 );
		this.skillLabel1 = new JLabel( "required" );
		this.skillLabel1.setToolTipText( "The text to display on the button." );
		this.skillLabel2 = new JLabel( "required" );
		this.skillLabel2.setToolTipText( "The preference that the button will track. "
			+ "You need to provide a boolean or an integer preference." );
		this.skillLabel3 = new JLabel( "(optional)" );
		this.skillLabel3.setToolTipText( "The skill that the button will cast." );
		this.skillLabel4 = new JLabel( "(set pref)" );
		this.skillLabel4.setToolTipText( "Set the preference first to determine if this is needed." );
		this.skillButton = new ThreadedButton( "add deed" );

		this.skillField1.getDocument().addDocumentListener( new SkillField1Listener() );
		this.skillField2.getDocument().addDocumentListener( new SkillField2Listener() );
		// listener 1 sets the state of both label1 and label3
		this.skillField3.getDocument().addDocumentListener( new SkillField1Listener() );
		this.skillField4.getDocument().addDocumentListener( new SkillField4Listener() );
		this.skillButton.addActionListener( new SkillActionListener() );

		title.add( new JLabel( "Adding Skill Deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "skill:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxCasts:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( this.skillField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( this.skillField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( this.skillField3, c );
		c.gridx = 1;
		c.gridy = 4;
		textPanel.add( this.skillField4, c );
		this.skillField4.setEnabled( false );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( this.skillLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( this.skillLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( this.skillLabel3, c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( this.skillLabel4, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( this.skillButton, c );
		this.skillButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Skill", panel );
	}

	private void addTextDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		this.textField = new JTextField( 25 );
		this.textArea = new JTextArea();
		this.textArea.setColumns( 10 );
		this.textArea.setRows( 4 );
		this.textArea.setMaximumSize( this.textArea.getPreferredSize() );
		this.textArea.setBorder( BorderFactory.createLoweredBevelBorder() );
		this.textArea.setLineWrap( true );
		this.textArea.setWrapStyleWord( true );
		this.textArea.setEditable( false );
		this.textArea.setOpaque( false );
		this.textArea.setFont( KoLConstants.DEFAULT_FONT );
		this.textDeedButton = new ThreadedButton( "add deed" );
		this.addTextButton = new ThreadedButton( "add text" );
		ThreadedButton undoButton = new ThreadedButton( "undo" );
		ThreadedButton clearButton = new ThreadedButton( "clear" );

		this.textField.getDocument().addDocumentListener( new TextFieldListener() );
		this.addTextButton.addActionListener( new AddTextListener() );
		undoButton.addActionListener( new RemoveLastTextListener() );
		clearButton.addActionListener( new ClearTextListener() );
		this.textDeedButton.addActionListener( new TextActionListener() );

		title.add( new JLabel( "Adding Text Deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTH;
		textPanel.add( this.textField, c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		textPanel.add( this.addTextButton, c );
		this.addTextButton.setEnabled( false );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 2;
		c.gridy = 2;
		c.anchor = GridBagConstraints.WEST;
		textPanel.add( undoButton, c );
		c.gridx = 2;
		c.gridy = 3;
		c.anchor = GridBagConstraints.NORTHWEST;
		textPanel.add( clearButton, c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		c.gridheight = 2;
		textPanel.add( this.textArea, c );

		c.gridx = 0;
		c.gridy = 5;
		c.gridheight = 1;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( this.textDeedButton, c );
		this.textDeedButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Text", panel );
	}

	public class BoolField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			String pref = Preferences.getString( getField( BOOL_FIELD_2 ).getText() );
			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( BOOL_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( BOOL_LABEL_2, "required", "The preference that the button will track. "
					+ "You must provide a boolean preference." );
			}
			else if ( pref.equalsIgnoreCase( "false" ) || pref.equalsIgnoreCase( "true" ) )
			{
				setLabel( BOOL_LABEL_2, "OK" );
			}
			else
			{
				setLabel( BOOL_LABEL_2, "BAD", "You need to provide a valid BOOLEAN preference." );
			}

			boolean enabled = getLabel( BOOL_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( BOOL_LABEL_2 ).getText().equalsIgnoreCase( "OK" );

			getBoolButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	private class BoolField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( BOOL_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( BOOL_LABEL_1, "required", "The text to display on the button." );
			}
			else
			{
				setLabel( BOOL_LABEL_1, "OK" );
			}

			boolean enabled = getLabel( BOOL_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( BOOL_LABEL_2 ).getText().equalsIgnoreCase( "OK" );

			getBoolButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class MultiField4Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( MULTI_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( MULTI_LABEL_4, "required",
					"Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
			}
			else
			{
				try
				{
					Integer.parseInt( getField( MULTI_FIELD_4 ).getText() );
					setLabel( MULTI_LABEL_4, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( MULTI_LABEL_4, "BAD", "Integer only, please." );
				}
			}
			boolean enabled = getLabel( MULTI_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_2 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_3 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_4 ).getText().equalsIgnoreCase( "OK" );

			getMultiButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class MultiField3Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent arg0 )
		{
			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( MULTI_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( MULTI_LABEL_3, "required",
					"The command that the button will execute.  Required." );
			}
			else
			{
				setLabel( MULTI_LABEL_3, "OK" );
			}
			boolean enabled = getLabel( MULTI_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_2 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_3 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_4 ).getText().equalsIgnoreCase( "OK" );

			getMultiButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class MultiField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( MULTI_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( MULTI_LABEL_2, "required", "The preference that the button will track. "
					+ "You must provide an integer preference." );
			}
			else
			{
				String pref = Preferences.getString( getField( MULTI_FIELD_2 ).getText() );
				try
				{
					Integer.parseInt( pref );
					setLabel( MULTI_LABEL_2, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( MULTI_LABEL_2, "BAD",
						"You need to provide a valid INTEGER preference." );
				}

			}

			boolean enabled = getLabel( MULTI_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_2 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_3 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_4 ).getText().equalsIgnoreCase( "OK" );

			getMultiButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class MultiField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( MULTI_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( MULTI_LABEL_1, "required", "The text to display on the button." );
			}
			else
			{
				setLabel( MULTI_LABEL_1, "OK" );
			}
			boolean enabled = getLabel( MULTI_LABEL_1 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_2 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_3 ).getText().equalsIgnoreCase( "OK" )
				&& getLabel( MULTI_LABEL_4 ).getText().equalsIgnoreCase( "OK" );

			getMultiButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class ItemField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			String pref = Preferences.getString( getField( ITEM_FIELD_2 ).getText() );
			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( ITEM_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( ITEM_LABEL_2, "required", "The preference that the button will track. "
					+ "You must provide a boolean preference." );
			}
			else if ( pref.equalsIgnoreCase( "false" ) || pref.equalsIgnoreCase( "true" ) )
			{
				setLabel( ITEM_LABEL_2, "OK" );
			}
			else
			{
				setLabel( ITEM_LABEL_2, "BAD", "You need to provide a valid BOOLEAN preference." );
			}
			String label1 = getLabel( ITEM_LABEL_1 ).getText();
			String label2 = getLabel( ITEM_LABEL_2 ).getText();
			String label3 = getLabel( ITEM_LABEL_3 ).getText();

			boolean enabled = ( label1.equals( "OK" ) && label2.equals( "OK" ) && !label3.equals( "BAD" ) );

			getItemButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class ItemField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent arg0 )
		{
			/*
			 * Since the states of field 1 and field 3 depend on each other, set the states of both
			 * whenever one of the fields is changed.
			 * 
			 * State 1: displayText empty, item empty = [ required, (optional) ]
			 * State 2: displayText non-matching, item empty = [ (need item), required ]
			 * State 3: displayText matching, item empty = [ OK, (optional) ]
			 * State 4: displayText empty, item non-matching = [ required, BAD ]
			 * State 5: displayText empty, item matching = [ required, OK ]
			 * State 6: displayText non-empty, item non-matching = [ OK, BAD ]
			 * State 7: displayText non-empty, item matching = [ OK, OK ]
			 * 
			 * To enable the button, we check that label 1 is OK and label 3 not BAD
			 */

			boolean field1empty = getField( ITEM_FIELD_1 ).getText().equalsIgnoreCase( "" );
			boolean field3empty = getField( ITEM_FIELD_3 ).getText().equalsIgnoreCase( "" );
			boolean field1matching = ItemDatabase.getItemId( getField( ITEM_FIELD_1 ).getText() ) != -1;
			boolean field3matching = ItemDatabase.getItemId( getField( ITEM_FIELD_3 ).getText() ) != -1;

			/* State 1 */
			if ( field1empty && field3empty )
			{
				setLabel( ITEM_LABEL_1, "required", "The text to display on the button." );
				setLabel( ITEM_LABEL_3, "(optional)",
					"If an item is not specified, defaults to displayText.  Uses fuzzy matching." );
			}
			/* State 2 */
			else if ( !field1matching && field3empty )
			{
				setLabel( ITEM_LABEL_1, "(need item)",
					"The display text does not match an item, so you need to specify one under item:" );
				setLabel( ITEM_LABEL_3, "required",
					"The display text does not match an item, so you need to specify one." );
			}
			/* State 3 */
			else if ( field1matching && field3empty )
			{
				setLabel(
					ITEM_LABEL_1,
					"OK",
					"Display text matches item: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_1 ).getText() ) ) );
				setLabel( ITEM_LABEL_3, "(optional)",
					"The display text matches an item, so you don't need to specify one here." );
			}
			/* State 4 */
			else if ( field1empty && !field3matching )
			{
				setLabel( ITEM_LABEL_1, "required", "The text to display on the button." );
				setLabel( ITEM_LABEL_3, "BAD",
					"Could not find a matching item for: " + getField( ITEM_FIELD_3 ).getText() );
			}
			/* State 5 */
			else if ( field1empty && field3matching )
			{
				setLabel( ITEM_LABEL_1, "required", "You still need to specify the text to display." );
				setLabel(
					ITEM_LABEL_3,
					"OK",
					"Matching item found: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_3 ).getText() ) ) );
			}
			/* State 6 */
			else if ( !field1empty && !field3matching )
			{
				setLabel( ITEM_LABEL_1, "OK" );
				setLabel( ITEM_LABEL_3, "BAD",
					"Could not find a matching item for: " + getField( ITEM_FIELD_3 ).getText() );
			}
			/* State 7 */
			else if ( !field1empty && field3matching )
			{
				setLabel( ITEM_LABEL_1, "OK" );
				setLabel(
					ITEM_LABEL_3,
					"OK",
					"Matching item found: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_3 ).getText() ) ) );
			}

			String label1 = getLabel( ITEM_LABEL_1 ).getText();
			String label2 = getLabel( ITEM_LABEL_2 ).getText();
			String label3 = getLabel( ITEM_LABEL_3 ).getText();

			boolean enabled = ( label1.equals( "OK" ) && label2.equals( "OK" ) && !label3.equals( "BAD" ) );

			getItemButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField4Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SKILL_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( SKILL_LABEL_4, "required", "Specify an integer to disable the deed at." );
			}
			else
			{
				try
				{
					Integer.parseInt( getField( SKILL_FIELD_4 ).getText() );
					setLabel( SKILL_LABEL_4, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( SKILL_LABEL_4, "BAD", "Integer only, please." );
				}
			}
			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional" ) )
				&& ( label4.equalsIgnoreCase( "OK" ) || label4.equalsIgnoreCase( "(not used)" ) );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField2Listener
		implements DocumentListener
	{
		boolean isBool;
		boolean isInteger;

		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SKILL_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				this.isBool = false;
				this.isInteger = false;
			}
			else
			{
				String pref = Preferences.getString( getField( SKILL_FIELD_2 ).getText() );
				try
				{
					Integer.parseInt( pref );
					this.isBool = false;
					this.isInteger = true;
				}
				catch ( NumberFormatException exception )
				{
					this.isInteger = false;
					if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" ) )
					{
						this.isBool = true;
					}
					else
					{
						this.isBool = false;
					}
				}
			}

			if ( this.isBool )
			{
				setLabel( SKILL_LABEL_2, "OK", "Using a boolean preference." );
				setLabel( SKILL_LABEL_4, "(not used)",
					"This field is only used for integer preferences." );
				getField( SKILL_FIELD_4 ).setEnabled( false );
			}
			else if ( this.isInteger )
			{
				if ( !getField( SKILL_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_1, "OK" );
				}
				setLabel( SKILL_LABEL_2, "OK", "Using an integer preference." );
				if ( getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "required", "You need to specify a valid skill." );
				}
				if ( getField( SKILL_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_4, "required",
						"Specify an integer to disable the deed at." );
				}
				else
				{
					try
					{
						Integer.parseInt( getField( SKILL_FIELD_4 ).getText() );
						setLabel( SKILL_LABEL_4, "OK" );
					}
					catch ( NumberFormatException exception )
					{
						setLabel( SKILL_LABEL_4, "BAD", "Integer only, please." );
					}
				}
				getField( SKILL_FIELD_4 ).setEnabled( true );
			}
			else if ( getField( SKILL_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( SKILL_LABEL_2, "required", "The preference that the button will track. "
					+ "You need to provide a boolean or an integer preference." );
				setLabel( SKILL_LABEL_4, "(set pref)",
					"Set the preference first to determine if this is needed." );
				getField( SKILL_FIELD_4 ).setEnabled( false );
			}
			else
			{
				boolean field1empty = getField( SKILL_FIELD_1 ).getText().equalsIgnoreCase( "" );
				boolean field1matching = SkillDatabase.getMatchingNames(
					getField( SKILL_FIELD_1 ).getText() ).size() == 1;
				boolean field3matching = SkillDatabase.getMatchingNames(
					getField( SKILL_FIELD_3 ).getText() ).size() == 1;
				if ( !field1empty && !field1matching && !field3matching )
				{
					setLabel( SKILL_LABEL_1, "(need skill)",
						"The display text does not match a skill, so you need to specify one under skill:" );
				}
				setLabel( SKILL_LABEL_2, "BAD",
					"Could not find a matching boolean or integer preference." );
				setLabel( SKILL_LABEL_4, "(set pref)",
					"Set a valid preference first to determine if this is needed." );
				getField( SKILL_FIELD_4 ).setEnabled( false );
			}
			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional" ) )
				&& ( label4.equalsIgnoreCase( "OK" ) || label4.equalsIgnoreCase( "(not used)" ) );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField1Listener
		implements DocumentListener
	{
		boolean isBool;
		boolean isInteger;

		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SKILL_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				this.isBool = false;
				this.isInteger = false;
			}
			else
			{
				String pref = Preferences.getString( getField( SKILL_FIELD_2 ).getText() );
				try
				{
					Integer.parseInt( pref );
					this.isBool = false;
					this.isInteger = true;
				}
				catch ( NumberFormatException exception )
				{
					this.isInteger = false;
					if ( pref.equalsIgnoreCase( "true" ) || pref.equalsIgnoreCase( "false" ) )
					{
						this.isBool = true;
					}
					else
					{
						this.isBool = false;
					}
				}
			}
			boolean field1empty = getField( SKILL_FIELD_1 ).getText().equalsIgnoreCase( "" );
			boolean field3empty = getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" );
			boolean field1matching = SkillDatabase.getMatchingNames( getField( SKILL_FIELD_1 ).getText() )
				.size() == 1;
			boolean field3matching = SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() )
				.size() == 1;

			if ( this.isInteger )
			{
				// All fields are required.
				String skillName = (String) ( field3matching ? SkillDatabase.getMatchingNames(
					getField( SKILL_FIELD_3 ).getText() ).get( 0 ) : "" );

				getField( SKILL_FIELD_4 ).setEnabled( true );

				if ( getField( SKILL_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_FIELD_1, "required", "The text to display on the button." );
				}
				else
				{
					setLabel( SKILL_FIELD_1, "OK" );
				}

				if ( getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "required", "You must specify a valid skill name." );
				}
				else if ( skillName.equals( "" ) )
				{
					setLabel( SKILL_LABEL_3, "BAD",
						"Couldn't match a skill for: " + getField( SKILL_FIELD_3 ).getText() );
				}
				else
				{
					setLabel( SKILL_LABEL_3, "OK", "Matching skill found: " + skillName );
				}
			}
			else
			{
				/*
				 * Since the states of field 1 and field 3 depend on each other, set the states of both
				 * whenever one of the fields is changed.
				 * 
				 * State 1: displayText empty, skill empty = [ required, (optional) ]
				 * State 2: displayText non-matching, skill empty = [ (need skill), required ]
				 * State 3: displayText matching, skill empty = [ OK, (optional) ]
				 * State 4: displayText empty, skill non-matching = [ required, BAD ]
				 * State 5: displayText empty, skill matching = [ required, OK ]
				 * State 6: displayText non-empty, skill non-matching = [ OK, BAD ]
				 * State 7: displayText non-empty, skill matching = [ OK, OK ]
				 * 
				 * To enable the button, we check that label 1 is OK and label 3 not BAD
				 */

				getField( SKILL_FIELD_4 ).setEnabled( false );

				/* State 1 */
				if ( field1empty && field3empty )
				{
					setLabel( SKILL_LABEL_1, "required", "The text to display on the button." );
					setLabel( SKILL_LABEL_3, "(optional)",
						"If an skill is not specified, defaults to displayText.  Uses fuzzy matching." );
				}

				/* State 2 */
				else if ( !field1matching && field3empty )
				{
					setLabel( SKILL_LABEL_1, "(need skill)",
						"The display text does not match a skill, so you need to specify one under skill:" );
					setLabel( SKILL_LABEL_3, "required",
						"The display text does not match a skill, so you need to specify one." );
				}

				/* State 3 */
				else if ( field1matching && field3empty )
				{
					setLabel( SKILL_LABEL_1, "OK", "Display text matches skill: "
						+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_1 ).getText() )
							.get( 0 ) );
					setLabel( SKILL_LABEL_3, "(optional)",
						"The display text matches a skill, so you don't need to specify one here." );
				}

				/* State 4 */
				else if ( field1empty && !field3matching )
				{
					setLabel( SKILL_LABEL_1, "required", "The text to display on the button." );
					setLabel( SKILL_LABEL_3, "BAD", "Could not find a matching skill for: "
						+ getField( SKILL_FIELD_3 ).getText() );
				}

				/* State 5 */
				else if ( field1empty && field3matching )
				{
					setLabel( SKILL_LABEL_1, "required",
						"You still need to specify the text to display." );
					setLabel( SKILL_LABEL_3, "OK", "Matching skill found: "
						+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() )
							.get( 0 ) );
				}

				/* State 6 */
				else if ( !field1empty && !field3matching )
				{
					setLabel( SKILL_LABEL_1, "OK" );
					setLabel( SKILL_LABEL_3, "BAD", "Could not find a matching skill for: "
						+ getField( SKILL_FIELD_3 ).getText() );
				}

				/* State 7 */
				else if ( !field1empty && field3matching )
				{
					setLabel( SKILL_LABEL_1, "OK" );
					setLabel( SKILL_LABEL_3, "OK", "Matching skill found: "
						+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() )
							.get( 0 ) );
				}

			}
			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional" ) )
				&& ( label4.equalsIgnoreCase( "OK" ) || label4.equalsIgnoreCase( "(not used)" ) );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class TextFieldListener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( TEXT_FIELD ).getText().equalsIgnoreCase( "" ) )
			{
				getAddTextButton().setEnabled( false );
			}
			else
			{
				getAddTextButton().setEnabled( true );
			}
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillActionListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String display = getField( SKILL_FIELD_1 ).getText();
			String pref = getField( SKILL_FIELD_2 ).getText();
			String skill = getField( SKILL_FIELD_3 ).getText();
			String maxCasts = getField( SKILL_FIELD_4 ).getText();

			String deed = "$CUSTOM|Skill|" + display + "|" + pref;

			if ( !skill.equals( "" ) )
			{
				deed += "|" + skill;
			}
			if ( getLabel( SKILL_LABEL_4 ).getText().equalsIgnoreCase( "OK" ) )
			{
				deed += "|" + maxCasts;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );

			getSkillButton().setEnabled( false );
		}
	}

	public class ItemPrefActionListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String display = getField( ITEM_FIELD_1 ).getText();
			String pref = getField( ITEM_FIELD_2 ).getText();
			String item = getField( ITEM_FIELD_3 ).getText();

			String deed = "$CUSTOM|BooleanItem|" + display + "|" + pref;

			if ( !item.equals( "" ) )
			{
				deed += "|" + item;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getItemButton().setEnabled( false );
		}
	}

	public class MultiActionListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String display = getField( MULTI_FIELD_1 ).getText();
			String pref = getField( MULTI_FIELD_2 ).getText();
			String command = getField( MULTI_FIELD_3 ).getText();
			String maxUses = getField( MULTI_FIELD_4 ).getText();

			String deed = "$CUSTOM|MultiPref|" + display + "|" + pref + "|" + command + "|" + maxUses;

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getMultiButton().setEnabled( false );
		}
	}

	private class BooleanPrefActionListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String display = getField( BOOL_FIELD_1 ).getText();
			String pref = getField( BOOL_FIELD_2 ).getText();
			String command = getField( BOOL_FIELD_3 ).getText();

			String deed = "$CUSTOM|BooleanPref|" + display + "|" + pref;

			if ( !command.equals( "" ) )
			{
				deed += "|" + command;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getBoolButton().setEnabled( false );
		}
	}

	public class RemoveLastTextListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String piece;
			ArrayList buffer = getTextDeed();
			String display = "";

			buffer.remove( buffer.size() - 1 );

			for ( int i = 0; i < buffer.size(); ++i )
			{
				piece = (String) buffer.get( i );
				piece = piece.replaceAll( "\\|", "" );

				if ( Preferences.getString( piece ).equals( "" ) )
				{
					display += piece;
				}
				else
				{
					display += Preferences.getString( piece );
				}
			}

			display = display.replaceAll( "\\|", "" );
			getTextArea().setText( display );
			if ( buffer.size() == 0 )
			{
				getTextDeedButton().setEnabled( false );
			}
		}
	}

	public class AddTextListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			String piece;
			ArrayList buffer = getTextDeed();
			String display = "";

			if ( buffer.isEmpty() )
			{
				buffer.add( getField( TEXT_FIELD ).getText().replaceAll( ",", ",|" ) );
			}
			else
			{
				buffer.add( "|" + getField( TEXT_FIELD ).getText().replaceAll( ",", ",|" ) );
			}

			for ( int i = 0; i < buffer.size(); ++i )
			{
				piece = (String) buffer.get( i );
				piece = piece.replaceAll( "\\|", "" );

				if ( Preferences.getString( piece ).equals( "" ) )
				{
					display += piece;
				}
				else
				{
					display += Preferences.getString( piece );
				}
			}

			getField( TEXT_FIELD ).setText( "" );
			getTextArea().setText( display );
			getAddTextButton().setEnabled( false );
			getTextDeedButton().setEnabled( true );
		}
	}

	public class ClearTextListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent arg0 )
		{
			getTextDeed().clear();
			getTextArea().setText( "" );
			getTextDeedButton().setEnabled( false );
		}
	}

	public class TextActionListener
		implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			ArrayList buffer = getTextDeed();
			String deed = "$CUSTOM|Text|";

			for ( int i = 0; i < buffer.size(); ++i )
			{
				deed += (String) buffer.get( i );
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );

			getTextDeed().clear();
			getTextArea().setText( "" );
			getTextDeedButton().setEnabled( false );
		}
	}

	public JTextField getField( int choice )
	{
		JTextField[] fields =
		{
			this.boolField1, this.boolField2, this.boolField3, this.multiField1, this.multiField2,
			this.multiField3, this.multiField4, this.itemField1, this.itemField2, this.itemField3,
			this.skillField1, this.skillField2, this.skillField3, this.skillField4, this.textField
		};
		return fields[ choice ];
	}

	public JLabel getLabel( int choice )
	{
		JLabel[] labels =
		{
			this.boolLabel1, this.boolLabel2, this.boolLabel3, this.multiLabel1, this.multiLabel2,
			this.multiLabel3, this.multiLabel4, this.itemLabel1, this.itemLabel2, this.itemLabel3,
			this.skillLabel1, this.skillLabel2, this.skillLabel3, this.skillLabel4
		};
		return labels[ choice ];
	}

	public void setLabel( int choice, String label )
	{
		setLabel( choice, label, null );
	}

	public void setLabel( int choice, String label, String tip )
	{
		getLabel( choice ).setText( label );
		getLabel( choice ).setToolTipText( tip );
	}

	public ThreadedButton getBoolButton()
	{
		return boolButton;
	}

	public ThreadedButton getMultiButton()
	{
		return multiButton;
	}

	public ThreadedButton getItemButton()
	{
		return itemButton;
	}

	public ThreadedButton getSkillButton()
	{
		return skillButton;
	}

	public ThreadedButton getTextDeedButton()
	{
		return textDeedButton;
	}

	public ThreadedButton getAddTextButton()
	{
		return addTextButton;
	}

	public JTextArea getTextArea()
	{
		return textArea;
	}

	public ArrayList getTextDeed()
	{
		return textDeed;
	}
}
