# Nihonjindes Japanese TTS - Android implementation for Codename One

by [Francesco Galgani](https://www.informatica-libera.net/)

Offline Japanese text-to-speech (TTS) on Android for **Codename One** apps, powered by **Open JTalk** with the **Mei (Normal)** voice.

For immediate testing, you can try `japanese-offline-tts-hq-debug.apk`.

Supported ABIs: arm64-v8a, armeabi-v7a, and x86_64 (it supports both actual devices and the Android Studio emulator).

*minSdkVersion 21 (Android 5+)*

## Usage

In general, you can install this library from the Codename One settings GUI. However, if you would like to install the CN1Lib manually, you can use this command:

`mvn cn1:install-cn1lib -Dfile=/ABS/PATH/TO/japanese-offline-tts.cn1lib`

The `cn1lib-example-usage` directory provides an example of how to use the exposed APIs.

Here is a screenshot:

<img src="japanese-tts.jpg" alt="Screenshot" width="400" />


## About the voice quality

I fine-tuned the voice to achieve decent quality:

>-s 48000 (sampling frequency)
> 
>-p 240 (frame period)
> 
>-a 0.55 (all-pass constant for 48kHz voices)
> 
>-b 0.8 (postfiltering; this is usually the key knob to reduce the “robotic” sound)
> 
>plus explicit defaults for: -u 0.5 -jm 1.0 -jf 1.0

You can compare it to this online demo:

- https://open-jtalk.sp.nitech.ac.jp/index.php → select **“Mei (Normal)”**

The online demo and the voice produced with this CN1Lib are identical on my testing device (Samsung Galaxy A20e, Android 11).

## Known Issues (to be fixed in the next release)

* The volume is a little lower than it should be.

* Although the demo does not impose any limits on text length, the text should still be relatively short.

* The input text cannot contain line breaks, such as /n, because any text following an /n will be ignored. Therefore, in text sent to CN1Lib, each line break must be replaced with a space.

* In the demo, WAV files accumulate. In a real app, however, they should be deleted after playback.

* It appears that the "Defaults" button must be pressed twice for it to work. I have not investigated this issue further.

## License

All original work of [Francesco Galgani](https://www.informatica-libera.net/) in this repository is released under **CC0 1.0 Universal**.
Third‑party components are licensed under their respective licenses; see **ATTRIBUTION.md**.

