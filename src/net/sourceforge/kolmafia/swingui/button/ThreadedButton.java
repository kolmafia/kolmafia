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

package net.sourceforge.kolmafia.swingui.button;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;

import net.sourceforge.kolmafia.RequestThread;

public class ThreadedButton
	extends JButton
	implements ActionListener, KeyListener
{
	private Runnable action;

	public ThreadedButton( final String label, Runnable action )
	{
		super( label );
		this.addActionListener( this );
		this.setOpaque( true );

		this.action = action;
	}

	public ThreadedButton( final ImageIcon icon, Runnable action )
	{
		super( icon );
		this.addActionListener( this );
		this.setOpaque( true );

		this.action = action;
	}

	public void setAction( Runnable action )
	{
		this.action = action;
	}

	public void actionPerformed( final ActionEvent e )
	{
		if ( !this.isValidEvent( e ) )
		{
			return;
		}

		RequestThread.runInParallel( action );
	}

	protected boolean isValidEvent( final ActionEvent e )
	{
		if ( e == null || e.getSource() == null )
		{
			return true;
		}

		if ( e.getSource() instanceof JComboBox )
		{
			return ( (JComboBox) e.getSource() ).isPopupVisible();
		}

		return true;
	}

	public void keyPressed( final KeyEvent e )
	{
	}

	public void keyReleased( final KeyEvent e )
	{
		if ( e.isConsumed() )
		{
			return;
		}

		if ( e.getKeyCode() != KeyEvent.VK_ENTER )
		{
			return;
		}

		RequestThread.runInParallel( action );
		e.consume();
	}

	public void keyTyped( final KeyEvent e )
	{
	}
}
