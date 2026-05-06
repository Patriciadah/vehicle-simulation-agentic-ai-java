package org.example.akka_actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import org.example.model.Vector2D;
import org.example.websocket.SimulationWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
* Is in charge of maintaining a Map of all VehicleActors in comes in contact with
* ! Make sure he is aware of all Vehicle Actors
*
* STATUS: INCOMPLETE
*
* Implemented this far:
*   1. Receives message from VehicleActor to update vehicle location
*   2. Sends message to VehicleActor about other nearby (100 distance radius) vehicle positions
*   3. Is created by: None
*   4. Communicates with Websocket?
*
* */
public class SpatialActor extends AbstractBehavior<SimulationMessages.SpatialCommand> {
    private final Map<String, VehicleState> vehicleStates = new HashMap<>();
    private final Map<Vector2D, String> stationLocks = new HashMap<>();
    private final ActorRef<WebSocketActor.Command> webSocketActor;
    public static class VehicleState {
        public double x, y;
        public boolean hasContainer;


        public String type;

        public VehicleState(Vector2D pos, boolean cargo) {
            this.x = pos.x;
            this.y = pos.y;
            this.hasContainer = cargo;
            this.type = "AGV";
        }
    }

    public static Behavior<SimulationMessages.SpatialCommand> create(ActorRef<WebSocketActor.Command> webSocketActor) {
        return Behaviors.setup(context -> new SpatialActor(context,webSocketActor));
    }

    private SpatialActor(ActorContext<SimulationMessages.SpatialCommand> context,ActorRef<WebSocketActor.Command> webSocketActor) {
        super(context);
        this.webSocketActor=webSocketActor;
    }

    @Override
    public Receive<SimulationMessages.SpatialCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SimulationMessages.UpdatePosition.class, this::onUpdatePosition)
                .onMessage(SimulationMessages.CheckSpawnSafety.class, this::onCheckSafety)
                .build();
    }
    private Behavior<SimulationMessages.SpatialCommand> onCheckSafety(SimulationMessages.CheckSpawnSafety msg) {
        boolean isSafe = vehicleStates.values().stream()
                .noneMatch(v -> new Vector2D(v.x, v.y).distance(msg.pos) < msg.radius);

        msg.replyTo.tell(isSafe);
        return this;
    }
    private Behavior<SimulationMessages.SpatialCommand> onUpdatePosition(SimulationMessages.UpdatePosition msg) {

        vehicleStates.put(msg.id, new VehicleState(msg.pos, msg.hasContainer));
        Vector2D targetStation = msg.targetStation;
        // If I am very close to my target, I "Lock" it
        double arrivalThreshold = 50.0;
        if (msg.pos.distance(targetStation) < arrivalThreshold) {
            stationLocks.put(targetStation, msg.id);
        } else {
            // If I was the one locking it but I moved away, release it
            if (msg.id.equals(stationLocks.get(targetStation))) {
                stationLocks.remove(targetStation);
            }
        }

        boolean stationBusy = stationLocks.containsKey(targetStation) &&
                !msg.id.equals(stationLocks.get(targetStation));



        List<Vector2D> neighbors = vehicleStates.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(msg.id)) // Don't detect self
                .map(entry -> new Vector2D(entry.getValue().x, entry.getValue().y)) // Map back to Vector2D
                .filter(pos -> msg.pos.distance(pos) < 200)
                .collect(Collectors.toList());

        // 3. Send the "Radar" data back to the vehicle for its AI swerving logic
        msg.vehicleRef.tell(new SimulationMessages.NearbyVehicles(neighbors,stationBusy));

        webSocketActor.tell(
                new WebSocketActor.BroadcastState(vehicleStates)
        );

        return this;
    }
}