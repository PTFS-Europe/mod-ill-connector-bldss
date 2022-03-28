package org.folio.util;

import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class BLDSSAuth {

  private String authorisation;
  private final String api_application;
  private final String api_application_auth;
  private final String api_key;
  private final String api_key_auth;
  private final String request_time;
  private final String nonce;
  private final String signature_method;
  private final String httpMethod;
  private final String path;
  private final String payload;
  private final HashMap<String, String> requestParameters;

  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  public BLDSSAuth(String httpMethod, String path, HashMap<String, String> requestParameters, String payload, JSONObject conf) {
    this.api_application = conf.getString("apiApplication");
    this.api_application_auth = conf.getString("apiApplicationAuth");
    this.api_key = conf.getString("apiKey");
    this.api_key_auth = conf.getString("apiKeyAuth");
    this.request_time = String.valueOf(System.currentTimeMillis());
    this.nonce = this.getNonce();
    this.signature_method = "HMAC-SHA1";
    this.requestParameters = requestParameters;
    this.httpMethod = httpMethod;
    this.path = path;
    this.payload = payload;
  }

  // Return the authorisation string
  public String getAuthorisation() {
    String requestString = getRequestString();
    String hmacKey = this.api_application_auth + "&" + this.api_key_auth;
    return calculateHMAC(requestString, hmacKey);
  }

  // Return a nonce
  private String getNonce() {
    return RandomStringUtils.randomAlphanumeric(10);
  }

  // Generate the request string
  private String getRequestString() {
    String paramString = getParameterString();
    return this.httpMethod + "&" + encodeValue(this.path) + "&" + encodeValue(paramString);
  }

  // Return the Authorisation header contents
  public String getHeaderString() {
    String authString = this.getAuthorisation();
    // The order of elements apparently matters
    String[] headerElements = {
      "api_application=" + this.api_application,
      "nonce=" + this.nonce,
      "signature_method=" + this.signature_method,
      "request_time=" + this.request_time,
      "authorisation=" + authString,
      "api_key=" + this.api_key
    };
    return String.join(",", headerElements);
  }

  // Generate the parameter string
  private String getParameterString() {
    // TreeMap so we store keys in sorted order
    Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    // First add our authentication request parameters
    map.put("api_application", this.api_application);
    map.put("api_key", this.api_key);
    map.put("request", this.payload);
    map.put("request_time", this.request_time);
    map.put("nonce", this.nonce);
    map.put("signature_method", this.signature_method);
    // Now add our request parameters
    if (this.requestParameters != null) {
      map.putAll(this.requestParameters);
    }
    // Return a "&" concatenated key/value pair string
    return map.entrySet()
      .stream()
      .map(e -> {
        if (e.getKey() != null && e.getValue() != null) {
          return encodeValue(e.getKey()) + "=" + encodeValue(e.getValue());
        } else {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.joining("&"));
  }

  // URL encode a string
  private static String encodeValue(String value) {
    String output = "";
    try {
      output = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return output;
  }

  // Receive a string and key and return a HMAC-SHA1 string
  private static String calculateHMAC(String data, String key) {
    String output = "";
    // Based on https://apitest.bldss.bl.uk/docs/guide/single.html#hmac
    try {
      // Get an hmac_sha1 key from the raw key bytes
      SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

      // Get an hmac_sha1 Mac instance and initialize with the signing key
      Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(signingKey);

      // Compute the hmac on input data bytes
      byte[] rawHmac = mac.doFinal(data.getBytes());

      // Base64-encode the hmac
      output = Base64.getEncoder().encodeToString(rawHmac);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      e.printStackTrace();
    }
    return output;
  }

}
