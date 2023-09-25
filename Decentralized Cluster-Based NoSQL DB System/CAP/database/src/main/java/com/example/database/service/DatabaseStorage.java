package com.example.database.service;

import com.example.database.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DatabaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStorage.class);

    private static final String STORAGE_DIR = "storage";

    public DatabaseStorage() {
        // Create directory if not exists
        File directory = new File(STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdir();
            logger.info("Created storage directory: {}", STORAGE_DIR);
        } else {
            logger.info("Storage directory {} already exists", STORAGE_DIR);
        }
    }

    public Document get(String id) {
        logger.info("Entering get method with id: {}", id);
        try {
            Path filePath = getFilePath(id);
            if (!Files.exists(filePath)) {
                logger.warn("File not found for id: {}", id);
                return null;
            }
            String contentStr = new String(Files.readAllBytes(filePath));
            JSONObject contentJSON = new JSONObject(contentStr);

            // Extract needed information
            JSONObject actualContent = contentJSON.has("documentContent") ? contentJSON.getJSONObject("documentContent") : contentJSON;
            String affinityNode = contentJSON.has("nodePort") ? contentJSON.getString("nodePort") : null;
            String storedNodeName = contentJSON.has("nodeName") ? contentJSON.getString("nodeName") : null;
            int version = contentJSON.has("version") ? contentJSON.getInt("version") : 1;

            logger.info("Document retrieved: AffinityNode {}, NodeName {}", affinityNode, storedNodeName);

            return new Document(id, actualContent, affinityNode, storedNodeName, version);
        } catch (IOException e) {
            logger.error("Failed to read document with id: {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to read document", e);
        }
    }

    public void save(Document document) {
        logger.info("Entering save method with Document: {}", document.toString());
        try {
            JSONObject contentWithNodePort = new JSONObject();
            contentWithNodePort.put("nodePort", document.getAffinityNode());
            contentWithNodePort.put("nodeName", document.getNodeName());
            contentWithNodePort.put("documentContent", document.getContent());
            contentWithNodePort.put("version", document.getVersion());

            Files.write(getFilePath(document.getId()), contentWithNodePort.toString().getBytes());
            logger.info("Successfully saved document with id: {}", document.getId());
        } catch (IOException e) {
            logger.error("Failed to save document with id: {}. Error: {}", document.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save document", e);
        }
    }

    public void delete(String id) {
        logger.info("Entering delete method with id: {}", id);
        try {
            if (Files.deleteIfExists(getFilePath(id))) {
                logger.info("Successfully deleted document with id: {}", id);
            } else {
                logger.warn("Document with id: {} does not exist. No deletion performed.", id);
            }
        } catch (IOException e) {
            logger.error("Failed to delete document with id: {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    private Path getFilePath(String id) {
        return Paths.get(STORAGE_DIR, id + ".json");
    }

}
