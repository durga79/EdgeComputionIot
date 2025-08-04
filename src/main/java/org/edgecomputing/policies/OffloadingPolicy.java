package org.edgecomputing.policies;

import java.util.List;
import org.edgecomputing.models.EdgeNode;
import org.edgecomputing.models.IoTDevice;
import org.edgecomputing.models.Task;

/**
 * Interface for task offloading strategies.
 * Different implementations can define various algorithms for deciding
 * whether tasks should be executed locally on IoT devices or offloaded to edge nodes.
 */
public interface OffloadingPolicy {
    
    /**
     * Decides where to execute a task (locally or on edge).
     *
     * @param task The task to be executed
     * @param device The IoT device that generated the task
     * @param edgeNodes List of available edge nodes
     * @return true if the task should be offloaded, false if it should be executed locally
     */
    boolean shouldOffload(Task task, IoTDevice device, List<EdgeNode> edgeNodes);
    
    /**
     * Selects the best edge node to offload a task to.
     *
     * @param task The task to be offloaded
     * @param device The IoT device that generated the task
     * @param edgeNodes List of available edge nodes
     * @return The selected edge node, or null if none suitable
     */
    EdgeNode selectEdgeNode(Task task, IoTDevice device, List<EdgeNode> edgeNodes);
    
    /**
     * Get the name of this offloading policy.
     *
     * @return The policy name
     */
    String getName();
}
