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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;

import java.awt.datatransfer.StringSelection;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.PixelGrabber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.TrophyRequest;
import net.sourceforge.kolmafia.request.TrophyRequest.Trophy;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerCache;

public class TrophyFrame
	extends GenericFrame
{

	public TrophyFrame()
	{
		super( "Trophy Arranger" );
		this.setCenterComponent( new TrophyArrangePanel() );
	}

	public static class TrophyArrangePanel
		extends JPanel
	{
		private TrophyPanel shownList, hiddenList;

		public TrophyArrangePanel()
		{
			this.setLayout( new BorderLayout() );
			JPanel eastPanel = new JPanel( new BorderLayout() );
			this.add( eastPanel, BorderLayout.EAST );
			JPanel buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
			eastPanel.add( buttonPanel, BorderLayout.NORTH );
			buttonPanel.add( new InvocationButton( "refresh", this, "doRefresh" ) );
			buttonPanel.add( new InvocationButton( "save", this, "doSave" ) );
			buttonPanel.add( new InvocationButton( "show all", this, "doShowAll" ) );
			buttonPanel.add( new InvocationButton( "hide all", this, "doHideAll" ) );
			buttonPanel.add( new InvocationButton( "autosort", this, "doAutoSort" ) );
			eastPanel.add( new JLabel( "<html><center>top list:<br>visible<hr>bottom:<br>hidden<br><br>drag to<br>rearrange<br><br>trophies<br>exchange<br>positions<br>if dropped<br>directly<br>on top of<br>another,<br>otherwise<br>moved.</html>" ), BorderLayout.SOUTH );
			shownList = new TrophyPanel( true );
			hiddenList = new TrophyPanel( false );
			JSplitPane split = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
				new TrophyScrollPane( shownList ),
				new TrophyScrollPane( hiddenList ) );
			split.setOneTouchExpandable( true );
			split.setResizeWeight( 0.8 );
			this.add( split, BorderLayout.CENTER );
			this.doRefresh();
		}

		public void doRefresh()
		{
			this.shownList.removeAll();
			this.hiddenList.removeAll();
			TrophyRequest req = new TrophyRequest();
			RequestThread.postRequest( req );
			ArrayList trophies = req.getTrophies();
			if ( req == null )
			{
				return;
			}
			Iterator i = trophies.iterator();
			while ( i.hasNext() )
			{
				Trophy t = (Trophy) i.next();
				FileUtilities.downloadImage( "http://images.kingdomofloathing.com/" + 
					t.filename );
				(t.visible ? this.shownList : this.hiddenList).add(
					new DraggableTrophy( t ) );
			}
			this.shownList.revalidate();
			this.shownList.repaint();
			this.hiddenList.revalidate();
			this.hiddenList.repaint();
		}
		
		public void doSave()
		{
			ArrayList trophies = new ArrayList();
			this.shownList.addChildrenToList( trophies );
			this.hiddenList.addChildrenToList( trophies );
			RequestThread.postRequest( new TrophyRequest( trophies ) );
		}

		public void doShowAll()
		{
			while ( this.hiddenList.getComponentCount() > 0 )
			{
				this.shownList.add( this.hiddenList.getComponent( 0 ) );
			}
			this.shownList.revalidate();
			this.shownList.repaint();
			this.hiddenList.revalidate();
			this.hiddenList.repaint();
		}

		public void doHideAll()
		{
			while ( this.shownList.getComponentCount() > 0 )
			{
				this.hiddenList.add( this.shownList.getComponent( 0 ) );
			}
			this.shownList.revalidate();
			this.shownList.repaint();
			this.hiddenList.revalidate();
			this.hiddenList.repaint();
		}

		public void doAutoSort()
		{
			this.shownList.doAutoSort();
			this.hiddenList.doAutoSort();
		}
	}
	
	private static class TrophyScrollPane
		extends JScrollPane
	{
		public TrophyScrollPane( JComponent component )
		{
			super( component, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			this.getVerticalScrollBar().setUnitIncrement( 25 );
		}
	}
	
	private static class TrophyPanel
		extends JPanel
		implements LayoutManager, DropTargetListener
	{
		private boolean shown;
		protected static TrophyPanel sourceList = null;
		protected static DraggableTrophy source = null;
		protected static int destIndex = -1;
		protected static boolean isExchange = false;
		
		public TrophyPanel( boolean shown )
		{
			super( null );
			this.shown = shown;
			this.setLayout( this );
			new DropTarget( this, DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK,
				(DropTargetListener) this );
		}
		
		public void addChildrenToList( ArrayList list )
		{
			int nc = this.getComponentCount();
			for ( int i = 0; i < nc; ++i )
			{
				DraggableTrophy t = (DraggableTrophy) this.getComponent( i );
				t.trophy.visible = this.shown;
				list.add( t.trophy );
			}
		}

		public void doAutoSort()
		{
			int nc = this.getComponentCount();
			for ( int i = 0; i < nc; ++i )
			{
				((DraggableTrophy) this.getComponent( i )).score = Integer.MAX_VALUE;
			}
			
			for ( int i = 0; i < nc - 2; ++i )
			{
				DraggableTrophy one = (DraggableTrophy) this.getComponent( i );
				DraggableTrophy best = null;
				int bestScore = Integer.MAX_VALUE;
				for ( int j = i + 1; j < nc; ++j )
				{
					DraggableTrophy two = (DraggableTrophy) this.getComponent( j );
					int score = Math.min( two.score, one.getSimilarity( two ) );
					two.score = score;
					if ( score < bestScore )
					{
						bestScore = score;
						best = two;
					}
				}
				this.add( best, i + 1 );
				this.revalidate();
				this.repaint();
			}
		}

		/* Required methods for DropTargetListener */
		
		public void dragEnter( DropTargetDragEvent dtde )
		{
		}
		
		public void dragOver( DropTargetDragEvent dtde )
		{
			Point xy = dtde.getLocation();
			dtde.acceptDrag( this.findDrop( xy.x, xy.y ) );
		}
		
		public void dropActionChanged( DropTargetDragEvent dtde )
		{
			Point xy = dtde.getLocation();
			dtde.acceptDrag( this.findDrop( xy.x, xy.y ) );
		}
		
		public void dragExit( DropTargetEvent dte )
		{
		}
		
		public void drop( DropTargetDropEvent dtde )
		{
			Point xy = dtde.getLocation();
			dtde.acceptDrop( this.findDrop( xy.x, xy.y ) );
			if ( this.source == null )
			{	// something dropped from elsewhere, ignore it.
			}
			else
			{
				int sourceIndex = this.source.getIndex();
				int destIndex = this.destIndex;
				if ( this.isExchange )
				{
					DraggableTrophy dest =
						(DraggableTrophy) this.getComponent( destIndex );
					//System.out.println( this.source.getSimilarity( dest ) + " " +
					//	this.source.trophy.name + "/" + dest.trophy.name );
					if ( this.sourceList == this )
					{
						if ( sourceIndex < destIndex )
						{
							this.add( dest, sourceIndex );
							this.add( this.source, destIndex );
						}
						else
						{
							this.add( this.source, destIndex );
							this.add( dest, sourceIndex );
						}
					}
					else
					{
						this.add( this.source, destIndex );
						this.sourceList.add( dest, sourceIndex );
					}
				}
				else	// move, instead of exchange
				{
					if ( this.sourceList == this && destIndex >= sourceIndex )
					{
						--destIndex;
					}
					this.add( this.source, destIndex );
				}
				this.revalidate();
				this.repaint();
				if ( this.sourceList != this )
				{
					this.sourceList.revalidate();
					this.sourceList.repaint();
				}
				this.source = null;
			}
			dtde.dropComplete( true );
		}
		
		/* Required methods for LayoutManager */
		
		public void addLayoutComponent( String name, Component comp )
		{
		}
		
		public void removeLayoutComponent( Component comp )
		{
		}
		
		public Dimension minimumLayoutSize( Container parent )
		{
			return this.preferredLayoutSize( parent );
		}
		
		public Dimension preferredLayoutSize( Container parent )
		{
			int nc = parent.getComponentCount();
			int height = ( nc / 11 ) * 2;
			nc %= 11;
			if ( nc > 5 )
			{
				height += 2;
			}
			else if ( nc > 0 || height == 0 )
			{
				++height;
			}
			Insets ins = parent.getInsets();
			return new Dimension( 600 + ins.left + ins.right,
				100 * height + ins.top + ins.bottom );
		}
		
		public void layoutContainer( Container parent )
		{
			Insets ins = parent.getInsets();
			int nc = parent.getComponentCount();
			int line = 0;
			int index = 0;
			while ( index < nc )
			{
				int nl = Math.min( nc - index, 5 + (line & 1) );
				int y = 100 * line + ins.top;
				int x = 50 * (6 - nl) + ins.left;
				while ( nl-- > 0 )
				{
					Component c = parent.getComponent( index++ );
					c.setBounds( x, y, 100, 100 );
					x += 100;
				}
				++line;
			}
		}
		
		private int lastX = -99;
		private boolean lastExch = false;
		
		private int findDrop( int x, int y )
		{
			Insets ins = this.getInsets();
			int nc = this.getComponentCount();
			y -= ins.top;
			y = Math.max( 0, y / 100 );
			int sol = (y / 2) * 11 + 5 * (y & 1);
			int nl = Math.min( Math.max( 0, nc - sol ), 5 + (y & 1) );
			x -= 50 * (6 - nl) + ins.left - 25;
			x = x >= 0 ? x / 50 : -99;
			boolean isExchange = (x & 1) != 0;
			x /= 2;
			if ( x < 0 )
			{	// before start of this line
				x = 0;
				isExchange = false;
			}
			else if ( x >= nl )
			{	// after end of this line
				x = nl;
				isExchange = false;
			}
			this.destIndex = Math.min( x + sol, nc );
			this.isExchange = isExchange;
			return isExchange ? DnDConstants.ACTION_LINK : DnDConstants.ACTION_MOVE;
		}
	}
	
	private static class DraggableTrophy
		extends JLabel
		implements DragGestureListener, DragSourceListener
	{
		public Trophy trophy;
		private static final DragSource dragSource = DragSource.getDefaultDragSource();
		private static final HashMap similarities = new HashMap();
		private int[] cache;
		public int score;
		
		public DraggableTrophy( Trophy trophy )
		{
			super( JComponentUtilities.getImage( trophy.filename ) );
			this.trophy = trophy;
			this.setToolTipText( "<html>" + trophy.name + "<br>" +
				trophy.filename.substring( trophy.filename.lastIndexOf( "/" ) + 1 )
				+ "</html>" );
			this.dragSource.createDefaultDragGestureRecognizer( this,
				DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK,
				(DragGestureListener) this );
		}
		
		public int getIndex()
		{
			Container parent = this.getParent();
			int nc = parent.getComponentCount();
			for ( int i = 0; i < nc; ++i )
			{
				if ( parent.getComponent( i ) == this )
				{
					return i;
				}
			}
			return -1;	// wut?
		}
		
		public int getSimilarity( DraggableTrophy other )
		{
			Integer key, rv;
			int id1 = this.trophy.id;
			int id2 = other.trophy.id;
			key = IntegerCache.valueOf( id1 < id2 ? (id1 << 16 ) | id2 :
				(id2 << 16) | id1 );
			rv = (Integer) DraggableTrophy.similarities.get( key );
			if ( rv != null ) return rv.intValue();
			int[] img1 = this.grab();
			int[] img2 = other.grab();
			int score = 0;
			for ( int i = Math.min( img1.length, img2.length ) - 1; i >= 0; --i )
			{
				score += Math.abs( (img1[ i ] & 0xFF) - (img2[ i ] & 0xFF) );			
			}
			
			DraggableTrophy.similarities.put( key, IntegerCache.valueOf( score ) );
			return score;
		}
		
		private int[] grab()
		{
			if ( this.cache != null ) return this.cache;
			
			PixelGrabber g = new PixelGrabber(
				this.createImage(
					new FilteredImageSource(
						((ImageIcon) this.getIcon()).getImage().getSource(),
						new AreaAveragingScaleFilter( 25, 25 ) ) ),
				0, 0, 25, 25, true );
			try
			{
				g.grabPixels();
			}
			catch ( InterruptedException e )
			{
				return new int[ 0 ];
			}
			
			Object rv = g.getPixels();
			if ( rv instanceof int[] )
			{
				this.cache = (int[]) rv;
				return this.cache;
			}
			return new int[ 0 ];	// don't know how to handle any other format
		}
		
		/* Methods required by DragGestureListener */
		
		public void dragGestureRecognized( DragGestureEvent dge )
		{
			TrophyPanel.source = this;
			TrophyPanel.sourceList = (TrophyPanel) this.getParent();
			dge.startDrag( null, new StringSelection( this.trophy.name ),
				(DragSourceListener) this );
			//dge.startDrag( null, ((ImageIcon) this.getIcon()).getImage(),
			//	new Point( -50, -50 ), new StringSelection( this.trophy.name ),
			//	(DragSourceListener) this);
		}
		
		/* Methods required by DragSourceListener */
		
		public void dragEnter( DragSourceDragEvent dsde )
		{
		}
		
		public void dragOver( DragSourceDragEvent dsde )
		{
		}
		
		public void dropActionChanged( DragSourceDragEvent dsde )
		{
		}
		
		public void dragExit( DragSourceEvent dse )
		{
		}
		
		public void dragDropEnd( DragSourceDropEvent dsde )
		{
		}
	}
}
