# Flexbooru

This is my mod on flexbooru(checkout branch: dev not master) and all the changes in my build is listed below:
1. all the dev booru are added by default and removed limit
2. speed loading of posts for smooth experience   - enable preloading for pager adapter
3. added post browse tap on screen to simulate swipe left/right for larger screens
4. fixed tags in post fragment not showing relevant tags to the user
5. changed from name to count query in searchtag for post fragment
6. added support for hydrus -- https://github.com/hydrusnetwork/hydrus
7. added support for idolcomplex
8. added play button for gif and video
    - gif play btn for saving bandwidth
    - video play btn that shows preview instead of blank as vid loads
9. added nsfw only and safe mode only modes in settings

Still a lot of bugfixes and additions to be made. DM me on flexbooru discord for beta testing,bugfixes and suggestions :)

A booru client for Android, support [Danbooru](https://github.com/r888888888/danbooru), [Moebooru](https://github.com/moebooru/moebooru), Gelbooru, Sankaku, etc.

[![Telegram](https://img.shields.io/badge/chat-Telegram-blue.svg)](https://t.me/flexbooru)
[![Discord](https://img.shields.io/discord/555912761742458880.svg?label=discord)](https://discord.gg/zxAX5Jh)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Downloads](https://img.shields.io/github/downloads/flexbooru/flexbooru/total.svg)](https://github.com/flexbooru/flexbooru/releases)
[![Language: Kotlin](https://img.shields.io/github/languages/top/flexbooru/flexbooru.svg)](https://github.com/flexbooru/flexbooru/search?l=kotlin)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg?label=donate)](https://www.paypal.me/fiepi)

## Translate
Click on this [link](https://crowdin.com/project/flexbooru) and you can translate this app into your language.

## Downlad
<a href="https://play.google.com/store/apps/details?id=onlymash.flexbooru.play"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="48"></a>

## Screenshot
<img src="art/screenshot.webp" height="300" hspace="20">

## Thanks to

- [OkHttp:](https://github.com/square/okhttp) An HTTP+HTTP/2 client for Android and Java applications. 
- [Ktor:](https://github.com/ktorio/ktor) A framework for quickly creating web applications in Kotlin with minimal effort.
- [Retrofit:](https://github.com/square/retrofit) Type-safe HTTP client for Android and Java by Square.
- [Gson:](https://github.com/google/gson) A Java serialization/deserialization library to convert Java Objects into JSON and back.
- [TikXml:](https://github.com/Tickaroo/tikxml) Modern XML Parser for Android.
- [Glide:](https://github.com/bumptech/glide) An image loading and caching library for Android focused on smooth scrolling.
- [Picasso:](https://github.com/square/picasso) A powerful image downloading and caching library for Android.
- [MaterialDrawer:](https://github.com/mikepenz/MaterialDrawer) A drawer with material 2 design.
- [SimpleMenuPreference:](https://github.com/takisoft/preferencex-android) A preference displaying a simple menu, originally implemented by RikkaW. On pre-Lollipop devices it falls back to a ListPreference as the older devices can't handle elevation and animation properly introduced in API 21.
- [FlexboxLayout:](https://github.com/google/flexbox-layout) A library project which brings the similar capabilities of CSS Flexible Box Layout Module to Android.
- [PhotoView:](https://github.com/chrisbanes/PhotoView) Implementation of ImageView for Android that supports zooming, by various touch gestures.
- [SubsamplingScaleImageView:](https://github.com/davemorrissey/subsampling-scale-image-view) Highly configurable, easily extendable deep zoom view for displaying huge images without loss of detail. Perfect for photo galleries, maps, building plans etc.
- [ExoPlayer:](https://github.com/google/ExoPlayer) An application level media player for Android.
- [Kodein-DI:](https://github.com/Kodein-Framework/Kodein-DI) A very simple and yet very useful dependency retrieval container. It is very easy to use and configure.
- [Muzei:](https://github.com/romannurik/muzei) A live wallpaper that gently refreshes your home screen each day with famous works of art. It also recedes into the background, blurring and dimming artwork to keep your icons and widgets in the spotlight. Simply double touch the wallpaper or open the Muzei app to enjoy and explore the artwork in its full glory.
