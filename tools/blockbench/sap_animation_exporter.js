(function () {
    "use strict";

    const PLUGIN_ID = "sap_animation_exporter";
    const FORMAT_ID = "sap_animation";
    const ROLE_ANIMATED_BONE = "animated_bone";
    const ROLE_SOCKET = "socket";
    const ROLE_REFERENCE = "reference";
    const ROLE_GUIDE = "guide";
    const PLUGIN_VERSION = "1.1.0";
    const PROFILES = ["first_person", "third_person"];
    const PROFILE_ROOTS = {
        first_person: "SAP 第一人称",
        third_person: "SAP 第三人称"
    };
    const THIRD_PERSON_ITEM_ANCHORS = [
        {
            arm: "right_arm",
            anchor: "main_hand_item",
            side: "right",
            offset: [1, 2, -10]
        },
        {
            arm: "left_arm",
            anchor: "off_hand_item",
            side: "left",
            offset: [-1, 2, -10]
        }
    ];
    const PLAYER_MODELS = ["steve", "alex"];
    const VANILLA_PLAYER_SKINS = {
        // Minecraft 26.1.2 client assets:
        // assets/minecraft/textures/entity/player/wide/steve.png
        steve: {
            name: "steve.png",
            slim: false,
            data: "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAAdVBMVEUAAAAKvLwAzMwmGgokGAgrHg0zJBE/KhW3g2uzeV5SPYn///+qclmbY0mQWT8Af38AaGhVVVWUYD52SzOBUzmPXj5JJRBCHQp3QjVqQDA0JRIoKCg3Nzc/Pz9KSko6MYlBNZtGOqUDenoFiIgElZUApKQAr6/wvakZAAAAAXRSTlMAQObYZgAAAolJREFUeNrt1l1rHucZReFrj/whu5hSCCQtlOTE/f+/Jz4q9Cu0YIhLcFVpVg+FsOCVehi8jmZgWOzZz33DM4CXlum3gH95GgeAzQZVeL4gTm6Cbp4vqFkD8HwBazPY8wWbMq9utu3mNZ5fotVezbzOE3kBEFbaZuc8kb00NTMUbWJp678Xf2GV7RRtx1TDQQ6XBNvsmL2+2vHq1TftmMPIyAWujtN2cl274ua2jpVpZneXEjjo7XW1q53V9ds4ODO5xIuhvGHvfLI3aixauig415uuO2+vl9+cncfsFw25zL650fXn687jqnXuP68/X3+eV3zE7y6u9eB73MlfAcfbTf3yR8CfAX+if8S/H5/EAbAxj5LN48tULvEBOh8V1AageMTXe2YHAOwHbZxrzPkSR3+ffr8TR2JDzE/4Fj8CDgEwDsW+q+9GsR07hhg2CsALBgMo2v5wNxXnQXMeGQVW7gUAyKI2m6KDsJ8Au3++F5RZO+kKNQjQcLLWgjwUjBXLltFgWWMUUlviocBgNoxNGgMjSxiYAA7zgLFo2hgIENiDU8gQCzDOmViGFAsEuBcQSDCothhpJaDRA8E5fHqH2nTbYm5fHLo1V0u3B7DAuheoeScRYabjjjuzs17cHVaTrTXmK78m9swP34d9oK/dfeXSIH2PW/MXwPvxN/bJlxw8zlYAcEyeI6gNgA/O8P8neN8xe1IHP2gTzegjvhUDfuRygmwEs2GE4mkCDIAzm2R4yAuPsIdR9k8AvMc+3L9+2UEjo4WP0FpgP19O0MzCsqxIoMsdDBvYcQyGmO0ZJRoYCKjLJWY0BAhYwGUBCgkh8MRdOKt+ruqMwAB2OcEX94U1TPbYJP0PkyyAI1S6cSIAAAAASUVORK5CYII="
        },
        // assets/minecraft/textures/entity/player/slim/alex.png
        alex: {
            name: "alex.png",
            slim: true,
            data: "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAAZlBMVEUAAAD////v2r/r0LDvu7HfxKLUt4/zqFiUyJKMvorrmD+GuYTljT97snjegS5xq26UlmuAglpvb29lZWV8Vz51UDhYWFhyTjZPT09sRi5kQSwjYiQrVChcOyc/Pz82NjYYOBYoKCgm8xoJAAAAAXRSTlMAQObYZgAAAwZJREFUeNrtlu1W6jAQRWtjWiaZtCC9/YgC8v4vec9Mg6Uur6b8ve6S0Lo8O2NYmClusOMGF7Nr8HKu2IoDTYPk44K6lvBciePNAm7mlTGz3G2voAaYZg9vFzCCjtnZqoHjAYHDJjAM1tqqgmR7Bahf9tFaA6zND6JwXRpxxosZIkz4uQzOWFmTrJPOUgdjOP1hzu7flmNrJe4wtCBoMgR1LVE1mNIYNbAurpafN5N5Xto8vZbl65PhWg0aRiU/C2Rpa0x5w/LHnjQwZVTgGk755+eyNMbWXDeukQIwMjaRm6qyJjmMtTAi1ziHF0OQQwsul+u1SJBnZsTbRJEgIqzxD8H1erksAt80MLTtCawFpvpScDqtKzhUYpA8yKgAv7Wq4GCripmT4PsKPFGIQxdjbFsPCPiDsbY6tAvBU2K+KRZiCHHquimC0ylGCoH8vjSmVEG4xb0Mmb2MYmGYpgH5boBA7iAIPuxL5CUtT0Q72u0wy6SDioVOosh3k5ggQIZgQB4iDITlOqwoFiaJwzJAcMJARhOShUCX3AGjWMWYYqHv+zghHntwPPaoWQJh58mLRe5FUSpQ6BenWDgi1B81rCAki0oIgiDvQRRlMkh+JdDgi2b78zieEUKEWglRG4gQp+8EEupTeOz78f39/U32n1oKb3iAQ642CWQfPlWAUAr3MtoQ2jck4XmTe7wL7ykPrFkJRkm9HCUuopF8CN6TXsGTEAjvt2+7Gu4Fl/N5xN+PMs6XCwZJLv4BER480MzHFsyfwi9ZpDNSYH7IIKcDPKB4iHSwPk7tVo974ZDYZ1VQ/PKfg9Nsddg6ouWfyCMNh2OqzBbBCdxXUDMq2Cq4r6CmDRVEoRvm4z4Q8OwowdmCbgDThEq8Jwg8pb4gSzANU5cY4l1ngCtDMM0NR4yYwfS5LzgoewXPXwuQ1bZDsIJJ2OJHPvcL49wQJEyGQI+5pV/o9Sj8IKcCSd31C5sFEhrv+gVrzAbBul9QINiwB+t+QdHWLBmyPgXtF8CtX5CO4LsK/gL4WlU0/HKH/gAAAABJRU5ErkJggg=="
        }
    };
    // Minecraft 26.1.2 ItemInHandRenderer idle matrices, converted to model pixels.
    // In Blockbench, armTranslation and itemTranslation are represented by the
    // exported groups' local origins. At runtime the same values are exported as
    // rest translations while the PoseStack pivot remains zero.
    const FIRST_PERSON_POSES = {
        right: {
            armTranslation: [5.556366, -17.703216, -11.836864],
            armRotation: [-84.98303, 34.872156, -116.646127],
            itemTranslation: [-8.314181, 5.285143, -1.63448],
            itemRotation: [-81.694014, -60.22075, 137.803025]
        },
        left: {
            armTranslation: [-5.556366, -17.703216, -11.836864],
            armRotation: [-84.98303, -34.872156, 116.646127],
            itemTranslation: [8.314181, 5.285143, -1.63448],
            itemRotation: [-81.694014, 60.22075, -137.803025]
        }
    };
    const actions = [];
    const properties = [];
    let sapFormat;
    let studioApi;
    let projectParsedHandler;
    let projectCompileHandler;
    let displayDefaultPoseHandler;
    let firstPersonHudHandler;
    let firstPersonHudStyle;
    const firstPersonHudOverlays = new Map();
    let parsedRefreshToken = 0;

    if (typeof Plugin === "undefined") {
        if (typeof module !== "undefined" && module.exports) {
            module.exports = {inferUseSequence, useSequenceTransitions};
        }
        return;
    }

    Plugin.register(PLUGIN_ID, {
        title: "Shadows And Petals 动画制作器",
        author: "Shadows And Petals",
        description: "用于离线制作第一人称与第三人称玩家使用动画。",
        icon: "fa-person-running",
        tags: ["Animation", "Minecraft: Java Edition"],
        variant: "desktop",
        version: PLUGIN_VERSION,
        min_version: "5.0.6",
        onload() {
            installFormat();
            installProperties();
            installActions();
            installPublicApi();
            installFirstPersonHud();
            projectParsedHandler = handleProjectParsed;
            projectCompileHandler = sanitizeSapEditorState;
            displayDefaultPoseHandler = applyThirdPersonPreviewHandedness;
            firstPersonHudHandler = synchronizeFirstPersonHud;
            Codecs.project.on("parsed", projectParsedHandler);
            Codecs.project.on("compile", projectCompileHandler);
            Blockbench.on("display_default_pose", displayDefaultPoseHandler);
            for (const event of [
                "render_frame",
                "resize_window",
                "select_project",
                "unselect_project"
            ]) {
                Blockbench.on(event, firstPersonHudHandler);
            }
            handleProjectParsed();
        },
        onunload() {
            if (projectParsedHandler) {
                Codecs.project.removeListener("parsed", projectParsedHandler);
                projectParsedHandler = null;
            }
            if (projectCompileHandler) {
                Codecs.project.removeListener("compile", projectCompileHandler);
                projectCompileHandler = null;
            }
            if (displayDefaultPoseHandler) {
                Blockbench.removeListener("display_default_pose", displayDefaultPoseHandler);
                displayDefaultPoseHandler = null;
            }
            if (firstPersonHudHandler) {
                for (const event of [
                    "render_frame",
                    "resize_window",
                    "select_project",
                    "unselect_project"
                ]) {
                    Blockbench.removeListener(event, firstPersonHudHandler);
                }
                firstPersonHudHandler = null;
            }
            uninstallFirstPersonHud();
            parsedRefreshToken++;
            if (globalThis.SAPAnimationStudio === studioApi) {
                delete globalThis.SAPAnimationStudio;
            }
            actions.splice(0).forEach(action => action.delete());
            properties.splice(0).forEach(property => property.delete());
            if (sapFormat) sapFormat.delete();
        }
    });

    function installFormat() {
        sapFormat = new ModelFormat(FORMAT_ID, {
            name: "SAP 使用动画",
            description: "SAP 第一人称与第三人称使用动画工作区",
            icon: "fa-person-running",
            category: "minecraft",
            target: "Minecraft: Java Edition",
            codec: Codecs.project,
            bone_rig: true,
            animation_mode: true,
            animation_files: true,
            animation_controllers: true,
            animation_loop_wrapping: true,
            locators: true,
            rotate_cubes: true,
            euler_order: "ZYX",
            display_mode: true,
            per_texture_uv_size: true,
            centered_grid: true,
            block_size: 16,
            box_uv: true,
            show_on_start_screen: true,
            show_in_new_list: true
        });
        sapFormat.new = function () {
            showNewProjectDialog();
            return true;
        };
    }

    function installProperties() {
        properties.push(new Property(Group, "enum", "sap_role", {
            condition: () => isSapProject(),
            default: ROLE_ANIMATED_BONE,
            values: [ROLE_ANIMATED_BONE, ROLE_SOCKET, ROLE_REFERENCE, ROLE_GUIDE]
        }));
        properties.push(new Property(Group, "enum", "sap_reference_side", {
            condition: group => isSapProject()
                && (group.sap_role === ROLE_REFERENCE || group.sap_role === ROLE_GUIDE),
            default: "both",
            values: ["both", "left", "right"]
        }));
        properties.push(new Property(Group, "vector", "sap_rest_translation", {
            condition: group => isExportedGroup(group),
            default: [0, 0, 0]
        }));
        properties.push(new Property(Group, "vector", "sap_rest_scale", {
            condition: group => isExportedGroup(group),
            default: [1, 1, 1]
        }));
        properties.push(new Property(Group, "enum", "sap_held_item_hand", {
            condition: group => isSapProject() && group.sap_role === ROLE_REFERENCE,
            default: "none",
            values: ["none", "left", "right"]
        }));
        properties.push(new Property(Group, "string", "sap_item_display_context", {
            condition: group => isSapProject()
                && group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none",
            default: ""
        }));
        properties.push(new Property(Group, "string", "sap_item_source", {
            condition: group => isSapProject()
                && group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none",
            default: ""
        }));
        properties.push(new Property(Group, "vector", "sap_item_display_scale", {
            condition: group => isSapProject()
                && group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none",
            default: [1, 1, 1]
        }));
        properties.push(new Property(Group, "boolean", "sap_item_uses_mesh_scale", {
            condition: group => isSapProject()
                && group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none",
            default: false
        }));
        properties.push(new Property(Cube, "boolean", "sap_player_reference", {
            condition: () => isSapProject(),
            default: false
        }));
        properties.push(new Property(Cube, "string", "sap_item_element_id", {
            condition: () => isSapProject(),
            default: ""
        }));
        if (typeof ModelProject !== "undefined") {
            projectProperty("string", "sap_profiles", "first_person");
            projectProperty("string", "sap_active_profile", "first_person");
            projectProperty("string", "sap_namespace", "shadowsandpetals");
            projectProperty("string", "sap_rig_path", "animation/main");
            projectProperty("string", "sap_controller_path", "animation/main");
            projectProperty("string", "sap_resource_roots", "");
            projectProperty("boolean", "sap_show_left_hand", true);
            projectProperty("boolean", "sap_show_first_person_hud", true);
            projectProperty("string", "sap_player_model", "steve");
            projectProperty("string", "sap_transitions", "[]");
        }

        if (typeof Animation !== "undefined") {
            properties.push(new Property(Animation, "string", "sap_events", {
                condition: () => isSapProject(),
                default: "[]"
            }));
            properties.push(new Property(Animation, "number", "sap_speed", {
                condition: () => isSapProject(),
                default: 1
            }));
            properties.push(new Property(Animation, "boolean", "sap_additive", {
                condition: () => isSapProject(),
                default: false
            }));
        }
    }

    function projectProperty(type, name, defaultValue) {
        properties.push(new Property(ModelProject, type, name, {
            condition: () => isSapProject(),
            default: defaultValue
        }));
    }

    function installPublicApi() {
        studioApi = Object.freeze({
            version: PLUGIN_VERSION,
            createProject(options) {
                return createSapProject(options || {});
            },
            setPreviewItem(filePath, options) {
                return setPreviewItemFromApi(filePath, options);
            },
            clearPreviewItem(options) {
                return clearPreviewItemsFromApi(options);
            },
            setFirstPersonHudVisible(visible) {
                if (!isSapProject()) throw new Error("请先打开一个 SAP 使用动画项目，再设置第一人称 HUD");
                setFirstPersonHudVisibility(visible);
            },
            loadPlayerSkin(filePath) {
                if (!isSapProject()) throw new Error("请先打开一个 SAP 使用动画项目，再加载玩家皮肤");
                return loadPlayerSkin(filePath);
            },
            exportBundle(directory, options) {
                if (!isSapProject()) throw new Error("请先打开一个 SAP 使用动画项目，再执行导出");
                return exportAnimationBundle(directory, options || {});
            },
            activateProfile,
            generateRig
        });
        globalThis.SAPAnimationStudio = studioApi;
    }

    function installFirstPersonHud() {
        if (typeof document === "undefined") return;
        document.querySelectorAll(".sap-first-person-hud").forEach(node => node.remove());
        document.getElementById("sap-first-person-hud-style")?.remove();
        firstPersonHudStyle = document.createElement("style");
        firstPersonHudStyle.id = "sap-first-person-hud-style";
        firstPersonHudStyle.textContent = `
            .sap-first-person-hud {
                --sap-hud-scale: 2;
                position: absolute;
                left: 0;
                top: 0;
                width: 100%;
                height: 100%;
                z-index: 8;
                overflow: hidden;
                border-radius: 7px;
                pointer-events: none;
                user-select: none;
                image-rendering: pixelated;
            }
            .sap-first-person-hud[hidden] {
                display: none;
            }
            .sap-hud-crosshair {
                position: absolute;
                left: 50%;
                top: 50%;
                width: 15px;
                height: 15px;
                transform: translate(-50%, -50%) scale(var(--sap-hud-scale));
                transform-origin: center;
                filter: drop-shadow(1px 0 #000) drop-shadow(-1px 0 #000)
                    drop-shadow(0 1px #000) drop-shadow(0 -1px #000);
            }
            .sap-hud-crosshair::before,
            .sap-hud-crosshair::after {
                content: "";
                position: absolute;
                background: rgba(255, 255, 255, 0.92);
            }
            .sap-hud-crosshair::before {
                left: 7px;
                top: 2px;
                width: 1px;
                height: 11px;
            }
            .sap-hud-crosshair::after {
                left: 2px;
                top: 7px;
                width: 11px;
                height: 1px;
            }
            .sap-hud-bottom {
                position: absolute;
                left: 50%;
                bottom: 6px;
                width: 182px;
                transform: translateX(-50%) scale(var(--sap-hud-scale));
                transform-origin: bottom center;
            }
            .sap-hud-status {
                display: flex;
                justify-content: space-between;
                width: 182px;
                height: 10px;
                margin-bottom: 2px;
            }
            .sap-hud-meter {
                display: grid;
                grid-template-columns: repeat(10, 8px);
                gap: 1px;
                width: 89px;
                height: 9px;
            }
            .sap-hud-meter.hunger {
                direction: rtl;
            }
            .sap-hud-heart,
            .sap-hud-hunger {
                position: relative;
                width: 8px;
                height: 8px;
                filter: drop-shadow(1px 1px rgba(0, 0, 0, 0.9));
            }
            .sap-hud-heart {
                background: #d92b2b;
                clip-path: polygon(
                    0 25%, 13% 13%, 38% 13%, 50% 25%,
                    63% 13%, 88% 13%, 100% 25%, 100% 50%,
                    50% 100%, 0 50%
                );
            }
            .sap-hud-heart::after {
                content: "";
                position: absolute;
                left: 2px;
                top: 2px;
                width: 2px;
                height: 2px;
                background: #ff9191;
            }
            .sap-hud-hunger {
                background: #c77a2d;
                clip-path: polygon(
                    13% 0, 50% 0, 63% 13%, 75% 13%, 100% 38%,
                    100% 75%, 75% 100%, 38% 100%, 13% 75%,
                    13% 50%, 0 38%
                );
            }
            .sap-hud-hunger::after {
                content: "";
                position: absolute;
                right: 1px;
                top: 1px;
                width: 3px;
                height: 2px;
                background: #f5bd62;
            }
            .sap-hud-experience {
                width: 180px;
                height: 3px;
                margin: 0 1px 2px;
                background: #171717;
                border: 1px solid rgba(0, 0, 0, 0.9);
                box-sizing: border-box;
                box-shadow: inset 0 1px #565656;
            }
            .sap-hud-experience::after {
                content: "";
                display: block;
                width: 58%;
                height: 1px;
                background: #80dc16;
                box-shadow: 0 0 1px #baff45;
            }
            .sap-hud-hotbar {
                display: grid;
                grid-template-columns: repeat(9, 20px);
                width: 182px;
                height: 22px;
                padding: 1px;
                background: rgba(12, 12, 12, 0.72);
                box-sizing: border-box;
                box-shadow:
                    inset 1px 1px rgba(255, 255, 255, 0.45),
                    inset -1px -1px rgba(0, 0, 0, 0.9);
            }
            .sap-hud-slot {
                position: relative;
                width: 20px;
                height: 20px;
                background: rgba(90, 90, 90, 0.28);
                border: 1px solid rgba(20, 20, 20, 0.8);
                box-sizing: border-box;
                box-shadow:
                    inset 1px 1px rgba(255, 255, 255, 0.17),
                    inset -1px -1px rgba(0, 0, 0, 0.5);
            }
            .sap-hud-slot.selected::after {
                content: "";
                position: absolute;
                inset: -2px;
                border: 2px solid #f5f5f5;
                box-shadow:
                    0 0 0 1px rgba(0, 0, 0, 0.9),
                    inset 0 0 0 1px rgba(0, 0, 0, 0.55);
            }
        `;
        document.head.appendChild(firstPersonHudStyle);
        synchronizeFirstPersonHud();
    }

    function uninstallFirstPersonHud() {
        firstPersonHudOverlays.forEach(overlay => overlay.remove());
        firstPersonHudOverlays.clear();
        if (firstPersonHudStyle) {
            firstPersonHudStyle.remove();
            firstPersonHudStyle = null;
        }
    }

    function synchronizeFirstPersonHud() {
        if (typeof Preview === "undefined" || !Array.isArray(Preview.all)) return;
        const previews = Preview.all.filter(
            preview => preview && preview.node && !preview.offscreen
        );
        const livePreviews = new Set(previews);
        firstPersonHudOverlays.forEach((overlay, preview) => {
            if (livePreviews.has(preview) && overlay.isConnected) return;
            overlay.remove();
            firstPersonHudOverlays.delete(preview);
        });

        const visible = isSapProject()
            && Project.sap_active_profile === "first_person"
            && Project.sap_show_first_person_hud !== false;
        previews.forEach(preview => {
            let overlay = firstPersonHudOverlays.get(preview);
            if (!overlay) {
                overlay = createFirstPersonHudOverlay();
                preview.node.appendChild(overlay);
                firstPersonHudOverlays.set(preview, overlay);
            }
            if (overlay.hidden !== !visible) overlay.hidden = !visible;
            if (visible) updateFirstPersonHudScale(preview, overlay);
        });
    }

    function createFirstPersonHudOverlay() {
        const overlay = document.createElement("div");
        overlay.className = "sap-first-person-hud";
        const hearts = Array.from(
            {length: 10},
            () => '<span class="sap-hud-heart"></span>'
        ).join("");
        const hunger = Array.from(
            {length: 10},
            () => '<span class="sap-hud-hunger"></span>'
        ).join("");
        const slots = Array.from(
            {length: 9},
            (_, index) => `<span class="sap-hud-slot${index === 0 ? " selected" : ""}"></span>`
        ).join("");
        overlay.innerHTML = `
            <div class="sap-hud-crosshair"></div>
            <div class="sap-hud-bottom">
                <div class="sap-hud-status">
                    <div class="sap-hud-meter health">${hearts}</div>
                    <div class="sap-hud-meter hunger">${hunger}</div>
                </div>
                <div class="sap-hud-experience"></div>
                <div class="sap-hud-hotbar">${slots}</div>
            </div>
        `;
        return overlay;
    }

    function updateFirstPersonHudScale(preview, overlay) {
        const canvas = preview.canvas;
        if (!canvas) return;
        const left = Number(canvas.offsetLeft || 0);
        const top = Number(canvas.offsetTop || 0);
        const width = Number(canvas.offsetWidth || preview.width || 0);
        const height = Number(canvas.offsetHeight || preview.height || 0);
        if (width <= 0 || height <= 0) return;
        const bounds = {
            left: `${left}px`,
            top: `${top}px`,
            width: `${width}px`,
            height: `${height}px`
        };
        Object.entries(bounds).forEach(([property, value]) => {
            if (overlay.style[property] !== value) overlay.style[property] = value;
        });
        const availableScale = Math.min(
            (width - 16) / 182,
            (height - 16) / 130,
            2
        );
        const scale = availableScale >= 2
            ? 2
            : availableScale >= 1.5
                ? 1.5
                : availableScale >= 1
                    ? 1
                    : 0.5;
        const value = String(scale);
        if (overlay.style.getPropertyValue("--sap-hud-scale") !== value) {
            overlay.style.setProperty("--sap-hud-scale", value);
        }
    }

    function installActions() {
        addAction("new_sap_animation_project", {
            name: "新建 SAP 使用动画项目",
            description: "创建第一人称和/或第三人称离线动画工作区",
            icon: "fa-file-circle-plus",
            click: showNewProjectDialog
        }, "file.new");
        addAction("configure_sap_animation_project", {
            name: "配置 SAP 使用动画项目",
            icon: "fa-sliders",
            condition: () => isSapProject(),
            click: showProjectDialog
        }, "tools");
        addAction("import_sap_reference_model", {
            name: "设置 SAP 预览物品",
            description: "设置或替换指定手的非导出物品预览",
            icon: "fa-cubes",
            condition: () => isSapProject(),
            click: showPreviewItemDialog
        }, "file.import");
        addAction("clear_sap_preview_item", {
            name: "清除 SAP 预览物品",
            description: "移除指定手的非导出物品预览",
            icon: "fa-eraser",
            condition: () => isSapProject(),
            click: showClearPreviewItemDialog
        }, "tools");
        addAction("load_sap_player_skin", {
            name: "加载 SAP 玩家皮肤",
            description: "使用本地 64x64 PNG 皮肤替换玩家参考纹理",
            icon: "fa-shirt",
            condition: () => isSapProject(),
            click: showPlayerSkinDialog
        }, "file.import");
        addAction("sap_camera_first_person", {
            name: "SAP 相机：第一人称",
            icon: "fa-camera",
            condition: () => isSapProject(),
            click: () => activateProfile("first_person")
        }, "view");
        addAction("sap_camera_third_person", {
            name: "SAP 相机：第三人称",
            icon: "fa-camera",
            condition: () => isSapProject(),
            click: () => activateProfile("third_person")
        }, "view");
        addAction("toggle_sap_left_hand", {
            name: "切换 SAP 第一人称左手显示",
            icon: "fa-hand",
            condition: () => isSapProject(),
            click: toggleLeftHand
        }, "view");
        addAction("toggle_sap_first_person_hud", {
            name: "切换 SAP 第一人称基础 HUD",
            description: "显示或隐藏准星、快捷栏、生命值、饱食度和经验条",
            icon: "fa-crosshairs",
            condition: () => isSapProject(),
            click: toggleFirstPersonHud
        }, "view");
        addAction("edit_sap_bone_rest_pose", {
            name: "编辑 SAP 骨骼静止姿势",
            description: "编辑导出的静止平移和缩放",
            icon: "fa-person",
            condition: () => isSapProject() && Group.selected && isExportedGroup(Group.selected),
            click: showRestPoseDialog
        }, "tools");
        addAction("export_sap_animation_bundle", {
            name: "导出 SAP 动画资源包",
            description: "导出动画片段、骨架和控制器",
            icon: "fa-file-export",
            condition: () => isSapProject(),
            click: showExportDialog
        }, "file.export");
    }

    function addAction(id, options, menu) {
        const action = new Action(id, options);
        actions.push(action);
        MenuBar.addAction(action, menu);
    }

    function showNewProjectDialog() {
        const dialog = new Dialog({
            id: "sap_animation_new_project",
            title: "新建 SAP 使用动画项目",
            form: {
                first_person: {label: "第一人称工作区", type: "checkbox", value: true},
                third_person: {label: "第三人称工作区", type: "checkbox", value: true},
                show_first_person_hud: {
                    label: "显示第一人称基础 HUD",
                    type: "checkbox",
                    value: true
                },
                player_model: {
                    label: "原版玩家参考模型",
                    type: "select",
                    options: {
                        steve: "Steve（宽手臂）",
                        alex: "Alex（细手臂）"
                    },
                    value: "steve"
                },
                namespace: {label: "命名空间", type: "text", value: "shadowsandpetals"},
                resource_path: {label: "骨架/控制器路径", type: "text", value: "animation/main"}
            },
            onConfirm(form) {
                const profiles = PROFILES.filter(profile => form[profile]);
                createSapProject({
                    profiles,
                    showFirstPersonHud: form.show_first_person_hud,
                    playerModel: form.player_model,
                    namespace: form.namespace,
                    rigPath: form.resource_path,
                    controllerPath: form.resource_path
                });
                dialog.hide();
            }
        });
        dialog.show();
    }

    function createSapProject(options) {
        const profiles = Array.isArray(options.profiles)
            ? parseProfiles(options.profiles.join(","))
            : parseProfiles(options.profiles || "first_person");
        const namespace = String(options.namespace || "shadowsandpetals").trim();
        const rigPath = cleanPath(options.rigPath || "animation/main");
        const controllerPath = cleanPath(options.controllerPath || rigPath);
        const playerModel = normalizePlayerModel(options.playerModel);
        const slimArms = VANILLA_PLAYER_SKINS[playerModel].slim;
        validateIdentifier(namespace, rigPath);
        validateIdentifier(namespace, controllerPath);

        if (typeof newProject === "function") {
            newProject(sapFormat);
        } else {
            ModelFormat.prototype.new.call(sapFormat);
        }
        Project.texture_width = 64;
        Project.texture_height = 64;
        Project.box_uv = true;
        Project.sap_profiles = profiles.join(",");
        Project.sap_active_profile = profiles[0];
        Project.sap_namespace = namespace;
        Project.sap_rig_path = rigPath;
        Project.sap_controller_path = controllerPath;
        Project.sap_show_left_hand = options.showLeftHand !== false;
        Project.sap_show_first_person_hud = options.showFirstPersonHud !== false;
        Project.sap_player_model = playerModel;
        Project.sap_resource_roots = String(options.resourceRoots || "");
        const transitions = typeof options.transitions === "string"
            ? parseJsonArray(options.transitions, "控制器过渡")
            : options.transitions || [];
        if (!Array.isArray(transitions)) throw new Error("控制器过渡必须是 JSON 数组");
        Project.sap_transitions = JSON.stringify(transitions);
        const skinTexture = createReferenceSkinTexture(playerModel);
        createWorkspace(profiles, slimArms, skinTexture);
        if (!Project.sap_show_left_hand) setLeftHandVisibility(false);
        activateProfile(profiles[0]);
        return Project;
    }

    function showProjectDialog() {
        const dialog = new Dialog({
            id: "sap_animation_project_settings",
            title: "配置 SAP 使用动画项目",
            form: {
                profiles: {
                    label: "人称配置（逗号分隔）",
                    type: "text",
                    value: Project.sap_profiles || "first_person"
                },
                namespace: {label: "命名空间", type: "text", value: projectValue("sap_namespace", "shadowsandpetals")},
                rig_path: {label: "骨架路径", type: "text", value: projectValue("sap_rig_path", "animation/main")},
                controller_path: {label: "控制器路径", type: "text", value: projectValue("sap_controller_path", "animation/main")},
                resource_roots: {
                    label: "本地资源根目录（分号分隔）",
                    type: "text",
                    value: Project.sap_resource_roots || ""
                },
                show_first_person_hud: {
                    label: "显示第一人称基础 HUD",
                    type: "checkbox",
                    value: Project.sap_show_first_person_hud !== false
                },
                transitions: {
                    label: "控制器过渡 JSON",
                    type: "textarea",
                    value: Project.sap_transitions || "[]"
                }
            },
            onConfirm(form) {
                const profiles = parseProfiles(form.profiles);
                validateIdentifier(form.namespace.trim(), cleanPath(form.rig_path));
                validateIdentifier(form.namespace.trim(), cleanPath(form.controller_path));
                parseJsonArray(form.transitions, "控制器过渡");
                Project.sap_profiles = profiles.join(",");
                Project.sap_namespace = form.namespace.trim();
                Project.sap_rig_path = cleanPath(form.rig_path);
                Project.sap_controller_path = cleanPath(form.controller_path);
                Project.sap_resource_roots = form.resource_roots.trim();
                Project.sap_show_first_person_hud = !!form.show_first_person_hud;
                Project.sap_transitions = form.transitions.trim() || "[]";
                synchronizeFirstPersonHud();
                dialog.hide();
            }
        });
        dialog.show();
    }

    function createWorkspace(profiles, slimArms, skinTexture) {
        Undo.initEdit({outliner: true, textures: [skinTexture]});
        if (profiles.includes("first_person")) createFirstPersonTemplate(slimArms, skinTexture);
        if (profiles.includes("third_person")) createThirdPersonTemplate(slimArms, skinTexture);
        Undo.finishEdit("创建 SAP 动画工作区");
        Canvas.updateAll();
    }

    function createFirstPersonTemplate(slimArms, skinTexture) {
        const guide = makeGroup(PROFILE_ROOTS.first_person, [0, 0, 0], ROLE_GUIDE);
        const rightPose = FIRST_PERSON_POSES.right;
        const rightArm = makeGroup(
            "first_person_right_arm",
            rightPose.armTranslation,
            ROLE_ANIMATED_BONE,
            guide
        );
        rightArm.rotation = rightPose.armRotation.slice();
        rightArm.sap_rest_translation = rightPose.armTranslation.slice();
        const rightReference = makeGroup(
            "第一人称右臂参考",
            offsetVector(rightArm.origin, [-5, 2, 0]),
            ROLE_REFERENCE,
            rightArm,
            "right"
        );
        rightReference.rotation = [0, 0, 0.1 * 180 / Math.PI];
        addFirstPersonArm(rightReference, false, slimArms, rightArm.origin, skinTexture);
        const rightItem = makeGroup(
            "first_person_right_item",
            offsetVector(rightArm.origin, rightPose.itemTranslation),
            ROLE_SOCKET,
            rightArm
        );
        rightItem.rotation = rightPose.itemRotation.slice();
        rightItem.sap_rest_translation = rightPose.itemTranslation.slice();

        const leftPose = FIRST_PERSON_POSES.left;
        const leftArm = makeGroup(
            "first_person_left_arm",
            leftPose.armTranslation,
            ROLE_ANIMATED_BONE,
            guide
        );
        leftArm.rotation = leftPose.armRotation.slice();
        leftArm.sap_rest_translation = leftPose.armTranslation.slice();
        const leftReference = makeGroup(
            "第一人称左臂参考",
            offsetVector(leftArm.origin, [5, 2, 0]),
            ROLE_REFERENCE,
            leftArm,
            "left"
        );
        leftReference.rotation = [0, 0, -0.1 * 180 / Math.PI];
        addFirstPersonArm(leftReference, true, slimArms, leftArm.origin, skinTexture);
        const leftItem = makeGroup(
            "first_person_left_item",
            offsetVector(leftArm.origin, leftPose.itemTranslation),
            ROLE_SOCKET,
            leftArm
        );
        leftItem.rotation = leftPose.itemRotation.slice();
        leftItem.sap_rest_translation = leftPose.itemTranslation.slice();
    }

    function createThirdPersonTemplate(slimArms, skinTexture) {
        const guide = makeGroup(PROFILE_ROOTS.third_person, [0, 24, 0], ROLE_GUIDE);
        const root = makeGroup("root", [0, 24, 0], ROLE_ANIMATED_BONE, guide);
        const body = makeGroup("body", [0, 24, 0], ROLE_ANIMATED_BONE, root);
        const bodyReference = makeGroup(
            "第三人称身体参考", body.origin, ROLE_REFERENCE, body);
        addPlayerCube(bodyReference, "SAP 玩家身体", [-4, 12, -2], [4, 24, 2], [16, 16], skinTexture);
        addPlayerCube(
            bodyReference, "SAP 玩家外套", [-4, 12, -2], [4, 24, 2],
            [16, 32], skinTexture, 0.25);

        const head = makeGroup("head", [0, 24, 0], ROLE_ANIMATED_BONE, root);
        const headReference = makeGroup(
            "第三人称头部参考", head.origin, ROLE_REFERENCE, head);
        addPlayerCube(headReference, "SAP 玩家头部", [-4, 24, -4], [4, 32, 4], [0, 0], skinTexture);
        addPlayerCube(
            headReference, "SAP 玩家帽层", [-4, 24, -4], [4, 32, 4],
            [32, 0], skinTexture, 0.5);

        const armWidth = slimArms ? 3 : 4;
        const rightArm = makeGroup("right_arm", [-5, 22, 0], ROLE_ANIMATED_BONE, root);
        const rightReference = makeGroup(
            "第三人称右臂参考", rightArm.origin, ROLE_REFERENCE, rightArm, "right");
        addPlayerArm(rightReference, false, slimArms, false, skinTexture);
        createThirdPersonItemPreviewAnchor(
            "第三人称右手物品基准",
            "main_hand_item",
            rightArm,
            "right"
        );

        const leftArm = makeGroup("left_arm", [5, 22, 0], ROLE_ANIMATED_BONE, root);
        const leftReference = makeGroup(
            "第三人称左臂参考", leftArm.origin, ROLE_REFERENCE, leftArm, "left");
        addPlayerArm(leftReference, true, slimArms, false, skinTexture);
        createThirdPersonItemPreviewAnchor(
            "第三人称左手物品基准",
            "off_hand_item",
            leftArm,
            "left"
        );

        const rightLeg = makeGroup("right_leg", [-1.9, 12, 0], ROLE_ANIMATED_BONE, root);
        const rightLegReference = makeGroup(
            "第三人称右腿参考", rightLeg.origin, ROLE_REFERENCE, rightLeg);
        addPlayerCube(
            rightLegReference, "SAP 玩家右腿", [-3.9, 0, -2], [0.1, 12, 2],
            [0, 16], skinTexture);
        addPlayerCube(
            rightLegReference, "SAP 玩家右裤层", [-3.9, 0, -2], [0.1, 12, 2],
            [0, 32], skinTexture, 0.25);

        const leftLeg = makeGroup("left_leg", [1.9, 12, 0], ROLE_ANIMATED_BONE, root);
        const leftLegReference = makeGroup(
            "第三人称左腿参考", leftLeg.origin, ROLE_REFERENCE, leftLeg);
        addPlayerCube(
            leftLegReference, "SAP 玩家左腿", [-0.1, 0, -2], [3.9, 12, 2],
            [16, 48], skinTexture);
        addPlayerCube(
            leftLegReference, "SAP 玩家左裤层", [-0.1, 0, -2], [3.9, 12, 2],
            [0, 48], skinTexture, 0.25);
    }

    function createThirdPersonItemPreviewAnchor(baseName, anchorName, arm, side) {
        const definition = THIRD_PERSON_ITEM_ANCHORS.find(
            candidate => candidate.anchor === anchorName && candidate.side === side
        );
        if (!definition) {
            throw new Error(`未知的第三人称物品锚点：${anchorName} (${side})`);
        }
        const base = makeGroup(baseName, arm.origin, ROLE_GUIDE, arm, side);
        // The preview-only X reflection on the player represents the living-entity
        // renderer transform. ItemInHandLayer's Y 180 rotation cancels that reflection
        // for item geometry, so the base receives a second preview-only X reflection.
        // The anchor itself keeps ItemInHandLayer's game-space hand offset.
        base.rotation = [-90, 0, 0];
        return makeGroup(
            anchorName,
            offsetVector(arm.origin, definition.offset),
            ROLE_GUIDE,
            base,
            side
        );
    }

    function makeGroup(name, origin, role, parent, side) {
        const group = new Group({name, origin: origin.slice()}).init();
        group.sap_role = role;
        group.sap_reference_side = side || "both";
        if (parent) group.addTo(parent);
        else group.addTo();
        return group;
    }

    function addPlayerArm(parent, left, slim, firstPerson, skinTexture) {
        const width = slim ? 3 : 4;
        const fromX = firstPerson
            ? (left ? -6 - width / 2 : 6 - width / 2)
            : (left ? 4 : -4 - width);
        const from = [fromX, firstPerson ? -9 : 12, -2];
        const to = [fromX + width, firstPerson ? 3 : 24, 2];
        const baseUv = left ? [32, 48] : [40, 16];
        const sleeveUv = left ? [48, 48] : [40, 32];
        addPlayerCube(
            parent, `SAP 玩家${left ? "左" : "右"}臂`,
            from, to, baseUv, skinTexture);
        addPlayerCube(
            parent, `SAP 玩家${left ? "左" : "右"}袖层`,
            from, to, sleeveUv, skinTexture, 0.25);
    }

    function addFirstPersonArm(parent, left, slim, armOrigin, skinTexture) {
        const width = slim ? 3 : 4;
        const pivotX = left ? 5 : -5;
        const localFromX = left ? -1 : (slim ? -2 : -3);
        const from = offsetVector(armOrigin, [pivotX + localFromX, 0, -2]);
        const to = offsetVector(from, [width, 12, 4]);
        const baseUv = left ? [32, 48] : [40, 16];
        const sleeveUv = left ? [48, 48] : [40, 32];
        addPlayerCube(
            parent, `SAP 玩家${left ? "左" : "右"}臂`,
            from, to, baseUv, skinTexture, 0, true);
        addPlayerCube(
            parent, `SAP 玩家${left ? "左" : "右"}袖层`,
            from, to, sleeveUv, skinTexture, 0.25, true);
    }

    function handleProjectParsed() {
        if (!isSapProject()) return [];
        const parsedProject = Project;
        const profile = inferProfileFromSavedVisibility();
        const refreshed = refreshPreviewReferences(false);
        if (profile) {
            ensureParsedProfileAvailable(profile);
            activateProfile(profile);
        }
        scheduleParsedPreviewRefresh(parsedProject);
        return refreshed;
    }

    function sanitizeSapEditorState(event) {
        if (!isSapProject()
                || !event
                || !event.model
                || !event.model.editor_state) {
            return;
        }
        const editorState = event.model.editor_state;
        if (!Array.isArray(editorState.selected_elements)) return;
        const referenceUuids = new Set(
            Outliner.elements
                .filter(isPreviewReferenceElement)
                .map(element => element.uuid)
        );
        editorState.selected_elements = editorState.selected_elements.filter(
            uuid => !referenceUuids.has(uuid)
        );
    }

    function scheduleParsedPreviewRefresh(parsedProject) {
        const token = ++parsedRefreshToken;
        setTimeout(() => {
            if (token !== parsedRefreshToken
                    || Project !== parsedProject
                    || !isSapProject()) {
                return;
            }
            refreshPreviewReferences(false);
            removePreviewReferencesFromSelection();
            if (typeof UVEditor !== "undefined"
                    && typeof UVEditor.loadData === "function") {
                UVEditor.loadData();
            }
        }, 0);
    }

    function removePreviewReferencesFromSelection() {
        if (!Array.isArray(Project.selected_elements)) return;
        const retained = Project.selected_elements.filter(
            element => element && !isPreviewReferenceElement(element)
        );
        if (retained.length === Project.selected_elements.length) return;
        Project.selected_elements.splice(
            0,
            Project.selected_elements.length,
            ...retained
        );
        if (typeof UVEditor !== "undefined" && UVEditor.vue) {
            UVEditor.vue.elements = Project.selected_elements;
        }
    }

    function isPreviewReferenceElement(element) {
        if (!(element instanceof Cube)) return false;
        if (element.sap_player_reference) return true;
        let parent = element.parent;
        while (parent instanceof Group) {
            if (parent.sap_role === ROLE_REFERENCE
                    || parent.sap_role === ROLE_GUIDE) {
                return true;
            }
            parent = parent.parent;
        }
        return false;
    }

    function ensureParsedProfileAvailable(profile) {
        let profiles;
        try {
            profiles = parseProfiles(Project.sap_profiles || "");
        } catch (error) {
            profiles = [];
        }
        if (!profiles.includes(profile)) {
            profiles.push(profile);
            Project.sap_profiles = profiles.join(",");
        }
    }

    function inferProfileFromSavedVisibility() {
        const roots = Object.entries(PROFILE_ROOTS)
            .map(([profile, name]) => ({
                profile,
                root: Group.all.find(
                    group => group.name === name && group.sap_role === ROLE_GUIDE)
            }))
            .filter(entry => entry.root);
        if (roots.length === 1) return roots[0].profile;

        const visibleRoots = roots.filter(entry => entry.root.visibility !== false);
        if (visibleRoots.length === 1) return visibleRoots[0].profile;

        let profiles;
        try {
            profiles = parseProfiles(Project.sap_profiles || "");
        } catch (error) {
            profiles = [];
        }
        const activeProfile = String(Project.sap_active_profile || "")
            .trim()
            .toLowerCase();
        if (profiles.includes(activeProfile)) return activeProfile;
        return profiles[0] || (roots[0] && roots[0].profile) || null;
    }

    function refreshPreviewReferences(refreshProfile = true) {
        const refreshedSkins = refreshEmbeddedReferenceSkins();
        const refreshedPositions = refreshThirdPersonPreviewPositions();
        const refreshedArmUvs = refreshFirstPersonArmUvs();
        const refreshedPreviewUvs = refreshPreviewItemFaceUvs();
        if (refreshProfile && isSapProject()) {
            applyProfileVisibility();
            applyProfileTransformSpace(Project.sap_active_profile);
        }
        return refreshedSkins.concat(
            refreshedPositions,
            refreshedArmUvs,
            refreshedPreviewUvs
        );
    }

    function refreshThirdPersonPreviewPositions() {
        if (!isSapProject()) return [];
        const repaired = [];
        THIRD_PERSON_ITEM_ANCHORS.forEach(definition => {
            const arm = Group.all.find(group =>
                group.name === definition.arm
                    && group.sap_role === ROLE_ANIMATED_BONE);
            const anchor = Group.all.find(group =>
                group.name === definition.anchor
                    && group.sap_role === ROLE_GUIDE);
            if (!arm || !anchor) return;

            const base = anchor.parent;
            if (base instanceof Group && !vectorsEqual(base.origin, arm.origin)) {
                translateReferenceTree(base, subtractVector(arm.origin, base.origin));
                repaired.push(base);
            }

            const expectedAnchor = offsetVector(arm.origin, definition.offset);
            if (!vectorsEqual(anchor.origin, expectedAnchor)) {
                translateReferenceTree(anchor, subtractVector(expectedAnchor, anchor.origin));
                repaired.push(anchor);
            }

            anchor.children
                .filter(child => child instanceof Group
                    && child.sap_role === ROLE_REFERENCE
                    && child.sap_held_item_hand === definition.side
                    && child.sap_item_display_context
                        === `thirdperson_${definition.side}hand`)
                .forEach(reference => {
                    const model = previewItemDefinition(reference.sap_item_source);
                    if (!model) return;
                    const display = displayForContext(
                        model.flattened.display,
                        reference.sap_item_display_context
                    );
                    const transform = normalizeDisplay(
                        display,
                        definition.side === "left"
                    );
                    const expectedOrigin = offsetVector(anchor.origin, transform.translation);
                    if (!vectorsEqual(reference.origin, expectedOrigin)) {
                        translateReferenceTree(
                            reference,
                            subtractVector(expectedOrigin, reference.origin)
                        );
                        repaired.push(reference);
                    }
                    if (!vectorsEqual(reference.rotation, transform.rotation)) {
                        reference.rotation = transform.rotation;
                        repaired.push(reference);
                    }
                    if (reference.sap_item_uses_mesh_scale
                            && !vectorsEqual(
                                reference.sap_item_display_scale,
                                transform.scale
                            )) {
                        reference.sap_item_display_scale = transform.scale;
                        repaired.push(reference);
                    }
                });
        });
        if (repaired.length) Canvas.updateAll();
        return repaired;
    }

    function previewItemDefinition(modelPath) {
        try {
            const fs = require("fs");
            const path = require("path");
            if (!modelPath || !fs.existsSync(modelPath)) return null;
            return loadPreviewItemDefinition(fs, path, modelPath);
        } catch (error) {
            return null;
        }
    }

    function refreshPreviewItemFaceUvs() {
        if (!isSapProject()) return [];
        const repaired = [];
        Group.all
            .filter(group => group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none"
                && /^(first|third)person_/.test(String(group.sap_item_display_context)))
            .forEach(group => {
                const definition = previewItemDefinition(group.sap_item_source);
                if (!definition) return;
                const flattened = definition.flattened;
                const elements = definition.elements;
                const cubes = collectTreeNodes(group).filter(
                    child => child instanceof Cube
                );
                const cubesByElementId = new Map(cubes
                    .filter(cube => cube.sap_item_element_id)
                    .map(cube => [cube.sap_item_element_id, cube]));
                elements.forEach((element, index) => {
                    const elementId = previewElementId(element, index, flattened.bbmodel);
                    const cube = cubesByElementId.get(elementId) || cubes[index];
                    if (!cube) return;
                    let changed = false;
                    if (cube.box_uv) {
                        cube.box_uv = false;
                        changed = true;
                    }
                    if (cube.autouv !== 0) {
                        cube.autouv = 0;
                        changed = true;
                    }
                    Object.entries(element.faces || {}).forEach(
                        ([direction, source]) => {
                            const face = cube.faces[direction];
                            if (!face) return;
                            if (Array.isArray(source.uv)) {
                                const expected = flattened.bbmodel
                                    ? numericVector(
                                        source.uv,
                                        [0, 0, 0, 0],
                                        "BBModel UV"
                                    )
                                    : minecraftUvToTextureUv(
                                        source.uv,
                                        flattened.texture_size
                                    );
                                if (!vectorsEqual(face.uv, expected)) {
                                    face.uv = expected;
                                    changed = true;
                                }
                            }
                            const expectedTexture = flattened.bbmodel
                                ? bbmodelTexture(
                                    definition.textures,
                                    source.texture
                                )
                                : definition.textures[resolveTextureKey(
                                    flattened.textures || {},
                                    source.texture
                                )];
                            if (expectedTexture
                                    && face.texture !== expectedTexture.uuid) {
                                face.texture = expectedTexture.uuid;
                                changed = true;
                            }
                        }
                    );
                    if (!changed) return;
                    Cube.preview_controller.updateUV(cube);
                    repaired.push(cube);
                });
            });
        if (repaired.length) {
            Canvas.updateView({
                elements: repaired,
                element_aspects: {uv: true}
            });
        }
        return repaired;
    }

    function setTextureUvSize(texture, size) {
        const normalized = normalizeTextureSize(size);
        const oldWidth = positiveNumber(
            texture.uv_width,
            Project.texture_width || 16
        );
        const oldHeight = positiveNumber(
            texture.uv_height,
            Project.texture_height || 16
        );
        if (Math.abs(oldWidth - normalized[0]) < 1.0e-6
                && Math.abs(oldHeight - normalized[1]) < 1.0e-6) {
            return [];
        }
        const affected = [];
        Cube.all.forEach(cube => {
            let changed = false;
            Object.values(cube.faces).forEach(face => {
                if (face.texture !== texture.uuid) return;
                face.uv[0] *= normalized[0] / oldWidth;
                face.uv[2] *= normalized[0] / oldWidth;
                face.uv[1] *= normalized[1] / oldHeight;
                face.uv[3] *= normalized[1] / oldHeight;
                changed = true;
            });
            if (changed) affected.push(cube);
        });
        texture.uv_width = normalized[0];
        texture.uv_height = normalized[1];
        return affected;
    }

    function normalizeTextureSize(size) {
        return [
            positiveNumber(size && size[0], 16),
            positiveNumber(size && size[1], 16)
        ];
    }

    function positiveNumber(value, fallback) {
        const number = Number(value);
        return Number.isFinite(number) && number > 0 ? number : fallback;
    }

    function translateReferenceTree(node, delta) {
        if (node instanceof Group) {
            node.origin = offsetVector(node.origin, delta);
        }
        if (node instanceof Cube) {
            node.from = offsetVector(node.from, delta);
            node.to = offsetVector(node.to, delta);
            node.origin = offsetVector(node.origin, delta);
        }
        if (Array.isArray(node.children)) {
            node.children.forEach(child => translateReferenceTree(child, delta));
        }
    }

    function refreshFirstPersonArmUvs() {
        if (!isSapProject()) return [];
        const repaired = [];
        for (const name of [
            "第一人称右臂参考",
            "第一人称左臂参考"
        ]) {
            const reference = Group.all.find(
                group => group.name === name && group.sap_role === ROLE_REFERENCE
            );
            if (!reference) continue;
            reference.children
                .filter(child => child instanceof Cube && child.sap_player_reference)
                .forEach(cube => {
                    const relativeFromY = cube.from[1] - reference.origin[1];
                    const relativeToY = cube.to[1] - reference.origin[1];
                    let changed = false;
                    if (Math.abs(relativeFromY + 10) < 1.0e-4
                            && Math.abs(relativeToY - 2) < 1.0e-4) {
                        cube.from[1] = reference.origin[1] - 2;
                        cube.to[1] = reference.origin[1] + 10;
                        changed = true;
                    }
                    const expected = minecraftModelPartUvs(cube, cube.uv_offset);
                    if (cube.box_uv || Object.entries(expected).some(
                        ([face, uv]) => !vectorsEqual(cube.faces[face].uv, uv)
                    )) {
                        applyMinecraftModelPartUvs(cube, expected);
                        changed = true;
                    }
                    if (changed) repaired.push(cube);
                });
        }
        if (repaired.length) {
            Canvas.updateView({
                elements: repaired,
                element_aspects: {geometry: true, uv: true}
            });
        }
        return repaired;
    }

    function minecraftModelPartUvs(cube, uvOffset) {
        const [width, height, depth] = cube.size();
        const u0 = uvOffset[0];
        const u1 = u0 + depth;
        const u2 = u1 + width;
        const u22 = u2 + width;
        const u3 = u2 + depth;
        const u4 = u3 + width;
        const v0 = uvOffset[1];
        const v1 = v0 + depth;
        const v2 = v1 + height;
        return {
            down: [u1, v0, u2, v1],
            up: [u2, v1, u22, v0],
            west: [u1, v2, u0, v1],
            north: [u2, v2, u1, v1],
            east: [u3, v2, u2, v1],
            south: [u4, v2, u3, v1]
        };
    }

    function applyMinecraftModelPartUvs(cube, faceUvs) {
        cube.box_uv = false;
        Object.entries(faceUvs).forEach(([face, uv]) => {
            cube.faces[face].uv = uv.slice();
            cube.faces[face].rotation = 0;
        });
    }

    function vectorsEqual(first, second) {
        return first.length === second.length
            && first.every((value, index) => Math.abs(Number(value) - Number(second[index])) < 1.0e-4);
    }

    function offsetVector(first, second) {
        return [0, 1, 2].map(axis => Number(first[axis]) + Number(second[axis]));
    }

    function subtractVector(first, second) {
        return [0, 1, 2].map(axis => Number(first[axis]) - Number(second[axis]));
    }

    function addPlayerCube(
        parent, name, from, to, uvOffset, skinTexture, inflate, useMinecraftModelPartUvs
    ) {
        const cube = new Cube({
            name,
            from: from.slice(),
            to: to.slice(),
            box_uv: true,
            uv_offset: uvOffset.slice(),
            autouv: 0,
            inflate: Number(inflate || 0)
        }).addTo(parent).init();
        cube.sap_player_reference = true;
        assignTexture(cube, skinTexture);
        if (useMinecraftModelPartUvs) {
            applyMinecraftModelPartUvs(cube, minecraftModelPartUvs(cube, uvOffset));
            Cube.preview_controller.updateUV(cube);
        }
        return cube;
    }

    function activateProfile(profile) {
        const presets = {
            first_person: {
                projection: "perspective",
                position: [0, 0, 0],
                target: [0, 0, -16],
                fov: 70,
                aspect_ratio: 16 / 9
            },
            third_person: {
                projection: "perspective",
                // The preview root mirrors Minecraft's renderer handedness. From
                // the front, the anatomical right hand therefore appears on the
                // viewport's left, as it does on a real front-facing person.
                position: [-38, 22, -52],
                target: [0, 20, 0],
                fov: 45,
                aspect_ratio: 16 / 9
            }
        };
        const preset = presets[profile];
        if (!preset) throw new Error(`未知的 SAP 人称配置：${profile}`);
        if (!parseProfiles(Project.sap_profiles || "").includes(profile)) {
            throw new Error(`当前 SAP 项目不包含 ${profile} 工作区`);
        }
        Project.sap_active_profile = profile;
        applyProfileVisibility();
        applyProfileTransformSpace(profile);
        const preview = Preview.selected;
        if (preview && typeof preview.loadAnglePreset === "function") {
            preview.loadAnglePreset(preset);
        } else if (preview) {
            preview.camera.position.fromArray(preset.position);
            preview.controls.target.fromArray(preset.target);
            preview.camera.fov = preset.fov;
            preview.camera.updateProjectionMatrix();
        }
        synchronizeFirstPersonHud();
    }

    function applyProfileTransformSpace(profile) {
        if (profile !== "first_person" || typeof BarItems === "undefined") return;
        for (const itemName of [
            "transform_space",
            "rotation_space",
            "transform_pivot_space"
        ]) {
            const item = BarItems[itemName];
            if (!item || typeof item.change !== "function" || item.value === "global") continue;
            item.change("global");
        }
    }

    function toggleLeftHand() {
        setLeftHandVisibility(!Project.sap_show_left_hand);
        Blockbench.showQuickMessage(
            Project.sap_show_left_hand
                ? "已显示第一人称左手参考"
                : "已隐藏第一人称左手参考",
            1800
        );
    }

    function setLeftHandVisibility(visible) {
        Project.sap_show_left_hand = !!visible;
        applyProfileVisibility();
    }

    function toggleFirstPersonHud() {
        setFirstPersonHudVisibility(Project.sap_show_first_person_hud === false);
        Blockbench.showQuickMessage(
            Project.sap_show_first_person_hud
                ? "已显示第一人称基础 HUD"
                : "已隐藏第一人称基础 HUD",
            1800
        );
    }

    function setFirstPersonHudVisibility(visible) {
        Project.sap_show_first_person_hud = !!visible;
        synchronizeFirstPersonHud();
    }

    function applyProfileVisibility() {
        const visibility = new Map();
        Object.entries(PROFILE_ROOTS).forEach(([entryProfile, name]) => {
            const root = Group.all.find(group => group.name === name && group.sap_role === ROLE_GUIDE);
            if (root) {
                collectTreeNodes(root).forEach(
                    node => visibility.set(node, entryProfile === Project.sap_active_profile)
                );
            }
        });
        if (!Project.sap_show_left_hand) {
            const firstPersonRoot = Group.all.find(group =>
                group.name === PROFILE_ROOTS.first_person
                    && group.sap_role === ROLE_GUIDE);
            const firstPersonNodes = new Set(
                firstPersonRoot ? collectTreeNodes(firstPersonRoot) : []
            );
            Group.all
                .filter(group => group.sap_reference_side === "left"
                    && firstPersonNodes.has(group))
                .forEach(group => collectTreeNodes(group).forEach(node => visibility.set(node, false)));
        }
        hideFirstPersonArmsWithHeldItems(visibility);

        const elements = [];
        const groups = [];
        visibility.forEach((visible, node) => {
            node.visibility = visible;
            if (node instanceof Group) groups.push(node);
            else elements.push(node);
        });

        if (typeof Canvas.updateView === "function") {
            Canvas.updateView({
                elements,
                element_aspects: {visibility: true},
                groups,
                group_aspects: {visibility: true}
            });
        } else {
            Canvas.updateVisibility();
        }
        applyThirdPersonPreviewHandedness();

        // Group's preview controller does not synchronize scene_object.visible.
        // Keep it explicit so loaded projects with stale child state are repaired.
        visibility.forEach((visible, node) => {
            const sceneObject = node.mesh || node.scene_object;
            if (sceneObject) sceneObject.visible = visible;
        });
        if (globalThis.Outliner?.vue && typeof Outliner.vue.$forceUpdate === "function") {
            Outliner.vue.$forceUpdate();
        }
        synchronizeFirstPersonHud();
    }

    function applyThirdPersonPreviewHandedness() {
        applyPreviewItemDisplayScales();
        const root = Group.all.find(group =>
            group.name === PROFILE_ROOTS.third_person
                && group.sap_role === ROLE_GUIDE);
        if (!root?.mesh) return;

        // Animator.showDefaultPose() resets every group scale before each preview
        // frame. The display_default_pose hook calls this function immediately after
        // that reset so these editor-only coordinate-frame corrections persist.
        root.mesh.scale.x = -1;
        for (const definition of THIRD_PERSON_ITEM_ANCHORS) {
            const anchor = Group.all.find(group =>
                group.name === definition.anchor
                    && group.sap_role === ROLE_GUIDE);
            const itemBase = anchor?.parent;
            if (itemBase instanceof Group && itemBase.mesh) {
                // ItemInHandLayer's Y 180 rotation cancels the living renderer's
                // X reflection for item geometry while retaining the hand offset.
                itemBase.mesh.scale.x = -1;
            }
        }
        root.mesh.updateMatrixWorld(true);
    }

    function applyPreviewItemDisplayScales() {
        Group.all
            .filter(group => group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none"
                && group.sap_item_uses_mesh_scale
                && group.mesh)
            .forEach(group => {
                const scale = vectorOr(
                    group.sap_item_display_scale,
                    [1, 1, 1],
                    `${group.name} 的预览 display 缩放`
                );
                group.mesh.scale.set(
                    scale[0] || 0.001,
                    scale[1] || 0.001,
                    scale[2] || 0.001
                );
                group.mesh.updateMatrixWorld(true);
            });
    }

    function hideFirstPersonArmsWithHeldItems(visibility) {
        for (const definition of [
            {
                hand: "right",
                socketName: "first_person_right_item",
                armReferenceName: "第一人称右臂参考"
            },
            {
                hand: "left",
                socketName: "first_person_left_item",
                armReferenceName: "第一人称左臂参考"
            }
        ]) {
            const socket = Group.all.find(
                group => group.name === definition.socketName && group.sap_role === ROLE_SOCKET);
            const hasHeldItem = socket?.children.some(
                child => child.sap_role === ROLE_REFERENCE
                    && child.sap_held_item_hand === definition.hand
                    && String(child.sap_item_display_context).startsWith("firstperson_")
            );
            if (!hasHeldItem) continue;
            const armReference = Group.all.find(
                group => group.name === definition.armReferenceName
                    && group.sap_role === ROLE_REFERENCE
            );
            if (armReference) {
                collectTreeNodes(armReference).forEach(node => visibility.set(node, false));
            }
        }
    }

    function collectTreeNodes(root) {
        const nodes = [root];
        if (typeof root.forEachChild === "function") {
            root.forEachChild(node => nodes.push(node));
            return nodes;
        }
        const pending = Array.from(root.children || []);
        while (pending.length) {
            const node = pending.shift();
            nodes.push(node);
            pending.push(...Array.from(node.children || []));
        }
        return nodes;
    }

    function showPlayerSkinDialog() {
        Blockbench.import({
            resource_id: "sap_player_skin",
            extensions: ["png"],
            type: "64x64 PNG 玩家皮肤",
            multiple: false
        }, files => {
            if (!files || !files.length) return;
            loadPlayerSkin(files[0].path);
        });
    }

    function loadPlayerSkin(filePath) {
        const fs = require("fs");
        const path = require("path");
        if (typeof filePath !== "string" || !filePath.trim() || !fs.existsSync(filePath.trim())) {
            throw new Error("玩家皮肤路径必须指向一个存在的 PNG 文件");
        }
        if (path.extname(filePath).toLowerCase() !== ".png") {
            throw new Error("玩家皮肤必须是 PNG 文件");
        }
        const image = fs.readFileSync(filePath);
        const pngSignature = image.subarray(0, 8).toString("hex");
        if (pngSignature !== "89504e470d0a1a0a" || image.length < 24) {
            throw new Error("玩家皮肤不是有效的 PNG 文件");
        }
        const width = image.readUInt32BE(16);
        const height = image.readUInt32BE(20);
        if (width !== 64 || height !== 64) {
            throw new Error(`玩家皮肤必须为 64x64 像素，当前为 ${width}x${height}`);
        }
        const texture = new Texture({
            name: path.basename(filePath),
            path: filePath
        }).fromPath(filePath).add(false);
        setTextureUvSize(texture, [64, 64]);
        texture.sap_player_skin = true;
        applyPlayerTexture(texture);
        Blockbench.showQuickMessage(`已加载玩家皮肤：${texture.name}`, 2500);
        return texture;
    }

    function createReferenceSkinTexture(playerModel) {
        const normalized = normalizePlayerModel(playerModel);
        const skin = VANILLA_PLAYER_SKINS[normalized];
        const texture = new Texture({name: skin.name})
            .fromDataURL(`data:image/png;base64,${skin.data}`)
            .add(false);
        setTextureUvSize(texture, [64, 64]);
        return markEmbeddedReferenceSkin(texture, normalized);
    }

    function refreshEmbeddedReferenceSkins() {
        if (!isSapProject()) return [];
        const repaired = [];
        Texture.all.forEach(texture => {
            const playerModel = PLAYER_MODELS.find(model => {
                const skin = VANILLA_PLAYER_SKINS[model];
                const source = `data:image/png;base64,${skin.data}`;
                return texture.source === source
                    || (texture.internal
                        && texture.name === `sap_vanilla_${model}.png`);
            });
            if (!playerModel) return;
            const skin = VANILLA_PLAYER_SKINS[playerModel];
            const changed = texture.name !== skin.name || texture.saved !== true;
            markEmbeddedReferenceSkin(texture, playerModel);
            if (changed) repaired.push(texture);
        });
        return repaired;
    }

    function markEmbeddedReferenceSkin(texture, playerModel) {
        const normalized = normalizePlayerModel(playerModel);
        texture.name = VANILLA_PLAYER_SKINS[normalized].name;
        texture.saved = true;
        texture.sap_player_skin = true;
        texture.sap_vanilla_player_model = normalized;
        return texture;
    }

    function normalizePlayerModel(playerModel) {
        if (playerModel == null || String(playerModel).trim() === "") {
            return "steve";
        }
        const normalized = String(playerModel).trim().toLowerCase();
        if (!PLAYER_MODELS.includes(normalized)) {
            throw new Error(`不支持的原版玩家参考模型：${playerModel}`);
        }
        return normalized;
    }

    function applyPlayerTexture(texture) {
        Cube.all.filter(cube => cube.sap_player_reference).forEach(cube => assignTexture(cube, texture));
        Canvas.updateAll();
    }

    function assignTexture(cube, texture) {
        Object.values(cube.faces).forEach(face => {
            face.texture = texture.uuid;
        });
    }

    function showPreviewItemDialog() {
        const dialog = new Dialog({
            id: "sap_set_preview_item",
            title: "设置 SAP 预览物品",
            form: {
                hand: {
                    label: "手",
                    type: "select",
                    options: {
                        right: "右手",
                        left: "左手"
                    },
                    value: "right"
                }
            },
            onConfirm(form) {
                dialog.hide();
                Blockbench.import({
                    resource_id: "sap_preview_item",
                    extensions: ["json", "bbmodel"],
                    type: "Minecraft 物品模型 JSON / Blockbench BBModel",
                    multiple: false
                }, files => {
                    if (!files || !files.length) return;
                    const file = files[0];
                    setPreviewItem(file.path, form.hand);
                });
            }
        });
        dialog.show();
    }

    function showClearPreviewItemDialog() {
        const dialog = new Dialog({
            id: "sap_clear_preview_item",
            title: "清除 SAP 预览物品",
            form: {
                hand: {
                    label: "手",
                    type: "select",
                    options: {
                        right: "右手",
                        left: "左手"
                    },
                    value: "right"
                }
            },
            onConfirm(form) {
                dialog.hide();
                const removed = clearPreviewItems(form.hand);
                Blockbench.showQuickMessage(
                    removed
                        ? `已清除${handLabel(form.hand)}预览物品`
                        : `${handLabel(form.hand)}没有可清除的预览物品`,
                    2000
                );
            }
        });
        dialog.show();
    }

    function setPreviewItemFromApi(filePath, options) {
        if (!isSapProject()) {
            throw new Error("请先打开一个 SAP 使用动画项目，再设置预览物品");
        }
        if (typeof filePath !== "string" || !filePath.trim()) {
            throw new Error("预览物品模型路径不能为空");
        }
        return setPreviewItem(filePath.trim(), previewItemHand(options));
    }

    function clearPreviewItemsFromApi(options) {
        if (!isSapProject()) {
            throw new Error("请先打开一个 SAP 使用动画项目，再清除预览物品");
        }
        return clearPreviewItems(previewItemHand(options));
    }

    function previewItemHand(options) {
        const hand = String(options && options.hand || "").trim().toLowerCase();
        if (!["left", "right"].includes(hand)) {
            throw new Error("预览物品的手必须是 left 或 right");
        }
        return hand;
    }

    function handLabel(hand) {
        return hand === "left" ? "左手" : "右手";
    }

    function setPreviewItem(filePath, hand) {
        const fs = require("fs");
        const path = require("path");
        refreshThirdPersonPreviewPositions();
        const targets = previewItemTargets(hand);
        if (!targets.length) {
            throw new Error(
                `当前 SAP 项目没有${handLabel(hand)}预览锚点`
            );
        }
        const definition = loadPreviewItemDefinition(
            fs,
            path,
            filePath,
            false
        );
        const {
            modelPath,
            flattened,
            elements
        } = definition;
        if (!elements.length) {
            throw new Error(`模型 ${modelPath} 没有可用于预览的立方体元素`);
        }

        Undo.initEdit({outliner: true, textures: []});
        removePreviewItems(targets, hand);
        const textures = definition.loadTextures();
        const imported = {};
        targets.forEach(target => {
            imported[target.profile] = instantiatePreviewItem(
                path,
                modelPath,
                flattened,
                elements,
                textures,
                target,
                hand
            );
        });
        Undo.finishEdit(`设置 SAP ${handLabel(hand)}预览物品`);
        Canvas.updateAll();
        applyProfileVisibility();
        const selectedTarget = targets.find(
            target => target.profile === Project.sap_active_profile) || targets[0];
        selectedTarget.animationTarget.select();
        Blockbench.showQuickMessage(
            `已将 ${path.basename(modelPath, path.extname(modelPath))} 设置为`
                + `${targets.length} 个工作区中的${handLabel(hand)}非导出预览；`
                + `请制作 ${selectedTarget.animationTarget.name} 的动画`,
            2500
        );
        return {
            hand,
            firstPerson: imported.first_person || null,
            thirdPerson: imported.third_person || null,
            roots: Object.values(imported)
        };
    }

    function loadPreviewItemDefinition(
        fs,
        path,
        filePath,
        includeTextures = true
    ) {
        const source = readJson(fs, filePath);
        if (isBbmodelSource(source, path, filePath)) {
            return loadBbmodelDefinition(
                fs,
                path,
                filePath,
                source,
                includeTextures
            );
        }

        const inferredRoot = inferResourceRoot(path, filePath);
        const roots = configuredRoots(path);
        if (inferredRoot && !roots.includes(inferredRoot)) roots.unshift(inferredRoot);
        const initialNamespace = inferNamespace(path, filePath)
            || Project.sap_namespace
            || "minecraft";
        let model = source;
        let modelPath = filePath;
        if (source.model && typeof source.model === "object") {
            if (!source.model.model) {
                throw new Error(
                    `不支持 MC 26 物品模型分派器 ${source.model.type || "<未知>"}；`
                    + "请选择一个具体的 models/item 模型 JSON 或 BBModel 作为预览"
                );
            }
            const resolved = resolveAsset(
                path,
                roots,
                source.model.model,
                "models",
                initialNamespace
            );
            if (!resolved) throw new Error(`无法解析物品模型：${source.model.model}`);
            modelPath = resolved;
            model = readJson(fs, resolved);
        }
        const flattened = flattenJavaModel(
            fs,
            path,
            roots,
            model,
            modelPath,
            initialNamespace,
            new Set()
        );
        const elements = flattened.elements && flattened.elements.length
            ? flattened.elements
            : flattened.generated_item
                ? generatedItemElements(flattened.textures || {})
                : [];
        const loadTextures = () => loadModelTextures(
            fs,
            path,
            roots,
            flattened.textures || {},
            initialNamespace,
            flattened.texture_size
        );
        return {
            modelPath,
            flattened,
            elements,
            textures: includeTextures ? loadTextures() : {},
            loadTextures
        };
    }

    function isBbmodelSource(source, path, filePath) {
        return path.extname(filePath).toLowerCase() === ".bbmodel"
            || !!(source
                && source.meta
                && source.meta.format_version
                && Array.isArray(source.elements)
                && Array.isArray(source.outliner));
    }

    function loadBbmodelDefinition(
        fs,
        path,
        modelPath,
        source,
        includeTextures
    ) {
        if (!source.meta || !Array.isArray(source.elements)) {
            throw new Error(`BBModel ${modelPath} 缺少 meta 或 elements`);
        }
        const unsupported = source.elements.filter(element =>
            element
                && element.export !== false
                && element.type
                && element.type !== "cube"
        );
        if (unsupported.length) {
            const types = [...new Set(unsupported.map(
                element => String(element.type || "<未知>")
            ))];
            throw new Error(
                `BBModel ${modelPath} 包含不支持的预览元素：${types.join(", ")}；`
                + "当前仅支持立方体元素"
            );
        }
        const elements = source.elements.filter(element =>
            element && element.export !== false && (!element.type || element.type === "cube")
        );
        const resolution = source.resolution || {};
        const modelFormat = String(source.meta.model_format || "");
        const flattened = {
            bbmodel: true,
            display: source.display || {},
            elements,
            groups: Array.isArray(source.groups) ? source.groups : [],
            outliner: Array.isArray(source.outliner) ? source.outliner : [],
            texture_size: [
                positiveNumber(resolution.width, 16),
                positiveNumber(resolution.height, 16)
            ],
            model_center: modelFormat === "java_block" ? [8, 8, 8] : [0, 0, 0]
        };
        const textureEntries = Array.isArray(source.textures)
            ? source.textures
            : [];
        const loadTextures = () => loadBbmodelTextures(
            fs,
            path,
            modelPath,
            textureEntries,
            flattened.texture_size
        );
        return {
            modelPath,
            flattened,
            elements,
            textures: includeTextures ? loadTextures() : {},
            loadTextures
        };
    }

    function clearPreviewItems(hand) {
        const targets = previewItemTargets(hand);
        Undo.initEdit({outliner: true});
        const removed = removePreviewItems(targets, hand);
        Undo.finishEdit(`清除 SAP ${handLabel(hand)}预览物品`);
        if (removed) {
            Canvas.updateAll();
            applyProfileVisibility();
        }
        return removed;
    }

    function removePreviewItems(targets, hand) {
        let removed = 0;
        targets.forEach(target => {
            [...target.parent.children]
                .filter(child => child instanceof Group
                    && child.sap_role === ROLE_REFERENCE
                    && child.sap_held_item_hand === hand
                    && child.sap_item_display_context === target.context)
                .forEach(child => {
                    child.remove();
                    removed++;
                });
        });
        return removed;
    }

    function previewItemTargets(hand) {
        const suffix = hand === "right" ? "righthand" : "lefthand";
        const definitions = [
            {
                profile: "first_person",
                anchor: hand === "right" ? "first_person_right_item" : "first_person_left_item",
                anchorRole: ROLE_SOCKET,
                animationTarget: hand === "right"
                    ? "first_person_right_arm"
                    : "first_person_left_arm",
                context: `firstperson_${suffix}`
            },
            {
                profile: "third_person",
                anchor: hand === "right" ? "main_hand_item" : "off_hand_item",
                anchorRole: ROLE_GUIDE,
                animationTarget: hand === "right" ? "right_arm" : "left_arm",
                context: `thirdperson_${suffix}`
            }
        ];
        return definitions
            .map(definition => Object.assign(
                {}, definition, {
                    parent: Group.all.find(
                        group => group.name === definition.anchor
                            && group.sap_role === definition.anchorRole),
                    animationTarget: Group.all.find(
                        group => group.name === definition.animationTarget
                            && group.sap_role === ROLE_ANIMATED_BONE)
                }
            ))
            .filter(definition => definition.parent && definition.animationTarget);
    }

    function instantiatePreviewItem(
        path,
        modelPath,
        flattened,
        elements,
        textures,
        target,
        hand
    ) {
        const anchor = target.parent.origin.slice();
        const display = displayForContext(flattened.display, target.context);
        const transform = normalizeDisplay(display, hand === "left");
        const pivot = offsetVector(anchor, transform.translation);
        const modelName = path.basename(modelPath, path.extname(modelPath));
        const root = makeGroup(
            `${modelName} (${target.context})`,
            pivot,
            ROLE_REFERENCE,
            target.parent,
            hand
        );
        root.sap_held_item_hand = hand;
        root.sap_item_display_context = target.context;
        root.sap_item_source = modelPath;
        root.sap_item_display_scale = transform.scale;
        root.sap_item_uses_mesh_scale = true;
        root.rotation = transform.rotation;
        const geometryTransform = Object.assign({}, transform, {
            scale: [1, 1, 1]
        });

        if (flattened.bbmodel) {
            instantiateBbmodelHierarchy(
                root,
                flattened,
                elements,
                textures,
                geometryTransform,
                pivot,
                hand
            );
            return root;
        }

        elements.forEach((element, index) => {
            const from = transformPoint(
                element.from || [0, 0, 0],
                geometryTransform,
                pivot
            );
            const to = transformPoint(
                element.to || [0, 0, 0],
                geometryTransform,
                pivot
            );
            const rotation = element.rotation || {};
            const cube = new Cube({
                name: element.name || `element_${index}`,
                from: from.map((value, axis) => Math.min(value, to[axis])),
                to: to.map((value, axis) => Math.max(value, from[axis])),
                origin: transformPoint(
                    rotation.origin || [8, 8, 8],
                    geometryTransform,
                    pivot
                ),
                rotation: axisRotation(rotation.axis, Number(rotation.angle || 0)),
                box_uv: false,
                autouv: 0,
                inflate: Number(element.inflate || 0)
            }).addTo(root).init();
            cube.sap_item_element_id = previewElementId(element, index, false);
            applyFaces(
                cube,
                element.faces || {},
                flattened.textures || {},
                textures,
                flattened.texture_size
            );
            Cube.preview_controller.updateUV(cube);
        });
        return root;
    }

    function instantiateBbmodelHierarchy(
        root,
        flattened,
        elements,
        textures,
        transform,
        pivot,
        hand
    ) {
        const elementByUuid = new Map(elements
            .filter(element => element.uuid)
            .map(element => [element.uuid, element]));
        const elementIndices = new Map(elements.map(
            (element, index) => [element, index]
        ));
        const groupByUuid = new Map(flattened.groups
            .filter(group => group && group.uuid)
            .map(group => [group.uuid, group]));
        const instantiated = new Set();
        const referencedElementUuids = new Set();

        function collectReferencedElements(node) {
            if (typeof node === "string") {
                if (elementByUuid.has(node)) referencedElementUuids.add(node);
                return;
            }
            if (!node || typeof node !== "object") return;
            if (node.uuid && elementByUuid.has(node.uuid)) {
                referencedElementUuids.add(node.uuid);
                return;
            }
            const template = node.uuid && groupByUuid.get(node.uuid) || node;
            const children = Array.isArray(node.children)
                ? node.children
                : Array.isArray(template.children)
                    ? template.children
                    : [];
            children.forEach(collectReferencedElements);
        }
        flattened.outliner.forEach(collectReferencedElements);

        function addElement(element, parent) {
            if (!element || instantiated.has(element) || element.export === false) return;
            const index = elementIndices.get(element);
            const from = transformPoint(
                element.from || [0, 0, 0],
                transform,
                pivot,
                flattened.model_center
            );
            const to = transformPoint(
                element.to || [0, 0, 0],
                transform,
                pivot,
                flattened.model_center
            );
            const cube = new Cube({
                name: element.name || `element_${index}`,
                from: from.map((value, axis) => Math.min(value, to[axis])),
                to: to.map((value, axis) => Math.max(value, from[axis])),
                origin: transformPoint(
                    element.origin || flattened.model_center,
                    transform,
                    pivot,
                    flattened.model_center
                ),
                rotation: vectorOr(
                    element.rotation,
                    [0, 0, 0],
                    `BBModel 元素 ${element.name || index} 的旋转`
                ),
                box_uv: false,
                autouv: 0,
                inflate: Number(element.inflate || 0),
                shade: element.shade !== false
            }).addTo(parent).init();
            cube.sap_item_element_id = previewElementId(element, index, true);
            applyBbmodelFaces(cube, element.faces || {}, textures);
            Cube.preview_controller.updateUV(cube);
            instantiated.add(element);
        }

        function addNode(node, parent) {
            if (typeof node === "string") {
                addElement(elementByUuid.get(node), parent);
                return;
            }
            if (!node || typeof node !== "object") return;
            const element = node.uuid && elementByUuid.get(node.uuid);
            if (element) {
                addElement(element, parent);
                return;
            }
            const template = node.uuid && groupByUuid.get(node.uuid) || node;
            if (template.export === false) return;
            const group = makeGroup(
                template.name || "BBModel 组",
                transformPoint(
                    template.origin || flattened.model_center,
                    transform,
                    pivot,
                    flattened.model_center
                ),
                ROLE_REFERENCE,
                parent,
                hand
            );
            group.rotation = vectorOr(
                template.rotation,
                [0, 0, 0],
                `BBModel 组 ${template.name || template.uuid || "<未知>"} 的旋转`
            );
            const children = Array.isArray(node.children)
                ? node.children
                : Array.isArray(template.children)
                    ? template.children
                    : [];
            children.forEach(child => addNode(child, group));
        }

        flattened.outliner.forEach(node => addNode(node, root));
        elements
            .filter(element =>
                !element.uuid || !referencedElementUuids.has(element.uuid)
            )
            .forEach(element => addElement(element, root));
    }

    function previewElementId(element, index, bbmodel) {
        return bbmodel && element.uuid ? `bbmodel:${element.uuid}` : `element:${index}`;
    }

    function displayForContext(display, context) {
        const transforms = display || {};
        if (transforms[context]) return transforms[context];
        if (context === "firstperson_lefthand") {
            return transforms.firstperson_righthand || null;
        }
        if (context === "thirdperson_lefthand") {
            return transforms.thirdperson_righthand || null;
        }
        return null;
    }

    function flattenJavaModel(fs, path, roots, model, filePath, namespace, visited) {
        const key = path.resolve(filePath);
        if (visited.has(key)) throw new Error(`模型父级存在循环引用：${filePath}`);
        visited.add(key);
        let parent = {};
        if (model.parent && !String(model.parent).startsWith("builtin/")) {
            const parentPath = resolveAsset(path, roots, model.parent, "models", namespace);
            if (parentPath) {
                parent = flattenJavaModel(
                    fs, path, roots, readJson(fs, parentPath), parentPath,
                    inferNamespace(path, parentPath) || namespace, visited
                );
            }
        }
        visited.delete(key);
        const modelParent = String(model.parent || "");
        return {
            textures: Object.assign({}, parent.textures || {}, model.textures || {}),
            display: Object.assign({}, parent.display || {}, model.display || {}),
            elements: model.elements || parent.elements || [],
            texture_size: model.texture_size || parent.texture_size || [16, 16],
            generated_item: !!parent.generated_item
                || modelParent === "builtin/generated"
                || modelParent.endsWith(":item/generated")
                || modelParent.endsWith(":item/handheld")
        };
    }

    function generatedItemElements(textureMap) {
        const layer = Object.prototype.hasOwnProperty.call(textureMap, "layer0")
            ? "#layer0"
            : Object.keys(textureMap).length ? `#${Object.keys(textureMap)[0]}` : null;
        if (!layer) return [];
        return [{
            name: "生成式物品参考",
            from: [0, 0, 7.875],
            to: [16, 16, 8.125],
            faces: {
                north: {uv: [0, 0, 16, 16], texture: layer},
                south: {uv: [16, 0, 0, 16], texture: layer}
            }
        }];
    }

    function loadBbmodelTextures(fs, path, modelPath, textureEntries, fallbackSize) {
        const loaded = {};
        textureEntries.forEach((entry, index) => {
            if (!entry || typeof entry !== "object") return;
            const texturePath = resolveBbmodelTexturePath(
                fs,
                path,
                modelPath,
                entry
            );
            const embeddedSource = typeof entry.source === "string"
                    && entry.source.startsWith("data:image/")
                ? entry.source
                : null;
            let texture;
            if (texturePath) {
                const resolvedPath = path.resolve(texturePath);
                texture = Texture.all.find(candidate => [
                    candidate.path,
                    candidate.relative_path
                ].some(candidatePath => candidatePath
                    && path.resolve(candidatePath) === resolvedPath));
                if (!texture) {
                    texture = new Texture({
                        name: entry.name || path.basename(texturePath),
                        path: texturePath
                    }).fromPath(texturePath).add(false);
                }
            } else if (embeddedSource) {
                texture = Texture.all.find(
                    candidate => candidate.source === embeddedSource
                );
                if (!texture) {
                    texture = new Texture({
                        name: entry.name || `bbmodel_texture_${index}.png`
                    }).fromDataURL(embeddedSource).add(false);
                }
            }
            if (!texture) return;
            setTextureUvSize(texture, [
                positiveNumber(entry.uv_width, fallbackSize[0]),
                positiveNumber(entry.uv_height, fallbackSize[1])
            ]);
            loaded[String(index)] = texture;
            if (entry.uuid) loaded[entry.uuid] = texture;
            if (entry.id != null) loaded[String(entry.id)] = texture;
        });
        return loaded;
    }

    function resolveBbmodelTexturePath(fs, path, modelPath, entry) {
        const candidates = [];
        if (entry.relative_path) {
            candidates.push(path.resolve(
                path.dirname(modelPath),
                String(entry.relative_path)
            ));
        }
        if (entry.path) {
            candidates.push(path.isAbsolute(String(entry.path))
                ? path.resolve(String(entry.path))
                : path.resolve(path.dirname(modelPath), String(entry.path)));
        }
        return candidates.find(candidate => fs.existsSync(candidate)) || null;
    }

    function applyBbmodelFaces(cube, faces, textures) {
        cube.box_uv = false;
        Object.keys(faces).forEach(direction => {
            if (!cube.faces[direction]) return;
            const source = faces[direction] || {};
            if (Array.isArray(source.uv) && source.uv.length === 4) {
                cube.faces[direction].uv = numericVector(
                    source.uv,
                    [0, 0, 0, 0],
                    `BBModel ${cube.name} 的 ${direction} 面 UV`
                );
            }
            cube.faces[direction].rotation = Number(source.rotation || 0);
            cube.faces[direction].cullface = source.cullface || "";
            const texture = bbmodelTexture(textures, source.texture);
            if (texture) cube.faces[direction].texture = texture.uuid;
            else if (source.texture === null) cube.faces[direction].texture = null;
        });
    }

    function bbmodelTexture(textures, reference) {
        if (reference === null || reference === false) return null;
        if (reference == null) return textures["0"] || null;
        return textures[String(reference)] || null;
    }

    function loadModelTextures(
        fs,
        path,
        roots,
        textureMap,
        namespace,
        textureSize
    ) {
        const loaded = {};
        const normalizedSize = normalizeTextureSize(textureSize);
        Object.keys(textureMap).forEach(key => {
            const resolvedId = resolveTextureReference(textureMap, textureMap[key]);
            if (!resolvedId || resolvedId.startsWith("#")) return;
            const texturePath = resolveAsset(path, roots, `${resolvedId}.png`, "textures", namespace, true);
            if (!texturePath || !fs.existsSync(texturePath)) return;
            const resolvedTexturePath = path.resolve(texturePath);
            let texture = Texture.all.find(candidate => [
                candidate.path,
                candidate.relative_path
            ].some(candidatePath => candidatePath
                && path.resolve(candidatePath) === resolvedTexturePath));
            if (!texture) texture = new Texture({name: path.basename(texturePath), path: texturePath}).fromPath(texturePath).add(false);
            setTextureUvSize(texture, normalizedSize);
            loaded[key] = texture;
            loaded[resolvedId] = texture;
        });
        return loaded;
    }

    function applyFaces(cube, faces, textureMap, textures, textureSize) {
        cube.box_uv = false;
        Object.keys(faces).forEach(direction => {
            if (!cube.faces[direction]) return;
            const source = faces[direction];
            if (Array.isArray(source.uv)) {
                cube.faces[direction].uv = minecraftUvToTextureUv(
                    source.uv,
                    textureSize
                );
            }
            cube.faces[direction].rotation = Number(source.rotation || 0);
            cube.faces[direction].cullface = source.cullface || "";
            const key = resolveTextureKey(textureMap, source.texture);
            if (textures[key]) cube.faces[direction].texture = textures[key].uuid;
        });
    }

    function minecraftUvToTextureUv(uv, textureSize) {
        const normalizedSize = normalizeTextureSize(textureSize);
        const scaleX = normalizedSize[0] / 16;
        const scaleY = normalizedSize[1] / 16;
        return uv.map((value, index) => Number(value) * (index % 2 === 0 ? scaleX : scaleY));
    }

    function resolveTextureKey(textureMap, value) {
        let current = String(value || "");
        const visited = new Set();
        while (current.startsWith("#")) {
            const key = current.substring(1);
            if (!visited.add(key)) return key;
            const next = textureMap[key];
            if (!next || !String(next).startsWith("#")) return key;
            current = String(next);
        }
        return current;
    }

    function resolveTextureReference(textureMap, value) {
        let current = String(value || "");
        const visited = new Set();
        while (current.startsWith("#")) {
            const key = current.substring(1);
            if (!visited.add(key)) return "";
            current = String(textureMap[key] || "");
        }
        return current;
    }

    function resolveAsset(path, roots, id, category, defaultNamespace, extensionIncluded) {
        let value = String(id);
        let namespace = defaultNamespace;
        let resourcePath = value;
        const separator = value.indexOf(":");
        if (separator >= 0) {
            namespace = value.substring(0, separator);
            resourcePath = value.substring(separator + 1);
        }
        const extension = extensionIncluded ? "" : ".json";
        for (const root of roots) {
            const candidate = path.join(root, "assets", namespace, category, resourcePath + extension);
            if (require("fs").existsSync(candidate)) return candidate;
        }
        return null;
    }

    function configuredRoots(path) {
        return String(Project.sap_resource_roots || "")
            .split(";")
            .map(root => root.trim())
            .filter(Boolean)
            .map(root => path.resolve(root));
    }

    function inferResourceRoot(path, filePath) {
        let current = path.dirname(path.resolve(filePath));
        while (path.dirname(current) !== current) {
            if (path.basename(current).toLowerCase() === "assets") return path.dirname(current);
            current = path.dirname(current);
        }
        return null;
    }

    function inferNamespace(path, filePath) {
        const parts = path.resolve(filePath).split(path.sep);
        const assetsIndex = parts.map(part => part.toLowerCase()).lastIndexOf("assets");
        return assetsIndex >= 0 && parts.length > assetsIndex + 1 ? parts[assetsIndex + 1] : null;
    }

    function readJson(fs, filePath) {
        try {
            return JSON.parse(fs.readFileSync(filePath, "utf8"));
        } catch (error) {
            throw new Error(`读取 JSON 失败：${filePath}：${error.message}`);
        }
    }

    function normalizeDisplay(display, leftHand) {
        const source = display || {};
        const translation = vectorOr(
            source.translation, [0, 0, 0], "display 平移");
        const rotation = vectorOr(source.rotation, [0, 0, 0], "display 旋转");
        const rightRotation = vectorOr(
            source.right_rotation, [0, 0, 0], "display 右手旋转");
        if (leftHand) {
            translation[0] = -translation[0];
            rotation[1] = -rotation[1];
            rotation[2] = -rotation[2];
            rightRotation[1] = -rightRotation[1];
            rightRotation[2] = -rightRotation[2];
        }
        const blockbenchRotation = itemRotationToBlockbench(
            rotation,
            rightRotation
        );
        const scale = vectorOr(source.scale, [1, 1, 1], "display 缩放");
        const rotationPivot = vectorOr(
            source.rotation_pivot,
            [0, 0, 0],
            "display 旋转枢轴"
        ).map(value => value * 16);
        const scalePivot = vectorOr(
            source.scale_pivot,
            [0, 0, 0],
            "display 缩放枢轴"
        ).map(value => value * 16);
        const pivotCorrection = displayPivotCorrection(
            blockbenchRotation,
            scale,
            rotationPivot,
            scalePivot
        );
        return {
            translation: offsetVector(translation, pivotCorrection),
            rotation: blockbenchRotation,
            scale
        };
    }

    function displayPivotCorrection(
        rotation,
        scale,
        rotationPivot,
        scalePivot
    ) {
        const rotatedRotationPivot = rotateDisplayVector(
            rotationPivot,
            rotation
        );
        const rotatedScalePivot = rotateDisplayVector(scalePivot, rotation);
        return [0, 1, 2].map(axis =>
            rotationPivot[axis]
                - rotatedRotationPivot[axis]
                + rotatedScalePivot[axis] * (1 - Math.abs(scale[axis]))
        );
    }

    function rotateDisplayVector(vector, rotation) {
        const radians = Math.PI / 180;
        return new THREE.Vector3()
            .fromArray(vector)
            .applyEuler(new THREE.Euler(
                rotation[0] * radians,
                rotation[1] * radians,
                rotation[2] * radians,
                "ZYX"
            ))
            .toArray();
    }

    function itemRotationToBlockbench(rotation, rightRotation) {
        const radians = Math.PI / 180;
        const primary = new THREE.Quaternion().setFromEuler(new THREE.Euler(
            rotation[0] * radians,
            rotation[1] * radians,
            rotation[2] * radians,
            "XYZ"
        ));
        const secondary = new THREE.Quaternion().setFromEuler(new THREE.Euler(
            rightRotation[0] * radians,
            rightRotation[1] * radians,
            rightRotation[2] * radians,
            "XYZ"
        ));
        const euler = new THREE.Euler().setFromQuaternion(
            primary.multiply(secondary),
            "ZYX"
        );
        return [euler.x / radians, euler.y / radians, euler.z / radians];
    }

    function transformPoint(point, transform, pivot, center = [8, 8, 8]) {
        const source = vectorOr(point, [0, 0, 0], "模型坐标点");
        const normalizedCenter = vectorOr(center, [8, 8, 8], "模型中心");
        return source.map((value, axis) =>
            pivot[axis] + (value - normalizedCenter[axis]) * transform.scale[axis]);
    }

    function axisRotation(axis, angle) {
        if (axis === "x") return [angle, 0, 0];
        if (axis === "y") return [0, angle, 0];
        if (axis === "z") return [0, 0, angle];
        return [0, 0, 0];
    }

    function showRestPoseDialog() {
        const group = Group.selected;
        if (!group) return;
        const firstPerson = isFirstPersonRuntimeGroup(group);
        const translation = firstPerson
            ? firstPersonLocalTranslation(group)
            : vectorOr(
                group.sap_rest_translation,
                [0, 0, 0],
                `${group.name} 的静止平移`
            );
        const scale = vectorOr(group.sap_rest_scale, [1, 1, 1], `${group.name} 的静止缩放`);
        const dialog = new Dialog({
            id: "sap_bone_rest_pose",
            title: `SAP 静止姿势：${group.name}`,
            form: {
                translation_x: {label: "平移 X", type: "number", value: translation[0]},
                translation_y: {label: "平移 Y", type: "number", value: translation[1]},
                translation_z: {label: "平移 Z", type: "number", value: translation[2]},
                scale_x: {label: "缩放 X", type: "number", value: scale[0], min: 0.000001},
                scale_y: {label: "缩放 Y", type: "number", value: scale[1], min: 0.000001},
                scale_z: {label: "缩放 Z", type: "number", value: scale[2], min: 0.000001}
            },
            onConfirm(form) {
                Undo.initEdit({outliner: true, groups: [group]});
                const nextTranslation = [
                    Number(form.translation_x),
                    Number(form.translation_y),
                    Number(form.translation_z)
                ];
                if (firstPerson) {
                    const delta = subtractVector(nextTranslation, translation);
                    translateReferenceTree(group, delta);
                    Canvas.updateAll();
                }
                group.sap_rest_translation = nextTranslation;
                group.sap_rest_scale = [Number(form.scale_x), Number(form.scale_y), Number(form.scale_z)];
                Undo.finishEdit("编辑 SAP 骨骼静止姿势");
                dialog.hide();
            }
        });
        dialog.show();
    }

    function showExportDialog() {
        const selectedName = Animation.selected ? cleanPath(Animation.selected.name) : "animation";
        const dialog = new Dialog({
            id: "sap_animation_export",
            title: "导出 SAP 动画资源包",
            form: {
                namespace: {label: "命名空间", type: "text", value: projectValue("sap_namespace", "shadowsandpetals")},
                rig_path: {label: "骨架路径", type: "text", value: projectValue("sap_rig_path", selectedName)},
                controller_path: {label: "控制器路径", type: "text", value: projectValue("sap_controller_path", selectedName)},
                export_all: {label: "导出全部动画", type: "checkbox", value: true}
            },
            onConfirm(form) {
                let handled = false;
                const exportTo = directory => {
                    if (handled || typeof directory !== "string" || !directory.trim()) return;
                    handled = true;
                    try {
                        const result = exportAnimationBundle(directory, {
                            namespace: form.namespace,
                            rigPath: form.rig_path,
                            controllerPath: form.controller_path,
                            animations: form.export_all
                                ? Animation.all.slice()
                                : Animation.selected ? [Animation.selected] : []
                        });
                        dialog.hide();
                        Blockbench.showQuickMessage(
                            `已导出 ${result.animationCount} 个 SAP 动画`, 2500);
                    } catch (error) {
                        Blockbench.showMessageBox({
                            title: "SAP 动画导出失败",
                            message: String(error && error.message || error),
                            icon: "error"
                        });
                    }
                };
                try {
                    // Blockbench 5 同步返回目录；回调仍用于兼容其桌面文件选择器行为。
                    const directory = Blockbench.pickDirectory({
                        title: "选择 src/main/resources 或 src/generated/resources",
                        resource_id: "sap_animation_resource_root"
                    }, exportTo);
                    exportTo(directory);
                } catch (error) {
                    Blockbench.showMessageBox({
                        title: "SAP 动画导出失败",
                        message: String(error && error.message || error),
                        icon: "error"
                    });
                }
            }
        });
        dialog.show();
    }

    function exportAnimationBundle(directory, options) {
        if (typeof directory !== "string" || !directory.trim()) {
            throw new Error("导出目录不能为空");
        }
        const namespace = String(options.namespace || Project.sap_namespace || "shadowsandpetals").trim();
        const rigPath = cleanPath(options.rigPath || Project.sap_rig_path || "animation/main");
        const controllerPath = cleanPath(
            options.controllerPath || Project.sap_controller_path || rigPath);
        validateIdentifier(namespace, rigPath);
        validateIdentifier(namespace, controllerPath);
        const animations = options.animations || Animation.all.slice();
        if (!animations.length) throw new Error("项目中没有可导出的动画");
        validateFirstPersonTransformFrame();
        const rig = generateRig();
        validateAnimations(namespace, rig, animations);
        Project.sap_namespace = namespace;
        Project.sap_rig_path = rigPath;
        Project.sap_controller_path = controllerPath;
        const files = writeBundle(
            directory.trim(), namespace, rigPath, controllerPath, rig, animations);
        return {namespace, rigPath, controllerPath, animationCount: animations.length, files};
    }

    function validateFirstPersonTransformFrame() {
        const firstPersonGroups = Group.all.filter(isFirstPersonRuntimeGroup);
        const invalid = firstPersonGroups
            .filter(group => firstPersonLocalTranslation(group).some(
                value => !Number.isFinite(value)))
            .map(group => group.name);
        if (invalid.length) {
            throw new Error(
                `以下第一人称骨骼的局部原点无效：${invalid.join(", ")}`);
        }
    }

    function generateRig() {
        const exported = Group.all.filter(isExportedGroup);
        const names = new Set();
        const bones = exported.map(group => {
            if (!group.name || names.has(group.name)) {
                throw new Error(`导出骨骼名称为空或重复：${group.name || "<空白>"}`);
            }
            names.add(group.name);
            const exportedParent = nearestExportedParent(group);
            const parent = exportedParent ? exportedParent.name : null;
            const firstPerson = isFirstPersonRuntimeGroup(group);
            const bone = {
                name: group.name,
                pivot: firstPerson ? [0, 0, 0] : roundVector(group.origin)
            };
            if (parent) bone.parent = parent;
            const translation = firstPerson
                ? firstPersonLocalTranslation(group)
                : vectorOr(
                    group.sap_rest_translation,
                    [0, 0, 0],
                    `${group.name} 的静止平移`
                );
            const editorRotation = vectorOr(
                group.rotation,
                [0, 0, 0],
                `${group.name} 的静止旋转`
            );
            const rotation = isThirdPersonRuntimeGroup(group)
                ? thirdPersonRotationToMinecraft(editorRotation)
                : editorRotation;
            const scale = vectorOr(group.sap_rest_scale, [1, 1, 1], `${group.name} 的静止缩放`);
            const rest = {};
            if (translation.some(value => Math.abs(value) > 1.0e-6)) rest.translation = roundVector(translation);
            if (rotation.some(value => Math.abs(value) > 1.0e-6)) rest.rotation = roundVector(rotation);
            if (scale.some(value => Math.abs(value - 1) > 1.0e-6)) {
                if (scale.some(value => !Number.isFinite(value) || value === 0)) {
                    throw new Error(`骨骼 ${group.name} 的静止缩放无效`);
                }
                rest.scale = roundVector(scale);
            }
            if (Object.keys(rest).length) bone.rest = rest;
            return bone;
        });
        if (!bones.length) throw new Error("项目中没有可导出的骨骼或 socket");
        return {format_version: 1, bones};
    }

    function isFirstPersonRuntimeGroup(group) {
        if (!isExportedGroup(group)) return false;
        return isGroupInProfile(group, "first_person")
            || group.name.startsWith("first_person_");
    }

    function isThirdPersonRuntimeGroup(group) {
        return isExportedGroup(group) && isGroupInProfile(group, "third_person");
    }

    function isGroupInProfile(group, profile) {
        let ancestor = group;
        while (ancestor instanceof Group) {
            if (ancestor.name === PROFILE_ROOTS[profile]
                    && ancestor.sap_role === ROLE_GUIDE) {
                return true;
            }
            ancestor = ancestor.parent;
        }
        return false;
    }

    function firstPersonLocalTranslation(group) {
        const parentOrigin = group.parent instanceof Group
            ? group.parent.origin
            : [0, 0, 0];
        return subtractVector(group.origin, parentOrigin);
    }

    function nearestExportedParent(group) {
        let parent = group.parent;
        while (parent instanceof Group) {
            if (isExportedGroup(parent)) return parent;
            parent = parent.parent;
        }
        return null;
    }

    function generateClip(animation) {
        const thirdPersonBones = new Set(
            Group.all
                .filter(isThirdPersonRuntimeGroup)
                .map(group => group.name)
        );
        const result = {
            length: animation.length,
            loop: animation.loop === "loop",
            animations: []
        };
        animationChannels(animation).forEach(channel => result.animations.push(
            generateChannel(
                channel.bone,
                channel.target,
                channel.keyframes,
                thirdPersonBones.has(channel.bone)
            )));
        return result;
    }

    function generateChannel(bone, target, keyframes, thirdPerson) {
        return {
            bone,
            target: `minecraft:${target}`,
            keyframes: [...keyframes]
                .sort((left, right) => left.time - right.time)
                .map(keyframe => ({
                    timestamp: round(keyframe.time),
                    target: roundVector(
                        thirdPerson && target === "rotation"
                            ? thirdPersonRotationToMinecraft([
                                keyframe.get("x"),
                                keyframe.get("y"),
                                keyframe.get("z")
                            ])
                            : [
                                keyframe.get("x"),
                                keyframe.get("y"),
                                keyframe.get("z")
                            ]
                    ),
                    interpolation: `minecraft:${keyframe.interpolation === "catmullrom" ? "catmullrom" : "linear"}`
                }))
        };
    }

    function thirdPersonRotationToMinecraft(rotation) {
        const source = vectorOr(rotation, [0, 0, 0], "第三人称旋转");
        return [-source[0], source[1], -source[2]];
    }

    function generateController(namespace, rigPath, animations) {
        const states = {};
        for (const animation of animations) {
            const statePath = cleanPath(animation.name);
            if (!statePath || states[statePath]) throw new Error(`导出状态名称为空或重复：${statePath || "<空白>"}`);
            const events = parseJsonArray(animation.sap_events || "[]", `动画 ${animation.name} 的事件`)
                .map(event => ({time: Number(event.time), id: String(event.id)}));
            const state = {
                clip: `${namespace}:${statePath}`,
                speed: Number(animation.sap_speed == null ? 1 : animation.sap_speed),
                wrap: animation.loop === "loop" ? "loop" : "clamp",
                mask: animationBoneNames(animation)
            };
            if (animation.sap_additive) state.additive = true;
            if (events.length) state.events = events;
            states[statePath] = state;
        }
        const configuredTransitions = parseJsonArray(
            Project.sap_transitions || "[]", "控制器过渡");
        const sequence = configuredTransitions.length === 0
            ? inferUseSequence(Object.keys(states))
            : null;
        return {
            format_version: 1,
            rig: `${namespace}:${rigPath}`,
            initial: sequence ? sequence.intro : cleanPath(animations[0].name),
            states,
            transitions: sequence
                ? useSequenceTransitions(sequence)
                : configuredTransitions
        };
    }

    function inferUseSequence(statePaths) {
        const states = new Set(statePaths);
        const candidates = statePaths
            .filter(path => path.endsWith("_intro"))
            .map(intro => {
                const loop = intro.slice(0, -"_intro".length);
                return {
                    intro,
                    loop,
                    outro: `${loop}_outro`
                };
            })
            .filter(sequence => states.has(sequence.loop)
                && states.has(sequence.outro));
        return candidates.length === 1 ? candidates[0] : null;
    }

    function useSequenceTransitions(sequence) {
        return [
            {from: sequence.intro, to: sequence.loop, duration: 0},
            {from: sequence.intro, to: sequence.outro, duration: 0.08},
            {from: sequence.loop, to: sequence.outro, duration: 0.08},
            {from: sequence.outro, to: sequence.intro, duration: 0.08}
        ];
    }


    function validateAnimations(namespace, rig, animations) {
        const rigBones = new Set(rig.bones.map(bone => bone.name));
        const animationPaths = new Set();
        for (const animation of animations) {
            const path = cleanPath(animation.name);
            validateIdentifier(namespace, path);
            if (!path || animationPaths.has(path)) throw new Error(`动画路径为空或重复：${path || "<空白>"}`);
            animationPaths.add(path);
            if (!Number.isFinite(Number(animation.length)) || Number(animation.length) <= 0) {
                throw new Error(`动画 ${animation.name} 的长度必须大于 0`);
            }
            if (!Number.isFinite(Number(animation.sap_speed == null ? 1 : animation.sap_speed))
                    || Number(animation.sap_speed == null ? 1 : animation.sap_speed) < 0) {
                throw new Error(`动画 ${animation.name} 的速度无效`);
            }
            const previewTargets = previewItemAnimationTargets(animation);
            if (previewTargets.length) {
                throw new Error(
                    `动画 ${animation.name} 在仅用于预览的物品组上存在关键帧：`
                    + `${previewTargets.join(", ")}。请制作第一人称手臂/socket 骨骼或`
                    + "第三人称人物手臂骨骼的动画；第三人称物品始终跟随手臂"
                );
            }
            for (const bone of animationBoneNames(animation)) {
                if (!rigBones.has(bone)) throw new Error(`动画 ${animation.name} 引用了缺失的骨骼 ${bone}`);
            }
            const channels = animationChannels(animation);
            if (!channels.length && animationHasKeyframes(animation)) {
                throw new Error(
                    `动画 ${animation.name} 的关键帧全部位于预览/reference 组；`
                    + "请改为制作可导出的 SAP 骨骼或第一人称物品 socket 动画"
                );
            }
            const events = parseJsonArray(animation.sap_events || "[]", `动画 ${animation.name} 的事件`);
            let previous = -1;
            events.forEach(event => {
                const time = Number(event.time);
                validateIdentifierValue(event.id, `动画 ${animation.name} 的事件 ID`);
                if (!Number.isFinite(time) || time < previous || time < 0 || time > animation.length) {
                    throw new Error(`动画 ${animation.name} 的事件时间无效：${event.time}`);
                }
                previous = time;
            });
        }
    }

    function animationBoneNames(animation) {
        const result = [];
        animationChannels(animation).forEach(channel => {
            if (!result.includes(channel.bone)) result.push(channel.bone);
        });
        return result;
    }

    function animationChannels(animation) {
        const exportedNames = new Set(
            Group.all.filter(isExportedGroup).map(group => group.name));
        const channels = [];
        const mappedChannels = new Set();
        for (const id in animation.animators) {
            const animator = animation.animators[id];
            if (!(animator instanceof BoneAnimator)) continue;
            const bone = animationTargetBone(id, animator, exportedNames);
            if (!bone) continue;
            for (const target of ["position", "rotation", "scale"]) {
                if (!animator[target].length) continue;
                const key = `${bone}\u0000${target}`;
                if (!mappedChannels.add(key)) {
                    throw new Error(
                        `动画 ${animation.name} 同时通过 socket 和预览 reference `
                        + `控制 ${bone} 的 ${target} 通道`
                    );
                }
                channels.push({bone, target, keyframes: animator[target]});
            }
        }
        return channels;
    }

    function animationTargetBone(id, animator, exportedNames) {
        const group = Group.all.find(candidate => candidate.uuid === id);
        if (group && isExportedGroup(group)) return group.name;
        return exportedNames.has(animator.name) ? animator.name : null;
    }

    function previewItemAnimationTargets(animation) {
        const targets = [];
        for (const id in animation.animators) {
            const animator = animation.animators[id];
            if (!(animator instanceof BoneAnimator) || !animatorHasKeyframes(animator)) continue;
            const group = Group.all.find(candidate => candidate.uuid === id);
            if (!group) continue;
            const isPreviewReference = group.sap_role === ROLE_REFERENCE
                && group.sap_held_item_hand !== "none"
                && /^(first|third)person_/.test(String(group.sap_item_display_context));
            const isThirdPersonAnchor = group.sap_role === ROLE_GUIDE
                && (group.name === "main_hand_item" || group.name === "off_hand_item");
            if (isPreviewReference || isThirdPersonAnchor) {
                targets.push(group.name);
            }
        }
        return targets;
    }

    function animationHasKeyframes(animation) {
        return Object.values(animation.animators).some(
            animator => animator instanceof BoneAnimator && animatorHasKeyframes(animator));
    }

    function animatorHasKeyframes(animator) {
        return ["position", "rotation", "scale"].some(
            channel => animator[channel].length > 0);
    }

    function writeBundle(directory, namespace, rigPath, controllerPath, rig, animations) {
        const fs = require("fs");
        const path = require("path");
        const assets = path.join(directory, "assets", namespace);
        const files = [];
        const rigFile = path.join(assets, "sap", "animations", "rigs", `${rigPath}.json`);
        writeJson(fs, rigFile, rig);
        files.push(rigFile);
        const controllerFile = path.join(
            assets, "sap", "animations", "controllers", `${controllerPath}.json`);
        writeJson(
            fs,
            controllerFile,
            generateController(namespace, rigPath, animations)
        );
        files.push(controllerFile);
        for (const animation of animations) {
            const clipFile = path.join(
                assets, "neoforge", "animations", "entity", `${cleanPath(animation.name)}.json`);
            writeJson(
                fs,
                clipFile,
                generateClip(animation)
            );
            files.push(clipFile);
        }
        return files;
    }

    function writeJson(fs, file, value) {
        fs.mkdirSync(require("path").dirname(file), {recursive: true});
        fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, "utf8");
    }

    function isSapProject() {
        return Format && Format.id === FORMAT_ID;
    }

    function isExportedGroup(group) {
        if (!(group instanceof Group)) return false;
        return group.sap_role === ROLE_ANIMATED_BONE || group.sap_role === ROLE_SOCKET;
    }

    function inferProfiles() {
        const names = Group.all.map(group => group.name);
        const profiles = [];
        if (names.some(name => name.startsWith("first_person_"))) profiles.push("first_person");
        if (names.includes("right_arm") && names.includes("left_arm")
                || names.some(name => name.startsWith("third_person_"))) {
            profiles.push("third_person");
        }
        return profiles.length ? profiles.join(",") : "first_person";
    }

    function parseProfiles(value) {
        const profiles = String(value)
            .split(",")
            .map(profile => profile.trim().toLowerCase())
            .filter(Boolean);
        if (!profiles.length || profiles.some(profile => !PROFILES.includes(profile))) {
            throw new Error(`人称配置只能使用：${PROFILES.join(", ")}`);
        }
        return [...new Set(profiles)];
    }

    function projectValue(name, fallback) {
        return Project && Project[name] ? Project[name] : fallback;
    }

    function parseJsonArray(value, description) {
        let parsed;
        try {
            parsed = JSON.parse(String(value || "[]"));
        } catch (error) {
            throw new Error(`${description} JSON 无效：${error.message}`);
        }
        if (!Array.isArray(parsed)) throw new Error(`${description}必须是 JSON 数组`);
        return parsed;
    }

    function validateIdentifier(namespace, path) {
        if (!/^[a-z0-9][a-z0-9_.-]*$/.test(namespace)) throw new Error(`命名空间无效：${namespace}`);
        if (!/^[a-z0-9_./-]+$/.test(path)
                || path.startsWith("/")
                || path.endsWith("/")
                || path.includes("//")
                || path.split("/").some(segment => segment === "." || segment === "..")) {
            throw new Error(`资源路径无效：${path}`);
        }
    }

    function validateIdentifierValue(value, description) {
        const parts = String(value || "").split(":");
        if (parts.length !== 2) throw new Error(`${description}无效：${value}`);
        validateIdentifier(parts[0], parts[1]);
        return value;
    }

    function cleanPath(value) {
        return String(value)
            .replace(/^animation[._]/, "")
            .replaceAll(".", "/")
            .replace(/[^a-zA-Z0-9_/-]/g, "_")
            .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
            .replace(/\/{2,}/g, "/")
            .replace(/^\/+|\/+$/g, "")
            .toLowerCase();
    }

    function roundVector(vector) {
        return [round(vector[0]), round(vector[1]), round(vector[2])];
    }

    function vectorOr(value, fallback, description) {
        return numericVector(value, fallback, description);
    }

    function numericVector(value, fallback, description) {
        const vector = Array.isArray(value) && value.length === fallback.length
            ? value.map(Number)
            : fallback.slice();
        if (vector.some(component => !Number.isFinite(component))) {
            throw new Error(`${description}包含非有限数值`);
        }
        return vector;
    }

    function round(value) {
        const numeric = Number(value);
        if (!Number.isFinite(numeric)) throw new Error(`动画数值不是有限数：${value}`);
        return Math.round(numeric * 1000000) / 1000000;
    }
})();
