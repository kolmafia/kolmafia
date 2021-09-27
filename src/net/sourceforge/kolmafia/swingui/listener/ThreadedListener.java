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
	protected KeyEvent keyEvent;
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
			modifiers = this.keyEvent.getModifiersEx();
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

	protected boolean isValidKeyCode( int keyCode )
	{
		return keyCode == KeyEvent.VK_ENTER;
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

		if ( !this.isValidKeyCode( e.getKeyCode() ) )
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
			JComponent source = this.getSource();
			if ( source != null )
			{
				source.grabFocus();
			}
		}

		this.actionEvent = null;
		this.keyEvent = null;
		this.mouseEvent = null;
	}

	protected abstract void execute();
}
