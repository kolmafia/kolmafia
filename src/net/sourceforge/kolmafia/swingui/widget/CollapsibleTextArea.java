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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import net.sourceforge.kolmafia.KoLConstants;

import org.jdesktop.swingx.JXCollapsiblePane;

/*
 A replacement for JTextArea that places a small expand/collapse icon to the left.

 Does not extend JTextComponent, so some finagling does have to be done to get it to drop
 in to some installations.
 */

public class CollapsibleTextArea
	extends JXCollapsiblePane
{
	private JLabel label;
	private JTextArea area;

	public CollapsibleTextArea( String label )
	{
		this.label = new JLabel( label );

		this.area = new JTextArea( 1, 10 );
		area.setLineWrap( true );
		area.setFont( KoLConstants.DEFAULT_FONT );
		area.setBorder( BorderFactory.createLineBorder( Color.black ) );
		area.setMinimumSize( area.getPreferredSize() );

		this.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		Action act = this.getActionMap().get( JXCollapsiblePane.TOGGLE_ACTION );

		JButton collapser = new JButton( act );
		collapser.setText( "" );
		collapser.setContentAreaFilled( false );
		collapser.setBorderPainted( false );
		collapser.setFocusPainted( false );
		collapser.setPreferredSize( new Dimension( 12, 12 ) );
		collapser.setMinimumSize( new Dimension( 12, 12 ) );
		collapser.setMaximumSize( new Dimension( 12, 12 ) );

		act.putValue( JXCollapsiblePane.COLLAPSE_ICON, UIManager.getIcon( "Tree.expandedIcon" ) );
		act.putValue( JXCollapsiblePane.EXPAND_ICON, UIManager.getIcon( "Tree.collapsedIcon" ) );

		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		this.add( collapser, c );

		c.anchor = GridBagConstraints.EAST;
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add( area, c );

		this.setMinimumSize( new Dimension( 1, this.getPreferredSize().height ) );
	}

	public CollapsibleTextArea()
	{
		this( "" );
	}

	public JLabel getLabel()
	{
		return label;
	}

	public String getText()
	{
		return this.area.getText();
	}

	public JTextArea getArea()
	{
		return this.area;
	}

	public void setText( String text )
	{
		this.area.setText( text );
	}
}