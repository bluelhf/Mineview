package blue.lhf.mineview.model.procedure;

import java.util.Iterator;

/**
 * A procedure is a set of steps that can be executed in order.
 * Each step must report its progress. Steps can pass data to each other.
 * */
public abstract class Procedure<C extends ProcedureContext> {
    protected final C context;
    private final Iterator<Step<C>> steps;

    protected Procedure(final C context, final Iterator<Step<C>> steps) {
        this.context = context;
        this.steps = steps;
    }

    public void run() throws Exception {
        while (steps.hasNext()) {
            final Step<C> step = steps.next();
            step.start(context);
        }
    }
}
