# AisleSpy app icon — handoff (design 3a "recon magnifier")

## Concept
A magnifying lens inspecting a barcode — the "recon" brand read. Muted barcode bars recede on a near-black warm ground; the olive lens with pale-lime rim pulls three bars into focus.

## Files
- `ic_launcher_full.svg` — full-bleed 108×108 artwork with `#23211a` background. Master file; edit this.
- `ic_launcher_foreground.svg` — same artwork, transparent background. Use as the adaptive-icon foreground layer.
- `png/ic_launcher_{512,192,144,96,72,48}.png` — rasterized full-bleed exports (512 = Play Store listing; others = legacy densities xxxhdpi→mdpi).

## Colors
| Element | Hex |
|---|---|
| Ground | `#23211a` (matches scan screen) |
| Background barcode bars | `#faf6ee` @ 45% opacity |
| Lens fill | `#5d6633` (brand olive / `primary`) |
| Lens rim + handle | `#cdd6a3` (pale lime, scan-screen accent) |
| Magnified bars | `#faf6ee` (cream) |

## Android adaptive icon implementation
The SVG uses the standard 108dp adaptive-icon canvas; the safe zone is the center 66dp circle and the composition already fits it (lens center at 46,50; handle tip ~x87 stays inside for square/squircle masks — verify on circle mask, it clips fine).

1. Convert `ic_launcher_foreground.svg` to a `VectorDrawable` (Android Studio: New → Vector Asset → Local file). Shapes are plain rects/circles; the rotated handle rect becomes a path.
2. `res/values/ic_launcher_background.xml` → color `#23211A`.
3. `mipmap-anydpi-v26/ic_launcher.xml`:
   ```xml
   <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
     <background android:drawable="@color/ic_launcher_background"/>
     <foreground android:drawable="@drawable/ic_launcher_foreground"/>
     <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
   </adaptive-icon>
   ```
4. **Monochrome layer (themed icons, Android 13+):** derive from the foreground — single-color silhouette: lens rim as a 6dp ring + the three magnified bars + handle, all `#FFFFFF` in the drawable (system tints it). Drop the background bars — too fine for themed rendering.
5. Legacy mipmaps: use the provided PNGs (48→mdpi, 72→hdpi, 96→xhdpi, 144→xxhdpi, 192→xxxhdpi) or let Image Asset Studio generate them from the layers.
6. Play Store: `png/ic_launcher_512.png` (Play requires no alpha for the listing icon — the full-bleed version has none).

## Do
- Keep the flat, no-gradient rendering.
- Keep the lens off-center left with handle to bottom-right.

## Don't
- Don't add shadows, gloss, or a border.
- Don't recolor the lens to a score-band color (green/amber/red) — the icon is brand, not a verdict.
- Don't put the full artwork in the foreground layer of the adaptive icon (it would double-draw the ground; use the transparent-bg file).
