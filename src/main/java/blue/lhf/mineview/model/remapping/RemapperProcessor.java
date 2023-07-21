package blue.lhf.mineview.model.remapping;

import proguard.obfuscate.MappingProcessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RemapperProcessor implements MappingProcessor, Iterable<Map.Entry<String, String>> {
    interface Entry {}
    record FieldEntry(String owner, String name, String newName) implements Entry {}
    record MethodEntry(String owner, String arguments, String returnType, String name, String newName) implements Entry {}

    private final Set<Entry> entries = ConcurrentHashMap.newKeySet();
    private final Map<String, String> classMap = new ConcurrentHashMap<>();

    @Override
    public boolean processClassMapping(final String oldName, final String newName) {
        classMap.put(ClassUtility.toInternalName(oldName), ClassUtility.toInternalName(newName));
        return true;
    }

    @Override
    public void processFieldMapping(
            final String owner, final String type, final String name,
            final String newOwner, final String newName) {
        entries.add(new FieldEntry(owner, name, newName));
    }

    @Override
    public void processMethodMapping(final String owner, final int startLine, final int endLine, final String type, final String name, final String arguments, final String newOwner, final int newStartLine, final int newEndLine, final String newName) {
        entries.add(new MethodEntry(owner, arguments, type, name, newName));
    }

    public Iterator<Map.Entry<String, String>> iterator() {
        return Spliterators.iterator(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

            final Iterator<Entry> entryIterator = entries.iterator();
            final Iterator<Map.Entry<String, String>> classMapIterator = classMap.entrySet().iterator();

            @Override
            public boolean tryAdvance(final Consumer<? super Map.Entry<String, String>> action) {
                final BiConsumer<String, String> consumer = (key, value) -> action.accept(new AbstractMap.SimpleEntry<>(key, value));
                if (entryIterator.hasNext()) {
                    nextEntry(consumer);
                    return true;
                } else return nextClass(consumer);
            }

            private void nextEntry(final BiConsumer<String, String> action) {
                final Entry entry = entryIterator.next();
                if (entry instanceof final FieldEntry field) {
                    action.accept(classMap.get(ClassUtility.toInternalName(field.owner)) + "." + field.newName, field.name);
                } else if (entry instanceof final MethodEntry method) {
                    final String internalReturnType = ClassUtility.toInternalName(method.returnType);
                    final String newReturnTypeDescriptor = ClassUtility.toDescriptor(classMap.getOrDefault(internalReturnType, internalReturnType));

                    final String[] argSplit = method.arguments.split(",");
                    final String[] newArgumentDescriptors = new String[argSplit.length];
                    for (int i = 0; i < argSplit.length; i++) {
                        final String internalArgumentType = ClassUtility.toInternalName(argSplit[i]);
                        newArgumentDescriptors[i] = ClassUtility.toDescriptor(classMap.getOrDefault(internalArgumentType, internalArgumentType));
                    }

                    final String newMethodDescriptor = String.format("(%s)%s", String.join("", newArgumentDescriptors), newReturnTypeDescriptor);
                    final String internalOwner = ClassUtility.toInternalName(method.owner);
                    final String newOwner = classMap.getOrDefault(internalOwner, internalOwner);
                    action.accept(newOwner + "." + method.newName + newMethodDescriptor, method.name);
                }
            }

            private boolean nextClass(final BiConsumer<String, String> action) {
                if (!classMapIterator.hasNext()) return false;
                final var entry = classMapIterator.next();
                action.accept(entry.getValue(), entry.getKey());
                return true;
            }
        });
    }
}
