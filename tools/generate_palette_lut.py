#!/usr/bin/env python3
"""Generate the shared 256xN RGB palette LUT used by the abstract shaders."""
import pathlib
import struct
import zlib

PALETTES = [
    ("001014", "00A8CC", "7DFFFF"),
    ("12000F", "C2007A", "FF6FE0"),
    ("140A00", "D97706", "FFD27D"),
    ("001208", "059669", "7CFFC4"),
    ("0A0014", "7C3AED", "D4B3FF"),
    ("140000", "DC2626", "FF9B8A"),
    ("120E00", "D4A017", "FFEFAF"),
    ("000E12", "0D7377", "7FEAE0"),
    ("140008", "E0517A", "FFC2D9"),
    ("0A0A0C", "6B7280", "E5E7EB"),
    ("12002B", "FF238E", "D9FF38"),
    ("071A52", "E32636", "FFD400"),
    ("001B4D", "00E5C4", "FF5A68"),
    ("120038", "35F08B", "FF58C7"),
    ("061B4F", "FF6A32", "55F5FF"),
    ("082B25", "E3C37A", "B83419"),
    ("07351E", "FF4C9A", "FFD84A"),
    ("17151B", "F0E7D2", "C55A24"),
    ("10104A", "00E7FF", "FF2E93"),
    ("071C36", "B9F5FF", "65A66A"),
    ("00152E", "0066CC", "66D9FF"),
    ("00051F", "1236A6", "5D8CFF"),
    ("002F46", "00AFC1", "A7FFF2"),
    ("020817", "193B70", "B9D9FF"),
    ("062D38", "55B8B1", "E0FFF4"),
    ("00030A", "1B315A", "FFFFFF"),
    ("090021", "8D2DCE", "FFB5E8"),
    ("03051C", "3C4FB8", "FFD98A"),
    ("00040D", "008DFF", "E9FFFF"),
    ("000000", "FF6400", "FFFFFF"),
    ("020B28", "16D98B", "FF70CF"),
    ("010A20", "68BFFF", "FFFFFF"),
    ("05001B", "623CFF", "74FFFF"),
    ("000718", "176BFF", "FFFFFF"),
    ("01040D", "176CA6", "FFB45E"),
    ("001326", "0077B6", "CAF0F8"),
    ("050607", "69737A", "FFFFFF"),
    ("F2E9DC", "31505A", "05070A"),
    ("001C3D", "0096C7", "A9F0FF"),
    ("21002D", "D12B73", "FFCB57"),
    ("000817", "004E75", "84F5E5"),
    ("080016", "7B2CFF", "8CFFCB"),
    ("080A0C", "88939B", "F8FFFF"),
    ("12002E", "21D4C2", "FF8BD8"),
    ("02091A", "155E9B", "D8F4FF"),
]

def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))

def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))

rows = []
for palette in PALETTES:
    low, mid, high = map(rgb, palette)
    row = bytearray([0])
    for x in range(256):
        u = x / 255
        color = lerp(low, mid, u * 2) if u < .5 else lerp(mid, high, (u - .5) * 2)
        row.extend(color)
    rows.append(bytes(row))

def chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)

png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", 256, len(PALETTES), 8, 2, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
png += chunk(b"IEND", b"")
target = pathlib.Path(__file__).parents[1] / "app/src/main/res/drawable-nodpi/palette_lut.png"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
print(target)
