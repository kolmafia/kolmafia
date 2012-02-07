/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sourceforge.kolmafia.RequestThread;

public abstract class ThreadedListener
	implements ActionListener, ItemListener, KeyListener, MouseListener, PopupMenuListener, Runnable
{
	private ActionEvent actionEvent;
	private KeyEvent keyEvent;
	private MouseEvent mouseEvent;

	public void actionPerformed( final ActionEvent e )
	{
		if ( !this.isValidEvent( e ) )
		{
			return;
		}

		this.actionEvent = e;
		RequestThread.runInParallel( this );
	}

	public boolean isAction()
	{
		return ( this.actionEvent != null );
	}

	public int getKeyCode()
	{
		if ( this.keyEvent == null )
		{
			return 0;
		}

		return this.keyEvent.getKeyCode();
	}

	public boolean hasShiftModifier()
	{
		int modifiers = 0;

		if ( this.actionEvent != null )
		{
			modifiers = this.actionEvent.getModifiers();
		}
		else if ( this.keyEvent != null )
		{
			modifiers = this.keyEvent.getModifiers();
		}

		return ( modifiers & ActionEvent.SHIFT_MASK ) != 0;
	}

	protected boolean isValidEvent( final ActionEvent e )
	{
		if ( e == null || e.getSource() == null )
		{
			return true;
		}

		if ( e.getSource() instanceof JComboBox )
		{
			JComboBox control = (JComboBox) e.getSource();
			return control.isPopupVisible();
		}

		return true;
	}

	public void itemStateChanged( ItemEvent e )
	{
		if ( e.getStateChange() == ItemEvent.SELECTED )
		{
			RequestThread.runInParallel( this );
		}
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

		this.keyEvent = e;
		RequestThread.runInParallel( this );

		e.consume();
	}

	public void keyTyped( final KeyEvent e )
	{
	}

	public void popupMenuCanceled( PopupMenuEvent e )
	{
		RequestThread.runInParallel( this );
	}

	public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
	{
		RequestThread.runInParallel( this );
	}

	public void popupMenuWillBecomeVisible( PopupMenuEvent e )
	{
	}

	public void mouseClicked( MouseEvent e )
	{
	}

	public void mousePressed( MouseEvent e )
	{
	}

	public void mouseReleased( MouseEvent e )
	{
		this.mouseEvent = e;

		RequestThread.runInParallel( this );
	}

	public void mouseEntered( MouseEvent e )
	{
	}

	public void mouseExited( MouseEvent e )
	{
	}

	protected int getMousePositionX()
	{
		if ( this.mouseEvent == null )
		{
			return -1;
		}

		return this.mouseEvent.getX();
	}

	protected int getMousePositionY()
	{
		if ( this.mouseEvent == null )
		{
			return -1;
		}

		return this.mouseEvent.getY();
	}

	protected MouseEvent getMouseEvent()
	{
		return this.mouseEvent;
	}

	protected JComponent getSource()
	{
		Object o = 
			this.actionEvent != null ?
			this.actionEvent.getSource() :
			this.keyEvent != null ?
			this.keyEvent.getSource() :
			this.mouseEvent != null ?
			this.mouseEvent.getSource() :
			null;			
		return ( o instanceof JComponent ) ? (JComponent) o : null;
	}
		
	protected boolean retainFocus()
	{
		return false;
	}

	public final void run()
	{
		this.execute();

		if ( this.retainFocus() )
		{
			this.getSource().grabFocus();
		}

		this.actionEvent = null;
		this.keyEvent = null;
		this.mouseEvent = null;
	}

	protected abstract void execute();
}
