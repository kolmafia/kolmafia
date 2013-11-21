$(document).ready( resetPosition );
$(window).resize( resetPosition );
$.noConflict();

function resetPosition() {
	var topbar = $('#mafiabuttons').height();
	$('#content_').css( 'top', topbar + 'px' );
}
