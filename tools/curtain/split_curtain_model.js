"use strict";

/**
 * Splits curtain aggregate models into per-bone model files.
 *
 * The placed curtain is rendered by CurtainBlockEntityRenderer through the
 * SAP animation rig: AnimatedBlockModel binds one baked model file per rig
 * bone. Those per-bone files live in models/block/curtain/curtain_<part>/.
 * The aggregate models (curtain_upper_r.json, curtain_lower_r.json) are only
 * used for item models and blockstates, so any texture/UV/geometry edit to
 * them must be re-split into the per-bone files before it shows up in game.
 *
 * Usage:  node tools/curtain/split_curtain_model.js
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const curtainDir = path.join(
    repoRoot, "src", "main", "resources", "assets", "shadowsandpetals", "models", "block", "curtain"
);

/**
 * Part definitions: aggregate model file, output folder, and the
 * element-index -> rig-bone mapping verified against each part's bbmodel
 * outliner. Keep the element order in the aggregate model stable.
 */
const PARTS = [
    {
        name: "curtain_upper_r",
        // Element 9 is the rail-end outcrop; it rides the static rail bone.
        boneOfElement: ["g1_1", "g1", "g2_1", "g2", "g3_1", "g3", "g4_1", "g4", "group", "group"]
    },
    {
        name: "curtain_lower_r",
        // Lower panels bind straight to the g1..g4 bones; each panel carries
        // both the gx translation and the gx_1 rotation of the upper rig.
        boneOfElement: ["g1", "g2", "g3", "g4"]
    },
    {
        name: "curtain_upper_l",
        boneOfElement: ["g1_1", "g1", "g2_1", "g2", "g3_1", "g3", "g4_1", "g4", "group", "group"]
    },
    {
        name: "curtain_lower_l",
        // Left mirrors bind like the right ones.
        boneOfElement: ["g1", "g2", "g3", "g4"]
    }
];

function fail(message) {
    console.error("split_curtain_model: " + message);
    process.exit(1);
}

for (const part of PARTS) {
    const aggregateFile = path.join(curtainDir, part.name + ".json");
    const model = JSON.parse(fs.readFileSync(aggregateFile, "utf8"));
    if (!Array.isArray(model.elements) || model.elements.length !== part.boneOfElement.length) {
        fail(part.name + ".json: expected " + part.boneOfElement.length + " elements, found "
            + (Array.isArray(model.elements) ? model.elements.length : "none"));
    }
    if (!model.textures) {
        fail(part.name + ".json has no textures");
    }

    const byBone = new Map();
    model.elements.forEach((element, index) => {
        const bone = part.boneOfElement[index];
        if (!byBone.has(bone)) byBone.set(bone, []);
        byBone.get(bone).push(element);
    });

    const bonesDir = path.join(curtainDir, part.name);
    for (const [bone, elements] of byBone) {
        const out = { textures: model.textures, elements };
        fs.mkdirSync(bonesDir, { recursive: true });
        fs.writeFileSync(
            path.join(bonesDir, bone + ".json"),
            JSON.stringify(out, null, 2) + "\n"
        );
    }

    console.log("split_curtain_model: " + part.name + " -> wrote " + byBone.size
        + " per-bone models (bones: " + [...byBone.keys()].join(", ") + ")");
}