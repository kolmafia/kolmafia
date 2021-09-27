package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Image;

import java.awt.image.ImageObserver;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class UnanimatedLabel
	extends JLabel
{
	public UnanimatedLabel()
	{
		super( " ", null, SwingConstants.CENTER );
	}

	@Override
	public boolean imageUpdate( final Image img, final int infoflags, final int x, final int y, final int width,
		final int height )
	{
		if ( infoflags == ImageObserver.FRAMEBITS )
		{
			return true;
		}

		super.imageUpdate( img, infoflags, x, y, width, height );
		return true;
	}
}

