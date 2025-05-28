package com.graduationproject.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.dialogflow.v2.WebhookRequest;
import com.google.cloud.dialogflow.v2.WebhookResponse;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.ProductService;
import com.graduationproject.backend.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook/dialogflow")
public class DialogflowWebhookController {

    @Autowired
    private ProductService productService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> handleWebhookRequest(@RequestBody String rawJson) {
        WebhookResponse.Builder webhookResponseBuilder = WebhookResponse.newBuilder();

        try {
            WebhookRequest.Builder requestBuilder = WebhookRequest.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(rawJson, requestBuilder);
            WebhookRequest request = requestBuilder.build();

            String intentName = request.getQueryResult().getIntent().getDisplayName();
            Struct parameters = request.getQueryResult().getParameters();

            if ("GetProductInfo".equals(intentName)) {
                String productName = null;
                Integer productId = null;

                if (parameters.getFieldsMap().containsKey("product_name")) {
                    Value nameValue = parameters.getFieldsMap().get("product_name");
                    if (nameValue != null && nameValue.hasStringValue() && !nameValue.getStringValue().isEmpty()) {
                        productName = nameValue.getStringValue();
                    }
                }

                if (parameters.getFieldsMap().containsKey("product-id")) {
                    Value idValue = parameters.getFieldsMap().get("product-id");
                    if (idValue != null) {
                        if (idValue.hasNumberValue()) {
                            productId = (int) idValue.getNumberValue();
                        } else if (idValue.hasStringValue()) {
                            try {
                                productId = Integer.parseInt(idValue.getStringValue());
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                if ((productId == null || productId == 0) && (productName == null || productName.isEmpty())) {
                    webhookResponseBuilder.setFulfillmentText("Bạn vui lòng cung cấp tên hoặc ID sản phẩm để tôi tra cứu giúp nhé.");
                    return ResponseEntity.ok(toJson(webhookResponseBuilder.build()));
                }

                Product product = null;
                String speechText = "Xin lỗi, tôi không tìm thấy thông tin cho sản phẩm này.";

                if (productId != null && productId != 0) {
                    try {
                        product = productService.findProductEntityById(productId);
                    } catch (ResourceNotFoundException e) {
                        System.err.println("Không tìm thấy sản phẩm theo ID: " + productId);
                    }
                } else if (productName != null && !productName.isEmpty()) {
                    Optional<Product> productOpt = productService.findProductByNameForDialogflow(productName);
                    if (productOpt.isPresent()) {
                        product = productOpt.get();
                    } else {
                        System.err.println("Không tìm thấy sản phẩm theo tên: " + productName);
                    }
                }

                if (product != null) {
                    speechText = String.format("Thông tin sản phẩm %s (ID: %d): Giá là %s VND, tồn kho còn %d sản phẩm. %s",
                            product.getName(), product.getProductId(), product.getPrice().toString(), product.getStock(),
                            product.getStock() > 0 ? "Sản phẩm này còn hàng." : "Sản phẩm này hiện đã hết hàng.");
                }

                webhookResponseBuilder.setFulfillmentText(speechText);

            } else {
                webhookResponseBuilder.setFulfillmentText("Xin lỗi, tôi không hiểu yêu cầu của bạn hoặc chưa được lập trình để trả lời câu hỏi này.");
            }

            return ResponseEntity.ok(toJson(webhookResponseBuilder.build()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"fulfillmentText\":\"Lỗi xử lý webhook: " + e.getMessage() + "\"}");
        }
    }


    private String toJson(WebhookResponse response) throws Exception {
        StringWriter writer = new StringWriter();
        JsonFormat.printer().print(response);
        return writer.toString();
    }

}
