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
    	    httpObject = new XMLHttpRequest();

    return httpObject;
};

function getNewMessages()
{
	  	var httpObject = getHttpObject();
  		if ( !httpObject )
		     	return true;

  		httpObject.open( "GET", "http://<!--MAFIA_HOST_PORT-->/KoLmafia/getNewMessages" );
	  	httpObject.onreadystatechange = function()
		  {
		     	if ( httpObject.readyState != 4 )
				        return;

        if ( httpObject.responseText == null || httpObject.responseText.length < 2 )
            return;

     			if ( !isRefreshing && httpObject.responseText.indexOf( "<!-- REFRESH -->" ) != -1 )
			     {
				        isRefreshing = true;
				        top.charpane.location.reload( true );
				        isRefreshing = false;
			     }
		  }

		  httpObject.send( null );
};
