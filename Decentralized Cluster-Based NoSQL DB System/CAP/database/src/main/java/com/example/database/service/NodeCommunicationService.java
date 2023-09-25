package com.example.database.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class NodeCommunicationService {

    @Value("#{'${nodes}'.split(',')}")
    private List<String> nodeAddresses;

    @Value("${node.name}")
    private String nodeName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void synchronizeDocumentCreation(String id, String content, String affinityNode, String nodeName) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/document-sync-create/" + id
                    + "?affinityNode=" + affinityNode
                    + "&nodeName=" + nodeName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(content, headers);

            restTemplate.postForEntity(url, entity, String.class);
        });
    }

    public void synchronizeDocumentUpdate(String id, String content) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/document-sync-update/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(content, headers);
            restTemplate.put(url, entity);
        });
    }

    public void synchronizeDocumentDeletion(String id) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/document-sync-delete/" + id;
            restTemplate.delete(url);
        });
    }
    public void synchronizeIndex(String property, Object oldValue, Object newValue, String id) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/index-sync-update/" + id
                    + "?property=" + property
                    + "&oldValue=" + oldValue
                    + "&newValue=" + newValue;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(url, entity, String.class);
        });
    }

    public void synchronizeIndexDeletion(String property, Object value, String id) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/index-sync-delete/" + id + "?property=" + property + "&value=" + value;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.delete(url, entity);
        });
    }

    public void synchronizeCache(String id) {
        nodeAddresses.forEach(address -> {
            if (address.contains("//" + nodeName)) {
                return;  // skip the current node
            }
            String url = address + "/db/cache-sync-update/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            restTemplate.postForEntity(url, entity, String.class);
        });
    }


}
