package com.minimate.touchpad.model

private fun p(name: String, a: Long, b: Long, c: Long, d: Long) = SceneColorway(name, listOf(a, b, c, d))

private fun sc(
    theme: AbstractShaderTheme,
    index: Int,
    label: String,
    reaction: TouchReaction,
    sourceId: String,
    sourceUrl: String,
    sourceLicense: String,
    vararg palettes: SceneColorway,
    custom: Boolean = true
) = AbstractSubtheme(
    theme, index, label, reaction, palettes.toList(),
    "$label is a cover-screen adaptation of $sourceId; the source content remains the composition instead of being replaced by a generated look-alike.",
    label, "Source-native foreground, subject, and background depth", "Scene-specific camera and material motion",
    "Every retained touch distorts or excites the scene without cancelling older contacts", "Source-native material", custom,
    sourceId, sourceUrl, sourceLicense
)

private const val SPACE_CODE = "https://github.com/Eluvade/cosmos"
private const val OCEAN_CODE = "https://github.com/Nugget8/Three.js-Ocean-Scene"
private const val GERSTNER_CODE = "https://github.com/CaffeineViking/osgw"
private const val GAMES_CODE = "https://github.com/forinda/canvas-games"
private const val ABSTRACT = "https://github.com/tuxalin/procedural-tileable-shaders"
private const val MATRIX = "https://github.com/Rezmason/matrix"
private const val MIT = "MIT"

internal val authoredThemeScenes = listOf(
    sc(AbstractShaderTheme.COSMIC,0,"Cepheus Starfield",TouchReaction.WARP,"cosmos common star field",SPACE_CODE,MIT,p("Survey Natural",0xFF01040A,0xFF111A30,0xFF8094B5,0xFFF5F1E7),p("Blue Plate",0xFF01030A,0xFF0B244B,0xFF5C9CE6,0xFFEAF7FF),p("Red Plate",0xFF080204,0xFF351014,0xFFD35C45,0xFFFFF0D8)),
    sc(AbstractShaderTheme.COSMIC,1,"M83 Grand Spiral",TouchReaction.WARP,"cosmos galaxy.glsl",SPACE_CODE,MIT,p("Hubble Natural",0xFF020309,0xFF20294A,0xFF8BA6D5,0xFFFFD9B0),p("Ultraviolet",0xFF03020B,0xFF241754,0xFF765DE0,0xFFF3D9FF),p("Hydrogen Alpha",0xFF080203,0xFF4A1018,0xFFE84B5F,0xFFFFE7D6)),
    sc(AbstractShaderTheme.COSMIC,2,"Relativistic Black Hole",TouchReaction.WARP,"cosmos black-hole renderer",SPACE_CODE,MIT,p("Simulation Natural",0xFF000000,0xFF35130A,0xFFF27A24,0xFFFFF0C8),p("X-Ray",0xFF000006,0xFF092A58,0xFF42A5FF,0xFFFFFFFF),p("Radio",0xFF010100,0xFF4A3200,0xFFFFC43D,0xFFFFF8DF)),
    sc(AbstractShaderTheme.COSMIC,3,"Dumbbell Nebula",TouchReaction.RIPPLE,"cosmos nebula renderer",SPACE_CODE,MIT,p("Visible Spectrum",0xFF010307,0xFF173140,0xFF55C7C2,0xFFECA4B4),p("Oxygen",0xFF010608,0xFF053E4D,0xFF28D3C1,0xFFE7FFFF),p("Sulfur",0xFF070203,0xFF4C1514,0xFFF07447,0xFFFFE6B8),p("Deep Violet",0xFF030107,0xFF30134D,0xFFA467E8,0xFFFCE8FF)),
    sc(AbstractShaderTheme.COSMIC,4,"Saturn Ring Plane",TouchReaction.TRAIL,"cosmos gas-giant-ringed.glsl",SPACE_CODE,MIT,p("Cassini Natural",0xFF050505,0xFF3A3934,0xFFBEB69F,0xFFFFF9E8),p("Cold Rings",0xFF030509,0xFF263646,0xFF9AB6C9,0xFFF4FBFF),p("Sepia Survey",0xFF080604,0xFF4B3C26,0xFFC39B62,0xFFFFE8BD)),
    sc(AbstractShaderTheme.COSMIC,5,"Earth from Orbit",TouchReaction.RIPPLE,"cosmos base-planet + land-mass + clouds",SPACE_CODE,MIT,p("Blue Marble",0xFF01050A,0xFF073463,0xFF2A9CD6,0xFFFFD38E),p("Nightside",0xFF000207,0xFF0A1836,0xFF1C6FA4,0xFFF4B45F),p("Infrared Earth",0xFF050206,0xFF2A174A,0xFFB34A86,0xFFFFD77A)),
    sc(AbstractShaderTheme.COSMIC,6,"Earthrise",TouchReaction.TRAIL,"cosmos terrain-dry.glsl",SPACE_CODE,MIT,p("Apollo Natural",0xFF020204,0xFF26272B,0xFF8D8E92,0xFFE8D6B4),p("Silver Gelatin",0xFF020202,0xFF333333,0xFFA7A7A7,0xFFF4F4F4),p("Earthshine",0xFF010309,0xFF242D40,0xFF7186A4,0xFFE5D6C2)),
    sc(AbstractShaderTheme.COSMIC,7,"Jovian Aurora",TouchReaction.PULSE,"cosmos atmosphere.glsl",SPACE_CODE,MIT,p("Hubble Composite",0xFF010308,0xFF1B3554,0xFF4FD8E6,0xFFE9FFFF),p("Far UV",0xFF02020A,0xFF26206A,0xFF8A70FF,0xFFF4F0FF),p("Cyan Emission",0xFF000608,0xFF00404B,0xFF00D9CC,0xFFFFFFFF)),
    sc(AbstractShaderTheme.COSMIC,8,"W49B Supernova",TouchReaction.BURST,"cosmos star + flow-layer",SPACE_CODE,MIT,p("Chandra Composite",0xFF020208,0xFF38235A,0xFFE15F54,0xFFFFD878),p("X-Ray Blue",0xFF01040B,0xFF113D71,0xFF56AFFF,0xFFE9FAFF),p("Iron Map",0xFF080201,0xFF511506,0xFFF24A1C,0xFFFFE39A)),
    sc(AbstractShaderTheme.COSMIC,9,"Station Silhouette",TouchReaction.TRAIL,"cosmos common + canvas-games pixel-art",SPACE_CODE,MIT,p("Orbital Natural",0xFF010309,0xFF14243B,0xFF7D93AC,0xFFFFF0D5),p("Blueprint",0xFF01040A,0xFF0A315B,0xFF5EB6E8,0xFFF0FBFF),p("Solar Gold",0xFF050301,0xFF49300E,0xFFD99B32,0xFFFFEDBD)),

    sc(AbstractShaderTheme.PRISMATIC,0,"Cellular Quartz",TouchReaction.WARP,"cellularNoise.glsl",ABSTRACT,MIT,p("Rock Crystal",0xFF05070A,0xFF233146,0xFF8FC9D8,0xFFF5FFFF),p("Amethyst",0xFF09050F,0xFF3A1A55,0xFFAE70E8,0xFFFFEAFE),p("Citrine",0xFF0A0702,0xFF4E3610,0xFFE3A634,0xFFFFF0C2)),
    sc(AbstractShaderTheme.PRISMATIC,1,"Voronoi Cathedral",TouchReaction.RIPPLE,"voronoi.glsl",ABSTRACT,MIT,p("Stained Glass",0xFF04040A,0xFF251755,0xFF36B7D4,0xFFFF5F85),p("Rose Window",0xFF080305,0xFF53172E,0xFFE1507A,0xFFFFD9A3),p("Night Nave",0xFF010509,0xFF0A3141,0xFF2BAFA7,0xFFD9FFF7)),
    sc(AbstractShaderTheme.PRISMATIC,2,"Perlin Silk",TouchReaction.TRAIL,"perlinNoise.glsl",ABSTRACT,MIT,p("Peacock",0xFF03070A,0xFF0E4350,0xFF1CC1B4,0xFFE8BD62),p("Mulberry",0xFF080307,0xFF481B3D,0xFFD65E99,0xFFFFDBBA),p("Ink",0xFF020305,0xFF242A35,0xFF7D8798,0xFFF4F6FA)),
    sc(AbstractShaderTheme.PRISMATIC,3,"Hexagonal Relief",TouchReaction.PULSE,"hexagons.glsl",ABSTRACT,MIT,p("Basalt Gold",0xFF040403,0xFF292720,0xFFA98746,0xFFFFE3A0),p("Ceramic",0xFF05080A,0xFF26404B,0xFF88B8C5,0xFFF2FFFF),p("Lacquer",0xFF080203,0xFF481018,0xFFCC3242,0xFFFFD5D2)),
    sc(AbstractShaderTheme.PRISMATIC,4,"Warped Marble",TouchReaction.WARP,"warp.glsl",ABSTRACT,MIT,p("Carrara",0xFF101114,0xFF4C5360,0xFFBCC5CD,0xFFFFFFFF),p("Verde",0xFF03100C,0xFF174B39,0xFF73A684,0xFFEAF2D7),p("Rosso",0xFF100506,0xFF5B2021,0xFFC76B5B,0xFFFFE3CD)),
    sc(AbstractShaderTheme.PRISMATIC,5,"Fbm Topography",TouchReaction.RIPPLE,"fbm.glsl",ABSTRACT,MIT,p("Survey",0xFF030609,0xFF16354A,0xFF70A1B4,0xFFF0E5C6),p("Desert Map",0xFF090603,0xFF5B3D1D,0xFFD59B55,0xFFFFE0A5),p("Abyss Map",0xFF01070A,0xFF06364D,0xFF167BA3,0xFFBEEFFF)),
    sc(AbstractShaderTheme.PRISMATIC,6,"Tile Weave",TouchReaction.TRAIL,"patterns.glsl weave",ABSTRACT,MIT,p("Bauhaus",0xFF080808,0xFF2C3038,0xFFE7463B,0xFFF2C84B),p("Indigo Loom",0xFF03050B,0xFF152853,0xFF4B72C8,0xFFE8E2CA),p("Forest Loom",0xFF030805,0xFF193F28,0xFF6C9B55,0xFFF0DCB2)),
    sc(AbstractShaderTheme.PRISMATIC,7,"Metric Bloom",TouchReaction.BURST,"metric.glsl",ABSTRACT,MIT,p("Electric Garden",0xFF04030A,0xFF28165A,0xFF36D5C8,0xFFFF6BA8),p("Solar Petal",0xFF090402,0xFF50200C,0xFFFF8A2A,0xFFFFE45A),p("Ice Petal",0xFF01070A,0xFF123D5B,0xFF6ECFF1,0xFFF4FFFF)),
    sc(AbstractShaderTheme.PRISMATIC,8,"Gradient Moire",TouchReaction.PULSE,"gradientNoise.glsl",ABSTRACT,MIT,p("Op Art",0xFF020202,0xFF262626,0xFFBEBEBE,0xFFFFFFFF),p("Cobalt",0xFF02040B,0xFF102A69,0xFF416DE1,0xFFF0EFFF),p("Vermilion",0xFF090202,0xFF4C1010,0xFFE84835,0xFFFFE1C4)),
    sc(AbstractShaderTheme.PRISMATIC,9,"Lissajous Membrane",TouchReaction.BURST,"patterns.glsl layered wave membrane",ABSTRACT,MIT,p("Electric Orchid",0xFF030309,0xFF232058,0xFF34C7D8,0xFFFF658C),p("Pearl Current",0xFF040403,0xFF31302B,0xFFC3A95F,0xFFFFF2CD),p("Night Iris",0xFF030107,0xFF2E1552,0xFF9759E2,0xFFF6E9FF)),

    sc(AbstractShaderTheme.TECH,0,"Japanese Code Rain",TouchReaction.WARP,"rainPass + Japanese atlas",MATRIX,MIT,p("Operator Green",0xFF000300,0xFF002B0B,0xFF00C73B,0xFFE8FFE9),p("Reloaded Gold",0xFF040300,0xFF332600,0xFFDDAF22,0xFFFFF1B8),p("Resurrections Cyan",0xFF000304,0xFF00303A,0xFF00BBD0,0xFFE8FFFF)),
    sc(AbstractShaderTheme.TECH,1,"Binary Downpour",TouchReaction.RIPPLE,"rainPass + binary atlas",MATRIX,MIT,p("Machine Green",0xFF000300,0xFF042A0D,0xFF1BDA55,0xFFFFFFFF),p("Cold Boot",0xFF000307,0xFF082851,0xFF38A6FF,0xFFF3FAFF),p("Amber Terminal",0xFF050300,0xFF352000,0xFFFFA51E,0xFFFFF0C4)),
    sc(AbstractShaderTheme.TECH,2,"Hex Memory",TouchReaction.TRAIL,"rainPass + hexadecimal atlas",MATRIX,MIT,p("Diagnostic Cyan",0xFF000405,0xFF073039,0xFF22CEE0,0xFFF0FFFF),p("Memory Violet",0xFF020207,0xFF29215C,0xFF8D6BE7,0xFFF4EEFF),p("Fault Red",0xFF050101,0xFF3A0B0B,0xFFE33E37,0xFFFFE4D8)),
    sc(AbstractShaderTheme.TECH,3,"Paradise Matrix",TouchReaction.PULSE,"paradise configuration",MATRIX,MIT,p("Paradise",0xFF020609,0xFF174557,0xFF6ED6DD,0xFFFFF3C7),p("Rose Program",0xFF080306,0xFF4A1D3B,0xFFE676A6,0xFFFFF0D6),p("Lavender Program",0xFF040308,0xFF302651,0xFFA58AD8,0xFFFFFFFF)),
    sc(AbstractShaderTheme.TECH,4,"Nightmare Matrix",TouchReaction.BURST,"nightmare configuration",MATRIX,MIT,p("Nightmare",0xFF050000,0xFF2C0505,0xFFC51A1A,0xFFFFD0C0),p("Infrared",0xFF030000,0xFF351003,0xFFF05A18,0xFFFFE09A),p("Monochrome Alarm",0xFF030303,0xFF292929,0xFFD8D8D8,0xFFFFFFFF)),
    sc(AbstractShaderTheme.TECH,5,"Mirror Code Canyon",TouchReaction.WARP,"mirrorPass",MATRIX,MIT,p("Emerald Canyon",0xFF000301,0xFF06321B,0xFF22C66D,0xFFE8FFF2),p("Blue Canyon",0xFF000308,0xFF0A2C55,0xFF3B8DE1,0xFFE9F4FF),p("Magenta Canyon",0xFF050205,0xFF3B1538,0xFFD653BB,0xFFFFE8FA)),
    sc(AbstractShaderTheme.TECH,6,"Glyph Quilt",TouchReaction.RIPPLE,"quiltPass",MATRIX,MIT,p("Green Quilt",0xFF010402,0xFF153522,0xFF49B86D,0xFFEFFFF2),p("Circuit Blue",0xFF010408,0xFF18304D,0xFF5996CB,0xFFF2FAFF),p("Copper Trace",0xFF060302,0xFF402217,0xFFD27A49,0xFFFFE4C6)),
    sc(AbstractShaderTheme.TECH,7,"Signal Stripes",TouchReaction.TRAIL,"stripePass",MATRIX,MIT,p("Oscilloscope",0xFF000402,0xFF063A25,0xFF21E68D,0xFFE9FFF6),p("Vectorscope",0xFF000306,0xFF092B46,0xFF2DD0D5,0xFFF0FFFF),p("Phosphor Amber",0xFF050300,0xFF3D2604,0xFFF2A51F,0xFFFFF0BF)),
    sc(AbstractShaderTheme.TECH,8,"Bloom Processor",TouchReaction.PULSE,"bloomPass",MATRIX,MIT,p("White Hot",0xFF010302,0xFF12341E,0xFF51C97B,0xFFFFFFFF),p("Plasma Blue",0xFF010208,0xFF17295A,0xFF587AE8,0xFFFFFFFF),p("Hot Pink",0xFF050204,0xFF3A1732,0xFFE95EAE,0xFFFFFFFF)),
    sc(AbstractShaderTheme.TECH,9,"Intro Trace",TouchReaction.BURST,"rainPass.intro",MATRIX,MIT,p("Wake Up",0xFF000200,0xFF072810,0xFF13BE44,0xFFE8FFE9),p("System Blue",0xFF000306,0xFF0B2949,0xFF2C9BE0,0xFFF0FAFF),p("Root Amber",0xFF040200,0xFF332000,0xFFE89B1D,0xFFFFEDB3)),

    sc(AbstractShaderTheme.ARCADE,0,"Tetris Atelier",TouchReaction.TRAIL,"canvas-games tetris engine",GAMES_CODE,MIT,p("Tetromino Pop",0xFF101315,0xFF37413C,0xFFDD3C32,0xFFFFE76B),p("Game Boy Garden",0xFF05070B,0xFF182B3E,0xFF33BCE0,0xFFFF6A55),p("Monochrome Puzzle",0xFF090909,0xFF383838,0xFFBDBDBD,0xFFFFFFFF)),
    sc(AbstractShaderTheme.ARCADE,1,"Pocket Grand Prix",TouchReaction.BURST,"canvas-games racing engine",GAMES_CODE,MIT,p("Day Circuit",0xFF050617,0xFF202D65,0xFF39C7F0,0xFFFF5B7F),p("Night Circuit",0xFF020702,0xFF133C19,0xFF50D85D,0xFFFFF26A),p("Candy Kart",0xFF090316,0xFF3C1769,0xFFB65CDE,0xFFFFD65A)),
    sc(AbstractShaderTheme.ARCADE,2,"Five-a-Side",TouchReaction.TRAIL,"canvas-games basketball movement adapted to five-a-side",GAMES_CODE,MIT,p("Pitch Natural",0xFF092A16,0xFF1F6E39,0xFFED4545,0xFFFFFFFF),p("Blue League",0xFF06172B,0xFF174D8C,0xFF4DB8FF,0xFFFFD54B),p("Night League",0xFF03070A,0xFF183C35,0xFF55D6A0,0xFFFF7A66)),
    sc(AbstractShaderTheme.ARCADE,3,"Brick Breaker",TouchReaction.WARP,"canvas-games breakout engine",GAMES_CODE,MIT,p("Primary Bricks",0xFF050617,0xFF24365A,0xFF3BB7E8,0xFFFFD34E),p("Neon Candy",0xFF100711,0xFF5A2451,0xFFEF5FA8,0xFFFFD55C),p("Mono Cabinet",0xFF030303,0xFF292929,0xFFBDBDBD,0xFFFFFFFF)),
    sc(AbstractShaderTheme.ARCADE,4,"Pixel Invaders",TouchReaction.BURST,"canvas-games space-invaders engine",GAMES_CODE,MIT,p("Lunar Green",0xFF000300,0xFF06320C,0xFF00D63B,0xFFE8FFE9),p("Alien Violet",0xFF020208,0xFF291955,0xFF9E6BE8,0xFFF4EEFF),p("Amber Armada",0xFF050300,0xFF3D2500,0xFFEAA018,0xFFFFF0B8)),
    sc(AbstractShaderTheme.ARCADE,5,"Maze Munch",TouchReaction.PULSE,"canvas-games pacman engine",GAMES_CODE,MIT,p("Maze Classic",0xFF04110C,0xFF174A36,0xFFC9433D,0xFFF3E4C1),p("Pastel Ghosts",0xFF04050A,0xFF222A48,0xFF8B65C5,0xFFFFD76A),p("Midnight Maze",0xFF171411,0xFF5A4C3D,0xFFC49A60,0xFFFFF5DF)),
    sc(AbstractShaderTheme.ARCADE,6,"Frogger Crossing",TouchReaction.TRAIL,"canvas-games frogger engine",GAMES_CODE,MIT,p("River Day",0xFF0A0D10,0xFF354653,0xFF4CB0D8,0xFFFFC857),p("Night Crossing",0xFF050A04,0xFF263E19,0xFF83D747,0xFFFFEC70),p("Autumn Traffic",0xFF0C0610,0xFF4F255A,0xFFE269BA,0xFFFFD76A)),
    sc(AbstractShaderTheme.ARCADE,7,"Garden Snake",TouchReaction.BURST,"canvas-games snake engine",GAMES_CODE,MIT,p("Garden Apple",0xFF06080E,0xFF24365A,0xFF3BB7E8,0xFFFFD34E),p("Nokia Garden",0xFF100711,0xFF5A2451,0xFFEF5FA8,0xFFFFD55C),p("Neon Serpent",0xFF030303,0xFF292929,0xFFBDBDBD,0xFFFFFFFF)),
    sc(AbstractShaderTheme.ARCADE,8,"Pinball Parlor",TouchReaction.TRAIL,"canvas-games gravity-ball and physics-puzzle engines",GAMES_CODE,MIT,p("Chrome Table",0xFF14243C,0xFF477AB0,0xFF67C36D,0xFFFFE08A),p("Candy Table",0xFF1A1022,0xFF6D3858,0xFFE57A62,0xFFFFD597),p("Space Table",0xFF040713,0xFF162D5A,0xFF4B77C8,0xFFE8F0FF)),
    sc(AbstractShaderTheme.ARCADE,9,"Platform Run",TouchReaction.RIPPLE,"canvas-games platformer engine",GAMES_CODE,MIT,p("PICO Natural",0xFF000000,0xFF1D2B53,0xFF29ADFF,0xFFFFEC27),p("Forest Cart",0xFF050B08,0xFF1B4A2A,0xFF6ABE30,0xFFF3E9B8),p("Violet Cart",0xFF0B0614,0xFF3F2468,0xFFB55088,0xFFFFD866)),

    sc(AbstractShaderTheme.OCEANIC,0,"Cape Shoreline",TouchReaction.RIPPLE,"Ocean Scene shoreline refraction adaptation",OCEAN_CODE,MIT,p("Coast Natural",0xFF082633,0xFF087D9D,0xFF73D3D0,0xFFF2D6A0),p("Atlantic",0xFF061D2E,0xFF0A5679,0xFF5BA3B5,0xFFE8D4AD),p("Golden Coast",0xFF13230D,0xFF166F65,0xFF7BC4A4,0xFFFFD78B)),
    sc(AbstractShaderTheme.OCEANIC,1,"Gerstner Horizon",TouchReaction.WARP,"osgw four-wave Gerstner sum",GERSTNER_CODE,MIT,p("Pacific Natural",0xFF041E2D,0xFF075F7C,0xFF46A9BD,0xFFD9F4ED),p("Deep Cobalt",0xFF031225,0xFF143E7B,0xFF397CD1,0xFFEAF3FF),p("Green Current",0xFF041D20,0xFF0C5E5A,0xFF51B29C,0xFFE6F3C9)),
    sc(AbstractShaderTheme.OCEANIC,2,"Great Barrier Reef",TouchReaction.BURST,"Ocean Scene caustic and volume model",OCEAN_CODE,MIT,p("Reef Natural",0xFF062635,0xFF087E8E,0xFF59C8B2,0xFFF0D19A),p("Coral Survey",0xFF08222E,0xFF126A78,0xFFE77868,0xFFFFD97F),p("Deep Dive",0xFF031524,0xFF0A4562,0xFF2787A0,0xFFE9F4DA)),
    sc(AbstractShaderTheme.OCEANIC,3,"Living Tidepool",TouchReaction.RIPPLE,"Ocean Scene refraction plus persistent ripple field",OCEAN_CODE,MIT,p("Basalt Pool",0xFF062A38,0xFF0794A8,0xFF7FE1D4,0xFFF5E5B8),p("Granite Coast",0xFF05232D,0xFF087484,0xFF6CC2BB,0xFFF2EDD8),p("Moon Pool",0xFF032F35,0xFF009C9A,0xFF62D6C0,0xFFFFE0A0)),
    sc(AbstractShaderTheme.OCEANIC,4,"Palm Beach Afternoon",TouchReaction.WARP,"Ocean Scene sky-water-light model plus coded palm scene",OCEAN_CODE,MIT,p("High Noon",0xFF062638,0xFF087D98,0xFF63CDBD,0xFFEAD294),p("Golden Hour",0xFF062B30,0xFF078C7E,0xFF68D2AF,0xFFF0D6A2),p("Moon Beach",0xFF051B31,0xFF0D5F8B,0xFF55A6C7,0xFFEADFC4)),
    sc(AbstractShaderTheme.OCEANIC,5,"Bioluminescent Breaker",TouchReaction.TRAIL,"osgw wave crest and foam model",OCEAN_CODE,MIT,p("Blue Plankton",0xFF010B18,0xFF063B5B,0xFF00D9CA,0xFFD8FFFF),p("Violet Bloom",0xFF040316,0xFF1F1754,0xFF7C4BD8,0xFFF1DEFF),p("Green Spark",0xFF021214,0xFF094C47,0xFF09B65C,0xFFCFFFF0)),
    sc(AbstractShaderTheme.OCEANIC,6,"Kelp Cathedral",TouchReaction.TRAIL,"Ocean Scene underwater absorption and current model",OCEAN_CODE,MIT,p("Kelp Natural",0xFF061A1C,0xFF134B3E,0xFF5C8E56,0xFFC9E1C6),p("Emerald Current",0xFF041B16,0xFF0E5A42,0xFF58B276,0xFFDDF3D2),p("Blue Forest",0xFF051521,0xFF123F5B,0xFF4D8392,0xFFCEE9DF)),
    sc(AbstractShaderTheme.OCEANIC,7,"Windwritten Dunes",TouchReaction.BURST,"osgw wind spectrum adapted to dunes",GERSTNER_CODE,MIT,p("Quartz Noon",0xFF6E512E,0xFFC79E61,0xFFF0D9A4,0xFFFFF4DA),p("Rose Dune",0xFF563028,0xFF9B6252,0xFFD8A885,0xFFFFE0C7),p("Moon Dune",0xFF101822,0xFF394353,0xFF8A8F95,0xFFE4E8E9)),
    sc(AbstractShaderTheme.OCEANIC,8,"Storm at Sea",TouchReaction.WARP,"osgw storm Gerstner composition",GERSTNER_CODE,MIT,p("Steel Ocean",0xFF07121C,0xFF243848,0xFF2D6072,0xFFD9E6E8),p("Green Squall",0xFF070D16,0xFF1E2A38,0xFF274759,0xFFAAB2B5),p("Night Lightning",0xFF02030A,0xFF18233F,0xFF3C5C87,0xFFEAF7FF)),
    sc(AbstractShaderTheme.OCEANIC,9,"Underwater Sunbeams",TouchReaction.RIPPLE,"Ocean Scene underwater volume and light shafts",OCEAN_CODE,MIT,p("Mediterranean",0xFF052534,0xFF087D92,0xFF68D2C1,0xFFF2DDAA),p("Cenote",0xFF061C2C,0xFF0E5978,0xFF55A8B8,0xFFE9E1C7),p("Emerald Bay",0xFF062A2D,0xFF0A8A79,0xFF62C9A6,0xFFF0D5A0))
)
