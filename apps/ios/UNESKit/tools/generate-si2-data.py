#!/usr/bin/env python3
"""Generates SI2Data.swift from the original Space-Impact-II data files.

Usage: generate-si2-data.py <Space-Impact-II checkout>/data <output SI2Data.swift>

Decoding mirrors UncompressPixelMap in graphics.c: bits are packed MSB-first
per byte, and the LAST byte holds the remainder (pixels % 8) in its LOW bits.
"""
import sys
from pathlib import Path

SRC = Path(sys.argv[1])
OUT = Path(sys.argv[2])


def unpack(data: bytes, pixels: int) -> list[int]:
    # Trailing all-zero bytes may be omitted (see cmIntro); the C decompressor
    # still gives the LAST PROVIDED byte the remainder (pixels % 8) bit count.
    out = [0] * pixels
    nbytes = min(len(data), (pixels + 7) // 8)
    for b in range(nbytes):
        n = (pixels % 8 or 8) if b == nbytes - 1 else 8
        for k in range(n):
            if b * 8 + k < pixels:
                out[b * 8 + k] = (data[b] >> (n - 1 - k)) & 1
    return out


def rows(bits: list[int], w: int, h: int) -> list[str]:
    return ["".join("#" if bits[y * w + x] else "." for x in range(w)) for y in range(h)]


def load_object(path: Path) -> list[str]:
    data = path.read_bytes()
    w, h = data[0], data[1]
    return rows(unpack(data[2:], w * h), w, h)


# Compressed intro sprites hardcoded in graphics.c (cmSpace/cmIntro/cmImpact).
CM_SPACE = bytes([15,255,63,248,127,131,252,127,227,255,199,255,159,249,255,143,252,120,0,224,231,15,60,3,192,30,0,56,28,225,207,0,120,3,192,7,3,28,57,
                  224,15,0,255,240,255,231,255,60,1,255,143,255,31,248,255,231,0,63,224,3,231,192,28,121,224,15,128,0,124,248,7,143,60,1,240,0,15,31,0,
                  241,231,128,62,1,255,227,224,28,56,255,231,255,127,240,248,7,143,7,249,255,12])
CM_INTRO = bytes([0,0,0,0,0,0,45,193,128,0,0,14,0,2,244,27,0,0,2,120,0,191,168,240,0,0,62,156,11,219,41,128,0,9,228,242,220,163,192,0,0,224,125,0,1,176,
                  0,0,32,19,192,0,96,0,0,0,1,192,0,0,0,0,0,0,2])
CM_IMPACT = bytes([31,31,135,207,252,63,193,254,127,241,225,252,252,255,231,254,127,231,255,30,31,255,204,30,225,231,128,7,131,225,255,249,193,206,28,
                   240,0,240,62,63,255,156,24,225,207,0,15,3,195,206,241,255,159,252,240,0,240,60,60,207,31,241,255,206,0,15,3,195,192,227,192,28,121,
                   224,1,224,60,120,14,60,3,199,158,0,30,7,135,129,227,192,60,121,224,1,224,120,120,30,60,3,199,31,252,30,15,143,1,231,128,120,240,127,
                   131,192])

# Uncompressed statics from graphics.c the watch build needs (icons padded to 5×5
# like the original C arrays, whose initializers zero-fill the declared [25]).
def padded(vals: list[int], w: int, h: int) -> list[str]:
    vals = vals + [0] * (w * h - len(vals))
    return rows(vals, w, h)

STATICS = {
    "shot": padded([1, 1, 1], 3, 1),
    "explosion1": padded([0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1], 5, 5),
    "explosion2": padded([0,0,0,0,0,0,0,1,0,0,0,1,0,1,0,0,0,1], 5, 5),
    "missileIcon": padded([0,0,0,0,0,1,0,1,1,0,1,1,1,1,1,1,0,1,1], 5, 5),
    "beamIcon": padded([0,0,0,0,0,1,1,0,0,0,1,1,1,1,1,1,1], 5, 5),
    "wallIcon": padded([0,1,1,1,0,1,1,0,1,1,1,1,0,1,1,1,1,0,1,1,0,1,1,1], 5, 5),
    "introSpace": rows(unpack(CM_SPACE, 67 * 12), 67, 12),
    "introShips": rows(unpack(CM_INTRO, 59 * 9), 59, 9),
    "introImpact": rows(unpack(CM_IMPACT, 76 * 12), 76, 12),
}


def swift_sprite(r: list[str], indent: str) -> str:
    inner = ",\n".join(f'{indent}    "{row}"' for row in r)
    return f"SI2Sprite([\n{inner},\n{indent}])"


def main() -> None:
    objects = {int(p.stem): load_object(p) for p in SRC.glob("objects/*.dat")}

    enemies = {}
    for p in SRC.glob("enemies/*.dat"):
        d = p.read_bytes()
        enemies[int(p.stem)] = dict(
            model=d[0], animCount=d[1], lives=int.from_bytes(d[2:3], "little", signed=True),
            floats=d[3], shotTime=d[4], moveUp=d[5], moveDown=d[6], moveAnyway=d[7],
            mbTop=d[8], mbBottom=d[9],
        )

    levels = []
    for i in range(6):
        d = (SRC / f"levels/{i}.dat").read_bytes()
        count = d[0]
        spawns = []
        for e in range(count):
            b = d[1 + e * 5 : 6 + e * 5]
            spawns.append((b[0] * 256 + b[1], b[2], b[3], b[4] - 1))
        assert len(d) == 1 + count * 5
        levels.append(spawns)

    lines = []
    a = lines.append
    a("// Generated from the Space-Impact-II clone's data files")
    a("// (data/{objects,enemies,levels}/*.dat plus the compressed intro sprites")
    a("// hardcoded in graphics.c). Regenerate with tools/generate-si2-data.py")
    a("// rather than editing by hand.")
    a("")
    a("#if os(watchOS)")
    a("")
    a("/// Sprites, enemy definitions and level layouts of the original game.")
    a("enum SI2Data {")

    a("    /// Dynamic objects (`data/objects`): enemy animation phases, scenery,")
    a("    /// the player ship (255), its protection blink (250/251) and the")
    a("    /// bonus-weapon projectiles (252–254).")
    a("    static let objects: [Int: SI2Sprite] = [")
    for oid in sorted(objects):
        a(f"        {oid}: {swift_sprite(objects[oid], '        ')},")
    a("    ]")
    a("")
    for name, r in STATICS.items():
        a(f"    static let {name} = {swift_sprite(r, '    ')}")
        a("")

    a("    /// Enemy definitions (`data/enemies`); 255 is the bonus-weapon carrier.")
    a("    static let enemies: [Int: SI2EnemyType] = [")
    for eid in sorted(enemies):
        e = enemies[eid]
        a(f"        {eid}: SI2EnemyType(")
        a(f"            model: {e['model']}, animCount: {e['animCount']}, lives: {e['lives']},")
        a(f"            floats: {str(bool(e['floats'])).lower()}, shotTime: {e['shotTime']},")
        a(f"            movesUp: {str(bool(e['moveUp'])).lower()}, movesDown: {str(bool(e['moveDown'])).lower()}, movesOffScreen: {str(bool(e['moveAnyway'])).lower()},")
        a(f"            topBound: {e['mbTop']}, bottomBound: {e['mbBottom']}")
        a("        ),")
    a("    ]")
    a("")
    a("    /// Level layouts (`data/levels`): spawn position, enemy id and initial")
    a("    /// vertical direction (-1 up, 0 still, 1 down). Ordered by x; the last")
    a("    /// spawn of a level is its boss.")
    a("    static let levels: [[SI2Spawn]] = [")
    for spawns in levels:
        a("        [")
        for x, y, eid, dr in spawns:
            a(f"            SI2Spawn(x: {x}, y: {y}, enemy: {eid}, direction: {dr}),")
        a("        ],")
    a("    ]")
    a("}")
    a("#endif")
    a("")

    OUT.write_text("\n".join(lines))
    print(f"objects={len(objects)} enemies={len(enemies)} levels={[len(l) for l in levels]}")
    print("player ship (object 255):")
    print("\n".join(objects[255]))
    print("enemy 0 def:", enemies[0], "model sprite:")
    print("\n".join(objects[enemies[0]["model"]]))


main()
