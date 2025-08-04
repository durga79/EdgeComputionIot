package org.edgecomputing.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.edgecomputing.simulation.EdgeComputingSimulation;
import org.edgecomputing.utils.ResultsVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates and compares multiple simulation scenarios.
 * Runs different configurations, collects results, and generates comparison reports.
 */
public class ScenarioEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEvaluator.class);
    
    // List of scenario configurations to evaluate
    private final List<ScenarioConfig> scenarios;
    
    // Base output directory
    private final String outputBaseDir;
    
    // Results
    private final Map<String, ScenarioResults> results;
    
    /**
     * Creates a new ScenarioEvaluator.
     * 
     * @param outputBaseDir Base directory for all output files
     */
    public ScenarioEvaluator(String outputBaseDir) {
        this.scenarios = new ArrayList<>();
        this.outputBaseDir = outputBaseDir;
        this.results = new HashMap<>();
        
        // Create output base directory if it doesn't exist
        File dir = new File(outputBaseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Add a scenario configuration to evaluate.
     * 
     * @param name Unique name for this scenario
     * @param configFile Path to the configuration file
     * @param description Human-readable description of the scenario
     * @return this ScenarioEvaluator for method chaining
     */
    public ScenarioEvaluator addScenario(String name, String configFile, String description) {
        scenarios.add(new ScenarioConfig(name, configFile, description));
        return this;
    }
    
    /**
     * Run all configured scenarios.
     * 
     * @throws IOException if there's an error reading/writing files
     */
    public void runScenarios() throws IOException {
        logger.info("Starting evaluation of {} scenarios", scenarios.size());
        
        for (ScenarioConfig scenario : scenarios) {
            logger.info("Running scenario: {}", scenario.name);
            logger.info("Description: {}", scenario.description);
            logger.info("Configuration: {}", scenario.configFile);
            
            // Create scenario output directory
            String scenarioOutputDir = outputBaseDir + "/" + scenario.name;
            Files.createDirectories(Paths.get(scenarioOutputDir));
            
            // Run simulation
            long startTime = System.currentTimeMillis();
            EdgeComputingSimulation simulation = new EdgeComputingSimulation(scenario.configFile);
            simulation.runSimulation();
            simulation.exportResults(scenarioOutputDir);
            long endTime = System.currentTimeMillis();
            
            // Generate visualizations
            String chartsDir = scenarioOutputDir + "/charts";
            ResultsVisualizer visualizer = new ResultsVisualizer(scenarioOutputDir, chartsDir);
            visualizer.generateAllCharts();
            
            // Record execution time
            long executionTime = endTime - startTime;
            logger.info("Scenario '{}' completed in {} ms", scenario.name, executionTime);
            
            // Extract and store key results
            ScenarioResults scenarioResults = extractResults(scenarioOutputDir, executionTime);
            results.put(scenario.name, scenarioResults);
        }
        
        // Generate comparative analysis
        generateComparisonReport();
        
        logger.info("All scenarios completed successfully");
    }
    
    /**
     * Extract key metrics from a scenario's results.
     * 
     * @param scenarioOutputDir Directory containing scenario results
     * @param executionTime Execution time in milliseconds
     * @return ScenarioResults containing key metrics
     * @throws IOException if there's an error reading result files
     */
    private ScenarioResults extractResults(String scenarioOutputDir, long executionTime) throws IOException {
        ScenarioResults results = new ScenarioResults();
        results.executionTimeMs = executionTime;
        
        // Read the summary.txt file to extract key metrics
        String summaryPath = scenarioOutputDir + "/summary.txt";
        List<String> lines = Files.readAllLines(Paths.get(summaryPath));
        
        for (String line : lines) {
            line = line.trim();
            
            // Extract task distribution
            if (line.startsWith("Total tasks:")) {
                results.totalTasks = extractIntValue(line);
            } else if (line.startsWith("Tasks executed locally:")) {
                results.localTasks = extractIntValue(line);
                results.localTasksPercentage = extractPercentage(line);
            } else if (line.startsWith("Tasks executed on edge:")) {
                results.edgeTasks = extractIntValue(line);
                results.edgeTasksPercentage = extractPercentage(line);
            } else if (line.startsWith("Tasks executed on cloud:")) {
                results.cloudTasks = extractIntValue(line);
                results.cloudTasksPercentage = extractPercentage(line);
            }
            
            // Extract performance metrics
            else if (line.startsWith("Deadline met:")) {
                results.deadlineMetPercentage = extractPercentageDirectly(line);
            } else if (line.startsWith("Average response time:")) {
                results.avgResponseTime = extractDoubleValue(line, "seconds");
            } else if (line.startsWith("Average execution time:")) {
                results.avgExecutionTime = extractDoubleValue(line, "seconds");
            } else if (line.startsWith("Average energy consumed:")) {
                results.avgEnergyConsumed = extractDoubleValue(line, "units");
            } else if (line.startsWith("Average final battery level:")) {
                results.avgFinalBatteryLevel = extractPercentageDirectly(line);
            }
            
            // Extract utilization metrics
            else if (line.startsWith("Average CPU utilization:")) {
                results.avgCpuUtilization = extractPercentageDirectly(line);
            } else if (line.startsWith("Average RAM utilization:")) {
                results.avgRamUtilization = extractPercentageDirectly(line);
            } else if (line.startsWith("Average bandwidth utilization:")) {
                results.avgBwUtilization = extractPercentageDirectly(line);
            }
        }
        
        return results;
    }
    
    /**
     * Generate a comparative report of all scenario results.
     * 
     * @throws IOException if there's an error writing the report
     */
    private void generateComparisonReport() throws IOException {
        logger.info("Generating comparison report");
        
        String reportPath = outputBaseDir + "/comparison_report.html";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
            // HTML header
            writer.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            writer.write("  <meta charset=\"UTF-8\">\n");
            writer.write("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("  <title>Edge Computing Simulation Comparison</title>\n");
            writer.write("  <style>\n");
            writer.write("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write("    h1 { color: #2c3e50; }\n");
            writer.write("    h2 { color: #3498db; margin-top: 30px; }\n");
            writer.write("    table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
            writer.write("    th, td { border: 1px solid #ddd; padding: 8px; text-align: right; }\n");
            writer.write("    th { background-color: #f2f2f2; text-align: center; }\n");
            writer.write("    th:first-child, td:first-child { text-align: left; }\n");
            writer.write("    tr:hover { background-color: #f5f5f5; }\n");
            writer.write("    .best { font-weight: bold; color: #27ae60; }\n");
            writer.write("    .worst { color: #e74c3c; }\n");
            writer.write("  </style>\n");
            writer.write("</head>\n<body>\n");
            
            // Report header
            writer.write("  <h1>Edge Computing Simulation Comparison Report</h1>\n");
            writer.write("  <p>Generated on " + java.time.LocalDateTime.now() + "</p>\n");
            writer.write("  <p>Comparing " + scenarios.size() + " scenarios</p>\n");
            
            // Scenario descriptions
            writer.write("  <h2>Scenario Descriptions</h2>\n");
            writer.write("  <table>\n");
            writer.write("    <tr><th>Scenario</th><th>Description</th><th>Config File</th></tr>\n");
            
            for (ScenarioConfig scenario : scenarios) {
                writer.write("    <tr><td>" + scenario.name + "</td><td>" + 
                           scenario.description + "</td><td>" + scenario.configFile + "</td></tr>\n");
            }
            writer.write("  </table>\n");
            
            // Task distribution comparison
            writer.write("  <h2>Task Distribution</h2>\n");
            writer.write("  <table>\n");
            writer.write("    <tr><th>Metric</th>");
            for (ScenarioConfig scenario : scenarios) {
                writer.write("<th>" + scenario.name + "</th>");
            }
            writer.write("</tr>\n");
            
            // Total tasks
            writer.write("    <tr><td>Total Tasks</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + result.totalTasks + "</td>");
            }
            writer.write("</tr>\n");
            
            // Tasks executed locally
            writer.write("    <tr><td>Local Tasks</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + result.localTasks + 
                           " (" + formatPercentage(result.localTasksPercentage) + ")</td>");
            }
            writer.write("</tr>\n");
            
            // Tasks executed on edge
            writer.write("    <tr><td>Edge Tasks</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + result.edgeTasks + 
                           " (" + formatPercentage(result.edgeTasksPercentage) + ")</td>");
            }
            writer.write("</tr>\n");
            
            // Tasks executed on cloud
            writer.write("    <tr><td>Cloud Tasks</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + result.cloudTasks + 
                           " (" + formatPercentage(result.cloudTasksPercentage) + ")</td>");
            }
            writer.write("</tr>\n");
            
            // Deadline met percentage
            writer.write("    <tr><td>Deadline Met</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatPercentage(result.deadlineMetPercentage) + "</td>");
            }
            writer.write("</tr>\n");
            writer.write("  </table>\n");
            
            // Performance metrics comparison
            writer.write("  <h2>Performance Metrics</h2>\n");
            writer.write("  <table>\n");
            writer.write("    <tr><th>Metric</th>");
            for (ScenarioConfig scenario : scenarios) {
                writer.write("<th>" + scenario.name + "</th>");
            }
            writer.write("</tr>\n");
            
            // Response time
            writer.write("    <tr><td>Avg Response Time</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatDouble(result.avgResponseTime) + " s</td>");
            }
            writer.write("</tr>\n");
            
            // Execution time
            writer.write("    <tr><td>Avg Execution Time</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatDouble(result.avgExecutionTime) + " s</td>");
            }
            writer.write("</tr>\n");
            
            // Energy consumed
            writer.write("    <tr><td>Avg Energy Consumed</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatDouble(result.avgEnergyConsumed) + " units</td>");
            }
            writer.write("</tr>\n");
            
            // Battery level
            writer.write("    <tr><td>Avg Final Battery</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatPercentage(result.avgFinalBatteryLevel) + "</td>");
            }
            writer.write("</tr>\n");
            writer.write("  </table>\n");
            
            // Resource utilization comparison
            writer.write("  <h2>Resource Utilization</h2>\n");
            writer.write("  <table>\n");
            writer.write("    <tr><th>Metric</th>");
            for (ScenarioConfig scenario : scenarios) {
                writer.write("<th>" + scenario.name + "</th>");
            }
            writer.write("</tr>\n");
            
            // CPU utilization
            writer.write("    <tr><td>Avg CPU Utilization</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatPercentage(result.avgCpuUtilization) + "</td>");
            }
            writer.write("</tr>\n");
            
            // RAM utilization
            writer.write("    <tr><td>Avg RAM Utilization</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatPercentage(result.avgRamUtilization) + "</td>");
            }
            writer.write("</tr>\n");
            
            // Bandwidth utilization
            writer.write("    <tr><td>Avg BW Utilization</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatPercentage(result.avgBwUtilization) + "</td>");
            }
            writer.write("</tr>\n");
            
            // Execution time
            writer.write("    <tr><td>Simulation Time</td>");
            for (ScenarioConfig scenario : scenarios) {
                ScenarioResults result = results.get(scenario.name);
                writer.write("<td>" + formatDouble(result.executionTimeMs / 1000.0) + " s</td>");
            }
            writer.write("</tr>\n");
            writer.write("  </table>\n");
            
            // Summary
            writer.write("  <h2>Summary</h2>\n");
            writer.write("  <p>Key findings:</p>\n");
            writer.write("  <ul>\n");
            
            // Add summary points based on results
            if (results.size() > 1) {
                ScenarioResults energyConfig = results.get("energy-efficient");
                ScenarioResults latencyConfig = results.get("latency-optimized");
                
                if (energyConfig != null && latencyConfig != null) {
                    if (energyConfig.avgEnergyConsumed < latencyConfig.avgEnergyConsumed) {
                        writer.write("    <li>The energy-efficient configuration reduced energy consumption by " + 
                                   formatDouble(100 * (1 - energyConfig.avgEnergyConsumed / latencyConfig.avgEnergyConsumed)) + 
                                   "% compared to the latency-optimized configuration.</li>\n");
                    }
                    
                    if (latencyConfig.avgResponseTime < energyConfig.avgResponseTime) {
                        writer.write("    <li>The latency-optimized configuration reduced response time by " + 
                                   formatDouble(100 * (1 - latencyConfig.avgResponseTime / energyConfig.avgResponseTime)) + 
                                   "% compared to the energy-efficient configuration.</li>\n");
                    }
                    
                    if (energyConfig.avgFinalBatteryLevel > latencyConfig.avgFinalBatteryLevel) {
                        writer.write("    <li>The energy-efficient configuration preserved more battery life, ending with " + 
                                   formatPercentage(energyConfig.avgFinalBatteryLevel) + " battery remaining vs " + 
                                   formatPercentage(latencyConfig.avgFinalBatteryLevel) + " for latency-optimized.</li>\n");
                    }
                }
                
                // Add more summary points for high-density scenario if available
                ScenarioResults highDensity = results.get("high-density");
                if (highDensity != null) {
                    writer.write("    <li>The high-density scenario processed " + highDensity.totalTasks + 
                               " tasks with " + formatPercentage(highDensity.deadlineMetPercentage) + 
                               " meeting their deadlines.</li>\n");
                }
            }
            
            writer.write("  </ul>\n");
            
            // Conclusion
            writer.write("  <h2>Conclusion</h2>\n");
            writer.write("  <p>This evaluation demonstrates the trade-offs between energy efficiency and latency " +
                       "in edge computing environments. The service slicing and task offloading strategies " +
                       "implemented in this project allow for flexible configuration based on specific " +
                       "deployment requirements.</p>\n");
            
            // HTML footer
            writer.write("</body>\n</html>\n");
        }
        
        logger.info("Comparison report generated at: {}", reportPath);
    }
    
    /**
     * Helper method to extract an integer value from a line.
     */
    private int extractIntValue(String line) {
        String[] parts = line.split(":");
        if (parts.length < 2) return 0;
        
        String valueStr = parts[1].trim().split("\\s+")[0];
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Helper method to extract a double value from a line.
     */
    private double extractDoubleValue(String line, String unit) {
        String[] parts = line.split(":");
        if (parts.length < 2) return 0.0;
        
        String valueStr = parts[1].trim().split("\\s+" + unit)[0];
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Helper method to extract a percentage value from a line.
     */
    private double extractPercentage(String line) {
        String[] parts = line.split("\\(");
        if (parts.length < 2) return 0.0;
        
        String percentStr = parts[1].split("%")[0];
        try {
            return Double.parseDouble(percentStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Helper method to extract a percentage value directly from a line.
     */
    private double extractPercentageDirectly(String line) {
        String[] parts = line.split(":");
        if (parts.length < 2) return 0.0;
        
        String percentStr = parts[1].trim().split("%")[0];
        try {
            return Double.parseDouble(percentStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Format a double value to two decimal places.
     */
    private String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(value);
    }
    
    /**
     * Format a percentage value.
     */
    private String formatPercentage(double value) {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(value) + "%";
    }
    
    /**
     * Inner class to represent a scenario configuration.
     */
    private static class ScenarioConfig {
        private final String name;
        private final String configFile;
        private final String description;
        
        public ScenarioConfig(String name, String configFile, String description) {
            this.name = name;
            this.configFile = configFile;
            this.description = description;
        }
    }
    
    /**
     * Inner class to store key results from a scenario.
     */
    private static class ScenarioResults {
        // Task distribution
        public int totalTasks = 0;
        public int localTasks = 0;
        public int edgeTasks = 0;
        public int cloudTasks = 0;
        public double localTasksPercentage = 0.0;
        public double edgeTasksPercentage = 0.0;
        public double cloudTasksPercentage = 0.0;
        
        // Performance metrics
        public double deadlineMetPercentage = 0.0;
        public double avgResponseTime = 0.0;
        public double avgExecutionTime = 0.0;
        public double avgEnergyConsumed = 0.0;
        public double avgFinalBatteryLevel = 0.0;
        
        // Resource utilization
        public double avgCpuUtilization = 0.0;
        public double avgRamUtilization = 0.0;
        public double avgBwUtilization = 0.0;
        
        // Execution time
        public long executionTimeMs = 0;
    }
    
    /**
     * Main method to run the evaluator.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Default output directory
            String outputDir = "evaluation_results";
            
            // Use output directory from arguments if provided
            if (args.length > 0) {
                outputDir = args[0];
            }
            
            logger.info("Starting scenario evaluation");
            logger.info("Output directory: {}", outputDir);
            
            // Create and run evaluator
            ScenarioEvaluator evaluator = new ScenarioEvaluator(outputDir);
            
            // Add scenarios
            evaluator.addScenario(
                    "baseline", 
                    "src/main/resources/simulation_config.json",
                    "Baseline scenario with balanced energy and latency weights")
                    
                .addScenario(
                    "energy-efficient", 
                    "src/main/resources/configs/energy_efficient_config.json",
                    "Energy-efficient scenario prioritizing battery preservation")
                    
                .addScenario(
                    "latency-optimized", 
                    "src/main/resources/configs/latency_optimized_config.json",
                    "Latency-optimized scenario prioritizing fast response times")
                    
                .addScenario(
                    "high-density", 
                    "src/main/resources/configs/high_density_config.json",
                    "High-density scenario with large number of IoT devices");
            
            // Run all scenarios
            evaluator.runScenarios();
            
            logger.info("Scenario evaluation completed");
            
        } catch (Exception e) {
            logger.error("Error in scenario evaluation", e);
            e.printStackTrace();
        }
    }
}
