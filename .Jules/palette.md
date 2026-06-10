## 2025-01-24 - [RTL and Accessibility Patterns in Android]
**Learning:** When developing for RTL languages like Arabic in Android, using `marginStart/End` instead of `marginLeft/Right` is crucial for automatic layout mirroring. Additionally, always ensuring `contentDescription` is present on icons is a fundamental accessibility win.
**Action:** Default to `marginStart/End` for all layouts and audit all `ImageViews` for missing `contentDescription`.

## 2025-01-24 - [Empty State UX]
**Learning:** A blank list can be confusing for users. Providing a clear empty state with localized text (`no_devices`) improves the perceived quality and usability of the app.
**Action:** Always implement a conditional empty state view when using `RecyclerView`.
