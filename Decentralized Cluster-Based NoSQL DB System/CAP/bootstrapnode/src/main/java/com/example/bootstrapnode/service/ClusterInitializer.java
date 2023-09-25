package com.example.bootstrapnode.service;

import com.example.bootstrapnode.model.Node;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClusterInitializer {

    // Node port to Node mapping
    private Map<Integer, Node> nodeMap = new HashMap<>();

    @PostConstruct
    public void initNodes() {
        nodeMap.put(0, new Node("node0", 8081));
        nodeMap.put(1, new Node("node1", 8082));
        nodeMap.put(2, new Node("node2", 8083));
        // Add more nodes as needed
    }

    public Collection<Node> getAllNodes() {
        return nodeMap.values();
    }


}