package blue.lhf.mineview.model.piston_meta;

import blue.lhf.mineview.model.VersionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonMeta(NavigableSet<PistonVersion> versions) {
    public static void main(final String[] args) {
        try {
            final PistonMeta meta = PistonMeta.fetch();

            final Map<VersionType, TreeSet<PistonVersion>> viable =
                meta.versions.stream().filter(PistonVersion::hasMappings)
                    .collect(Collectors.groupingBy(PistonVersion::type,
                        Collectors.toCollection(TreeSet::new)));

            for (final Map.Entry<VersionType, TreeSet<PistonVersion>> entry : viable.entrySet()) {
                System.out.println(entry.getKey());
                for (final PistonVersion version : entry.getValue().descendingSet()) {
                    System.out.println(version.id());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PistonMeta fetch() throws IOException {
        final URL url = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.readValue(url, PistonMeta.class);
    }
}
