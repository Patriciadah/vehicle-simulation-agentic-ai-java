package org.example.collision_detection;


import org.example.model.Vector2D;

/**
 * Immutable result of a movement step.
 *
 * Instead of mutating inputs, we return the new state.
 */
public class MovementResult {
    public final Vector2D position;
    public final Vector2D velocity;

    public MovementResult(Vector2D position, Vector2D velocity) {
        this.position = position;
        this.velocity = velocity;
    }
}