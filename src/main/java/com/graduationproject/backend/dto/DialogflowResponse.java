package com.graduationproject.backend.dto;

public class DialogflowResponse {
    private String fulfillmentText;

    public DialogflowResponse() {
    }

    public DialogflowResponse(String fulfillmentText) {
        this.fulfillmentText = fulfillmentText;
    }

    public String getFulfillmentText() {
        return fulfillmentText;
    }

    public void setFulfillmentText(String fulfillmentText) {
        this.fulfillmentText = fulfillmentText;
    }
}
