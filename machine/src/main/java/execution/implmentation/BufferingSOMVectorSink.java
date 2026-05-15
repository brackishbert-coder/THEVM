package execution.implmentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import execution.SOMVectorSink;

/**
 * A simple in-memory sink for SOM observation vectors.
 *
 * Useful for:
 * - debugging
 * - batch SOM training
 * - exporting vectors later
 */
public class BufferingSOMVectorSink implements SOMVectorSink {

    private final List<SimpleSOMObserver.ObservationRecord> records = new ArrayList<>();

    @Override
    public void accept(SimpleSOMObserver.ObservationRecord record) {
        records.add(record);
    }

    public List<SimpleSOMObserver.ObservationRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public void clear() {
        records.clear();
    }
}