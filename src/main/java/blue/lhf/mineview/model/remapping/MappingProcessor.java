package blue.lhf.mineview.model.remapping;

public interface MappingProcessor {
    boolean processClassMapping(final String oldName,
                                final String newName);

    void processFieldMapping(final String owner, final String type, final String name,
                             final String newOwner, final String newName);

    void processMethodMapping(final String owner, final int startLine, final int endLine, final String type,
                              final String name, final String arguments, final String newOwner, final int newStartLine,
                              final int newEndLine, final String newName);
}