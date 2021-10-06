function resetPosition() {
	var topbar = getComputedStyle(document.getElementById('#mafiabuttons')[0]).height;
	document.getElementById('content_').style.top = topbar;
}


if (document.readyState != 'loading'){
	resetPosition();
} else {
	document.addEventListener('DOMContentLoaded', resetPosition);
}
window.addEventListener('resize', resetPosition);
