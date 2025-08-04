package org.edgecomputing.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.vms.Vm;

/**
 * Represents an IoT device that can generate tasks and make offloading decisions.
 * This class models different types of IoT devices with varying capabilities,
 * battery constraints, and network connections.
 */
public class IoTDevice {
    private final int deviceId;
    private final String deviceType;
    private final int mips;
    private final int ram;
    private final double batteryCapacity;
    private double currentBattery;
    private final double batteryConsumptionRate;
    private final double taskGenerationRate;
    private final String wirelessTech;
    private final boolean mobility;
    
    // Location coordinates (x, y)
    private double locationX;
    private double locationY;
    
    // Connected edge node (if any)
    private int connectedEdgeNode;
    
    // Network parameters for the device's wireless technology
    private double latencyMs;
    private double bandwidthMbps;
    private double reliability;
    private double networkEnergyConsumption;
    private List<String> supportedTaskTypes;
    
    // Offloading policy thresholds
    private final double thresholdBatteryLevel;
    private final long thresholdTaskSize;
    private final double thresholdNetworkQuality;
    private final double weightEnergy;
    private final double weightLatency;
    private final double weightCost;
    
    // Performance metrics
    private int tasksGenerated;
    private int tasksOffloaded;
    private int tasksProcessedLocally;
    private double energyConsumed;
    
    // Associated VM in CloudSim (if device has processing capability)
    private Vm vm;
    
    // Random number generator
    private final Random random;
    
    // Tasks currently being processed locally
    private final List<Task> localTasks;
    
    /**
     * Creates a new IoT device with specified parameters.
     *
     * @param deviceId Unique identifier for this device
     * @param deviceType Type of device (sensor, smartphone, etc.)
     * @param mips Processing capability in MIPS
     * @param ram Memory in MB
     * @param batteryCapacity Maximum battery capacity in mAh
     * @param batteryConsumptionRate Rate at which battery is consumed per second
     * @param taskGenerationRate Rate at which tasks are generated per second
     * @param wirelessTech Wireless technology used (WiFi, LTE, etc.)
     * @param mobility Whether the device is mobile or stationary
     * @param latencyMs Network latency in milliseconds
     * @param bandwidthMbps Network bandwidth in Mbps
     * @param reliability Network reliability (0-1)
     * @param networkEnergyConsumption Energy used for network transmission
     * @param thresholdBatteryLevel Battery threshold for offloading decision
     * @param thresholdTaskSize Task size threshold for offloading decision
     * @param thresholdNetworkQuality Network quality threshold for offloading decision
     * @param weightEnergy Weight of energy in offloading decision
     * @param weightLatency Weight of latency in offloading decision
     * @param weightCost Weight of cost in offloading decision
     */
    public IoTDevice(int deviceId, String deviceType, int mips, int ram, 
                    double batteryCapacity, double batteryConsumptionRate, 
                    String wirelessTech, double taskGenerationRate, boolean mobility, 
                    double mobilitySpeed, double locationX, double locationY) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.mips = mips;
        this.ram = ram;
        this.batteryCapacity = batteryCapacity;
        this.currentBattery = batteryCapacity;  // Start with full battery
        this.batteryConsumptionRate = batteryConsumptionRate;
        this.wirelessTech = wirelessTech;
        this.taskGenerationRate = taskGenerationRate;
        this.mobility = mobility;
        
        // Set location
        this.random = new Random();
        this.locationX = locationX;
        this.locationY = locationY;
        this.connectedEdgeNode = -1;
        
        // Default network parameters - will be set by setNetworkParameters
        this.latencyMs = 10; // Default values
        this.bandwidthMbps = 10; // Default values
        this.reliability = 0.9; // Default values
        this.networkEnergyConsumption = 0.1; // Default values
        
        // Default offloading thresholds - can be set later
        this.thresholdBatteryLevel = 0.2; // Default values
        this.thresholdTaskSize = 5000; // Default values
        this.thresholdNetworkQuality = 0.5; // Default values
        this.weightEnergy = 0.33; // Default values
        this.weightLatency = 0.33; // Default values
        this.weightCost = 0.34; // Default values
        
        this.tasksGenerated = 0;
        this.tasksOffloaded = 0;
        this.tasksProcessedLocally = 0;
        this.energyConsumed = 0;
        
        this.localTasks = new ArrayList<>();
        this.supportedTaskTypes = new ArrayList<>();
    }
    
    /**
     * Update battery level based on time elapsed and device activity.
     *
     * @param timeElapsed Time elapsed since last update in seconds
     * @return true if device is still operational, false if battery depleted
     */
    public boolean updateBattery(double timeElapsed) {
        double consumption = batteryConsumptionRate * timeElapsed;
        currentBattery -= consumption;
        energyConsumed += consumption;
        
        if (currentBattery <= 0) {
            currentBattery = 0;
            System.out.println("Device " + deviceId + " battery depleted!");
            return false;
        }
        return true;
    }
    
    /**
     * Set network parameters for this device
     * 
     * @param latencyMs Network latency in milliseconds
     * @param bandwidthMbps Network bandwidth in Mbps
     * @param reliability Network reliability (0-1)
     * @param energyConsumption Energy consumed for network operations
     */
    public void setNetworkParameters(double latencyMs, double bandwidthMbps, double reliability, double energyConsumption) {
        this.latencyMs = latencyMs;
        this.bandwidthMbps = bandwidthMbps;
        this.reliability = reliability;
        this.networkEnergyConsumption = energyConsumption;
    }
    
    /**
     * Set the task types this device supports
     * 
     * @param taskTypes List of task type names
     */
    public void setSupportedTaskTypes(List<String> taskTypes) {
        this.supportedTaskTypes = new ArrayList<>(taskTypes);
    }
    
    /**
     * Update device position if it has mobility
     * 
     * @param newX New X coordinate
     * @param newY New Y coordinate
     */
    public void updatePosition(double newX, double newY) {
        if (mobility) {
            this.locationX = newX;
            this.locationY = newY;
        }
    }
    
    /**
     * Generate tasks based on probability and elapsed time
     * 
     * @param timeStep Current simulation time step
     * @param currentTime Current simulation time
     * @return List of generated tasks
     */
    public List<Task> generateTasks(double timeStep, double currentTime) {
        List<Task> generatedTasks = new ArrayList<>();
        
        // Simple probability-based task generation
        if (random.nextDouble() < taskGenerationRate * timeStep) {
            Task task = generateTask(currentTime);
            if (task != null) {
                generatedTasks.add(task);
            }
        }
        
        return generatedTasks;
    }
    
    /**
     * Generate a new task based on the device's task generation rate.
     *
     * @param currentTime Current simulation time
     * @return A new Task if one should be generated, null otherwise
     */
    public Task generateTask(double currentTime) {
        // Determine if a task should be generated based on probability
        if (random.nextDouble() <= taskGenerationRate) {
            String taskId = deviceId + "_" + tasksGenerated;
            tasksGenerated++;
            
            // Choose task type based on device capabilities
            String taskType;
            if (mips < 1000) {
                taskType = "lightweight";
            } else if (mips < 3000) {
                taskType = random.nextBoolean() ? "lightweight" : "medium";
            } else {
                int rand = random.nextInt(3);
                if (rand == 0) taskType = "lightweight";
                else if (rand == 1) taskType = "medium";
                else taskType = "intensive";
            }
            
            // Task parameters based on type
            long mipsRequired;
            long inputSize;
            long outputSize;
            double deadline;
            
            switch (taskType) {
                case "medium":
                    mipsRequired = 2000;
                    inputSize = 5120;
                    outputSize = 1024;
                    deadline = 5;
                    break;
                case "intensive":
                    mipsRequired = 8000;
                    inputSize = 10240;
                    outputSize = 2048;
                    deadline = 10;
                    break;
                default:  // lightweight
                    mipsRequired = 500;
                    inputSize = 1024;
                    outputSize = 512;
                    deadline = 2;
                    break;
            }
            
            // Create and return a new task
            Task newTask = new Task(taskId, taskType, mipsRequired, inputSize, 
                                    outputSize, deadline, deviceId, currentTime);
            System.out.println("Device " + deviceId + " generated task " + taskId + " of type " + taskType);
            return newTask;
        }
        
        return null;
    }
    
    /**
     * Decide whether to offload a task based on multiple factors.
     *
     * @param task The task to potentially offload
     * @param edgeNodes Available edge nodes
     * @return true if task should be offloaded, false otherwise
     */
    public boolean shouldOffloadTask(Task task, List<EdgeNode> edgeNodes) {
        // If battery is too low, offload to save energy
        if (currentBattery / batteryCapacity < thresholdBatteryLevel) {
            return true;
        }
            
        // If task is computationally intensive, offload
        if (task.getMipsRequired() > mips) {
            return true;
        }
        
        // If task is large, consider offloading
        if (task.getInputSize() > thresholdTaskSize) {
            // But only if network quality is good enough
            double networkQuality = reliability * (bandwidthMbps / 100.0);  // Normalize to 0-1
            if (networkQuality > thresholdNetworkQuality) {
                return true;
            }
        }
        
        // Calculate estimated completion time locally
        double localCompletionTime = task.getMipsRequired() / (double) mips;
        
        // Find best edge node based on distance
        EdgeNode bestEdgeNode = selectBestEdgeNode(edgeNodes);
        if (bestEdgeNode != null) {
            // Calculate estimated completion time on edge
            // Consider network latency, transfer time, and processing time
            double latencySec = latencyMs / 1000.0;
            double transferTime = task.getInputSize() / (bandwidthMbps * 125000);  // Convert Mbps to bytes/sec
            double edgeProcessingTime = task.getMipsRequired() / (double) bestEdgeNode.getMips();
            double edgeCompletionTime = latencySec + transferTime + edgeProcessingTime;
            
            // Calculate energy consumption for local vs edge processing
            double localEnergy = (task.getMipsRequired() / (double) mips) * batteryConsumptionRate * 2;  // Higher local energy use
            double edgeEnergy = transferTime * networkEnergyConsumption;
            
            // Calculate utility based on weighted factors
            double localUtility = (
                weightEnergy * localEnergy +
                weightLatency * localCompletionTime
            );
            
            double edgeUtility = (
                weightEnergy * edgeEnergy +
                weightLatency * edgeCompletionTime +
                weightCost * (task.getMipsRequired() * bestEdgeNode.getCostPerMips())
            );
            
            // Lower utility is better (costs less)
            return edgeUtility < localUtility;
        }
            
        return false;
    }
    
    /**
     * Select the best edge node based on distance and load.
     *
     * @param edgeNodes Available edge nodes
     * @return The best edge node to connect to, or null if none available
     */
    public EdgeNode selectBestEdgeNode(List<EdgeNode> edgeNodes) {
        if (edgeNodes == null || edgeNodes.isEmpty()) {
            return null;
        }
            
        EdgeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;
        
        for (EdgeNode node : edgeNodes) {
            // Calculate distance to edge node
            double distance = Math.sqrt(
                Math.pow(locationX - node.getLocationX(), 2) + 
                Math.pow(locationY - node.getLocationY(), 2)
            );
            
            // Calculate load factor (0-1, higher is more loaded)
            double loadFactor = node.getUtilization();
            
            // Calculate score (lower is better)
            // We weight distance more heavily for higher latency technologies
            double distanceWeight;
            switch (wirelessTech) {
                case "WiFi":
                    distanceWeight = 1.0;
                    break;
                case "BLE":
                    distanceWeight = 1.5;
                    break;
                case "LTE":
                    distanceWeight = 0.7;
                    break;
                case "FiveG":
                    distanceWeight = 0.5;
                    break;
                default:
                    distanceWeight = 1.0;
            }
            
            double score = (distance * distanceWeight) + (loadFactor * 100);
            
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }
                
        return bestNode;
    }
    
    /**
     * Process a task on the local device.
     *
     * @param task The task to process
     * @param currentTime Current simulation time
     * @return Time when task will be completed
     */
    public double processTaskLocally(Task task, double currentTime) {
        // Calculate execution time based on MIPS
        double executionTime = task.getMipsRequired() / (double) mips;
        
        // Update task status
        task.setStartTime(currentTime);
        task.setCompletionTime(currentTime + executionTime);
        task.setExecutedOn("device_" + deviceId);
        task.setStatus("processing");
        
        // Update device statistics
        tasksProcessedLocally++;
        localTasks.add(task);
        
        // Calculate and update energy consumption
        double energyUsed = executionTime * batteryConsumptionRate * 2;  // Processing uses more energy
        currentBattery -= energyUsed;
        energyConsumed += energyUsed;
        task.setEnergyConsumed(energyUsed);
        
        System.out.println("Device " + deviceId + " processing task " + task.getTaskId() + 
                          " locally, estimated completion at " + task.getCompletionTime());
                     
        return task.getCompletionTime();
    }
    
    /**
     * Update device location if it's mobile.
     *
     * @param timeElapsed Time elapsed since last update
     */
    public void updateLocation(double timeElapsed) {
        if (!mobility) {
            return;
        }
            
        // Simple random movement model
        double speed = 5.0;  // units per second
        double distance = speed * timeElapsed;
        
        // Random direction
        double angle = random.nextDouble() * 2 * Math.PI;
        double dx = distance * Math.cos(angle);
        double dy = distance * Math.sin(angle);
        
        // Update location
        locationX = Math.max(0, Math.min(1000, locationX + dx));
        locationY = Math.max(0, Math.min(1000, locationY + dy));
    }
    
    /**
     * Update tasks being processed locally.
     * 
     * @param currentTime Current simulation time
     * @return List of tasks that have completed
     */
    public List<Task> updateLocalTasks(double currentTime) {
        List<Task> completedTasks = new ArrayList<>();
        List<Task> remainingTasks = new ArrayList<>();
        
        for (Task task : localTasks) {
            if (currentTime >= task.getCompletionTime()) {
                task.setStatus("completed");
                completedTasks.add(task);
            } else {
                remainingTasks.add(task);
            }
        }
        
        // Update the list of local tasks
        localTasks.clear();
        localTasks.addAll(remainingTasks);
        
        return completedTasks;
    }
    
    /**
     * @return the deviceId
     */
    public int getDeviceId() {
        return deviceId;
    }

    /**
     * @return the deviceType
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * @return the mips
     */
    public int getMips() {
        return mips;
    }

    /**
     * @return the ram
     */
    public int getRam() {
        return ram;
    }

    /**
     * @return the batteryCapacity
     */
    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    /**
     * @return the currentBattery
     */
    public double getCurrentBattery() {
        return currentBattery;
    }

    /**
     * @return the batteryConsumptionRate
     */
    public double getBatteryConsumptionRate() {
        return batteryConsumptionRate;
    }

    /**
     * @return the taskGenerationRate
     */
    public double getTaskGenerationRate() {
        return taskGenerationRate;
    }

    /**
     * @return the wirelessTech
     */
    public String getWirelessTech() {
        return wirelessTech;
    }

    /**
     * @return whether the device has mobility
     */
    public boolean hasMobility() {
        return mobility;
    }

    /**
     * @return the locationX
     */
    public double getLocationX() {
        return locationX;
    }

    /**
     * @return the locationY
     */
    public double getLocationY() {
        return locationY;
    }

    /**
     * @return the connectedEdgeNode
     */
    public int getConnectedEdgeNode() {
        return connectedEdgeNode;
    }

    /**
     * @param connectedEdgeNode the connectedEdgeNode to set
     */
    public void setConnectedEdgeNode(int connectedEdgeNode) {
        this.connectedEdgeNode = connectedEdgeNode;
    }

    /**
     * @return the latencyMs
     */
    public double getLatencyMs() {
        return latencyMs;
    }

    /**
     * @return the bandwidthMbps
     */
    public double getBandwidthMbps() {
        return bandwidthMbps;
    }

    /**
     * @return the reliability
     */
    public double getReliability() {
        return reliability;
    }

    /**
     * @return the networkEnergyConsumption
     */
    public double getNetworkEnergyConsumption() {
        return networkEnergyConsumption;
    }

    /**
     * @return the tasksGenerated
     */
    public int getTasksGenerated() {
        return tasksGenerated;
    }

    /**
     * @return the tasksOffloaded
     */
    public int getTasksOffloaded() {
        return tasksOffloaded;
    }
    
    /**
     * Increment the counter for tasks offloaded
     */
    public void incrementTasksOffloaded() {
        tasksOffloaded++;
    }

    /**
     * @return the tasksProcessedLocally
     */
    public int getTasksProcessedLocally() {
        return tasksProcessedLocally;
    }
    
    /**
     * @return the tasksExecutedLocally (alias for tasksProcessedLocally)
     */
    public int getTasksExecutedLocally() {
        return tasksProcessedLocally;
    }

    /**
     * @return the energyConsumed
     */
    public double getEnergyConsumed() {
        return energyConsumed;
    }

    /**
     * @return the vm
     */
    public Vm getVm() {
        return vm;
    }

    /**
     * @param vm the vm to set
     */
    public void setVm(Vm vm) {
        this.vm = vm;
    }
    
    /**
     * @return battery level as a percentage
     */
    public double getBatteryLevel() {
        return currentBattery / batteryCapacity;
    }
    
    @Override
    public String toString() {
        return "IoTDevice [id=" + deviceId + ", type=" + deviceType + 
               ", battery=" + String.format("%.2f%%", getBatteryLevel() * 100) +
               ", location=(" + String.format("%.1f", locationX) + "," + 
               String.format("%.1f", locationY) + "), wireless=" + wirelessTech + "]";
    }
}
