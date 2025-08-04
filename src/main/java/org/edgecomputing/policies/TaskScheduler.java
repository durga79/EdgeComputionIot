package org.edgecomputing.policies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.edgecomputing.models.CloudDatacenter;
import org.edgecomputing.models.EdgeNode;
import org.edgecomputing.models.IoTDevice;
import org.edgecomputing.models.Task;

/**
 * TaskScheduler manages task distribution across IoT devices, edge nodes, and cloud datacenter.
 * It implements the task offloading strategy from the paper
 * "IoT Service Slicing and Task Offloading for Edge Computing"
 */
public class TaskScheduler {
    
    private final OffloadingPolicy offloadingPolicy;
    private final ServiceSlicingPolicy slicingPolicy;
    
    // Tasks waiting to be executed
    private final List<Task> pendingTasks;
    
    // Tasks currently being executed
    private final Map<String, Task> runningTasks;
    
    // Tasks that have completed execution
    private final List<Task> completedTasks;
    
    /**
     * Creates a new Task Scheduler.
     *
     * @param offloadingPolicy The offloading policy to use
     * @param slicingPolicy The service slicing policy to use
     */
    public TaskScheduler(OffloadingPolicy offloadingPolicy, ServiceSlicingPolicy slicingPolicy) {
        this.offloadingPolicy = offloadingPolicy;
        this.slicingPolicy = slicingPolicy;
        this.pendingTasks = new ArrayList<>();
        this.runningTasks = new HashMap<>();
        this.completedTasks = new ArrayList<>();
    }
    
    /**
     * Submit a new task for scheduling.
     *
     * @param task The task to schedule
     */
    public void submitTask(Task task) {
        pendingTasks.add(task);
    }
    
    /**
     * Process pending tasks and make offloading decisions.
     *
     * @param devices IoT devices in the system
     * @param edgeNodes Edge nodes in the system
     * @param cloud Cloud datacenter (if available)
     * @param currentTime Current simulation time
     */
    public void schedulePendingTasks(List<IoTDevice> devices, List<EdgeNode> edgeNodes,
                                  CloudDatacenter cloud, double currentTime) {
        // Create a map of deviceId -> device for quick lookups
        Map<Integer, IoTDevice> deviceMap = new HashMap<>();
        for (IoTDevice device : devices) {
            deviceMap.put(device.getDeviceId(), device);
        }
        
        List<Task> remainingTasks = new ArrayList<>();
        
        // Process each pending task
        for (Task task : pendingTasks) {
            IoTDevice sourceDevice = deviceMap.get(task.getSourceDeviceId());
            if (sourceDevice == null) {
                System.err.println("Error: Device " + task.getSourceDeviceId() + 
                                 " not found for task " + task.getTaskId());
                continue;
            }
            
            // Make offloading decision
            boolean shouldOffload = offloadingPolicy.shouldOffload(task, sourceDevice, edgeNodes);
            
            if (shouldOffload) {
                // Select edge node
                EdgeNode targetNode = offloadingPolicy.selectEdgeNode(task, sourceDevice, edgeNodes);
                
                if (targetNode != null) {
                    // Process on edge
                    double completionTime = targetNode.processTask(task, currentTime);
                    if (completionTime > 0) {
                        runningTasks.put(task.getTaskId(), task);
                        sourceDevice.incrementTasksOffloaded();
                        System.out.println("Task " + task.getTaskId() + " offloaded to Edge Node " + 
                                         targetNode.getNodeId());
                    } else {
                        // Edge node couldn't process; try cloud if available
                        if (cloud != null) {
                            double cloudCompletionTime = cloud.processTask(task, currentTime);
                            if (cloudCompletionTime > 0) {
                                runningTasks.put(task.getTaskId(), task);
                                sourceDevice.incrementTasksOffloaded();
                                System.out.println("Task " + task.getTaskId() + 
                                                " offloaded to Cloud Datacenter");
                            } else {
                                // Cloud couldn't process; defer
                                remainingTasks.add(task);
                                System.out.println("Task " + task.getTaskId() + 
                                                " deferred - no resources available");
                            }
                        } else {
                            // No cloud available; try local execution
                            double localCompletionTime = sourceDevice.processTaskLocally(task, currentTime);
                            runningTasks.put(task.getTaskId(), task);
                            System.out.println("Task " + task.getTaskId() + 
                                            " executed locally (fallback) on Device " + 
                                            sourceDevice.getDeviceId());
                        }
                    }
                } else if (cloud != null) {
                    // No suitable edge node; try cloud
                    double cloudCompletionTime = cloud.processTask(task, currentTime);
                    if (cloudCompletionTime > 0) {
                        runningTasks.put(task.getTaskId(), task);
                        sourceDevice.incrementTasksOffloaded();
                        System.out.println("Task " + task.getTaskId() + 
                                        " offloaded to Cloud Datacenter (no edge node)");
                    } else {
                        // Cloud couldn't process; try local execution
                        double localCompletionTime = sourceDevice.processTaskLocally(task, currentTime);
                        runningTasks.put(task.getTaskId(), task);
                        System.out.println("Task " + task.getTaskId() + 
                                        " executed locally (fallback) on Device " + 
                                        sourceDevice.getDeviceId());
                    }
                } else {
                    // No edge or cloud available; process locally
                    double localCompletionTime = sourceDevice.processTaskLocally(task, currentTime);
                    runningTasks.put(task.getTaskId(), task);
                    System.out.println("Task " + task.getTaskId() + 
                                     " executed locally (no alternatives) on Device " + 
                                     sourceDevice.getDeviceId());
                }
            } else {
                // Process locally on device
                double localCompletionTime = sourceDevice.processTaskLocally(task, currentTime);
                runningTasks.put(task.getTaskId(), task);
                System.out.println("Task " + task.getTaskId() + 
                                 " executed locally (by policy) on Device " + 
                                 sourceDevice.getDeviceId());
            }
        }
        
        // Update pending tasks
        pendingTasks.clear();
        pendingTasks.addAll(remainingTasks);
    }
    
    /**
     * Update status of running tasks.
     *
     * @param devices IoT devices in the system
     * @param edgeNodes Edge nodes in the system
     * @param cloud Cloud datacenter (if available)
     * @param currentTime Current simulation time
     */
    public void updateTaskStatus(List<IoTDevice> devices, List<EdgeNode> edgeNodes,
                               CloudDatacenter cloud, double currentTime) {
        // Check for completed tasks on devices
        for (IoTDevice device : devices) {
            List<Task> deviceCompletedTasks = device.updateLocalTasks(currentTime);
            for (Task task : deviceCompletedTasks) {
                System.out.println("Task " + task.getTaskId() + 
                                 " completed on Device " + device.getDeviceId() + 
                                 " at time " + currentTime);
                completedTasks.add(task);
                runningTasks.remove(task.getTaskId());
            }
        }
        
        // Check for completed tasks on edge nodes
        for (EdgeNode node : edgeNodes) {
            List<Task> edgeCompletedTasks = node.updateTasks(currentTime);
            for (Task task : edgeCompletedTasks) {
                System.out.println("Task " + task.getTaskId() + 
                                 " completed on Edge Node " + node.getNodeId() + 
                                 " at time " + currentTime);
                completedTasks.add(task);
                runningTasks.remove(task.getTaskId());
            }
        }
        
        // Check for completed tasks on cloud
        if (cloud != null) {
            List<Task> cloudCompletedTasks = cloud.updateTasks(currentTime);
            for (Task task : cloudCompletedTasks) {
                System.out.println("Task " + task.getTaskId() + 
                                 " completed on Cloud Datacenter at time " + currentTime);
                completedTasks.add(task);
                runningTasks.remove(task.getTaskId());
            }
        }
    }
    
    /**
     * @return the number of pending tasks
     */
    public int getPendingTaskCount() {
        return pendingTasks.size();
    }
    
    /**
     * @return the number of running tasks
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
    
    /**
     * @return the number of completed tasks
     */
    public int getCompletedTaskCount() {
        return completedTasks.size();
    }
    
    /**
     * @return the completed tasks
     */
    public List<Task> getCompletedTasks() {
        return completedTasks;
    }
    
    /**
     * Get statistics about completed tasks.
     *
     * @return Map of metrics to their values
     */
    public Map<String, Double> getTaskStatistics() {
        Map<String, Double> stats = new HashMap<>();
        
        if (completedTasks.isEmpty()) {
            return stats;
        }
        
        // Task completion statistics
        double totalResponseTime = 0;
        double totalExecutionTime = 0;
        double totalEnergyConsumed = 0;
        int localTasks = 0;
        int edgeTasks = 0;
        int cloudTasks = 0;
        int tasksMetDeadline = 0;
        
        for (Task task : completedTasks) {
            totalResponseTime += task.getResponseTime();
            totalExecutionTime += task.getExecutionTime();
            totalEnergyConsumed += task.getEnergyConsumed();
            
            if (task.getExecutedOn().startsWith("device_")) {
                localTasks++;
            } else if (task.getExecutedOn().startsWith("edge_")) {
                edgeTasks++;
            } else if (task.getExecutedOn().startsWith("cloud_")) {
                cloudTasks++;
            }
            
            if (task.metDeadline()) {
                tasksMetDeadline++;
            }
        }
        
        // Calculate averages
        double avgResponseTime = totalResponseTime / completedTasks.size();
        double avgExecutionTime = totalExecutionTime / completedTasks.size();
        double avgEnergyConsumed = totalEnergyConsumed / completedTasks.size();
        double deadlineMetPercentage = (double)tasksMetDeadline / completedTasks.size() * 100;
        
        // Store statistics
        stats.put("averageResponseTime", avgResponseTime);
        stats.put("averageExecutionTime", avgExecutionTime);
        stats.put("averageEnergyConsumed", avgEnergyConsumed);
        stats.put("localTasks", (double)localTasks);
        stats.put("edgeTasks", (double)edgeTasks);
        stats.put("cloudTasks", (double)cloudTasks);
        stats.put("totalTasks", (double)completedTasks.size());
        stats.put("deadlineMetPercentage", deadlineMetPercentage);
        
        return stats;
    }
}
