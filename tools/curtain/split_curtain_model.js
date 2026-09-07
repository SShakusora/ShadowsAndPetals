"use strict";

/**
 * Generates and splits the small-curtain model family.
 *
 * The runtime layout intentionally mirrors the large-curtain family:
 *
 *   models/block/curtain/static/<side>/<pose>/<color>/<half>.json
 *   models/block/curtain/animated/<side>/<color>/<bone>.json
 *   models/block/curtain/item/<color>.json
 *
 * The four closed white masters and the white item source live outside the
 * runtime asset tree under tools/curtain/source/curtain/. Open-pose static
 * masters and all colored/animated variants are generated from those sources.
 * Blockbench files are not read or rewritten by this script.
 *
 * Stage 0 - bake open pose: reads each part's "opening" clip keyframes and
 * the rig pivots to compute the open-pose transform of every bone, then bakes
 * the closed white masters into the static/<side>/open/white/<half>.json
 * masters. Panels rotate 22.5 (authored) + 67.5 (clip) = 90 degrees about
 * their rig pivot, which maps their boxes onto new axis-aligned boxes, so the
 * baked model needs no element rotation at all. Faces are permuted to follow
 * the rotated geometry; texture UVs stay attached to their face.
 *
 * Stage 1 - derive: writes colored static and item models as parent-reference
 * stubs that only override the dyeable fabric texture.
 *
 * Stage 2 - split: writes the closed per-bone models used by
 * CurtainBlockEntityRenderer. The open pose renders through the static
 * block-state model, so no open per-bone files exist.
 *
 * Usage:  node tools/curtain/split_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals", "models", "block", "curtain"
);
const sourceDir = path.join(repoRoot, "tools", "curtain", "source", "curtain");
const animDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals"
);

const COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
    "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
];

const NS = "shadowsandpetals:block/curtain/";
const WHITE_TEXTURE = NS + "white";

/**
 * Aggregate part definitions: side/half identify the semantic runtime path;
 * source points at the hand-authored JSON export and boneOfElement maps the
 * element order to the rig bones verified against the Blockbench outliner.
 */
const PARTS = [
    {
        id: "upper_right",
        side: "right",
        half: "upper",
        source: "right/upper.json",
        // Element 9 is the rail-end outcrop; it rides the static rail bone.
        boneOfElement: ["panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail"]
    },
    {
        id: "lower_right",
        side: "right",
        half: "lower",
        source: "right/lower.json",
        // Lower panels bind straight to the panel bones.
        boneOfElement: ["panel_1", "panel_2", "panel_3", "panel_4"]
    },
    {
        id: "upper_left",
        side: "left",
        half: "upper",
        source: "left/upper.json",
        boneOfElement: ["panel_1_fabric", "panel_1_anchor", "panel_2_fabric", "panel_2_anchor",
            "panel_3_fabric", "panel_3_anchor", "panel_4_fabric", "panel_4_anchor", "rail", "rail"]
    },
    {
        id: "lower_left",
        side: "left",
        half: "lower",
        source: "left/lower.json",
        // Left mirrors bind like the right ones.
        boneOfElement: ["panel_1", "panel_2", "panel_3", "panel_4"]
    }
];

// Clip resource per part used to bake the open pose.
const OPEN_CLIPS = {
    upper_right: "neoforge/animations/entity/curtain/upper_right/opening.json",
    lower_right: "neoforge/animations/entity/curtain/lower_right/opening.json",
    upper_left: "neoforge/animations/entity/curtain/upper_left/opening.json",
    lower_left: "neoforge/animations/entity/curtain/lower_left/opening.json"
};

// Rig resource per part (pivots for baking rotations).
const RIGS = {
    upper_right: "sap/animations/rigs/curtain/upper_right.json",
    lower_right: "sap/animations/rigs/curtain/lower_right.json",
    upper_left: "sap/animations/rigs/curtain/upper_left.json",
    lower_left: "sap/animations/rigs/curtain/lower_left.json"
};

function fail(message) {
    console.error("split_curtain_model: " + message);
    process.exit(1);
}

function readJson(relPath) {
    return JSON.parse(fs.readFileSync(path.join(animDir, relPath), "utf8"));
}

function readSourceJson(relPath) {
    const file = path.join(sourceDir, relPath);
    if (!fs.existsSync(file)) {
        fail("missing source file: " + relPath);
    }
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function staticRel(part, pose, color = "white") {
    return path.join("static", part.side, pose, color, part.half + ".json");
}

function readStatic(part, pose, color = "white") {
    const relPath = staticRel(part, pose, color);
    const file = path.join(curtainDir, relPath);
    if (!fs.existsSync(file)) {
        fail("missing static model: " + relPath);
    }
    return JSON.parse(fs.readFileSync(file, "utf8"));
}

function modelId(relPath) {
    return "shadowsandpetals:block/curtain/"
            + relPath.slice(0, -".json".length).replaceAll(path.sep, "/");
}

function writeModel(file, model) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(model, null, 2) + "\n");
}

/**
 * Returns the texture keys of the master that point at the white curtain
 * texture; color variants must override exactly those keys.
 */
function whiteTextureKeys(model) {
    return Object.keys(model.textures).filter(key => model.textures[key] === WHITE_TEXTURE);
}

// ---------------------------------------------------------------------------
// Stage 0: bake the open pose.
// ---------------------------------------------------------------------------

/**
 * Face permutation under a Y rotation. The element face that pointed along
 * the old direction now points along the rotated direction; up and down
 * faces keep their direction.
 */
function rotateDirection(direction, degrees) {
    // +90: north -> east, east -> south, south -> west, west -> north.
    const clockwise = { north: "east", east: "south", south: "west", west: "north" };
    if (direction === "up" || direction === "down") {
        return direction;
    }
    let steps = 0;
    if (degrees > 0) {
        steps = Math.round(degrees / 90) % 4;
    } else {
        steps = (4 - Math.round(-degrees / 90) % 4) % 4;
    }
    let dir = direction;
    for (let i = 0; i < steps; i++) {
        dir = clockwise[dir];
    }
    return dir;
}

function rotateElement(element, bonePose, pivot) {
    const degrees = (bonePose.ry || 0) + (element.rotation && element.rotation.axis === "y"
        ? element.rotation.angle : 0);
    const tx = (bonePose.tx || 0);
    const ty = (bonePose.ty || 0);
    const tz = (bonePose.tz || 0);

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
        from: [
            Math.min(...xs) + tx,
            Math.min(...ys) + ty,
            Math.min(...zs) + tz
        ],
        to: [
            Math.max(...xs) + tx,
            Math.max(...ys) + ty,
            Math.max(...zs) + tz
        ]
    };
    if (element.rotation && (element.rotation.axis !== "y" || degrees === 0)) {
        // Non-Y rotations (hanging rings) keep their authored rotation.
        out.rotation = element.rotation;
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

/**
 * Reads the open clip and returns, per bone name, the final keyframe values:
 * a translation in model pixels and a rotation in degrees.
 */
function openPoseOf(part) {
    const clip = readJson(OPEN_CLIPS[part.id]);
    const pose = new Map();
    for (const anim of clip.animations) {
        const last = anim.keyframes[anim.keyframes.length - 1].target;
        if (anim.target === "minecraft:position") {
            pose.set(anim.bone, { tx: last[0], ty: last[1], tz: last[2], ry: 0 });
        } else if (anim.target === "minecraft:rotation") {
            const prev = pose.get(anim.bone) || { tx: 0, ty: 0, tz: 0 };
            pose.set(anim.bone, {
                tx: prev.tx, ty: prev.ty, tz: prev.tz,
                ry: last[1]
            });
        }
    }
    return pose;
}

function rigPivotOf(part, boneName) {
    const rig = readJson(RIGS[part.id]);
    const bone = rig.bones.find(b => b.name === boneName);
    if (!bone) {
        fail(RIGS[part.id] + " has no bone " + boneName);
    }
    return bone.pivot;
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

function validateSource(model, label, expectedElements) {
    if (!Array.isArray(model.elements) || model.elements.length !== expectedElements) {
        fail(label + ": expected " + expectedElements + " elements, found "
                + (Array.isArray(model.elements) ? model.elements.length : "none"));
    }
    if (whiteTextureKeys(model).length === 0) {
        fail(label + " does not reference the white curtain texture");
    }
}

function writeWhiteMasters() {
    for (const part of PARTS) {
        const master = readSourceJson(part.source);
        validateSource(master, part.source, part.boneOfElement.length);
        writeModel(path.join(curtainDir, staticRel(part, "closed")), master);
    }
    const item = readSourceJson("item.json");
    if (whiteTextureKeys(item).length === 0) {
        fail("item.json does not reference the white curtain texture");
    }
    writeModel(path.join(curtainDir, "item", "white.json"), item);
    console.log("masters: four static closed models + item/white.json");
}

function bakeOpenMasters() {
    for (const part of PARTS) {
        const master = readStatic(part, "closed");
        const pose = openPoseOf(part);
        const rig = readJson(RIGS[part.id]);
        const parentOf = new Map();
        for (const bone of rig.bones) {
            if (bone.parent) {
                parentOf.set(bone.name, bone.parent);
            }
        }
        // Collapse each bone's full parent chain into one composed transform:
        // translations of the bone and all its ancestors accumulate, and the
        // bone itself contributes its rotation.
        const boneTransform = new Map();
        for (const boneName of pose.keys()) {
            let tx = 0;
            let ty = 0;
            let tz = 0;
            let ry = 0;
            let pivotBone = boneName;
            for (let current = boneName; current; current = parentOf.get(current)) {
                const bonePose = pose.get(current);
                if (!bonePose) {
                    continue;
                }
                tx += bonePose.tx;
                ty += bonePose.ty;
                tz += bonePose.tz;
                if (current === boneName) {
                    ry = bonePose.ry;
                } else if (tx !== 0 || ty !== 0 || tz !== 0) {
                    pivotBone = current;
                }
            }
            if (tx === 0 && ty === 0 && tz === 0 && ry === 0) {
                continue;
            }
            boneTransform.set(boneName, { tx, ty, tz, ry, pivotBone });
        }

        const elements = master.elements.map((element, index) => {
            const boneName = part.boneOfElement[index];
            const bonePose = boneTransform.get(boneName);
            if (!bonePose) {
                return element;
            }
            // Rotations happen about the animated ancestor's pivot; the rig
            // pivot equals the panel element's model-space origin.
            const pivot = element.rotation && element.rotation.axis === "y"
                ? element.rotation.origin
                : rigPivotOf(part, bonePose.pivotBone);
            return rotateElement(element, bonePose, pivot);
        });

        const openModel = { textures: master.textures, elements };
        writeModel(path.join(curtainDir, staticRel(part, "open")), openModel);
        console.log("bake: static/" + part.side + "/open/white/" + part.half
                + ".json with " + elements.length + " elements");
    }
}

// ---------------------------------------------------------------------------
// Stage 1: derive the per-color static and item models from the masters.
// ---------------------------------------------------------------------------

function deriveColorVariants() {
    for (const part of PARTS) {
        for (const pose of ["closed", "open"]) {
            const master = readStatic(part, pose);
            validateSource(master, staticRel(part, pose), part.boneOfElement.length);
            const keys = whiteTextureKeys(master);
            for (const color of COLORS) {
                if (color === "white") continue;
                const whiteRel = staticRel(part, pose);
                const variant = { parent: modelId(whiteRel), textures: {} };
                for (const key of keys) {
                    variant.textures[key] = NS + color;
                }
                writeModel(path.join(curtainDir, staticRel(part, pose, color)), variant);
            }
        }
        console.log("derive: static/" + part.side + "/*/" + part.half
                + " -> " + (COLORS.length - 1) + " color stubs");
    }

    const item = readJson("models/block/curtain/item/white.json");
    const itemKeys = whiteTextureKeys(item);
    if (itemKeys.length === 0) {
        fail("item/white.json does not reference the white curtain texture");
    }
    for (const color of COLORS) {
        if (color === "white") continue;
        const variant = { parent: modelId("item/white.json"), textures: {} };
        for (const key of itemKeys) {
            variant.textures[key] = NS + color;
        }
        writeModel(path.join(curtainDir, "item", color + ".json"), variant);
    }
    console.log("derive: item/white -> " + (COLORS.length - 1) + " color stubs");
}

// ---------------------------------------------------------------------------
// Stage 2: split closed masters into per-bone models under side/color dirs.
// ---------------------------------------------------------------------------

function pruneBoneDirectory(dir, expected) {
    if (!fs.existsSync(dir)) return;
    for (const file of fs.readdirSync(dir)) {
        if (file.endsWith(".json") && !expected.has(file.slice(0, -".json".length))) {
            fs.rmSync(path.join(dir, file), { force: true });
        }
    }
}

function splitParts() {
    const sideBones = new Map([["left", new Set()], ["right", new Set()]]);
    for (const part of PARTS) {
        const master = readStatic(part, "closed");
        validateSource(master, staticRel(part, "closed"), part.boneOfElement.length);
        const keys = whiteTextureKeys(master);
        const byBone = new Map();
        master.elements.forEach((element, index) => {
            const bone = part.boneOfElement[index];
            if (!byBone.has(bone)) byBone.set(bone, []);
            byBone.get(bone).push(element);
            sideBones.get(part.side).add(bone);
        });

        // White per-bone files keep the geometry; colored files are texture
        // override stubs of those white models.
        for (const [bone, elements] of byBone) {
            writeModel(path.join(curtainDir, "animated", part.side, "white", bone + ".json"), {
                textures: master.textures,
                elements
            });
            for (const color of COLORS) {
                if (color === "white") continue;
                const stub = { parent: modelId(path.join("animated", part.side, "white", bone + ".json")), textures: {} };
                for (const key of keys) {
                    stub.textures[key] = NS + color;
                }
                writeModel(path.join(curtainDir, "animated", part.side, color, bone + ".json"), stub);
            }
        }
        console.log("split: animated/" + part.side + "/" + part.half
                + " -> " + byBone.size + " bones");
    }

    // Remove stale bone names after both halves of each side have been written.
    for (const [side, bones] of sideBones) {
        pruneBoneDirectory(path.join(curtainDir, "animated", side, "white"), bones);
        for (const color of COLORS) {
            if (color === "white") continue;
            pruneBoneDirectory(path.join(curtainDir, "animated", side, color), bones);
        }
    }
}

writeWhiteMasters();
bakeOpenMasters();
deriveColorVariants();
splitParts();
