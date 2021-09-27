package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class InterruptableDialog
{
	private static final PauseObject pauser = new PauseObject();

	private static class InterruptableConfirmDialogBox
		extends JOptionPane
		implements Runnable
	{
		private final String message;
		private Boolean result = null;

		public InterruptableConfirmDialogBox( String mes )
		{
			this.message = mes;

			SwingUtilities.invokeLater( this );
		}

		public Boolean getResult()
		{
			return result;
		}

		public void setResult( boolean b )
		{
			this.result = b;
		}

		public void run()
		{
			this.setResult( JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
				StringUtilities.basicTextWrap( message ), "", JOptionPane.YES_NO_OPTION ) );

			synchronized ( InterruptableDialog.pauser )
			{
				InterruptableDialog.pauser.notifyAll();
			}
		}
	}

	private static class InterruptableInputDialogBox
			extends JOptionPane
			implements Runnable
	{
		private final String message;
		private String result = null;
		private boolean closed = false;

		public InterruptableInputDialogBox( String mes )
		{
			this.message = mes;

			SwingUtilities.invokeLater( this );
		}

		public String getResult()
		{
			return result;
		}
		public void setResult( String b )
		{
			this.result = b;
		}

		public boolean getClosed() { return closed; }
		public void setClosed( boolean b ) { this.closed = b; }

		public void run()
		{
			String result = JOptionPane.showInputDialog( null,
					StringUtilities.basicTextWrap( message ) );
			this.setResult( result );
			this.setClosed( result == null );

			synchronized ( InterruptableDialog.pauser )
			{
				InterruptableDialog.pauser.notifyAll();
			}
		}
	}

	public static Value confirm( Value message, Value timeOut, Value defaultBoolean )
	{
		if ( StaticEntity.isHeadless() )
		{
			// this doesn't support headless operation yet, sorry
			RequestLogger.printLine( message.toString() );
			RequestLogger.printLine( "(Y/N, leave blank to choose N)" );

			String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine( " > " );

			return DataTypes.makeBooleanValue( reply.equalsIgnoreCase( "y" ) );
		}

		String mes = message.toString();
		long time = timeOut.intValue();

		InterruptableConfirmDialogBox r = new InterruptableConfirmDialogBox( mes );

		synchronized ( pauser ) // get the object's monitor
		{
			try
			{
				pauser.wait( time ); // wait for <time> millis, unless the dialog notifies the pauser
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
		Boolean result = r.getResult(); // will be null if the user hasn't selected an option

		if ( result != null )
		{
			return DataTypes.makeBooleanValue( result );
		}

		// must have reached timeOut, so dispose of the frame and return defaultBoolean

		Frame f = JOptionPane.getFrameForComponent( r );

		if ( f != null )
		{
			f.dispose();
		}

		return defaultBoolean;
	}

	public static Value input( Value message, Value timeOut, Value defaultString )
	{
		if ( StaticEntity.isHeadless() )
		{
			// this doesn't support headless operation yet, sorry
			RequestLogger.printLine( message.toString() );
			RequestLogger.printLine( "(type your response)" );

			String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine( " > " );

			return DataTypes.makeStringValue( reply.toString() );
		}

		String mes = message.toString();
		long time = timeOut.intValue();

		InterruptableInputDialogBox r = new InterruptableInputDialogBox( mes );

		synchronized ( pauser ) // get the object's monitor
		{
			try
			{
				pauser.wait( time ); // wait for <time> millis, unless the dialog notifies the pauser
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
		String result = r.getResult(); // will be null if the user hasn't selected an option

		if ( result != null )
		{
			return DataTypes.makeStringValue( result );
		}

		if ( !r.getClosed() )
		{
			// must have reached timeOut, so dispose of the frame and return defaultBoolean
			Frame f = JOptionPane.getFrameForComponent( r );

			if ( f != null )
			{
				f.dispose();
			}
		}

		return defaultString;
	}
}