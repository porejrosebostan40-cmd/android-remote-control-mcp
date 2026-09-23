# Android 11 compatibility branch

This branch targets Android 11 (API 30) and later.

## Compatibility changes

- minSdk is 30.
- API 33 AccessibilityInputConnection / InputMethod Editor support is not used.
- Text entry uses AccessibilityNodeInfo.ACTION_SET_TEXT with local cursor/selection tracking.
- Framework accessibility-cache invalidation is used only on API 33+; Android 11/12 safely skip that optional operation.
- Android 11 shared-media access uses READ_EXTERNAL_STORAGE.
- Android 13 notification permission requests are gated by SDK level.
- The accessibility-service XML does not request the API 33 input-method-editor flag.
- Screenshot capture continues to use AccessibilityService.takeScreenshot(), which is available from API 30.

## Known compatibility trade-off

The API 33 natural AccessibilityInputConnection typing path is replaced by accessibility-node text replacement. This keeps the MCP text tools functional on Android 11, but some applications/WebViews may expose editable fields that do not support ACTION_SET_TEXT. Those cases should report an action failure rather than silently claiming success.

The official main branch remains Android 13+; this branch is intentionally isolated for Android 11 devices.