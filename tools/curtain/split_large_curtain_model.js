"use strict";

/**
 * Generates the large-curtain model family for the four-block (2x2) curtain.
 *
 * Source of truth (editor coordinates, authored in Blockbench):
 *   tools/curtain/source/large_curtain/right/model.json
 *   tools/curtain/source/large_curtain/right/animation.json
 *   tools/curtain/source/large_curtain/left/model.json
 *   tools/curtain/source/large_curtain/left/animation.json
 *   (the left animation has every operation value negated so the mirror lands
 *   in the left-bunching pose; the .bbmodel files are intentionally untouched)
 *
 * The .animation.json is Blockbench's native Bedrock export: its X position
 * and Y rotation channels are mirrored relative to the editor model, so
 * editor = negate bedrock x / negate bedrock rotY.
 *
 * Coordinate frame: VERBATIM editor coordinates everywhere. The authored
 * export right/model.json is the base model as-is (block-local
 * per the Blockbench block workspace centered convention), and the
 * per-bone models, rig pivots and baked open pose all reuse that same
 * frame untouched, so the static and animated renders always agree.
 * *
 * Outputs:
 *   models/block/large_curtain/item/white.json             white item display model
 *   models/block/large_curtain/animated/<side>/white/<bone>.json
 *   models/block/large_curtain/animated/<side>/<color>/<bone>.json
 *   models/block/large_curtain/static/<side>/<pose>/<color>/<quadrant>.json
 *   sap/animations/rigs/large_curtain/<side>.json          rig (local pivots)
 *   sap/animations/controllers/large_curtain/<side>.json   controller
 *   neoforge/animations/entity/large_curtain/<side>/*.json opening/closing
 *
 * Usage:  node tools/curtain/split_large_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainRoot = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals"
);
const sourceDir = path.join(repoRoot, "tools", "curtain", "source", "large_curtain");
const outDir = path.join(curtainRoot, "models", "block", "large_curtain");
const animDir = curtainRoot;

const WHITE_TEXTURE = "shadowsandpetals:block/curtain/white";
const DECO_TEXTURE = "shadowsandpetals:block/curtain/curtain_deco";
const NS = "shadowsandpetals:block/curtain/";

const COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
    "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
];

/** Texture keys of a model that point at the white curtain texture. */
function whiteTextureKeys(model) {
    return Object.keys(model.textures || {}).filter(key => model.textures[key] === WHITE_TEXTURE);
}

function fail(message) {
    console.error("split_large_curtain_model: " + message);
    process.exit(1);
}

function readJson(relPath) {
    const file = path.join(animDir, relPath);
    if (!fs.existsSync(file)) fail("missing file: " + relPath);
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function readSourceJson(relPath) {
    const file = path.join(sourceDir, relPath);
    if (!fs.existsSync(file)) fail("missing source file: " + relPath);
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, value) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2) + "\n");
}

function masterTextures() {
    // Keys mirror the authored export: #1 is the dyeable fabric, #0 the
    // shared deco texture, particle tracks the fabric.
    return { "1": WHITE_TEXTURE, "0": DECO_TEXTURE, "particle": WHITE_TEXTURE };
}

// ---------------------------------------------------------------------------
// Source loading
// ---------------------------------------------------------------------------

function loadSources(modelFile, animationFile) {
    const closed = readSourceJson(modelFile);
    if (!Array.isArray(closed.elements) || !closed.elements.length) {
        fail("source model has no elements");
    }
    const animation = readSourceJson(animationFile);
    const opening = animation.animations.OPENING;
    const closing = animation.animations.CLOSING;
    if (!opening || !closing) fail("animation file lacks OPENING/CLOSING clips");
    return { closed, opening, closing };
}

/** Element-index -> bone name from the exported outline. */
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

/**
 * Local-frame rig pivots derived from the elements' authored rotation
 * origins (the export moved geometry into block-local space while the
 * outline groups still carry stale editor origins). Each bone takes the
 * rotation origin of its first rotating element; bones without one (none
 * in this model after the update) fall back to the outline group origin.
 */
function groupPivots(closed, boneOf) {
    const pivots = new Map();
    closed.elements.forEach((element, index) => {
        const bone = boneOf[index];
        if (pivots.has(bone) || !element.rotation || !element.rotation.origin) {
            return;
        }
        pivots.set(bone, element.rotation.origin);
    });
    (function collect(nodes) {
        for (const node of nodes) {
            if (typeof node === "object" && node.name && !pivots.has(node.name) && node.origin) {
                pivots.set(node.name, node.origin);
            }
            if (typeof node === "object" && node.children) collect(node.children);
        }
    })(closed.groups || []);
    return pivots;
}

// ---------------------------------------------------------------------------
// Bedrock -> editor pose
// ---------------------------------------------------------------------------

/**
 * Final keyframe of every bone in a clip, converted from the Bedrock export
 * (mirrored X position and Y rotation) into editor coordinates.
 */
function poseOf(clip) {
    const pose = new Map();
    for (const [bone, channels] of Object.entries(clip.bones)) {
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

/** Runtime NeoForge clip from the Bedrock source with the mirror applied. */
function runtimeClip(clip) {
    const out = { length: clip.animation_length, loop: false, animations: [] };
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
    return out;
}

// ---------------------------------------------------------------------------
// Baking (editor coords, using the small curtain's verified math)
// ---------------------------------------------------------------------------

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

function bakeElements(closed, boneOf, pose, pivots) {
    return closed.elements.map((element, index) => {
        const bone = boneOf[index];
        const bonePose = pose.get(bone) || { tx: 0, ry: 0 };
        let tx = bonePose.tx;
        let ry = bonePose.ry;
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
// Verbatim frame: the authored export coordinates ARE the model space; no
// transformation anywhere.
// ---------------------------------------------------------------------------

function toLocal(element) {
    return element;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

function main() {
    // Right curtain: authored export + the white item display model.
    processSide({
        side: "right",
        modelFile: "right/model.json",
        animationFile: "right/animation.json",
        writeMasters: true
    });
    // Left curtain: mirrored editor model; its animation source has every
    // operation value negated so the Bedrock mirror below lands on the
    // left-bunching pose.
    processSide({
        side: "left",
        modelFile: "left/model.json",
        animationFile: "left/animation.json",
        writeMasters: false
    });
    deriveColorVariants();
}

/**
 * Derives every dye-color variant as a tiny parent-reference stub that only
 * overrides the fabric texture keys: one stub per static quadrant master,
 * one per per-bone renderer model (both sides), and one item display model.
 */
function deriveColorVariants() {
    // Static quadrant masters: closed and open, both sides. The long names
    // make SIDE, pose, color, row and column explicit in the resource path.
    for (const side of ["right", "left"]) {
        for (const pose of ["closed", "open"]) {
            for (const half of ["upper", "lower"]) {
                for (const column of ["outer", "inner"]) {
                    const quadrant = half + "_" + column;
                    const masterRel = path.join("static", side, pose, "white", quadrant + ".json");
                    const master = readJson("models/block/large_curtain/" + masterRel);
                    const keys = whiteTextureKeys(master);
                    const parent = "shadowsandpetals:block/large_curtain/"
                            + masterRel.slice(0, -".json".length).replaceAll(path.sep, "/");
                    for (const color of COLORS) {
                        if (color === "white") continue;
                        const stub = { parent, textures: {} };
                        for (const key of keys) stub.textures[key] = NS + color;
                        writeJson(path.join(outDir, "static", side, pose, color, quadrant + ".json"), stub);
                    }
                }
            }
        }
    }
    console.log("derive: 16 quadrant masters -> " + (COLORS.length - 1) + " color stubs each");

    // Per-bone renderer models: white bones keep the geometry; colored bones
    // are texture-override stubs of the white bone, one directory per color.
    for (const side of ["right", "left"]) {
        const bonesDir = path.join(outDir, "animated", side, "white");
        const boneFiles = fs.readdirSync(bonesDir).filter(file => file.endsWith(".json"));
        for (const boneFile of boneFiles) {
            const bone = boneFile.slice(0, -".json".length);
            const whiteBone = readJson("models/block/large_curtain/animated/" + side + "/white/" + boneFile);
            const keys = whiteTextureKeys(whiteBone);
            for (const color of COLORS) {
                if (color === "white") continue;
                const stub = {
                    parent: "shadowsandpetals:block/large_curtain/animated/" + side + "/white/" + bone,
                    textures: {}
                };
                for (const key of keys) stub.textures[key] = NS + color;
                writeJson(path.join(outDir, "animated", side, color, bone + ".json"), stub);
            }
        }
        console.log("derive: animated/" + side + "/white/ -> " + (COLORS.length - 1) + " colored bone dirs");
    }

    // Item display models: parented to the white whole-curtain model.
    for (const color of COLORS) {
        if (color === "white") continue;
        writeJson(path.join(outDir, "item", color + ".json"), {
            parent: "shadowsandpetals:block/large_curtain/item/white",
            textures: { "1": NS + color }
        });
    }
    console.log("derive: item/white -> " + (COLORS.length - 1) + " color item models");
}

function processSide({ side, modelFile, animationFile, writeMasters }) {
    const { closed, opening, closing } = loadSources(modelFile, animationFile);
    const boneOf = boneBindings(closed);
    const pivots = groupPivots(closed, boneOf);
    const pose = poseOf(opening);

    const openElements = bakeElements(closed, boneOf, pose, pivots);

    // Sanity: the baked fabric pile must collapse into a contiguous bundle.
    const pileX = openElements
            .map((element, index) => ({ element, bone: boneOf[index] }))
            .filter(entry => entry.bone.endsWith("_fabric"))
            .flatMap(entry => [entry.element.from[0], entry.element.to[0]]);
    const pileMin = Math.min(...pileX);
    const pileMax = Math.max(...pileX);
    if (pileMax - pileMin > 10 + 1.0e-4) {
        fail("baked pile is " + (pileMax - pileMin).toFixed(2) + "px wide, expected ~8");
    }
    console.log("bake[" + side + "]: fabric pile x " + pileMin.toFixed(2) + ".." + pileMax.toFixed(2));

    if (writeMasters) {
        // Keep the complete authored model only as the white item display
        // model. Static block quadrants are curated files and animation uses
        // the per-bone models below.
        writeJson(path.join(outDir, "item", "white.json"), closed);
        console.log("masters: item/white.json (" + closed.elements.length + " authored elements)");
    }

    // Per-bone parents for the animation renderer (closed pose, verbatim).
    const bones = [...new Set(boneOf)];
    const bonesDir = path.join(outDir, "animated", side, "white");
    fs.rmSync(bonesDir, { recursive: true, force: true });
    for (const bone of bones) {
        const elements = closed.elements
                .filter((_, index) => boneOf[index] === bone)
                .map(toLocal);
        writeJson(path.join(bonesDir, bone + ".json"), {
            textures: masterTextures(),
            elements
        });
    }
    console.log("split[" + side + "]: " + bones.length + " per-bone parents under animated/" + side + "/white/");

    // Rig with verbatim pivots; controller; runtime clips.
    const rigBones = bones.map(bone => {
        const pivot = pivots.get(bone);
        if (!pivot) fail("no pivot for bone " + bone);
        const entry = {
            name: bone,
            pivot: pivot.slice()
        };
        if (bone.endsWith("_fabric")) {
            entry.parent = bone.replace(/_fabric$/, "_anchor");
        }
        return entry;
    });
    writeJson(path.join(curtainRoot, "sap", "animations", "rigs", "large_curtain", side + ".json"),
            { format_version: 1, bones: rigBones });
    writeJson(path.join(curtainRoot, "neoforge", "animations", "entity", "large_curtain", side, "opening.json"),
            runtimeClip(opening));
    writeJson(path.join(curtainRoot, "neoforge", "animations", "entity", "large_curtain", side, "closing.json"),
            runtimeClip(closing));
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
    console.log("anim[" + side + "]: large_curtain/" + side + " rig(" + rigBones.length + " bones) + controller + 2 clips");
}

main();
