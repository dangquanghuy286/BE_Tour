package com.project.booktour.components;

import com.project.booktour.configurations.VNPayConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class VNPayUtil {

    public static String generatePaymentUrl(VNPayConfig vnPayConfig, String orderId, double amount, String ipAddress) throws Exception {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf((long) (amount * 100))); // VNPay requires amount * 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang: " + orderId);
        vnpParams.put("vnp_OrderType", "tour");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(new Date());
        vnpParams.put("vnp_CreateDate", createDate);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        String expireDate = formatter.format(cal.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()))
                    .append("&");
        }

        String queryString = query.substring(0, query.length() - 1);
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), queryString);
        queryString += "&vnp_SecureHash=" + secureHash;

        return vnPayConfig.getPaymentUrl() + "?" + queryString;
    }

    public static String hmacSHA512(String key, String data) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        byte[] hmacData = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hmacData) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static boolean verifyChecksum(VNPayConfig vnPayConfig, Map<String, String> vnpParams, String secureHash) throws Exception {
        Map<String, String> sortedParams = new TreeMap<>(vnpParams);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            signData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()))
                    .append("&");
        }
        String signDataStr = signData.substring(0, signData.length() - 1);
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), signDataStr);
        return calculatedHash.equalsIgnoreCase(secureHash);
    }
}