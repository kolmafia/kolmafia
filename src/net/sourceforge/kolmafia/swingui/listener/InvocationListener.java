package net.sourceforge.kolmafia.swingui.listener;

import java.lang.reflect.Method;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

public class InvocationListener
	extends ThreadedListener
{
	private Object object;
	private Method method;

	public InvocationListener( Object object, Class c, String methodName )
	{
		try
		{
			this.object = object;
			this.method = c.getMethod( methodName, KoLConstants.NOPARAMS );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	@Override
	protected void execute()
	{
		try
		{
			if ( this.method != null )
			{
				this.method.invoke( this.object, (Object []) null );
			}
		}
		catch ( Exception e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}
	}
}
