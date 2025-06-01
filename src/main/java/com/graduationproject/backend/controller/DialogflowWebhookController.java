// src/main/java/com/graduationproject/backend/controller/DialogflowWebhookController.java
package com.graduationproject.backend.controller;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.WebhookRequest;
import com.google.cloud.dialogflow.v2.WebhookResponse;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.graduationproject.backend.dto.CategoryDTO;
import com.graduationproject.backend.dto.OrderDTO;
import com.graduationproject.backend.dto.ProductPageDTO; // Đảm bảo DTO này được import đúng
import com.graduationproject.backend.entity.Product;
import com.graduationproject.backend.entity.User;
import com.graduationproject.backend.exception.ResourceNotFoundException;
import com.graduationproject.backend.service.CategoryService;
import com.graduationproject.backend.service.OrderService;
import com.graduationproject.backend.service.ProductService;
import com.graduationproject.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page; // << THÊM IMPORT NÀY
import org.springframework.data.domain.PageRequest; // << THÊM IMPORT NÀY
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhook/dialogflow")
public class DialogflowWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(DialogflowWebhookController.class);

    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private CategoryService categoryService;

    @org.springframework.beans.factory.annotation.Value("${dialogflow.project-id}")
    private String dialogflowProjectId;

    private String formatCurrency(java.math.BigDecimal amount, Locale locale) {
        if (amount == null) return messageSource.getMessage("invoicePage.dataNotAvailable", null, "N/A", locale);
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#,###");
        String currencySuffix = " VND";
        if (locale.getLanguage().equals("en")) currencySuffix = " USD";
        else if (locale.getLanguage().equals("vi")) currencySuffix = " đ";
        return df.format(amount) + currencySuffix;
    }

    private String getLocalizedOrderStatus(String statusKey, Locale locale) {
        try {
            return messageSource.getMessage("orderStatus." + statusKey, null, statusKey, locale);
        } catch (Exception e) {
            logger.warn("Missing translation for orderStatus.{} in locale {}", statusKey, locale.getLanguage());
            return statusKey;
        }
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> handleWebhookRequest(@RequestBody String rawJson) {
        WebhookResponse.Builder webhookResponseBuilder = WebhookResponse.newBuilder();
        logger.debug("Received Dialogflow webhook request (raw): {}", rawJson);

        try {
            WebhookRequest.Builder requestBuilder = WebhookRequest.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(rawJson, requestBuilder);
            WebhookRequest request = requestBuilder.build();

            String intentName = request.getQueryResult().getIntent().getDisplayName();
            Struct parameters = request.getQueryResult().getParameters();
            String sessionIdFromRequest = request.getSession();
            String shortSessionId = sessionIdFromRequest.substring(sessionIdFromRequest.lastIndexOf('/') + 1);
            String languageCode = request.getQueryResult().getLanguageCode();
            Locale userLocale = Locale.forLanguageTag(languageCode);

            logger.info("Intent Display Name: {}", intentName);
            // logger.debug("Parameters received: {}", parameters.toString());

            String speechText = messageSource.getMessage("error.generic.webhook", null, "Xin lỗi, không thể xử lý yêu cầu.", userLocale);

            if ("GetProductInfo".equals(intentName) || "GetProductDetail".equals(intentName)) {
                // ... (Giữ nguyên logic cho GetProductInfo và GetProductDetail như bạn đã có)
                logger.info("Handling {} intent", intentName);
                String productNameParam = null;
                Integer productIdParam = null;

                if (parameters.getFieldsMap().containsKey("any")) {
                    Value nameValue = parameters.getFieldsMap().get("any");
                    if (nameValue != null && nameValue.hasStringValue() && !nameValue.getStringValue().isEmpty()) {
                        productNameParam = nameValue.getStringValue();
                    }
                }
                if (parameters.getFieldsMap().containsKey("number")) {
                    Value idValue = parameters.getFieldsMap().get("number");
                    if (idValue != null) {
                        if (idValue.hasNumberValue()) {
                            productIdParam = (int) idValue.getNumberValue();
                        } else if (idValue.hasStringValue() && !idValue.getStringValue().isEmpty()) {
                            try { productIdParam = Integer.parseInt(idValue.getStringValue()); }
                            catch (NumberFormatException e) { logger.warn("Could not parse product ID from string: {}", idValue.getStringValue());}
                        }
                    }
                }
                logger.info("Intent [{}], Params - productName: '{}', productId: '{}'", intentName, productNameParam, productIdParam);

                if ((productIdParam == null || productIdParam == 0) && (productNameParam == null || productNameParam.isEmpty())) {
                    speechText = messageSource.getMessage("dialogflow.product.askForIdOrName", null, "Bạn vui lòng cung cấp tên hoặc ID sản phẩm.", userLocale);
                } else {
                    Product product = null;
                    if (productIdParam != null && productIdParam != 0) {
                        try { product = productService.findProductEntityById(productIdParam); }
                        catch (ResourceNotFoundException e) {
                            if (productNameParam != null && !productNameParam.isEmpty()) {
                                product = productService.findProductByNameForDialogflow(productNameParam).orElse(null);
                            }
                        }
                    } else if (productNameParam != null && !productNameParam.isEmpty()) {
                        product = productService.findProductByNameForDialogflow(productNameParam).orElse(null);
                    }

                    if (product != null) {
                        String stockStatusMsgKey = product.getStock() > 0 ? "dialogflow.product.inStock" : "dialogflow.product.outOfStock";
                        String stockStatusDefault = product.getStock() > 0 ? "Còn hàng." : "Hết hàng.";
                        String stockStatus = messageSource.getMessage(stockStatusMsgKey, null, stockStatusDefault, userLocale);

                        if ("GetProductDetail".equals(intentName)) {
                            StringBuilder detailBuilder = new StringBuilder();
                            detailBuilder.append(String.format(messageSource.getMessage("dialogflow.product.detail.intro", null, "Thông tin chi tiết sản phẩm %s (ID: %d):\n", userLocale), product.getName(), product.getProductId()));
                            detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.priceLabel", null, "Giá", userLocale), formatCurrency(product.getPrice(), userLocale) ));
                            detailBuilder.append(String.format("- %s: %d. %s\n", messageSource.getMessage("dialogflow.product.stockLabel", null, "Tồn kho", userLocale), product.getStock(), stockStatus));
                            if (product.getBrand() != null && !product.getBrand().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.brandLabel", null, "Thương hiệu", userLocale), product.getBrand()));
                            if (product.getModel() != null && !product.getModel().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.modelLabel", null, "Mẫu mã", userLocale), product.getModel()));
                            if (product.getCaseMaterial() != null && !product.getCaseMaterial().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.caseMaterialLabel",null,"Chất liệu vỏ",userLocale), product.getCaseMaterial()));
                            if (product.getStrapMaterial() != null && !product.getStrapMaterial().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.strapMaterialLabel",null,"Chất liệu dây đeo",userLocale), product.getStrapMaterial()));
                            if (product.getDialColor() != null && !product.getDialColor().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.dialColorLabel",null,"Màu mặt số",userLocale), product.getDialColor()));
                            if (product.getWaterResistance() != null && !product.getWaterResistance().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.waterResistanceLabel",null,"Khả năng chống nước",userLocale), product.getWaterResistance()));
                            if (product.getDescription() != null && !product.getDescription().isEmpty()) detailBuilder.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.product.descriptionLabel", null, "Mô tả", userLocale), product.getDescription()));
                            speechText = detailBuilder.toString().trim();
                        } else { // GetProductInfo
                            speechText = String.format(messageSource.getMessage("dialogflow.product.infoFormat", null, "Sản phẩm %s (ID: %d) giá %s, tồn kho %d. %s", userLocale),
                                    product.getName(), product.getProductId(), formatCurrency(product.getPrice(), userLocale), product.getStock(), stockStatus);
                        }
                    } else {
                        speechText = messageSource.getMessage("dialogflow.product.notFound", null, "Xin lỗi, tôi không tìm thấy sản phẩm bạn yêu cầu.", userLocale);
                    }
                }

            } else if ("SearchProductByCategory".equals(intentName)) {
                logger.info("Handling SearchProductByCategory intent");
                String categoryNameParam; // Khởi tạo để tránh lỗi nếu không tìm thấy param
                if (parameters.getFieldsMap().containsKey("category_name_param")) {
                    Value catNameValue = parameters.getFieldsMap().get("category_name_param");
                    if (catNameValue != null && catNameValue.hasStringValue() && !catNameValue.getStringValue().isEmpty()) {
                        categoryNameParam = catNameValue.getStringValue();
                    } else {
                        categoryNameParam = null;
                    }
                } else {
                    categoryNameParam = null;
                }

                if (categoryNameParam != null && !categoryNameParam.isEmpty()) {
                    logger.info("Searching products for category name: {}", categoryNameParam);
                    List<CategoryDTO> allCategories = categoryService.getAllCategories();
                    Optional<CategoryDTO> foundCategory = allCategories.stream()
                            .filter(cat -> cat.getName().equalsIgnoreCase(categoryNameParam))
                            .findFirst();

                    if (foundCategory.isPresent()) {
                        int categoryId = foundCategory.get().getCategoryId();
                        Pageable pageable = PageRequest.of(0, 3);

                        // === SỬA LỖI CLASSCASTEXCEPTION ===
                        Page<com.graduationproject.backend.dto.ProductDTO> springProductPage =
                                productService.findProductsByFilter(null, categoryId, null, null, pageable);
                        ProductPageDTO customProductPage = ProductPageDTO.fromPage(springProductPage);
                        // ==================================

                        if (customProductPage != null && !customProductPage.getContent().isEmpty()) {
                            List<String> productNames = customProductPage.getContent().stream()
                                    .map(pDto -> pDto.getName() + " (" + formatCurrency(pDto.getPrice(), userLocale) + ")")
                                    .collect(Collectors.toList());
                            speechText = String.format(messageSource.getMessage("dialogflow.category.productsFound", null, "Trong danh mục %s, có %d sản phẩm. Ví dụ: %s.", userLocale),
                                    categoryNameParam, customProductPage.getTotalItems(), String.join("; ", productNames));
                        } else {
                            speechText = String.format(messageSource.getMessage("dialogflow.category.noProductsFound", null, "Xin lỗi, không có sản phẩm nào trong danh mục %s.", userLocale), categoryNameParam);
                        }
                    } else {
                        speechText = String.format(messageSource.getMessage("dialogflow.category.notFound", null, "Không tìm thấy danh mục '%s'.", userLocale), categoryNameParam);
                    }
                } else {
                    speechText = messageSource.getMessage("dialogflow.category.askForName", null, "Bạn muốn tìm sản phẩm trong danh mục nào?", userLocale);
                }

            } else if ("SearchProductByBrand".equals(intentName)) {
                logger.info("Handling SearchProductByBrand intent");
                String brandNameParam = null;
                if (parameters.getFieldsMap().containsKey("brand_name_param")) {
                    Value brandNameValue = parameters.getFieldsMap().get("brand_name_param");
                    if (brandNameValue != null && brandNameValue.hasStringValue() && !brandNameValue.getStringValue().isEmpty()) {
                        brandNameParam = brandNameValue.getStringValue();
                    }
                }

                if (brandNameParam != null && !brandNameParam.isEmpty()) {
                    logger.info("Searching products for brand name: {}", brandNameParam);
                    Pageable pageable = PageRequest.of(0, 3);

                    // === SỬA LỖI CLASSCASTEXCEPTION ===
                    Page<com.graduationproject.backend.dto.ProductDTO> springProductPage =
                            productService.findProductsByFilter(brandNameParam, null, null, null, pageable);
                    ProductPageDTO customProductPage = ProductPageDTO.fromPage(springProductPage);
                    // ==================================

                    if (customProductPage != null && !customProductPage.getContent().isEmpty()) {
                        List<String> productNames = customProductPage.getContent().stream()
                                .map(pDto -> pDto.getName() + " (" + formatCurrency(pDto.getPrice(), userLocale) + ")")
                                .collect(Collectors.toList());
                        speechText = String.format(messageSource.getMessage("dialogflow.brand.productsFound", null, "Sản phẩm của thương hiệu %s: %s.", userLocale),
                                brandNameParam, String.join("; ", productNames));
                    } else {
                        speechText = String.format(messageSource.getMessage("dialogflow.brand.noProductsFound", null, "Không tìm thấy sản phẩm nào của thương hiệu %s.", userLocale), brandNameParam);
                    }
                } else {
                    speechText = messageSource.getMessage("dialogflow.brand.askForName", null, "Bạn muốn tìm sản phẩm của thương hiệu nào?", userLocale);
                }

            } else if ("StartPlacingOrder".equals(intentName)) {
                logger.info("Handling StartPlacingOrder intent");
                Struct webhookPayload = request.getQueryResult().getWebhookPayload();
                String authenticatedUserIdFromPayload = null;
                if (webhookPayload != null && webhookPayload.getFieldsMap().containsKey("authenticated_user_id")) {
                    authenticatedUserIdFromPayload = webhookPayload.getFieldsMap().get("authenticated_user_id").getStringValue();
                }

                if (authenticatedUserIdFromPayload != null && !authenticatedUserIdFromPayload.isEmpty()) {
                    boolean cartIsNotEmpty = true; // Giả định
                    if (cartIsNotEmpty) {
                        speechText = messageSource.getMessage("dialogflow.order.start.askShippingName", null, "Để đặt hàng, tên người nhận là gì?", userLocale);
                        String outputContextName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_shipping_info", dialogflowProjectId, shortSessionId);
                        Context shippingContext = Context.newBuilder().setName(outputContextName).setLifespanCount(5).build();
                        webhookResponseBuilder.addOutputContexts(shippingContext);
                    } else {
                        speechText = messageSource.getMessage("dialogflow.order.start.cartEmpty", null, "Giỏ hàng trống.", userLocale);
                    }
                } else {
                    speechText = messageSource.getMessage("dialogflow.order.start.loginRequired", null, "Cần đăng nhập để đặt hàng.", userLocale);
                }


            } else if ("GetOrderDetail_AskForID".equals(intentName)) {
                logger.info("Handling GetOrderDetail_AskForID intent.");
                String orderIdCaptured = null;
                if (parameters.getFieldsMap().containsKey("order_id_capture")) {
                    Value paramValue = parameters.getFieldsMap().get("order_id_capture");
                    if (paramValue.hasStringValue() && !paramValue.getStringValue().isEmpty()) {
                        orderIdCaptured = paramValue.getStringValue().replaceAll("[^0-9A-Za-z-]", "");
                    } else if (paramValue.hasNumberValue()) {
                        orderIdCaptured = String.valueOf((int) paramValue.getNumberValue());
                    }
                }
                logger.info("GetOrderDetail_AskForID - Captured Order ID: '{}'", orderIdCaptured);

                if (orderIdCaptured != null && !orderIdCaptured.isEmpty()) {
                    String outputContextName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_customer_email_for_order", dialogflowProjectId, shortSessionId);
                    Struct.Builder contextParamsBuilder = Struct.newBuilder();
                    contextParamsBuilder.putFields("order_id_from_context", Value.newBuilder().setStringValue(orderIdCaptured).build());
                    Context outputContext = Context.newBuilder().setName(outputContextName).setLifespanCount(2).setParameters(contextParamsBuilder.build()).build();
                    webhookResponseBuilder.addOutputContexts(outputContext);
                    speechText = String.format(messageSource.getMessage("dialogflow.order.askForEmailVerification", null, "Được rồi! Để xác minh, bạn vui lòng cung cấp địa chỉ email đã sử dụng để đặt đơn hàng #%s nhé?", userLocale), orderIdCaptured);
                } else {
                    speechText = messageSource.getMessage("dialogflow.order.askForOrderId", null, "Bạn muốn kiểm tra đơn hàng nào? Vui lòng cho tôi biết mã đơn hàng của bạn.", userLocale);
                }

            } else if ("GetOrderDetail_VerifyEmailAndFetch".equals(intentName)) {

                logger.info("Handling GetOrderDetail_VerifyEmailAndFetch intent.");
                String orderIdFromContext = null;
                String emailProvided = null;
                String inputContextNameSuffix = "/contexts/awaiting_customer_email_for_order";

                for (Context context : request.getQueryResult().getOutputContextsList()) {
                    if (context.getName().endsWith(inputContextNameSuffix)) {
                        if (context.getParameters().getFieldsMap().containsKey("order_id_from_context")) {
                            orderIdFromContext = context.getParameters().getFieldsMap().get("order_id_from_context").getStringValue();
                            break;
                        }
                    }
                }
                if (parameters.getFieldsMap().containsKey("customer_email_provided")) {
                    Value emailValue = parameters.getFieldsMap().get("customer_email_provided");
                    if (emailValue != null && emailValue.hasStringValue() && !emailValue.getStringValue().isEmpty()) {
                        emailProvided = emailValue.getStringValue();
                    }
                }
                logger.info("VerifyEmail - Order ID from context: '{}', Email provided: '{}'", orderIdFromContext, emailProvided);

                if (orderIdFromContext != null && !orderIdFromContext.isEmpty() &&
                        emailProvided != null && !emailProvided.isEmpty()) {
                    try {
                        int parsedOrderId = Integer.parseInt(orderIdFromContext);
                        OrderDTO orderDTO = orderService.findOrderDTOById(parsedOrderId);
                        User orderUser = userService.findById(orderDTO.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("User for order " + parsedOrderId, "ID", orderDTO.getUserId()));

                        if (orderUser.getEmail().equalsIgnoreCase(emailProvided)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format(messageSource.getMessage("dialogflow.order.detail.introVerified", null, "Đã xác thực! Thông tin đơn hàng #%d của bạn:\n", userLocale), orderDTO.getOrderId()));
                            String localizedStatus = getLocalizedOrderStatus(orderDTO.getStatus(), userLocale);
                            sb.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.order.statusLabel", null, "Trạng thái", userLocale), localizedStatus));
                            sb.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.order.totalAmountLabel", null, "Tổng tiền", userLocale), formatCurrency(orderDTO.getTotalAmount(), userLocale)));
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", userLocale);
                            sb.append(String.format("- %s: %s\n", messageSource.getMessage("dialogflow.order.dateLabel", null, "Ngày đặt", userLocale), sdf.format(orderDTO.getCreatedAt())));
                            if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
                                sb.append(String.format("- %s (%d %s):\n", messageSource.getMessage("dialogflow.order.itemsLabel",null,"Sản phẩm",userLocale), orderDTO.getOrderItems().size(), messageSource.getMessage("dialogflow.order.itemTypesLabel",null,"loại",userLocale)));
                                for (com.graduationproject.backend.dto.OrderItemDTO item : orderDTO.getOrderItems()) {
                                    sb.append(String.format("  + %s (%s: %d)\n", item.getProductName(), messageSource.getMessage("dialogflow.order.quantityLabel",null,"SL",userLocale), item.getQuantity()));
                                }
                            } else {
                                sb.append(messageSource.getMessage("dialogflow.order.noItems", null, "- Đơn hàng không có sản phẩm nào.\n", userLocale));
                            }
                            speechText = sb.toString().trim();
                            String contextToClearName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_customer_email_for_order", dialogflowProjectId, shortSessionId);
                            Context clearContext = Context.newBuilder().setName(contextToClearName).setLifespanCount(0).build();
                            webhookResponseBuilder.addOutputContexts(clearContext);
                        } else {
                            speechText = String.format(messageSource.getMessage("dialogflow.order.emailMismatch", null, "Xin lỗi, email không khớp với đơn hàng #%s.", userLocale), orderIdFromContext);
                        }
                    } catch (ResourceNotFoundException e) {
                        speechText = String.format(messageSource.getMessage("dialogflow.order.notFoundOrUserError", null, "Không tìm thấy đơn hàng #%s hoặc thông tin người dùng.", userLocale), orderIdFromContext);
                        String contextToClearName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_customer_email_for_order", dialogflowProjectId, shortSessionId);
                        Context clearContext = Context.newBuilder().setName(contextToClearName).setLifespanCount(0).build();
                        webhookResponseBuilder.addOutputContexts(clearContext);
                    } catch (Exception e) {
                        logger.error("Unexpected error in VerifyEmailAndFetch for order {}: {}", orderIdFromContext, e.getMessage(), e);
                        speechText = messageSource.getMessage("dialogflow.order.verificationError", null, "Lỗi khi xác thực hoặc lấy thông tin đơn hàng.", userLocale);
                        String contextToClearName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_customer_email_for_order", dialogflowProjectId, shortSessionId);
                        Context clearContext = Context.newBuilder().setName(contextToClearName).setLifespanCount(0).build();
                        webhookResponseBuilder.addOutputContexts(clearContext);
                    }
                } else {
                    speechText = messageSource.getMessage("dialogflow.order.missingInfoForVerification", null, "Tôi cần mã đơn hàng và email để xác minh.", userLocale);
                    String contextToClearName = String.format("projects/%s/agent/sessions/%s/contexts/awaiting_customer_email_for_order", dialogflowProjectId, shortSessionId);
                    Context clearContext = Context.newBuilder().setName(contextToClearName).setLifespanCount(0).build();
                    webhookResponseBuilder.addOutputContexts(clearContext);
                }
            }
            else {
                speechText = messageSource.getMessage("dialogflow.webhook.unhandledIntent", new Object[]{intentName}, "Xin lỗi, tôi chưa được hướng dẫn xử lý yêu cầu '" + intentName + "' này. Tôi có thể giúp bạn tìm sản phẩm hoặc tra cứu đơn hàng.", userLocale);
                logger.warn("Webhook called for unhandled intent: {}. Sending specific fallback.", intentName);
            }

            logger.info("INTENT: '{}' -- FINAL speechText TO BE SET ON BUILDER: '{}'", intentName, speechText);
            if (speechText != null && !speechText.isEmpty()) {
                webhookResponseBuilder.setFulfillmentText(speechText);
            } else {
                logger.error("CRITICAL: speechText was null or empty for intent '{}' before sending response! Setting a generic error message.", intentName);
                webhookResponseBuilder.setFulfillmentText(messageSource.getMessage("error.generic.webhook.processing", null, "Lỗi xử lý, thử lại sau.", userLocale));
            }

            String jsonResponse = toJson(webhookResponseBuilder.build());
            logger.info("Final Webhook JSON response being SENT to Dialogflow for intent '{}': {}", intentName, jsonResponse);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/json; charset=UTF-8"))
                    .body(jsonResponse);

        } catch (Exception e) {
            logger.error("FATAL Error processing Dialogflow webhook request (outer try-catch): {}", e.getMessage(), e);
            Locale errorLocale = Locale.getDefault();
            try {
                WebhookRequest.Builder tempRequestBuilder = WebhookRequest.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(rawJson, tempRequestBuilder);
                errorLocale = Locale.forLanguageTag(tempRequestBuilder.build().getQueryResult().getLanguageCode());
            } catch (Exception ignored) {}
            return ResponseEntity.badRequest().body(String.format("{\"fulfillmentText\":\"%s\"}", messageSource.getMessage("error.generic.webhook.fatal", null, "Lỗi hệ thống nghiêm trọng.", errorLocale)));
        }
    }

    private String toJson(WebhookResponse response) throws Exception {
        return JsonFormat.printer().includingDefaultValueFields().print(response);
    }
}