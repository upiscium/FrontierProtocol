# R8 Graphical Verification

## Environment

- Date: 2026-07-28
- Branch: `feat/r8-operational-ux`
- Initial baseline: `69dd2893c5c90ed62e1a9c32173aec7c6509049a`
- Command: `./gradlew runClient -PenableRecipeViewerRuntime`
- Display: Xvfb at 1920x1080
- Renderer: llvmpipe software rendering
- World: existing creative smoke world `aR6 Create Smoke`
- Languages: English and Japanese

Resource reload alone was not accepted as a client smoke result. The client joined the world and the checks below used normal mouse and keyboard interaction, live machines, inventory items, Engineer's Goggles, and Ponder playback.

## Verified

### Engineer's Goggles

- English and Japanese targeted displays render without raw translation keys and remain inside the 1920x1080 viewport.
- Tier 1, Tier 2, and Tier 3 report their 32, 64, and 128 RPM requirements and 1, 9, and 25 suppressed chunks.
- OFFLINE diagnostics were checked for no rotation, insufficient RPM, and no Cell.
- ACTIVE displays were checked at 32, 64, and 128 RPM.
- GRACE_PERIOD reports the grace diagnostic instead of a misleading no-rotation diagnostic.
- Shift details show the center chunk, exact X/Z chunk bounds, full-dimension-height behavior, Stress impact, and Cell duration.
- Reducing Tier 2 capacity from 32 to 8 preserves and displays the existing `32 / 8` inventory. The setting was restored after the check.
- `showStabilizerGoggleDetails = false` hides Frontier Protocol details. The default was restored after the check.
- Removing Engineer's Goggles immediately removes the targeted display. Re-equipping them restores it, and moving the crosshair off the machine clears it again.

The initial user verification confirmed the Overstressed diagnostic in addition to its unit-test coverage.

### Range Overlay

- Tier 1, Tier 2, and Tier 3 render 1x1, 3x3, and 5x5 chunk grids.
- ACTIVE uses the green treatment and OFFLINE uses red.
- A negative-coordinate fixture in chunk `-2, -3` produced X bounds `-3..-1` and Z bounds `-4..-2`.
- Terrain depth testing, internal chunk lines, and vertical corners were visible.
- Disabling `showStabilizerRangeOverlay` hides the overlay.
- Disabling `rangeOverlayRequiresSneaking` allows display without Shift.
- Disabling `showRangeVerticalCorners` retains the horizontal grid without corner lines.
- All client options were restored to their defaults after verification.

### Item Guidance

- Tier 1 Stabilizer, Stabilization Cell, and Stabilization Compound tooltips were checked in English and Japanese.
- Normal tooltips show the summary, role, Ponder hint, and Shift hint.
- Shift tooltips show the extended guidance and stay inside the viewport.
- No raw translation key was visible.

### Ponder

- Ponder was opened by holding its configured key over Tier 1 Stabilizer and Stabilization Cell items.
- Tier 1, Tier 2, and Tier 3 Operation scenes show the matching Stabilizer block. The Cell uses the documented Tier 1 representative scene.
- Operation shows a base plate, Shaft, Chute, Depot, Cell prompt, kinetic state changes, grace, and offline progression.
- Coverage shows the populated tile grid and 1x1, 3x3, and 5x5 outlines.
- Operation, Coverage, and Production were played in English and Japanese without a scene crash.
- The final 1-based localization entries render translated text rather than raw keys.
- Returning to the world after Ponder left the live Stabilizer fixture operational.

Physical Mechanical Crafter execution and continuous automated Cell delivery remain separate production smoke checks and were not verified here.

### Production Ponder Correction

Initial user verification established that Stabilizer operation, overlays, tooltips, localization, and Overstressed behavior passed. Compound and Cell Production Ponder played, but did not visibly render the expected processing equipment. The earlier base-plate and guidance-only playback must not be treated as equipment verification.

The revised scene was replayed from the Cell entry in both English and Japanese. The first revision again showed only item overlays because dynamically added blocks had no positions in the initial schematic. The final schematic reserves all 12 equipment positions, after which the complete scene rendered correctly:

- The Compound stage visibly shows a TFMG fluid supply, Fluid Pipe, Basin, Mechanical Mixer, Shaft, and Depot. Its overlays show the fluid bucket, Sand, Blue Ice, eight Iron Nuggets, and one Compound output.
- The Cell stage visibly shows a powered Deployer, Shaft, and Depot with Compound and Iron Sheet inputs and one Cell output.
- The Stabilizer stage visibly shows a 3x3 Mechanical Crafter wall and Shaft, followed by Tier 1 output and Tier 2/Tier 3 upgrade guidance.
- All equipment and text remained inside the 1920x1080 viewport. English and Japanese playback completed without raw keys or a scene crash.

JEI displayed one unheated Mixing recipe with TFMG's `tfmg:molten_plastic` fluid localized as Molten Plastic at 100 mB, Sand, Blue Ice, eight Iron Nuggets, one Compound output, and `No Heating Required`. The unchanged Deploying recipe displayed Compound plus an Iron Sheet producing one Cell. The old Spore Biomass, Redstone, Charcoal, and Water recipe was absent from the one-page Compound recipe view.

## Result

The R8 in-world client smoke for Goggles, range visualization, item tooltips, JEI recipes, Ponder playback, and Japanese localization passed. The client returned through Save and Quit, accepted a normal Quit request, and its Gradle and game processes exited. The graphical passes found and corrected GRACE diagnostic priority, Ponder localization indexing, Japanese state naming, empty schematic bounds, scene framing, and missing Production equipment positions. Automated verification and dedicated-server results are recorded separately in [R8 Operational UX](r8-operational-ux.md) and [Alpha Release](alpha-release.md).
