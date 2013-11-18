$(document).ready( resetPosition );
$(window).resize( resetPosition );

function resetPosition() {
	var topbar = $('#mafiabuttons').height();
	$('#content_').css( 'top', topbar + 'px' );
}
