package com.graduationproject.backend.service;

import com.graduationproject.backend.entity.Order;
import com.graduationproject.backend.exception.OperationFailedException;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException; // Import
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat; // Import
import java.util.*; // Import Map, List, Calendar, TimeZone

@Service
public class VnPayService {

    // Lấy cấu hình từ application.properties
    @Value("${app.vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${app.vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${app.vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    private final String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"; // Giữ sandbox URL
    private final String vnp_Version = "2.1.0";
    private final String vnp_Command = "pay";
    private final String vnp_OrderType = "other";
    private final String vnp_Locale = "vn";
    private final String vnp_CurrCode = "VND";

    public String createPaymentUrl(Order order, String ipAddr) {
        // VNPay yêu cầu số tiền * 100 và là số nguyên
        long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String vnp_TxnRef = String.valueOf(order.getOrderId()); // Mã tham chiếu giao dịch của bạn
        String vnp_OrderInfo = "Thanh toan don hang " + order.getOrderId();

        // Lấy thời gian hiện tại theo múi giờ GMT+7
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        // Thêm thời gian hết hạn (ví dụ: 15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        // Thêm các tham số khác nếu cần (vnp_Bill_*, vnp_Inv_*)

        // Sắp xếp tham số theo alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    // Encode giá trị cho URL query và hash data
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    hashData.append(encodedValue);
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(encodedValue);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace(); // Hoặc xử lý lỗi khác
                }

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = HmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnp_Url + "?" + queryUrl;
    }

    // Giữ nguyên hàm HmacSHA512
    private String HmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new OperationFailedException("Error calculating VNPay hash", ex);
        }
    }

    // Thêm hàm kiểm tra chữ ký trả về từ VNPay (quan trọng)
    public boolean validateReturnSignature(Map<String, String> vnpayParams) {
        String secureHash = vnpayParams.remove("vnp_SecureHash"); // Lấy và xóa hash khỏi map
        if (secureHash == null || secureHash.isEmpty()) {
            return false;
        }

        // Sắp xếp và tạo chuỗi hash data từ các tham số còn lại
        List<String> fieldNames = new ArrayList<>(vnpayParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpayParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String calculatedHash = HmacSHA512(vnp_HashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(secureHash);
    }
}