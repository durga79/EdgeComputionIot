package org.edgecomputing;

import org.edgecomputing.simulation.EdgeComputingSimulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A simple test class to run the simulation with a strict timeout
 * to avoid infinite loops or excessive output.
 */
public class QuickTest {
    private static final Logger logger = LoggerFactory.getLogger(QuickTest.class);

    public static void main(String[] args) {
        try {
            // Set up minimal test configuration
            String configFile = "src/main/resources/simulation_config.json";
            String outputDir = "results/quick_test";
            
            // Create output directory
            Files.createDirectories(Paths.get(outputDir));
            
            // Create simulation with a very short timeout
            EdgeComputingSimulation simulation = new EdgeComputingSimulation(configFile);
            
            // Start a watchdog thread to terminate after 5 seconds
            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait 5 seconds
                    logger.warn("Simulation timeout reached. Terminating...");
                    System.exit(0); // Force termination
                } catch (InterruptedException e) {
                    // Interrupted, do nothing
                }
            });
            watchdog.setDaemon(true);
            watchdog.start();
            
            // Run simulation
            logger.info("Starting quick test simulation");
            simulation.runSimulation();
            
            // Export results if we reach here
            simulation.exportResults(outputDir);
            logger.info("Quick test completed successfully");
            
        } catch (IOException e) {
            logger.error("Error during quick test: {}", e.getMessage(), e);
        }
    }
}
