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

The Overstressed diagnostic is covered by unit tests but was not reproduced with a graphical kinetic-network fixture during this pass.

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
- Production shows the populated base plate and Mixing, Deploying, Mechanical Crafting, and continuous-delivery guidance.
- Operation, Coverage, and Production were played in English and Japanese without a scene crash.
- The final 1-based localization entries render translated text rather than raw keys.
- Returning to the world after Ponder left the live Stabilizer fixture operational.

Physical Mechanical Crafter execution and continuous automated Cell delivery remain separate production smoke checks and were not verified here.

## Result

The R8 in-world client smoke for Goggles, range visualization, item tooltips, Ponder playback, and Japanese localization passed. The client accepted a normal window-close request and its Gradle and game processes exited. The graphical pass found and corrected GRACE diagnostic priority, Ponder localization indexing, Japanese state naming, empty schematic bounds, and scene framing. Automated verification and dedicated-server results are recorded separately in [R8 Operational UX](r8-operational-ux.md) and [Alpha Release](alpha-release.md).
