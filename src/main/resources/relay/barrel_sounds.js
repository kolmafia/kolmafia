(function(window) {
	var BARREL_SMASH_SOUND = "/images/barrel_smash.mp3";

	var AudioContext = window.AudioContext || window.webkitAudioContext;

	if (AudioContext) {
		// web audio API

		var context = new AudioContext();
		var audioBuffer;

		function play_source() {
			if (audioBuffer) {
				var source = context.createBufferSource();

				source.buffer = audioBuffer;
				source.playbackRate.value = 1 + Math.random() / 2;
				source.connect(context.destination)
				source.start(context.currentTime);
			}
		}

		var request = new XMLHttpRequest();

		request.open("GET", BARREL_SMASH_SOUND);
		request.responseType = "arraybuffer";

		request.onload = function() {
			context.decodeAudioData(request.response, function(decodedBuffer) {
				audioBuffer = decodedBuffer;

				if ($(document).live) {
					$(".spot").live("click", play_source);
				} else {
					$(document).on("click", ".spot", play_source);
				}
			});
		}

		request.send();
	} else if (window.Audio) {
		// HTML5 audio element

		var audio = new Audio(BARREL_SMASH_SOUND);

		function play_element() {
			audio.play();
		}

		if ($(document).live) {
			$(".spot").live("click", play_element);
		} else {
			$(document).on("click", ".spot", play_element);
		}
	}
})(this);
