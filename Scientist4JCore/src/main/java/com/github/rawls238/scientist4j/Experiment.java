package com.github.rawls238.scientist4j;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rawls238.scientist4j.exceptions.MismatchException;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Experiment<T> {

    private static final String NAMESPACE_PREFIX = "scientist";
    private static final String CONTROL = "control";
    private static final String CANDIDATE = "candidate";
    private static final String MISMATCH = "mismatch";
    private static final String TOTAL = "total";

    static final MetricRegistry metrics = new MetricRegistry();


    private final String name;
    private final boolean raiseOnMismatch;
    private Map<String, Object> context;
    private final Timer controlTimer;
    private final Timer candidateTimer;
    private final Counter mismatchCount;
    private final Counter totalCount;

    public Experiment() {
        this("Experiment");
    }

    public Experiment(String name) {
        this(name, false);
    }

    public Experiment(String name, Map<String, Object> context) {
        this(name, context, false);
    }

    public Experiment(String name, boolean raiseOnMismatch) {
        this(name, new HashMap<String, Object>(), raiseOnMismatch);
    }

    public Experiment(String name, Map<String, Object> context, boolean raiseOnMismatch) {
        this.name = name;
        this.context = context;
        this.raiseOnMismatch = raiseOnMismatch;
        controlTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, CONTROL));
        candidateTimer = metrics.timer(MetricRegistry.name(NAMESPACE_PREFIX, this.name, CANDIDATE));
        mismatchCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, MISMATCH));
        totalCount = metrics.counter(MetricRegistry.name(NAMESPACE_PREFIX, this.name, TOTAL));
    }

    public boolean getRaiseOnMismatch() {
        return raiseOnMismatch;
    }

    public String getName() {
        return name;
    }

    public T run(Supplier<T> control, Supplier<T> candidate) throws Exception {
        Observation<T> controlObservation;
        Optional<Observation<T>> candidateObservation = Optional.absent();

        if (Math.random() < 0.5) {
            controlObservation = executeResult(CONTROL, controlTimer, control, true);
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false));
            }
        } else {
            if (runIf() && enabled()) {
                candidateObservation = Optional.of(executeResult(CANDIDATE, candidateTimer, candidate, false));
            }
            controlObservation = executeResult(CONTROL, controlTimer, control, true);
        }

        Result<T> result = new Result<T>(this, controlObservation, candidateObservation, context);
        publish(result);
        return controlObservation.getValue();
    }

    public Observation<T> executeResult(String name, Timer timer, Supplier<T> control, boolean shouldThrow) throws Exception {
        Observation<T> observation = new Observation<T>(name, timer);
        observation.startTimer();
        try {
            observation.setValue(control.get());
        } catch (Exception e) {
            observation.setException(e);
        } finally { //Why not just return in the catch?
            observation.endTimer();
            if (shouldThrow && observation.getException().isPresent()) {
                throw observation.getException().get();
            } else {
                return observation;
            }
        }
    }

    protected boolean compareResults(T controlVal, T candidateVal) {
        return controlVal.equals(candidateVal);
    }

    public boolean compare(Observation<T> controlVal, Observation<T> candidateVal) throws MismatchException {
        boolean resultsMatch = !candidateVal.getException().isPresent() && compareResults(controlVal.getValue(), candidateVal.getValue());
        totalCount.inc();
        if (!resultsMatch) {
            mismatchCount.inc();
            handleComparisonMismatch(controlVal, candidateVal);
        }
        return true;
    }

    protected void publish(Result<T> r) {
    }

    protected boolean runIf() {
        return true;
    }

    protected boolean enabled() {
        return true;
    }

    private void handleComparisonMismatch(Observation<T> controlVal, Observation<T> candidateVal) throws MismatchException {
        String msg;
        final Optional<Exception> exceptionOptional = candidateVal.getException();
        if (exceptionOptional.isPresent()) {
            final Exception exception = exceptionOptional.get();
            String stackTrace = Arrays.toString(exception.getStackTrace());
            String exceptionName = exception.getClass().getName();
            msg = String.format("%s raised an exception: %s %s", candidateVal.getName(), exceptionName, stackTrace);
        } else {
            msg = String.format("%s does not match control value (%s != %s)", candidateVal.getName(), controlVal.getValue(), candidateVal.getValue());
        }
        throw new MismatchException(msg);
    }
}
