package org.example.akka_actors;
import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import org.example.collision_detection.DefaultMovementStrategy;
import org.example.collision_detection.MovementResult;
import org.example.collision_detection.MovementStrategy;
import org.example.model.Vector2D;


import java.time.Duration;
import java.util.List;
public class VehicleActor extends AbstractBehavior<SimulationMessages.VehicleCommand> {

    private final String id;

    /*
     *  Vehicle properties responsible for movement:
     *  @position: the current position of vehicle in the 2D plane- represented by the canvas; once every 30 ms changes as to give a sense of continuous movement
     *  @velocity: speed + direction
     */
    private Vector2D position;
    private Vector2D velocity;

    /*
    * Each vehicle(AGV) has a predefined logical route: shore crane -> yard crane -> shore crane
    * @shoreCrane, @yardCrane: current destination coordinates of the vehicle
    * @pendingShore, @pendingYard: future destinations coordinates as a result of changing direction (cranes)
    * @currentTarget: current movement direction
    * @movingToYard: true if current movement direction is a yard crane, otherwise is a shore crane
    */
    private Vector2D shoreCrane;
    private Vector2D yardCrane;
    private Vector2D pendingShore;
    private Vector2D pendingYard;
    private Vector2D currentTarget;
    private boolean movingToYard;

    /*
     * Each vehicle(AGV) has a container while traveling from shore to yard and no container from yard to shore
     * @hasContainer: true if it is on the shore -> yard path
     */
    private boolean hasContainer;

    private boolean stationBusy = false;

    private  MovementStrategy strategy;
    private List<Vector2D> latestNeighbors = List.of();


    /**
     * Callback of <b>SimulationMessages.UpdateMission</b> command received by SpatialActor.
     * If vehicle is too close user is not allowed to change destination cranes.
     *
     * @param msg Contains new destination coordinates of shore and yard crane
     * */
    private Behavior<SimulationMessages.VehicleCommand> onUpdateMission(SimulationMessages.UpdateMission msg) {
        double distToTarget = position.distance(currentTarget);

        if (distToTarget < 20.0) {
            System.out.println("Change rejected: Too close to target!");
            return this;
        }
        if (movingToYard) {
            this.yardCrane = msg.newYard;
            this.currentTarget = yardCrane;
            this.pendingShore = msg.newShore;
        } else {
            this.shoreCrane = msg.newShore;
            this.currentTarget = shoreCrane;
            this.pendingYard = msg.newYard;
        }
        return this;
    }
    /*
     * Each vehicle actor holds the reference of the spatial actor for bidirectional communication
     * @spatialActor: akka reference of spatial actor (mediator)
     */
    private final ActorRef<SimulationMessages.SpatialCommand> spatialActor;

    public static Behavior<SimulationMessages.VehicleCommand> create(String id, Vector2D shore, Vector2D yard, ActorRef<SimulationMessages.SpatialCommand> spatial) {
        return Behaviors.withTimers(timers ->
                Behaviors.setup(context -> new VehicleActor(context, timers, id, shore, yard, spatial))
        );
    }

    private VehicleActor(ActorContext<SimulationMessages.VehicleCommand> context,
                         TimerScheduler<SimulationMessages.VehicleCommand> timers,
                         String id, Vector2D shore, Vector2D yard,
                         ActorRef<SimulationMessages.SpatialCommand> spatial) {
        super(context);
        this.id = id;
        this.shoreCrane = shore;
        this.yardCrane = yard;
        this.currentTarget = yardCrane;
        this.position = shore.copy();
        this.velocity = new Vector2D(Math.random() * 2, Math.random() * 2);
        this.spatialActor = spatial;
        this.movingToYard = true;
        this.hasContainer = true; // Spawning at shore usually means picking up cargo
        this.strategy = new DefaultMovementStrategy();

        timers.startTimerAtFixedRate(SimulationMessages.Tick.INSTANCE, Duration.ofMillis(30));
    }

    @Override
    public Receive<SimulationMessages.VehicleCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SimulationMessages.Tick.class, t -> onTick())
                .onMessage(SimulationMessages.NearbyVehicles.class, this::onNearbyDetected)
                .onMessage(SimulationMessages.UpdateMission.class, this::onUpdateMission)
                .build();
    }

    private Behavior<SimulationMessages.VehicleCommand> onTick() {
        double distanceToTarget = position.distance(currentTarget);

        // --- Target reached logic ---
        if (distanceToTarget < 5.0) {

            velocity = new Vector2D(0, 0);

            movingToYard = !movingToYard;
            hasContainer = movingToYard;

            // Apply pending mission updates
            if (movingToYard && pendingYard != null) {
                yardCrane = pendingYard;
                pendingYard = null;
            } else if (!movingToYard && pendingShore != null) {
                shoreCrane = pendingShore;
                pendingShore = null;
            }

            currentTarget = movingToYard ? yardCrane : shoreCrane;

            System.out.println("Vehicle " + id + " reached target. Cargo: " + hasContainer);
        }

        // --- Movement handled by strategy ---
        MovementResult result = strategy.computeNext(
                position,
                velocity,
                currentTarget,
                latestNeighbors,
                stationBusy
        );

        this.position = result.position;
        this.velocity = result.velocity;


        spatialActor.tell(new SimulationMessages.UpdatePosition(
                id,
                position,
                hasContainer,
                currentTarget,
                getContext().getSelf()
        ));

        return this;
    }

    private Behavior<SimulationMessages.VehicleCommand> onNearbyDetected(SimulationMessages.NearbyVehicles msg) {
        this.stationBusy = msg.stationIsBusy;
        this.latestNeighbors = msg.positions;
        return this;
    }

    // TODO: Runtime Strategy changing
    public Behavior<SimulationMessages.VehicleCommand> onChangeStrategy(
            MovementStrategy newStrategy
    ) {
        this.strategy = newStrategy;
        return this;
    }
}