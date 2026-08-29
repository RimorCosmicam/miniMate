#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/// Uses an already-active modern OBS Camera Extension as MiniMate's system-facing
/// camera. This is a compatibility path for machines where macOS blocks DAL.
bool MMModernCameraAvailable(void);
bool MMModernCameraSendJPEG(const uint8_t *bytes, size_t length);
void MMModernCameraStop(void);

#ifdef __cplusplus
}
#endif
