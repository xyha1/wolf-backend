package com.game.werewolf.dispatcher;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public interface RoomCommandDispatcher {

    <T> CompletableFuture<T> dispatch(String roomId, Callable<T> callable);
}
