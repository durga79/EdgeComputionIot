package org.edgecomputing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.edgecomputing.simulation.EdgeComputingSimulation;
import org.edgecomputing.utils.ResultsVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main demonstration class for the Edge Computing IoT project.
 * This class demonstrates how to configure and run simulations with
 * different parameters and scenarios based on the paper:
 * "IoT Service Slicing and Task Offloading for Edge Computing"
 */
public class EdgeComputingDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(EdgeComputingDemo.class);
    
    /**
     * Main method that runs a demonstration of the edge computing simulation.
     * 
     * @param args Command-line arguments (optional):
     *             args[0]: Scenario type ("baseline", "energy-efficient", "latency-optimized")
     *             args[1]: Output directory for results
     */
    public static void main(String[] args) {
        try {
            // Default parameters
            String scenarioType = "baseline";
            String outputDir = "results";
            
            // Parse command-line arguments
            if (args.length > 0) {
                scenarioType = args[0];
            }
            if (args.length > 1) {
                outputDir = args[1];
            }
            
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));
            
            // Select configuration based on scenario
            String configFile = selectConfigurationFile(scenarioType);
            
            logger.info("Starting Edge Computing Demo");
            logger.info("Scenario: {}", scenarioType);
            logger.info("Configuration file: {}", configFile);
            logger.info("Output directory: {}", outputDir);
            
            // Run simulation
            runSimulation(configFile, outputDir);
            
            // Generate visualization charts
            generateVisualizations(outputDir);
            
            logger.info("Edge Computing Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Error running Edge Computing Demo", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Select the appropriate configuration file based on the scenario type.
     * 
     * @param scenarioType Type of scenario to run or path to a configuration file
     * @return Path to the configuration file
     */
    private static String selectConfigurationFile(String scenarioType) {
        // Check if scenarioType is a path to an existing file
        if (Files.exists(Paths.get(scenarioType))) {
            logger.info("Using custom configuration file: {}", scenarioType);
            return scenarioType;
        }
        
        // Otherwise, use built-in configuration types
        switch (scenarioType.toLowerCase()) {
            case "energy-efficient":
                return "src/main/resources/configs/energy_efficient_config.json";
                
            case "latency-optimized":
                return "src/main/resources/configs/latency_optimized_config.json";
                
            case "high-density":
                return "src/main/resources/configs/high_density_config.json";
                
            case "baseline":
            default:
                return "src/main/resources/simulation_config.json";
        }
    }
    
    /**
     * Run the simulation with the specified configuration.
     * 
     * @param configFile Path to the configuration file
     * @param outputDir Directory to store results
     * @throws IOException if there's an error reading or writing files
     */
    private static void runSimulation(String configFile, String outputDir) throws IOException {
        logger.info("Running simulation with configuration: {}", configFile);
        
        long startTime = System.currentTimeMillis();
        
        // Create and run simulation
        EdgeComputingSimulation simulation = new EdgeComputingSimulation(configFile);
        simulation.runSimulation();
        
        // Export results
        simulation.exportResults(outputDir);
        
        long endTime = System.currentTimeMillis();
        logger.info("Simulation completed in {} ms", (endTime - startTime));
    }
    
    /**
     * Generate visualization charts from simulation results.
     * 
     * @param resultsDir Directory containing results
     * @throws IOException if there's an error reading or writing files
     */
    private static void generateVisualizations(String resultsDir) throws IOException {
        logger.info("Generating visualizations from results");
        
        String chartsDir = resultsDir + "/charts";
        Files.createDirectories(Paths.get(chartsDir));
        
        // Create and run visualizer
        ResultsVisualizer visualizer = new ResultsVisualizer(resultsDir, chartsDir);
        visualizer.generateAllCharts();
        
        logger.info("Visualization charts generated successfully");
    }
}
