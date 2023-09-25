package com.example.database.controller;

import com.example.database.model.Document;
import com.example.database.model.MedicineContent;
import com.example.database.service.DocumentService;
import com.example.database.service.IndexService;
import com.example.database.service.NodeCommunicationService;
import com.example.database.utils.JsonValidator;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/db")
public class DatabaseController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private NodeCommunicationService nodeCommunicationService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private IndexService indexService;

    @Value("${node.port}")
    private String nodePort;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    @Value("${medicine.schema}")
    private String medicineSchema;

    private JsonValidator validator;

    @PostConstruct
    public void initValidator() {
        validator = new JsonValidator(medicineSchema);
    }


    @PostMapping("/cache-sync-update/{id}")
    public ResponseEntity<String> updateCache(@PathVariable String id) {
        try {
            documentService.updateCacheForSync(id, 180000);  // Update the cache
            logger.info("Cache updated successfully for ID: {}", id);
            return new ResponseEntity<>("Cache updated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to update cache for ID: {}", id, e);
            return new ResponseEntity<>("Failed to update cache.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<String>> getDocumentsByIndexedProperty(@RequestParam String property, @RequestParam String value) {
        try {
            List<String> documentIds = indexService.getIdsByProperty(property, value);
            return ResponseEntity.ok().body(documentIds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/index-sync-update/{id}")
    public ResponseEntity<String> synchronizeIndexUpdate(@PathVariable String id, @RequestParam("property") String property, @RequestParam("oldValue") Object oldValue, @RequestParam("newValue") Object newValue) {
        try {
            documentService.updateIndexEntryForSync(id, property, oldValue, newValue);
            return new ResponseEntity<>("Index updated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to update index.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/index-sync-delete/{id}")
    public ResponseEntity<String> deleteIndexSync(@PathVariable String id, @RequestParam String property, @RequestParam String value) {
        try {
            indexService.removeFromIndex(property, value, id);
            return ResponseEntity.ok("Index deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/document")
    public ResponseEntity<String> createDocument(@RequestBody String content) {
        if (!validator.validate(content)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid medicine schema.");
        }
        try {
            String id = documentService.createDocumentEntry(content);
            Document doc = documentService.retrieveDocumentEntry(id);

            indexService.addToIndex("name", doc.getContent().optString("name"), id);
            indexService.addToIndex("expirationDate", doc.getContent().optString("expirationDate"), id);

            String affinityNode = doc.getAffinityNode();
            String nodeName = doc.getNodeName();

            nodeCommunicationService.synchronizeDocumentCreation(id, content, affinityNode, nodeName); // Synchronize
            nodeCommunicationService.synchronizeIndex("name", null, doc.getContent().optString("name"), id);
            nodeCommunicationService.synchronizeIndex("expirationDate", null, doc.getContent().optString("expirationDate"), id);
            nodeCommunicationService.synchronizeCache(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        }  catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/document-sync-create/{id}")
    public ResponseEntity<String> syncDocumentCreation(@PathVariable String id, @RequestBody String content, @RequestParam String affinityNode, @RequestParam String nodeName) {
        if (!validator.validate(content)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid medicine schema.");
        }

        try {
            // Create a document entry directly using the provided id, content, affinityNode, and nodeName
            documentService.createDocumentEntryForSync(id, content, affinityNode, nodeName);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/document/{id}")
    public ResponseEntity<MedicineContent> getDocument(@PathVariable String id) {
        Document doc = documentService.retrieveDocumentEntry(id);
        if (doc != null) {
            JSONObject content = doc.getContent();

            if (content != null) {
                // Extract the medicine details from the content. This was done to properly format the text when being displayed.
                String medicineId = content.optString("id");
                String name = content.optString("name");
                String expirationDate = content.optString("expirationDate");

                MedicineContent medicineContent = new MedicineContent(medicineId, name, expirationDate);
                return ResponseEntity.ok().body(medicineContent);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PutMapping("/document/{id}")
    public ResponseEntity<String> updateDocument(@PathVariable String id, @RequestBody String content) {
        JSONObject jsonContent = new JSONObject(content); // Convert the string content to JSONObject

        Set<ValidationMessage> validationErrors = validator.getValidationErrors(jsonContent.toString());
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid medicine schema: " + validationErrors);
        }

        Document doc = documentService.retrieveDocumentEntry(id);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String oldName = doc.getContent().optString("name");
        String newName = jsonContent.optString("name");
        String oldExpirationDate = doc.getContent().optString("expirationDate");
        String newExpirationDate = jsonContent.optString("expirationDate");

        String affinityName = doc.getNodeName();
        String affinityPort = doc.getAffinityNode();
        String currentPort = nodePort;
        int currVersion = doc.getVersion();

        try {
            if (Objects.equals(currentPort, affinityPort)) {
                documentService.updateDocumentEntry(id, jsonContent, currVersion); // Pass the JSONObject for updating
                indexService.removeFromIndex("name", oldName, id);
                indexService.addToIndex("name", newName, id);
                indexService.removeFromIndex("expirationDate", oldExpirationDate, id);
                indexService.addToIndex("expirationDate", newExpirationDate, id);
                nodeCommunicationService.synchronizeDocumentUpdate(id, content);
                nodeCommunicationService.synchronizeIndex("name", oldName, newName, id);
                nodeCommunicationService.synchronizeIndex("expirationDate", oldExpirationDate, newExpirationDate, id);
                nodeCommunicationService.synchronizeCache(id);
                return ResponseEntity.ok().body("Document updated.");
            } else {
                logger.info("Redirecting update for document {} to {} at port {}", id, affinityName, affinityPort);
                String targetUrl = "http://" + affinityName + ":8081" + "/db/document/" + id;
                return restTemplate.exchange(targetUrl, HttpMethod.PUT, new HttpEntity<>(content), String.class);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/document-sync-update/{id}")
    public ResponseEntity<String> syncDocumentUpdate(@PathVariable String id, @RequestBody String content) {
        JSONObject jsonContent = new JSONObject(content); // Convert the string content to JSONObject

        if (!validator.validate(content)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid content schema.");
        }

        Document doc = documentService.retrieveDocumentEntry(id);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        int currVersion= doc.getVersion();

        try {
             {
                documentService.updateDocumentEntryForSync(id, jsonContent, currVersion);
                return ResponseEntity.status(HttpStatus.OK).body(id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @DeleteMapping("/document/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {
        Document doc = documentService.retrieveDocumentEntry(id);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String affinityName = doc.getNodeName();
        String affinityPort = doc.getAffinityNode();
        String currentPort = nodePort;
        String nameToDelete = doc.getContent().optString("name");

        try {
            if (Objects.equals(currentPort, affinityPort)) {
                documentService.deleteDocumentEntry(id);
                nodeCommunicationService.synchronizeDocumentDeletion(id);
                nodeCommunicationService.synchronizeIndexDeletion("name", nameToDelete, id);
                nodeCommunicationService.synchronizeCache(id);
                return ResponseEntity.ok().body("Document deleted.");
            }
            else {
                logger.info("Redirecting delete for document {} to node at port {}", id, affinityPort);
                String targetUrl = "http://" + affinityName + ":8081"+ "/db/document/" + id;
                return restTemplate.exchange(targetUrl, HttpMethod.DELETE, null, String.class);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/document-sync-delete/{id}")
    public ResponseEntity<String> syncDocumentDeletion(@PathVariable String id) {
        try {
            documentService.deleteDocumentEntryForSync(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
