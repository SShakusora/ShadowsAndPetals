"use strict";

/**
 * Generates the large-curtain (2 wide x 2 tall, eight-panel) model family.
 *
 * Source of truth (editor coordinates, authored in Blockbench):
 *   models/block/large_curtain/large_curtain_right.json        closed pose
 *   models/block/large_curtain/large_curtain.animation.json    OPENING/CLOSING
 *
 * The .animation.json is Blockbench's native Bedrock animation export: its
 * X position and Y rotation channels are mirrored relative to the editor
 * model (editor = negate bedrock x / negate bedrock rotY). The baked pile
 * only tiles cleanly under that negation, which matches the small-curtain
 * convention exactly.
 *
 * Coordinate frame (editor -> block local):
 *   The authored half is the WEST half of a pair. Column split at editor
 *   x = 0: elements west of it land in the anchor block (local x = editor
 *   + 16), elements east of it land in the east neighbor (local x = editor).
 *   Row split at editor y = 16: below is the lower block (local y = editor
 *   y), above is the upper block (local y = editor y - 16).
 *   z is uniform: local z = editor z + 8 (fabric plane 14..15, rail 14..16 —
 *   identical to the small curtain masters).
 *   The EAST half mirrors editor x (x' = -x) and negates authored Y rotations.
 *
 * Stage 1 - bake: the OPENING clip's final keyframes rotate every fabric
 *   22.5 (authored) + 67.5 (clip) = 90 degrees about its pivot; anchors and
 *   rings translate. The pile lands at editor x -15..-7, an 8px bundle.
 * Stage 2 - tile: cut both poses into per-block masters normalized to local
 *   0..16 coordinates.
 * Stage 3 - derive: sixteen dye colors as parent-reference texture stubs.
 * Stage 4 - split: per-bone models for the animation renderer; colored files
 *   are texture-override stubs of the white bone parents.
 *
 * Usage:  node tools/curtain/split_large_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainRoot = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals"
);
const sourceDir = path.join(curtainRoot, "models", "block", "large_curtain");
const outDir = sourceDir;
const animDir = curtainRoot;

const COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
    "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
];

const NS_MODEL = "shadowsandpetals:block/large_curtain/";
const WHITE_TEXTURE = "shadowsandpetals:block/curtain/white";
const DECO_TEXTURE = "shadowsandpetals:block/curtain/curtain_deco";

const Z_OFFSET = 8;
const ROW_SPLIT_Y = 16;
const COL_SPLIT_X = 0;
const EPS = 1.0e-6;

function fail(message) {
    console.error("split_large_curtain_model: " + message);
    process.exit(1);
}

function readJson(relPath) {
    const file = path.join(animDir, relPath);
    if (!fs.existsSync(file)) fail("missing file: " + relPath);
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeModel(file, model) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(model, null, 2) + "\n");
}

// ---------------------------------------------------------------------------
// Source loading
// ---------------------------------------------------------------------------

function loadSources() {
    const closed = readJson("models/block/large_curtain/large_curtain_right.json");
    if (!Array.isArray(closed.elements) || !closed.elements.length) {
        fail("source model has no elements");
    }
    const animation = readJson("models/block/large_curtain/large_curtain.animation.json");
    const opening = animation.animations.OPENING;
    const closing = animation.animations.CLOSING;
    if (!opening || !closing) fail("animation file lacks OPENING/CLOSING clips");
    return { closed, opening, closing };
}

/**
 * Element-index -> bone map, straight from the exported outline. Nested
 * groups assign their bone name to every descendant element index.
 */
function boneBindings(closed) {
    const boneOf = new Array(closed.elements.length).fill(null);
    (function walk(nodes, parent) {
        for (const node of nodes) {
            if (typeof node === "number") {
                boneOf[node] = parent;
                continue;
            }
            if (typeof node === "object" && node.name) {
                walk(node.children || [], node.name);
            }
        }
    })(closed.groups || [], null);
    const missing = boneOf.filter(bone => !bone).length;
    if (missing) fail("outline leaves " + missing + " elements unbound to a bone");
    return boneOf;
}

function groupPivots(closed) {
    const pivots = new Map();
    (function collect(nodes) {
        for (const node of nodes) {
            if (typeof node === "object" && node.name) {
                pivots.set(node.name, node.origin);
                collect(node.children || []);
            }
        }
    })(closed.groups || []);
    return pivots;
}

// ---------------------------------------------------------------------------
// Stage 1: bake the open pose (editor coordinates)
// ---------------------------------------------------------------------------

/**
 * Bedrock -> editor pose of the OPENING clip's final keyframe: negate the X
 * translation and the Y rotation of every bone channel.
 */
function openPoseOf(opening) {
    const pose = new Map();
    for (const [bone, channels] of Object.entries(opening.bones)) {
        let tx = 0;
        let ry = 0;
        if (channels.position) {
            const frames = channels.position;
            const last = frames[Object.keys(frames).reduce((a, b) => Number(b) > Number(a) ? b : a)];
            tx = -last[0];
        }
        if (channels.rotation) {
            const frames = channels.rotation;
            const last = frames[Object.keys(frames).reduce((a, b) => Number(b) > Number(a) ? b : a)];
            ry = -last[1];
        }
        pose.set(bone, { tx, ry });
    }
    return pose;
}

/**
 * The Bedrock clip also describes the runtime clip the renderer plays; both
 * are produced from the same source with the mirror applied.
 */
function runtimeClip(clip, rigBones) {
    const length = clip.animation_length;
    const out = { length, loop: false, animations: [] };
    for (const [bone, channels] of Object.entries(clip.bones)) {
        for (const [channel, frames] of Object.entries(channels)) {
            const target = channel === "position" ? "minecraft:position" : "minecraft:rotation";
            const keyframes = Object.keys(frames)
                    .sort((a, b) => Number(a) - Number(b))
                    .map(key => {
                        const value = frames[key];
                        const converted = channel === "position"
                                ? [-value[0], value[1], value[2]]
                                : [value[0], -value[1], value[2]];
                        return {
                            timestamp: Number(key),
                            target: converted,
                            interpolation: "minecraft:linear"
                        };
                    });
            out.animations.push({ bone, target, keyframes });
        }
    }
    void rigBones;
    return out;
}

function rotYAbout(point, pivot, degrees) {
    const radians = (degrees * Math.PI) / 180;
    const cos = Math.cos(radians);
    const sin = Math.sin(radians);
    const dx = point[0] - pivot[0];
    const dz = point[2] - pivot[2];
    return [
        pivot[0] + dx * cos + dz * sin,
        point[1],
        pivot[2] - dx * sin + dz * cos
    ];
}

function rotateDirection(direction, degrees) {
    if (direction === "up" || direction === "down") return direction;
    // +90: north -> east, east -> south, south -> west, west -> north.
    const clockwise = { north: "east", east: "south", south: "west", west: "north" };
    let steps = degrees > 0
            ? Math.round(degrees / 90) % 4
            : (4 - Math.round(-degrees / 90) % 4) % 4;
    let dir = direction;
    for (let i = 0; i < steps; i++) dir = clockwise[dir];
    return dir;
}

function bakeElement(element, bonePose, pivot) {
    const authoredY = element.rotation && element.rotation.axis === "y"
            ? element.rotation.angle
            : 0;
    const degrees = authoredY + bonePose.ry;
    const corners = [];
    for (const x of [element.from[0], element.to[0]]) {
        for (const y of [element.from[1], element.to[1]]) {
            for (const z of [element.from[2], element.to[2]]) {
                corners.push(rotYAbout([x, y, z], pivot, degrees));
            }
        }
    }
    const xs = corners.map(c => c[0]);
    const ys = corners.map(c => c[1]);
    const zs = corners.map(c => c[2]);
    const out = {
        name: element.name,
        from: [Math.min(...xs) + bonePose.tx, Math.min(...ys), Math.min(...zs)],
        to: [Math.max(...xs) + bonePose.tx, Math.max(...ys), Math.max(...zs)]
    };
    if (element.rotation && (element.rotation.axis !== "y" || degrees === 0)) {
        // Hanging rings keep their authored X rotation; their pivot moves
        // with the baked translation.
        out.rotation = JSON.parse(JSON.stringify(element.rotation));
        out.rotation.origin[0] += bonePose.tx;
    }
    if (degrees % 360 !== 0) {
        const faces = {};
        for (const [direction, face] of Object.entries(element.faces)) {
            faces[rotateDirection(direction, degrees)] = face;
        }
        out.faces = faces;
    } else {
        out.faces = element.faces;
    }
    return out;
}

function bakeOpenElements(closed, boneOf, pose, pivots) {
    return closed.elements.map((element, index) => {
        const bone = boneOf[index];
        const fabricPose = pose.get(bone) || { tx: 0, ry: 0 };
        // A fabric bone inherits its anchor's translation (the outline nests
        // fabric under anchor and the rig composes parent chains).
        let tx = fabricPose.tx;
        let ry = fabricPose.ry;
        if (bone.endsWith("_fabric")) {
            const anchorPose = pose.get(bone.replace(/_fabric$/, "_anchor"));
            if (anchorPose) tx += anchorPose.tx;
        }
        if (bone === "rail" || (tx === 0 && ry === 0)) {
            return JSON.parse(JSON.stringify(element));
        }
        const authoredY = element.rotation && element.rotation.axis === "y";
        const pivot = authoredY ? element.rotation.origin : pivots.get(bone);
        if (!pivot) fail("no pivot for bone " + bone);
        return bakeElement(element, { tx, ry }, pivot);
    });
}

// ---------------------------------------------------------------------------
// Stage 2: tile editor elements into per-block local models
// ---------------------------------------------------------------------------

/**
 * Converts one editor element to local coordinates inside its (row, column)
 * tile. Returns null when the element does not intersect the tile.
 */
function toTile(element, row, column, mirror) {
    const out = JSON.parse(JSON.stringify(element));
    if (mirror) {
        mirrorElement(out);
    }
    const loX = Math.min(out.from[0], out.to[0]);
    const hiX = Math.max(out.from[0], out.to[0]);
    const loY = Math.min(out.from[1], out.to[1]);
    const hiY = Math.max(out.from[1], out.to[1]);
    if (loY < ROW_SPLIT_Y - EPS && hiY > ROW_SPLIT_Y + EPS) {
        fail("element spans the row boundary: " + JSON.stringify(out.from) + ".." + JSON.stringify(out.to));
    }
    if (row === "upper" && loY < ROW_SPLIT_Y - EPS) return null;
    if (row === "lower" && loY >= ROW_SPLIT_Y - EPS) return null;

    // Column semantics in the (possibly mirrored) frame: the RIGHT curtain
    // bunches at editor x<0, so its outer column is x<0 and its inner column
    // is x>=0. The whole-element rule keeps sub-pixel boundary overhangs
    // (panel_4's fabric) with the column of the element center, matching the
    // panel pivot's column.
    const centerX = (out.from[0] + out.to[0]) / 2;
    // The RIGHT curtain bunches at x<0; the mirrored LEFT curtain bunches at
    // x>=0. "outer" is always the bunching column.
    const inOuterColumn = mirror ? centerX >= COL_SPLIT_X : centerX < COL_SPLIT_X;
    if ((column === "outer") !== inOuterColumn) return null;
    // Outer column local x: RIGHT curtain outer is editor -16..0 (local =
    // editor + 16); mirrored LEFT outer is editor 0..16 (local = editor).
    // Inner columns take the other shift.
    const xShift = column === "outer" ? (mirror ? 0 : 16) : (mirror ? 16 : 0);
    const yShift = row === "upper" ? ROW_SPLIT_Y : 0;
    const clipped = out;
    clipped.from[0] += xShift;
    clipped.to[0] += xShift;
    clipped.from[1] -= yShift;
    clipped.to[1] -= yShift;
    if (clipped.rotation) {
        clipped.rotation.origin[0] += xShift;
        clipped.rotation.origin[1] -= yShift;
    }
    clipped.from[2] += Z_OFFSET;
    clipped.to[2] += Z_OFFSET;
    return clipped;
}

const FACE_MIRROR = { north: "north", south: "south", east: "west", west: "east", up: "up", down: "down" };

/**
 * Mirrors an editor element about x = 0 for the LEFT partner curtain: negate
 * x, negate authored Y rotations, swap east/west faces and flip their U axis.
 */
function mirrorElement(element) {
    const from = element.from[0];
    const to = element.to[0];
    const lo = Math.min(from, to);
    const hi = Math.max(from, to);
    const mirroredLo = -hi;
    const mirroredHi = -lo;
    element.from[0] = mirroredLo;
    element.to[0] = mirroredHi;
    if (element.rotation) {
        element.rotation.origin[0] = -element.rotation.origin[0];
        if (element.rotation.axis === "y") {
            element.rotation.angle = -element.rotation.angle;
        }
    }
    if (element.faces) {
        const faces = {};
        for (const [direction, face] of Object.entries(element.faces)) {
            const copied = JSON.parse(JSON.stringify(face));
            if (Array.isArray(copied.uv) && copied.uv.length === 4) {
                const [u0, v0, u1, v1] = copied.uv;
                copied.uv = [16 - u1, v0, 16 - u0, v1];
            }
            faces[FACE_MIRROR[direction] || direction] = copied;
        }
        element.faces = faces;
    }
}

function tileMaster(elements, row, column, mirror) {
    const tiled = [];
    for (const element of elements) {
        const local = toTile(element, row, column, mirror);
        if (local) tiled.push(local);
    }
    return tiled;
}

// ---------------------------------------------------------------------------
// Stages 3 + 4: color derivation and per-bone split
// ---------------------------------------------------------------------------

function masterTextures() {
    // Keys mirror the authored export: #1 is the dyeable fabric, #0 the
    // shared deco texture, particle tracks the fabric.
    return { "1": WHITE_TEXTURE, "0": DECO_TEXTURE, "particle": WHITE_TEXTURE };
}

function colorStubTextures(color) {
    return { "1": "shadowsandpetals:block/curtain/" + color, "particle": "shadowsandpetals:block/curtain/" + color };
}

function deriveColorStubs(baseName) {
    for (const color of COLORS) {
        if (color === "white") continue;
        writeModel(path.join(outDir, baseName + "_" + color + ".json"), {
            parent: NS_MODEL + baseName,
            textures: colorStubTextures(color)
        });
    }
}

function splitBones(baseName, boneElements) {
    const bonesDir = path.join(outDir, baseName);
    // Prune stale output so renamed bones or changed column splits never linger.
    fs.rmSync(bonesDir, { recursive: true, force: true });
    for (const color of COLORS) {
        if (color !== "white") {
            fs.rmSync(path.join(outDir, baseName + "_" + color), { recursive: true, force: true });
        }
    }
    for (const [bone, elements] of Object.entries(boneElements)) {
        writeModel(path.join(bonesDir, bone + ".json"), {
            textures: masterTextures(),
            elements
        });
    }
    for (const color of COLORS) {
        if (color === "white") continue;
        const colorDir = path.join(outDir, baseName + "_" + color);
        for (const bone of Object.keys(boneElements)) {
            writeModel(path.join(colorDir, bone + ".json"), {
                parent: NS_MODEL + baseName + "/" + bone,
                textures: colorStubTextures(color)
            });
        }
    }
    console.log("split_large_curtain_model: " + baseName + " -> "
            + Object.keys(boneElements).length + " bone parents + "
            + (COLORS.length - 1) + " color stub sets");
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

function main() {
    const { closed, opening, closing } = loadSources();
    const boneOf = boneBindings(closed);
    const pivots = groupPivots(closed);
    const pose = openPoseOf(opening);

    const openElements = bakeOpenElements(closed, boneOf, pose, pivots);

    // Pile sanity: baked fabrics must collapse into a contiguous bundle.
    const fabricBoxes = openElements
            .map((element, index) => ({ element, bone: boneOf[index] }))
            .filter(entry => entry.bone.endsWith("_fabric"))
            .map(entry => entry.element);
    const pileX = fabricBoxes.flatMap(e => [e.from[0], e.to[0]]);
    const pileMin = Math.min(...pileX);
    const pileMax = Math.max(...pileX);
    if (pileMax - pileMin > 10 + 1.0e-4) {
        fail("baked pile is " + (pileMax - pileMin).toFixed(2) + "px wide, expected ~8");
    }
    console.log("bake: fabric pile x " + pileMin.toFixed(2) + ".." + pileMax.toFixed(2));

    // This model is the RIGHT curtain of a pair: it bunches toward its outer
    // (west) column. The LEFT partner mirrors the whole model. Each curtain
    // is a 2x2 structure: rows lower/upper x columns outer (bunching side)
    // and inner (toward the partner).
    const sides = [
        { side: "right", mirror: false },
        { side: "left", mirror: true }
    ];
    const rows = ["lower", "upper"];
    const columns = ["outer", "inner"];

    // Per-bone elements in editor space (closed pose) for the split stage.
    const bones = [...new Set(boneOf)];

    for (const { side, mirror } of sides) {
        for (const row of rows) {
            for (const column of columns) {
                // Static masters: closed and open poses per block.
                for (const poseName of ["closed", "open"]) {
                    const elements = tileMaster(
                            poseName === "closed" ? closed.elements : openElements, row, column, mirror);
                    const baseName = "large_curtain_" + row + "_" + column + "_" + side
                            + (poseName === "open" ? "_open" : "");
                    writeModel(path.join(outDir, baseName + ".json"), {
                        textures: masterTextures(),
                        elements
                    });
                    deriveColorStubs(baseName);
                }
                // Per-bone models for the renderer (closed pose drives the rig).
                const boneElements = {};
                for (const bone of bones) {
                    if (bone === "rail" && row === "lower") continue;
                    const subset = [];
                    closed.elements.forEach((element, index) => {
                        if (boneOf[index] !== bone) return;
                        const local = toTile(element, row, column, mirror);
                        if (local) subset.push(local);
                    });
                    if (subset.length) boneElements[bone] = subset;
                }
                splitBones("large_curtain_" + row + "_" + column + "_" + side, boneElements);
            }
        }
    }

    // Runtime animation resources: one rig + one controller + two clips per
    // half; the renderer's rig pivots are the group origins shifted into
    // local coordinates of the anchor block (west) or mirrored (east).
    for (const { side, mirror } of sides) {
        const rigBones = [];
        for (const bone of bones) {
            const pivot = pivots.get(bone);
            if (!pivot) fail("no pivot for bone " + bone);
            const entry = {
                name: bone,
                pivot: [mirror ? -pivot[0] : pivot[0], pivot[1], pivot[2] + Z_OFFSET]
            };
            if (bone.endsWith("_fabric")) {
                entry.parent = bone.replace(/_fabric$/, "_anchor");
            }
            rigBones.push(entry);
        }
        writeJson(path.join(curtainRoot, "sap", "animations", "rigs", "large_curtain", side + ".json"),
                { format_version: 1, bones: rigBones });
        // Clips: the runtime plays them against block-local pivots; reuse the
        // bedrock mirror conversion on both clips.
        const openingClip = runtimeClip(opening, rigBones);
        const closingClip = runtimeClip(closing, rigBones);
        writeJson(path.join(curtainRoot, "neoforge", "animations", "entity", "large_curtain", side, "opening.json"), openingClip);
        writeJson(path.join(curtainRoot, "neoforge", "animations", "entity", "large_curtain", side, "closing.json"), closingClip);
        const controller = {
            format_version: 1,
            rig: "shadowsandpetals:large_curtain/" + side,
            initial: "closed",
            states: {
                open: {
                    clip: "shadowsandpetals:large_curtain/" + side + "/opening",
                    speed: 1,
                    wrap: "clamp",
                    mask: bones
                },
                closed: {
                    clip: "shadowsandpetals:large_curtain/" + side + "/closing",
                    speed: 1,
                    wrap: "clamp",
                    mask: bones
                }
            },
            transitions: [
                { from: "open", to: "closed", duration: 0.08 },
                { from: "closed", to: "open", duration: 0.08 }
            ]
        };
        writeJson(path.join(curtainRoot, "sap", "animations", "controllers", "large_curtain", side + ".json"), controller);
        console.log("anim: large_curtain/" + side + " rig(" + rigBones.length + " bones) + controller + 2 clips");
    }

    // Item display model: a plain parent stub of the outer lower master.
    writeModel(path.join(outDir, "large_white_curtain.json"), {
        parent: NS_MODEL + "large_curtain_lower_outer_right",
        textures: masterTextures()
    });
    for (const color of COLORS) {
        if (color === "white") continue;
        writeModel(path.join(outDir, "large_" + color + "_curtain.json"), {
            parent: "shadowsandpetals:block/large_curtain/large_white_curtain",
            textures: colorStubTextures(color)
        });
    }
    console.log("derive: large_white_curtain -> " + (COLORS.length - 1) + " color hand models");
}

function writeJson(file, value) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2) + "\n");
}

main();