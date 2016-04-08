package com.github.rawls238.scientist4j;

import com.github.rawls238.scientist4j.exceptions.MismatchException;
import com.google.common.base.Supplier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExperimentTest {

    private Integer exceptionThrowingFunction() {
        throw new RuntimeException("throw an exception");
    }

    private Integer safeFunction() {
        return 3;
    }

    private Integer safeFunctionWithDifferentResult() {
        return 4;
    }

    private Supplier<Integer> exceptionThrowingSupplier = new Supplier<Integer>() {
        @Override
        public Integer get() {
            return exceptionThrowingFunction();
        }
    };

    private final Supplier<Integer> safeSupplier = new Supplier<Integer>() {
        @Override
        public Integer get() {
            return safeFunction();
        }
    };

    private final Supplier<Integer> safeSupplierWithDifferentResult = new Supplier<Integer>() {
        @Override
        public Integer get() {
            return safeFunctionWithDifferentResult();
        }
    };

    @Test
    public void itThrowsAnExceptionWhenControlFails() throws Exception {
        Experiment<Integer> experiment = new Experiment<Integer>("test");
        boolean controlThrew = false;
        try {
            experiment.run(exceptionThrowingSupplier, exceptionThrowingSupplier);
        } catch (RuntimeException e) {
            controlThrew = true;
        }
        assertThat(controlThrew).isEqualTo(true);
    }

    @Test
    public void itDoesntThrowAnExceptionWhenCandidateFails() throws Exception {
        Experiment<Integer> experiment = new Experiment<Integer>("test");
        boolean candidateThrew = false;
        Integer val = 0;
        try {
            val = experiment.run(safeSupplier, exceptionThrowingSupplier);
        } catch (RuntimeException e) {
            candidateThrew = true;
        }
        assertThat(candidateThrew).isEqualTo(false);
        assertThat(val).isEqualTo(3);
    }

    @Test
    public void itThrowsOnMismatch() throws Exception {
        Experiment<Integer> experiment = new Experiment<Integer>("test", true);
        boolean candidateThrew = false;
        try {
            experiment.run(safeSupplier, safeSupplierWithDifferentResult);
        } catch (MismatchException e) {
            candidateThrew = true;
        }

        assertThat(candidateThrew).isEqualTo(true);
    }

    @Test
    public void itDoesNotThrowOnMatch() {
        Experiment<Integer> exp = new Experiment<Integer>("test", true);
        boolean candidateThrew = false;
        Integer val = 0;
        try {
            val = exp.run(safeSupplier, safeSupplier);
        } catch (Exception e) {
            candidateThrew = true;
        }

        assertThat(val).isEqualTo(3);
        assertThat(candidateThrew).isEqualTo(false);
    }

    @Test
    public void itWorksWithAnExtendedClass() throws Exception {
        Experiment<Integer> exp = new TestPublishExperiment<Integer>("test");

        exp.run(safeSupplier, safeSupplier);

    }

}
