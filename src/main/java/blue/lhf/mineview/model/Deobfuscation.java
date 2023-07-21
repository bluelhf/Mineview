package blue.lhf.mineview.model;

import blue.lhf.mineview.model.piston_meta.PistonDownload;
import blue.lhf.mineview.model.piston_meta.PistonVersion;
import blue.lhf.mineview.model.piston_meta.PistonVersionMeta;
import blue.lhf.mineview.model.procedure.Procedure;
import blue.lhf.mineview.model.procedure.ProcedureContext;
import blue.lhf.mineview.model.procedure.Step;
import blue.lhf.mineview.model.util.ProgressInputStream;
import blue.lhf.mineview.model.logging.LineLogger;
import blue.lhf.mineview.model.remapping.MappingUtility;
import blue.lhf.mineview.model.remapping.RemapperProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static blue.lhf.mineview.model.ApplicationSide.CLIENT;
import static java.util.concurrent.TimeUnit.MINUTES;

public class Deobfuscation extends Procedure<Deobfuscation.State> {
    public Deobfuscation(final ApplicationSide side, final PistonVersion version, final Path outputDirectory) {
        super(new State(side, version, outputDirectory), List.<Step<State>>of(
                Deobfuscation::downloadMetadata,
                Deobfuscation::parseMappings,
                Deobfuscation::extractBundle,
                Deobfuscation::remapArchive,
                Deobfuscation::done
        ).iterator());
    }

    static class State extends ProcedureContext {

        private final LineLogger logger = new LineLogger();
        private final ApplicationSide side;

        private final PistonVersion version;
        private final Path outputDirectory;

        private PistonVersionMeta meta;
        private Remapper remapper;
        private JarFile archive;

        public State(final ApplicationSide side, final PistonVersion version, final Path outputDirectory) {
            this.side = side;
            this.version = version;
            this.outputDirectory = outputDirectory;
        }

        private Path getOutputPath() {
            return outputDirectory.resolve("deobfuscated-%s-%s.jar".formatted(side.name().toLowerCase(), version.id()));
        }
    }
    private static ProgressInputStream fetch(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        final long total = connection.getContentLengthLong();
        return new ProgressInputStream(connection.getInputStream(), total);
    }

    private static void downloadMetadata(final State context) throws IOException {
        context.logger.log("Downloading metadata for " + context.version.id() + "...");
        final ObjectMapper mapper = new ObjectMapper();
        try (final ProgressInputStream stream = fetch(context.version.url()).withTotal(context::setTotal).onProgress(context::addProgress)) {
            final JsonNode node = mapper.readTree(stream);
            context.meta = mapper.treeToValue(node.get("downloads"), PistonVersionMeta.class);
        }
    }

    private static void parseMappings(final State context) throws IOException, InterruptedException {
        final PistonVersionMeta meta = context.meta;
        final ApplicationSide side = context.side;

        final PistonDownload mappings = switch (side) {
            case CLIENT -> meta.clientMappings();
            case SERVER -> meta.serverMappings();
        };

        final List<String> lines;
        context.setProgress(0);
        context.logger.log("Downloading mappings...");
        final int index = context.logger.log("  .. initialising download...").join();
        try (final ProgressInputStream stream = fetch(mappings.url()).withTotal(context::setTotal).onProgress((increment) -> {
            context.logger.relog(index, "  .. %d/%d bytes (%05.2f %%)".formatted(
                    context.getProgress(), context.getTotal(),
                    context.getProgress() / (double) context.getTotal() * 100));
            context.addProgress(increment);
        }); final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            lines = reader.lines().filter(s -> !s.trim().startsWith("#")).toList();
        }

        final RemapperProcessor processor = new RemapperProcessor();
        final ExecutorService mappingPool = Executors.newWorkStealingPool(8);
        context.setProgress(0);
        context.setTotal(lines.size());
        context.logger.log("Parsing mappings...");

        String className = null;

        for (final String line : lines) {
            context.addProgress(1);

            if (line.endsWith(":")) {
                className = MappingUtility.processClassMapping(line, processor);
            } else if (className != null) {
                final String $className = className;
                mappingPool.execute(() -> {
                    MappingUtility.processClassMemberMapping($className, line, processor);
                });
            }
        }

        mappingPool.shutdown();
        while (!mappingPool.awaitTermination(1, MINUTES));
        context.logger.log("Parsed " + lines.size() + " mappings.");

        final Map<String, String> map = new HashMap<>(lines.size());
        context.setProgress(0, lines.size());
        context.logger.log("Building mapping table...");
        for (final var entry : processor) {
            map.put(entry.getKey(), entry.getValue());
            context.addProgress(1);
        }
        context.logger.log("Built mapping table with %d entries.".formatted(map.size()));
        context.remapper = new SimpleRemapper(map);
    }

    private static void extractBundle(final State context) throws IOException {
        final PistonVersionMeta meta = context.meta;

        final ApplicationSide side = context.side;
        final PistonDownload download = switch (side) {
            case CLIENT -> meta.client();
            case SERVER -> meta.server();
        };

        final Path cachePath = Files.createTempFile("mineview_jar_cache", ".jar");
        try (final OutputStream cacheStream = Files.newOutputStream(cachePath);
             final InputStream stream = fetch(download.url())) {

            context.setProgress(0);
            if (side == CLIENT) {
                context.logger.log("Downloading client archive...");
                final int index = context.logger.log("  .. initialising download...").join();
                try (final ProgressInputStream downloadStream = new ProgressInputStream(stream, download.size())
                        .withTotal(context::setTotal)
                        .onProgress((increment) -> {
                            context.logger.relog(index, "  .. %d/%d bytes (%05.2f %%)".formatted(
                                    context.getProgress(), context.getTotal(),
                                    context.getProgress() / (double) context.getTotal() * 100));
                            context.addProgress(increment);
                        })
                ) {
                    downloadStream.transferTo(cacheStream);
                }

                context.archive = new JarFile(cachePath.toFile());
            } else {
                context.logger.log("Looking for server archive in bundle...");
                try (final JarInputStream jarStream = new JarInputStream(stream)) {
                    JarEntry entry;
                    while ((entry = jarStream.getNextJarEntry()) != null) {
                        final String name = entry.getName();
                        if (name.startsWith("META-INF/versions/") && name.endsWith(".jar")) {
                            context.logger.log("  .. found it at " + name + "!");
                            context.logger.log("Downloading server bundle...");
                            final int index = context.logger.log("  .. initialising download...").join();
                            final var bundleStream = new ProgressInputStream(jarStream, entry.getSize());
                            bundleStream.withTotal(context::setTotal).onProgress((increment) -> {
                                context.addProgress(increment);
                                context.logger.relog(index, "  .. %d/%d bytes (%05.2f %%)".formatted(context.getProgress(), context.getTotal(), context.getProgress() / (double) context.getTotal() * 100));
                            }).transferTo(cacheStream);

                            break;
                        } else {
                            context.logger.log("  .. not " + name);
                        }
                    }
                }
            }

            context.archive = new JarFile(cachePath.toFile());
        }
    }

    private static void remapArchive(final State context) throws IOException {
        context.logger.log("Remapping archive...");
        try (final JarFile archive = context.archive;
             final JarOutputStream output = new JarOutputStream(Files.newOutputStream(context.getOutputPath()))) {
            context.setProgress(0);
            context.setTotal(archive.stream().count());

            final ReentrantLock writeDoor = new ReentrantLock();

            archive.stream().parallel().forEach(entry -> {
                context.addProgress(1);
                try {
                    final InputStream reader = archive.getInputStream(entry);
                    if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                        final ClassReader classReader = new ClassReader(reader);
                        final ClassWriter writer = new ClassWriter(0);
                        final ClassRemapper remappingVisitor = new ClassRemapper(writer, context.remapper);
                        classReader.accept(remappingVisitor, 0);

                        String remappedPath = context.remapper.map(classReader.getClassName());
                        if (remappedPath == null) {
                            remappedPath = classReader.getClassName();
                        }

                        context.logger.log("  .. transformed " + classReader.getClassName() + " -> " + remappedPath);

                        try {
                            writeDoor.lock();
                            output.putNextEntry(new JarEntry(remappedPath + ".class"));
                            output.write(writer.toByteArray());
                            output.closeEntry();
                        } finally {
                            writeDoor.unlock();
                        }
                    } else {
                        context.logger.log("  .. copied " + entry.getName());
                        try {
                            writeDoor.lock();
                            output.putNextEntry(new JarEntry(entry.getName()));
                            reader.transferTo(output);
                            output.closeEntry();
                        } finally {
                            writeDoor.unlock();
                        }
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });
        }

        context.logger.log("Remapped %d entries!".formatted(context.getProgress()));
    }

    private static void done(final State context) {
        context.logger.log("Done!");
        context.logger.log("The remapped archive can be found at " + context.getOutputPath());
    }

    public Deobfuscation onProgress(final BiConsumer<Long, Long> progressListener) {
        this.context.onProgress(context -> progressListener.accept(context.getProgress(), context.getTotal()));
        return this;
    }

    public LineLogger getLogger() {
        return this.context.logger;
    }
}
