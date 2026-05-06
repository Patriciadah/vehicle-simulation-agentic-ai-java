package org.example.akka_actors;

import akka.actor.typed.ActorRef;
import org.example.model.Vector2D;

import java.util.List;
/**
 *  Akka compatible message formats.<br>
 *  Used for communication between SpatialActor and VehicleActor
 */
public class SimulationMessages {
    public interface Command {}
    public interface SpatialCommand extends Command{}
    public interface VehicleCommand extends Command{}

    /**
    *  Vehicle Actor -> Spatial Actor:<br>
    *  When a vehicle actor changes the position field, an <b>UpdatePosition</b> message is sent to the SpatialActor <br>
    */
    public static class UpdatePosition implements SpatialCommand {
        public final String id;
        public final Vector2D pos;
        public final boolean hasContainer;
        public final ActorRef<VehicleCommand> vehicleRef;

        public final Vector2D targetStation;

        public UpdatePosition(String id, Vector2D pos, boolean hasContainer, Vector2D targetStation, ActorRef<VehicleCommand> ref) {
            this.id = id;
            this.pos = pos;
            this.hasContainer = hasContainer;
            this.targetStation=targetStation;
            this.vehicleRef = ref;
        }
    }

    /**
     *  Spatial Actor -> Vehicle Actor:<br>
     *  Spatial Actor checks for nearby vehicles (neighbours within a certain radius) and sends a <b>NearbyVehicles</b> message to all vehicles <br>
     */
    public static class NearbyVehicles implements VehicleCommand {
        public final List<Vector2D> positions;
        public final boolean stationIsBusy;
        public NearbyVehicles(List<Vector2D> positions,boolean stationIsBusy ) { this.positions = positions; this.stationIsBusy=stationIsBusy; }
    }

    public static class UpdateMission implements VehicleCommand {
        public final Vector2D newShore;
        public final Vector2D newYard;
        public UpdateMission(Vector2D shore, Vector2D yard) {
            this.newShore = shore;
            this.newYard = yard;
        }
    }
    // Internal Heartbeat
    public enum Tick implements VehicleCommand { INSTANCE }

    public static class CheckSpawnSafety implements SpatialCommand {
        public final Vector2D pos;
        public final double radius;
        public final ActorRef<Boolean> replyTo; // We expect a Boolean back

        public CheckSpawnSafety(Vector2D pos, double radius, ActorRef<Boolean> replyTo) {
            this.pos = pos;
            this.radius = radius;
            this.replyTo = replyTo;
        }
    }
}