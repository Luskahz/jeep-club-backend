package com.jeepclub.backend.shared.export;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class ExportPagesTest {
    @Test void exactChunkRequiresOneEmptyLookaheadWithoutDroppingOrRepeatingRows() {
        var offsets = new ArrayList<Integer>(); var seen = new ArrayList<Integer>();
        var input = IntStream.range(0,200).boxed().toList();
        ExportPages.read(offset -> { offsets.add(offset); return offset == 0 ? input : List.of(); }, seen::add);
        assertThat(offsets).containsExactly(0,200);
        assertThat(seen).containsExactlyElementsOf(input);
    }
    @Test void partialFinalChunkStopsAfterAllRowsInOrder() {
        var input = IntStream.range(0,401).boxed().toList();
        var offsets = new ArrayList<Integer>(); var seen = new ArrayList<Integer>();
        ExportPages.read(offset -> { offsets.add(offset); return input.subList(offset,Math.min(offset+200,input.size())); },seen::add);
        assertThat(offsets).containsExactly(0,200,400);
        assertThat(seen).containsExactlyElementsOf(input);
    }
    @Test void queryAndConsumerFailuresPropagateWithoutLoadingAnotherPage() {
        var failure = new IllegalStateException("Synthetic export failure");
        assertThatThrownBy(() -> ExportPages.read(offset -> { throw failure; }, value -> {})).isSameAs(failure);
        var offsets = new ArrayList<Integer>();
        assertThatThrownBy(() -> ExportPages.read(offset -> { offsets.add(offset); return List.of(1,2); },
                value -> { throw failure; })).isSameAs(failure);
        assertThat(offsets).containsExactly(0);
    }
}
