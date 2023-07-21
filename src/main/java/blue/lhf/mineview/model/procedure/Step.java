package blue.lhf.mineview.model.procedure;

@FunctionalInterface
public interface Step<Context> {
    void start(final Context context) throws Exception;
}
