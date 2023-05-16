# Flow template

An OPENRNDR template named "Flow" to jump start music visualization with the most useful tools prepared.

Run the template demo to see the example program in action.
It shows what the template is capable of.

Adapt the demo to your liking or start from the empty template. 

Discover useful APIs for working with music, visual content, visual effects and much more.

The result should be stunning visuals - audio reactive and in sync.

## Intention

I am Lukas, a VJ and creative coder from Germany.

Many of my VJ projects in [OPENRNDR](https://openrndr.org/) start with similar functionality,
which I used to copy back and forth.

This template is my "expansion" of OPENRNDR that suits the needs for live music visualization.
But I also wanted to share this template, providing the tools and an demo to run for everyone.

Feel free to clone and experiment with it, or fork it and make it your own version.

I use this myself as a starting ground for my projects. 
So it is likely, to get updates in the future until I am satisfied with it.

## How to use

### Prerequisites

* [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Community or Professional) installed. Other IDEs are discouraged.
* Git configured in IntelliJ IDEA.

### Clone And Run

Open IntelliJ IDEA. Navigate to File → New → "Project from Version Control...".

Add this repo's url and clone this repository.

Navigate to the demo at `src/main/kotlin/FlowTemplateDemo.kt` and run "Current File". 
(Similar to [OPENRNDR Template](https://github.com/openrndr/openrndr-template))

Press `f1` to toggle the available commands. The other keys are used for the demo itself.

### Screenshots of the FlowTemplateDemo

#### Live demo

![](./videos/DemoClip.mp4)

👉 Full Demo on YouTube: [Flow Template ~ Live Demo](https://youtu.be/IqMl_r1qLPQ)

#### Demo: On Startup

![Demo: On Startup](./images/TemplateProgram-OnStartup.png)

#### Demo: Diamond ring on, 2 Bouncy balls activated

![Demo: Diamond ring on, 2 of 4 bouncy balls activated](./images/TemplateProgram-DiamondRing-BouncyBalls.png)

#### Demo: Spectral Audio Analysis

![Demo: Spectral Audio Analysis](./images/TemplateProgram-SpectralAudioAnalysis.png)

### Experiment …

You might notice that this is a GitHub template, not a library.
This way, you have everything in one file to adapt config or logic.

To experiment with the demo, some starting points might be:
* Setting the bpm to your favorite song's bpm (See: [songbpm.com](https://songbpm.com/))
* Tweaking values
* Changing the color palette
* Write your own visual groups (See: `audioGroup`, `diamondGroup` and `circleGroup`)
* Write your own bpm-based envelopes (See: `kick` and `flash`)

Even better, start from scratch and build something of your own.
Try things out, follow the fun and enjoy !

## Why should I use this ?

If you are computer graphics beginner or veteran, <br>
a VJ or video creator, <br>
creative coder, motion designer, multimedia artist or just interested in cool looking visuals to enhance your music experience -<br>
then this might be for you.

This template aims to build on top of OPENRNDR to quickly build live music visuals programs, 
with well integrated APIs that can be used right away.

* Based on OPENRNDR
* Useful APIs for live music visualization
* Emphasis on single-file config
* Demo program to run for yourself

It is not a full-blown VJ software, but a starting point for your own project.

## APIs of the Flow Template

### Beat Clock

Start with `val beatClock = extend(BeatClock(bpm: Int))` and get a beat-tracking extension.

Use its `phase` counting the beats (with decimals) for beat-based effects. 

Bind envelopes via `val cubicInOut by bpm.bindEnvelope { … }` to get a cyclic `Envelope` and
use its value to animate stuff, like size, position, color, etc.

### Audio

Start with `val audio = Audio()`, create audio processors like `VolumeProcessor` or `ConstantQProcessor` via its `audio.create<...>Processor` methods.

Then call `audio.start()` to let the audio analysis run in a background thread.
Fetch the latest audio data from each processor.

Provides common values for working with audio, like the frequency range a human can hear, also called "Acoustic Range": `LOWEST_FQ`/`HIGHEST_FQ`.
Or typical frequency analysis ranges `BASS`, `MID` and `TREBLE`.

### Color Repo

Start with `val colorRepo = colorRepo { … }` and set up your color repository.

Use `palette = listOf( … )` using your favorite color model. 
From `ColorRGBa` to `ColorXSVa` or even `ColorLABa`.

Then use `colorRepo[colorIndex: Int]` to get a color from your palette.

### Visual Groups

Start with `val myVisualGroup = object: VisualGroup { … }` or write your own class, 
to inherit from visual group.

This is the main API to create your own ... well, visual groups.
This allows to organize your components and define an isolated draw procedure for each group.

Implement the `Drawer.draw()` method to draw your visuals.
Then, call `myVisualGroup.draw()`, as well as the other groups' draw function, inside your draw loop.

### Input Scheme

Start with `val inputScheme = inputScheme(myInputDevice: KeyEvents) { … }` and set up your input scheme.

The template uses the default `keyboard` input device, but you can provide your own device interface.

For the keyboard, keys can always be used by their name, like `'escape'` or `'k'`, 
or by their layout-independent key code, if they have one, like `KEY_ESCAPE` (`'k'` doesn't have one).

Use `inputScheme.keyDown { … }` and specify `String/Int.bind(myDescription) { myAction() }` 
to bind keys to actions.

You can also track keys by different tracking styles: `PIANO` and `TOGGLE`.
Piano tracking treats a key as active as long as it is pressed.
Toggle tracking treats a key as inactive at first, but toggles its active state on every press.

Use their active state anywhere with `inputScheme.isKeyActive(myKey: String/Int)`.

In general, you can unbind/untrack keys and dynamically change key bindings during runtime.

### Render Pipeline and FxRepo

Start with `val renderPipeline = RenderPipeline(widht, height, drawer)` 
and init a render pipeline with a visual effect repository `fxRepo` and 
some standard renderTargets/colorBuffers prepared.

Provides a setup for a graphics pipeline, similar to vanilla OPENRNDR `compose { … }` blocks.
Those seem to be wasteful with resources, however. So I wrote my own wrapper.

Define your effect chain with `fxRepo` and apply during your procedure. 
Typically at the end, but hey - no on is stopping you to use it in the beginning or the middle. Or multiple times.

# Appendix

## Roadmap

I am happy with the current state of the template, but there is more to come.

Several APIs are planned as listed below.
Most current APIs are subject to change, either by refactoring or by adding new features. 
So just expect some basic integration.

For the progress, I marked those tasks as done, that are stable and used in the template.
All the other tasks require some more work.

### Template Progress

- [x] Executable Template

### Presented APIs
  - [x] `BeatClock`
  - [x] `Audio`
  - [x] `ColorRepo`
  - [x] `VisualGroup`
  - [x] `InputScheme`
  - [x] `FxRepo`
    - [x] `MirrorFx` 
    - [x] `GalaxyShadeStyle`
  - [x] `UiDisplay`
  - [x] `Realtime filters`, currently just OneEuroFilter

### API Progress
- [x] BeatClock
  - [x] Beat tracking
- [ ] Audio API (stable during runtime, but has bug on closing audio stream)
  - [x] Audio dispatch logic
  - [ ] Setup based on system settings/ used processors
    - [ ] Audio device selection
    - [ ] Fixing occasional zombie audio input stream after app exit (killable with force-quit) 
  - [x] General Volume Processor
  - [x] Range-Specific Volume processor (ConstantQ)
  - [ ] Source → Cache/Filter middleware → Value provider (Refactoring)
- [x] Color API
  - [x] Color Repo
  - [x] Color Palette
  - [ ] Color Picker
  - [ ] Sampling, like linear blend or along a color path
- [x] Content API
  - [x] Visual group for organizing content
  - [x] Inline-object property/function definition and access
- [x] Input Scheme API
  - [x] Input Scheme definition
  - [ ] Common Devices 
    - [x] Keyboard, by name or key code
    - [ ] Mouse
    - [ ] MIDI
    - [ ] Probably some Hardware Abstraction Layer
  - [x] Key tracking
    - [x] PIANO style
    - [x] TOGGLE style
    - [x] track/untrack
  - [x] Key binding
    - [x] bind/unbind
  - [ ] Hard-bound keys (like `f1` for hiding the UI, `escape` for exiting the app)
  - [ ] Input recording
    - [ ] Binary, Analog 1D, Analog 2D
    - [ ] Mouse recording (with pre-calibration)
      - [ ] Linear sampling, 2D sampling
      - [ ] As Envelope, binding to animation
- [x] RenderPipeline API
  - [x] Chain definition and usage
  - [x] FxRepo
- [x] UiDisplay API
  - [x] Basic UI 
  - [x] Can be hidden
  - [x] Tracking values and displaying them

### Long-Term Goals

* Scenes API
  * Scene management 
  * Scene transitions 
    * Cross-Fading
    * Key Rebinding
* FFMPEG Video API
  * VisualGroup-based Video player
  * Start, Stop, Move to time, etc.
* Param Picker
  * Like [orx-gui](https://github.com/openrndr/orx/tree/master/orx-jvm/orx-gui), but in a different application / interface
  * Live program or similar to change values on the fly (like OliveProgram, but only for hand-picked variables)
* [Spotify API](https://developer.spotify.com/documentation/web-api/reference) integration

## Useful References

### Framework and Language

#### OPENRNDR
Visit their [Website](https://openrndr.org/) and run your first program with the help of the [Guide](https://guide.openrndr.org/).

You can also check out their [GitHub](https://github.com/openrndr), where the [Original Template](https://github.com/openrndr/openrndr-template) also resides.

If you have questions on a topic, want to see what others have done or want to share your own creations,
you can visit the [Discourse](https://openrndr.discourse.group/) forum.

#### Kotlin
You can learn Kotlin or refresh your memory in the [Reference](https://kotlinlang.org/docs/reference/) documentation.
In particular, you might find working with [Collections](https://kotlinlang.org/docs/reference/collections-overview.html) useful.

If you want to quickly test something in Kotlin, you can go to the official [Kotlin Playground](https://play.kotlinlang.org).

### Working with music

#### Find the bpm of your song
Website to search your song's name and artist anf get its bpm. 
I found this one to be the most reliable.
* [songbpm.com](https://songbpm.com/)

#### Acoustics / "Human hearing"
Article on acoustics and musical frequency ranges
* ["Bass Mid and Treble – Frequency Ranges Explained (With Hertz Chart)"](https://housegrail.com/bass-treble-hertz-frequency-chart/)

#### TarsosDSP
Comprehensive audio analysis (and synthesis) library for the JVM. 
Provides most of the Audio API functionality.
* [GitHub](https://github.com/JorenSix/TarsosDSP)
* [ConstantQ paper](http://academics.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf)

### Working with live data

#### OneEuroFilter
Update filter for noisy live data. Useful for input filtering, like audio or mouse movement.
* paper: https://cristal.univ-lille.fr/~casiez/1euro/

### Working with shaders

### The Book Of Shaders
Highly recommended beginner online-book to learn about shaders, GPU-CPU relationship,
pros and cons of shaders, when and how to use them and much more.
Covers the basics and presents concise examples.
* [The Book Of Shaders](https://thebookofshaders.com/)

#### Inigo Quilez' Articles
Archive of articles about shaders and CG in general.
Easy to understand, highly informative.
* https://iquilezles.org/articles

### Music visualizer inspiration

#### Milkdrop 
Music visualizer, now continued as ProjectM
* original: https://www.geisswerks.com/milkdrop/
* ProjectM: https://github.com/projectM-visualizer/projectm

#### Electric Sheep (Flam3)
Fractal images that change based on genetic algorithms. Contains long list of "artistic math functions".
* S. Draves, E. Reckase. "The Fractal Flame Algorithm", Nov 2008. URL: https://flam3.com/flame_draves.pdf
