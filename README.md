# Flow template 

___ Forked from  https://github.com/openrndr/openrndr-template ___

An OPENRNDR template to visualize music. Batteries included.

Use the prepared tools and wire them together as you please.
The result should be stunning visuals. Audio reactive and in sync.

Many of my VJ projects start similar 
and I wanted to create my own expansion of OPENRNDR that suits my needs.

## OPENRNDR

The OPENRNDR framework is powerful and concise out-of-the-box.

It combines the feature-richness of Processing.org and the expression freedom of Kotlin.
At first glance, it's a render engine like many other, like Skia or Processing.

But the access to the different computer graphics ins-and-outs 
lets you wire anything in the graphics pipeline together.

It was called "a tool to make tools". And I sympathize with that idea:
It allows for many great creative-coding applications. 
However, there are so many paths to continue, that it gets difficult to pick one.

This template is my spin on the formula. Fewer ingredients, higher integration.

## Tools 

### BPM

Start with `val bpm = extend(BPM()) { … }` and access the bpm repo builder.

Use `bpm.phase` to query the relative phase since program start 
or use `val cubicInOut = bpm.bindEnvelope { … }` to bind an envelope to the phase 
and sample it with `cubicInOut.sample()`. No time travel allowed.

### FFT

Start with `val fft = extend(FFT()) { … }` and access the minim-based FFT builder.

Access to the original "orx-minim" minim. 
For example, query the equidistant frequency bands with `fft.minim.getBand(i: Int)`.
This makes no sense for acoustics, 
as the perception of frequencies depend on ratio, not on absolute difference.

Use `fft.sample(AcousticRanges.BASS, 20)` 
to get the 20 frequencies samples from the audio bytes in the bass range, namely 160Hz to 320Hz.
Try `fft.volume`, `fft.multisample(vararg ranges...)` and `fft.waveform`.

### Color Repo

Start with `val colorRepo = extend(colorRepo()) { … }` and configure your color repository.

Define palettes, color paths and blend procedures.
Use `ColorRGBa`, `ColorXSVa` or the color model of your choice. 
Then use `color { c1: DOuble, c2: Double, c3: Double }` as the unit-cube color builder.

### Tools I want

* Compositor-like `FxFunnel` builder
* FFMPEG Video-controls API
* `InputBind` API to set binds for keyboard, mouse or MIDI device inputs.
* Param Picker
  * Like gui, but in a different application / interface
  * Live program or similar to change values on the fly

## Paradigms

### Bring your own constants

Take `fft.sample(AcousticRanges.BASS, 20)` for example.
You can go to the file where they are stored and change `BASS = 160.0 ..< 320.0` to whatever you like,
if you find a sound range that works for you.

The same is true for the other template constants. Use and re-use your favorite ones.

### No Time Travel

I repeat:
No time travel.

Time travel, like sampling the past or the future of an envelope, is difficult to get right.

Treat new samples like playing cards you add to a deck of them.
Usually, you add to the top or to the bottom so consider an `ArrayDeque`.
Then use the collection to perform effects based on the buffered values.

### Calculate, Don't accumulate

TL;DR: Floating point error sucks. 
Calculate values on every frame, don't rely on accumulation as it propagates the error. 
If possible, use explicit formulas.

Even 2 digits behind the floating point for values of type Double can lead to catastrophe.
I've created an example on play.kotlinlang.org showcasing this:
https://pl.kotl.in/q7dn_abzS

If you accumulate, you might get a crash, or skip a video or audio frame from time to time.
Being 1 frame or 1 pixel off, can make the difference.


