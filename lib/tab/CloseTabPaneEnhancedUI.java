/*
 * David Bismut, davidou@mageos.com
 * Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004
 * Ecole des Mines de Nantes, France
 */

package tab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.View;

/**
 * This UI displays a different interface, which is independent from the look and feel.
 * 
 * @author David Bismut, davidou@mageos.com
 */
public class CloseTabPaneEnhancedUI
	extends CloseTabPaneUI
{

	private static Color whiteColor = Color.white;
	private static Color transparent = new Color( 0x184EB6 );

	private static Color nearBlack = new Color( 0, 0, 0, 80 );
	private static Color lightWhite = new Color( 150, 150, 150, 50 );

	public static Color notifiedA = new Color( 0xFFA98C );
	public static Color notifiedB = new Color( 180, 70, 15 );

	public static Color selectedA = new Color( 0x8CA9FF );
	public static Color selectedB = new Color( 15, 70, 180 );

	public static ComponentUI createUI( final JComponent c )
	{
		return new CloseTabPaneEnhancedUI();
	}

	@Override
	protected void paintFocusIndicator( final Graphics g, final int tabPlacement, final Rectangle[] rects,
		final int tabIndex, final Rectangle iconRect, final Rectangle textRect, final boolean isSelected )
	{
	}

	@Override
	protected void paintTabBorder( final Graphics g, final int tabPlacement, final int tabIndex, final int x,
		final int y, final int w, final int h, final boolean isSelected )
	{
		g.setColor( this.shadow );

		g.drawLine( x, y + 2, x, y + h - 1 ); // left highlight
		g.drawLine( x + 1, y + 1, x + 1, y + 1 ); // top-left highlight
		g.drawLine( x + 2, y, x + w - 3, y ); // top highlight
		g.drawLine( x + w - 1, y + 2, x + w - 1, y + h - 1 );
		g.drawLine( x + w - 2, y + 1, x + w - 2, y + 1 ); // top-right shadow

		if ( isSelected )
		{
			//Do the highlights
			g.setColor( this.lightHighlight );
			g.drawLine( x + 2, y + 2, x + 2, y + h - 1 );
			g.drawLine( x + 3, y + 1, x + w - 3, y + 1 );
			g.drawLine( x + w - 3, y + 2, x + w - 3, y + 2 );
			g.drawLine( x + w - 2, y + 2, x + w - 2, y + h - 1 );

		}

	}

	@Override
	protected void paintContentBorderTopEdge( final Graphics g, final int tabPlacement, final int selectedIndex,
		final int x, final int y, final int w, final int h )
	{

		if ( this.tabPane.getTabCount() < 1 )
		{
			return;
		}

		g.setColor( this.shadow );
		g.drawLine( x, y, x + w - 2, y );
	}

	@Override
	protected void paintContentBorderLeftEdge( final Graphics g, final int tabPlacement, final int selectedIndex,
		final int x, final int y, final int w, final int h )
	{

	}

	@Override
	protected void paintContentBorderBottomEdge( final Graphics g, final int tabPlacement, final int selectedIndex,
		final int x, final int y, final int w, final int h )
	{

	}

	@Override
	protected void paintContentBorderRightEdge( final Graphics g, final int tabPlacement, final int selectedIndex,
		final int x, final int y, final int w, final int h )
	{

	}

	@Override
	protected void paintTabBackground( final Graphics g, final int tabPlacement, final int tabIndex, final int x,
		final int y, final int w, final int h, final boolean isSelected )
	{
		if ( isSelected || this.tabStates.size() > tabIndex && this.tabStates.get( tabIndex ) == Boolean.TRUE )
		{

			GradientPaint leftGradient;
			GradientPaint rightGradient;

			int delta = 2;
			int delta2 = 2;

			if ( this.tabStates.size() > tabIndex && isSelected )
			{
				this.tabStates.set( tabIndex, Boolean.FALSE );
			}

			Color fadeFromColor =
				this.tabStates.size() > tabIndex && this.tabStates.get( tabIndex ) == Boolean.TRUE ? CloseTabPaneEnhancedUI.notifiedB : CloseTabPaneEnhancedUI.selectedB;

			Color fadeToColor =
				this.tabStates.size() > tabIndex && this.tabStates.get( tabIndex ) == Boolean.TRUE ? CloseTabPaneEnhancedUI.notifiedA : CloseTabPaneEnhancedUI.selectedA;

			if ( this.tabPane.isEnabledAt( tabIndex ) )
			{
				leftGradient = new GradientPaint( x, y, fadeFromColor, x + w / 2, y, fadeToColor );

				rightGradient = new GradientPaint( x + w / 2, y, fadeToColor, x + w + delta, y, fadeFromColor );
			}
			else
			{
				leftGradient = new GradientPaint( x, y, this.shadow, x + w / 2, y, CloseTabPaneEnhancedUI.lightWhite );

				rightGradient =
					new GradientPaint(
						x + w / 2, y, CloseTabPaneEnhancedUI.lightWhite, x + w + delta, y,
						CloseTabPaneEnhancedUI.transparent );
			}

			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint( leftGradient );
			//g2.setPaintMode();
			//g2.setColor(selectedColor);
			g2.fillRect( x + 2, y + 2, w / 2, h - 2 );
			g2.setPaint( rightGradient );
			//g2.setPaintMode();
			//g2.setColor(selectedA);
			g2.fillRect( x + 2 + w / 2, y + 2, w / 2 - delta2, h - 2 );
		}
	}

	@Override
	protected void paintText( final Graphics g, final int tabPlacement, final Font font, final FontMetrics metrics,
		final int tabIndex, final String title, final Rectangle textRect, final boolean isSelected )
	{

		g.setFont( font );

		View v = this.getTextViewForTab( tabIndex );
		if ( v != null )
		{
			// html
			v.paint( g, textRect );
		}
		else
		{
			// plain text
			int mnemIndex = this.tabPane.getDisplayedMnemonicIndexAt( tabIndex );

			if ( this.tabPane.isEnabled() && this.tabPane.isEnabledAt( tabIndex ) )
			{
				if ( isSelected || this.tabStates.size() > tabIndex && this.tabStates.get( tabIndex ) == Boolean.TRUE )
				{
					Graphics2D g2d = (Graphics2D) g;
					g2d.setColor( CloseTabPaneEnhancedUI.nearBlack );
					g2d.drawString( title, textRect.x + 1, textRect.y + metrics.getAscent() + 1 );
					g.setColor( CloseTabPaneEnhancedUI.whiteColor );
				}
				else
				{
					g.setColor( this.tabPane.getForegroundAt( tabIndex ) );
				}

				BasicGraphicsUtils.drawStringUnderlineCharAt(
					g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent() );

			}
			else
			{ // tab disabled
				g.setColor( this.tabPane.getBackgroundAt( tabIndex ).brighter() );
				BasicGraphicsUtils.drawStringUnderlineCharAt(
					g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent() );
				g.setColor( this.tabPane.getBackgroundAt( tabIndex ).darker() );
				BasicGraphicsUtils.drawStringUnderlineCharAt(
					g, title, mnemIndex, textRect.x - 1, textRect.y + metrics.getAscent() - 1 );

			}
		}
	}

	protected class ScrollableTabButton
		extends CloseTabPaneUI.ScrollableTabButton
	{

		public ScrollableTabButton( final int direction )
		{
			super( direction );
			this.setRolloverEnabled( true );
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension( 16, CloseTabPaneEnhancedUI.this.calculateMaxTabHeight( 0 ) );
		}

		@Override
		public void paint( final Graphics g )
		{
			Color origColor;
			boolean isPressed, isRollOver, isEnabled;
			int w, h, size;

			w = this.getSize().width;
			h = this.getSize().height;
			origColor = g.getColor();
			isPressed = this.getModel().isPressed();
			isRollOver = this.getModel().isRollover();
			isEnabled = this.isEnabled();

			g.setColor( this.getBackground() );
			g.fillRect( 0, 0, w, h );

			g.setColor( CloseTabPaneEnhancedUI.this.shadow );
			// Using the background color set above
			if ( this.direction == SwingConstants.WEST )
			{
				g.drawLine( 0, 0, 0, h - 1 ); //left
				g.drawLine( w - 1, 0, w - 1, 0 ); //right
			}
			else
			{
				g.drawLine( w - 2, h - 1, w - 2, 0 ); //right
			}

			g.drawLine( 0, 0, w - 2, 0 ); //top

			if ( isRollOver )
			{
				//do highlights or shadows

				Color color1;
				Color color2;

				if ( isPressed )
				{
					color2 = CloseTabPaneEnhancedUI.whiteColor;
					color1 = CloseTabPaneEnhancedUI.this.shadow;
				}
				else
				{
					color1 = CloseTabPaneEnhancedUI.whiteColor;
					color2 = CloseTabPaneEnhancedUI.this.shadow;
				}

				g.setColor( color1 );

				if ( this.direction == SwingConstants.WEST )
				{
					g.drawLine( 1, 1, 1, h - 1 ); //left
					g.drawLine( 1, 1, w - 2, 1 ); //top
					g.setColor( color2 );
					g.drawLine( w - 1, h - 1, w - 1, 1 ); //right
				}
				else
				{
					g.drawLine( 0, 1, 0, h - 1 );
					g.drawLine( 0, 1, w - 3, 1 ); //top
					g.setColor( color2 );
					g.drawLine( w - 3, h - 1, w - 3, 1 ); //right
				}

			}

			//g.drawLine(0, h - 1, w - 1, h - 1); //bottom

			// If there's no room to draw arrow, bail
			if ( h < 5 || w < 5 )
			{
				g.setColor( origColor );
				return;
			}

			if ( isPressed )
			{
				g.translate( 1, 1 );
			}

			// Draw the arrow
			size = Math.min( ( h - 4 ) / 3, ( w - 4 ) / 3 );
			size = Math.max( size, 2 );
			this.paintTriangle( g, ( w - size ) / 2, ( h - size ) / 2, size, this.direction, isEnabled );

			// Reset the Graphics back to it's original settings
			if ( isPressed )
			{
				g.translate( -1, -1 );
			}
			g.setColor( origColor );

		}

	}

	@Override
	protected CloseTabPaneUI.ScrollableTabButton createScrollableTabButton( final int direction )
	{
		return new ScrollableTabButton( direction );
	}

}