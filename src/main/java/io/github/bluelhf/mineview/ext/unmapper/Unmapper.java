package io.github.bluelhf.mineview.ext.unmapper;

import io.github.bluelhf.mineview.ext.unmapper.util.ClassNodeRemapper;
import io.github.bluelhf.mineview.ext.unmapper.util.MappingReader;
import io.github.bluelhf.mineview.ext.unmapper.util.Observable;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author SuspiciousActivity
 * @see <a href="https://github.com/SuspiciousActivity/ProGuard-Unmapper">ProGuard-Unmapper on GitHub</a>
 * */
public class Unmapper implements Opcodes {

    private final HashMap<JarEntry, byte[]> OTHER_FILES = new HashMap<>();
    private final ArrayList<ClassNode> classNodes = new ArrayList<>();
    private final Observable<State> state = new Observable<>(State.IDLE);
    private boolean removeLocals = false;

    public CompletableFuture<Void> map(File mappingFile) {
        return CompletableFuture.runAsync(() -> {
            state.set(State.REMAPPING);
            MappingReader mapping = new MappingReader(mappingFile);
            try {
                mapping.load();
            } catch (Exception ex) {
                new Exception("Error loading mapping", ex).printStackTrace();
                System.exit(1);
            }

            ClassNodeRemapper remapper = new ClassNodeRemapper(mapping.getClassMappings(), mapping.getMemberMappings());
            for (ClassNode cn : classNodes) {
                remapper.remap(cn, removeLocals);
            }
        });
    }

    public Observable<State> getState() {
        return state;
    }

    public CompletableFuture<Void> save(File jar) {
        return CompletableFuture.runAsync(() -> {
            state.set(State.SAVING);
            try {
                try (final JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
                    for (Entry<JarEntry, byte[]> entry : OTHER_FILES.entrySet()) {
                        output.putNextEntry(entry.getKey());
                        output.write(entry.getValue());
                        output.closeEntry();
                    }
                    for (ClassNode element : classNodes) {
                        ClassWriter writer = new ClassWriter(0);
                        element.accept(writer);
                        output.putNextEntry(new JarEntry(element.name + ".class"));
                        output.write(writer.toByteArray());
                        output.closeEntry();
                    }
                }
            } catch (Exception ex) {
                new Exception("Error saving jar file", ex).printStackTrace();
            }
            state.set(State.DONE);
        });
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> load(File input) {
        return CompletableFuture.runAsync(() -> {
            state.set(State.LOADING);
            try {
                JarFile jar = new JarFile(input);
                ArrayList<JarEntry> entrysLeft = new ArrayList<>();
                Enumeration<JarEntry> enumeration = jar.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry next = enumeration.nextElement();
                    byte[] data = IOUtils.toByteArray(jar.getInputStream(next));
                    if (next.getName().endsWith(".class")) {
                        ClassReader reader = new ClassReader(data);
                        ClassNode node = new ClassNode();
                        reader.accept(node, 0);

                        try {
                            classNodes.add(node);
                        } catch (NoClassDefFoundError e) {
                            entrysLeft.add(next);
                        }
                    } else {
                        OTHER_FILES.put(new JarEntry(next.getName()), data);
                    }
                }
                while (!entrysLeft.isEmpty()) {
                    for (JarEntry next : (ArrayList<JarEntry>) entrysLeft.clone()) {
                        byte[] data = IOUtils.toByteArray(jar.getInputStream(next));
                        if (next.getName().endsWith(".class")) {
                            ClassReader reader = new ClassReader(data);
                            ClassNode node = new ClassNode();
                            reader.accept(node, 0);
                            try {
                                classNodes.add(node);
                                entrysLeft.remove(next);
                            } catch (NoClassDefFoundError ignored) {

                            }
                        } else {
                            OTHER_FILES.put(new JarEntry(next.getName()), data);
                        }
                    }
                }
                jar.close();
            } catch (Exception ex) {
                new Exception("Error loading jar file", ex).printStackTrace();
            }
        });
    }

    public enum State {
        IDLE,
        LOADING,
        REMAPPING,
        SAVING,
        DONE;
    }

    public static class Builder {
        private boolean removeLocals = false;
        private File mappings = null;
        private File input = null;
        private File output = null;

        public Builder() {

        }

        public Builder removeLocals() {
            this.removeLocals = true;
            return this;
        }

        public Builder withMappings(File file) {
            this.mappings = file;
            return this;
        }

        public Builder withInput(File file) {
            this.input = file;
            return this;
        }

        public Builder withOutput(File file) {
            this.output = file;
            return this;
        }

        public Observable<State> run() {
            Unmapper unmapper = new Unmapper();
            unmapper.removeLocals = removeLocals;

            unmapper
                    .load(input)
                    .thenRun(() -> unmapper.map(mappings))
                    .thenRun(() -> unmapper.save(output));

            return unmapper.state;
        }
    }

}
