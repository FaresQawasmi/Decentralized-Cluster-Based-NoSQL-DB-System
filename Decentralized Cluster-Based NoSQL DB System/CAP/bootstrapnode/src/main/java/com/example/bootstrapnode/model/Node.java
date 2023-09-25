package com.example.bootstrapnode.model;

public class Node {
    private String name;
    private int port;

    public Node(String nodeName, int port) {
        this.name = nodeName;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }
}
