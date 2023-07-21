package blue.lhf.mineview.model.piston_meta;

import blue.lhf.mineview.model.ApplicationSide;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.jetbrains.annotations.*;

import java.io.IOException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonVersionMeta(@NotNull PistonDownload client, @NotNull PistonDownload server,
                                @UnknownNullability PistonDownload clientMappings,
                                @UnknownNullability PistonDownload serverMappings) {

    @JsonCreator
    public PistonVersionMeta {}

    public boolean hasMappings() {
        return clientMappings != null && serverMappings != null;
    }

    public static PistonVersionMeta forVersion(final PistonVersion version) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree(version.url());

        return mapper.treeToValue(node.get("downloads"), PistonVersionMeta.class);
    }
}
