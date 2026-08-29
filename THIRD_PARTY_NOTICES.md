# Third-party shader and simulation sources

## libASPL

The macOS MiniMate CoreAudio endpoint driver uses libASPL, copyright Victor Gaydov and contributors, under the MIT License. The vendored source and complete license texts are stored in `companion/macos/Driver/vendor/libASPL`.

## coremediaio-dal-minimal-example

The private-distribution MiniMate Camera plug-in adapts John Boiles' CoreMediaIO DAL minimal example, copyright 2020 John Boiles, under the MIT License. The build is pinned to commit `d7d24bc801f07303ac3367c2791fbf13f573cc7c`; MiniMate's frame source and the complete license are stored in `companion/macos/CameraDriver`.

MiniMate's five theme worlds are rendered from code. The application bundles no downloaded
Space/Beach photographs, Arcade screenshots, sprite sheets, character likenesses, or scene JPEGs.

## cosmos

- Copyright (c) 2026 Eluvade
- Source: https://github.com/Eluvade/cosmos
- License: MIT
- Adapted techniques: random star fields, density-wave galaxies, ringed gas giants, procedural
  planets, cloud layers, terrain, atmospheres, stellar materials, nebula flow, and black-hole
  composition. The implementations were ported from GLSL/WebGL to single-pass Android AGSL.

## Three.js-Ocean-Scene

- Copyright (c) 2023 Nugget8
- Source: https://github.com/Nugget8/Three.js-Ocean-Scene
- License: MIT
- Adapted techniques: water reflection/refraction, underwater absorption, volume lighting,
  caustic treatment, sky-water composition, and depth-dependent light.

## osgw

- Copyright (c) 2018 Erik S. Vasconcelos Jansson
- Source: https://github.com/CaffeineViking/osgw
- License: MIT
- Adapted techniques: multi-directional Gerstner wave sums, wave normals, wind-driven wave
  composition, and small-scale surface variation.

## canvas-games

- Copyright (c) 2026 Felix Orinda
- Source: https://github.com/forinda/canvas-games
- License: MIT
- Adapted mechanics: Tetris pieces and falling-board loop, racing movement, sports AI motion,
  Breakout physics, Space Invaders formations, Pac-Man maze traversal, Frogger traffic and river
  lanes, Snake grid movement, gravity-ball/pinball motion, and platformer movement. Rendering is
  rebuilt as code-generated pixel art in AGSL; no source images are bundled.

## matrix

- Copyright (c) 2018 Rezmason
- Source: https://github.com/Rezmason/matrix
- License: MIT
- Adapted techniques: rain, glyph mutation, mirror, quilt, stripe, intro, and bloom passes.

## procedural-tileable-shaders

- Copyright (c) 2019 Alin (tuxalin)
- Source: https://github.com/tuxalin/procedural-tileable-shaders
- License: MIT
- Adapted techniques: cellular, Voronoi, Perlin, FBM, domain warp, hexagon, weave, distance
  metric, gradient, and multi-hash fields.

All adaptations retain the applicable copyright notice. The MIT license permits use, copying,
modification, distribution, sublicensing, and sale provided the copyright and permission notice
is included. The software is provided without warranty.
