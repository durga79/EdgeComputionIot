package org.edgecomputing.simulation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.edgecomputing.models.CloudDatacenter;
import org.edgecomputing.models.EdgeNode;
import org.edgecomputing.models.IoTDevice;
import org.edgecomputing.models.Task;
import org.edgecomputing.policies.EnergyAwareOffloadingPolicy;
import org.edgecomputing.policies.OffloadingPolicy;
import org.edgecomputing.policies.ServiceSlicingPolicy;
import org.edgecomputing.policies.TaskScheduler;
import org.edgecomputing.utils.SimulationResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main simulation class for Edge Computing IoT project.
 * This class initializes the simulation environment, configures components,
 * and runs the simulation based on the paper:
 * "IoT Service Slicing and Task Offloading for Edge Computing"
 */
public class EdgeComputingSimulation {
    
    private static final Logger logger = LoggerFactory.getLogger(EdgeComputingSimulation.class);
    
    // Simulation configuration
    private JsonObject config;
    
    // Simulation components
    private List<IoTDevice> devices;
    private List<EdgeNode> edgeNodes;
    private CloudDatacenter cloudDatacenter;
    private OffloadingPolicy offloadingPolicy;
    private ServiceSlicingPolicy slicingPolicy;
    private TaskScheduler taskScheduler;
    
    // Simulation parameters
    private double simStartTime = 0.0;
    private double simEndTime = 3600.0; // Default 1 hour simulation
    private double timeStep = 0.1;      // Default 100ms time step
    private Random random;
    
    // Results
    private SimulationResults results;
    
    /**
     * Create and initialize the simulation.
     *
     * @param configFile Path to the simulation configuration JSON file
     * @throws IOException if the configuration file cannot be read
     */
    public EdgeComputingSimulation(String configFile) throws IOException {
        logger.info("Initializing Edge Computing Simulation");
        
        // Load configuration
        loadConfiguration(configFile);
        
        // Set random seed for reproducibility
        random = new Random(42);
        
        // Initialize components
        devices = new ArrayList<>();
        edgeNodes = new ArrayList<>();
        initializeComponents();
        
        // Create results tracker
        results = new SimulationResults();
        
        logger.info("Simulation initialized with {} IoT devices and {} edge nodes", 
                    devices.size(), edgeNodes.size());
    }
    
    /**
     * Load the simulation configuration from JSON file.
     *
     * @param configFile Path to the configuration file
     * @throws IOException if the file cannot be read
     */
    private void loadConfiguration(String configFile) throws IOException {
        logger.info("Loading configuration from {}", configFile);
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, JsonObject.class);
        }
        
        // Get simulation parameters
        if (config.has("simulation")) {
            JsonObject simConfig = config.getAsJsonObject("simulation");
            
            // Time parameters
            if (simConfig.has("duration")) {
                simEndTime = simConfig.get("duration").getAsDouble();
            }
            if (simConfig.has("time_step")) {
                timeStep = simConfig.get("time_step").getAsDouble();
            }
        }
        
        logger.info("Simulation configured with duration={} seconds, timeStep={} seconds", 
                   simEndTime, timeStep);
    }
    
    /**
     * Initialize simulation components from configuration.
     */
    private void initializeComponents() {
        // Initialize offloading policy
        JsonObject policyConfig = config.getAsJsonObject("offloading_policy");
        String policyType = policyConfig.get("type").getAsString();
        
        JsonObject policyParams = policyConfig.getAsJsonObject("parameters");
        // Use default values for parameters that might not be in the config
        double batteryThreshold = 0.2; // Default 20% battery threshold
        long taskSizeThreshold = 1000000; // Default 1MB task size threshold
        double networkQualityThreshold = 0.7; // Default 70% network quality threshold
        
        // Get weights from parameters
        double weightEnergy = policyParams.get("weight_energy").getAsDouble();
        double weightLatency = policyParams.get("weight_latency").getAsDouble();
        double weightCost = policyParams.get("weight_cost").getAsDouble();
        
        logger.info("Creating energy-aware offloading policy with weights: energy={}, latency={}, cost={}",
                   weightEnergy, weightLatency, weightCost);
                   
        offloadingPolicy = new EnergyAwareOffloadingPolicy(
                batteryThreshold,
                taskSizeThreshold,
                networkQualityThreshold,
                weightEnergy,
                weightLatency,
                weightCost
        );
        
        // Initialize service slicing
        JsonObject slicingConfig = config.getAsJsonObject("service_slicing");
        slicingPolicy = new ServiceSlicingPolicy();
        
        // Configure slicing policy from config
        if (slicingConfig.has("slices")) {
            JsonArray slices = slicingConfig.getAsJsonArray("slices");
            for (int i = 0; i < slices.size(); i++) {
                JsonObject slice = slices.get(i).getAsJsonObject();
                String name = slice.get("name").getAsString();
                double resourcePercentage = slice.get("resource_percentage").getAsDouble();
                int priority = slice.get("priority").getAsInt();
                
                // Convert task types to List<String>
                List<String> taskTypes = new ArrayList<>();
                JsonArray taskTypeArray = slice.getAsJsonArray("task_types");
                for (int j = 0; j < taskTypeArray.size(); j++) {
                    taskTypes.add(taskTypeArray.get(j).getAsString());
                }
                
                slicingPolicy.addSliceConfiguration(name, resourcePercentage, priority, taskTypes);
            }
        }
        
        // Initialize task scheduler
        taskScheduler = new TaskScheduler(offloadingPolicy, slicingPolicy);
        
        // Initialize edge nodes
        initializeEdgeNodes();
        
        // Initialize cloud datacenter
        initializeCloud();
        
        // Initialize IoT devices
        initializeIoTDevices();
    }
    
    /**
     * Initialize edge nodes from configuration.
     */
    private void initializeEdgeNodes() {
        JsonObject edgeNodesConfig = config.getAsJsonObject("edge_nodes");
        int totalEdgeCount = edgeNodesConfig.get("count").getAsInt();
        
        JsonArray edgeTypes = edgeNodesConfig.getAsJsonArray("types");
        int edgeIdCounter = 0;
        
        logger.info("Initializing {} edge nodes", totalEdgeCount);
        
        for (int typeIdx = 0; typeIdx < edgeTypes.size(); typeIdx++) {
            JsonObject typeConfig = edgeTypes.get(typeIdx).getAsJsonObject();
            String nodeType = typeConfig.get("name").getAsString();
            
            // Distribute edge nodes according to type ratio
            int count = Math.max(1, totalEdgeCount / edgeTypes.size());
            
            for (int i = 0; i < count; i++) {
                EdgeNode edgeNode = new EdgeNode(
                        edgeIdCounter++,
                        "Edge-" + nodeType + "-" + i,
                        typeConfig.get("mips").getAsInt(),
                        typeConfig.get("ram").getAsInt(),
                        typeConfig.get("storage").getAsInt(),
                        typeConfig.get("bw").getAsInt(),
                        typeConfig.get("cost_per_mips").getAsDouble(),
                        random.nextDouble() * 1000,  // Random X location
                        random.nextDouble() * 1000   // Random Y location
                );
                
                edgeNodes.add(edgeNode);
                logger.debug("Created edge node: {}", edgeNode);
            }
        }
    }
    
    /**
     * Initialize cloud datacenter from configuration.
     */
    private void initializeCloud() {
        JsonObject cloudConfig = config.getAsJsonObject("cloud");
        
        cloudDatacenter = new CloudDatacenter(
                0,
                "Cloud-Datacenter",
                cloudConfig.get("mips").getAsInt(),
                cloudConfig.get("ram").getAsInt(),
                cloudConfig.get("storage").getAsInt(),
                cloudConfig.get("bw").getAsInt(),
                cloudConfig.get("cost_per_mips").getAsDouble(),
                cloudConfig.get("latency_to_edge_ms").getAsDouble()
        );
        
        logger.debug("Created cloud datacenter: {}", cloudDatacenter);
    }
    
    /**
     * Initialize IoT devices from configuration.
     */
    private void initializeIoTDevices() {
        JsonObject deviceConfig = config.getAsJsonObject("iot_devices");
        int totalDeviceCount = deviceConfig.get("count").getAsInt();
        
        JsonArray deviceTypes = deviceConfig.getAsJsonArray("types");
        int deviceIdCounter = 0;
        
        logger.info("Initializing {} IoT devices", totalDeviceCount);
        
        for (int typeIdx = 0; typeIdx < deviceTypes.size(); typeIdx++) {
            JsonObject typeConfig = deviceTypes.get(typeIdx).getAsJsonObject();
            String deviceType = typeConfig.get("name").getAsString();
            
            // Distribute devices according to type ratio
            int count = Math.max(1, totalDeviceCount / deviceTypes.size());
            
            for (int i = 0; i < count; i++) {
                IoTDevice device = new IoTDevice(
                        deviceIdCounter++,
                        "Device-" + deviceType + "-" + i,
                        typeConfig.get("mips").getAsInt(),
                        typeConfig.get("ram").getAsInt(),
                        typeConfig.get("battery_capacity").getAsDouble(),
                        typeConfig.get("battery_consumption_rate").getAsDouble(),
                        typeConfig.get("wireless_technology").getAsString(),
                        typeConfig.get("task_generation_rate").getAsDouble(),
                        typeConfig.get("mobility").getAsBoolean(),
                        typeConfig.get("mobility_speed").getAsDouble(),
                        random.nextDouble() * 1000,  // Random X location
                        random.nextDouble() * 1000   // Random Y location
                );
                
                // Set network parameters
                String wirelessTech = typeConfig.get("wireless_technology").getAsString();
                JsonObject networkConfig = config.getAsJsonObject("network")
                                                .getAsJsonObject("technologies")
                                                .getAsJsonObject(wirelessTech);
                device.setNetworkParameters(
                        networkConfig.get("latency_ms").getAsDouble(),
                        networkConfig.get("bandwidth").getAsDouble(),
                        networkConfig.has("reliability") ? networkConfig.get("reliability").getAsDouble() : 0.95, // Default reliability if not specified
                        networkConfig.has("energy_per_bit") ? networkConfig.get("energy_per_bit").getAsDouble() : 0.0001 // Default energy consumption
                );
                
                // Add supported task types
                if (typeConfig.has("supported_task_types")) {
                    Gson gson = new Gson();
                    JsonArray supportedTypes = typeConfig.getAsJsonArray("supported_task_types");
                    List<String> taskTypes = new ArrayList<>();
                    for (int j = 0; j < supportedTypes.size(); j++) {
                        taskTypes.add(supportedTypes.get(j).getAsString());
                    }
                    device.setSupportedTaskTypes(taskTypes);
                } else {
                    // Default - support all task types
                    device.setSupportedTaskTypes(Arrays.asList("lightweight", "medium", "intensive"));
                }
                
                devices.add(device);
            }
        }
    }
    
    /**
     * Run the simulation from start time to end time.
     */
    public void runSimulation() {
        logger.info("Starting simulation with duration={} seconds, timeStep={} seconds", 
                   simEndTime, timeStep);
        
        // Use wrapper classes to hold mutable state that needs to be accessed from lambdas
        final class SimulationState {
            double currentTime = simStartTime;
            int totalTasksGenerated = 0;
            boolean completed = false;
        }
        
        final SimulationState state = new SimulationState();
        
        // Set timeout to prevent infinite loops
        final int timeout = 60; // seconds
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.submit(() -> {
            try {
                // Main simulation loop
                while (state.currentTime < simEndTime) {
                    final double currentTimeSnapshot = state.currentTime; // Create effectively final copy
                    
                    // Generate new tasks from IoT devices
                    for (IoTDevice device : devices) {
                        List<Task> newTasks = device.generateTasks(currentTimeSnapshot, timeStep);
                        for (Task task : newTasks) {
                            taskScheduler.submitTask(task);
                            state.totalTasksGenerated++;
                        }
                    }
                    
                    // Process task offloading and scheduling
                    taskScheduler.schedulePendingTasks(devices, edgeNodes, cloudDatacenter, currentTimeSnapshot);
                    
                    // Update task status
                    taskScheduler.updateTaskStatus(devices, edgeNodes, cloudDatacenter, currentTimeSnapshot);
                    
                    // Collect metrics at this time step
                    collectMetrics(currentTimeSnapshot);
                    
                    // Advance simulation time
                    state.currentTime += timeStep;
                    
                    // Print progress every 10% of simulation time
                    if (Math.floor(currentTimeSnapshot / (simEndTime / 10)) > 
                        Math.floor((currentTimeSnapshot - timeStep) / (simEndTime / 10))) {
                        logger.info("Simulation {}% complete. Tasks generated: {}, Tasks completed: {}",
                                   (int)(currentTimeSnapshot / simEndTime * 100),
                                   state.totalTasksGenerated,
                                   taskScheduler.getCompletedTaskCount());
                    }
                }
                
                logger.info("Simulation completed. Total tasks: {}, Completed: {}, Pending: {}, Running: {}",
                           state.totalTasksGenerated,
                           taskScheduler.getCompletedTaskCount(),
                           taskScheduler.getPendingTaskCount(),
                           taskScheduler.getRunningTaskCount());
                
                state.completed = true;
            } catch (Exception e) {
                logger.error("Error during simulation execution", e);
            }
        });
        
        try {
            executor.shutdown();
            if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                logger.warn("Simulation timed out after {} seconds. Forcing termination.", timeout);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Simulation was interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (!state.completed) {
            logger.warn("Simulation did not complete within the time limit. Results may be incomplete.");
        }
    }
    
    /**
     * Get the number of completed tasks.
     *
     * @return The number of completed tasks
     */
    public int getCompletedTaskCount() {
        return taskScheduler.getCompletedTaskCount();
    }
    
    /**
     * Collect metrics at the current simulation time.
     *
     * @param currentTime The current simulation time
     */
    private void collectMetrics(double currentTime) {
        // Device metrics
        for (IoTDevice device : devices) {
            results.recordDeviceState(
                    currentTime, 
                    device.getDeviceId(), 
                    device.getBatteryLevel(), 
                    device.getTasksExecutedLocally(),
                    device.getTasksOffloaded());
        }
        
        // Edge node metrics
        for (EdgeNode node : edgeNodes) {
            results.recordEdgeNodeState(
                    currentTime,
                    node.getNodeId(),
                    node.getCpuUtilization(),
                    node.getRamUtilization(),
                    node.getBwUtilization(),
                    node.getActiveTaskCount(),
                    node.getTotalTasksProcessed());
        }
        
        // Cloud metrics
        results.recordCloudState(
                currentTime,
                cloudDatacenter.getCpuUtilization(),
                cloudDatacenter.getRamUtilization(),
                cloudDatacenter.getBwUtilization(),
                cloudDatacenter.getActiveTasks().size(),
                cloudDatacenter.getTotalTasksProcessed());
        
        // Task metrics
        if (taskScheduler.getCompletedTaskCount() > 0) {
            Map<String, Double> taskStats = taskScheduler.getTaskStatistics();
            results.recordTaskMetrics(
                    currentTime,
                    taskStats.getOrDefault("averageResponseTime", 0.0),
                    taskStats.getOrDefault("averageExecutionTime", 0.0),
                    taskStats.getOrDefault("averageEnergyConsumed", 0.0),
                    taskStats.getOrDefault("localTasks", 0.0).intValue(),
                    taskStats.getOrDefault("edgeTasks", 0.0).intValue(),
                    taskStats.getOrDefault("cloudTasks", 0.0).intValue(),
                    taskStats.getOrDefault("deadlineMetPercentage", 0.0));
        }
    }
    
    /**
     * Get the simulation results.
     *
     * @return The simulation results
     */
    public SimulationResults getResults() {
        return results;
    }
    
    /**
     * Export simulation results to CSV files.
     *
     * @param outputDir Directory to export results to
     * @throws IOException if there's an error writing the files
     */
    public void exportResults(String outputDir) throws IOException {
        logger.info("Exporting simulation results to {}", outputDir);
        
        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get(outputDir));
        
        // Export device metrics
        results.exportDeviceMetrics(outputDir + "/device_metrics.csv");
        
        // Export edge node metrics
        results.exportEdgeNodeMetrics(outputDir + "/edge_node_metrics.csv");
        
        // Export cloud metrics
        results.exportCloudMetrics(outputDir + "/cloud_metrics.csv");
        
        // Export task metrics
        results.exportTaskMetrics(outputDir + "/task_metrics.csv");
        
        // Export summary metrics
        results.exportSummary(outputDir + "/summary.json");
        
        logger.info("Results exported successfully");
    }
    
    // Helper methods and getters are defined above
    
    /**
     * Main method to run the simulation.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            String configFile = args.length > 0 ? args[0] : "src/main/resources/simulation_config.json";
            String outputDir = args.length > 1 ? args[1] : "results";
            
            logger.info("Starting Edge Computing Simulation");
            logger.info("Configuration file: {}", configFile);
            logger.info("Output directory: {}", outputDir);
            
            EdgeComputingSimulation simulation = new EdgeComputingSimulation(configFile);
            simulation.runSimulation();
            simulation.exportResults(outputDir);
            
            logger.info("Edge Computing Simulation completed successfully");
            
        } catch (Exception e) {
            logger.error("Error running Edge Computing Simulation", e);
            e.printStackTrace();
        }
    }
}
