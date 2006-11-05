/**
 * Copyright (c) 1997-date Stuart Langridge
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// This file based on the code freely available for download at
// Kryogenix, http://www.kryogenix.org/code/browser/sorttable/.
// Modifications include the ability to sort numbers where commas
// are used to differentiate magnitude and the forced no-decoration
// of table header sorting links.

addEvent(window, "load", sortables_init);
var SORT_COLUMN_INDEX;

function sortables_init()
{
	// Find all tables with class sortable and make them sortable
	if (!document.getElementsByTagName) return;
	tbls = document.getElementsByTagName("table");
	for (ti=0;ti<tbls.length;ti++) {
		thisTbl = tbls[ti];
		if (((' '+thisTbl.className+' ').indexOf("sortable") != -1) && (thisTbl.id)) {
			//initTable(thisTbl.id);
			ts_makeSortable(thisTbl);
		}
	}
}

function ts_makeSortable(table)
{
	if (table.rows && table.rows.length > 0) {
		var firstRow = table.rows[0];
	}
	if (!firstRow) return;

	// We have a first row: assume it's the header, and make its contents clickable links
	for (var i=0;i<firstRow.cells.length;i++) {
		var cell = firstRow.cells[i];
		var txt = ts_getInnerText(cell);

		cell.innerHTML = '<a href="#" class="sortheader" onclick="ts_resortTable(this);return false;" style="text-decoration:none">'+txt+'<span class="sortarrow"><br>&nbsp;</span></a>';
	}

	if ( document.location.href.indexOf( ".php" ) == -1 )
	{
		for ( var i = 1; i < table.rows.length; ++i )
		{
			if ( i % 2 == 0 )
				table.rows[i].style.backgroundColor = "#e0e0ff";
			else
				table.rows[i].style.backgroundColor = "#ffffff";
		}
	}
}

function ts_getInnerText(el)
{
	if (typeof el == "string") return el;
	if (typeof el == "undefined") return el;
	if (el.innerText) return el.innerText;	// Not needed but it is faster
	return el.innerHTML;
}

function ts_resortTable(lnk)
{
	// get the span
	var span;
	for (var ci=0;ci<lnk.childNodes.length;ci++) {
		if (lnk.childNodes[ci].tagName && lnk.childNodes[ci].tagName.toLowerCase() == 'span') span = lnk.childNodes[ci];
	}
	var spantext = ts_getInnerText(span);
	var td = lnk.parentNode;

	var column = td.cellIndex;
	var tr = td.parentNode;
	if ( column == 0 ) {
		for (var tri=0;tri<tr.childNodes.length;tri++) {
			if (td==tr.childNodes[tri]) {
				column = tr.childNodes[0] ? tri : tri - 1;
			}
		}
	}

	var table = getParent(td,'TABLE');

	// Work out a type for the column
	if (table.rows.length <= 1) return;

	var regexp = new RegExp( "&.+;", "g" );
	var itm = ts_getInnerText(table.rows[1].cells[column]).replace(regexp,"");

	sortfn = ts_sort_numeric;
	if (itm.match(/^\d\d[\/]\d\d[\/]\d\d/)) sortfn = ts_sort_date;
	if (itm.match(/^[£$]/)) sortfn = ts_sort_currency;

	if ( !itm || (typeof itm == "string" && (itm == "" || itm.match(/[A-Za-z]/))) ) sortfn = ts_sort_default;

	SORT_COLUMN_INDEX = column;

	var firstRow = new Array();
	var newRows = new Array();
	var bottomRows = new Array();

	for (i=0;i<table.rows[0].length;i++)
		firstRow[i] = table.rows[0][i];

	for (j=1;j<table.rows.length;j++)
	{
		if (!table.rows[j].className || (table.rows[j].className && (table.rows[j].className.indexOf('sortbottom') == -1)))
		{
			newRows.push(table.rows[j]);
			table.rows[j].setAttribute('sortindex',j);
		}
		else
			bottomRows.push(table.rows[j]);
	}

	newRows.sort(sortfn);

	if (span.getAttribute('sortdir') == 'down')
	{
		ARROW = '<br>&uarr;';
		newRows.reverse();
		span.setAttribute('sortdir','up');
	}
	else
	{
		ARROW = '<br>&darr;';
		span.setAttribute('sortdir','down');
	}

	// We append rows that already exist to the tbody, so it moves them rather than creating new ones
	for (i=0;i<newRows.length;i++) table.tBodies[0].appendChild(newRows[i]);
	// do sortbottom rows only
	for (i=0;i<bottomRows.length;i++) table.tBodies[0].appendChild(bottomRows[i]);

	// Delete any other arrows there may be showing
	var allspans = document.getElementsByTagName("span");
	for (var ci=0;ci<allspans.length;ci++)
	{
		if (allspans[ci].className == 'sortarrow')
		{
			if (getParent(allspans[ci],"table") == getParent(lnk,"table")) { // in the same table as us?
				allspans[ci].innerHTML = '<br>&nbsp;';

		}
	}

	if ( document.location.href.indexOf( ".php" ) == -1 )
	{
		for ( var i = 1; i < table.rows.length; ++i )
		{
			if ( i % 2 == 0 )
				table.rows[i].style.backgroundColor = "#e0e0ff";
			else
				table.rows[i].style.backgroundColor = "#ffffff";
		}
	}
}

	span.innerHTML = ARROW;
}

function getParent(el, pTagName)
{
	if (el == null) return null;
	else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase())	// Gecko bug, supposed to be uppercase
		return el;
	else
		return getParent(el.parentNode, pTagName);
}

function compareIndex(a,b)
{
	aa = parseInt(a.getAttribute('sortindex'));
	bb = parseInt(b.getAttribute('sortindex'));
	return aa - bb;
}

function ts_sort_date(a,b)
{
	aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
	bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);

	yr = aa.substr(6,2);
	if (parseInt(yr) < 50) { yr = '20'+yr; } else { yr = '19'+yr; }
	dt1 = yr+aa.substr(0,2)+aa.substr(3,2);

	yr = bb.substr(6,2);
	if (parseInt(yr) < 50) { yr = '20'+yr; } else { yr = '19'+yr; }
	dt2 = yr+bb.substr(0,2)+bb.substr(3,2);

	if (dt1>dt2) return 1;
	if (dt1<dt2) return -1;

	return compareIndex(a,b);
}

function ts_sort_currency(a,b)
{
	aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');
	bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');

	result = parseFloat(aa) - parseFloat(bb);
	if ( result != 0 )
		return result;

	return compareIndex(a,b);
}

function ts_sort_numeric(a,b)
{
	var regexp = new RegExp( "[ ,;A-Za-z]", "ig" );
	aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(regexp,""));
	if (isNaN(aa)) aa = 0;
	bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(regexp,""));
	if (isNaN(bb)) bb = 0;

	result = aa - bb;
	if ( result != 0 )
		return result;

	return compareIndex(a,b);
}

function ts_sort_default(a,b)
{
	aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).toLowerCase();
	bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).toLowerCase();
	if (aa>bb) return 1;
	if (aa<bb) return -1;

	return compareIndex(a,b);
}

// addEvent and removeEvent
// cross-browser event handling for IE5+,  NS6 and Mozilla
// By Scott Andrew
function addEvent(elm, evType, fn, useCapture)
{
	if (elm.addEventListener)
	{
		elm.addEventListener(evType, fn, useCapture);
		return true;
	}
	else if (elm.attachEvent)
	{
		var r = elm.attachEvent("on"+evType, fn);
		return r;
	}
}
