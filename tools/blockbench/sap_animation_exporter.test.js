"use strict";

const assert = require("node:assert/strict");
const {
    inferUseSequence,
    useSequenceTransitions,
    normalizeAnimationMode,
    parseBlockBones,
    blockEditorPoint,
    blockRuntimePoint,
    defaultBlockBones
} = require("./sap_animation_exporter.js");

const hammer = inferUseSequence([
    "use/hammer",
    "use/hammer_outro",
    "use/hammer_intro"
]);
assert.deepEqual(hammer, {
    intro: "use/hammer_intro",
    loop: "use/hammer",
    outro: "use/hammer_outro"
});
assert.deepEqual(useSequenceTransitions(hammer), [
    {from: "use/hammer_intro", to: "use/hammer", duration: 0},
    {from: "use/hammer_intro", to: "use/hammer_outro", duration: 0.08},
    {from: "use/hammer", to: "use/hammer_outro", duration: 0.08},
    {from: "use/hammer_outro", to: "use/hammer_intro", duration: 0.08}
]);

assert.equal(inferUseSequence([
    "use/hammer",
    "use/hammer_intro"
]), null);

assert.equal(normalizeAnimationMode("player"), "player");
assert.equal(normalizeAnimationMode("BER"), "block_entity");
assert.throws(() => normalizeAnimationMode("unsupported"), /动画类型无效/);
assert.deepEqual(blockEditorPoint([0, 4, 16]), [-8, 4, 8]);
assert.deepEqual(blockRuntimePoint([-8, 4, 8]), [0, 4, 16]);
assert.deepEqual(blockEditorPoint([8, 9, 9]), [0, 9, 1]);
assert.deepEqual(blockRuntimePoint([0, 9, 1]), [8, 9, 9]);
assert.deepEqual(blockRuntimePoint(blockEditorPoint([3.5, 2, 12.25])), [3.5, 2, 12.25]);
assert.throws(() => blockEditorPoint([0, 0]), /长度为 3/);
assert.deepEqual(parseBlockBones([
    {
        name: "main",
        pivot: [8, 16, 8],
        parent: "root",
        rest: {
            translation: [0, 1, 0],
            rotation: [0, 0, 4],
            scale: [1, 1, 1]
        }
    },
    {name: "root", pivot: [8, 0, 8]}
]), [
    {
        name: "main",
        pivot: [8, 16, 8],
        parent: "root",
        rest: {
            translation: [0, 1, 0],
            rotation: [0, 0, 4],
            scale: [1, 1, 1]
        }
    },
    {
        name: "root",
        pivot: [8, 0, 8],
        parent: null,
        rest: {
            translation: [0, 0, 0],
            rotation: [0, 0, 0],
            scale: [1, 1, 1]
        }
    }
]);

const defaults = defaultBlockBones();
defaults[0].pivot[0] = 99;
assert.equal(defaultBlockBones()[0].pivot[0], 8);

assert.throws(
    () => parseBlockBones([
        {name: "root", pivot: [0, 0, 0]},
        {name: "root", pivot: [0, 0, 0]}
    ]),
    /名称重复/
);
assert.throws(
    () => parseBlockBones([{name: "main", pivot: [0, 0, 0], parent: "missing"}]),
    /缺失父级/
);
assert.throws(
    () => parseBlockBones([
        {name: "a", pivot: [0, 0, 0], parent: "b"},
        {name: "b", pivot: [0, 0, 0], parent: "a"}
    ]),
    /父级循环/
);

assert.equal(inferUseSequence([
    "use/hammer",
    "use/hammer_intro",
    "use/hammer_outro",
    "use/chisel",
    "use/chisel_intro",
    "use/chisel_outro"
]), null);
