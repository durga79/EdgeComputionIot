package org.edgecomputing.policies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.edgecomputing.models.EdgeNode;
import org.edgecomputing.models.Task;

/**
 * Implements the service slicing mechanism described in the paper
 * "IoT Service Slicing and Task Offloading for Edge Computing"
 * 
 * Service slicing partitions edge computing resources based on
 * task requirements and service priorities.
 */
public class ServiceSlicingPolicy {
    
    // Map of slice name -> configuration
    private final Map<String, SliceConfiguration> sliceConfigurations;
    
    /**
     * Creates a new Service Slicing Policy.
     */
    public ServiceSlicingPolicy() {
        this.sliceConfigurations = new HashMap<>();
    }
    
    /**
     * Adds a service slice configuration.
     * 
     * @param name Slice name
     * @param resourcePercentage Percentage of resources to allocate to this slice
     * @param priority Priority of this slice (lower number = higher priority)
     * @param taskTypes Types of tasks that can be processed by this slice
     */
    public void addSliceConfiguration(String name, double resourcePercentage, 
                                    int priority, List<String> taskTypes) {
        SliceConfiguration config = new SliceConfiguration(
                name, resourcePercentage, priority, taskTypes);
        sliceConfigurations.put(name, config);
    }
    
    /**
     * Apply service slicing to an edge node.
     * 
     * @param edgeNode The edge node to configure
     */
    public void applySlicing(EdgeNode edgeNode) {
        System.out.println("[Slicing] Applying service slicing to Edge Node " + edgeNode.getNodeId());
        
        // Apply each slice configuration
        for (SliceConfiguration config : sliceConfigurations.values()) {
            edgeNode.addServiceSlice(config.getName(), 
                                   config.getResourcePercentage(), 
                                   config.getPriority(), 
                                   config.getTaskTypes());
            
            System.out.println("[Slicing] Created slice '" + config.getName() + 
                             "' with " + (config.getResourcePercentage() * 100) + 
                             "% resources and priority " + config.getPriority());
        }
    }
    
    /**
     * Determine the appropriate service slice for a task.
     * 
     * @param task The task to find a slice for
     * @param edgeNode The edge node to find a slice in
     * @return The name of the appropriate slice, or null if none found
     */
    public String getSliceForTask(Task task, EdgeNode edgeNode) {
        // First, find slice configurations that support this task type
        String taskType = task.getType();
        int bestPriority = Integer.MAX_VALUE;
        String bestSlice = null;
        
        for (SliceConfiguration config : sliceConfigurations.values()) {
            if (config.getTaskTypes().contains(taskType)) {
                // Find the highest priority (lowest number) matching slice
                if (config.getPriority() < bestPriority) {
                    bestPriority = config.getPriority();
                    bestSlice = config.getName();
                }
            }
        }
        
        // Now check if this slice has capacity in the edge node
        if (bestSlice != null) {
            EdgeNode.ServiceSlice slice = edgeNode.getServiceSlices().get(bestSlice);
            if (slice != null && slice.hasCapacity()) {
                return bestSlice;
            }
        }
        
        // If no matching slice or no capacity, find any slice with capacity
        for (Map.Entry<String, EdgeNode.ServiceSlice> entry : 
                edgeNode.getServiceSlices().entrySet()) {
            if (entry.getValue().hasCapacity()) {
                return entry.getKey();
            }
        }
        
        return null;  // No suitable slice found
    }
    
    /**
     * Inner class representing a slice configuration.
     */
    private static class SliceConfiguration {
        private final String name;
        private final double resourcePercentage;
        private final int priority;
        private final List<String> taskTypes;
        
        public SliceConfiguration(String name, double resourcePercentage, 
                               int priority, List<String> taskTypes) {
            this.name = name;
            this.resourcePercentage = resourcePercentage;
            this.priority = priority;
            this.taskTypes = taskTypes;
        }
        
        public String getName() {
            return name;
        }
        
        public double getResourcePercentage() {
            return resourcePercentage;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public List<String> getTaskTypes() {
            return taskTypes;
        }
    }
}
