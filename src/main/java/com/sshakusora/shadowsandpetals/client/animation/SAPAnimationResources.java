package com.sshakusora.shadowsandpetals.client.animation;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SAPAnimationResources extends ContextAwareReloadListener {
    public static final SAPAnimationResources INSTANCE = new SAPAnimationResources();

    private static final int FORMAT_VERSION = 1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final FileToIdConverter RIGS = FileToIdConverter.json("sap/animations/rigs");
    private static final FileToIdConverter CONTROLLERS = FileToIdConverter.json("sap/animations/controllers");

    private volatile Map<Identifier, RigDefinition> rigs = Map.of();
    private volatile Map<Identifier, AnimationControllerDefinition> controllers = Map.of();
    private volatile Set<Identifier> clipIds = Set.of();

    private SAPAnimationResources() {
    }

    public RigDefinition rig(Identifier id) {
        RigDefinition rig = rigs.get(id);
        if (rig == null) {
            throw new IllegalArgumentException("Unknown animation rig " + id);
        }
        return rig;
    }

    public @Nullable RigDefinition findRig(Identifier id) {
        return rigs.get(id);
    }

    public AnimationControllerDefinition controller(Identifier id) {
        AnimationControllerDefinition controller = controllers.get(id);
        if (controller == null) {
            throw new IllegalArgumentException("Unknown animation controller " + id);
        }
        return controller;
    }

    public @Nullable AnimationControllerDefinition findController(Identifier id) {
        return controllers.get(id);
    }

    public Set<Identifier> rigIds() {
        return rigs.keySet();
    }

    public Set<Identifier> controllerIds() {
        return controllers.keySet();
    }

    public Set<Identifier> clipIds() {
        return clipIds;
    }

    public float stateDurationSeconds(AnimationResourceRef.State stateRef) {
        Objects.requireNonNull(stateRef, "stateRef");
        AnimationControllerDefinition.State state =
                controller(stateRef.controller().id()).state(stateRef.name());
        if (state.clip() == null || state.speed() <= 0.0F) {
            throw new IllegalArgumentException(
                    "Animation state " + stateRef.name()
                            + " must have a clip and positive speed");
        }
        AnimationDefinition clip = AnimationLoader.INSTANCE.getAnimation(state.clip());
        if (clip == null) {
            throw new IllegalArgumentException(
                    "Animation state " + stateRef.name()
                            + " references missing clip " + state.clip());
        }
        float duration = clip.lengthInSeconds() / state.speed();
        if (!Float.isFinite(duration) || duration <= 0.0F) {
            throw new IllegalArgumentException(
                    "Animation state " + stateRef.name()
                            + " has invalid effective duration " + duration);
        }
        return duration;
    }

    @Override
    public CompletableFuture<Void> reload(
            PreparableReloadListener.SharedState currentReload,
            Executor prepareExecutor,
            PreparableReloadListener.PreparationBarrier preparationBarrier,
            Executor applyExecutor
    ) {
        ResourceManager manager = currentReload.resourceManager();
        AnimationLoader.PendingAnimations pendingAnimations =
                currentReload.get(AnimationLoader.STATE_KEY);
        return CompletableFuture.supplyAsync(
                        () -> prepare(manager, pendingAnimations, Profiler.get()),
                        prepareExecutor
                )
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(
                        prepared -> apply(prepared, manager, Profiler.get()),
                        applyExecutor
                );
    }

    private Prepared prepare(
            ResourceManager manager,
            AnimationLoader.PendingAnimations pendingAnimations,
            ProfilerFiller profiler
    ) {
        SAPAnimationRegistry.Snapshot registrations = SAPAnimationRegistry.snapshot();
        Map<Identifier, RigDefinition> preparedRigs = load(
                manager,
                RIGS,
                registrations.rigs().stream().map(AnimationResourceRef.Rig::id).toList(),
                this::parseRig
        );
        Map<Identifier, AnimationControllerDefinition> preparedControllers = load(
                manager,
                CONTROLLERS,
                registrations.controllers().stream()
                        .map(AnimationResourceRef.Controller::id)
                        .toList(),
                this::parseController
        );
        Set<Identifier> preparedClipIds = registrations.clips().stream()
                .map(AnimationResourceRef.Clip::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        validate(registrations, preparedRigs, preparedControllers, pendingAnimations);
        return new Prepared(
                Map.copyOf(preparedRigs),
                Map.copyOf(preparedControllers),
                Set.copyOf(preparedClipIds));
    }

    private void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        rigs = prepared.rigs();
        controllers = prepared.controllers();
        clipIds = prepared.clipIds();
        UseAnimationPlaybackManager.INSTANCE.clear();
        LOGGER.info(
                "Loaded {} SAP animation rigs and {} controllers; discovered {} entity clips",
                rigs.size(), controllers.size(), clipIds.size());
    }

    private <T> Map<Identifier, T> load(
            ResourceManager manager,
            FileToIdConverter converter,
            List<Identifier> ids,
            JsonParser<T> parser
    ) {
        Map<Identifier, T> result = new HashMap<>();
        for (Identifier id : ids) {
            Identifier fileId = converter.idToFile(id);
            Resource resource = manager.getResource(fileId).orElseThrow(() ->
                    new IllegalArgumentException("Missing registered animation resource " + fileId));
            try (Reader reader = resource.openAsReader()) {
                result.put(id, parser.parse(id, GSON.fromJson(reader, JsonObject.class)));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalArgumentException("Failed to load animation resource " + fileId, exception);
            }
        }
        return result;
    }

    private RigDefinition parseRig(Identifier id, JsonObject json) {
        validateFormatVersion(id, json);
        JsonArray bonesJson = GsonHelper.getAsJsonArray(json, "bones");
        List<RigDefinition.BoneSpec> bones = new ArrayList<>(bonesJson.size());
        for (JsonElement element : bonesJson) {
            JsonObject bone = GsonHelper.convertToJsonObject(element, "bone");
            String name = GsonHelper.getAsString(bone, "name");
            String parent = GsonHelper.getAsString(bone, "parent", null);
            Vector3f pivot = vector(bone.get("pivot"), new Vector3f());
            JsonObject rest = GsonHelper.getAsJsonObject(bone, "rest", new JsonObject());
            Vector3f translation = vector(rest.get("translation"), new Vector3f());
            Vector3f rotation = vector(rest.get("rotation"), new Vector3f()).mul((float) (Math.PI / 180.0));
            Vector3f scale = vector(rest.get("scale"), new Vector3f(1.0F));
            bones.add(new RigDefinition.BoneSpec(
                    name, parent, pivot, new BoneTransform(translation, rotation, scale)));
        }
        return RigDefinition.create(id, bones);
    }

    private AnimationControllerDefinition parseController(Identifier id, JsonObject json) {
        validateFormatVersion(id, json);
        Identifier rig = Identifier.parse(GsonHelper.getAsString(json, "rig"));
        String initial = GsonHelper.getAsString(json, "initial");
        Map<String, AnimationControllerDefinition.State> states = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry :
                GsonHelper.getAsJsonObject(json, "states").entrySet()) {
            JsonObject state = GsonHelper.convertToJsonObject(entry.getValue(), "state");
            Identifier clip = state.has("clip")
                    ? Identifier.parse(GsonHelper.getAsString(state, "clip"))
                    : null;
            float speed = GsonHelper.getAsFloat(state, "speed", 1.0F);
            ClipWrap wrap = ClipWrap.valueOf(
                    GsonHelper.getAsString(state, "wrap", "definition").toUpperCase(Locale.ROOT));
            Set<String> mask = stringSet(state.getAsJsonArray("mask"));
            boolean additive = GsonHelper.getAsBoolean(state, "additive", false);
            List<AnimationControllerDefinition.EventMarker> events = new ArrayList<>();
            JsonArray eventJson = state.getAsJsonArray("events");
            if (eventJson != null) {
                for (JsonElement eventElement : eventJson) {
                    JsonObject event = GsonHelper.convertToJsonObject(eventElement, "event");
                    events.add(new AnimationControllerDefinition.EventMarker(
                            GsonHelper.getAsFloat(event, "time"),
                            Identifier.parse(GsonHelper.getAsString(event, "id"))));
                }
            }
            states.put(entry.getKey(), new AnimationControllerDefinition.State(
                    clip, speed, wrap, mask, additive, events));
        }

        List<AnimationControllerDefinition.Transition> transitions = new ArrayList<>();
        JsonArray transitionJson = json.getAsJsonArray("transitions");
        if (transitionJson != null) {
            for (JsonElement transitionElement : transitionJson) {
                JsonObject transition = GsonHelper.convertToJsonObject(transitionElement, "transition");
                transitions.add(new AnimationControllerDefinition.Transition(
                        GsonHelper.getAsString(transition, "from"),
                        GsonHelper.getAsString(transition, "to"),
                        GsonHelper.getAsFloat(transition, "duration", 0.0F)));
            }
        }
        return new AnimationControllerDefinition(id, rig, initial, states, transitions);
    }

    private static void validateFormatVersion(Identifier id, JsonObject json) {
        int formatVersion = GsonHelper.getAsInt(json, "format_version", FORMAT_VERSION);
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Animation resource " + id + " uses unsupported format version "
                            + formatVersion + "; expected " + FORMAT_VERSION);
        }
    }

    private static void validate(
            SAPAnimationRegistry.Snapshot registrations,
            Map<Identifier, RigDefinition> rigs,
            Map<Identifier, AnimationControllerDefinition> controllers,
            AnimationLoader.PendingAnimations pendingAnimations
    ) {
        Map<Identifier, SAPAnimationRegistry.Registration> registrationByController = new HashMap<>();
        for (SAPAnimationRegistry.Registration registration : registrations.registrations()) {
            registrationByController.put(registration.controller().id(), registration);
        }
        for (AnimationControllerDefinition controller : controllers.values()) {
            SAPAnimationRegistry.Registration registration =
                    registrationByController.get(controller.id());
            if (registration == null || !registration.rig().id().equals(controller.rig())) {
                throw new IllegalArgumentException(
                        "Controller " + controller.id() + " does not use its registered rig");
            }
            RigDefinition rig = rigs.get(controller.rig());
            if (rig == null) {
                throw new IllegalArgumentException(
                        "Controller " + controller.id() + " references missing rig " + controller.rig());
            }
            for (Map.Entry<String, AnimationControllerDefinition.State> entry : controller.states().entrySet()) {
                AnimationControllerDefinition.State state = entry.getValue();
                if (!Float.isFinite(state.speed()) || state.speed() < 0.0F) {
                    throw new IllegalArgumentException(
                            "Controller " + controller.id() + " state " + entry.getKey()
                                    + " has invalid speed " + state.speed());
                }
                for (String bone : state.mask()) {
                    if (rig.indexOf(bone) < 0) {
                        throw new IllegalArgumentException(
                                "Controller " + controller.id() + " state " + entry.getKey()
                                        + " masks missing bone " + bone);
                    }
                }
                if (state.clip() == null) {
                    if (!state.events().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Controller " + controller.id() + " state " + entry.getKey()
                                        + " has events but no clip");
                    }
                    continue;
                }
                if (!registration.clips().contains(new AnimationResourceRef.Clip(state.clip()))) {
                    throw new IllegalArgumentException(
                            "Controller " + controller.id() + " references unregistered clip " + state.clip());
                }
                AnimationDefinition clip = pendingAnimations.get(state.clip());
                if (clip == null) {
                    throw new IllegalArgumentException(
                            "Controller " + controller.id() + " references missing clip " + state.clip());
                }
                validateClip(state.clip(), clip);
                float previousEventTime = -1.0F;
                for (AnimationControllerDefinition.EventMarker event : state.events()) {
                    if (!Float.isFinite(event.time())
                            || event.time() < previousEventTime
                            || event.time() < 0.0F
                            || event.time() > clip.lengthInSeconds()) {
                        throw new IllegalArgumentException(
                                "Controller " + controller.id() + " state " + entry.getKey()
                                        + " has invalid event time " + event.time());
                    }
                    previousEventTime = event.time();
                }
                for (String bone : clip.boneAnimations().keySet()) {
                    if (rig.indexOf(bone) < 0) {
                        throw new IllegalArgumentException(
                                "Clip " + state.clip() + " animates missing bone " + bone
                                        + " in rig " + rig.id());
                    }
                }
            }
            Set<String> transitionPairs = new HashSet<>();
            for (AnimationControllerDefinition.Transition transition : controller.transitions()) {
                controller.state(transition.from());
                controller.state(transition.to());
                if (!Float.isFinite(transition.duration()) || transition.duration() < 0.0F) {
                    throw new IllegalArgumentException(
                            "Controller " + controller.id() + " has an invalid transition duration");
                }
                String transitionPair = transition.from() + "\u0000" + transition.to();
                if (!transitionPairs.add(transitionPair)) {
                    throw new IllegalArgumentException(
                            "Controller " + controller.id() + " has duplicate transition "
                                    + transition.from() + " -> " + transition.to());
                }
            }
        }
        validateProfiles(
                registrations.profiles(), rigs, controllers, pendingAnimations);
    }

    private static void validateProfiles(
            Set<UseAnimationProfile> profiles,
            Map<Identifier, RigDefinition> rigs,
            Map<Identifier, AnimationControllerDefinition> controllers,
            AnimationLoader.PendingAnimations pendingAnimations
    ) {
        for (UseAnimationProfile profile : profiles) {
            RigDefinition rig = rigs.get(profile.rig().id());
            if (rig == null) {
                throw new IllegalArgumentException(
                        "Use-animation profile " + profile.id()
                                + " references missing rig " + profile.rig().id());
            }
            AnimationControllerDefinition controller =
                    controllers.get(profile.controller().id());
            if (controller == null) {
                throw new IllegalArgumentException(
                        "Use-animation profile " + profile.id()
                                + " references missing controller "
                                + profile.controller().id());
            }
            controller.state(profile.defaultState().name());

            UseAnimationSequence sequence = profile.sequence();
            if (sequence != null) {
                requireTimedSequenceState(
                        profile, controller, sequence.intro(), pendingAnimations);
                AnimationControllerDefinition.State loop =
                        requireTimedSequenceState(
                                profile, controller, sequence.loop(), pendingAnimations);
                requireTimedSequenceState(
                        profile, controller, sequence.outro(), pendingAnimations);
                AnimationDefinition loopClip = pendingAnimations.get(loop.clip());
                boolean looping = loop.wrap() == ClipWrap.LOOP
                        || loop.wrap() == ClipWrap.DEFINITION && loopClip.looping();
                if (!looping) {
                    throw new IllegalArgumentException(
                            "Use-animation profile " + profile.id()
                                    + " sequence loop state " + sequence.loop().name()
                                    + " does not loop");
                }
            }

            UseAnimationProfile.FirstPersonBinding firstPerson =
                    profile.firstPerson();
            if (firstPerson != null) {
                for (AnimationResourceRef.Socket socket
                        : firstPerson.itemSockets().values()) {
                    requireRigEntry(profile.id(), rig, socket.name(), "socket");
                }
            }

            UseAnimationProfile.ThirdPersonBinding thirdPerson =
                    profile.thirdPerson();
            if (thirdPerson != null) {
                for (AnimationResourceRef.Bone bone : thirdPerson.bones().values()) {
                    requireRigEntry(profile.id(), rig, bone.name(), "bone");
                }
            }
        }
    }

    private static AnimationControllerDefinition.State requireTimedSequenceState(
            UseAnimationProfile profile,
            AnimationControllerDefinition controller,
            AnimationResourceRef.State stateRef,
            AnimationLoader.PendingAnimations pendingAnimations
    ) {
        if (!stateRef.controller().equals(profile.controller())) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + profile.id()
                            + " has a sequence state from another controller");
        }
        AnimationControllerDefinition.State state = controller.state(stateRef.name());
        if (state.clip() == null || state.speed() <= 0.0F) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + profile.id()
                            + " sequence state " + stateRef.name()
                            + " must have a clip and positive speed");
        }
        AnimationDefinition clip = pendingAnimations.get(state.clip());
        if (clip == null || !Float.isFinite(clip.lengthInSeconds())
                || clip.lengthInSeconds() <= 0.0F) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + profile.id()
                            + " sequence state " + stateRef.name()
                            + " has no valid clip");
        }
        return state;
    }

    private static void requireRigEntry(
            Identifier profileId,
            RigDefinition rig,
            String name,
            String type
    ) {
        if (rig.indexOf(name) < 0) {
            throw new IllegalArgumentException(
                    "Use-animation profile " + profileId + " references missing "
                            + type + " " + name + " in rig " + rig.id());
        }
    }

    private static void validateClip(Identifier id, AnimationDefinition clip) {
        float length = clip.lengthInSeconds();
        if (!Float.isFinite(length) || length <= 0.0F) {
            throw new IllegalArgumentException(
                    "Animation clip " + id + " has invalid length " + length);
        }
        for (Map.Entry<String, List<AnimationChannel>> boneEntry : clip.boneAnimations().entrySet()) {
            if (boneEntry.getKey().isBlank()) {
                throw new IllegalArgumentException("Animation clip " + id + " has a blank bone name");
            }
            for (AnimationChannel channel : boneEntry.getValue()) {
                Keyframe[] keyframes = channel.keyframes();
                if (keyframes.length == 0) {
                    throw new IllegalArgumentException(
                            "Animation clip " + id + " has an empty channel on " + boneEntry.getKey());
                }
                float previousTime = -1.0F;
                for (Keyframe keyframe : keyframes) {
                    float timestamp = keyframe.timestamp();
                    if (!Float.isFinite(timestamp)
                            || timestamp < 0.0F
                            || timestamp > length
                            || timestamp <= previousTime) {
                        throw new IllegalArgumentException(
                                "Animation clip " + id + " has invalid keyframe time "
                                        + timestamp + " on " + boneEntry.getKey());
                    }
                    if (!finite(keyframe.preTarget()) || !finite(keyframe.postTarget())) {
                        throw new IllegalArgumentException(
                                "Animation clip " + id + " has a non-finite keyframe target on "
                                        + boneEntry.getKey());
                    }
                    previousTime = timestamp;
                }
            }
        }
    }

    private static boolean finite(Vector3fc vector) {
        return Float.isFinite(vector.x())
                && Float.isFinite(vector.y())
                && Float.isFinite(vector.z());
    }

    private static Set<String> stringSet(@Nullable JsonArray json) {
        if (json == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (JsonElement element : json) {
            String bone = GsonHelper.convertToString(element, "mask bone");
            if (!result.add(bone)) {
                throw new IllegalArgumentException("Duplicate animation mask bone " + bone);
            }
        }
        return result;
    }

    private static Vector3f vector(@Nullable JsonElement element, Vector3f fallback) {
        if (element == null) {
            return fallback;
        }
        JsonArray array = GsonHelper.convertToJsonArray(element, "vector");
        if (array.size() != 3) {
            throw new IllegalArgumentException("Animation vectors must have exactly three values");
        }
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public record Prepared(
            Map<Identifier, RigDefinition> rigs,
            Map<Identifier, AnimationControllerDefinition> controllers,
            Set<Identifier> clipIds
    ) {
    }

    @FunctionalInterface
    private interface JsonParser<T> {
        T parse(Identifier id, JsonObject json);
    }
}
