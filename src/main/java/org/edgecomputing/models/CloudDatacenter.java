package org.edgecomputing.models;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.datacenters.Datacenter;

/**
 * Represents a Cloud Datacenter that can process offloaded tasks from Edge nodes.
 * This class models the cloud computing layer of the architecture, with high
 * computing power but higher latency compared to edge nodes.
 */
public class CloudDatacenter {
    private final int datacenterId;
    private final String name;
    private final int mips;
    private final int ram;
    private final long storage;
    private final long bandwidth;
    private final double costPerMips;
    
    // Network characteristics to edge nodes
    private final double latencyToEdgeMs;
    
    // Tasks currently being processed
    private final List<Task> activeTasks;
    private int totalTasksProcessed;
    
    // Current resource utilization
    private double cpuUtilization;
    private double ramUtilization;
    private double bwUtilization;
    
    // Associated datacenter in CloudSim
    private Datacenter datacenter;

    /**
     * Creates a new Cloud Datacenter with specified parameters.
     *
     * @param datacenterId Unique identifier for this datacenter
     * @param name Name of this datacenter
     * @param mips Processing capability in MIPS
     * @param ram Memory in MB
     * @param storage Storage in MB
     * @param bandwidth Bandwidth in Mbps
     * @param costPerMips Cost per MIPS used
     * @param latencyToEdgeMs Average latency to edge nodes in ms
     */
    public CloudDatacenter(int datacenterId, String name, int mips, int ram, 
                         long storage, long bandwidth, double costPerMips,
                         double latencyToEdgeMs) {
        this.datacenterId = datacenterId;
        this.name = name;
        this.mips = mips;
        this.ram = ram;
        this.storage = storage;
        this.bandwidth = bandwidth;
        this.costPerMips = costPerMips;
        this.latencyToEdgeMs = latencyToEdgeMs;
        
        this.activeTasks = new ArrayList<>();
        this.totalTasksProcessed = 0;
        
        this.cpuUtilization = 0.0;
        this.ramUtilization = 0.0;
        this.bwUtilization = 0.0;
    }
    
    /**
     * Process a task on this cloud datacenter.
     *
     * @param task The task to process
     * @param currentTime Current simulation time
     * @return Time when task will be completed
     */
    public double processTask(Task task, double currentTime) {
        // Calculate transfer time from edge to cloud
        double transferTime = task.getInputSize() / (bandwidth * 125000.0);  // Convert Mbps to bytes/sec
        
        // Calculate execution time based on cloud MIPS
        double executionTime = task.getMipsRequired() / (double)mips;
        
        // Total time including latency, transfer, and execution
        double totalTime = (latencyToEdgeMs / 1000.0) + transferTime + executionTime;
        
        // Update task status
        task.setStartTime(currentTime);
        task.setCompletionTime(currentTime + totalTime);
        task.setExecutedOn("cloud_" + datacenterId);
        task.setStatus("processing");
        
        // Update cloud statistics
        activeTasks.add(task);
        updateUtilization();
        
        System.out.println("Cloud Datacenter " + datacenterId + " processing task " + 
                          task.getTaskId() + ", estimated completion at " + 
                          task.getCompletionTime());
                          
        return task.getCompletionTime();
    }
    
    /**
     * Update tasks being processed on this cloud datacenter.
     * 
     * @param currentTime Current simulation time
     * @return List of tasks that have completed
     */
    public List<Task> updateTasks(double currentTime) {
        List<Task> completedTasks = new ArrayList<>();
        List<Task> remainingTasks = new ArrayList<>();
        
        for (Task task : activeTasks) {
            if (currentTime >= task.getCompletionTime()) {
                task.setStatus("completed");
                completedTasks.add(task);
                totalTasksProcessed++;
            } else {
                remainingTasks.add(task);
            }
        }
        
        // Update the list of active tasks
        activeTasks.clear();
        activeTasks.addAll(remainingTasks);
        updateUtilization();
        
        return completedTasks;
    }
    
    /**
     * Update the utilization metrics for this cloud datacenter.
     */
    private void updateUtilization() {
        // Calculate CPU utilization
        double totalMipsUsed = 0;
        for (Task task : activeTasks) {
            totalMipsUsed += task.getMipsRequired();
        }
        cpuUtilization = Math.min(1.0, totalMipsUsed / mips);
        
        // Calculate RAM utilization (simplified)
        ramUtilization = Math.min(1.0, activeTasks.size() * 1024.0 / ram);
        
        // Calculate bandwidth utilization (simplified)
        double bwUsed = 0;
        for (Task task : activeTasks) {
            bwUsed += task.getInputSize() / 10.0;  // Assume 1/10th of task size is using bandwidth
        }
        bwUtilization = Math.min(1.0, bwUsed / bandwidth);
    }
    
    /**
     * @return the datacenterId
     */
    public int getDatacenterId() {
        return datacenterId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
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
     * @return the storage
     */
    public long getStorage() {
        return storage;
    }

    /**
     * @return the bandwidth
     */
    public long getBandwidth() {
        return bandwidth;
    }

    /**
     * @return the costPerMips
     */
    public double getCostPerMips() {
        return costPerMips;
    }

    /**
     * @return the latencyToEdgeMs
     */
    public double getLatencyToEdgeMs() {
        return latencyToEdgeMs;
    }

    /**
     * @return the active tasks
     */
    public List<Task> getActiveTasks() {
        return activeTasks;
    }

    /**
     * @return the totalTasksProcessed
     */
    public int getTotalTasksProcessed() {
        return totalTasksProcessed;
    }

    /**
     * @return the datacenter
     */
    public Datacenter getDatacenter() {
        return datacenter;
    }

    /**
     * @param datacenter the datacenter to set
     */
    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }
    
    /**
     * @return the overall utilization of this cloud datacenter (0-1)
     */
    public double getUtilization() {
        return (cpuUtilization * 0.5) + (ramUtilization * 0.3) + (bwUtilization * 0.2);
    }
    
    /**
     * @return the CPU utilization (0-1)
     */
    public double getCpuUtilization() {
        return cpuUtilization;
    }
    
    /**
     * @return the RAM utilization (0-1)
     */
    public double getRamUtilization() {
        return ramUtilization;
    }
    
    /**
     * @return the bandwidth utilization (0-1)
     */
    public double getBwUtilization() {
        return bwUtilization;
    }
    
    @Override
    public String toString() {
        return "CloudDatacenter [id=" + datacenterId + ", name=" + name + 
               ", util=" + String.format("%.2f%%", getUtilization() * 100) + "]";
    }
}
