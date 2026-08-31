package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.minimate.R
import com.minimate.ui.theme.Mont
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.touchpad.model.KeyboardShortcut
import com.minimate.touchpad.model.KeyboardLanguage
import com.minimate.touchpad.model.KeyboardFont
import com.minimate.touchpad.model.KeyboardFontWeight as KeyboardWeightSetting
import com.minimate.touchpad.model.KeyboardTrail
import com.minimate.touchpad.model.KeyboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot

private const val MOD_CTRL = 0x01
private const val MOD_SHIFT = 0x02
private const val MOD_OPTION = 0x04
private const val MOD_COMMAND = 0x08

private enum class KeyboardPanel(val label: String) {
    TYPE("Type"), SYMBOLS("123"), MAC("Mac"), SHORTCUTS("Shortcuts"), MEDIA("Media")
}

/** How long the release ring lives. Long enough to register, short enough not to trail typing. */
private const val FLASH_NANOS = 190_000_000f

/** Clears the clock pill, which is the only way in and out of this sheet. */
private val STUDIO_TOP = 50.dp

/** The keyboard never starts lower than this, so it cannot be pushed into the camera cutout. */
private val KEYBOARD_TOP_LIMIT = 196.dp

private enum class KeyboardCustomizer(val label: String) {
    THEME("Themes"), TRAIL("Trail"), FONT("Font"), SIZE("Size")
}

private fun composeFont(font: KeyboardFont): FontFamily = when (font) {
    KeyboardFont.SYSTEM -> FontFamily.Default
    KeyboardFont.MONT -> Mont
    KeyboardFont.MONO -> FontFamily.Monospace
    KeyboardFont.PIXEL -> FontFamily.Monospace
    KeyboardFont.SERIF -> FontFamily.Serif
}

private fun composeWeight(weight: KeyboardWeightSetting): FontWeight = when (weight) {
    KeyboardWeightSetting.LIGHT -> FontWeight.Light
    KeyboardWeightSetting.REGULAR -> FontWeight.Normal
    KeyboardWeightSetting.BOLD -> FontWeight.Bold
}

/** Mont as a platform typeface, for the Canvas-drawn key grid. */
@Composable
private fun rememberKeyTypeface(font: KeyboardFont, weight: KeyboardWeightSetting): android.graphics.Typeface {
    val context = LocalContext.current
    return remember(font, weight) {
        if (font == KeyboardFont.MONT) {
            val resource = when (weight) {
                KeyboardWeightSetting.LIGHT -> R.font.mont_light
                KeyboardWeightSetting.REGULAR -> R.font.mont_regular
                KeyboardWeightSetting.BOLD -> R.font.mont_black
            }
            runCatching { ResourcesCompat.getFont(context, resource) }.getOrNull()
                ?: androidFont(font, weight)
        } else androidFont(font, weight)
    }
}

private fun androidFont(font: KeyboardFont, weight: KeyboardWeightSetting): android.graphics.Typeface {
    val family = when (font) {
        KeyboardFont.SYSTEM -> "sans-serif"
        // Mont is a resource font and cannot be resolved by family name; the caller supplies the
        // real typeface and this is only the fallback if that lookup fails.
        KeyboardFont.MONT -> "sans-serif"
        KeyboardFont.MONO -> "monospace"
        KeyboardFont.PIXEL -> "monospace"
        KeyboardFont.SERIF -> "serif"
    }
    val numericWeight = when (weight) {
        KeyboardWeightSetting.LIGHT -> 300
        KeyboardWeightSetting.REGULAR -> 400
        KeyboardWeightSetting.BOLD -> 700
    }
    return android.graphics.Typeface.create(android.graphics.Typeface.create(family, android.graphics.Typeface.NORMAL), numericWeight, false)
}

@Composable
private fun KeyboardCustomizerTop(
    section: KeyboardCustomizer,
    theme: KeyboardTheme,
    trail: KeyboardTrail,
    font: KeyboardFont,
    fontWeight: KeyboardWeightSetting,
    opaque: Boolean,
    scale: Float,
    onSection: (KeyboardCustomizer) -> Unit,
    onTheme: (KeyboardTheme) -> Unit,
    onTrail: (KeyboardTrail) -> Unit,
    onFont: (KeyboardFont) -> Unit,
    onFontWeight: (KeyboardWeightSetting) -> Unit,
    onOpaque: (Boolean) -> Unit,
    onScale: (Float) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when (section) {
        KeyboardCustomizer.THEME -> theme.label
        KeyboardCustomizer.TRAIL -> trail.label
        KeyboardCustomizer.FONT -> "${font.label} · ${fontWeight.label}"
        KeyboardCustomizer.SIZE -> "%.0f%%".format(scale * 100)
    }
    StudioPanel("Keyboard Studio", subtitle, onCancel, onDone, modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyboardCustomizer.entries.forEach { item ->
                StudioChip(item.label, section == item, Modifier.weight(1f)) { onSection(item) }
            }
        }
        when (section) {
            KeyboardCustomizer.THEME -> KeyboardTheme.entries.chunked(5).forEach { themes ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    themes.forEach { option ->
                        StudioChip(option.label, theme == option, Modifier.weight(1f)) { onTheme(option) }
                    }
                }
            }
            KeyboardCustomizer.TRAIL -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyboardTrail.entries.forEach { option ->
                    StudioChip(option.label, trail == option, Modifier.weight(1f)) { onTrail(option) }
                }
            }
            KeyboardCustomizer.FONT -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyboardFont.entries.forEach { option ->
                        StudioChip(option.label, font == option, Modifier.weight(1f)) { onFont(option) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyboardWeightSetting.entries.forEach { option ->
                        StudioChip(option.label, fontWeight == option, Modifier.weight(1f)) { onFontWeight(option) }
                    }
                }
            }
            KeyboardCustomizer.SIZE -> Slider(
                value = scale,
                onValueChange = onScale,
                valueRange = 0.65f..1.3f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.18f))
            )
        }
        if (section == KeyboardCustomizer.THEME) {
            Row(Modifier.fillMaxWidth()) {
                StudioChip(if (opaque) "Opaque keys" else "Transparent keys", opaque, Modifier.fillMaxWidth()) {
                    onOpaque(!opaque)
                }
            }
        }
    }
}

private data class SwipeKey(val label: String, val character: Char?, val usage: Int, val rect: Rect)

private fun swipeKeyLayout(width: Float, density: Float): List<SwipeKey> {
    if (width <= 0f) return emptyList()
    val gap = 4f * density
    val rowHeight = 37f * density
    val rowGap = 5f * density
    val keys = mutableListOf<SwipeKey>()
    fun addRow(text: String, row: Int, inset: Float = 0f) {
        val available = width - inset * 2f - gap * (text.length - 1)
        val keyWidth = available / text.length
        val top = row * (rowHeight + rowGap)
        text.forEachIndexed { index, char ->
            val left = inset + index * (keyWidth + gap)
            keys += SwipeKey(char.toString(), char.lowercaseChar(), 0x04 + (char.lowercaseChar() - 'a'), Rect(left, top, left + keyWidth, top + rowHeight))
        }
    }
    addRow("QWERTYUIOP", 0)
    addRow("ASDFGHJKL", 1, 10f * density)

    val weights = listOf(1.3f) + List(7) { 1f } + listOf(1.3f)
    val totalWeight = weights.sum()
    val unit = (width - gap * (weights.size - 1)) / totalWeight
    var left = 0f
    val top = 2 * (rowHeight + rowGap)
    val labels = listOf("⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫")
    labels.forEachIndexed { index, label ->
        val keyWidth = unit * weights[index]
        val char = label.singleOrNull()?.takeIf { it in 'A'..'Z' }?.lowercaseChar()
        val usage = when (label) {
            "⇧" -> -1
            "⌫" -> 0x2A
            else -> 0x04 + (char!! - 'a')
        }
        keys += SwipeKey(label, char, usage, Rect(left, top, left + keyWidth, top + rowHeight))
        left += keyWidth + gap
    }
    return keys
}

@Composable
private fun SwipeTypingPanel(
    shifted: Boolean,
    ctrl: Boolean,
    option: Boolean,
    command: Boolean,
    language: KeyboardLanguage,
    onKey: (KeyboardKey) -> Unit,
    onText: (String) -> Unit,
    onShift: () -> Unit,
    onCtrl: () -> Unit,
    onOption: () -> Unit,
    onCommand: () -> Unit,
    onHaptic: () -> Unit,
    onSpaceLongPress: () -> Unit,
    onPreview: (String) -> Unit
) {
    val density = LocalDensity.current.density
    val repeatScope = rememberCoroutineScope()
    val theme = LocalKeyboardTheme.current
    val amoled = LocalKeyboardAmoled.current
    val font = LocalKeyboardFont.current
    val fontWeight = LocalKeyboardFontWeight.current
    val trailStyle = LocalKeyboardTrail.current
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    var trail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val keys = remember(surfaceSize, density) { swipeKeyLayout(surfaceSize.width.toFloat(), density) }
    val typeface = rememberKeyTypeface(font, fontWeight)

    // A key with no travel and no highlight gives nothing back — you cannot tell a press that
    // registered from one that missed. The held key lights while it is down, and leaves a ring
    // behind it on release so a quick tap is still visible after the finger has gone.
    var heldKey by remember { mutableStateOf<Rect?>(null) }
    var flash by remember { mutableStateOf<Pair<Rect, Long>?>(null) }
    var frameNanos by remember { mutableStateOf(0L) }
    val animating = flash != null
    LaunchedEffect(animating) {
        while (animating) {
            withFrameNanos { frameNanos = it }
            flash?.let { if (frameNanos - it.second > FLASH_NANOS) flash = null }
        }
    }

    Box(
        Modifier.fillMaxWidth().height(121.dp).onSizeChanged { surfaceSize = it }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val keyBrush = if (amoled) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else when (theme) {
                KeyboardTheme.GLASS -> Brush.verticalGradient(listOf(Color.White.copy(.18f), Color.White.copy(.07f)))
                KeyboardTheme.FROST -> Brush.verticalGradient(listOf(Color.White.copy(.88f), Color.White.copy(.58f)))
                KeyboardTheme.MONT -> Brush.verticalGradient(listOf(Color(0xFF141416), Color(0xFF101012)))
                KeyboardTheme.TITANIUM -> Brush.verticalGradient(listOf(Color(0xD8EEF2F5), Color(0x9A707983), Color(0xCCCDD3D8), Color(0x88616A73), Color(0xBDAEB6BD)))
                KeyboardTheme.NOIR -> Brush.verticalGradient(listOf(Color(0xE5222427), Color(0xFA050607)))
                KeyboardTheme.PORCELAIN -> Brush.verticalGradient(listOf(Color(0xFFFDF9EE), Color(0xFFE5DDCA)))
                KeyboardTheme.TERMINAL -> Brush.verticalGradient(listOf(Color(0xEE07120B), Color(0xFA020604)))
                KeyboardTheme.SUNSET -> Brush.linearGradient(listOf(Color(0xB5FFB35F), Color(0xA8E85F88), Color(0x886C4CCF)))
                KeyboardTheme.CYBER -> Brush.verticalGradient(listOf(Color(0xDD071419), Color(0xF0010508)))
                KeyboardTheme.PAPER -> Brush.verticalGradient(listOf(Color(0xFFF4E9D2), Color(0xFFE4D3B4)))
            }
            val keyBorder = if (amoled) Color.White.copy(.74f) else when (theme) {
                KeyboardTheme.FROST -> Color.White.copy(.62f)
                KeyboardTheme.MONT -> Color.Transparent
                KeyboardTheme.TITANIUM -> Color(0xFFF2F5F7).copy(.58f)
                KeyboardTheme.NOIR -> Color(0xFFFFF8EE).copy(.34f)
                KeyboardTheme.PORCELAIN -> Color(0xFF345B99).copy(.62f)
                KeyboardTheme.TERMINAL -> Color(0xFF53FF86).copy(.60f)
                KeyboardTheme.SUNSET -> Color(0xFFFFE6C6).copy(.62f)
                KeyboardTheme.CYBER -> Color(0xFF59F3FF).copy(.68f)
                KeyboardTheme.PAPER -> Color(0xFF6B5134).copy(.48f)
                KeyboardTheme.GLASS -> Color.White.copy(.18f)
            }
            val darkText = !amoled && theme in setOf(KeyboardTheme.FROST, KeyboardTheme.PORCELAIN, KeyboardTheme.PAPER)
            val textColor = if (darkText) android.graphics.Color.rgb(17, 18, 20) else android.graphics.Color.WHITE
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 12.sp.toPx()
                this.typeface = typeface
                isAntiAlias = font != KeyboardFont.PIXEL
            }
            // Mont keys are square and hard-edged; everything else keeps the rounded cap.
            val corner = CornerRadius(if (theme == KeyboardTheme.MONT) 0f else 9.dp.toPx())
            keys.forEach { key ->
                drawRoundRect(keyBrush, key.rect.topLeft, key.rect.size, corner)
                if (heldKey == key.rect) {
                    drawRoundRect(Color.White.copy(.30f), key.rect.topLeft, key.rect.size, corner)
                }
                drawRoundRect(
                    if (heldKey == key.rect) keyBorder.copy(alpha = 1f) else keyBorder,
                    key.rect.topLeft, key.rect.size, corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                )
                val label = if (shifted && key.character != null) key.label.uppercase() else key.label
                drawContext.canvas.nativeCanvas.drawText(label, key.rect.center.x, key.rect.center.y - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
            }

            flash?.let { (rect, startedAt) ->
                val age = ((frameNanos - startedAt).toFloat() / FLASH_NANOS).coerceIn(0f, 1f)
                val grow = age * 5f * density
                drawRoundRect(
                    Color.White.copy(alpha = (1f - age) * .55f),
                    Offset(rect.left - grow, rect.top - grow),
                    androidx.compose.ui.geometry.Size(rect.width + grow * 2f, rect.height + grow * 2f),
                    corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                )
                drawRoundRect(Color.White.copy(alpha = (1f - age) * .18f), rect.topLeft, rect.size, corner)
            }
            if (trail.size > 1) {
                when (trailStyle) {
                    KeyboardTrail.AURORA -> for (index in 1 until trail.size) {
                        val phase = index.toFloat() / trail.lastIndex.coerceAtLeast(1)
                        drawLine(Color(0xFF62EEFF).copy(.24f + phase * .42f), trail[index - 1] + Offset(0f, -1.4.dp.toPx()), trail[index] + Offset(0f, -1.4.dp.toPx()), 2.8.dp.toPx())
                        drawLine(Color(0xFFFF63C6).copy(.18f + phase * .34f), trail[index - 1] + Offset(0f, 1.4.dp.toPx()), trail[index] + Offset(0f, 1.4.dp.toPx()), 2.2.dp.toPx())
                    }
                    KeyboardTrail.COMET -> for (index in 1 until trail.size) {
                        val phase = index.toFloat() / trail.lastIndex.coerceAtLeast(1)
                        drawLine(Color.White.copy(.12f + phase * .78f), trail[index - 1], trail[index], (.8f + phase * 5.2f).dp.toPx())
                        if (index == trail.lastIndex) drawCircle(Color.White.copy(.92f), 4.5.dp.toPx(), trail[index])
                    }
                    KeyboardTrail.RIBBON -> for (index in 1 until trail.size) {
                        drawLine(Color.Black.copy(.32f), trail[index - 1], trail[index], 9.dp.toPx())
                        drawLine(Color.White.copy(.74f), trail[index - 1], trail[index], 4.2.dp.toPx())
                        drawLine(Color(0xFF8FEFFF).copy(.82f), trail[index - 1], trail[index], 1.2.dp.toPx())
                    }
                    KeyboardTrail.CONSTELLATION -> for (index in 1 until trail.size) {
                        drawLine(Color.White.copy(.24f), trail[index - 1], trail[index], .8.dp.toPx())
                        if (index % 2 == 0 || index == trail.lastIndex) {
                            drawCircle(Color.White.copy(.86f), if (index == trail.lastIndex) 3.2.dp.toPx() else 1.6.dp.toPx(), trail[index])
                        }
                    }
                    KeyboardTrail.INK -> for (index in 1 until trail.size) {
                        val phase = index.toFloat() / trail.lastIndex.coerceAtLeast(1)
                        val ink = if (amoled) Color.White else Color(0xFF06070A)
                        drawLine(ink.copy(.30f + phase * .48f), trail[index - 1], trail[index], (7.5f - phase * 2f).dp.toPx())
                        drawCircle(ink.copy(.45f), (1.5f + (index % 3)).dp.toPx(), trail[index] + Offset((index % 2 * 3 - 1.5f).dp.toPx(), 2.dp.toPx()))
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxSize().pointerInput(surfaceSize, shifted) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downKey = keys.firstOrNull { it.rect.contains(down.position) }
                    var moved = false
                    var repeated = false
                    var repeatJob: Job? = null
                    trail = listOf(down.position)
                    onPreview("")
                    heldKey = downKey?.rect

                    if (downKey?.label == "⌫") {
                        onHaptic(); onKey(KeyboardKey("⌫", 0x2A))
                        repeatJob = repeatScope.launch {
                            delay(340)
                            repeated = true
                            while (true) {
                                onKey(KeyboardKey("⌫", 0x2A))
                                delay(52)
                            }
                        }
                    } else if (downKey?.character != null) {
                        repeatJob = repeatScope.launch {
                            delay(380)
                            if (!moved) {
                                repeated = true
                                onHaptic()
                                while (true) {
                                    onKey(KeyboardKey(downKey.character.toString(), downKey.usage))
                                    delay(72)
                                }
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val position = change.position
                        val distance = hypot(position.x - down.position.x, position.y - down.position.y)
                        if (distance > 10f * density && !repeated) {
                            moved = true
                            repeatJob?.cancel()
                        }
                        if (trail.lastOrNull()?.let { hypot(position.x - it.x, position.y - it.y) } ?: 99f > 3f * density) {
                            trail = trail + position
                            if (moved && trail.size >= 4) onPreview(decodeSwipeWord(trail, keys, language))
                        }
                        change.consume()
                    }
                    repeatJob?.cancel()
                    heldKey = null
                    if (!moved && downKey != null) flash = downKey.rect to System.nanoTime()

                    if (moved) {
                        val word = decodeSwipeWord(trail, keys, language)
                        if (word.isNotEmpty()) {
                            onHaptic()
                            onText((if (shifted) word.replaceFirstChar { it.uppercase() } else word) + " ")
                        }
                    } else if (!repeated && downKey != null && downKey.label != "⌫") {
                        when (downKey.label) {
                            "⇧" -> onShift()
                            else -> downKey.character?.let { onKey(KeyboardKey(it.toString(), downKey.usage)) }
                        }
                    }
                    trail = emptyList()
                    onPreview("")
                }
            }
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("⌃", .82f, ctrl, onClick = onCtrl)
        GlassKey("⌥", .82f, option, onClick = onOption)
        GlassKey("⌘", .9f, command, onClick = onCommand)
        GlassKey(language.shortLabel, 4.2f, onLongPress = onSpaceLongPress) { onKey(KeyboardKey("Space", 0x2C)) }
        GlassKey("↵", 1.15f, repeatable = true) { onKey(KeyboardKey("Return", 0x28)) }
    }
}

private val englishGlideWords = """
    a able about after again all also am an and any are as at away back be because been before being best better between big both but by
    call came can car come could day did do does done down each end even every feel find first for found from get give go going good got great
    had has have he help her here him his home how i if in into is it its just keep know last like little long look made make many may me more most
    much must my need never new next no not now of off old on one only or other our out over own people place please put really right said same say
    see she should so some something still such take tell than thank thanks that the their them then there these they thing think this those through
    time to too trackpad try two up us use very want was way we well went were what when where which while who why will with word work world would
    yes you your keyboard delete space hello hi love nice okay ok phone mac apple open close copy paste undo type typing swipe glide quick
    today tomorrow yesterday morning night soon later around always another anything everyone everything nothing someone start stop move turn
    left right top bottom inside outside small large fast slow easy hard happy sorry sure maybe probably actually already almost enough ever
""".trimIndent().split(Regex("\\s+")).distinct()

private val portugueseGlideWords = """
    a agora ainda algo alguém amanhã amor antes aqui assim até bem boa bom brasil cada casa coisa como com quando dar de dela dele depois dia
    dizer do dois e ela ele eles em então entre era essa esse esta está estamos estão eu fazer fez ficar foi gente grande há hoje isso já lá mais
    mas melhor mesmo meu minha muito na nada não nem nessa neste no nós nossa nosso nova novo nunca o onde ontem os outra outro para pela pelo
    pessoas pode por porque português pouca pouco pra qual quando que quem quero coisa realmente saber se sem ser seu sim só sobre sua também
    tem tempo tenho ter toda todo todos trabalho três tudo um uma usar vai vamos você vocês voltar aqui agora obrigado obrigada oi olá beleza certo
    telefone teclado apagar espaço copiar colar desfazer abrir fechar rápido devagar fácil difícil esquerda direita cima baixo pequeno grande
    mensagem palavra escrever escrita deslizar toque tocar segurar soltar enviar receber hoje noite manhã tarde sempre talvez quase ainda depois
    coração ação atenção informação configuração conexão edição opção posição mão mãe pão não são então também porém através até avó você mês país
    público música número único último próxima próximo português português-br função questão relação versão solução pressão aplicação
""".trimIndent().split(Regex("\\s+")).distinct()

private fun baseLetter(character: Char): Char = when (character.lowercaseChar()) {
    'á', 'à', 'â', 'ã', 'ä' -> 'a'
    'é', 'ê', 'ë' -> 'e'
    'í', 'î', 'ï' -> 'i'
    'ó', 'ô', 'õ', 'ö' -> 'o'
    'ú', 'û', 'ü' -> 'u'
    'ç' -> 'c'
    else -> character.lowercaseChar()
}

private fun decodeSwipeWord(path: List<Offset>, keys: List<SwipeKey>, language: KeyboardLanguage): String {
    if (path.size < 2) return ""
    val centers = keys.mapNotNull { key -> key.character?.let { it to key.rect.center } }.toMap()
    val firstKey = keys.filter { it.character != null }.minByOrNull { hypot(path.first().x - it.rect.center.x, path.first().y - it.rect.center.y) }?.character
    val lastKey = keys.filter { it.character != null }.minByOrNull { hypot(path.last().x - it.rect.center.x, path.last().y - it.rect.center.y) }?.character
    val width = keys.maxOfOrNull { it.rect.right }?.coerceAtLeast(1f) ?: return ""
    val height = keys.maxOfOrNull { it.rect.bottom }?.coerceAtLeast(1f) ?: return ""
    val samples = 24
    fun sample(points: List<Offset>, index: Int): Offset {
        if (points.size == 1) return points.first()
        val position = index.toFloat() / (samples - 1) * (points.size - 1)
        val low = position.toInt().coerceIn(0, points.lastIndex)
        val high = (low + 1).coerceAtMost(points.lastIndex)
        val fraction = position - low
        return Offset(points[low].x + (points[high].x - points[low].x) * fraction, points[low].y + (points[high].y - points[low].y) * fraction)
    }
    val dictionary = if (language == KeyboardLanguage.PORTUGUESE_BR) portugueseGlideWords else englishGlideWords
    return dictionary.asSequence()
        .filter { it.length >= 2 && baseLetter(it.first()) == firstKey && baseLetter(it.last()) == lastKey }
        .minByOrNull { word ->
            val wordPath = word.mapNotNull { centers[baseLetter(it)] }
            var score = 0f
            for (index in 0 until samples) {
                val gesture = sample(path, index)
                val expected = sample(wordPath, index)
                score += hypot((gesture.x - expected.x) / width, (gesture.y - expected.y) / height)
            }
            score / samples + kotlin.math.abs(path.size / 3f - word.length) * .012f
        } ?: ""
}

private val LocalKeyboardTheme = staticCompositionLocalOf { KeyboardTheme.GLASS }
private val LocalKeyboardAmoled = staticCompositionLocalOf { false }
private val LocalKeyboardFont = staticCompositionLocalOf { KeyboardFont.SYSTEM }
private val LocalKeyboardFontWeight = staticCompositionLocalOf { KeyboardWeightSetting.REGULAR }
private val LocalKeyboardTrail = staticCompositionLocalOf { KeyboardTrail.AURORA }
private val LocalKeyboardOpaque = staticCompositionLocalOf { false }
private val LocalKeyboardScale = staticCompositionLocalOf { 1f }

private data class KeyboardKey(val label: String, val usage: Int, val requiredModifier: Int = 0, val weight: Float = 1f)

private fun letterRow(text: String) = text.map { char ->
    KeyboardKey(char.lowercase(), 0x04 + (char.lowercaseChar() - 'a'))
}

private val qwertyRows = listOf(letterRow("QWERTYUIOP"), letterRow("ASDFGHJKL"), letterRow("ZXCVBNM"))
private val numberUsages = listOf(0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27)
private val symbolRows = listOf(
    "1234567890".mapIndexed { i, c -> KeyboardKey(c.toString(), numberUsages[i]) },
    listOf("!", "@", "#", "\$", "%", "^", "&", "*", "(", ")").mapIndexed { i, c -> KeyboardKey(c, numberUsages[i], MOD_SHIFT) },
    listOf(
        KeyboardKey("-", 0x2D), KeyboardKey("_", 0x2D, MOD_SHIFT), KeyboardKey("=", 0x2E), KeyboardKey("+", 0x2E, MOD_SHIFT),
        KeyboardKey("[", 0x2F), KeyboardKey("]", 0x30), KeyboardKey(";", 0x33), KeyboardKey(":", 0x33, MOD_SHIFT),
        KeyboardKey("/", 0x38), KeyboardKey("?", 0x38, MOD_SHIFT)
    )
)

private val shortcutKeys = buildList {
    addAll(('A'..'Z').map { KeyboardKey(it.toString(), 0x04 + (it - 'A')) })
    addAll("1234567890".mapIndexed { i, c -> KeyboardKey(c.toString(), numberUsages[i]) })
    addAll(listOf(KeyboardKey("Space", 0x2C), KeyboardKey("Tab", 0x2B), KeyboardKey("Return", 0x28),
        KeyboardKey("Esc", 0x29), KeyboardKey("Delete", 0x2A), KeyboardKey("←", 0x50), KeyboardKey("→", 0x4F),
        KeyboardKey("↑", 0x52), KeyboardKey("↓", 0x51)))
    addAll((1..12).map { KeyboardKey("F$it", 0x39 + it) })
}

@Composable
fun BluetoothKeyboardOverlay(
    connected: Boolean,
    amoledMode: Boolean,
    shortcuts: List<KeyboardShortcut>,
    theme: KeyboardTheme,
    onThemeChange: (KeyboardTheme) -> Unit,
    language: KeyboardLanguage,
    onLanguageChange: (KeyboardLanguage) -> Unit,
    trail: KeyboardTrail,
    onTrailChange: (KeyboardTrail) -> Unit,
    font: KeyboardFont,
    onFontChange: (KeyboardFont) -> Unit,
    fontWeight: KeyboardWeightSetting,
    onFontWeightChange: (KeyboardWeightSetting) -> Unit,
    opaque: Boolean,
    onOpaqueChange: (Boolean) -> Unit,
    keyboardScale: Float,
    onKeyboardScaleChange: (Float) -> Unit,
    editorMode: Boolean,
    onEditorCancel: () -> Unit,
    onEditorDone: () -> Unit,
    onShortcutsChange: (List<KeyboardShortcut>) -> Unit,
    onKeyStroke: (Byte, Byte) -> Unit,
    onConsumerControl: (Int) -> Unit,
    onText: (String) -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panel by remember { mutableStateOf(KeyboardPanel.TYPE) }
    var shifted by remember { mutableStateOf(false) }
    var ctrl by remember { mutableStateOf(false) }
    var option by remember { mutableStateOf(false) }
    var command by remember { mutableStateOf(false) }
    var showLanguageSwitcher by remember { mutableStateOf(false) }
    var glidePreview by remember { mutableStateOf("") }
    var customizer by remember { mutableStateOf(KeyboardCustomizer.THEME) }
    // The editor used to sit above the keyboard on the strength of a fixed 118dp, which stopped
    // being true the moment its chrome grew. Measuring it means the keyboard is pushed down by
    // however tall the editor actually is, whatever is put in it later.
    var studioHeightPx by remember { mutableStateOf(0) }

    fun modifiers(required: Int = 0) = required or (if (shifted) MOD_SHIFT else 0) or
        (if (ctrl) MOD_CTRL else 0) or (if (option) MOD_OPTION else 0) or (if (command) MOD_COMMAND else 0)
    fun send(key: KeyboardKey) {
        onHaptic(); onKeyStroke(modifiers(key.requiredModifier).toByte(), key.usage.toByte())
        if (shifted) shifted = false
    }

    CompositionLocalProvider(
        LocalKeyboardTheme provides theme,
        LocalKeyboardAmoled provides amoledMode,
        LocalKeyboardFont provides font,
        LocalKeyboardFontWeight provides fontWeight,
        LocalKeyboardTrail provides trail,
        LocalKeyboardOpaque provides opaque,
        LocalKeyboardScale provides keyboardScale
    ) {
    Box(modifier.fillMaxSize()
        .background(if (amoledMode) Color.Black else Color.Transparent)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
        if (glidePreview.isNotEmpty()) {
            Text(
                text = glidePreview,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp),
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = composeWeight(fontWeight),
                fontFamily = composeFont(font),
                style = TextStyle(
                    shadow = Shadow(
                        color = if (amoledMode) Color.White.copy(.75f) else Color(0xFF8FEFFF),
                        offset = Offset.Zero,
                        blurRadius = 22f
                    )
                )
            )
        }
        // The clock pill remains visible above this sheet and is the sole open/close affordance.
        Column(
            Modifier.align(Alignment.TopCenter)
                .padding(
                    top = when {
                        // Back to its normal place the moment editing ends.
                        !editorMode -> 64.dp
                        studioHeightPx > 0 -> (STUDIO_TOP +
                            with(LocalDensity.current) { studioHeightPx.toDp() } + 8.dp)
                            .coerceIn(STUDIO_TOP + 40.dp, KEYBOARD_TOP_LIMIT)
                        else -> 150.dp
                    }
                )
                .fillMaxWidth()
                .padding(horizontal = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp * keyboardScale)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyboardPanel.entries.forEach { item ->
                    val label = if (item == KeyboardPanel.TYPE && connected) "Type •" else item.label
                    GlassKey(label, 1f, panel == item, compact = true) { onHaptic(); glidePreview = ""; panel = item }
                }
            }
            when (panel) {
                KeyboardPanel.TYPE -> SwipeTypingPanel(
                    shifted = shifted, ctrl = ctrl, option = option, command = command,
                    language = language, onKey = ::send, onText = onText, onShift = { onHaptic(); shifted = !shifted },
                    onCtrl = { onHaptic(); ctrl = !ctrl }, onOption = { onHaptic(); option = !option },
                    onCommand = { onHaptic(); command = !command }, onHaptic = onHaptic,
                    onSpaceLongPress = { onHaptic(); showLanguageSwitcher = true },
                    onPreview = { glidePreview = it }
                )
                KeyboardPanel.SYMBOLS -> TypingPanel(
                    rows = symbolRows,
                    letters = false, shifted = shifted, ctrl = ctrl, option = option, command = command,
                    onKey = ::send, onShift = { onHaptic(); shifted = !shifted }, onCtrl = { onHaptic(); ctrl = !ctrl },
                    onOption = { onHaptic(); option = !option }, onCommand = { onHaptic(); command = !command },
                    onSpaceLongPress = { onHaptic(); showLanguageSwitcher = true }
                )
                KeyboardPanel.MAC -> MacPanel(
                    shifted, ctrl, option, command, ::send,
                    { onHaptic(); shifted = !shifted }, { onHaptic(); ctrl = !ctrl },
                    { onHaptic(); option = !option }, { onHaptic(); command = !command }
                )
                KeyboardPanel.SHORTCUTS -> ShortcutPanel(shortcuts,
                    onSend = { onHaptic(); onKeyStroke(it.modifiers.toByte(), it.usage.toByte()) },
                    onChange = onShortcutsChange, onHaptic = onHaptic)
                KeyboardPanel.MEDIA -> MediaPanel(
                    onSend = { usage -> onHaptic(); onConsumerControl(usage) }
                )
            }
        }
        if (editorMode && glidePreview.isEmpty()) {
            KeyboardCustomizerTop(
                section = customizer,
                theme = theme,
                trail = trail,
                font = font,
                fontWeight = fontWeight,
                opaque = opaque,
                scale = keyboardScale,
                onSection = { customizer = it },
                onTheme = { onHaptic(); onThemeChange(it) },
                onTrail = { onHaptic(); onTrailChange(it) },
                onFont = { onHaptic(); onFontChange(it) },
                onFontWeight = { onHaptic(); onFontWeightChange(it) },
                onOpaque = { onHaptic(); onOpaqueChange(it) },
                onScale = onKeyboardScaleChange,
                onCancel = onEditorCancel,
                onDone = onEditorDone,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = STUDIO_TOP)
                    .onSizeChanged { studioHeightPx = it.height }
            )
        }
        if (showLanguageSwitcher) {
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = 268.dp).fillMaxWidth(.62f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                KeyboardLanguage.entries.forEach { option ->
                    GlassKey(option.label, 1f, selected = language == option) {
                        onHaptic()
                        onLanguageChange(option)
                        showLanguageSwitcher = false
                    }
                }
            }
        }
    }
    }
}

private object ConsumerUsage {
    const val BRIGHTNESS_UP = 0x006F
    const val BRIGHTNESS_DOWN = 0x0070
    const val PREVIOUS_TRACK = 0x00B6
    const val PLAY_PAUSE = 0x00CD
    const val NEXT_TRACK = 0x00B5
    const val MUTE = 0x00E2
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
}

@Composable
private fun MediaPanel(onSend: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("Brightness down", 1f, repeatable = true, icon = Icons.Default.BrightnessLow) {
            onSend(ConsumerUsage.BRIGHTNESS_DOWN)
        }
        GlassKey("Display", 1.25f, selected = true) {}
        GlassKey("Brightness up", 1f, repeatable = true, icon = Icons.Default.BrightnessHigh) {
            onSend(ConsumerUsage.BRIGHTNESS_UP)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("Previous", 1f, icon = Icons.Default.SkipPrevious) { onSend(ConsumerUsage.PREVIOUS_TRACK) }
        GlassKey("Play or pause", 1.25f, selected = true, icon = Icons.Default.PlayArrow) {
            onSend(ConsumerUsage.PLAY_PAUSE)
        }
        GlassKey("Next", 1f, icon = Icons.Default.SkipNext) { onSend(ConsumerUsage.NEXT_TRACK) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("Volume down", 1f, repeatable = true, icon = Icons.Default.VolumeDown) {
            onSend(ConsumerUsage.VOLUME_DOWN)
        }
        GlassKey("Mute", 1.25f, icon = Icons.Default.VolumeOff) { onSend(ConsumerUsage.MUTE) }
        GlassKey("Volume up", 1f, repeatable = true, icon = Icons.Default.VolumeUp) {
            onSend(ConsumerUsage.VOLUME_UP)
        }
    }
    Spacer(Modifier.height(37.dp))
}

@Composable
private fun TypingPanel(
    rows: List<List<KeyboardKey>>, letters: Boolean, shifted: Boolean, ctrl: Boolean, option: Boolean, command: Boolean,
    onKey: (KeyboardKey) -> Unit, onShift: () -> Unit, onCtrl: () -> Unit, onOption: () -> Unit, onCommand: () -> Unit,
    onSpaceLongPress: () -> Unit
) {
    KeyRow(rows[0], shifted && letters, onKey)
    KeyRow(rows[1], shifted && letters, onKey, 10.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("⇧", 1.3f, shifted, onClick = onShift)
        rows[2].forEach { key -> GlassKey(if (shifted && letters) key.label.uppercase() else key.label, key.weight, repeatable = true) { onKey(key) } }
        GlassKey("⌫", 1.3f, repeatable = true) { onKey(KeyboardKey("⌫", 0x2A)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("⌃", .82f, ctrl, onClick = onCtrl); GlassKey("⌥", .82f, option, onClick = onOption)
        GlassKey("⌘", .9f, command, onClick = onCommand); GlassKey("", 4.2f, onLongPress = onSpaceLongPress) { onKey(KeyboardKey("Space", 0x2C)) }
        GlassKey("↵", 1.15f, repeatable = true) { onKey(KeyboardKey("Return", 0x28)) }
    }
}

@Composable
private fun MacPanel(
    shifted: Boolean, ctrl: Boolean, option: Boolean, command: Boolean, onKey: (KeyboardKey) -> Unit,
    onShift: () -> Unit, onCtrl: () -> Unit, onOption: () -> Unit, onCommand: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassKey("⌘ Command", 1f, command, onClick = onCommand); GlassKey("⌥ Option", 1f, option, onClick = onOption)
        GlassKey("⌃ Control", 1f, ctrl, onClick = onCtrl); GlassKey("⇧ Shift", 1f, shifted, onClick = onShift)
    }
    KeyRow((1..6).map { KeyboardKey("F$it", 0x39 + it) }, false, onKey)
    KeyRow((7..12).map { KeyboardKey("F$it", 0x39 + it) }, false, onKey)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(KeyboardKey("esc", 0x29), KeyboardKey("tab", 0x2B), KeyboardKey("⌦", 0x4C), KeyboardKey("←", 0x50),
            KeyboardKey("↑", 0x52), KeyboardKey("↓", 0x51), KeyboardKey("→", 0x4F)).forEach { key ->
            GlassKey(key.label, 1f, repeatable = true) { onKey(key) }
        }
    }
}

@Composable
private fun ShortcutPanel(
    shortcuts: List<KeyboardShortcut>, onSend: (KeyboardShortcut) -> Unit,
    onChange: (List<KeyboardShortcut>) -> Unit, onHaptic: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var modifiers by remember { mutableIntStateOf(MOD_COMMAND) }
    var keyIndex by remember { mutableIntStateOf(0) }
    if (creating) {
        val key = shortcutKeys[keyIndex]
        val chord = chordLabel(modifiers, key.label)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("⌘" to MOD_COMMAND, "⌥" to MOD_OPTION, "⌃" to MOD_CTRL, "⇧" to MOD_SHIFT).forEach { (label, bit) ->
                GlassKey(label, 1f, modifiers and bit != 0) { onHaptic(); modifiers = modifiers xor bit }
            }
            GlassKey("Cancel", 1.5f) { onHaptic(); creating = false }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassKey("‹", 1f) { onHaptic(); keyIndex = (keyIndex - 1 + shortcutKeys.size) % shortcutKeys.size }
            GlassKey(key.label, 4f, true) { onHaptic(); keyIndex = (keyIndex + 1) % shortcutKeys.size }
            GlassKey("›", 1f) { onHaptic(); keyIndex = (keyIndex + 1) % shortcutKeys.size }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassKey(chord, 3f, true) {}
            GlassKey("Save", 1.2f) {
                onHaptic(); if (shortcuts.size < 8) onChange(shortcuts + KeyboardShortcut(chord, modifiers, key.usage)); creating = false
            }
        }
        Spacer(Modifier.height(37.dp))
    } else {
        val visible = shortcuts.take(8)
        repeat(2) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val items = visible.drop(row * 4).take(4)
                items.forEach { shortcut ->
                    GlassKey(if (deleting) "− ${shortcut.label}" else shortcut.label, 1f, deleting) {
                        onHaptic(); if (deleting) onChange(shortcuts - shortcut) else onSend(shortcut)
                    }
                }
                repeat(4 - items.size) { Spacer(Modifier.weight(1f).height(37.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassKey("+ New Shortcut", 2f) { onHaptic(); deleting = false; creating = true }
            GlassKey(if (deleting) "Done" else "Edit", 1f, deleting) { onHaptic(); deleting = !deleting }
        }
        Spacer(Modifier.height(37.dp))
    }
}

private fun chordLabel(modifiers: Int, key: String) = buildString {
    if (modifiers and MOD_CTRL != 0) append("⌃"); if (modifiers and MOD_OPTION != 0) append("⌥")
    if (modifiers and MOD_SHIFT != 0) append("⇧"); if (modifiers and MOD_COMMAND != 0) append("⌘"); append(key)
}

@Composable
private fun KeyRow(keys: List<KeyboardKey>, shifted: Boolean, onKey: (KeyboardKey) -> Unit, inset: androidx.compose.ui.unit.Dp = 0.dp) {
    Row(Modifier.fillMaxWidth().padding(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        keys.forEach { key -> GlassKey(if (shifted && key.label.length == 1 && key.label[0].isLetter()) key.label.uppercase() else key.label, key.weight, repeatable = true) { onKey(key) } }
    }
}

@Composable
private fun RowScope.GlassKey(
    label: String,
    weight: Float,
    selected: Boolean = false,
    compact: Boolean = false,
    repeatable: Boolean = false,
    /**
     * Drawn instead of the label when present. Transport and brightness controls were spelled with
     * whatever glyphs happened to resemble the symbol — an arrow and a bar for play/pause, a sun
     * for brightness — which renders in whatever the system font decides, at whatever weight it
     * decides. A real vector icon is the same shape on every device and at every key size.
     */
    icon: ImageVector? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val repeatScope = rememberCoroutineScope()
    val pressed by interaction.collectIsPressedAsState()
    val theme = LocalKeyboardTheme.current
    val shape = RoundedCornerShape(
        if (theme == KeyboardTheme.MONT) 0.dp else if (compact) 8.dp else 9.dp
    )
    val amoled = LocalKeyboardAmoled.current
    val font = LocalKeyboardFont.current
    val fontWeight = LocalKeyboardFontWeight.current
    val opaque = LocalKeyboardOpaque.current
    val scale = LocalKeyboardScale.current
    val active = pressed || selected
    val colors = if (amoled) {
        if (active) listOf(Color.White.copy(.18f), Color.White.copy(.07f)) else listOf(Color.Black, Color.Black)
    } else when (theme) {
        KeyboardTheme.GLASS -> if (active) listOf(Color(0x7AFFFFFF), Color(0x34FFFFFF)) else listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF))
        KeyboardTheme.FROST -> if (active) listOf(Color(0xE8FFFFFF), Color(0xA8FFFFFF)) else listOf(Color(0xB8FFFFFF), Color(0x70FFFFFF))
        KeyboardTheme.MONT -> if (active) {
            listOf(Color(0xFF2E2E33), Color(0xFF232327))
        } else {
            listOf(Color(0xFF141416), Color(0xFF101012))
        }
        KeyboardTheme.TITANIUM -> if (active) {
            listOf(Color(0xD8E5E9ED), Color(0xA87D8791), Color(0xCCBFC6CC))
        } else {
            listOf(Color(0x8AAAB2BA), Color(0x80626B74), Color(0x786F7881))
        }
        KeyboardTheme.NOIR -> if (active) listOf(Color(0xCC2A2B2D), Color(0xF008090A)) else listOf(Color(0xD8161719), Color(0xEE050607))
        KeyboardTheme.PORCELAIN -> if (active) listOf(Color(0xFFFFFFFF), Color(0xFFE8DFC9)) else listOf(Color(0xF8FDF9EE), Color(0xE8DED3BC))
        KeyboardTheme.TERMINAL -> if (active) listOf(Color(0xCC143A20), Color(0xF0040A06)) else listOf(Color(0xB80B1E10), Color(0xEE020604))
        KeyboardTheme.SUNSET -> if (active) listOf(Color(0xE8FFC46B), Color(0xD8F16D92), Color(0xC67A55D8)) else listOf(Color(0xB8EFA75D), Color(0xA8D95E84), Color(0x986347B8))
        KeyboardTheme.CYBER -> if (active) listOf(Color(0xB8144854), Color(0xE8031116)) else listOf(Color(0x9810313A), Color(0xE801080B))
        KeyboardTheme.PAPER -> if (active) listOf(Color(0xFFFFF4DD), Color(0xFFE7D3AE)) else listOf(Color(0xF8F1E4CA), Color(0xE8DFC8A4))
    }
    val fillColors = if (opaque && !amoled) colors.map { it.compositeOver(Color(0xFF090A0C)) } else colors
    val border = if (amoled) Color.White.copy(if (active) 1f else .72f) else when (theme) {
        KeyboardTheme.MONT -> if (active) Color.White.copy(.55f) else Color.Transparent
        KeyboardTheme.FROST -> Color.White.copy(if (active) .9f else .56f)
        KeyboardTheme.TITANIUM -> Color(0xFFF2F5F7).copy(if (active) .82f else .48f)
        KeyboardTheme.NOIR -> Color(0xFFFFF8EE).copy(if (active) .7f else .30f)
        KeyboardTheme.PORCELAIN -> Color(0xFF345B99).copy(if (active) .82f else .48f)
        KeyboardTheme.TERMINAL -> Color(0xFF53FF86).copy(if (active) .92f else .56f)
        KeyboardTheme.SUNSET -> Color(0xFFFFF1DD).copy(if (active) .88f else .48f)
        KeyboardTheme.CYBER -> Color(0xFF59F3FF).copy(if (active) .92f else .58f)
        KeyboardTheme.PAPER -> Color(0xFF6B5134).copy(if (active) .72f else .42f)
        KeyboardTheme.GLASS -> Color.White.copy(if (active) .48f else .18f)
    }
    val foreground = if (!amoled && theme in setOf(KeyboardTheme.FROST, KeyboardTheme.PORCELAIN, KeyboardTheme.PAPER)) Color(0xFF111214) else Color.White
    val actionModifier = if (onLongPress != null) {
        Modifier.pointerInput(onClick, onLongPress) {
            detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
        }
    } else if (repeatable) {
        Modifier.pointerInput(onClick) {
            detectTapGestures(onPress = {
                onClick()
                val repeatJob = repeatScope.launch {
                    delay(360)
                    while (true) {
                        onClick()
                        delay(55)
                    }
                }
                tryAwaitRelease()
                repeatJob.cancel()
            })
        }
    } else {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    }
    Box(Modifier.weight(weight).height((if (compact) 28.dp else 37.dp) * scale).clip(shape)
        .background(Brush.verticalGradient(fillColors)).border(1.dp, border, shape).then(actionModifier),
        contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = label,
                tint = foreground,
                modifier = Modifier.size((if (compact) 14.dp else 17.dp) * scale)
            )
        } else if (label.isNotEmpty() && font == KeyboardFont.PIXEL) {
            Canvas(Modifier.fillMaxSize()) {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = false
                    color = foreground.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = ((if (compact) 10.sp else 12.sp) * scale).toPx()
                    typeface = androidFont(font, fontWeight)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    size.width / 2f,
                    size.height / 2f - (paint.ascent() + paint.descent()) / 2f,
                    paint
                )
            }
        } else if (label.isNotEmpty()) {
            Text(label, color = foreground, fontSize = (if (compact) 10.sp else 12.sp) * scale, fontWeight = composeWeight(fontWeight), fontFamily = composeFont(font))
        }
    }
}
