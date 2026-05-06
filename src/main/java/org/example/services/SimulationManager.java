package org.example.services;


import akka.actor.typed.*;

import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import org.example.akka_actors.SimulationMessages;
import org.example.akka_actors.SpatialActor;
import org.example.akka_actors.VehicleActor;
import org.example.akka_actors.WebSocketActor;
import org.example.model.Vector2D;
import org.example.websocket.SimulationWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;

@Service
public class SimulationManager {
    private final ActorSystem<SimulationMessages.Command> system;
    private final ActorRef<SimulationMessages.SpatialCommand> spatialActor;
    private final double SPAWN_SAFE_RADIUS = 30.0;

    public SimulationManager(SimulationWebSocketHandler wsHandler) {

        this.system = ActorSystem.create(Behaviors.empty(), "SimulationSystem");
        ActorRef<WebSocketActor.Command> webSocketActor =
                system.systemActorOf(
                        WebSocketActor.create(wsHandler),
                        "websocket-actor",
                        Props.empty()
                );
        this.spatialActor = system.systemActorOf(
                Behaviors.supervise(SpatialActor.create(webSocketActor))
                        .onFailure(SupervisorStrategy.restart()),
                "spatial-actor",
                Props.empty()
        );
    }
    private final Map<String, ActorRef<SimulationMessages.VehicleCommand>> activeVehicles = new HashMap<>();
    public void trySpawnVehicle(String id, Vector2D shore, Vector2D yard) {
        // Use Akka Ask to check if the spot is clear
        CompletionStage<Boolean> checkResult = AskPattern.ask(
                spatialActor,
                replyTo -> new SimulationMessages.CheckSpawnSafety(shore, SPAWN_SAFE_RADIUS, replyTo),
                Duration.ofSeconds(2),
                system.scheduler()
        );

        checkResult.whenComplete((isSafe, throwable) -> {
            if (isSafe != null && isSafe) {

                // SPOT IS CLEAR: Spawn the actor
                ActorRef<SimulationMessages.VehicleCommand> ref =
                        system.systemActorOf(
                                Behaviors.supervise(
                                        VehicleActor.create(id, shore, yard, spatialActor)
                                ).onFailure(
                                        SupervisorStrategy.restartWithBackoff(
                                                Duration.ofSeconds(1),
                                                Duration.ofSeconds(10),
                                                0.2
                                        )
                                ),
                                id,
                                Props.empty()
                        );
                activeVehicles.put(id, ref);
            } else {
                // SPOT IS BLOCKED: Wait 1 second and try again (Recursive retry)
                System.out.println("Spawn blocked for " + id + ". Retrying...");
                system.scheduler().scheduleOnce(
                        Duration.ofSeconds(1),
                        () -> trySpawnVehicle(id, shore, yard),
                        system.executionContext()
                );
            }
        });
    }
    public void updateVehicleMission(String id, Vector2D shore, Vector2D yard) {
        ActorRef<SimulationMessages.VehicleCommand> ref = activeVehicles.get(id);
        if (ref != null) {
            ref.tell(new SimulationMessages.UpdateMission(shore, yard));
        }
    }
}

