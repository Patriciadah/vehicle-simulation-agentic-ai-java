package org.example.collision_detection;

import org.example.model.Vector2D;
import java.util.List;

/**
 * Default movement behavior:
 * - Seek target
 * - Avoid neighbors
 * - Slow near busy stations
 */
public class DefaultMovementStrategy implements MovementStrategy {
    // HYPER PARAMETERS
    private static final double QUEUE_RADIUS = 60.0;
    private static final double BRAKING_FACTOR= 0.8;
    private static final double MIN_SPEED=0.5;
    private static final double AVOIDANCE_DISTANCE_FACTOR=30.0;
    private static final double AVOIDANCE_WEIGHT = 1.2;
    private static final double MAX_SPEED = 4.0;

    // Seek Behaviour
    private static final double SLOWING_DISTANCE = 15.0;
    private static final double MAX_STEER_FORCE = 0.2;

    // Avoidance Behaviour
    private static final double MIN_EFFECTIVE_SPEED = 0.1;
    private static final double BASE_LOOKAHEAD = 60.0;
    private static final double LOOKAHEAD_SPEED_FACTOR = 10.0;
    private static final double AVOIDANCE_RADIUS = 40.0;
    private static final double FORWARD_THRESHOLD = 0.3;
    private static final double REPULSION_WEIGHT = 0.8;
    private static final double SIDE_BIAS = -1.0;
    private static final double MAX_AVOIDANCE_FORCE = 1.0;

    @Override
    public MovementResult computeNext(
            Vector2D position,
            Vector2D velocity,
            Vector2D target,
            List<Vector2D> neighbors,
            boolean stationBusy
    ) {



        double distanceToTarget = position.distance(target);

        Vector2D newVelocity = velocity;
        Vector2D newPosition;

        if (stationBusy && distanceToTarget < QUEUE_RADIUS) {

            newVelocity = velocity.multiply(BRAKING_FACTOR);

            // Add slight forward drift to avoid freezing completely
            if (newVelocity.magnitude() < MIN_SPEED) {
                Vector2D drift = target.subtract(position)
                        .normalize()
                        .multiply(MIN_SPEED);
                newVelocity = drift;
            }

            newPosition = position.add(newVelocity);
            return new MovementResult(newPosition, newVelocity);
        }

        //  SEEK: The force that moves toward target
        Vector2D seekForce = calculateSeek(position, velocity, target);

        //  AVOIDANCE: The force to avoid other vehicles
        Vector2D avoidanceForce = calculateAvoidance(position, velocity, neighbors);

        // Blend behavior based on distance to target
        double avoidanceFactor = Math.min(distanceToTarget /AVOIDANCE_DISTANCE_FACTOR, 1.0);
        avoidanceForce = avoidanceForce.multiply(AVOIDANCE_WEIGHT * avoidanceFactor);

        Vector2D totalForce = seekForce.add(avoidanceForce);

        newVelocity = velocity.add(totalForce).limitMagnitude(MAX_SPEED);
        newPosition = position.add(newVelocity);

        return new MovementResult(newPosition, newVelocity);
    }

    /**
     * SEEK behavior:
     * Produces a steering force that moves the agent toward a target.
     */
    private Vector2D calculateSeek(Vector2D position, Vector2D velocity, Vector2D target) {

        // Vector pointing from current position → target
        Vector2D desired = target.subtract(position);

        double distance = desired.magnitude();

        if (distance > 0) {
            desired = desired.normalize();

            // Slow down when approaching target
            if (distance < SLOWING_DISTANCE) {
                double ramped = MAX_SPEED * (distance /SLOWING_DISTANCE );
                desired = desired.multiply(ramped);
            } else {
                desired = desired.multiply(MAX_SPEED);
            }
        }

        // Steering = desired velocity - current velocity
        return desired.subtract(velocity).limitMagnitude(MAX_STEER_FORCE);
    }

    /**
     * AVOIDANCE behavior:
     * Prevents collisions with nearby agents.
     */
    private Vector2D calculateAvoidance(Vector2D position, Vector2D velocity, List<Vector2D> neighbors) {

        Vector2D steer = new Vector2D(0, 0);

        double speed = velocity.magnitude();
        if (speed <  MIN_EFFECTIVE_SPEED) return steer;

        // Normalized direction of movement
        Vector2D heading = velocity.normalize();

        double lookAhead =BASE_LOOKAHEAD + speed * LOOKAHEAD_SPEED_FACTOR;

        int count = 0;

        for (Vector2D other : neighbors) {

            Vector2D offset = other.subtract(position);
            double dist = offset.magnitude();

            if (dist <= 0 || dist > lookAhead) continue;

            Vector2D dirToOther = offset.normalize();

            // Dot product → tells if object is in front
            double forward = heading.x * dirToOther.x + heading.y * dirToOther.y;
            if (forward < FORWARD_THRESHOLD) continue;

            // Repulsion
            Vector2D repulse = position.subtract(other).normalize();

            double strength = Math.max((AVOIDANCE_RADIUS - dist) / MAX_AVOIDANCE_FORCE, 0);
            repulse = repulse.multiply(strength * REPULSION_WEIGHT);

            // Side Step
            Vector2D side = new Vector2D(-heading.y, heading.x);
            side = side.multiply(SIDE_BIAS);

            double sideStrength = (1.0 - dist / lookAhead);
            side = side.multiply(sideStrength);

            steer = steer.add(repulse.add(side));
            count++;
        }

        if (count > 0) {
            steer = steer.divide(count).limitMagnitude(MAX_AVOIDANCE_FORCE);
        }

        return steer;
    }
}