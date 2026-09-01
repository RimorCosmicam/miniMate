package com.minimate.touchpad.model

/**
 * The shader catalog.
 *
 * Replaces the previous fixed 5 x 5 x 3 grid, where every scene was forced to expose exactly the
 * same three knobs whether or not they meant anything for it. Here each scene declares its own
 * controls, so a rain scene offers fall speed and trail length while a fractal offers seed and
 * iteration depth, and the panel is built from whatever the selected scene actually has.
 *
 * A scene is one AGSL body plus its controls and palettes. Bodies define
 *
 *     float3 scene(float2 p, float2 uv, float t)
 *
 * where p is aspect-corrected and centred on zero, uv spans 0..1, and t is scaled time. The shared
 * header supplies noise, palette and touch helpers; the shared footer supplies main(). Scenes
 * therefore stay short enough to read as a single idea.
 */

/** One control on a scene's panel. Ranges are chosen so the extremes are both usable. */
data class ShaderParam(
    val key: String,
    val label: String,
    val min: Float,
    val max: Float,
    val default: Float
)

enum class ShaderFamily(val label: String) {
    MATRIX("Matrix"),
    MINIMAL("Minimal"),
    GEOMETRY("Geometry"),
    CANDY("Candy"),
    BLOOM("Bloom"),
    STARS("Stars")
}

data class ShaderScene(
    val id: String,
    val label: String,
    val family: ShaderFamily,
    val blurb: String,
    val params: List<ShaderParam>,
    val palettes: List<SceneColorway>,
    val body: String,
    /**
     * True when the scene splits wavelengths itself — refracting per channel, or Doppler-shifting
     * emission — rather than being sampled three times through the shared lens. Raymarched scenes
     * set this: three full marches per pixel is a cost the cover display cannot pay, and a prism
     * that disperses at the surface it refracts through looks right in a way a screen-space
     * channel offset never does.
     */
    val dispersive: Boolean = false
) {
    val defaults: List<Float> get() = params.map { it.default }
}

private fun p(label: String, vararg stops: Long) = SceneColorway(label, stops.toList())

// Lo-fi palettes: near-black grounds, one muted mid, one saturated accent, one warm cream
// highlight. Deliberately restrained rather than neon — the appeal of the original Paradise
// Matrix was that it never shouts, and saturated palettes lose that immediately.
private val PARADISE = p("Paradise", 0xFF020609, 0xFF174557, 0xFF6ED6DD, 0xFFFFF3C7)
private val OPERATOR = p("Operator", 0xFF000300, 0xFF002B0B, 0xFF00C73B, 0xFFE8FFE9)
private val SAKURA = p("Sakura", 0xFF080306, 0xFF4A1D3B, 0xFFE676A6, 0xFFFFF0D6)
private val LAVENDER = p("Lavender", 0xFF040308, 0xFF302651, 0xFFA58AD8, 0xFFFFFFFF)
private val AMBER = p("Amber Terminal", 0xFF050300, 0xFF352000, 0xFFFFA51E, 0xFFFFF0C4)
private val ICE = p("Ice", 0xFF01040A, 0xFF10304F, 0xFF63B8E8, 0xFFF0FAFF)
private val EMBER = p("Ember", 0xFF060201, 0xFF3A1508, 0xFFE0642A, 0xFFFFE2B8)
private val MOSS = p("Moss", 0xFF020402, 0xFF1B3520, 0xFF71B27A, 0xFFEDF7E4)
private val VIOLET = p("Deep Violet", 0xFF04020A, 0xFF251249, 0xFF7B4FD1, 0xFFEDE4FF)
private val MONO = p("Monochrome", 0xFF030303, 0xFF262626, 0xFFBFBFBF, 0xFFFFFFFF)
private val TEAL = p("Teal", 0xFF01060A, 0xFF0C3B43, 0xFF3FB8AE, 0xFFEAFBF6)
private val ROSE = p("Rose Gold", 0xFF070405, 0xFF43222B, 0xFFD98C7A, 0xFFFFEDDF)

private val LOFI = listOf(PARADISE, OPERATOR, SAKURA, LAVENDER, AMBER, ICE, MONO)
private val WARM = listOf(EMBER, AMBER, ROSE, SAKURA, PARADISE, MONO)
private val COOL = listOf(ICE, TEAL, LAVENDER, VIOLET, PARADISE, MONO)
private val ORGANIC = listOf(MOSS, TEAL, EMBER, ICE, SAKURA, MONO)
private val VIOLETS = listOf(VIOLET, LAVENDER, ICE, SAKURA, PARADISE, MONO)
private val MONOS = listOf(MONO, OPERATOR, AMBER, ICE, SAKURA, PARADISE)

// Bright palettes, built the other way up: the ground is the lightest stop and the marks are the
// saturated ones. Running the lo-fi palettes light simply washes them out — a bright scene needs
// its own set, not a brightened version of a dark one.
private val COTTON = p("Cotton", 0xFFFFF4F8, 0xFFFFC9E0, 0xFFFF6FA6, 0xFF6B2E4E)
private val SORBET = p("Sorbet", 0xFFFFF7EC, 0xFFFFD8A8, 0xFFFF8A5C, 0xFF7A3B2E)
private val MINTY = p("Mint", 0xFFEFFFF8, 0xFFB6F0D8, 0xFF3FD1A0, 0xFF1E5C48)
private val SKYLIGHT = p("Sky", 0xFFF0F8FF, 0xFFBFE0FF, 0xFF5FA8FF, 0xFF264B7A)
private val LEMON = p("Lemon", 0xFFFFFDF0, 0xFFFFEFA8, 0xFFFFD23F, 0xFF7A5E14)
private val LILAC = p("Lilac", 0xFFF8F4FF, 0xFFDCCBFF, 0xFF9B7BFF, 0xFF44306E)
private val PAPER_FLAT = p("Paper", 0xFFF4F1EA, 0xFFD9D2C4, 0xFF17171A, 0xFFE8433F)
// The onboarding's mustard, as a flat pair. Minimal is where it belongs: it is a poster colour.
private val MUSTARD_FLAT = p("Mustard", 0xFF141210, 0xFF4A3A10, 0xFFD8A628, 0xFFFFF1CE)
private val SIGNAL = p("Signal", 0xFFFAFAF7, 0xFFE4E4DE, 0xFF17171A, 0xFF2F6BFF)

private val BRIGHT = listOf(COTTON, SORBET, LEMON, MINTY, SKYLIGHT, LILAC)
private val FLAT = listOf(MUSTARD_FLAT, PAPER_FLAT, SIGNAL, COTTON, MINTY, MONO, OPERATOR)

// Night palettes. The ground stop is never pure black — a real dark sky has airglow in it, and a
// starfield on absolute black looks like a printing error rather than a sky.
private val MIDNIGHT = p("Midnight", 0xFF01030A, 0xFF0B1B3A, 0xFF8FB6E8, 0xFFFFFFFF)
private val SEPIA_SKY = p("Old Plate", 0xFF060503, 0xFF2C2418, 0xFFB39A6B, 0xFFFFF6E2)
private val HYDROGEN = p("Hydrogen", 0xFF06010A, 0xFF3A0E33, 0xFFD9527E, 0xFFFFE8F2)
private val NIGHT = listOf(MIDNIGHT, SEPIA_SKY, HYDROGEN, VIOLET, ICE, TEAL, MONO)

private fun param(key: String, label: String, min: Float, max: Float, default: Float) =
    ShaderParam(key, label, min, max, default)

val shaderScenes: List<ShaderScene> = listOf(

    // ---------------------------------------------------------------- Matrix

    ShaderScene(
        "code_rain", "Code Rain", ShaderFamily.MATRIX,
        "Katakana falling in columns, the original.",
        listOf(
            param("density", "Density", 12f, 60f, 28f),
            param("speed", "Fall speed", .2f, 2.5f, 1f),
            param("trail", "Trail length", .2f, 2.5f, 1f),
            param("flicker", "Glyph churn", 0f, 2f, .8f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float cols = uP0;
            float rows = cols * 1.7;
            float2 g = float2(uv.x * cols, uv.y * rows);
            float col = floor(g.x);
            float row = floor(g.y);
            float2 cell = fract(g);

            float seed = hash(float2(col, 7.0));
            float trail = max(uP2 * 14.0, 1.0);

            // A single drop per column, falling. Its row index grows with time, entering above
            // the top edge and leaving below the bottom, so the column is dark except where the
            // drop currently is. Adding time to the sampled row instead — which is what this
            // first did — scrolls the whole grid upward and lights every row at once.
            float head = fract(seed + t * uP1 * 0.14 * (0.55 + seed * 0.9)) * (rows + trail * 2.0) - trail;
            float behind = head - row;
            float lit = behind >= 0.0 ? exp(-behind / trail) : 0.0;
            float isHead = smoothstep(1.4, 0.0, abs(behind));

            // Characters mutate where they sit. Keying the glyph to a scrolling coordinate makes
            // them ride along with the drop, which reads as sliding paper rather than rain.
            float id = floor(hash(float2(col, row + floor(t * uP3 * 9.0))) * 64.0);
            float mask = glyph(id, cell);

            // Black ground, characters lit additively over it — nothing fills the empty cells.
            float3 c = uC0;
            c += uC2 * mask * lit;
            c += uC3 * mask * isHead;
            return c;
        }
        """
    ),

    ShaderScene(
        "paradise", "Paradise Matrix", ShaderFamily.MATRIX,
        "Softer rain with bloom and drifting warmth.",
        listOf(
            param("density", "Density", 10f, 48f, 22f),
            param("speed", "Fall speed", .2f, 2f, .7f),
            param("bloom", "Bloom", 0f, 1.5f, .8f),
            param("warmth", "Warm drift", 0f, 1f, .5f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float cols = uP0;
            float rows = cols * 1.6;
            float2 g = float2(uv.x * cols, uv.y * rows);
            float col = floor(g.x);
            float row = floor(g.y);
            float2 cell = fract(g);

            float seed = hash(float2(col, 11.0));
            float trail = 16.0;
            float head = fract(seed + t * uP1 * 0.11 * (0.5 + seed)) * (rows + trail * 2.0) - trail;
            float behind = head - row;
            float lit = behind >= 0.0 ? exp(-behind / trail) : 0.0;
            float isHead = smoothstep(1.6, 0.0, abs(behind));

            // A second, dimmer drop offset down the column so the field never looks sparse.
            float head2 = fract(seed * 1.7 + 0.5 + t * uP1 * 0.08 * (0.5 + seed)) * (rows + trail * 2.0) - trail;
            float behind2 = head2 - row;
            float lit2 = behind2 >= 0.0 ? exp(-behind2 / (trail * 0.7)) * 0.45 : 0.0;

            float id = floor(hash(float2(col, row + floor(t * 6.0))) * 64.0);
            float mask = glyph(id, cell);

            // The backdrop stays black. Washing it with a haze was what made this read as lit
            // paper rather than glyphs suspended in the dark, so every lift below is additive
            // around the characters themselves and never a fill behind them.
            float3 c = uC0;
            float glow = clamp(lit + lit2, 0.0, 1.0);
            c += uC2 * mask * glow;
            c += uC3 * mask * isHead;
            // Bloom has to follow the character's shape. Applying it to the cell instead paints
            // a lit rectangle behind every glyph, which is the block that showed up around the
            // text. Sampling the glyph at shrunk cell coordinates magnifies it, giving a dilated
            // mask that bleeds light just outside the strokes and nowhere else.
            float bleed = glyph(id, (cell - 0.5) * 0.68 + 0.5);
            c += uC2 * bleed * glow * glow * uP2 * 0.16;
            c += uC3 * mask * isHead * uP3 * 0.35;
            return c;
        }
        """
    ),

    ShaderScene(
        "mirror_canyon", "Mirror Canyon", ShaderFamily.MATRIX,
        "Rain reflected into a vertical canyon.",
        listOf(
            param("density", "Density", 10f, 50f, 24f),
            param("speed", "Fall speed", .2f, 2f, .9f),
            param("split", "Canyon width", .05f, .6f, .25f),
            param("depth", "Depth fade", 0f, 1f, .6f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float m = abs(p.x);
            float fold = smoothstep(uP2, uP2 + 0.02, m);
            float cols = uP0;
            float rows = cols * 1.6;
            float2 g = float2(m * cols, uv.y * rows);
            float col = floor(g.x);
            float row = floor(g.y);
            float2 cell = fract(g);

            float seed = hash(float2(col, 5.0));
            float trail = 13.0;
            float head = fract(seed + t * uP1 * 0.13 * (0.5 + seed)) * (rows + trail * 2.0) - trail;
            float behind = head - row;
            float lit = behind >= 0.0 ? exp(-behind / trail) : 0.0;
            float isHead = smoothstep(1.4, 0.0, abs(behind));

            float id = floor(hash(float2(col, row + floor(t * 7.0))) * 64.0);
            float mask = glyph(id, cell);
            float dim = mix(1.0, 1.0 - m * 1.4, uP3);

            float3 c = uC0;
            c += uC2 * mask * lit * fold * dim;
            c += uC3 * mask * isHead * fold;
            // Only the canyon seam itself catches light; the rest of the ground stays black.
            c += uC1 * (1.0 - fold) * 0.14;
            return c;
        }
        """
    ),

    ShaderScene(
        "glyph_quilt", "Glyph Quilt", ShaderFamily.MATRIX,
        "A woven field of glyphs that breathe rather than fall.",
        listOf(
            param("cell", "Cell size", 8f, 40f, 18f),
            param("drift", "Drift", 0f, 1.5f, .4f),
            param("flicker", "Flicker", 0f, 1f, .45f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 g = uv * uP0;
            float2 id2 = floor(g);
            float2 cell = fract(g);
            float wob = sin(t * uP1 + id2.x * 0.7 + id2.y * 1.3);
            float id = floor(hash(id2 + floor(t * uP2 * 3.0)) * 64.0);
            float mask = glyph(id, cell);
            float lum = 0.35 + 0.65 * (0.5 + 0.5 * wob);
            float3 c = mix(uC0, uC1, 0.25 + 0.2 * wob);
            c = mix(c, uC2, mask * lum);
            c = mix(c, uC3, mask * smoothstep(0.85, 1.0, lum));
            return c;
        }
        """
    ),

    ShaderScene(
        "terminal", "Terminal Boot", ShaderFamily.MATRIX,
        "Scrolling console lines behind a scanline.",
        listOf(
            param("lines", "Line count", 10f, 50f, 26f),
            param("scroll", "Scroll speed", 0f, 3f, 1f),
            param("scan", "Scanline", 0f, 1f, .5f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float rows = uP0;
            float y = uv.y * rows + t * uP1;
            float row = floor(y);
            float seed = hash(float2(row, 2.0));
            float len = 0.15 + seed * 0.75;
            float on = step(uv.x, len);
            float2 g = float2(uv.x * rows * 1.8, y);
            float mask = glyph(floor(hash(float2(floor(g.x), row)) * 64.0), fract(g));
            float scan = 0.5 + 0.5 * sin(uv.y * 380.0);
            float3 c = uC0;
            c = mix(c, uC2, mask * on * 0.9);
            c = mix(c, uC3, mask * on * step(0.93, fract(uv.x / max(len, 0.01))));
            c *= mix(1.0, scan, uP2 * 0.35);
            return c;
        }
        """
    ),

    ShaderScene(
        "hexpulse", "Hex Pulse", ShaderFamily.GEOMETRY,
        "Hexagonal cells lighting in travelling waves.",
        listOf(
            param("cell", "Cell size", 3f, 20f, 8f),
            param("pulse", "Pulse rate", 0f, 3f, 1f),
            param("edge", "Edge width", .02f, .3f, .09f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = p * uP0;
            float2 h = float2(1.0, 1.732);
            float2 a = mod(q, h) - h * 0.5;
            float2 b = mod(q - h * 0.5, h) - h * 0.5;
            float2 gv = dot(a, a) < dot(b, b) ? a : b;
            float2 id2 = q - gv;
            float d = max(abs(gv.x) * 0.866 + abs(gv.y) * 0.5, abs(gv.y));
            float edge = smoothstep(0.5 - uP2, 0.5, d);
            float wave = sin(length(id2) * 0.8 - t * uP1 * 2.0) * 0.5 + 0.5;
            float lit = pow(wave, 3.0);
            float3 c = mix(uC0, uC1, lit * 0.7);
            c = mix(c, uC2, edge * (0.3 + lit));
            c += uC3 * edge * lit * 0.8;
            return c;
        }
        """
    ),

    ShaderScene(
        "voronoi", "Voronoi Cells", ShaderFamily.GEOMETRY,
        "Drifting cell walls with glowing seams.",
        listOf(
            param("count", "Cell count", 2f, 16f, 6f),
            param("jitter", "Jitter", 0f, 1f, .8f),
            param("edge", "Seam glow", .01f, .3f, .08f)
        ),
        ORGANIC,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = p * uP0;
            float2 ip = floor(q);
            float2 fp = fract(q);
            float d1 = 8.0; float d2 = 8.0;
            for (int y = -1; y <= 1; y++){
                for (int x = -1; x <= 1; x++){
                    float2 nb = float2(float(x), float(y));
                    float2 pt = hash2(ip + nb);
                    pt = 0.5 + uP1 * 0.5 * sin(t * 0.7 + 6.28 * pt);
                    float d = length(nb + pt - fp);
                    if (d < d1){ d2 = d1; d1 = d; } else if (d < d2){ d2 = d; }
                }
            }
            float seam = smoothstep(0.0, uP2, d2 - d1);
            float3 c = mix(uC2, uC0, seam);
            c = mix(c, uC1, smoothstep(0.0, 1.0, d1));
            c += uC3 * (1.0 - seam) * 0.9;
            return c;
        }
        """
    ),

    ShaderScene(
        "kaleido", "Kaleidoscope", ShaderFamily.GEOMETRY,
        "Mirrored wedges folding a noise field.",
        listOf(
            param("segments", "Segments", 3f, 16f, 6f),
            param("zoom", "Zoom", .5f, 4f, 1.5f),
            param("spin", "Spin", -1.5f, 1.5f, .3f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float a = atan(p.y, p.x) + t * uP2;
            float r = length(p) * uP1;
            float seg = 6.2831853 / uP0;
            a = abs(mod(a, seg) - seg * 0.5);
            float2 q = float2(cos(a), sin(a)) * r;
            float v = fbm(q * 2.0 + t * 0.2);
            float rings = 0.5 + 0.5 * sin(r * 10.0 - t * 1.5);
            float3 c = pal(v * 0.7 + rings * 0.3);
            c += uC3 * pow(rings, 8.0) * 0.4;
            return c;
        }
        """
    ),

    ShaderScene(
        "lattice", "Crystal Lattice", ShaderFamily.GEOMETRY,
        "Faceted prisms catching moving light.",
        listOf(
            param("facets", "Facets", 2f, 14f, 6f),
            param("sharp", "Sharpness", .5f, 6f, 2.5f),
            param("spin", "Spin", -1f, 1f, .25f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = rot(p, t * uP2) * uP0;
            float2 id2 = floor(q);
            float2 f = fract(q) - 0.5;
            float tri = abs(f.x) + abs(f.y);
            float facet = pow(1.0 - clamp(tri, 0.0, 1.0), uP1);
            float shade = hash(id2) * 0.6 + 0.4;
            float sweep = 0.5 + 0.5 * sin(id2.x * 0.6 + id2.y * 0.4 - t * 1.6);
            float3 c = mix(uC0, uC1, facet * shade);
            c = mix(c, uC2, facet * sweep * 0.9);
            c += uC3 * pow(facet, 3.0) * sweep;
            return c;
        }
        """
    ),

    ShaderScene(
        "contours", "Topographic", ShaderFamily.GEOMETRY,
        "Elevation lines drifting like a living map.",
        listOf(
            param("lines", "Line count", 4f, 40f, 16f),
            param("scale", "Terrain scale", .5f, 5f, 2f),
            param("drift", "Drift", 0f, 1f, .25f)
        ),
        ORGANIC,
        """
        float3 scene(float2 p, float2 uv, float t){
            float h = fbm(p * uP1 + float2(t * uP2 * 0.3, t * uP2 * 0.2));
            float band = fract(h * uP0);
            float line = smoothstep(0.0, 0.06, band) - smoothstep(0.94, 1.0, band);
            float3 c = mix(uC0, uC1, h);
            c = mix(c, uC2, 1.0 - line);
            c += uC3 * (1.0 - line) * smoothstep(0.6, 1.0, h) * 0.8;
            return c;
        }
        """
    ),

    ShaderScene(
        "parallax_sky", "Parallax Sky", ShaderFamily.STARS,
        "Layered stars with real depth, drifting.",
        listOf(
            param("density", "Density", 3f, 16f, 7f),
            param("drift", "Drift", 0f, 2f, .7f),
            param("spikes", "Diffraction", 0f, 2f, .8f),
            param("twinkle", "Scintillation", 0f, 1f, .55f)
        ),
        NIGHT,
        """
        float3 scene(float2 p, float2 uv, float t){
            // Unresolved light between the stars. A sky is never empty where it is not bright.
            float3 c = uC0 + uC1 * fbm(p * 2.6 + 11.0) * 0.06;

            for (int layer = 0; layer < 4; layer++){
                float fl = float(layer);
                // Near layers sweep further than far ones for the same motion, and that difference
                // is the only thing that makes a flat field of dots read as depth.
                float near = 1.0 - fl * 0.22;
                float2 q = p * (uP0 * (1.0 + fl * 1.7))
                         + float2(t * uP1 * 0.06, t * uP1 * 0.014) * near
                         + fl * 37.0;
                float2 cell = floor(q);
                float2 f = fract(q) - 0.5;

                // The eight neighbours as well, so a star sitting near an edge is not cut by it.
                for (int j = 0; j < 9; j++){
                    float fj = float(j);
                    float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                    float2 id = cell + o;
                    float2 jitter = hash2(id);
                    // Steep power law. Most stars are faint and a handful carry the picture; a
                    // uniform brightness distribution reads as noise, not as a sky.
                    float mag = pow(hash(id + 0.37), 7.0) * (0.30 + near * 0.95);
                    if (mag > 0.0025){
                        float2 d = f - o + (jitter - 0.5) * 0.82;
                        float dist = length(d);
                        float shimmer = 1.0 - uP3 * 0.45
                            + uP3 * 0.45 * sin(t * (2.0 + jitter.x * 7.0) + jitter.y * 30.0);
                        float core = (0.010 + mag * 0.028) * (2.3 - near);
                        float3 tint = starColor(hash(id + 5.1));
                        c += tint * psf(dist, core) * mag * shimmer;
                        // Only the bright ones spike. On everything, it looks like a filter.
                        if (mag > 0.30){
                            float spike = exp(-abs(d.x) * 190.0) + exp(-abs(d.y) * 190.0);
                            c += tint * spike * exp(-dist * 11.0) * mag * uP2 * 0.5;
                        }
                    }
                }
            }
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "star_trails", "Star Trails", ShaderFamily.STARS,
        "A long exposure. The sky turns, the camera does not.",
        listOf(
            param("rings", "Star count", 20f, 120f, 60f),
            param("arc", "Exposure", .15f, 3.2f, 1.3f),
            param("turn", "Rotation", 0f, 2f, .5f),
            param("poleY", "Pole height", -.6f, .6f, .18f)
        ),
        listOf(SEPIA_SKY, MIDNIGHT, EMBER, VIOLET, MONO),
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 pole = float2(0.0, uP3);
            float2 d = p - pole;
            float radius = length(d);
            float angle = atan(d.y, d.x);

            float3 c = uC0 + uC1 * fbm(p * 3.0) * 0.05;

            // Each star keeps its distance from the pole all night, so a trail is an arc at fixed
            // radius. Working in rings means a pixel only has to ask which stars share its radius.
            float scaled = radius * uP0;
            float ring = floor(scaled);
            float across = fract(scaled) - 0.5;

            for (int k = 0; k < 3; k++){
                float2 seed = float2(ring, float(k) * 17.0 + 3.0);
                if (hash(seed + 0.9) > 0.45){
                    float mag = pow(hash(seed + 2.3), 3.0);
                    float start = hash(seed) * 6.2831 + t * uP2 * 0.06;
                    float along = mod(angle - start, 6.2831);
                    // The head is where the star is now and the tail fades back along the arc; an
                    // even streak looks drawn rather than exposed.
                    float body = along < uP1 ? pow(1.0 - along / uP1, 1.6) : 0.0;
                    float head = smoothstep(0.06, 0.0, along) * 2.2;
                    float width = exp(-abs(across) * (26.0 + mag * 40.0));
                    c += starColor(hash(seed + 5.5)) * (body + head) * width * (0.20 + mag * 0.85);
                }
            }
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "warp_jump", "Warp Jump", ShaderFamily.STARS,
        "Stars torn past, accelerating away from the point ahead.",
        listOf(
            param("count", "Stars", 16f, 64f, 40f),
            param("speed", "Speed", .1f, 1.5f, .5f),
            param("stretch", "Streak", .3f, 3f, 1.2f),
            param("bright", "Brightness", .2f, 2f, 1f)
        ),
        NIGHT,
        """
        float seg(float2 p, float2 a, float2 b){
            float2 pa = p - a, ba = b - a;
            return length(pa - ba * clamp(dot(pa, ba) / max(dot(ba, ba), 0.000001), 0.0, 1.0));
        }

        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;

            // Every star gets its own bearing and its own place in the run, and is drawn as the
            // line it covered this frame. The previous version laid them out on a grid in angle
            // against log-radius, which put them in concentric rings — and rings sweeping outward
            // is a tunnel, not a jump.
            for (int i = 0; i < 64; i++){
                float fi = float(i);
                if (fi >= uP0) break;

                float phase = fract(t * uP1 * 0.4 + hash(float2(fi, 1.7)));
                float bearing = hash(float2(fi, 2.3)) * 6.2831;
                float2 dir = float2(cos(bearing), sin(bearing));

                // Distance grows exponentially, so a star creeps while it is far off and tears
                // past at the end. That change of pace is the whole sensation.
                float far = 0.014 * exp(phase * 4.6);
                float near = 0.014 * exp(max(phase - 0.05 * uP2, 0.0) * 4.6);

                float d = seg(p, dir * near, dir * far);
                float mag = pow(hash(float2(fi, 3.1)), 1.8);
                // In at the vanishing point, gone at the edge.
                float fade = smoothstep(0.0, 0.10, phase) * smoothstep(1.0, 0.86, phase);
                float3 tint = starColor(clamp(hash(float2(fi, 4.5)) * 0.5 + uP1 * 0.4, 0.0, 1.0));

                c += tint * exp(-d * 300.0) * mag * fade * uP3;
                c += tint * exp(-d * 70.0) * mag * fade * uP3 * 0.10;
            }
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "constellations", "Constellations", ShaderFamily.STARS,
        "Orion, the Plough, Cassiopeia and the Northern Cross.",
        listOf(
            param("size", "Size", .30f, .62f, .46f),
            param("weight", "Line weight", .0008f, .008f, .0025f),
            param("hold", "Time each", .02f, .3f, .07f),
            param("field", "Background stars", 4f, 16f, 9f)
        ),
        NIGHT,
        """
        float seg(float2 p, float2 a, float2 b){
            float2 pa = p - a, ba = b - a;
            return length(pa - ba * clamp(dot(pa, ba) / max(dot(ba, ba), 0.000001), 0.0, 1.0));
        }

        float node(float2 p, float2 a, float mag){
            return psf(length(p - a), 0.014 + mag * 0.020) * mag;
        }

        // The four figures below are the real asterisms, with the stars roughly where they
        // actually sit relative to one another. A procedural lattice joined by a coin toss — which
        // is what this was — produces no figure at all, because a constellation is not a pattern,
        // it is a specific set of stars people agreed to join up. Each returns the distance to the
        // nearest line in x, and the accumulated starlight in y.

        /** Orion: shoulders, belt, knees, and the head above. */
        float2 orion(float2 q){
            float2 betelgeuse = float2(-0.42, 0.72);
            float2 bellatrix = float2(0.40, 0.80);
            float2 meissa = float2(-0.02, 1.12);
            float2 alnitak = float2(-0.22, 0.00);
            float2 alnilam = float2(0.00, 0.06);
            float2 mintaka = float2(0.22, 0.12);
            float2 saiph = float2(-0.38, -0.76);
            float2 rigel = float2(0.52, -0.82);

            float d = 9.0;
            d = min(d, seg(q, betelgeuse, bellatrix));
            d = min(d, seg(q, betelgeuse, meissa));
            d = min(d, seg(q, bellatrix, meissa));
            d = min(d, seg(q, betelgeuse, alnitak));
            d = min(d, seg(q, bellatrix, mintaka));
            d = min(d, seg(q, alnitak, alnilam));
            d = min(d, seg(q, alnilam, mintaka));
            d = min(d, seg(q, alnitak, saiph));
            d = min(d, seg(q, mintaka, rigel));

            float s = node(q, betelgeuse, 1.0) + node(q, rigel, 1.0) + node(q, bellatrix, 0.8)
                    + node(q, alnitak, 0.8) + node(q, alnilam, 0.85) + node(q, mintaka, 0.8)
                    + node(q, saiph, 0.7) + node(q, meissa, 0.5);
            return float2(d, s);
        }

        /** Ursa Major's plough: the bowl, and the handle bending away from it. */
        float2 plough(float2 q){
            float2 dubhe = float2(0.92, 0.34);
            float2 merak = float2(0.88, -0.10);
            float2 phecda = float2(0.40, -0.24);
            float2 megrez = float2(0.44, 0.16);
            float2 alioth = float2(0.02, 0.30);
            float2 mizar = float2(-0.40, 0.30);
            float2 alkaid = float2(-0.84, 0.06);

            float d = 9.0;
            d = min(d, seg(q, dubhe, merak));
            d = min(d, seg(q, merak, phecda));
            d = min(d, seg(q, phecda, megrez));
            d = min(d, seg(q, megrez, dubhe));
            d = min(d, seg(q, megrez, alioth));
            d = min(d, seg(q, alioth, mizar));
            d = min(d, seg(q, mizar, alkaid));

            float s = node(q, dubhe, 0.95) + node(q, merak, 0.85) + node(q, phecda, 0.8)
                    + node(q, megrez, 0.6) + node(q, alioth, 0.9) + node(q, mizar, 0.85)
                    + node(q, alkaid, 0.9);
            return float2(d, s);
        }

        /** Cassiopeia, the W. */
        float2 cassiopeia(float2 q){
            float2 caph = float2(-0.88, 0.16);
            float2 schedar = float2(-0.44, -0.18);
            float2 gamma = float2(0.02, 0.26);
            float2 ruchbah = float2(0.46, -0.12);
            float2 segin = float2(0.88, 0.30);

            float d = 9.0;
            d = min(d, seg(q, caph, schedar));
            d = min(d, seg(q, schedar, gamma));
            d = min(d, seg(q, gamma, ruchbah));
            d = min(d, seg(q, ruchbah, segin));

            float s = node(q, caph, 0.85) + node(q, schedar, 0.95) + node(q, gamma, 0.9)
                    + node(q, ruchbah, 0.8) + node(q, segin, 0.65);
            return float2(d, s);
        }

        /** Cygnus, the Northern Cross, laid along the swan. */
        float2 cygnus(float2 q){
            float2 deneb = float2(0.00, 1.00);
            float2 sadr = float2(0.00, 0.10);
            float2 albireo = float2(-0.04, -0.96);
            float2 gienah = float2(-0.78, 0.22);
            float2 delta = float2(0.74, 0.30);

            float d = 9.0;
            d = min(d, seg(q, deneb, sadr));
            d = min(d, seg(q, sadr, albireo));
            d = min(d, seg(q, gienah, sadr));
            d = min(d, seg(q, sadr, delta));

            float s = node(q, deneb, 1.0) + node(q, sadr, 0.8) + node(q, albireo, 0.7)
                    + node(q, gienah, 0.8) + node(q, delta, 0.75);
            return float2(d, s);
        }

        /** One figure, resolved into colour. */
        float3 figure(float2 hit, float scale, float fade){
            // The line width is given in screen terms and divided into local space, so a figure
            // does not draw with thicker strokes simply because it is larger.
            float width = uP1 / scale;
            float3 c = mix(uC2, uC3, 0.30) * smoothstep(width, 0.0, hit.x) * fade;
            c += mix(uC3, float3(1.0), 0.5) * hit.y * fade;
            return c;
        }

        float2 pick(float2 q, float which){
            if (which < 0.5) return orion(q);
            if (which < 1.5) return plough(q);
            if (which < 2.5) return cassiopeia(q);
            return cygnus(q);
        }

        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0 + uC1 * fbm(p * 3.0) * 0.05;

            // The sky the figure is picked out of.
            float2 g = p * uP3;
            float2 cell = floor(g);
            float2 f = fract(g) - 0.5;
            for (int j = 0; j < 9; j++){
                float fj = float(j);
                float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                float2 id = cell + o;
                float mag = pow(hash(id + 0.7), 7.0);
                if (mag > 0.003){
                    float2 d = f - o + (hash2(id) - 0.5) * 0.8;
                    c += starColor(hash(id + 4.2)) * psf(length(d), 0.011 + mag * 0.02) * mag * 0.7;
                }
            }

            // One figure at a time, filling the display, handing over to the next. Four of them at
            // a fifth of the size each was four things too small to recognise rather than one
            // anybody could name.
            float cycle = t * uP2;
            float phase = fract(cycle);
            float which = mod(floor(cycle), 4.0);
            float2 q = p / uP0;

            c += figure(pick(q, which), uP0, smoothstep(1.0, 0.82, phase) * smoothstep(0.0, 0.14, phase));
            c += figure(pick(q, mod(which + 1.0, 4.0)), uP0, smoothstep(0.86, 1.0, phase));
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "meteors", "Meteor Shower", ShaderFamily.STARS,
        "A still sky, cut by something moving very fast.",
        listOf(
            param("rate", "Fall rate", .3f, 3f, 1.1f),
            param("trail", "Trail length", .15f, 1.2f, .60f),
            param("radiant", "Direction", -3.14f, 3.14f, -2.4f),
            param("spread", "Spread", 0f, 1.2f, .35f)
        ),
        NIGHT,
        """
        float seg(float2 p, float2 a, float2 b){
            float2 pa = p - a, ba = b - a;
            return length(pa - ba * clamp(dot(pa, ba) / max(dot(ba, ba), 0.000001), 0.0, 1.0));
        }

        float3 scene(float2 p, float2 uv, float t){
            // A clean sky. The earlier haze wash sat under everything and turned the whole frame
            // muddy, which no amount of work on the meteors themselves was going to fix.
            float3 c = uC0;
            float2 g = p * 15.0;
            float2 cell = floor(g);
            float2 f = fract(g) - 0.5;
            for (int j = 0; j < 9; j++){
                float fj = float(j);
                float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                float2 id = cell + o;
                float mag = pow(hash(id + 0.7), 5.5);
                if (mag > 0.003){
                    float2 d = f - o + (hash2(id) - 0.5) * 0.8;
                    float twinkle = 0.80 + 0.20 * sin(t * (1.6 + hash(id) * 5.0) + hash(id) * 30.0);
                    c += starColor(hash(id + 4.2)) * psf(length(d), 0.009 + mag * 0.016) * mag * twinkle;
                }
            }

            for (int i = 0; i < 8; i++){
                float fi = float(i);
                float clock = t * uP0 * 0.30 + fi * 0.53;
                float life = fract(clock);
                float2 seed = hash2(float2(fi, floor(clock)));

                float aim = uP2 + (seed.x - 0.5) * uP3;
                float2 dir = float2(cos(aim), sin(aim));
                float2 across = float2(-dir.y, dir.x) * (seed.y - 0.5) * 1.0;
                float2 head = across - dir * 0.8 + dir * life * 1.6;
                float trail = uP1 * (0.5 + seed.y * 0.5);
                float2 tail = head - dir * trail;

                float d = seg(p, tail, head);
                float along = clamp(dot(p - tail, dir) / max(trail, 0.001), 0.0, 1.0);
                float fade = smoothstep(0.0, 0.06, life) * smoothstep(1.0, 0.70, life);

                // A hairline that stays a hairline. The previous one widened toward the head and
                // carried a fat round glow on the end, which is what made it a tadpole; a meteor is
                // a scratch of light, brightest at the front and thinning to nothing behind.
                float taper = pow(along, 2.5);
                float3 tint = starColor(0.45 + seed.y * 0.5);
                c += tint * exp(-d * 900.0) * taper * fade * 2.2;
                c += tint * exp(-d * 260.0) * taper * fade * 0.45;
                // Just enough bloom at the very tip to read as heat, not as a head.
                c += float3(1.0) * exp(-length(p - head) * 190.0) * fade * 0.9;
            }
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "supernova", "Supernova", ShaderFamily.STARS,
        "A star runs out, and takes the sky with it.",
        listOf(
            param("rate", "How often", .05f, .6f, .16f),
            param("swell", "Warning", 0f, 2f, 1f),
            param("expand", "Expansion", .3f, 2f, .9f),
            param("field", "Background stars", 5f, 18f, 10f)
        ),
        listOf(MIDNIGHT, HYDROGEN, EMBER, ICE, VIOLET, MONO),
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0 + uC1 * fbm(p * 2.6) * 0.05;

            float2 g = p * uP3;
            float2 cell = floor(g);
            float2 f = fract(g) - 0.5;
            for (int j = 0; j < 9; j++){
                float fj = float(j);
                float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                float2 id = cell + o;
                float mag = pow(hash(id + 1.1), 6.0);
                if (mag > 0.003){
                    float2 d = f - o + (hash2(id) - 0.5) * 0.8;
                    c += starColor(hash(id + 3.3)) * psf(length(d), 0.011 + mag * 0.02) * mag * 0.85;
                }
            }

            // Two events running half a cycle apart. One is always still fading while the next is
            // building, so the sky never cuts back to empty — the reset was the one thing wrong
            // with it, and it was an artefact of there being a single event to reset.
            for (int e = 0; e < 2; e++){
            float clock = t * uP0 + float(e) * 0.5;
            float life = fract(clock);
            float2 seed = hash2(float2(floor(clock), 5.0 + float(e) * 13.0));
            float2 at = (seed - 0.5) * 0.55;
            float2 d = p - at;
            float r = length(d);

            // Before: the star swells and brightens. Something is about to happen, and the wait is
            // most of what makes the moment land.
            float warn = smoothstep(0.0, 0.30, life);
            c += starColor(0.85) * psf(r, 0.005 + warn * 0.010)
                 * (0.35 + warn * warn * warn * 26.0 * uP1);

            float gone = smoothstep(0.30, 0.335, life);
            float age = max(life - 0.315, 0.0);

            // The flash. Everything nearby is briefly lit by it, and the aperture answers with
            // spikes, because that is what a camera does with light it cannot hold.
            float flash = exp(-age * 30.0) * gone;
            c += float3(1.0, 0.97, 0.90) * flash * (0.30 + 0.75 / (1.0 + r * 7.0));
            c += float3(1.0, 0.95, 0.85)
                 * (exp(-abs(d.x) * 90.0) + exp(-abs(d.y) * 90.0)) * exp(-r * 3.0) * flash * 0.6;

            // The shell: thrown outward, ragged rather than circular, thinning and cooling as it
            // goes — white, then blue, then the red of a remnant left behind.
            float shell = age * uP2 * 2.6;
            float around = atan(d.y, d.x);
            float ragged = 1.0 + fbm(float2(around * 2.2, shell * 2.6)) * 0.22 - 0.11;
            float thickness = 0.010 + shell * 0.13;
            float edge = exp(-pow(abs(r - shell * ragged) / thickness, 2.0));

            float3 hot = mix(float3(1.0, 0.98, 0.94), float3(0.58, 0.74, 1.0), clamp(shell * 1.5, 0.0, 1.0));
            hot = mix(hot, float3(1.0, 0.34, 0.20), clamp(shell * 1.0 - 0.45, 0.0, 1.0));
            float dying = exp(-age * 3.0);
            c += hot * edge * gone * dying * 3.2;

            // What is left inside the shell, glowing on for a while after.
            c += mix(uC2, uC3, 0.4) * exp(-pow(r / max(shell, 0.02), 2.0)) * gone * dying * 0.30;
            }
            return c;
        }
        """,
        dispersive = true
    ),

    // ---------------------------------------------------------------- Minimal
    //
    // Flat pattern, and nothing else: no lighting, no depth, no atmosphere. Everything is an edge
    // and a fill, which is the whole discipline — what is left when there is nowhere to hide.

    ShaderScene(
        "bauhaus", "Bauhaus", ShaderFamily.MINIMAL,
        "Discs, quarters and bars, rearranging.",
        listOf(
            param("cells", "Cells", 2f, 9f, 4f),
            param("turn", "Rearrange", 0f, 1.5f, .3f),
            param("fill", "How full", .2f, 1f, .7f),
            param("edge", "Softness", 0f, .06f, .006f)
        ),
        FLAT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            float2 g = uv * uP0;
            float2 id = floor(g);
            float2 f = fract(g);

            // The wall re-rolls on a slow clock rather than animating: a flat pattern that eases
            // between states stops being flat, because easing is depth by another name.
            float epoch = floor(t * uP1);
            float roll = hash(id + epoch * 7.3);
            if (roll > uP2) return c;

            float3 ink = roll < uP2 * 0.34 ? uC1 : (roll < uP2 * 0.67 ? uC2 : uC3);
            float quarter = floor(hash(id + epoch * 3.1) * 4.0) * 1.5708;
            float2 q = rot(f - 0.5, quarter) + 0.5;

            float kind = hash(id + epoch * 11.7);
            float mask;
            if (kind < 0.34) {
                mask = smoothstep(0.42 + uP3, 0.42 - uP3, length(q - 0.5));
            } else if (kind < 0.67) {
                // A quarter disc struck from one corner of the cell.
                mask = smoothstep(0.86 + uP3, 0.86 - uP3, length(q));
            } else {
                mask = smoothstep(0.30 + uP3, 0.30 - uP3, abs(q.y - 0.5));
            }
            return mix(c, ink, mask);
        }
        """
    ),

    ShaderScene(
        "halftone", "Halftone", ShaderFamily.MINIMAL,
        "A field of dots, breathing.",
        listOf(
            param("pitch", "Pitch", 8f, 46f, 22f),
            param("speed", "Speed", 0f, 2f, .5f),
            param("depth", "Contrast", .2f, 1f, .8f),
            param("angle", "Angle", 0f, 1.6f, .4f)
        ),
        FLAT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = rot(p, uP3);
            float2 g = q * uP0;
            float2 f = fract(g) - 0.5;

            // The dot's size follows one slow wave across the field, which is the entire idea: a
            // halftone is a picture drawn in dot area, so the wave has to be in the area.
            float wave = sin(q.x * 3.0 + t * uP1) * 0.5 + sin(q.y * 2.2 - t * uP1 * 0.7) * 0.5;
            float radius = (0.16 + 0.30 * (wave * 0.5 + 0.5)) * uP2;
            float dot = smoothstep(radius, radius - 0.06, length(f));
            return mix(uC0, uC2, dot);
        }
        """
    ),

    ShaderScene(
        "bars", "Bars", ShaderFamily.MINIMAL,
        "Hard diagonal bands, sliding.",
        listOf(
            param("width", "Band width", 4f, 30f, 12f),
            param("speed", "Slide", 0f, 2f, .5f),
            param("angle", "Angle", -1.6f, 1.6f, .5f),
            param("accent", "Accent every", 2f, 9f, 4f)
        ),
        FLAT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = rot(p, uP2);
            float band = q.y * uP0 + t * uP1;
            float index = floor(band);
            float within = fract(band);

            // Every so many bands takes the accent colour, so the eye has something to count by
            // and the rhythm is legible rather than a texture.
            float accent = step(mod(index, uP3), 0.5);
            float3 ink = mix(uC1, uC3, accent);
            float edge = smoothstep(0.0, 0.02, within) * smoothstep(1.0, 0.98, within);
            return mix(uC0, ink, edge * step(0.5, fract(index * 0.5)));
        }
        """
    ),

    ShaderScene(
        "grid", "Grid", ShaderFamily.MINIMAL,
        "Ruled squares, a few of them filled.",
        listOf(
            param("pitch", "Pitch", 4f, 26f, 12f),
            param("rule", "Rule weight", .002f, .04f, .012f),
            param("filled", "How many filled", 0f, .5f, .12f),
            param("turn", "Change", 0f, 1.5f, .25f)
        ),
        FLAT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 g = uv * uP0;
            float2 id = floor(g);
            float2 f = fract(g);

            float3 c = uC0;
            float epoch = floor(t * uP3);
            if (hash(id + epoch * 5.1) < uP2) c = mix(c, uC2, 0.9);

            // The rule sits on top of the fill, so a filled square is still part of the grid
            // rather than a hole punched through it.
            float line = max(
                smoothstep(uP1, 0.0, min(f.x, 1.0 - f.x)),
                smoothstep(uP1, 0.0, min(f.y, 1.0 - f.y))
            );
            return mix(c, uC3, line * 0.55);
        }
        """
    ),

    // ---------------------------------------------------------------- Candy
    //
    // A sweet is a solid object, so it is shaded as one. Each is treated as a sphere: the surface
    // normal is recovered from how far a point sits from the centre, and the diffuse falloff, the
    // specular dot and the rim all come from that single normal, so they agree about where the
    // light is. The pattern inside is displaced by the same normal, which is what a curved,
    // transparent surface does to whatever is behind it — that displacement is most of what
    // separates a boiled sweet from a coloured circle.

    ShaderScene(
        "boiled", "Bonbons", ShaderFamily.CANDY,
        "Glass discs with the swirl set inside them, rolling past.",
        listOf(
            param("size", "Size", 2.5f, 9f, 4.5f),
            param("swirl", "Swirl", 1f, 9f, 5f),
            param("drift", "Drift", 0f, 1.5f, .5f),
            param("gloss", "Gloss", 0f, 2f, 1f)
        ),
        BRIGHT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = mix(uC0, uC1, clamp(uv.y * 0.7, 0.0, 1.0) * 0.5);
            float3 lightDir = normalize(float3(-0.45, 0.55, 0.70));

            // The whole tray slides, and each sweet turns on its own path within its place. A
            // field of them sitting perfectly still is a wallpaper of circles; it is the drift
            // across the frame and the swirl turning behind the glass that make them objects.
            float2 g = p * uP0 + float2(t * uP2 * 0.20, t * uP2 * 0.07);
            float2 cell = floor(g);
            float2 f = fract(g) - 0.5;

            for (int j = 0; j < 9; j++){
                float fj = float(j);
                float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                float2 id = cell + o;
                float2 seed = hash2(id);
                float wander = t * uP2 * (0.4 + seed.y * 0.7) + seed.x * 6.28;
                float2 at = o + (seed - 0.5) * 0.34
                    + float2(cos(wander), sin(wander * 0.8)) * 0.07;
                float2 d = f - at;
                float radius = 0.30 + seed.x * 0.09;
                float unit = length(d) / radius;

                if (unit < 1.0){
                    // The sphere the sweet is: z from the circle equation, and the normal with it.
                    float z = sqrt(max(1.0 - unit * unit, 0.0));
                    float3 n = normalize(float3(d / radius, z));

                    // The swirl is read at a displaced position, so it bends toward the rim the
                    // way anything seen through a lens does, and stands still in the middle.
                    float2 inner = d / radius - n.xy * 0.30;
                    float spin = t * (0.5 + uP2) * (seed.y - 0.5) * 2.2;
                    float arms = sin(atan(inner.y, inner.x) * uP1 + length(inner) * 9.0 + spin);
                    float3 body = mix(pal(fract(hash(id + 2.3) + 0.1)),
                                      pal(fract(hash(id + 2.3) + 0.45)),
                                      smoothstep(-0.35, 0.35, arms));

                    float diffuse = 0.55 + 0.45 * max(dot(n, lightDir), 0.0);
                    float3 shaded = body * diffuse;
                    // Sugar is not metal: the highlight is tight and white, and there is a second
                    // faint one where the far side of the glass catches the same light.
                    float spec = pow(max(dot(reflect(-lightDir, n), float3(0.0, 0.0, 1.0)), 0.0), 48.0);
                    shaded += float3(spec * 0.9 * uP3);
                    shaded += float3(pow(1.0 - z, 4.0) * 0.35 * uP3);

                    // Sat on something, so it has a shadow under it and an edge that is not a step.
                    float edge = smoothstep(1.0, 0.94, unit);
                    c = mix(c, shaded, edge);
                }
                float contact = smoothstep(radius * 1.25, radius * 0.9, length(d - float2(0.0, 0.05)));
                c = mix(c, c * 0.88, contact * 0.35 * (1.0 - smoothstep(1.0, 0.9, unit)));
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "petalfall", "Petal Fall", ShaderFamily.BLOOM,
        "Turning over as they come down.",
        listOf(
            param("count", "How many", 6f, 26f, 15f),
            param("fall", "Fall", .1f, 1.2f, .35f),
            param("size", "Size", .05f, .22f, .11f),
            param("curl", "Curl", 0f, 1f, .6f)
        ),
        BRIGHT,
        """
        float petal(float2 q, float len, float wide){
            if (q.y < -len * 0.5 || q.y > len * 0.5) return 0.0;
            float along = (q.y + len * 0.5) / len;
            float w = wide * sin(3.14159 * pow(along, 0.62));
            return smoothstep(w, w * 0.76, abs(q.x));
        }

        float3 scene(float2 p, float2 uv, float t){
            float3 c = mix(uC0, uC1, clamp(uv.y, 0.0, 1.0) * 0.45);

            for (int i = 0; i < 26; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float2 seed = hash2(float2(fi, 4.0));

                float fall = fract(seed.y + t * uP1 * (0.4 + seed.x * 0.5));
                // Down the screen, which is toward +y here: the fragment coordinate starts at the
                // top-left, so subtracting from y sent these upward — they were rising, not
                // falling, which is the one thing the scene is named for.
                float2 at = float2(
                    (seed.x - 0.5) * 1.25 + sin(t * (0.4 + seed.y) + seed.x * 6.3) * 0.12,
                    -0.66 + fall * 1.35
                );

                // Turning about its own long axis: the petal narrows to nothing as it comes edge
                // on and opens out again, which is what a falling petal actually does and what a
                // spinning oval never does.
                float turn = t * (0.9 + seed.y * 1.4) + seed.x * 6.3;
                float face = cos(turn);
                float2 q = rot(p - at, seed.x * 6.3 + t * 0.25 * uP3);
                q.x /= max(abs(face), 0.12);

                float len = uP2 * (0.8 + seed.y * 0.5);
                float mask = petal(q, len, len * 0.36);
                if (mask > 0.001){
                    float along = clamp((q.y + len * 0.5) / len, 0.0, 1.0);
                    float3 front = pal(fract(hash(float2(fi, 9.0)) * 0.4 + 0.1));
                    // The underside is paler: which face is toward you is the sign of the turn.
                    float3 tint = face > 0.0 ? front : mix(front, float3(1.0), 0.45);
                    tint = mix(tint * 0.86, mix(tint, float3(1.0), 0.3), along);
                    c = mix(c, tint, mask * (0.35 + 0.65 * abs(face)));
                }
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "lollipops", "Lollipops", ShaderFamily.CANDY,
        "Spirals turning on their sticks.",
        listOf(
            param("size", "Size", 1.6f, 5f, 2.6f),
            param("turns", "Spiral turns", 1.5f, 8f, 4f),
            param("spin", "Spin", 0f, 2f, .7f),
            param("gloss", "Gloss", 0f, 2f, 1f)
        ),
        BRIGHT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = mix(uC0, uC1, clamp(uv.y * 0.8, 0.0, 1.0) * 0.45);
            float3 lightDir = normalize(float3(-0.45, 0.55, 0.70));

            float2 g = p * uP0 + float2(t * 0.06, 0.0);
            float2 cell = floor(g);
            float2 f = fract(g) - 0.5;

            for (int j = 0; j < 9; j++){
                float fj = float(j);
                float2 o = float2(mod(fj, 3.0) - 1.0, floor(fj / 3.0) - 1.0);
                float2 id = cell + o;
                float2 seed = hash2(id);
                float2 at = o + (seed - 0.5) * 0.30;
                float2 d = f - at;
                float radius = 0.26 + seed.x * 0.06;

                // The stick first, so the head sits over it rather than beside it.
                float2 stickTop = at + float2(0.0, radius * 0.6);
                float2 s = f - stickTop;
                float stick = smoothstep(radius * 0.075, radius * 0.05, abs(s.x))
                    * step(0.0, s.y) * smoothstep(radius * 1.5, radius * 1.35, s.y);
                c = mix(c, mix(uC1, float3(1.0), 0.55), stick * 0.9);

                float unit = length(d) / radius;
                if (unit < 1.0){
                    float z = sqrt(max(1.0 - unit * unit, 0.0));
                    float3 n = normalize(float3(d / radius, z));

                    // An Archimedean spiral: the angle advances with the radius, so the arm keeps
                    // an even gap as it winds out. Reading it through the displaced position is
                    // what curves it over the dome instead of lying flat on it.
                    float2 inner = d / radius - n.xy * 0.26;
                    float angle = atan(inner.y, inner.x);
                    float arm = fract((angle / 6.28318) * 1.0 + length(inner) * uP1
                        + t * uP2 * (seed.y - 0.5) * 2.0);
                    float band = smoothstep(0.46, 0.5, arm) * smoothstep(0.98, 0.94, arm);
                    float3 body = mix(pal(fract(hash(id + 1.9) + 0.08)), float3(1.0), band * 0.85);

                    float3 shaded = body * (0.55 + 0.45 * max(dot(n, lightDir), 0.0));
                    shaded += float3(pow(max(dot(reflect(-lightDir, n), float3(0.0, 0.0, 1.0)), 0.0), 44.0) * 0.85 * uP3);
                    shaded += float3(pow(1.0 - z, 4.0) * 0.30 * uP3);
                    c = mix(c, shaded, smoothstep(1.0, 0.94, unit));
                }
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "taffy", "Pulled Taffy", ShaderFamily.CANDY,
        "Twisted ropes, still glossy from the pull.",
        listOf(
            param("count", "Ropes", 2f, 7f, 4f),
            param("twist", "Twist", 0f, 3f, 1.4f),
            param("width", "Width", .05f, .26f, .12f),
            param("gloss", "Gloss", 0f, 2f, 1f)
        ),
        BRIGHT,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = mix(uC0, uC1, clamp(uv.y, 0.0, 1.0) * 0.4);

            for (int i = 0; i < 7; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float lane = (fi + 0.5) / max(uP0, 1.0) - 0.5;
                float centre = lane * 1.15 + sin(p.x * 1.5 + fi * 2.1 + t * 0.35) * 0.10;
                float across = (p.y - centre) / uP2;

                if (abs(across) < 1.0){
                    // A rope is a cylinder, so the normal comes from how far across it a point is
                    // and the highlight runs its whole length rather than sitting in one place.
                    float z = sqrt(max(1.0 - across * across, 0.0));
                    float3 n = normalize(float3(0.0, across, z));
                    float3 lightDir = normalize(float3(-0.3, 0.55, 0.78));

                    // The twist: two colours wound round each other, the seam advancing along the
                    // rope, which is what pulling taffy actually leaves behind.
                    float wind = sin((p.x * 5.0 + t * 0.8) * uP1 + asin(clamp(across, -1.0, 1.0)));
                    float3 body = mix(pal(fract(fi * 0.27 + 0.08)),
                                      pal(fract(fi * 0.27 + 0.42)),
                                      smoothstep(-0.3, 0.3, wind));

                    float3 shaded = body * (0.5 + 0.5 * max(dot(n, lightDir), 0.0));
                    shaded += float3(pow(max(dot(reflect(-lightDir, n), float3(0.0, 0.0, 1.0)), 0.0), 32.0) * 0.7 * uP3);
                    shaded += float3(pow(1.0 - z, 3.0) * 0.22 * uP3);
                    c = mix(c, shaded, smoothstep(1.0, 0.9, abs(across)));
                }
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "waterpetals", "Petals on Water", ShaderFamily.BLOOM,
        "Floating, and the water will not hold them still.",
        listOf(
            param("count", "How many", 4f, 18f, 9f),
            param("ripple", "Ripple", 0f, 2f, .9f),
            param("size", "Size", .05f, .2f, .1f),
            param("drift", "Drift", 0f, 1f, .3f)
        ),
        BRIGHT,
        """
        float petal(float2 q, float len, float wide){
            if (q.y < -len * 0.5 || q.y > len * 0.5) return 0.0;
            float along = (q.y + len * 0.5) / len;
            float w = wide * sin(3.14159 * pow(along, 0.62));
            return smoothstep(w, w * 0.76, abs(q.x));
        }

        float3 scene(float2 p, float2 uv, float t){
            // The surface: rings running outward, crossed by a slower swell.
            float rings = sin(length(p - float2(0.2, -0.15)) * 26.0 - t * 1.6) * 0.5
                + sin(length(p + float2(0.25, 0.2)) * 19.0 - t * 1.1) * 0.5;
            float swell = fbm(p * 3.0 + float2(0.0, t * 0.08));
            float height = rings * 0.35 + swell;

            // Slope of the surface, which is what bends the light and moves what floats on it.
            float e = 0.004;
            float2 slope = float2(
                sin(length(p + float2(e, 0.0) - float2(0.2, -0.15)) * 26.0 - t * 1.6) - rings * 2.0,
                sin(length(p + float2(0.0, e) - float2(0.2, -0.15)) * 26.0 - t * 1.6) - rings * 2.0
            ) * uP1;

            float3 c = mix(uC0, uC1, clamp(0.5 + height * 0.4, 0.0, 1.0));
            c += mix(uC2, float3(1.0), 0.5) * pow(clamp(height * 0.5 + 0.5, 0.0, 1.0), 8.0) * 0.5;

            for (int i = 0; i < 18; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float2 seed = hash2(float2(fi, 6.0));

                // Carried by the water rather than travelling through it: each drifts slowly and
                // is nudged by the same slope that is bending the light.
                float2 at = float2(
                    fract(seed.x + t * uP3 * 0.04 * (0.5 + seed.y)) * 1.4 - 0.7,
                    (seed.y - 0.5) * 1.1 + sin(t * 0.3 + seed.x * 6.3) * 0.05
                ) + slope * 0.03;

                float turn = seed.x * 6.3 + sin(t * 0.4 + seed.y * 6.3) * 0.35;
                float2 q = rot(p - at, turn);
                float len = uP2 * (0.85 + seed.y * 0.4);
                float mask = petal(q, len, len * 0.36);
                if (mask > 0.001){
                    float along = clamp((q.y + len * 0.5) / len, 0.0, 1.0);
                    float3 tint = pal(fract(hash(float2(fi, 3.0)) * 0.35 + 0.1));
                    tint = mix(tint * 0.85, mix(tint, float3(1.0), 0.35), along);
                    // A petal sits in the water, so it darkens what is under its edge.
                    float shadow = petal(rot(p - at - float2(0.012, 0.012), turn), len, len * 0.36);
                    c = mix(c, c * 0.82, shadow * 0.5);
                    c = mix(c, tint, mask);
                }
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "wisteria", "Wisteria", ShaderFamily.BLOOM,
        "Hanging strands, swaying together.",
        listOf(
            param("strands", "Strands", 3f, 10f, 6f),
            param("length", "Length", .4f, 1.4f, .95f),
            param("sway", "Sway", 0f, 1.5f, .6f),
            param("size", "Flower size", .012f, .05f, .026f)
        ),
        BRIGHT,
        """
        float petal(float2 q, float len, float wide){
            if (q.y < -len * 0.5 || q.y > len * 0.5) return 0.0;
            float along = (q.y + len * 0.5) / len;
            float w = wide * sin(3.14159 * pow(along, 0.62));
            return smoothstep(w, w * 0.74, abs(q.x));
        }

        float3 scene(float2 p, float2 uv, float t){
            float3 c = mix(uC0, uC1, clamp(uv.y, 0.0, 1.0) * 0.5);

            for (int i = 0; i < 10; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float2 seed = hash2(float2(fi, 7.0));
                float anchor = (fi + 0.5) / max(uP0, 1.0) - 0.5;
                float x = anchor * 1.25 + (seed.x - 0.5) * 0.06;
                float len = uP1 * (0.7 + seed.y * 0.6);

                for (int k = 0; k < 14; k++){
                    float fk = float(k);
                    float along = fk / 13.0;
                    if (along > 1.0) break;

                    // The whole strand swings from where it is fixed, so the sway grows toward the
                    // tip: a strand that moves evenly along its length reads as a sliding image.
                    float swing = sin(t * (0.7 + seed.y * 0.5) + fi * 1.3) * uP2 * 0.18 * along * along;
                    float2 at = float2(x + swing, -0.62 + along * len);

                    // Bigger and paler at the top, small and saturated at the tip, which is the
                    // way a raceme opens: the oldest flowers are the ones nearest the branch.
                    float size = uP3 * (1.25 - along * 0.55);
                    float3 tint = mix(mix(uC2, float3(1.0), 0.55), uC3, along * 0.8);

                    for (int q3 = 0; q3 < 3; q3++){
                        float fq = float(q3);
                        float turn = fq * 2.094 + seed.x * 6.3 + swing * 2.0;
                        float mask = petal(rot(p - at, turn) - float2(0.0, size * 0.5), size * 1.6, size * 0.7);
                        c = mix(c, tint, mask);
                    }
                }
            }
            return c;
        }
        """
    )
)

fun sceneById(id: String): ShaderScene = shaderScenes.firstOrNull { it.id == id } ?: shaderScenes.first()

fun scenesInFamily(family: ShaderFamily): List<ShaderScene> = shaderScenes.filter { it.family == family }
