function getObject( id )
{	return getObjectInPane( top.mainpane.document, id );
}


function getObjectInPane( pane, id )
{
	if ( document.getElementById )
		return pane.getElementById( id );
	if ( document.all )
		return pane.all[ id ];

	return false;
}


function getHttpObject()
{
	var httpObject = false;
	if ( window.ActiveXObject )
	{
		try
		{
			httpObject = new ActiveXObject( "Msxml2.XMLHTTP" );
		}
		catch ( e )
		{
			httpObject = new ActiveXObject( "Microsoft.XMLHTTP" );
		}
	}
	else
	{
		httpObject = new XMLHttpRequest();
	}

	return httpObject;
}

function updateDisplay( display, responseText )
{
	if ( responseText.length < 2 )
		return;

	display.innerHTML += responseText;

	if ( display.innerHTML.length > 30000 )
	{
		var lineIndex = display.innerHTML.indexOf( "<br", 20000 );
		if ( lineIndex != -1 )
			lineIndex = display.innerHTML.indexOf( ">", lineIndex );

		if ( lineIndex != -1 )
		{
			var length = display.innerHTML.length;
			display.innerHTML = display.innerHTML.substring( lineIndex + 1, length );
		}
		else
			display.innerHTML = "";
	}

	display.scrollTop = display.scrollHeight;
	if ( !isRefreshing && responseText.indexOf("<!-- REFRESH -->") != -1 )
		top.charpane.location.reload();
}

function inlineLoad( location, fields, id )
{
	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		var prefix = httpObject.responseText.substring(
			0, httpObject.responseText.indexOf( "<table" ) );

		var text = httpObject.responseText.substring(
			httpObject.responseText.indexOf( "<table" ),
			httpObject.responseText.lastIndexOf( "</table>" ) + 8 );

		var text = prefix + "<center>" + text + "</center>";

		// <tinyskills.js>

		var div = top.mainpane.document.getElementById('effdiv');
		if (!div)
		{
			var container = top.mainpane.document.createElement('DIV');
			container.id = 'effdiv';
			container.innerHTML = text;

			var buttons = top.mainpane.document.getElementById('mafiabuttons');
			if ( buttons )
			{
				top.mainpane.document.body.insertBefore(container, buttons.nextSibling);
			}
			else
			{
				top.mainpane.document.body.insertBefore(container, top.mainpane.document.body.firstChild);
			}
			div = container;
		}
		else
		{
			div.innerHTML = text;
		}

		div.style.display = "block";
		top.mainpane.scrollTo(0, 0);

		// </tinyskills.js>

		if ( httpObject.responseText.indexOf( "charpane" ) != -1 )
			top.charpane.location.reload();

		if ( httpObject.responseText.indexOf( "topmenu" ) != -1 )
			top.menupane.location.reload();

	};

	httpObject.open( "GET", "/" + location + "?" + fields, true );
	httpObject.send( null );
	return true;
}

function singleUse( location, fields )
{
	return inlineLoad( location, fields, false );
}


function multiUse( location, id, quantity )
{
	var qfield = "quantity";
	if ( location == "skills.php" )
		qfield = "itemquantity";

	var qvalue = quantity ? quantity : getObject( "quantity" + id ).value;
        var num = parseInt(prompt('How many?', qvalue));
        if (num < 1 || isNaN(num))
        {
            return false;
        }
	return inlineLoad( location, "MAFIAHIT&action=useitem&whichitem=" + id + "&" + qfield + "=" + num, id );
}


function showObject( id )
{
	getObject(id).style.display = "inline";
	return true;
}


function hideObject( id )
{
	getObject(id).style.display = "none";
	return true;
}


function getAdventureId( link )
{
	if ( link.indexOf( "adventure.php" ) != -1 )
		return link.substring( link.lastIndexOf( "?" ) + 1 );

	var name = link.substring( link.indexOf( "document." ) + 9, link.indexOf( ".submit" ) );
	var forms = document.getElementsByTagName( "form" );

	for ( var i = 0; i < forms.length; ++i )
		if ( forms[i].name == name )
			for ( var j = 0; j < forms[i].length; ++j )
				if ( forms[i][j].name == "adv" || forms[i][j].name == "snarfblat" )
					return "snarfblat=" + forms[i][j].value;

	return "";
}


function updateSafetyText()
{
	var safety = getObjectInPane( top.chatpane.document, "safety" );
	if ( !safety )
		return true;

	if ( safety.style.display == "none" )
		return true;

	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		if ( httpObject.responseText.length < 2 )
			return;

		safety.innerHTML = httpObject.responseText;
	}

	httpObject.open( "GET", "/KoLmafia/updateLocation?MAFIAHIT", true );
	httpObject.send( null );
	return true;
}


var lastAdventureId;

function showSafetyText( location )
{
	var safety = getObjectInPane( top.chatpane.document, "safety" );
	var adventureId = getAdventureId( location );
	if ( adventureId == "" )
		return true;

	if ( !safety )
	{
		safety = top.chatpane.document.createElement( "div" );
		safety.id = "safety";
		safety.closed = true;
		safety.active = true;

		safety.style.textAlign = "left";
		top.chatpane.document.body.appendChild( safety );

		safety.style.position = "absolute";
		safety.style.top = 0;
		safety.style.left = 0;
		safety.style.padding = "8px";
	}

	if ( !safety.active || adventureId == lastAdventureId )
	{
		lastAdventureId = "";
		document.location.href = "adventure.php?" + adventureId;
		return true;
	}

	lastAdventureId = adventureId;
	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		if ( httpObject.responseText.length < 2 )
			return;

		if ( safety.closed )
		{
			var nodes = top.chatpane.document.body.childNodes;
			for ( var i = 0; i < nodes.length; ++i )
			{
				if ( nodes[i].style && nodes[i].id != "safety" )
				{
					nodes[i].unsafety = nodes[i].style.display;
					nodes[i].style.display = "none";
				}
			}

			safety.style.display = "inline";
			safety.closed = false;
			safety.active = true;
		}

		safety.innerHTML = httpObject.responseText;
	}

	httpObject.open( "GET", "/KoLmafia/lookupLocation?" + adventureId + "&MAFIAHIT", true );
	httpObject.send( null );
	return true;
}


function attachSafetyText()
{
	var safety = getObjectInPane( top.chatpane.document, "safety" );
	if ( safety )
		safety.active = true;

	var links = document.getElementsByTagName( "a" );
	for ( var i = 0; i < links.length; ++i )
	{
		if ( links[i].href.indexOf( "showSafetyText" ) != -1 )
			return true;

		if ( links[i].href.indexOf( "adventure.php" ) != -1 )
		{
			links[i].data = links[i].href;
			links[i].href = "javascript: showSafetyText( '" + links[i].href + "' ); void(0);";
			links[i].style.cursor = "help";
		}
		else if ( links[i].href.indexOf( "submit" ) != -1 )
		{
			links[i].data = links[i].href;
			links[i].href = "javascript: showSafetyText( '" + links[i].href + "' ); void(0);";
			links[i].style.cursor = "help";
		}
	}

	return true;
}

function discardKarma() 
{
	var have = getObject('haveKarma').innerHTML;
	var banked = getObject('bankedKarma').innerHTML;

	if (Number(have) < 1)
		return true;

	if (have != null && banked != null) {
		getObject('haveKarma').innerHTML = Number(have) - 1;
		getObject('bankedKarma').innerHTML = Number(banked) + 11;
	}

	return true;
}


// ====================================================================
//			 URLEncode and URLDecode functions
//
// Copyright Albion Research Ltd. 2002
// httpObject://www.albionresearch.com/
//
// You may copy these functions providing that
// ( a ) you leave this copyright notice intact, and
// ( b ) if you use these functions on a publicly accessible
//		 web site you include a credit somewhere on the web site
//		 with a link back to http://www.albionresarch.com/
//
// If you find or fix any bugs, please let us know at albionresearch.com
//
// SpecialThanks to Neelesh Thakur for being the first to
// report a bug in URLDecode() - now fixed 2003-02-19.
// ====================================================================

function URLEncode( x )
{
	// The Javascript escape and unescape functions do not correspond
	// with what browsers actually do...
	var SAFECHARS = "0123456789" +					// Numeric
					"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +	// Alphabetic
					"abcdefghijklmnopqrstuvwxyz" +
					"-_.!~*'()";					// RFC2396 Mark characters
	var HEX = "0123456789ABCDEF";

	var plaintext = x;
	var encoded = "";
	for ( var i = 0; i < plaintext.length; i++ ) {
		var ch = plaintext.charAt( i );
		if ( ch=="+" ) {
			encoded+="%2B";
		} else if ( ch == " " ) {
				encoded += "+";				// x-www-urlencoded, rather than %20
		} else if ( SAFECHARS.indexOf( ch ) != -1 ) {
				encoded += ch;
		} else {
				var charCode = ch.charCodeAt( 0 );
			if ( charCode > 255 ) {
					alert( "Unicode Character '" + ch + "' cannot be encoded using standard URL encoding.\n" +
								"( URL encoding only supports 8-bit characters. )\n" +
						"A space ( + ) will be substituted." );
				encoded += "+";
			} else {
				encoded += "%";
				encoded += HEX.charAt( ( charCode >> 4 ) & 0xF );
				encoded += HEX.charAt( charCode & 0xF );
			}
		}
	}

	return encoded;
};

function URLDecode( x )
{
	 // Replace + with ' '
	 // Replace %xx with equivalent character
	 // Put [ERROR] in output if %xx is invalid.
	 var HEXCHARS = "0123456789ABCDEFabcdef";
	 var encoded = x;
	 var plaintext = "";
	 var i = 0;
	 while ( i < encoded.length ) {
			 var ch = encoded.charAt( i );
		 if ( ch == "+" ) {
				 plaintext += " ";
			 i++;
		 } else if ( ch == "%" ) {
			if ( i < ( encoded.length-2 )
					&& HEXCHARS.indexOf( encoded.charAt( i+1 ) ) != -1
					&& HEXCHARS.indexOf( encoded.charAt( i+2 ) ) != -1 ) {
				plaintext += unescape( encoded.substr( i,3 ) );
				i += 3;
			} else {
				alert( 'Bad escape combination near ...' + encoded.substr( i ) );
				plaintext += "%[ERROR]";
				i++;
			}
		} else {
			 plaintext += ch;
			 i++;
		}
	} // while
	 return plaintext;
};
