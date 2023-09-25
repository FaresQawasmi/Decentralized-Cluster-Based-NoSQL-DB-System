package com.example.database.utils;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public class JsonValidator {

    private final JsonSchema schema;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonValidator(String schemaString) {
        try {
            JsonNode rawSchema = objectMapper.readTree(schemaString);
            this.schema = JsonSchemaFactory.getInstance().getSchema(rawSchema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse schema string.", e);
        }
    }

    public boolean validate(String jsonContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            Set<ValidationMessage> validationResult = schema.validate(jsonNode);
            return validationResult.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public Set<ValidationMessage> getValidationErrors(String jsonContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            return schema.validate(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate JSON content.", e);
        }
    }

}
