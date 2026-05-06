package org.example.collision_detection;

import org.example.model.Vector2D;
import java.util.List;

/**
 * Strategy interface for movement behavior.
 *
 * Defines HOW a vehicle moves given:
 * - its current state
 * - environment (neighbors)
 * - goal (target)
 */
public interface MovementStrategy {

    MovementResult computeNext(
            Vector2D position,
            Vector2D velocity,
            Vector2D target,
            List<Vector2D> neighbors,
            boolean stationBusy
    );
}
