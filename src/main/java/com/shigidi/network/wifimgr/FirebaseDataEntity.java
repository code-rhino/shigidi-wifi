package com.shigidi.network.wifimgr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class FirebaseDataEntity {
    private final Object obj;

    public FirebaseDataEntity(Object obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            //Logger logger = LoggerFactory.getLogger(CodeSubmissionRequest.class);
            String errorMessage = "Failed to stringify the CodeSubmissionRequest.";
            //logger.error(errorMessage);
            throw new Error(e);
        }
    }
}
