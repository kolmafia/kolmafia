var isRefreshing = false;

// Standard http object retrieval to handle all the various
// modern browsers.

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
};

function refreshSidebar()
{
    var httpObject = getHttpObject();
    if ( !httpObject )
        return true;

    isRefreshing = true;
    httpObject.onreadystatechange = function()
    {
        if ( httpObject.readyState != 4 )
            return;

        top.charpane.document.getElementsByTagName( "body" )[0].innerHTML =
            httpObject.responseText.replace( new RegExp( "<html>.*<body[^>]*>", "g" ), "" ).replace( new RegExp( "</body>.*</html>", "g" ), "" );

        isRefreshing = false;
    }

    httpObject.open( "GET", "http://<!--MAFIA_HOST_PORT-->/charpane.php" );
    httpObject.send( null );
}

function getNewMessages()
{
    var httpObject = getHttpObject();
    if ( !httpObject )
        return true;

    httpObject.onreadystatechange = function()
    {
        if ( httpObject.readyState != 4 )
            return;

        if ( httpObject.responseText == null || httpObject.responseText.length < 2 )
            return;

        if ( !isRefreshing && httpObject.responseText.indexOf( "<!-- REFRESH -->" ) != -1 )
            refreshSidebar();
    }

    httpObject.open( "GET", "http://<!--MAFIA_HOST_PORT-->/KoLmafia/getNewMessages" );
    httpObject.send( null );
};
