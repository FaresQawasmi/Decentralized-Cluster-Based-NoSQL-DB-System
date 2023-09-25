package com.example.bootstrapnode.controller;

import com.example.bootstrapnode.model.Node;
import com.example.bootstrapnode.service.LoadBalancer;
import com.example.bootstrapnode.utils.FileUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class BootstrappingController {
    private LoadBalancer loadBalancer;
    private Logger logger = Logger.getLogger(BootstrappingController.class.getName());

    public BootstrappingController(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @GetMapping("/registerPharmacy/{pharmacyName}")
    public ResponseEntity<String> registerPharmacy(@PathVariable("pharmacyName") String pharmacyName) throws URISyntaxException {
        logger.info("Registering Pharmacy: " + pharmacyName + " ........");

        JSONArray existingPharmacies = FileUtil.readUsersFromFile();

        // Check if pharmacy already exists in file, then redirect to the assigned Node
        for (int i = 0; i < existingPharmacies.length(); i++) {
            JSONObject pharmacy = existingPharmacies.getJSONObject(i);
            if (pharmacyName.equals(pharmacy.getString("pharmacyName"))) {
                logger.warning("Pharmacy " + pharmacyName + " already exists!");
                int existingNodePort = pharmacy.getInt("port");
                URI pharmacyLocation = new URI("http://localhost:" + existingNodePort + "/crud.html");
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(pharmacyLocation);
                return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
            }
        }

        // Generate a unique token for the pharmacy
        String token = UUID.randomUUID().toString();

        // Simulate cluster initialization (injected in Load Balancer) and pharmacy assignment
        Node assignedNode = loadBalancer.getNextNode();

        // Construct JSON response with pharmacy credentials and assigned node information
        JSONObject newPharmacy = new JSONObject();
        newPharmacy.put("pharmacyName", pharmacyName);
        newPharmacy.put("token", token);
        newPharmacy.put("node", assignedNode.getName());
        newPharmacy.put("port", assignedNode.getPort());

        existingPharmacies.put(newPharmacy);
        FileUtil.writeUsersToFile(existingPharmacies);

        logger.info("Success!" + "Pharmacy " + pharmacyName + " registered! Token: " + token + " Node: " + assignedNode.getName());

        // Redirect the client directly to crud.html with their assigned node
        URI crudHtmlLocation = new URI("http://localhost:" + assignedNode.getPort() + "/crud.html");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(crudHtmlLocation);
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}
