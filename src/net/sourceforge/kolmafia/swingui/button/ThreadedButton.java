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
