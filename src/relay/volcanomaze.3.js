// <script type="text/javascript">
var uhohs = 0;
function setTitle(div, title) {
	var a = div.find('a');
	var cur = a.attr('title');
	a.attr('title', cur.replace(/- [^)]*\)/, '- '+title+')'));
}
function adjustPlatforms(res) {
	if (res) {
		if (res.won) {
			document.location = res.won;
			return;
		}
		$('#you').remove();
		$('.lastyou').removeClass('lastyou');
		$('.you').removeClass('you').find('a').addClass('lastyou');
		$('.sq').each(function () {
			t = $(this);
			if (t.hasClass('goal')) return true;
			var bg = t.find('a').css('background');
			t.removeClass('yes');
			if (bg && bg.match(/platformup[0-9]/) && t.attr('rel') != res.pos) {
				t.find('a').css('background','url("https://d2uyhvukfffg5a.cloudfront.net/itemimages/platformdown'+Math.floor((Math.random() * 4) +1)+'.gif?foo='+1+'")');
			}
			else t.find('a').css('background','');
			if (!t.hasClass('no')) { t.addClass('no'); setTitle(t, 'Lava'); }
			if (t.attr('rel') == res.pos) {
				t.removeClass('no').addClass('you');
				setTitle(t, 'You');
			}
			t.removeClass('next');
			if (t.attr('rel') == res.next) {
				t.removeClass('no').addClass('yes').addClass('next');
				setTitle(t, 'Next Platform');
			}
		});
		for (i in res.show) {
			if (!res.show.hasOwnProperty(i)) continue;
			var sq = $('#sq'+res.show[i]);
			if (sq.hasClass('goal')) continue;
			if (sq.hasClass('you')) continue;
			var image = 'platformup'+Math.floor((Math.random() * 4) +1);
			if (sq.hasClass('next')) {
				image += 'x';
			}
			var url ='url("https://d2uyhvukfffg5a.cloudfront.net/itemimages/'+image+'.gif?foo='+1+'")';
			if (!sq.hasClass('next')) {
				sq.removeClass('no').addClass('yes');
				setTitle(sq, 'Platform');
			}
			sq.find('a').css('background', url);
		}
	}
	else {
		uhohs++;
		if (uhohs > 3) {
			document.location = '?';
			return;
		}
		//alert('There doesn\'t seem to be a platform there.');
	}
}
$(document).ready(function () {
	$('#puzzle, .no').click(function (e) {
		if ($(e).hasClass('yes')) return false;
		if (confirm('Swim back to the start?')) {
			document.location = '?jump=1';
		}
		return false;
	});
	$('.sq a').click(function () {
		var going = $(this).parent().attr('rel');
		$.getJSON($(this).attr('href') + '&ajax=1', adjustPlatforms);
		return false;
	});
        $('#step').click(function () {
	    	$.get("?autostep", function (res) {
			if (res) {
				if (res.startsWith("<html>")) {
					$("html").html(res);
				} else {
					adjustPlatforms(JSON.parse(res));
				}
			}
		});
		return false;
	});
});
// </script>
