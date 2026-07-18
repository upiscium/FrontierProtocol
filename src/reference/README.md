# Legacy protection reference

This source tree is intentionally outside every Gradle source set and is not
compiled or packaged in the release JAR. It preserves the R0 protection
implementation for R2 and R3 design reference, including:

- Block Entity load, chunk-unload, and removal lifecycle
- One-slot inventory and item capability
- Inventory, fuel, and grace-period NBT persistence
- Fuel-to-grace-to-offline state transitions
- Loaded Block Entity indexing
- Initial spawn lookup and SavedData mechanics
- The related unit and GameTest examples

These files are historical reference, not active compatibility code. New R2
and R3 code must use the final suppression semantics rather than restoring mob
block-break or explosion protection.

`ProtectionGeometry` remains active production source under `src/main` and is
not duplicated here.
