package org.example.akka_actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.websocket.SimulationWebSocketHandler;

import java.util.Map;

public class WebSocketActor extends AbstractBehavior<WebSocketActor.Command> {

    public interface Command {}

    public static class BroadcastState implements Command {
        public final Map<String, SpatialActor.VehicleState> states;

        public BroadcastState(Map<String, SpatialActor.VehicleState> states) {
            this.states = states;
        }
    }

    private final SimulationWebSocketHandler handler;

    public static Behavior<Command> create(SimulationWebSocketHandler handler) {
        return Behaviors.setup(ctx -> new WebSocketActor(ctx, handler));
    }

    private WebSocketActor(ActorContext<Command> context,
                           SimulationWebSocketHandler handler) {
        super(context);
        this.handler = handler;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(BroadcastState.class, this::onBroadcast)
                .build();
    }

    private Behavior<Command> onBroadcast(BroadcastState msg) {
        handler.broadcast(msg.states);
        return this;
    }
}