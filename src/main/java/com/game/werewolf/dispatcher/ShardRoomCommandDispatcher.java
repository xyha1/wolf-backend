package com.game.werewolf.dispatcher;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class ShardRoomCommandDispatcher implements RoomCommandDispatcher {

    private final int shardCount;
    private final List<ExecutorService> shards;

    public ShardRoomCommandDispatcher() {
        this(64);
    }

    public ShardRoomCommandDispatcher(int shardCount) {
        this.shardCount = shardCount;
        this.shards = new ArrayList<>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            final int shardIndex = i;
            this.shards.add(Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("room-shard-" + shardIndex);
                thread.setDaemon(true);
                return thread;
            }));
        }
    }

    @Override
    public <T> CompletableFuture<T> dispatch(String roomId, Callable<T> callable) {
        ExecutorService executor = shards.get(Math.floorMod(roomId.hashCode(), shardCount));
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }
}
