/***
 * Windows Tray Icon
 * -----------------
 *
 * Written by Jan Struyf
 *
 *  jan.struyf@cs.kuleuven.ac.be
 *  http://jeans.studentenweb.org/java/trayicon/trayicon.html
 *
 * Please mail me if you
 *	- 've found bugs
 *	- like this program
 *	- don't like a particular feature
 *	- would like something to be modified
 *
 * I always give it my best shot to make a program useful and solid, but
 * remeber that there is absolutely no warranty for using this program as
 * stated in the following terms:
 *
 * THERE IS NO WARRANTY FOR THIS PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
 * LAW. THE COPYRIGHT HOLDER AND/OR OTHER PARTIES WHO MAY HAVE MODIFIED THE
 * PROGRAM, PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
 * PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
 * REPAIR OR CORRECTION.
 *
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW WILL ANY COPYRIGHT HOLDER,
 * OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE PROGRAM,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
 * PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
 * INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER
 * PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * May the Force be with you... Just compile it & use it!
 */

package com.jeans.trayicon;

// Checkbox menu item for Tray Icon
public class TrayIconPopupCheckItem
	extends TrayIconPopupSimpleItem
{

	// Checkbox selected?
	protected boolean m_Selected;

	/**
	 * Create new checkbox menu item Param item = the name of the new item
	 */
	public TrayIconPopupCheckItem( final String item )
	{
		super( item );
	}

	/**
	 * Set/Remove checkmark of item Param selected = checkmark/no checkmark?
	 */
	public void setCheck( final boolean selected )
	{
		this.m_Selected = selected;
		if ( this.m_TrayIcon != null )
		{
			this.m_TrayIcon.modifyPopup( this.m_MenuId, WindowsTrayIcon.POPUP_MODE_CHECK, this.m_Selected );
		}
	}

	/**
	 * Get checkmark of item
	 */
	public boolean getCheck()
	{
		return this.m_Selected;
	}

	/*******************************************************************************************************************
	 * * Next section is for inter use only -- or for hackers :O) * *
	 ******************************************************************************************************************/

	/**
	 * Callback when user selects menu item (find it by comparing menu id's) Param menuId = the id of the selected item
	 */
	@Override
	public boolean onSelected( final int menuId )
	{
		if ( menuId == this.m_MenuId )
		{
			this.m_Selected = !this.m_Selected;
			this.m_TrayIcon.modifyPopup( this.m_MenuId, WindowsTrayIcon.POPUP_MODE_CHECK, this.m_Selected );
		}
		return super.onSelected( menuId );
	}

	/**
	 * Create menu in native library - used by WindowsTrayIcon.setPopup() Param trayicon = the owner of this menu Param
	 * id = the icon's id Param level = the level (submenu depth)
	 */
	@Override
	public void setTrayIcon( final WindowsTrayIcon trayicon, final int id, final int level )
	{
		int extra = 0;
		if ( this.m_Enabled )
		{
			extra |= WindowsTrayIcon.POPUP_MODE_ENABLE;
		}
		if ( this.m_Selected )
		{
			extra |= WindowsTrayIcon.POPUP_MODE_CHECK;
		}
		this.m_MenuId = WindowsTrayIcon.subPopup( id, level, this.m_Item, WindowsTrayIcon.POPUP_TYPE_CHECKBOX, extra );
		this.m_TrayIcon = trayicon;
	}
}
