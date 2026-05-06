package org.example.controller;

import org.example.services.SimulationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    @Autowired
    private SimulationManager simulationManager;

    @PostMapping("/spawn")
    public ResponseEntity<String> spawnVehicle(@RequestBody SpawnRequest request) {
        if (request.shoreCrane == null || request.yardCrane == null) {
            return ResponseEntity.badRequest().body("Must provide both cranes");
        }

        simulationManager.trySpawnVehicle(
                request.id,
                request.shoreCrane,
                request.yardCrane
        );

        // We return OK immediately because the Manager handles the waiting/queuing in the background
        return ResponseEntity.ok("Spawn request received for: " + request.id + ". Waiting for clear spot...");
    }
    @PostMapping("/update-mission")
    public ResponseEntity<String> updateMission(@RequestBody UpdateMissionRequest request) {
        // We tell the SimulationManager to find the actor and update it
        simulationManager.updateVehicleMission(
                request.id,
                request.newShore,
                request.newYard
        );
        return ResponseEntity.ok("Mission update sent to " + request.id);
    }



}
