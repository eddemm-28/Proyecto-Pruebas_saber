package co.edu.unicauca.saberpro.simulations;

import co.edu.unicauca.saberpro.events.rabbit.CommonRabbitConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import(CommonRabbitConfiguration.class)
public class SimulationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimulationServiceApplication.class, args);
    }
}
