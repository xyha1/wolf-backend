package com.game.werewolf.dispatcher;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShardRoomCommandDispatcherTest {

    @Test
    void shouldExecuteCommandsInOrderWithinRoom() {
        ShardRoomCommandDispatcher dispatcher = new ShardRoomCommandDispatcher(2);
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            final int value = i;
            futures.add(dispatcher.dispatch("room-1", () -> {
                order.add(value);
                return null;
            }));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        assertEquals(50, order.size());
        for (int i = 1; i <= 50; i++) {
            assertEquals(i, order.get(i - 1));
        }
    }
}
