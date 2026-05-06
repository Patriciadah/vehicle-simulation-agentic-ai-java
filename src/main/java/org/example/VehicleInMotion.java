package org.example;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VehicleInMotion {

    @Bean
    public ActorSystem<Void> actorSystem() {
        return ActorSystem.create(Behaviors.empty(), "simulation-system");
    }
    public static void main(String[] args) {
        SpringApplication.run(VehicleInMotion.class, args);
    }
}