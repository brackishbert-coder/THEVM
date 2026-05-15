package execution;

import execution.implmentation.SimpleSOMObserver;

/**
 * Receives encoded observation vectors from a SOMObserver.
 *
 * This abstraction lets the runner-side observer remain independent from:
 * - a concrete SOM implementation
 * - storage details
 * - online vs batch training
 */
public interface SOMVectorSink {
    void accept(SimpleSOMObserver.ObservationRecord record);
}