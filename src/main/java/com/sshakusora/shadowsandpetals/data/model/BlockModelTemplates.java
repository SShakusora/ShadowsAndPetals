package com.sshakusora.shadowsandpetals.data.model;

import com.google.gson.JsonObject;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.template.ElementBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import net.neoforged.neoforge.client.model.generators.template.FaceBuilder;
import org.jspecify.annotations.Nullable;

public final class BlockModelTemplates {
    private static final Identifier GENERATED_TEMPLATE_ID = Identifier.withDefaultNamespace("generated_template");

    private BlockModelTemplates() {
    }

    public static JsonObject hedgeStateModel(Identifier texture, boolean north, boolean east, boolean south, boolean west) {
        ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                .parent(Identifier.withDefaultNamespace("block/block"))
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE);
        addHedgeCore(builder, north, east, south, west);
        if (north) {
            addHedgeArm(builder, Direction.NORTH);
        }
        if (east) {
            addHedgeArm(builder, Direction.EAST);
        }
        if (south) {
            addHedgeArm(builder, Direction.SOUTH);
        }
        if (west) {
            addHedgeArm(builder, Direction.WEST);
        }

        JsonObject json = create(
                builder,
                new TextureMapping()
                        .put(TextureSlot.ALL, new Material(texture))
                        .put(TextureSlot.PARTICLE, new Material(texture))
        );
        json.addProperty("render_type", "cutout_mipped");
        return json;
    }

    public static JsonObject woodPostCoreModel(Identifier sideTexture, Identifier endTexture) {
        ExtendedModelTemplateBuilder builder = woodPostBaseTemplate()
                .element(element -> addWoodPostCuboid(element, 0.0F, 16.0F))
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                        .rotation(75.0F, 45.0F, 0.0F)
                        .translation(0.0F, 1.5F, 0.0F)
                        .scale(0.375F))
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transform -> transform
                        .rotation(75.0F, 45.0F, 0.0F)
                        .translation(0.0F, 1.5F, 0.0F)
                        .scale(0.375F))
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                        .rotation(0.0F, 135.0F, 0.0F)
                        .translation(0.0F, 1.0F, 0.0F)
                        .scale(0.4F))
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transform -> transform
                        .rotation(0.0F, 135.0F, 0.0F)
                        .translation(0.0F, 1.0F, 0.0F)
                        .scale(0.4F))
                .transform(ItemDisplayContext.GROUND, transform -> transform
                        .translation(0.0F, 3.0F, 0.0F)
                        .scale(0.25F))
                .transform(ItemDisplayContext.GUI, transform -> transform
                        .rotation(30.0F, -135.0F, 0.0F)
                        .scale(0.65F))
                .transform(ItemDisplayContext.FIXED, transform -> transform.scale(0.5F));
        return create(builder, woodPostTextureMapping(sideTexture, endTexture));
    }

    public static JsonObject woodPostLinkModel(Identifier sideTexture, Identifier endTexture, boolean upperHalf) {
        float fromY = upperHalf ? 10.0F : 0.0F;
        float toY = upperHalf ? 16.0F : 6.0F;
        ExtendedModelTemplateBuilder builder = woodPostBaseTemplate()
                .element(element -> addWoodPostCuboid(element, fromY, toY));
        return create(builder, woodPostTextureMapping(sideTexture, endTexture));
    }

    public static JsonObject woodPostChainModel(boolean upperHalf, Identifier chainTexture) {
        float fromY = upperHalf ? 10.0F : 0.0F;
        float toY = upperHalf ? 16.0F : 6.0F;
        float originY = upperHalf ? 18.0F : 8.0F;
        ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                .parent(Identifier.withDefaultNamespace("block/block"))
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .element(element -> addChainPlane(element, 6.5F, fromY, 8.0F, 9.5F, toY, 8.0F, originY, true))
                .element(element -> addChainPlane(element, 8.0F, fromY, 6.5F, 8.0F, toY, 9.5F, originY, false));

        JsonObject json = create(
                builder,
                new TextureMapping()
                        .put(TextureSlot.ALL, new Material(chainTexture))
                        .put(TextureSlot.PARTICLE, new Material(chainTexture))
        );
        json.addProperty("render_type", "cutout");
        return json;
    }

    private static ExtendedModelTemplateBuilder woodPostBaseTemplate() {
        return ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .requiredTextureSlot(TextureSlot.SIDE)
                .requiredTextureSlot(TextureSlot.END);
    }

    private static TextureMapping woodPostTextureMapping(Identifier sideTexture, Identifier endTexture) {
        return new TextureMapping()
                .put(TextureSlot.PARTICLE, new Material(sideTexture))
                .put(TextureSlot.SIDE, new Material(sideTexture))
                .put(TextureSlot.END, new Material(endTexture));
    }

    private static void addWoodPostCuboid(ElementBuilder element, float fromY, float toY) {
        element.from(6.0F, fromY, 6.0F)
                .to(10.0F, toY, 10.0F);
        addFace(element, Direction.DOWN, TextureSlot.END, null);
        addFace(element, Direction.UP, TextureSlot.END, null);
        addFace(element, Direction.NORTH, TextureSlot.SIDE, null);
        addFace(element, Direction.SOUTH, TextureSlot.SIDE, null);
        addFace(element, Direction.WEST, TextureSlot.SIDE, null);
        addFace(element, Direction.EAST, TextureSlot.SIDE, null);
    }

    private static void addHedgeCore(ExtendedModelTemplateBuilder builder, boolean north, boolean east, boolean south, boolean west) {
        builder.element(element -> addCuboidAllSelective(
                element,
                4.0F, 0.0F, 4.0F,
                12.0F, 16.0F, 12.0F,
                true, true, !north, !south, !west, !east,
                false, false, false, false
        ));
    }

    private static void addHedgeArm(ExtendedModelTemplateBuilder builder, Direction direction) {
        builder.element(element -> {
            switch (direction) {
                case NORTH -> addCuboidAllSelective(
                        element,
                        4.0F, 0.0F, 0.0F,
                        12.0F, 16.0F, 4.0F,
                        true, true, true, false, true, true,
                        true, false, false, false
                );
                case EAST -> addCuboidAllSelective(
                        element,
                        12.0F, 0.0F, 4.0F,
                        16.0F, 16.0F, 12.0F,
                        true, true, true, true, false, true,
                        false, false, false, true
                );
                case SOUTH -> addCuboidAllSelective(
                        element,
                        4.0F, 0.0F, 12.0F,
                        12.0F, 16.0F, 16.0F,
                        true, true, false, true, true, true,
                        false, true, false, false
                );
                case WEST -> addCuboidAllSelective(
                        element,
                        0.0F, 0.0F, 4.0F,
                        4.0F, 16.0F, 12.0F,
                        true, true, true, true, true, false,
                        false, false, true, false
                );
                default -> throw new IllegalArgumentException("Unsupported hedge arm direction: " + direction);
            }
        });
    }

    private static void addCuboidAllSelective(
            ElementBuilder element,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            boolean includeDown,
            boolean includeUp,
            boolean includeNorth,
            boolean includeSouth,
            boolean includeWest,
            boolean includeEast,
            boolean cullNorth,
            boolean cullSouth,
            boolean cullWest,
            boolean cullEast
    ) {
        element.from(fromX, fromY, fromZ).to(toX, toY, toZ);
        if (includeDown) {
            addFace(element, Direction.DOWN, TextureSlot.ALL, null);
        }
        if (includeUp) {
            addFace(element, Direction.UP, TextureSlot.ALL, null);
        }
        if (includeNorth) {
            addFace(element, Direction.NORTH, TextureSlot.ALL, cullNorth ? Direction.NORTH : null);
        }
        if (includeSouth) {
            addFace(element, Direction.SOUTH, TextureSlot.ALL, cullSouth ? Direction.SOUTH : null);
        }
        if (includeWest) {
            addFace(element, Direction.WEST, TextureSlot.ALL, cullWest ? Direction.WEST : null);
        }
        if (includeEast) {
            addFace(element, Direction.EAST, TextureSlot.ALL, cullEast ? Direction.EAST : null);
        }
    }

    private static void addChainPlane(
            ElementBuilder element,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            float originY,
            boolean northSouthFaces
    ) {
        element.from(fromX, fromY, fromZ)
                .to(toX, toY, toZ)
                .shade(false)
                .rotation(rotation -> rotation
                        .singleAxis(Direction.Axis.Y, 45.0F)
                        .origin(8.0F, originY, 8.0F));
        if (northSouthFaces) {
            addFaceWithUv(element, Direction.NORTH, TextureSlot.ALL, 0.0F, upperLowerUvMin(fromY), 3.0F, upperLowerUvMax(fromY, toY));
            addFaceWithUv(element, Direction.SOUTH, TextureSlot.ALL, 0.0F, upperLowerUvMin(fromY), 3.0F, upperLowerUvMax(fromY, toY));
        } else {
            addFaceWithUv(element, Direction.EAST, TextureSlot.ALL, 3.0F, upperLowerUvMin(fromY), 6.0F, upperLowerUvMax(fromY, toY));
            addFaceWithUv(element, Direction.WEST, TextureSlot.ALL, 3.0F, upperLowerUvMin(fromY), 6.0F, upperLowerUvMax(fromY, toY));
        }
    }

    private static void addFace(ElementBuilder element, Direction direction, TextureSlot texture, @Nullable Direction cullface) {
        element.face(direction, face -> configureFace(face, texture, cullface));
    }

    private static void addFaceWithUv(
            ElementBuilder element,
            Direction direction,
            TextureSlot texture,
            float u1,
            float v1,
            float u2,
            float v2
    ) {
        element.face(direction, face -> configureFace(face, texture, null).uvs(u1, v1, u2, v2));
    }

    private static FaceBuilder configureFace(FaceBuilder face, TextureSlot texture, @Nullable Direction cullface) {
        face.texture(texture);
        if (cullface != null) {
            face.cullface(cullface);
        }
        return face;
    }

    private static JsonObject create(ExtendedModelTemplateBuilder builder, TextureMapping mapping) {
        JsonObject[] result = new JsonObject[1];
        builder.build().create(GENERATED_TEMPLATE_ID, mapping, (ignored, model) -> result[0] = model.get().getAsJsonObject());
        return result[0];
    }

    private static float upperLowerUvMin(float fromY) {
        return fromY <= 0.0F ? 10.0F : 0.0F;
    }

    private static float upperLowerUvMax(float fromY, float toY) {
        return fromY <= 0.0F ? 10.0F + (toY - fromY) : toY - fromY;
    }
}
