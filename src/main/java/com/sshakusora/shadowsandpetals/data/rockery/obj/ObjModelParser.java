package com.sshakusora.shadowsandpetals.data.rockery.obj;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.sshakusora.shadowsandpetals.data.rockery.obj.ObjModel.*;

/** Parses the small OBJ dialect emitted by Blockbench. */
public final class ObjModelParser {
    private ObjModelParser() {
    }

    public static ObjModel parse(Path path) throws IOException {
        List<ObjVector3> positions = new ArrayList<>();
        List<ObjVector2> textureCoordinates = new ArrayList<>();
        List<ObjVector3> normals = new ArrayList<>();
        List<ObjFace> faces = new ArrayList<>();
        List<ObjectBuilder> objects = new ArrayList<>();
        Set<String> materials = new LinkedHashSet<>();

        int currentObject = addObject(objects, "default");
        String currentMaterial = "default";
        String materialLibrary = null;
        int lineNumber = 0;

        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            int separator = firstWhitespace(line);
            String command = separator < 0 ? line : line.substring(0, separator);
            String arguments = separator < 0 ? "" : line.substring(separator).trim();

            switch (command) {
                case "v" -> {
                    String[] values = splitArguments(arguments, path, lineNumber, command);
                    if (values.length < 3) {
                        throw malformed(path, lineNumber, "A vertex needs at least three coordinates");
                    }
                    ObjVector3 position = new ObjVector3(
                            parseDouble(values[0], path, lineNumber),
                            parseDouble(values[1], path, lineNumber),
                            parseDouble(values[2], path, lineNumber)
                    );
                    positions.add(position);
                    objects.get(currentObject).positions.add(position);
                }
                case "vt" -> {
                    String[] values = splitArguments(arguments, path, lineNumber, command);
                    if (values.length < 1) {
                        throw malformed(path, lineNumber, "A texture coordinate needs at least U");
                    }
                    textureCoordinates.add(new ObjVector2(
                            parseDouble(values[0], path, lineNumber),
                            values.length >= 2 ? parseDouble(values[1], path, lineNumber) : 0.0D
                    ));
                }
                case "vn" -> {
                    String[] values = splitArguments(arguments, path, lineNumber, command);
                    if (values.length < 3) {
                        throw malformed(path, lineNumber, "A normal needs three coordinates");
                    }
                    normals.add(new ObjVector3(
                            parseDouble(values[0], path, lineNumber),
                            parseDouble(values[1], path, lineNumber),
                            parseDouble(values[2], path, lineNumber)
                    ));
                }
                case "o", "g" -> {
                    currentObject = addObject(objects, arguments.isEmpty() ? "unnamed" : arguments);
                }
                case "usemtl" -> {
                    if (arguments.isEmpty()) {
                        throw malformed(path, lineNumber, "usemtl needs a material name");
                    }
                    currentMaterial = arguments;
                    materials.add(currentMaterial);
                }
                case "mtllib" -> {
                    if (arguments.isEmpty()) {
                        throw malformed(path, lineNumber, "mtllib needs a material library path");
                    }
                    materialLibrary = arguments;
                }
                case "f" -> {
                    String[] values = splitArguments(arguments, path, lineNumber, command);
                    if (values.length < 3) {
                        throw malformed(path, lineNumber, "A face needs at least three corners");
                    }
                    List<ObjVertex> vertices = new ArrayList<>(values.length);
                    for (String value : values) {
                        vertices.add(parseVertex(
                                value,
                                positions,
                                textureCoordinates,
                                normals,
                                path,
                                lineNumber
                        ));
                    }
                    faces.add(new ObjFace(
                            currentObject,
                            objects.get(currentObject).name,
                            currentMaterial,
                            vertices
                    ));
                }
                default -> {
                    // Ignore comments and optional OBJ statements (s, l, vp,
                    // etc.) that do not affect the Blockbench mesh.
                }
            }
        }

        if (faces.isEmpty()) {
            throw new IllegalArgumentException("OBJ model contains no faces: " + path);
        }
        if (materialLibrary != null && !Files.isRegularFile(path.resolveSibling(materialLibrary))) {
            throw new IOException("OBJ material library does not exist: " + path.resolveSibling(materialLibrary));
        }

        return new ObjModel(
                faces,
                objects.stream().map(ObjectBuilder::build).toList(),
                materials
        );
    }

    private static ObjVertex parseVertex(
            String value,
            List<ObjVector3> positions,
            List<ObjVector2> textureCoordinates,
            List<ObjVector3> normals,
            Path path,
            int lineNumber
    ) {
        String[] indices = value.split("/", -1);
        int positionIndex = resolveIndex(indices[0], positions.size(), path, lineNumber, "position");
        ObjVector2 uv = indices.length >= 2 && !indices[1].isEmpty()
                ? textureCoordinates.get(resolveIndex(indices[1], textureCoordinates.size(), path, lineNumber, "texture"))
                : null;
        ObjVector3 normal = indices.length >= 3 && !indices[2].isEmpty()
                ? normals.get(resolveIndex(indices[2], normals.size(), path, lineNumber, "normal"))
                : null;
        return new ObjVertex(positions.get(positionIndex), uv, normal);
    }

    private static int resolveIndex(
            String value,
            int size,
            Path path,
            int lineNumber,
            String kind
    ) {
        try {
            int parsed = Integer.parseInt(value);
            int index = parsed > 0 ? parsed - 1 : size + parsed;
            if (parsed == 0 || index < 0 || index >= size) {
                throw new IllegalArgumentException();
            }
            return index;
        } catch (IllegalArgumentException exception) {
            throw malformed(path, lineNumber, "Invalid OBJ " + kind + " index: " + value);
        }
    }

    private static int addObject(List<ObjectBuilder> objects, String name) {
        objects.add(new ObjectBuilder(objects.size(), name));
        return objects.size() - 1;
    }

    private static String stripComment(String line) {
        int comment = line.indexOf('#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String[] splitArguments(String arguments, Path path, int lineNumber, String command) {
        if (arguments.isEmpty()) {
            throw malformed(path, lineNumber, command + " needs arguments");
        }
        return arguments.split("\\s+");
    }

    private static double parseDouble(String value, Path path, int lineNumber) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw malformed(path, lineNumber, "Invalid OBJ number: " + value);
        }
    }

    private static IllegalArgumentException malformed(Path path, int lineNumber, String message) {
        return new IllegalArgumentException(path + ":" + lineNumber + ": " + message);
    }

    private static final class ObjectBuilder {
        private final int index;
        private final String name;
        private final List<ObjVector3> positions = new ArrayList<>();

        private ObjectBuilder(int index, String name) {
            this.index = index;
            this.name = name;
        }

        private ObjObject build() {
            return new ObjObject(index, name, positions);
        }
    }
}
