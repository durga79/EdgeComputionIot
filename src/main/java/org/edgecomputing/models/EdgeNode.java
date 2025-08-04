package org.edgecomputing.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;

/**
 * Represents an Edge Node that can process offloaded tasks from IoT devices.
 * This class models the edge computing layer of the architecture, with varying
 * resource capabilities and service slicing.
 */
public class EdgeNode {
    private final int nodeId;
    private final String nodeType;
    private final int mips;
    private final int ram;
    private final long storage;
    private final long bandwidth;
    private final double costPerMips;
    
    // Location coordinates (x, y)
    private final double locationX;
    private final double locationY;
    
    // Connected IoT devices
    private final List<Integer> connectedDevices;
    
    // Service slices: name -> resource allocation
    private final Map<String, ServiceSlice> serviceSlices;
    
    // Tasks currently being processed
    private final List<Task> activeTasks;
    private int totalTasksProcessed;
    
    // Current resource utilization
    private double cpuUtilization;
    private double ramUtilization;
    private double bwUtilization;
    
    // Associated host in CloudSim
    private Host host;

    /**
     * Creates a new Edge Node with specified parameters.
     *
     * @param nodeId Unique identifier for this node
     * @param nodeType Type of edge node (small, medium, large)
     * @param mips Processing capability in MIPS
     * @param ram Memory in MB
     * @param storage Storage in MB
     * @param bandwidth Bandwidth in Mbps
     * @param costPerMips Cost per MIPS used
     * @param locationX X-coordinate of this node
     * @param locationY Y-coordinate of this node
     */
    public EdgeNode(int nodeId, String nodeType, int mips, int ram, long storage,
                    long bandwidth, double costPerMips, double locationX, double locationY) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.mips = mips;
        this.ram = ram;
        this.storage = storage;
        this.bandwidth = bandwidth;
        this.costPerMips = costPerMips;
        this.locationX = locationX;
        this.locationY = locationY;
        
        this.connectedDevices = new ArrayList<>();
        this.serviceSlices = new HashMap<>();
        this.activeTasks = new ArrayList<>();
        this.totalTasksProcessed = 0;
        
        this.cpuUtilization = 0.0;
        this.ramUtilization = 0.0;
        this.bwUtilization = 0.0;
    }
    
    /**
     * Add a service slice to this edge node.
     *
     * @param name Name of the slice (e.g., "critical", "standard", "best_effort")
     * @param resourcePercentage Percentage of resources allocated to this slice (0-1)
     * @param priority Priority of this slice (lower number = higher priority)
     * @param taskTypes List of task types supported by this slice
     */
    public void addServiceSlice(String name, double resourcePercentage, int priority, List<String> taskTypes) {
        int sliceMips = (int)(mips * resourcePercentage);
        int sliceRam = (int)(ram * resourcePercentage);
        long sliceBw = (long)(bandwidth * resourcePercentage);
        
        ServiceSlice slice = new ServiceSlice(name, sliceMips, sliceRam, sliceBw, priority, taskTypes);
        serviceSlices.put(name, slice);
    }
    
    /**
     * Connect an IoT device to this edge node.
     *
     * @param deviceId ID of the device to connect
     * @return true if connection was successful, false otherwise
     */
    public boolean connectDevice(int deviceId) {
        if (!connectedDevices.contains(deviceId)) {
            connectedDevices.add(deviceId);
            return true;
        }
        return false;
    }
    
    /**
     * Disconnect an IoT device from this edge node.
     *
     * @param deviceId ID of the device to disconnect
     * @return true if disconnection was successful, false otherwise
     */
    public boolean disconnectDevice(int deviceId) {
        return connectedDevices.remove(Integer.valueOf(deviceId));
    }
    
    /**
     * Get the appropriate service slice for a given task.
     *
     * @param task Task to find a slice for
     * @return The most appropriate service slice, or null if none matches
     */
    public ServiceSlice getSliceForTask(Task task) {
        ServiceSlice bestSlice = null;
        int bestPriority = Integer.MAX_VALUE;
        
        for (ServiceSlice slice : serviceSlices.values()) {
            if (slice.supportsTaskType(task.getType()) && slice.hasCapacity()) {
                if (slice.getPriority() < bestPriority) {
                    bestSlice = slice;
                    bestPriority = slice.getPriority();
                }
            }
        }
        
        return bestSlice;
    }
    
    /**
     * Process a task on this edge node.
     *
     * @param task The task to process
     * @param currentTime Current simulation time
     * @return Time when task will be completed, or -1 if can't process
     */
    public double processTask(Task task, double currentTime) {
        // Find appropriate service slice
        ServiceSlice slice = getSliceForTask(task);
        if (slice == null) {
            System.out.println("Edge Node " + nodeId + " cannot process task " + 
                              task.getTaskId() + " - no suitable service slice");
            return -1;
        }
        
        // Calculate execution time based on slice MIPS
        double executionTime = task.getMipsRequired() / (double)slice.getMips();
        
        // Update task status
        task.setStartTime(currentTime);
        task.setCompletionTime(currentTime + executionTime);
        task.setExecutedOn("edge_" + nodeId + "_slice_" + slice.getName());
        task.setStatus("processing");
        
        // Update edge node statistics
        activeTasks.add(task);
        slice.addTask(task);
        updateUtilization();
        
        System.out.println("Edge Node " + nodeId + " processing task " + task.getTaskId() + 
                          " on slice " + slice.getName() + ", estimated completion at " + 
                          task.getCompletionTime());
                          
        return task.getCompletionTime();
    }
    
    /**
     * Update tasks being processed on this edge node.
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
                
                // Find and update the slice that was processing this task
                String sliceName = task.getExecutedOn().split("_slice_")[1];
                ServiceSlice slice = serviceSlices.get(sliceName);
                if (slice != null) {
                    slice.removeTask(task);
                }
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
     * Update the utilization metrics for this edge node.
     */
    private void updateUtilization() {
        // Calculate CPU utilization
        double totalMipsUsed = 0;
        for (Task task : activeTasks) {
            totalMipsUsed += task.getMipsRequired();
        }
        cpuUtilization = Math.min(1.0, totalMipsUsed / mips);
        
        // Calculate RAM utilization (simplified)
        ramUtilization = Math.min(1.0, activeTasks.size() * 512.0 / ram);
        
        // Calculate bandwidth utilization (simplified)
        double bwUsed = 0;
        for (Task task : activeTasks) {
            bwUsed += task.getInputSize() / 10.0;  // Assume 1/10th of task size is using bandwidth
        }
        bwUtilization = Math.min(1.0, bwUsed / bandwidth);
    }
    
    /**
     * @return the nodeId
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
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
     * @return the connected devices
     */
    public List<Integer> getConnectedDevices() {
        return connectedDevices;
    }

    /**
     * @return the service slices
     */
    public Map<String, ServiceSlice> getServiceSlices() {
        return serviceSlices;
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
     * @return the host
     */
    public Host getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(Host host) {
        this.host = host;
    }
    
    /**
     * @return the overall utilization of this edge node (0-1)
     */
    public double getUtilization() {
        return (cpuUtilization * 0.6) + (ramUtilization * 0.3) + (bwUtilization * 0.1);
    }
    
    /**
     * Get the number of active tasks currently being processed
     * 
     * @return Count of active tasks
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
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
        return "EdgeNode [id=" + nodeId + ", type=" + nodeType + 
               ", util=" + String.format("%.2f%%", getUtilization() * 100) +
               ", location=(" + String.format("%.1f", locationX) + "," + 
               String.format("%.1f", locationY) + ")]";
    }
    
    /**
     * Inner class to represent a service slice in the edge node.
     */
    public class ServiceSlice {
        private final String name;
        private final int mips;
        private final int ram;
        private final long bandwidth;
        private final int priority;
        private final List<String> supportedTaskTypes;
        private final List<Task> assignedTasks;
        
        public ServiceSlice(String name, int mips, int ram, long bandwidth, 
                          int priority, List<String> supportedTaskTypes) {
            this.name = name;
            this.mips = mips;
            this.ram = ram;
            this.bandwidth = bandwidth;
            this.priority = priority;
            this.supportedTaskTypes = supportedTaskTypes;
            this.assignedTasks = new ArrayList<>();
        }
        
        public boolean supportsTaskType(String taskType) {
            return supportedTaskTypes.contains(taskType);
        }
        
        public boolean hasCapacity() {
            double mipsUsed = 0;
            for (Task task : assignedTasks) {
                mipsUsed += task.getMipsRequired();
            }
            return mipsUsed < mips * 0.9;  // Keep 10% headroom
        }
        
        public void addTask(Task task) {
            assignedTasks.add(task);
        }
        
        public void removeTask(Task task) {
            assignedTasks.remove(task);
        }
        
        public String getName() {
            return name;
        }
        
        public int getMips() {
            return mips;
        }
        
        public int getRam() {
            return ram;
        }
        
        public long getBandwidth() {
            return bandwidth;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public List<String> getSupportedTaskTypes() {
            return supportedTaskTypes;
        }
        
        public List<Task> getAssignedTasks() {
            return assignedTasks;
        }
        
        public double getUtilization() {
            double mipsUsed = 0;
            for (Task task : assignedTasks) {
                mipsUsed += task.getMipsRequired();
            }
            return mipsUsed / mips;
        }
    }
}
