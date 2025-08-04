package org.edgecomputing.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records and manages simulation results and metrics.
 * Provides methods to record different types of metrics during simulation
 * and export them to CSV files for analysis.
 */
public class SimulationResults {
    
    private static final Logger logger = LoggerFactory.getLogger(SimulationResults.class);
    
    // Device metrics: time -> deviceId -> metrics
    private final Map<Double, Map<Integer, DeviceMetrics>> deviceMetrics;
    
    // Edge node metrics: time -> nodeId -> metrics
    private final Map<Double, Map<Integer, EdgeNodeMetrics>> edgeNodeMetrics;
    
    // Cloud metrics: time -> metrics
    private final Map<Double, CloudMetrics> cloudMetrics;
    
    // Task metrics: time -> metrics
    private final Map<Double, TaskMetrics> taskMetrics;
    
    /**
     * Creates a new SimulationResults instance.
     */
    public SimulationResults() {
        this.deviceMetrics = new HashMap<>();
        this.edgeNodeMetrics = new HashMap<>();
        this.cloudMetrics = new HashMap<>();
        this.taskMetrics = new HashMap<>();
    }
    
    /**
     * Record metrics for an IoT device at a specific time.
     * 
     * @param time Simulation time
     * @param deviceId Device ID
     * @param batteryLevel Battery level (0-1)
     * @param tasksExecutedLocally Number of tasks executed locally
     * @param tasksOffloaded Number of tasks offloaded
     */
    public void recordDeviceState(double time, int deviceId, double batteryLevel,
                                 int tasksExecutedLocally, int tasksOffloaded) {
        Map<Integer, DeviceMetrics> timeMetrics = deviceMetrics.computeIfAbsent(time, k -> new HashMap<>());
        DeviceMetrics metrics = new DeviceMetrics(batteryLevel, tasksExecutedLocally, tasksOffloaded);
        timeMetrics.put(deviceId, metrics);
    }
    
    /**
     * Record metrics for an edge node at a specific time.
     * 
     * @param time Simulation time
     * @param nodeId Edge node ID
     * @param cpuUtilization CPU utilization (0-1)
     * @param ramUtilization RAM utilization (0-1)
     * @param bwUtilization Bandwidth utilization (0-1)
     * @param activeTaskCount Number of tasks currently being processed
     * @param totalTasksProcessed Total number of tasks processed so far
     */
    public void recordEdgeNodeState(double time, int nodeId, double cpuUtilization,
                                  double ramUtilization, double bwUtilization,
                                  int activeTaskCount, int totalTasksProcessed) {
        Map<Integer, EdgeNodeMetrics> timeMetrics = edgeNodeMetrics.computeIfAbsent(time, k -> new HashMap<>());
        EdgeNodeMetrics metrics = new EdgeNodeMetrics(cpuUtilization, ramUtilization,
                                                    bwUtilization, activeTaskCount, totalTasksProcessed);
        timeMetrics.put(nodeId, metrics);
    }
    
    /**
     * Record metrics for the cloud datacenter at a specific time.
     * 
     * @param time Simulation time
     * @param cpuUtilization CPU utilization (0-1)
     * @param ramUtilization RAM utilization (0-1)
     * @param bwUtilization Bandwidth utilization (0-1)
     * @param activeTaskCount Number of tasks currently being processed
     * @param totalTasksProcessed Total number of tasks processed so far
     */
    public void recordCloudState(double time, double cpuUtilization, double ramUtilization,
                               double bwUtilization, int activeTaskCount, int totalTasksProcessed) {
        CloudMetrics metrics = new CloudMetrics(cpuUtilization, ramUtilization,
                                               bwUtilization, activeTaskCount, totalTasksProcessed);
        cloudMetrics.put(time, metrics);
    }
    
    /**
     * Record task metrics at a specific time.
     * 
     * @param time Simulation time
     * @param avgResponseTime Average response time
     * @param avgExecutionTime Average execution time
     * @param avgEnergyConsumed Average energy consumed
     * @param localTasks Number of tasks executed locally
     * @param edgeTasks Number of tasks executed on edge nodes
     * @param cloudTasks Number of tasks executed in the cloud
     * @param deadlineMetPercentage Percentage of tasks that met their deadline
     */
    public void recordTaskMetrics(double time, double avgResponseTime, double avgExecutionTime,
                                double avgEnergyConsumed, int localTasks, int edgeTasks,
                                int cloudTasks, double deadlineMetPercentage) {
        TaskMetrics metrics = new TaskMetrics(avgResponseTime, avgExecutionTime, avgEnergyConsumed,
                                             localTasks, edgeTasks, cloudTasks, deadlineMetPercentage);
        taskMetrics.put(time, metrics);
    }
    
    /**
     * Export device metrics to a CSV file.
     * 
     * @param filePath Path to the output file
     * @throws IOException if there's an error writing the file
     */
    public void exportDeviceMetrics(String filePath) throws IOException {
        logger.info("Exporting device metrics to {}", filePath);
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("Time,DeviceId,BatteryLevel,TasksExecutedLocally,TasksOffloaded\n");
            
            // Write data
            List<Double> times = new ArrayList<>(deviceMetrics.keySet());
            times.sort(Double::compare);
            
            for (Double time : times) {
                Map<Integer, DeviceMetrics> timeMetrics = deviceMetrics.get(time);
                for (Map.Entry<Integer, DeviceMetrics> entry : timeMetrics.entrySet()) {
                    int deviceId = entry.getKey();
                    DeviceMetrics metrics = entry.getValue();
                    
                    writer.write(String.format("%.2f,%d,%.4f,%d,%d\n",
                                             time, deviceId, metrics.batteryLevel,
                                             metrics.tasksExecutedLocally, metrics.tasksOffloaded));
                }
            }
        }
    }
    
    /**
     * Export edge node metrics to a CSV file.
     * 
     * @param filePath Path to the output file
     * @throws IOException if there's an error writing the file
     */
    public void exportEdgeNodeMetrics(String filePath) throws IOException {
        logger.info("Exporting edge node metrics to {}", filePath);
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("Time,NodeId,CpuUtilization,RamUtilization,BwUtilization,ActiveTaskCount,TotalTasksProcessed\n");
            
            // Write data
            List<Double> times = new ArrayList<>(edgeNodeMetrics.keySet());
            times.sort(Double::compare);
            
            for (Double time : times) {
                Map<Integer, EdgeNodeMetrics> timeMetrics = edgeNodeMetrics.get(time);
                for (Map.Entry<Integer, EdgeNodeMetrics> entry : timeMetrics.entrySet()) {
                    int nodeId = entry.getKey();
                    EdgeNodeMetrics metrics = entry.getValue();
                    
                    writer.write(String.format("%.2f,%d,%.4f,%.4f,%.4f,%d,%d\n",
                                             time, nodeId, metrics.cpuUtilization, metrics.ramUtilization,
                                             metrics.bwUtilization, metrics.activeTaskCount, 
                                             metrics.totalTasksProcessed));
                }
            }
        }
    }
    
    /**
     * Export cloud metrics to a CSV file.
     * 
     * @param filePath Path to the output file
     * @throws IOException if there's an error writing the file
     */
    public void exportCloudMetrics(String filePath) throws IOException {
        logger.info("Exporting cloud metrics to {}", filePath);
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("Time,CpuUtilization,RamUtilization,BwUtilization,ActiveTaskCount,TotalTasksProcessed\n");
            
            // Write data
            List<Double> times = new ArrayList<>(cloudMetrics.keySet());
            times.sort(Double::compare);
            
            for (Double time : times) {
                CloudMetrics metrics = cloudMetrics.get(time);
                writer.write(String.format("%.2f,%.4f,%.4f,%.4f,%d,%d\n",
                                         time, metrics.cpuUtilization, metrics.ramUtilization,
                                         metrics.bwUtilization, metrics.activeTaskCount, 
                                         metrics.totalTasksProcessed));
            }
        }
    }
    
    /**
     * Export task metrics to a CSV file.
     * 
     * @param filePath Path to the output file
     * @throws IOException if there's an error writing the file
     */
    public void exportTaskMetrics(String filePath) throws IOException {
        logger.info("Exporting task metrics to {}", filePath);
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("Time,AvgResponseTime,AvgExecutionTime,AvgEnergyConsumed," +
                      "LocalTasks,EdgeTasks,CloudTasks,DeadlineMetPercentage\n");
            
            // Write data
            List<Double> times = new ArrayList<>(taskMetrics.keySet());
            times.sort(Double::compare);
            
            for (Double time : times) {
                TaskMetrics metrics = taskMetrics.get(time);
                writer.write(String.format("%.2f,%.4f,%.4f,%.4f,%d,%d,%d,%.2f\n",
                                         time, metrics.avgResponseTime, metrics.avgExecutionTime,
                                         metrics.avgEnergyConsumed, metrics.localTasks, 
                                         metrics.edgeTasks, metrics.cloudTasks,
                                         metrics.deadlineMetPercentage));
            }
        }
    }
    
    /**
     * Export a summary of the simulation results.
     * 
     * @param filePath Path to the output file
     * @throws IOException if there's an error writing the file
     */
    public void exportSummary(String filePath) throws IOException {
        logger.info("Exporting simulation summary to {}", filePath);
        
        DecimalFormat df = new DecimalFormat("0.00");
        
        // Get final time metrics
        double finalTime = 0.0;
        for (Double time : taskMetrics.keySet()) {
            finalTime = Math.max(finalTime, time);
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("EDGE COMPUTING SIMULATION SUMMARY\n");
            writer.write("===============================\n\n");
            
            // Task distribution
            if (taskMetrics.containsKey(finalTime)) {
                TaskMetrics finalMetrics = taskMetrics.get(finalTime);
                int totalTasks = finalMetrics.localTasks + finalMetrics.edgeTasks + finalMetrics.cloudTasks;
                
                writer.write("TASK DISTRIBUTION\n");
                writer.write("-----------------\n");
                writer.write("Total tasks: " + totalTasks + "\n");
                writer.write("Tasks executed locally: " + finalMetrics.localTasks + 
                           " (" + df.format((double)finalMetrics.localTasks / totalTasks * 100) + "%)\n");
                writer.write("Tasks executed on edge: " + finalMetrics.edgeTasks +
                           " (" + df.format((double)finalMetrics.edgeTasks / totalTasks * 100) + "%)\n");
                writer.write("Tasks executed on cloud: " + finalMetrics.cloudTasks +
                           " (" + df.format((double)finalMetrics.cloudTasks / totalTasks * 100) + "%)\n");
                writer.write("Deadline met: " + df.format(finalMetrics.deadlineMetPercentage) + "%\n\n");
                
                writer.write("PERFORMANCE METRICS\n");
                writer.write("------------------\n");
                writer.write("Average response time: " + df.format(finalMetrics.avgResponseTime) + " seconds\n");
                writer.write("Average execution time: " + df.format(finalMetrics.avgExecutionTime) + " seconds\n");
                writer.write("Average energy consumed: " + df.format(finalMetrics.avgEnergyConsumed) + " units\n\n");
            }
            
            // IoT device statistics
            if (!deviceMetrics.isEmpty() && deviceMetrics.containsKey(finalTime)) {
                writer.write("IOT DEVICE STATISTICS\n");
                writer.write("--------------------\n");
                
                Map<Integer, DeviceMetrics> finalDeviceMetrics = deviceMetrics.get(finalTime);
                
                // Calculate average battery level
                double totalBatteryLevel = 0.0;
                for (DeviceMetrics metrics : finalDeviceMetrics.values()) {
                    totalBatteryLevel += metrics.batteryLevel;
                }
                double avgBatteryLevel = totalBatteryLevel / finalDeviceMetrics.size();
                
                writer.write("Number of IoT devices: " + finalDeviceMetrics.size() + "\n");
                writer.write("Average final battery level: " + df.format(avgBatteryLevel * 100) + "%\n\n");
            }
            
            // Edge node statistics
            if (!edgeNodeMetrics.isEmpty() && edgeNodeMetrics.containsKey(finalTime)) {
                writer.write("EDGE NODE STATISTICS\n");
                writer.write("-------------------\n");
                
                Map<Integer, EdgeNodeMetrics> finalEdgeMetrics = edgeNodeMetrics.get(finalTime);
                
                // Calculate average utilization
                double totalCpuUtil = 0.0;
                double totalRamUtil = 0.0;
                double totalBwUtil = 0.0;
                int totalProcessed = 0;
                
                for (EdgeNodeMetrics metrics : finalEdgeMetrics.values()) {
                    totalCpuUtil += metrics.cpuUtilization;
                    totalRamUtil += metrics.ramUtilization;
                    totalBwUtil += metrics.bwUtilization;
                    totalProcessed += metrics.totalTasksProcessed;
                }
                
                double avgCpuUtil = totalCpuUtil / finalEdgeMetrics.size();
                double avgRamUtil = totalRamUtil / finalEdgeMetrics.size();
                double avgBwUtil = totalBwUtil / finalEdgeMetrics.size();
                
                writer.write("Number of edge nodes: " + finalEdgeMetrics.size() + "\n");
                writer.write("Average CPU utilization: " + df.format(avgCpuUtil * 100) + "%\n");
                writer.write("Average RAM utilization: " + df.format(avgRamUtil * 100) + "%\n");
                writer.write("Average bandwidth utilization: " + df.format(avgBwUtil * 100) + "%\n");
                writer.write("Total tasks processed: " + totalProcessed + "\n\n");
            }
            
            // Cloud statistics
            if (!cloudMetrics.isEmpty() && cloudMetrics.containsKey(finalTime)) {
                writer.write("CLOUD STATISTICS\n");
                writer.write("---------------\n");
                
                CloudMetrics finalCloudMetrics = cloudMetrics.get(finalTime);
                
                writer.write("CPU utilization: " + df.format(finalCloudMetrics.cpuUtilization * 100) + "%\n");
                writer.write("RAM utilization: " + df.format(finalCloudMetrics.ramUtilization * 100) + "%\n");
                writer.write("Bandwidth utilization: " + df.format(finalCloudMetrics.bwUtilization * 100) + "%\n");
                writer.write("Total tasks processed: " + finalCloudMetrics.totalTasksProcessed + "\n");
            }
        }
    }
    
    /**
     * Inner class for IoT device metrics.
     */
    private static class DeviceMetrics {
        private final double batteryLevel;
        private final int tasksExecutedLocally;
        private final int tasksOffloaded;
        
        public DeviceMetrics(double batteryLevel, int tasksExecutedLocally, int tasksOffloaded) {
            this.batteryLevel = batteryLevel;
            this.tasksExecutedLocally = tasksExecutedLocally;
            this.tasksOffloaded = tasksOffloaded;
        }
    }
    
    /**
     * Inner class for edge node metrics.
     */
    private static class EdgeNodeMetrics {
        private final double cpuUtilization;
        private final double ramUtilization;
        private final double bwUtilization;
        private final int activeTaskCount;
        private final int totalTasksProcessed;
        
        public EdgeNodeMetrics(double cpuUtilization, double ramUtilization, double bwUtilization,
                              int activeTaskCount, int totalTasksProcessed) {
            this.cpuUtilization = cpuUtilization;
            this.ramUtilization = ramUtilization;
            this.bwUtilization = bwUtilization;
            this.activeTaskCount = activeTaskCount;
            this.totalTasksProcessed = totalTasksProcessed;
        }
    }
    
    /**
     * Inner class for cloud metrics.
     */
    private static class CloudMetrics {
        private final double cpuUtilization;
        private final double ramUtilization;
        private final double bwUtilization;
        private final int activeTaskCount;
        private final int totalTasksProcessed;
        
        public CloudMetrics(double cpuUtilization, double ramUtilization, double bwUtilization,
                           int activeTaskCount, int totalTasksProcessed) {
            this.cpuUtilization = cpuUtilization;
            this.ramUtilization = ramUtilization;
            this.bwUtilization = bwUtilization;
            this.activeTaskCount = activeTaskCount;
            this.totalTasksProcessed = totalTasksProcessed;
        }
    }
    
    /**
     * Inner class for task metrics.
     */
    private static class TaskMetrics {
        private final double avgResponseTime;
        private final double avgExecutionTime;
        private final double avgEnergyConsumed;
        private final int localTasks;
        private final int edgeTasks;
        private final int cloudTasks;
        private final double deadlineMetPercentage;
        
        public TaskMetrics(double avgResponseTime, double avgExecutionTime, double avgEnergyConsumed,
                          int localTasks, int edgeTasks, int cloudTasks,
                          double deadlineMetPercentage) {
            this.avgResponseTime = avgResponseTime;
            this.avgExecutionTime = avgExecutionTime;
            this.avgEnergyConsumed = avgEnergyConsumed;
            this.localTasks = localTasks;
            this.edgeTasks = edgeTasks;
            this.cloudTasks = cloudTasks;
            this.deadlineMetPercentage = deadlineMetPercentage;
        }
    }
}
