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
    /*
    * NEXT POSITION PREDICTION ALGORITHM:
    *   Hyper parameters:
    *   @QUEUE_RADIUS: radius of station overwhelming awareness: Vehicles slow down to avoid collision too close to the station that is already occupied - @stationBusy=true
    *   @BRAKING_FACTOR: slowing down factor to avoid station overwhelming
    *   @MIN_SPEED: avoids complete stopping
    *
    */
    @Override
    public MovementResult computeNext(
            Vector2D position,
            Vector2D velocity,
            Vector2D target,
            List<Vector2D> neighbors,
            boolean stationBusy
    ) {

        // HYPER PARAMETERS
        final double QUEUE_RADIUS = 60.0;
        final double BRAKING_FACTOR= 0.8;
        final double MIN_SPEED=0.5;

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
        double avoidanceFactor = Math.min(distanceToTarget / 20.0, 1.0);
        avoidanceForce = avoidanceForce.multiply(1.2 * avoidanceFactor);

        Vector2D totalForce = seekForce.add(avoidanceForce);

        newVelocity = velocity.add(totalForce).limitMagnitude(4.0);
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

        double slowingDistance = 15.0;
        double maxSpeed = 4.0;

        if (distance > 0) {
            desired = desired.normalize();

            // Slow down when approaching target
            if (distance < slowingDistance) {
                double ramped = maxSpeed * (distance / slowingDistance);
                desired = desired.multiply(ramped);
            } else {
                desired = desired.multiply(maxSpeed);
            }
        }

        // Steering = desired velocity - current velocity
        return desired.subtract(velocity).limitMagnitude(0.2);
    }

    /**
     * AVOIDANCE behavior:
     * Prevents collisions with nearby agents.
     */
    private Vector2D calculateAvoidance(Vector2D position, Vector2D velocity, List<Vector2D> neighbors) {

        Vector2D steer = new Vector2D(0, 0);

        double speed = velocity.magnitude();
        if (speed < 0.1) return steer;

        // Normalized direction of movement
        Vector2D heading = velocity.normalize();

        double lookAhead = 60.0 + speed * 10.0;
        double radius = 40.0;

        int count = 0;

        for (Vector2D other : neighbors) {

            Vector2D offset = other.subtract(position);
            double dist = offset.magnitude();

            if (dist <= 0 || dist > lookAhead) continue;

            Vector2D dirToOther = offset.normalize();

            // Dot product → tells if object is in front
            double forward = heading.x * dirToOther.x + heading.y * dirToOther.y;
            if (forward < 0.3) continue;

            // --- REPULSION ---
            Vector2D repulse = position.subtract(other).normalize();

            double strength = Math.max((radius - dist) / radius, 0);
            repulse = repulse.multiply(strength * 0.8);

            // --- SIDE STEP ---
            Vector2D side = new Vector2D(-heading.y, heading.x);
            side = side.multiply(-1.0);

            double sideStrength = (1.0 - dist / lookAhead);
            side = side.multiply(sideStrength);

            steer = steer.add(repulse.add(side));
            count++;
        }

        if (count > 0) {
            steer = steer.divide(count).limitMagnitude(1.0);
        }

        return steer;
    }
}