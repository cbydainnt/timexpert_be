// src/main/java/com/graduationproject/backend/controller/DialogflowWebhookController.java
package com.graduationproject.backend.controller;

import com.google.cloud.dialogflow.v2.WebhookRequest;
import com.google.cloud.dialogflow.v2.WebhookResponse;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.graduationproject.backend.entity.Product; // Đảm bảo đã import
import com.graduationproject.backend.exception.ResourceNotFoundException; // Đảm bảo đã import
import com.graduationproject.backend.service.ProductService;
import org.slf4j.Logger; // Thêm Logger
import org.slf4j.LoggerFactory; // Thêm Logger
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter; // Đổi từ import com.fasterxml.jackson.databind.JsonNode; nếu không dùng nữa
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook/dialogflow")
public class DialogflowWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(DialogflowWebhookController.class); // Khởi tạo Logger

    @Autowired
    private ProductService productService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> handleWebhookRequest(@RequestBody String rawJson) {
        WebhookResponse.Builder webhookResponseBuilder = WebhookResponse.newBuilder();
        logger.info("Received Dialogflow webhook request: {}", rawJson); // Log raw request

        try {
            WebhookRequest.Builder requestBuilder = WebhookRequest.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(rawJson, requestBuilder);
            WebhookRequest request = requestBuilder.build();

            String intentName = request.getQueryResult().getIntent().getDisplayName();
            Struct parameters = request.getQueryResult().getParameters();
            logger.info("Intent Display Name: {}", intentName);
            logger.info("Parameters received: {}", parameters.toString());


            if ("GetProductInfo".equals(intentName)) {
                String productName = null;
                Integer productId = null;

                // Lấy giá trị từ tham số có key "any" (được cấu hình trong Dialogflow là @sys.any)
                if (parameters.getFieldsMap().containsKey("any")) { // <<==== SỬA KEY Ở ĐÂY
                    Value nameValue = parameters.getFieldsMap().get("any");
                    if (nameValue != null && nameValue.hasStringValue() && !nameValue.getStringValue().isEmpty()) {
                        productName = nameValue.getStringValue();
                        logger.info("Extracted product name (from 'any' parameter): {}", productName);
                    }
                }

                // Lấy giá trị từ tham số có key "number" (được cấu hình trong Dialogflow là @sys.number)
                if (parameters.getFieldsMap().containsKey("number")) { // <<==== SỬA KEY Ở ĐÂY
                    Value idValue = parameters.getFieldsMap().get("number");
                    if (idValue != null) {
                        if (idValue.hasNumberValue()) {
                            productId = (int) idValue.getNumberValue();
                            logger.info("Extracted product ID (from 'number' parameter, as number): {}", productId);
                        } else if (idValue.hasStringValue() && !idValue.getStringValue().isEmpty()) {
                            try {
                                productId = Integer.parseInt(idValue.getStringValue());
                                logger.info("Extracted product ID (from 'number' parameter, as string): {}", productId);
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse product ID from string: {}", idValue.getStringValue());
                            }
                        }
                    }
                }

                if ((productId == null || productId == 0) && (productName == null || productName.isEmpty())) {
                    webhookResponseBuilder.setFulfillmentText("Bạn vui lòng cung cấp tên hoặc ID sản phẩm để tôi tra cứu giúp nhé.");
                } else {
                    Product product = null;
                    // Ưu tiên tìm theo ID nếu có
                    if (productId != null && productId != 0) {
                        try {
                            logger.info("Attempting to find product by ID: {}", productId);
                            product = productService.findProductEntityById(productId); // Hàm này trả về Product hoặc ném ResourceNotFoundException
                            logger.info("Product found by ID: {}", product.getName());
                        } catch (ResourceNotFoundException e) {
                            logger.warn("Product not found by ID: {}", productId);
                            // Nếu tìm theo ID thất bại và có productName, thử tìm theo tên
                            if (productName != null && !productName.isEmpty()) {
                                logger.info("Attempting to find product by name (fallback): {}", productName);
                                Optional<Product> productOpt = productService.findProductByNameForDialogflow(productName);
                                if (productOpt.isPresent()) {
                                    product = productOpt.get();
                                    logger.info("Product found by name (fallback): {}", product.getName());
                                } else {
                                    logger.warn("Product also NOT found by name (fallback): {}", productName);
                                }
                            }
                        }
                    } else if (productName != null && !productName.isEmpty()) { // Nếu không có ID, tìm theo tên
                        logger.info("Attempting to find product by name: {}", productName);
                        Optional<Product> productOpt = productService.findProductByNameForDialogflow(productName);
                        if (productOpt.isPresent()) {
                            product = productOpt.get();
                            logger.info("Product found by name: {}", product.getName());
                        } else {
                            logger.warn("Product NOT found by name: {}", productName);
                        }
                    }

                    String speechText;
                    if (product != null) {
                        speechText = String.format("Thông tin sản phẩm %s (ID: %d): Giá là %s VND, tồn kho còn %d sản phẩm. %s",
                                product.getName(), product.getProductId(), product.getPrice().toString(), product.getStock(),
                                product.getStock() > 0 ? "Sản phẩm này còn hàng." : "Sản phẩm này hiện đã hết hàng.");
                    } else {
                        speechText = "Xin lỗi, tôi không tìm thấy thông tin cho sản phẩm bạn yêu cầu.";
                    }
                    logger.info("Final speechText to be set: {}", speechText);
                    webhookResponseBuilder.setFulfillmentText(speechText);
                }
            } else {
                webhookResponseBuilder.setFulfillmentText("Xin lỗi, tôi không hiểu yêu cầu của bạn hoặc chưa được lập trình để trả lời câu hỏi này.");
            }

            String jsonResponse = toJson(webhookResponseBuilder.build());
            logger.info("Webhook JSON response: {}", jsonResponse);
            return ResponseEntity.ok(jsonResponse);

        } catch (Exception e) {
            logger.error("Error processing Dialogflow webhook request: {}", e.getMessage(), e);
            // Trả về một JSON lỗi chuẩn cho Dialogflow
            return ResponseEntity.badRequest().body("{\"fulfillmentText\":\"Đã xảy ra lỗi nội bộ khi xử lý yêu cầu của bạn.\"}");
        }
    }

    private String toJson(WebhookResponse response) throws Exception {
        // JsonFormat.printer() sẽ tự động xử lý việc chuyển đổi.
        // Nếu bạn dùng thư viện Jackson, bạn có thể cần một MessageConverter tùy chỉnh
        // cho Protobuf hoặc chuyển đổi thủ công hơn.
        return JsonFormat.printer().print(response);
    }
}