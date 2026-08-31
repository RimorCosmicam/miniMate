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
    FIELD("Fields"),
    GEOMETRY("Geometry"),
    FRACTAL("Fractals"),
    MOTION("Motion"),
    CIRCUIT("Circuits"),
    DEPTH("Depth")
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

    // ---------------------------------------------------------------- Fields

    ShaderScene(
        "plasma", "Plasma Flow", ShaderFamily.FIELD,
        "Domain-warped noise folding through itself.",
        listOf(
            param("turbulence", "Turbulence", 0f, 3f, 1.2f),
            param("scale", "Scale", .5f, 6f, 2.2f),
            param("speed", "Speed", 0f, 2f, .5f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = p * uP1;
            float2 w = float2(fbm(q + t * uP2), fbm(q + 5.2 - t * uP2 * 0.7));
            float2 r = float2(fbm(q + w * uP0 + 1.7), fbm(q + w * uP0 + 8.3));
            float v = fbm(q + r * uP0);
            float3 c = pal(v);
            c += uC3 * pow(clamp(v, 0.0, 1.0), 6.0) * 0.5;
            return c;
        }
        """
    ),

    ShaderScene(
        "nebula", "Nebula Drift", ShaderFamily.FIELD,
        "Layered volumetric cloud with starlight behind.",
        listOf(
            param("density", "Density", .3f, 2.5f, 1f),
            param("depth", "Layers", 2f, 6f, 4f),
            param("drift", "Drift", 0f, 1f, .25f),
            param("stars", "Stars", 0f, 1f, .5f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            float acc = 0.0;
            for (int i = 0; i < 6; i++){
                float fi = float(i);
                if (fi >= uP1) break;
                float sc = 1.0 + fi * 0.85;
                float2 q = p * sc + float2(t * uP2 * (0.2 + fi * 0.1), -t * uP2 * 0.15);
                float d = fbm(q) * (1.0 / (1.0 + fi * 0.7));
                acc += d;
            }
            acc *= uP0;
            float st = hash(floor(p * 260.0));
            float star = smoothstep(0.9985, 1.0, st) * uP3;
            c = mix(c, uC1, clamp(acc * 0.7, 0.0, 1.0));
            c = mix(c, uC2, clamp(acc * acc * 0.5, 0.0, 1.0));
            c += uC3 * star;
            return c;
        }
        """
    ),

    ShaderScene(
        "ink", "Ink Diffusion", ShaderFamily.FIELD,
        "Pigment blooming through water.",
        listOf(
            param("spread", "Spread", .5f, 4f, 1.8f),
            param("contrast", "Contrast", .5f, 4f, 1.8f),
            param("flow", "Flow", 0f, 1.5f, .4f)
        ),
        ORGANIC,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = p * uP0;
            float f = fbm(q + fbm(q * 1.7 + t * uP2) * 1.5);
            float edge = smoothstep(0.42, 0.58, f);
            float veins = smoothstep(0.48, 0.5, f) - smoothstep(0.5, 0.52, f);
            float3 c = mix(uC0, uC1, pow(edge, uP1));
            c = mix(c, uC2, veins * 2.0);
            c = mix(c, uC3, pow(edge, uP1 * 3.0) * 0.5);
            return c;
        }
        """
    ),

    ShaderScene(
        "aurora", "Aurora Curtains", ShaderFamily.FIELD,
        "Vertical light sheets shimmering over a dark sky.",
        listOf(
            param("waviness", "Waviness", 0f, 3f, 1.2f),
            param("height", "Height", .2f, 1.5f, .7f),
            param("shimmer", "Shimmer", 0f, 2f, .8f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            for (int i = 0; i < 4; i++){
                float fi = float(i);
                float off = fi * 0.37;
                float wave = sin(p.x * (2.0 + fi) + t * (0.4 + fi * 0.2)) * 0.12 * uP0;
                float band = exp(-pow((p.y - wave + off * 0.25 - 0.1) / uP1, 2.0) * 6.0);
                float sh = 0.6 + 0.4 * sin(p.x * 18.0 + t * uP2 * 3.0 + fi);
                c += mix(uC1, uC2, fi / 3.0) * band * sh * 0.55;
            }
            c += uC3 * pow(clamp(c.g, 0.0, 1.0), 5.0) * 0.4;
            return c;
        }
        """
    ),

    // ---------------------------------------------------------------- Geometry

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

    // ---------------------------------------------------------------- Fractals

    ShaderScene(
        "julia", "Julia Bloom", ShaderFamily.FRACTAL,
        "A Julia set breathing through its parameter space.",
        listOf(
            param("seed", "Seed drift", 0f, 1f, .35f),
            param("zoom", "Zoom", .4f, 3f, 1.1f),
            param("iter", "Detail", 8f, 48f, 24f)
        ),
        VIOLETS,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 z = p / uP1;
            float2 c0 = float2(0.7885 * cos(t * 0.18 + uP0 * 6.0), 0.7885 * sin(t * 0.21 + uP0 * 6.0));
            float n = 0.0;
            for (int i = 0; i < 48; i++){
                if (float(i) >= uP2) break;
                z = float2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c0;
                if (dot(z, z) > 16.0) break;
                n += 1.0;
            }
            float v = n / max(uP2, 1.0);
            float3 c = pal(pow(v, 0.6));
            c += uC3 * pow(v, 12.0) * 0.6;
            return c;
        }
        """
    ),

    ShaderScene(
        "mandel", "Mandelbrot Dive", ShaderFamily.FRACTAL,
        "A slow descent into the boundary.",
        listOf(
            param("zoom", "Zoom rate", 0f, 1f, .25f),
            param("iter", "Detail", 16f, 64f, 40f),
            param("cycle", "Colour cycle", 0f, 3f, 1f)
        ),
        WARM,
        """
        float3 scene(float2 p, float2 uv, float t){
            float zoom = exp(-t * uP0 * 0.35) * 1.6 + 0.0008;
            float2 target = float2(-0.743643887, 0.131825904);
            float2 c0 = target + p * zoom;
            float2 z = float2(0.0);
            float n = 0.0;
            for (int i = 0; i < 64; i++){
                if (float(i) >= uP1) break;
                z = float2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c0;
                if (dot(z, z) > 16.0) break;
                n += 1.0;
            }
            float v = n / max(uP1, 1.0);
            float3 c = pal(fract(v * uP2 + t * 0.05));
            if (n >= uP1 - 0.5) c = uC0;
            return c;
        }
        """
    ),

    ShaderScene(
        "apollo", "Apollonian", ShaderFamily.FRACTAL,
        "Nested circles packing into infinity.",
        listOf(
            param("depth", "Depth", 3f, 12f, 7f),
            param("scale", "Scale", .8f, 3f, 1.4f),
            param("spin", "Spin", -1f, 1f, .2f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 z = rot(p, t * uP2) * uP1;
            float s = 1.0;
            float d = 1e9;
            for (int i = 0; i < 12; i++){
                if (float(i) >= uP0) break;
                z = -1.0 + 2.0 * fract(0.5 * z + 0.5);
                float r2 = dot(z, z);
                float k = 1.1 / max(r2, 0.06);
                z *= k; s *= k;
                d = min(d, abs(length(z) - 1.0) / s);
            }
            float g = exp(-d * 90.0);
            float3 c = mix(uC0, uC1, exp(-d * 18.0));
            c = mix(c, uC2, g);
            c += uC3 * pow(g, 3.0);
            return c;
        }
        """
    ),

    // ---------------------------------------------------------------- Motion

    ShaderScene(
        "tunnel", "Tunnel Rush", ShaderFamily.MOTION,
        "Falling forward through a ribbed shaft.",
        listOf(
            param("speed", "Speed", 0f, 3f, 1f),
            param("twist", "Twist", -2f, 2f, .5f),
            param("rings", "Ring density", 2f, 30f, 12f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float r = max(length(p), 0.001);
            float a = atan(p.y, p.x);
            float z = 1.0 / r + t * uP0;
            a += z * uP1 * 0.15;
            float ring = 0.5 + 0.5 * sin(z * uP2);
            float rib = 0.5 + 0.5 * sin(a * 12.0);
            float fade = smoothstep(0.0, 0.9, r);
            float3 c = mix(uC0, uC1, ring * 0.8);
            c = mix(c, uC2, ring * rib * 0.9);
            c += uC3 * pow(ring, 6.0) * 0.7;
            c *= fade;
            return c;
        }
        """
    ),

    ShaderScene(
        "hyperspace", "Hyperspace", ShaderFamily.MOTION,
        "Stars stretching into streaks.",
        listOf(
            param("density", "Star density", 20f, 200f, 80f),
            param("speed", "Speed", 0f, 3f, 1f),
            param("streak", "Streak", 0f, 1f, .6f)
        ),
        COOL,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            float r = max(length(p), 0.001);
            float a = atan(p.y, p.x);
            for (int i = 0; i < 3; i++){
                float fi = float(i);
                float lane = floor(a / 6.2831853 * uP0 + fi * 13.0);
                float seed = hash(float2(lane, fi));
                float z = fract(seed + t * uP1 * (0.4 + seed * 0.6));
                float rr = z * 1.4;
                float d = abs(r - rr);
                float len = mix(0.02, 0.25, uP2) * z;
                float star = exp(-d / max(len, 0.004)) * smoothstep(0.0, 0.2, z);
                float ang = abs(mod(a * uP0 / 6.2831853, 1.0) - 0.5);
                star *= smoothstep(0.5, 0.0, ang * 2.0);
                c += mix(uC2, uC3, z) * star * 0.5;
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "ribbons", "Silk Ribbons", ShaderFamily.MOTION,
        "Wide bands of light folding over each other.",
        listOf(
            param("count", "Ribbons", 1f, 8f, 4f),
            param("wave", "Wave", 0f, 3f, 1.2f),
            param("width", "Width", .02f, .4f, .12f)
        ),
        WARM,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            for (int i = 0; i < 8; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float ph = fi * 1.7;
                float y = sin(p.x * (1.4 + fi * 0.4) + t * (0.5 + fi * 0.18) + ph) * 0.28 * uP1;
                y += sin(p.x * 3.1 - t * 0.7 + ph) * 0.08 * uP1;
                float d = abs(p.y - y + (fi - uP0 * 0.5) * 0.12);
                float band = smoothstep(uP2, 0.0, d);
                float3 tint = mix(uC1, uC2, fi / max(uP0 - 1.0, 1.0));
                c += tint * band * 0.7;
                c += uC3 * pow(band, 5.0) * 0.5;
            }
            return c;
        }
        """
    ),

    ShaderScene(
        "swarm", "Swarm", ShaderFamily.MOTION,
        "Points orbiting a shifting attractor.",
        listOf(
            param("count", "Points", 8f, 64f, 28f),
            param("spread", "Spread", .2f, 2f, .9f),
            param("speed", "Speed", 0f, 2f, .7f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            float2 attract = float2(sin(t * 0.31), cos(t * 0.27)) * 0.35;
            for (int i = 0; i < 64; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float seed = hash(float2(fi, 7.0));
                float ang = t * uP2 * (0.5 + seed) + seed * 6.2831853;
                float rad = (0.15 + seed * 0.85) * uP1;
                float2 pos = attract + float2(cos(ang), sin(ang * 1.3)) * rad;
                float d = length(p - pos);
                float dot0 = exp(-d * 60.0);
                c += mix(uC2, uC3, seed) * dot0;
                c += uC1 * exp(-d * 12.0) * 0.05;
            }
            return c;
        }
        """
    ),

    // ---------------------------------------------------------------- Circuits

    ShaderScene(
        "traces", "Circuit Traces", ShaderFamily.CIRCUIT,
        "Board routing with charge running the lines.",
        listOf(
            param("density", "Trace density", 4f, 30f, 12f),
            param("pulse", "Pulse speed", 0f, 3f, 1.2f),
            param("glow", "Glow", 0f, 2f, .9f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float2 q = p * uP0;
            float2 id2 = floor(q);
            float2 f = fract(q) - 0.5;
            float r = hash(id2);
            float bend = r < 0.5 ? 1.0 : -1.0;
            float d = r < 0.25 ? abs(f.x) : (r < 0.5 ? abs(f.y) : abs(length(f - float2(0.5 * bend, 0.5)) - 0.5));
            float trace = smoothstep(0.06, 0.0, d);
            float flow = fract(hash(id2 + 3.0) - t * uP1 * 0.3);
            float charge = exp(-abs(fract(f.x + f.y + flow) - 0.5) * 14.0);
            float pad = smoothstep(0.12, 0.09, length(f)) * step(0.82, r);
            float3 c = mix(uC0, uC1, trace * 0.55);
            c = mix(c, uC2, trace * charge * uP2);
            c = mix(c, uC3, pad);
            return c;
        }
        """
    ),

    ShaderScene(
        "glitch", "Datamosh", ShaderFamily.CIRCUIT,
        "Displaced blocks and channel separation.",
        listOf(
            param("rate", "Glitch rate", 0f, 3f, 1f),
            param("block", "Block size", 4f, 60f, 20f),
            param("shift", "Channel shift", 0f, .1f, .02f)
        ),
        MONOS,
        """
        float3 scene(float2 p, float2 uv, float t){
            float bs = uP1;
            float band = floor(uv.y * bs);
            float tick = floor(t * uP0 * 4.0);
            float jump = hash(float2(band, tick));
            float off = (jump - 0.5) * step(0.72, jump) * 0.35;
            float2 q = float2(fract(uv.x + off), uv.y);
            float2 g = q * float2(bs * 1.6, bs);
            float mask = glyph(floor(hash(floor(g) + tick) * 64.0), fract(g));
            float base = fbm(q * 3.0 + tick * 0.1);
            float id = floor(hash(floor(g) + tick) * 64.0);
            float mr = glyph(id, fract(g + float2(uP2 * 6.0, 0.0)));
            float mb = glyph(id, fract(g - float2(uP2 * 6.0, 0.0)));
            float3 c = mix(uC0, uC1, base * 0.6);
            c.r = mix(c.r, uC2.r, mr);
            c.g = mix(c.g, uC2.g, mask);
            c.b = mix(c.b, uC2.b, mb);
            c = mix(c, uC3, mask * step(0.72, jump) * 0.5);
            return c;
        }
        """
    ),

    ShaderScene(
        "scope", "Signal Scope", ShaderFamily.CIRCUIT,
        "Oscilloscope traces with phosphor persistence.",
        listOf(
            param("freq", "Frequency", 1f, 20f, 6f),
            param("sweep", "Sweep", 0f, 3f, 1f),
            param("persist", "Persistence", .01f, .3f, .08f)
        ),
        LOFI,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 c = uC0;
            float grid = 0.0;
            grid += smoothstep(0.02, 0.0, abs(fract(uv.x * 8.0) - 0.5) * 0.25);
            grid += smoothstep(0.02, 0.0, abs(fract(uv.y * 6.0) - 0.5) * 0.25);
            c = mix(c, uC1, clamp(grid, 0.0, 1.0) * 0.35);
            for (int i = 0; i < 3; i++){
                float fi = float(i);
                float ph = t * uP1 * (1.0 + fi * 0.3) + fi * 2.1;
                float y = sin(p.x * uP0 * (1.0 + fi * 0.35) + ph) * 0.3;
                y *= 0.6 + 0.4 * sin(p.x * 2.0 + ph * 0.5);
                float d = abs(p.y - y);
                float trace = smoothstep(uP2, 0.0, d);
                c += mix(uC2, uC3, fi / 2.0) * trace * 0.8;
            }
            return c;
        }
        """
    ),

    // ---------------------------------------------------------------- Depth
    //
    // These raymarch a lit surface rather than tint a pattern: distance fields, surface normals,
    // reflection and refraction, volumetric accumulation. They cost more per pixel than the flat
    // scenes, so each keeps its step count tight and handles its own dispersion.

    ShaderScene(
        "liquid_metal", "Liquid Metal", ShaderFamily.DEPTH,
        "Molten blobs merging under a polished skin.",
        listOf(
            param("size", "Mass", .18f, .42f, .30f),
            param("melt", "Melt", .05f, .55f, .28f),
            param("split", "Dispersion", 0f, 2f, 1f),
            param("tint", "Tint", 0f, 1f, .45f)
        ),
        COOL,
        """
        float mapBody(float3 q, float t){
            float d = 100000.0;
            for (int i = 0; i < 5; i++){
                float fi = float(i);
                float3 c = float3(
                    sin(t * (0.60 + fi * 0.17) + fi * 2.1) * 0.60,
                    cos(t * (0.51 + fi * 0.21) + fi * 1.3) * 0.44,
                    sin(t * (0.43 + fi * 0.13) + fi * 3.7) * 0.46);
                d = smin(d, sdSphere(q - c, uP0 * (0.8 + 0.25 * sin(t * 0.7 + fi))), uP1);
            }
            // A slow central mass, so the blobs read as one body pulling itself apart and back
            // together rather than as five spheres that happen to overlap.
            return smin(d, sdSphere(q, uP0 * 1.25), uP1 * 1.5);
        }

        float3 normBody(float3 q, float t){
            float2 e = float2(0.0018, 0.0);
            return normalize(float3(
                mapBody(q + e.xyy, t) - mapBody(q - e.xyy, t),
                mapBody(q + e.yxy, t) - mapBody(q - e.yxy, t),
                mapBody(q + e.yyx, t) - mapBody(q - e.yyx, t)));
        }

        /**
         * What the metal has to reflect. A polished surface with nothing around it reads as flat
         * paint, so the environment carries a key light, a horizon and banding for highlights to
         * slide across as the body turns.
         */
        float3 env(float3 rd){
            float h = rd.y * 0.5 + 0.5;
            float3 sky = mix(uC0, uC1, h);
            sky = mix(sky, uC2, pow(h, 4.0) * 0.75);
            float band = smoothstep(0.55, 1.0, sin(rd.y * 9.0 + rd.x * 2.2) * 0.5 + 0.5);
            sky += uC3 * band * 0.10;
            sky += uC3 * pow(max(dot(rd, normalize(float3(0.40, 0.78, -0.48))), 0.0), 30.0) * 1.5;
            return sky;
        }

        float3 scene(float2 p, float2 uv, float t){
            float spin = t * 0.20;
            float3 ro = rotY(float3(0.0, 0.10, 2.35), spin);
            float3 rd = rotY(normalize(float3(p, -1.45)), spin);

            float travelled = 0.0;
            float hit = 0.0;
            for (int i = 0; i < 56; i++){
                float d = mapBody(ro + rd * travelled, t);
                if (d < 0.0016) { hit = 1.0; break; }
                // Understepping keeps the smooth-minimum surface from being overshot where two
                // blobs are blending and the field is no longer a true distance.
                travelled += d * 0.82;
                if (travelled > 5.0) break;
            }

            float3 c;
            if (hit < 0.5) {
                c = env(rd) * 0.42;
            } else {
                float3 q = ro + rd * travelled;
                float3 n = normBody(q, t);
                float f = fresnel(n, rd, 3.0);

                // Each channel leaves along a slightly different normal, so the rim of the body
                // splits into colour exactly where a real polished surface separates wavelengths.
                float spread = uAberration * uP2 * 0.05;
                c.r = env(reflect(rd, normalize(n + spread))).r;
                c.g = env(reflect(rd, n)).g;
                c.b = env(reflect(rd, normalize(n - spread))).b;

                c *= mix(uC1, uC3, uP3) * 1.25;
                c += uC3 * pow(max(dot(reflect(rd, n), normalize(float3(0.40, 0.78, -0.48))), 0.0), 56.0) * 1.7;
                c *= 0.30 + 0.90 * f + 0.45 * max(dot(n, normalize(float3(0.3, 0.85, 0.4))), 0.0);
            }
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "event_horizon", "Event Horizon", ShaderFamily.DEPTH,
        "Light bent around a black hole, disk and all.",
        listOf(
            param("mass", "Mass", .35f, .85f, .55f),
            param("reach", "Disk reach", 2f, 6f, 3.6f),
            param("swirl", "Swirl", 0f, 3f, 1.2f),
            param("tilt", "Tilt", 0f, 1f, .42f)
        ),
        VIOLETS,
        """
        float3 scene(float2 p, float2 uv, float t){
            float spin = t * 0.06;
            float3 pos = rotY(float3(0.0, 0.35 + uP3 * 0.85, -4.3), spin);
            float3 vel = rotY(normalize(float3(p * 1.2, 1.0)), spin);

            // Conserved angular momentum of the photon. The deflection term below is the standard
            // null-geodesic approximation, which is what actually bends the disk up over the top
            // of the hole — a radial pull alone only smears the image inward.
            float3 h = cross(pos, vel);
            float h2 = dot(h, h);
            float rs = uP0;

            float3 c = float3(0.0);
            float escaped = 1.0;

            for (int i = 0; i < 96; i++){
                float3 prev = pos;
                pos += vel * 0.062;
                float rr2 = max(dot(pos, pos), 0.04);
                vel += -1.5 * h2 * pos / (rr2 * rr2 * sqrt(rr2)) * 0.062;

                float r = length(pos);
                if (r < rs) { escaped = 0.0; break; }
                if (r > 16.0) break;

                // Emission is gathered where the ray crosses the equatorial plane, interpolated to
                // the exact crossing so the disk stays a thin sheet instead of a fuzzy slab.
                if (prev.y * pos.y < 0.0){
                    float3 x = mix(prev, pos, prev.y / (prev.y - pos.y));
                    float rad = length(x.xz);
                    float band = smoothstep(rs * 1.9, rs * 2.5, rad) * smoothstep(uP1, uP1 * 0.68, rad);
                    if (band > 0.001){
                        float a = atan(x.z, x.x);
                        // Inner material orbits faster, which shears the cloud into spiral arms.
                        float swirl = a + t * uP2 * 1.6 / max(rad, 0.35);
                        float dens = pow(clamp(fbm(float2(swirl * 1.7, rad * 2.6 - t * 0.35)), 0.0, 1.0), 1.7);
                        float heat = smoothstep(uP1, rs * 2.0, rad);
                        // The side of the disk rotating toward us is beamed and blue-shifted; the
                        // receding side is dimmed. This asymmetry is the whole look.
                        float doppler = 1.0 + 1.15 * sin(swirl);
                        c += pal(0.20 + heat * 0.78) * (0.45 + heat * 2.4) * band * dens * max(doppler, 0.05) * 0.15;
                    }
                }
            }

            if (escaped > 0.5){
                float3 d = normalize(vel);
                float2 sp = float2(atan(d.z, d.x) * 0.55, d.y * 1.7);
                c += uC3 * pow(hash(floor(sp * 110.0)), 44.0) * 2.2;
                c += mix(uC0, uC1, d.y * 0.5 + 0.5) * 0.30;
            }

            // The photon ring: orbits that graze the hole pile light into a thin bright circle
            // just outside the shadow.
            float ring = length(p) / max(rs * 0.62, 0.05);
            c += uC3 * smoothstep(0.14, 0.0, abs(ring - 1.0)) * 0.55;
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "gemstone", "Cut Stone", ShaderFamily.DEPTH,
        "A brilliant cut throwing prismatic fire.",
        listOf(
            param("size", "Size", .35f, .8f, .58f),
            param("spin", "Spin", 0f, 1.2f, .35f),
            param("fire", "Fire", 0f, 2f, 1f),
            param("facets", "Facets", 2f, 9f, 5f)
        ),
        LOFI,
        """
        /** Octahedron cut by a box and a girdle plane: flat facets, brilliant silhouette. */
        float mapGem(float3 q){
            float o = (abs(q.x) + abs(q.y) + abs(q.z) - uP0) * 0.5773;
            float g = max(o, sdBox3(q, float3(uP0 * 0.66)));
            return max(g, abs(q.y) - uP0 * 0.55);
        }

        float3 normGem(float3 q){
            float2 e = float2(0.0012, 0.0);
            return normalize(float3(
                mapGem(q + e.xyy) - mapGem(q - e.xyy),
                mapGem(q + e.yxy) - mapGem(q - e.yxy),
                mapGem(q + e.yyx) - mapGem(q - e.yyx)));
        }

        /**
         * Discrete bright sources, not a gradient. Facets sparkle because they sweep across small
         * intense lights; a smooth environment makes even a perfect cut look like frosted plastic.
         */
        float3 gemEnv(float3 rd){
            float3 sky = mix(uC0, uC1, rd.y * 0.5 + 0.5);
            float2 sp = floor(float2(atan(rd.z, rd.x) * 1.7, rd.y * 3.2) * uP3 + 0.5);
            sky += uC3 * pow(hash(sp), 24.0) * 7.0;
            sky += uC2 * pow(max(rd.y, 0.0), 5.0) * 0.55;
            return sky;
        }

        /** One wavelength through the stone: in one facet, across the interior, out another. */
        float3 wavelength(float3 q, float3 n, float3 rd, float ior){
            float3 inside = refract(rd, n, 1.0 / ior);
            float march = 0.02;
            for (int j = 0; j < 28; j++){
                float d = -mapGem(q + inside * march);
                if (d < 0.0015) break;
                march += max(d, 0.005);
                if (march > 4.0) break;
            }
            float3 exitPoint = q + inside * march;
            float3 exitNormal = -normGem(exitPoint);
            float3 escape = refract(inside, exitNormal, ior);
            // Beyond the critical angle refract() gives nothing and the light stays trapped —
            // total internal reflection, which is precisely what makes a cut stone bright.
            if (dot(escape, escape) < 0.0001) escape = reflect(inside, exitNormal);
            return gemEnv(normalize(escape));
        }

        float3 scene(float2 p, float2 uv, float t){
            float a = t * uP1;
            float3 ro = rotX(rotY(float3(0.0, 0.0, 2.6), a), a * 0.55);
            float3 rd = rotX(rotY(normalize(float3(p, -1.6)), a), a * 0.55);

            float travelled = 0.0;
            float hit = 0.0;
            for (int i = 0; i < 46; i++){
                float d = mapGem(ro + rd * travelled);
                if (d < 0.0012) { hit = 1.0; break; }
                travelled += d;
                if (travelled > 6.0) break;
            }
            if (hit < 0.5) return gemEnv(rd) * 0.34;

            float3 q = ro + rd * travelled;
            float3 n = normGem(q);

            // Diamond's index runs from about 2.41 at red to 2.45 at violet. Spreading the three
            // samples around that is the dispersion — the fire is not a colour filter, it is red
            // and blue leaving the stone through different exit facets.
            float spread = 0.02 + uP2 * uAberration * 0.09;
            float3 c = float3(
                wavelength(q, n, rd, 2.42 - spread).r,
                wavelength(q, n, rd, 2.42).g,
                wavelength(q, n, rd, 2.42 + spread).b);

            float f = fresnel(n, rd, 5.0);
            c = mix(c, gemEnv(reflect(rd, n)), f * 0.75);
            c += uC3 * pow(max(dot(reflect(rd, n), normalize(float3(0.35, 0.85, -0.4))), 0.0), 90.0) * 2.2;
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "rain_glass", "Rain on Glass", ShaderFamily.DEPTH,
        "Beads on a window, lights fracturing behind.",
        listOf(
            param("scale", "Bead size", 2f, 9f, 4.5f),
            param("run", "Run-off", 0f, 2f, .9f),
            param("bend", "Refraction", 0f, 2f, 1f),
            param("lights", "Lights", 0f, 1f, .6f)
        ),
        LOFI,
        """
        /** Out-of-focus sources behind the glass. Bokeh needs points to bloom from. */
        float3 backdrop(float2 q, float t){
            float3 c = mix(uC0, uC1, clamp(q.y * 0.55 + 0.5, 0.0, 1.0)) * 0.55;
            for (int i = 0; i < 12; i++){
                float fi = float(i);
                float2 h = hash2(float2(fi, 3.0));
                float2 at = float2((h.x - 0.5) * 2.6, (h.y - 0.5) * 2.2);
                at.x += sin(t * 0.09 + fi) * 0.10;
                float d = length(q - at);
                float r = 0.045 + h.x * 0.085;
                float3 tint = mix(uC2, uC3, h.y);
                // A hard-edged disc plus a soft halo: a real defocused point is a flat circle of
                // confusion, not a gaussian, and the flat edge is what makes it read as bokeh.
                c += tint * smoothstep(r, r * 0.6, d) * (0.30 + h.y * 0.55) * uP3;
                c += tint * exp(-d * 7.5) * 0.13 * uP3;
            }
            return c;
        }

        /** Height of the water film: static beads at three scales, plus a few running trails. */
        float dropHeight(float2 q, float t){
            float h = 0.0;
            for (int i = 0; i < 3; i++){
                float fi = float(i);
                float sc = uP0 * (1.0 + fi * 1.65);
                float2 g = q * sc;
                float2 id = floor(g);
                float2 f = fract(g) - 0.5;
                float2 o = (hash2(id + fi * 31.0) - 0.5) * 0.66;
                float r = 0.16 + hash(id + fi * 7.0) * 0.22;
                h += smoothstep(1.0, 0.0, length(f - o) / r) * r * (1.2 - fi * 0.22);
            }

            // Trails. A bead that has broken loose runs down its own column and leaves a thinner
            // wet track above it, which is why rain on a window is streaked rather than dotted.
            float cols = uP0 * 0.7;
            float col = floor(q.x * cols);
            float seed = hash(float2(col, 11.0));
            float lane = abs(fract(q.x * cols) - 0.5);
            float head = 1.4 - fract(seed * 7.31 + t * uP1 * 0.13) * 2.8;
            float along = q.y - head;
            float track = along > 0.0 ? exp(-along * 2.6) : 0.0;
            float width = smoothstep(0.30, 0.06, lane);
            h += (smoothstep(0.09, 0.0, length(float2(lane * 1.4, along))) * 0.34 + track * width * 0.09)
                 * step(0.45, seed) * uP1;
            return h;
        }

        float3 scene(float2 p, float2 uv, float t){
            float e = 0.0035;
            float h = dropHeight(p, t);
            // Slope of the water surface. Where the film is thick and curved it acts as a lens.
            float2 slope = float2(dropHeight(p + float2(e, 0.0), t) - h,
                                  dropHeight(p + float2(0.0, e), t) - h) / e;

            float2 bend = -slope * uP2 * 0.055;
            // Thicker water bends more, and bends the short wavelengths hardest, so lights behind
            // a bead fringe blue on one side and red on the other.
            float split = uAberration * (0.18 + h * 1.6);
            float3 c = float3(
                backdrop(p + bend * (1.0 + split), t).r,
                backdrop(p + bend, t).g,
                backdrop(p + bend * (1.0 - split), t).b);

            // Dry glass is hazy and slightly dimmed; the wet beads are clear, so they punch.
            float wet = smoothstep(0.005, 0.055, h);
            c = mix(c * 0.62 + uC1 * 0.05, c * 1.30, wet);
            c += uC3 * pow(clamp(-slope.y * 0.06 + 0.5, 0.0, 1.0), 9.0) * wet * 0.42;
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "spotlight", "Velvet Spotlight", ShaderFamily.DEPTH,
        "A sweeping beam, thick with dust.",
        listOf(
            param("cone", "Cone", .82f, .99f, .93f),
            param("sweep", "Sweep", 0f, 2f, .7f),
            param("dust", "Dust", 0f, 2f, 1f),
            param("floorLift", "Floor", 0f, 1f, .55f)
        ),
        WARM,
        """
        float3 scene(float2 p, float2 uv, float t){
            float3 ro = float3(0.0, 0.12, 2.5);
            float3 rd = normalize(float3(p, -1.5));

            float sweep = sin(t * uP1 * 0.42);
            float3 lamp = float3(sweep * 1.05, 1.45, -0.25);
            float3 aim = normalize(float3(sweep * -0.30, -1.0, 0.18));

            float3 c = uC0 * 0.22;
            float beam = 0.0;
            float motes = 0.0;

            // Marching the beam rather than drawing a cone: the glow has to accumulate along the
            // ray, because that is why a spotlight is brightest where you look furthest through it.
            for (int i = 0; i < 40; i++){
                float3 q = ro + rd * (0.05 + float(i) * 0.075);
                float3 toLamp = q - lamp;
                float dist = length(toLamp);
                float inside = smoothstep(uP0, uP0 + 0.085, dot(toLamp / dist, aim));
                float fall = 1.0 / (1.0 + dist * dist * 1.5);
                beam += inside * fall * 0.062;
                float m = smoothstep(0.56, 0.95, fbm(q.xy * 3.6 + float2(0.0, -t * 0.28) + q.z * 1.9));
                motes += inside * fall * m * 0.34;
            }

            // The pool where the beam lands, and the floor falling away from it.
            float ground = (-0.62 - ro.y) / min(rd.y, -0.0001);
            if (rd.y < 0.0 && ground > 0.0){
                float3 g = ro + rd * ground;
                float3 toLamp = g - lamp;
                float dist = length(toLamp);
                float pool = smoothstep(uP0, uP0 + 0.05, dot(toLamp / dist, aim)) / (1.0 + dist * dist);
                float grain = fbm(g.xz * 6.0) * 0.35 + 0.65;
                c += mix(uC1, uC2, 0.4) * pool * grain * uP3 * 1.5 / (1.0 + ground * 0.35);
            }

            // The lamp's edge fringes: the cone boundary is a hard optical edge, and hard edges
            // are where a lens shows colour.
            float fringe = uAberration * 0.03;
            c += float3(
                mix(uC1, uC2, 0.35).r * (beam * (1.0 + fringe)),
                mix(uC1, uC2, 0.5).g * beam,
                mix(uC1, uC2, 0.65).b * (beam * (1.0 - fringe)));
            c += uC3 * motes * uP2 * 0.5;
            return c;
        }
        """,
        dispersive = true
    ),

    ShaderScene(
        "laser_grid", "Laser Labyrinth", ShaderFamily.DEPTH,
        "Beams crossing in a smoky room.",
        listOf(
            param("count", "Beams", 3f, 9f, 7f),
            param("drift", "Drift", 0f, 2f, .8f),
            param("thickness", "Thickness", .002f, .04f, .012f),
            param("haze", "Haze", 0f, 2f, .9f)
        ),
        VIOLETS,
        """
        /**
         * Closest approach between the view ray and a beam, solved rather than sampled. Marching
         * a beam this thin skips straight past it at most step sizes; the analytic distance keeps
         * it a clean hairline no matter how far away it runs.
         */
        float beamDistance(float3 ro, float3 rd, float3 bo, float3 bd){
            float3 w = ro - bo;
            float b = dot(rd, bd);
            float d = dot(rd, w);
            float e = dot(bd, w);
            float den = 1.0 - b * b;
            if (den < 0.00001) return 1000.0;
            float s = (b * e - d) / den;
            if (s < 0.0) return 1000.0;
            return length(w + rd * s - bd * ((e - b * d) / den));
        }

        float3 scene(float2 p, float2 uv, float t){
            float3 ro = float3(0.0, 0.0, 2.6);
            float3 rd = normalize(float3(p, -1.55));

            float3 c = uC0 * 0.35;
            float haze = 0.0;

            for (int i = 0; i < 9; i++){
                float fi = float(i);
                if (fi >= uP0) break;
                float a = fi * 2.399 + t * uP1 * 0.23;
                float3 origin = float3(sin(a * 1.3) * 0.75, cos(a * 0.9) * 0.55, sin(a * 0.7) * 0.5);
                float3 dir = normalize(float3(sin(a * 2.1), cos(a * 1.7) * 0.6, cos(a * 0.5) * 0.9));
                float d = beamDistance(ro, rd, origin, dir);
                float core = uP2 / (uP2 + d);
                // Core to the fourth reads as the beam itself; the plain falloff is the smoke it
                // is lighting up, which is what makes crossings flare instead of simply adding.
                float3 tint = pal(fract(fi * 0.191 + 0.15));
                c += tint * pow(core, 4.0) * 1.9;
                haze += core * 0.35;
            }

            c += mix(uC2, uC3, 0.4) * haze * uP3 * 0.35;
            c += uC3 * pow(haze * 0.5, 3.0) * 0.6;
            return c;
        }
        """
    ),

    ShaderScene(
        "tesseract", "Tesseract", ShaderFamily.DEPTH,
        "A hypercube turning through its fourth axis.",
        listOf(
            param("spin", "Spin", 0f, 1.5f, .45f),
            param("depth", "Depth fade", 0f, 1f, .6f),
            param("width", "Line width", .002f, .02f, .006f),
            param("glow", "Glow", 0f, 2f, .9f)
        ),
        COOL,
        """
        /** Vertex i of the hypercube, read out of the index's four bits without bitwise ops. */
        float4 corner(float i){
            return float4(
                mod(floor(i),        2.0) * 2.0 - 1.0,
                mod(floor(i / 2.0),  2.0) * 2.0 - 1.0,
                mod(floor(i / 4.0),  2.0) * 2.0 - 1.0,
                mod(floor(i / 8.0),  2.0) * 2.0 - 1.0);
        }

        /**
         * Two perspective divides, not one. The 4D point is projected to 3D by its w, then to 2D
         * by its z, and it is the first divide that makes the inner cube swell and pass through
         * the outer one instead of the whole thing simply rotating.
         */
        float3 project(float4 v, float a){
            float ca = cos(a), sa = sin(a);
            float4 r = float4(v.x * ca - v.w * sa, v.y, v.z, v.x * sa + v.w * ca);
            float cb = cos(a * 0.63), sb = sin(a * 0.63);
            r = float4(r.x, r.y * cb - r.z * sb, r.y * sb + r.z * cb, r.w);
            float3 p3 = rotY(float3(r.x, r.y, r.z) / (2.6 - r.w), a * 0.7);
            float k = 1.0 / (2.9 - p3.z);
            return float3(p3.x * k * 1.9, p3.y * k * 1.9, p3.z);
        }

        float segment(float2 p, float2 a, float2 b){
            float2 pa = p - a, ba = b - a;
            return length(pa - ba * clamp(dot(pa, ba) / max(dot(ba, ba), 0.000001), 0.0, 1.0));
        }

        float3 scene(float2 p, float2 uv, float t){
            float a = t * uP0;
            float3 c = uC0 * 0.5;

            for (int i = 0; i < 16; i++){
                float fi = float(i);
                float4 v = corner(fi);
                float3 pa = project(v, a);
                for (int k = 0; k < 4; k++){
                    // Only step a bit that is currently low, so every edge is drawn once.
                    float bit = k == 0 ? v.x : (k == 1 ? v.y : (k == 2 ? v.z : v.w));
                    if (bit < 0.0) {
                        float4 w = v;
                        if (k == 0) w.x = 1.0; else if (k == 1) w.y = 1.0;
                        else if (k == 2) w.z = 1.0; else w.w = 1.0;
                        float3 pb = project(w, a);

                        float d = segment(p, pa.xy, pb.xy);
                        float depth = clamp((pa.z + pb.z) * 0.5 * 0.6 + 0.5, 0.0, 1.0);
                        // Far edges are thinner and dimmer. Without that the projection collapses
                        // into a flat tangle of identical lines.
                        float wide = uP2 * mix(0.55, 1.35, depth);
                        float lit = mix(0.25, 1.0, pow(depth, mix(0.2, 2.2, uP1)));
                        float3 tint = mix(uC1, uC2, depth);
                        // Each channel is resolved at a slightly different line width, so edges
                        // fringe the way a lens makes them without projecting the whole solid
                        // three times over — eighty projections a pixel is already the budget.
                        float3 split = 1.0 + float3(uAberration * 0.16, 0.0, -uAberration * 0.16);
                        c += tint * smoothstep(wide, 0.0, d) * lit * split;
                        c += mix(tint, uC3, 0.5) * (wide * 2.4 / (wide * 2.4 + d)) * lit * uP3 * 0.09;
                    }
                }
            }
            return c;
        }
        """,
        dispersive = true
    )
)

fun sceneById(id: String): ShaderScene = shaderScenes.firstOrNull { it.id == id } ?: shaderScenes.first()

fun scenesInFamily(family: ShaderFamily): List<ShaderScene> = shaderScenes.filter { it.family == family }
