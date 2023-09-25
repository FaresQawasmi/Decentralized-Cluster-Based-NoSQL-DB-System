package com.example.database.model;
import org.json.JSONObject;

public class Document {
    private String id;
    private JSONObject content;
    private String nodePort;
    private String nodeName;
    private int version;

    public Document(String id, JSONObject content, String nodePort, String nodeName, int version) {
        this.id = id;
        this.content = content;
        this.nodePort = nodePort;
        this.nodeName = nodeName;
        this.version = version;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject getContent() {
        return content;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getAffinityNode() {
        return nodePort;
    }

    public void setAffinityNode(String nodePort) {
        this.nodePort = nodePort;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
