package com.minimate.ui.components

import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.EdgeControlMaterial
import com.minimate.touchpad.model.EdgeControlSide
import com.minimate.touchpad.model.HapticIntensity
import com.minimate.touchpad.model.KeyboardFont
import com.minimate.touchpad.model.KeyboardLanguage
import com.minimate.touchpad.model.ClockStyle
import com.minimate.touchpad.model.KeyboardTheme
import com.minimate.touchpad.model.withKeyboardTheme
import com.minimate.touchpad.model.KeyboardTrail
import com.minimate.touchpad.model.MicrophonePlacement
import com.minimate.touchpad.model.ShaderFamily
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.touchpad.model.WebcamResolution
import com.minimate.touchpad.model.panelThemes
import com.minimate.touchpad.model.sceneById
import com.minimate.touchpad.model.scenesInFamily

/**
 * Builds the command tree.
 *
 * Depth is capped at three because that is the point past which a menu stops being navigable on a
 * screen this size: category, group, then the control itself. Anything that would need a fourth
 * level is either a studio that opens over the canvas, or does not belong in a menu.
 */
/** Which floating panel a menu action refers to. */
enum class PanelTarget { AUDIO, CAMERA }

/**
 * The page the bar was opened over.
 *
 * The bar offers what the page in front of it can actually do. Listing the camera's capture sizes
 * while the audio panel is up describes somewhere else, and it is why the first column came out a
 * different length on every page for no reason a reader could see.
 */
enum class CommandContext { TRACKPAD, AUDIO, CAMERA, KEYBOARD }

fun buildCommandMenu(
    context: CommandContext,
    settings: TouchpadSettings,
    onChange: (TouchpadSettings) -> Unit,
    onOpenSceneStudio: () -> Unit,
    onOpenKeyboardStudio: () -> Unit,
    onOpenEdgeStudio: () -> Unit,
    onOpenPillEditor: () -> Unit,
    onEditPanels: (PanelTarget) -> Unit,
    onPairNewDevice: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDisconnect: () -> Unit,
    pairedSummary: String
): List<MenuNode> {
    val scene = sceneById(settings.shaderSceneId)

    val sceneBranch = MenuBranch(
        "Scene", listOf(
            MenuAction("Open Scene Studio") { onOpenSceneStudio() },
            MenuBranch("Choose", ShaderFamily.entries.map { family ->
                MenuBranch(family.label, scenesInFamily(family).map { option ->
                    MenuAction(option.label) {
                        onChange(
                            settings.copy(
                                shaderSceneId = option.id,
                                shaderParams = option.defaults,
                                shaderPaletteIndex = 0
                            )
                        )
                    }
                })
            }),
            MenuBranch("Palette", scene.palettes.mapIndexed { index, palette ->
                MenuAction(palette.label) { onChange(settings.copy(shaderPaletteIndex = index)) }
            }),
            MenuBranch("Tune", scene.params.mapIndexed { index, param ->
                val values = settings.shaderParams.takeIf { it.size == scene.params.size } ?: scene.defaults
                MenuSlider(
                    param.label,
                    values.getOrElse(index) { param.default },
                    param.min..param.max,
                    "%.2f".format(values.getOrElse(index) { param.default })
                ) { next ->
                    val updated = values.toMutableList()
                    while (updated.size < scene.params.size) updated += 0f
                    updated[index] = next
                    onChange(settings.copy(shaderParams = updated))
                }
            }),
            MenuBranch("Filters", ThemeFilter.entries.map { filter ->
                MenuToggle(
                    filter.label,
                    if (filter == ThemeFilter.NONE) settings.themeFilters.isEmpty()
                    else filter in settings.themeFilters
                ) {
                    onChange(
                        settings.copy(
                            themeFilters = when {
                                filter == ThemeFilter.NONE -> emptyList()
                                filter in settings.themeFilters -> settings.themeFilters - filter
                                else -> settings.themeFilters + filter
                            }
                        )
                    )
                }
            }),
            MenuBranch("Look", listOf(
                MenuSlider(
                    "Aberration", settings.shaderAberration, 0f..2f,
                    "%.1f".format(settings.shaderAberration)
                ) { onChange(settings.copy(shaderAberration = it)) },
                MenuSlider(
                    "Grain", settings.shaderGrain, 0f..1f,
                    "%.2f".format(settings.shaderGrain)
                ) { onChange(settings.copy(shaderGrain = it)) }
            )),
            MenuBranch("Motion", buildList {
                BackgroundAnimation.entries.forEach { motion ->
                    add(MenuToggle(motion.label, settings.backgroundAnimation == motion) {
                        onChange(settings.copy(backgroundAnimation = motion))
                    })
                }
                add(
                    MenuSlider(
                        "Touch", settings.shaderTouchStrength, 0f..2f,
                        "%.1f".format(settings.shaderTouchStrength)
                    ) { onChange(settings.copy(shaderTouchStrength = it)) }
                )
            })
        )
    )

    val panelsBranch = MenuBranch(
        "Panels", listOf(
            MenuBranch("Theme", panelThemes.mapIndexed { index, theme ->
                MenuToggle(theme.label, index == settings.panelThemeIndex) {
                    onChange(settings.copy(panelThemeIndex = index))
                }
            }),
            // Arranging a panel means arranging a specific one, and the panel has to be on
            // screen to be dragged. Choosing here opens that page already in edit mode; the
            // old single action turned on a mode with nothing visible to apply it to, which
            // switched itself straight back off.
            MenuBranch("Move & resize", listOf(
                MenuAction("Audio panel") { onEditPanels(PanelTarget.AUDIO) },
                MenuAction("Camera panel") { onEditPanels(PanelTarget.CAMERA) }
            )),
            MenuBranch("Reset", listOf(
                MenuAction("Audio panel") {
                    onChange(settings.copy(audioPanelX = .5f, audioPanelY = .46f, audioPanelScale = 1f))
                },
                MenuAction("Camera panel") {
                    onChange(settings.copy(cameraPanelX = .5f, cameraPanelY = .46f, cameraPanelScale = 1f))
                }
            ))
        )
    )

    val trackpadBranch = MenuBranch(
        "Trackpad", listOf(
            MenuBranch("Movement", listOf(
                MenuSlider("Speed", settings.trackingSpeed, .4f..2.5f, "%.2f".format(settings.trackingSpeed)) {
                    onChange(settings.copy(trackingSpeed = it))
                },
                MenuSlider("Acceleration", settings.acceleration, .5f..2.5f, "%.2f".format(settings.acceleration)) {
                    onChange(settings.copy(acceleration = it))
                },
                MenuToggle("Invert vertical", settings.invertCursorY) {
                    onChange(settings.copy(invertCursorY = it))
                }
            )),
            MenuBranch("Scrolling", listOf(
                MenuSlider("Speed", settings.scrollSpeed, .1f..2f, "%.2f".format(settings.scrollSpeed)) {
                    onChange(settings.copy(scrollSpeed = it))
                },
                MenuToggle("Natural", settings.naturalScrolling) {
                    onChange(settings.copy(naturalScrolling = it))
                },
                MenuToggle("Momentum", settings.momentumScrolling) {
                    onChange(settings.copy(momentumScrolling = it))
                }
            )),
            MenuBranch("Gestures", listOf(
                MenuToggle("Tap to click", settings.tapToClick) { onChange(settings.copy(tapToClick = it)) },
                MenuToggle("Two-finger right click", settings.twoFingerRightClick) {
                    onChange(settings.copy(twoFingerRightClick = it))
                },
                MenuToggle("Double-tap drag", settings.doubleTapDrag) {
                    onChange(settings.copy(doubleTapDrag = it))
                },
                MenuChoice(
                    "Haptics",
                    HapticIntensity.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    settings.hapticIntensity.ordinal
                ) { index -> onChange(settings.copy(hapticIntensity = HapticIntensity.entries[index])) }
            )),
            MenuBranch("Edges", listOf(
                MenuToggle("Scroll rail", settings.edgeScrollEnabled) {
                    onChange(settings.copy(edgeScrollEnabled = it))
                },
                MenuToggle("Corner right click", settings.edgeRightClickEnabled) {
                    onChange(settings.copy(edgeRightClickEnabled = it))
                },
                MenuChoice(
                    "Rail side",
                    EdgeControlSide.entries.map { it.label },
                    settings.edgeControlSide.ordinal
                ) { index -> onChange(settings.copy(edgeControlSide = EdgeControlSide.entries[index])) },
                MenuChoice(
                    "Rail material",
                    EdgeControlMaterial.entries.map { it.label },
                    settings.edgeRailMaterial.ordinal
                ) { index -> onChange(settings.copy(edgeRailMaterial = EdgeControlMaterial.entries[index])) },
                MenuAction("Open Buttons Studio") { onOpenEdgeStudio() }
            ))
        )
    )

    val keyboardBranch = MenuBranch(
        "Keyboard", listOf(
            MenuAction("Open Keyboard Studio") { onOpenKeyboardStudio() },
            MenuBranch("Look", listOf(
                MenuChoice("Theme", KeyboardTheme.entries.map { it.label }, settings.keyboardTheme.ordinal) {
                    onChange(settings.withKeyboardTheme(KeyboardTheme.entries[it]))
                },
                MenuChoice("Trail", KeyboardTrail.entries.map { it.label }, settings.keyboardTrail.ordinal) {
                    onChange(settings.copy(keyboardTrail = KeyboardTrail.entries[it]))
                },
                MenuChoice("Font", KeyboardFont.entries.map { it.label }, settings.keyboardFont.ordinal) {
                    onChange(settings.copy(keyboardFont = KeyboardFont.entries[it]))
                },
                MenuToggle("Opaque", settings.keyboardOpaque) { onChange(settings.copy(keyboardOpaque = it)) },
                MenuToggle("Vibration", settings.keyboardHapticsEnabled) {
                    onChange(settings.copy(keyboardHapticsEnabled = it))
                },
                MenuSlider("Size", settings.keyboardScale, .8f..1.3f, "%.2f".format(settings.keyboardScale)) {
                    onChange(settings.copy(keyboardScale = it))
                }
            )),
            MenuChoice("Language", KeyboardLanguage.entries.map { it.label }, settings.keyboardLanguage.ordinal) {
                onChange(settings.copy(keyboardLanguage = KeyboardLanguage.entries[it]))
            }
        )
    )

    val audioBranch = MenuBranch(
        "Audio", listOf(
            MenuToggle("Microphone", settings.audioMicrophoneEnabled) {
                onChange(settings.copy(audioMicrophoneEnabled = it))
            },
            MenuSlider("Mic gain", settings.audioMicrophoneGain, 0f..3f, "%.1fx".format(settings.audioMicrophoneGain)) {
                onChange(settings.copy(audioMicrophoneGain = it))
            },
            MenuBranch("Position", buildList {
                add(MenuToggle("Auto", settings.audioPlacementAuto) {
                    onChange(settings.copy(audioPlacementAuto = it))
                })
                MicrophonePlacement.entries.forEach { placement ->
                    add(
                        MenuToggle(
                            placement.label,
                            !settings.audioPlacementAuto && settings.audioMicrophonePlacement == placement
                        ) {
                            onChange(
                                settings.copy(
                                    audioPlacementAuto = false,
                                    audioMicrophonePlacement = placement
                                )
                            )
                        }
                    )
                }
            }),
            MenuToggle("Speaker", settings.audioOutputEnabled) {
                onChange(settings.copy(audioOutputEnabled = it))
            },
            MenuSlider("Volume", settings.audioOutputVolume, 0f..1f, "${(settings.audioOutputVolume * 100).toInt()}%") {
                onChange(settings.copy(audioOutputVolume = it))
            }
        )
    )

    val cameraBranch = MenuBranch(
        "Camera", listOf(
            MenuToggle("Enabled", settings.webcamEnabled) { onChange(settings.copy(webcamEnabled = it)) },
            MenuBranch("Capture", listOf(
                MenuChoice("Size", WebcamResolution.entries.map { it.label }, settings.webcamResolution.ordinal) {
                    onChange(settings.copy(webcamResolution = WebcamResolution.entries[it]))
                },
                MenuChoice(
                    "Frame rate",
                    listOf("15", "20", "24", "30"),
                    listOf(15, 20, 24, 30).indexOf(settings.webcamFps).coerceAtLeast(0)
                ) { index -> onChange(settings.copy(webcamFps = listOf(15, 20, 24, 30)[index])) },
                MenuToggle("Mirror", settings.webcamMirror) { onChange(settings.copy(webcamMirror = it)) }
            )),
            MenuInfo(
                "Heat",
                "Larger sizes and higher frame rates cost encode time and radio throughput, both of which become heat. Capture drops its own frame rate automatically when the device warms."
            )
        )
    )

    val pillBranch = MenuBranch(
        "Pill", listOf(
            MenuAction("Move & size") { onOpenPillEditor() },
            MenuToggle("24-hour", settings.show24HourFormat) { onChange(settings.copy(show24HourFormat = it)) },
            MenuToggle("Seconds", settings.showSeconds) { onChange(settings.copy(showSeconds = it)) },
            MenuToggle("Battery", settings.showBatteryPercentage) {
                onChange(settings.copy(showBatteryPercentage = it))
            }
        )
    )

    val pairingBranch = MenuBranch(
        "Pairing", listOf(
            MenuInfo("Status", pairedSummary),
            MenuAction("Pair new device") { onPairNewDevice() },
            MenuAction("Refresh") { onRefreshDevices() },
            MenuAction("Disconnect") { onDisconnect() }
        )
    )

    // Pairing is on every page: it is the state of the whole app, not of one screen. The trackpad
    // carries the scene and the pill, because it is the page on which you can actually see them.
    return when (context) {
        CommandContext.TRACKPAD -> listOf(sceneBranch, trackpadBranch, pillBranch, pairingBranch)
        CommandContext.AUDIO -> listOf(audioBranch, panelsBranch, pairingBranch)
        CommandContext.CAMERA -> listOf(cameraBranch, panelsBranch, pairingBranch)
        CommandContext.KEYBOARD -> listOf(keyboardBranch, pairingBranch)
    }
}
