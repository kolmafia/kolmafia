package net.sourceforge.kolmafia.swingui.button;

import java.lang.reflect.Method;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

/**
 * Internal class used to invoke the given no-parameter method on the given object. This is used whenever there is
 * the need to invoke a method and the creation of an additional class is unnecessary.
 */

public class InvocationButton
	extends ThreadedButton
{
	public Object object;
	public Method method;

	public InvocationButton( final String text, final Object object, final String methodName )
	{
		this( text, object == null ? null : object.getClass(), methodName );
		this.object = object;
	}

	public InvocationButton( final String text, final Class c, final String methodName )
	{
		super( text );
		this.object = c;

		this.completeConstruction( c, methodName );
	}

	public InvocationButton( final String tooltip, final String icon, final Object object, final String methodName )
	{
		this( tooltip, icon, object == null ? null : object.getClass(), methodName );
		this.object = object;
	}

	public InvocationButton( final String tooltip, final String icon, final Class c, final String methodName )
	{
		super( JComponentUtilities.getImage( icon ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );

		this.object = c;
		this.setToolTipText( tooltip );
		this.completeConstruction( c, methodName );
	}

	public void completeConstruction( final Class c, final String methodName )
	{
		try
		{
			this.method = c.getMethod( methodName, KoLConstants.NOPARAMS );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public void run()
	{
		RequestThread.openRequestSequence();

		try
		{
			if ( this.method != null )
			{
				this.method.invoke( this.object, null );
			}
		}
		catch ( Exception e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}

		RequestThread.closeRequestSequence();
	}
}
