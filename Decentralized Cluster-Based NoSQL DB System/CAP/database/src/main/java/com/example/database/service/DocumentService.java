package com.example.database.service;

import com.example.database.cache.Cache;
import com.example.database.controller.DatabaseController;
import com.example.database.exception.DocumentNotFoundException;
import com.example.database.exception.VersionMismatchException;
import com.example.database.model.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    @Autowired
    private DatabaseStorage databaseStorage;

    @Autowired
    private IndexService indexService;

    @Autowired
    private Cache<String, Document> documentCache;

    @Value("${node.port}")
    private String nodePort;

    @Value("${node.name}")
    private String nodeName;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    public String createDocumentEntry(String content) {
        logger.info("Entering createDocumentEntry with content: {}", content);
        String id = java.util.UUID.randomUUID().toString();
        Document document = new Document(id, new JSONObject(content), nodePort, nodeName,0); // Create a new document
        document.setAffinityNode(nodePort);
        document.setNodeName(nodeName);
        logger.info("Document created with AffinityNode: {}, NodeName: {}", document.getAffinityNode(), document.getNodeName());
        databaseStorage.save(document);
        return id;
    }

    public Document retrieveDocumentEntry(String id) {
        Document document = documentCache.get(id);
        if (document != null) {
            return document;
        }
        document = databaseStorage.get(id);
        if (document != null) {
            documentCache.add(id, document, 60000); // Cache it for 60 seconds
        }
        return document;
    }

    public void updateDocumentEntry(String id, JSONObject content, int version) throws DocumentNotFoundException, VersionMismatchException {
        Document existingDoc = databaseStorage.get(id);
        if (existingDoc == null) {
            logger.error("Document not found for ID: {}", id);
            throw new DocumentNotFoundException("Document not found for ID: " + id);
        }
        logger.error("Document Version: {} ", existingDoc.getVersion());
        logger.error("Document Version trying to update: {} ", version);

        if (existingDoc.getVersion() != version) {
            throw new VersionMismatchException("Version mismatch");
        }
        existingDoc.setContent(content);
        existingDoc.setVersion(version+1);
        documentCache.add(id, existingDoc, 180000); // Update the cache, remove every 3 minutes
        databaseStorage.save(existingDoc);
        logger.info("Document updated successfully for ID: {} and Port: {} with Node: {}", id,existingDoc.getAffinityNode(),existingDoc.getNodeName());
    }

    public void deleteDocumentEntry(String id) {
        // Remove from index before deleting
        Document existingDoc = databaseStorage.get(id);
        if (existingDoc != null) {
            indexService.removeIdFromAllIndexes(id);
        }
        databaseStorage.delete(id);
    }

    public void createDocumentEntryForSync(String id, String content, String affinityNode, String nodeName) {
        Document document = new Document(id, new JSONObject(content), affinityNode, nodeName, 0);  // Create a new document
        databaseStorage.save(document);
    }

    public void updateDocumentEntryForSync(String id, JSONObject content, int version) throws DocumentNotFoundException, VersionMismatchException {
        Document existingDoc = databaseStorage.get(id);
        if (existingDoc == null) {
            throw new DocumentNotFoundException("Document not found for ID: " + id);
        }

        logger.error("Document Version: {} ", existingDoc.getVersion());
        logger.error("Document Version trying to update: {} ", version);

        if (existingDoc.getVersion() != version) {
            throw new VersionMismatchException("Version mismatch");
        }

        existingDoc.setContent(content);
        existingDoc.setVersion(version+1);
        databaseStorage.save(existingDoc);
    }

    public void deleteDocumentEntryForSync(String id) {
        databaseStorage.delete(id);
    }

    public void updateIndexEntryForSync(String id, String property, Object oldValue, Object newValue) {
        indexService.updateIndex(property, oldValue, newValue, id);
        logger.info("Synchronized index for on property: {} from {} to {}", property, oldValue, newValue);
    }

    public void updateCacheForSync(String id, long timeToLive){
        Document existingDoc = databaseStorage.get(id);
        if (existingDoc == null) {
            documentCache.remove(id);
        }
        documentCache.add(id, existingDoc, timeToLive);
    }
}
