# Design system — Material Design 3

- framework: material
- implementation: native Jetpack Compose Material 3
- seed-color: #6750a4 (M3 baseline fallback)
- Android 12+: dynamic system color; older devices: baseline M3 scheme.
- Light/dark appearance follows Android's system setting. No network fonts.
- Typography: Compose M3 Typography / Android system Roboto.
- Surfaces, states, shapes and elevation use MaterialTheme and stock M3 components.
- Compact navigation: bottom navigation plus More drawer for secondary destinations.
- Medium navigation: rail; expanded: permanent navigation drawer.
- Existing records, privacy controls and workflows stay intact.

## Components

Scaffold, TopAppBar, NavigationBar, NavigationRail, NavigationDrawer, tonal filled
cards, filled/outlined buttons, chips, text fields, checkboxes, switches, progress
indicators and dialogs use Compose Material 3. Dashboard emphasizes today's
progress and next tasks. No decorative fake statistics.

## Assets

Home, task, calendar and focus vector icons copied from the user-provided
`the original local material-design-icons collection` Android rounded icon resources.
Apache 2.0 license is retained at `docs/MATERIAL_ICONS_LICENSE.txt`.
Task start/end audio supplied by the user is bundled locally in `res/raw`.

2026-10-19: Replaced the custom charcoal/lime interface with native Material 3.

2026-10-19: New original A18 logo generated with the built-in image tool. Rounded
indigo/periwinkle lettering integrates a check detail. Transparent PNG is shared by
the launcher and navigation header, with a pale lavender background for contrast.
Prompt and asset provenance: `docs/LOGO.md`.

2026-10-19: Native starting window and Android 12+ splash use the A18 logo on
lavender. Compose continues with a Material surface, branded badge, indeterminate
progress and loading text until local initialization and the first snapshot are
ready. Failure replaces progress with an accessible retry action. Fresh launches
open To-do list; there is no artificial timer or fabricated loading percentage.
