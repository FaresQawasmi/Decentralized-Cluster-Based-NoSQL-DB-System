package com.example.bootstrapnode.service;

import com.example.bootstrapnode.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoadBalancer {
    private final ClusterInitializer clusterInitializer;
    private int nextWorkerIndex = 0;

    @Autowired
    public LoadBalancer(ClusterInitializer clusterInitializer) {
        this.clusterInitializer = clusterInitializer;
    }

    public Node getNextNode() {
        List<Node> nodes = new ArrayList<>(clusterInitializer.getAllNodes());

        // Round-robin logic to distribute users among nodes
        Node nextNode = nodes.get(nextWorkerIndex);
        nextWorkerIndex = (nextWorkerIndex + 1) % nodes.size();

        return nextNode;
    }
}

