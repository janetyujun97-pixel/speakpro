package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// tc3HmacSHA256 计算 HMAC-SHA256
func tc3HmacSHA256(key []byte, data string) []byte {
	h := hmac.New(sha256.New, key)
	h.Write([]byte(data))
	return h.Sum(nil)
}

// tc3SHA256Hex 计算 SHA256 哈希的十六进制字符串
func tc3SHA256Hex(s string) string {
	h := sha256.New()
	h.Write([]byte(s))
	return hex.EncodeToString(h.Sum(nil))
}

// tencentCloudRequest 发送 TC3-HMAC-SHA256 签名请求到腾讯云 API
// service: "asr" 或 "soe"
// action: API 操作名（如 "SentenceRecognition"）
// version: API 版本（如 "2019-06-14"）
func tencentCloudRequest(service, action, version, secretID, secretKey, region, body string) ([]byte, error) {
	host := service + ".tencentcloudapi.com"
	endpoint := "https://" + host + "/"

	timestamp := time.Now().Unix()
	date := time.Unix(timestamp, 0).UTC().Format("2006-01-02")

	// Step 1: CanonicalRequest
	contentType := "application/json; charset=utf-8"
	canonicalHeaders := fmt.Sprintf("content-type:%s\nhost:%s\nx-tc-action:%s\n",
		contentType, host, strings.ToLower(action))
	signedHeaders := "content-type;host;x-tc-action"
	hashedPayload := tc3SHA256Hex(body)
	canonicalRequest := fmt.Sprintf("POST\n/\n\n%s\n%s\n%s",
		canonicalHeaders, signedHeaders, hashedPayload)

	// Step 2: StringToSign
	algorithm := "TC3-HMAC-SHA256"
	credentialScope := fmt.Sprintf("%s/%s/tc3_request", date, service)
	stringToSign := fmt.Sprintf("%s\n%d\n%s\n%s",
		algorithm, timestamp, credentialScope, tc3SHA256Hex(canonicalRequest))

	// Step 3: Signature
	secretDate := tc3HmacSHA256([]byte("TC3"+secretKey), date)
	secretService := tc3HmacSHA256(secretDate, service)
	secretSigning := tc3HmacSHA256(secretService, "tc3_request")
	signature := hex.EncodeToString(tc3HmacSHA256(secretSigning, stringToSign))

	// Step 4: Authorization header
	authorization := fmt.Sprintf("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
		algorithm, secretID, credentialScope, signedHeaders, signature)

	// 发送请求
	req, err := http.NewRequest("POST", endpoint, strings.NewReader(body))
	if err != nil {
		return nil, fmt.Errorf("创建请求失败: %w", err)
	}

	req.Header.Set("Content-Type", contentType)
	req.Header.Set("Host", host)
	req.Header.Set("X-TC-Action", action)
	req.Header.Set("X-TC-Version", version)
	req.Header.Set("X-TC-Timestamp", fmt.Sprintf("%d", timestamp))
	req.Header.Set("X-TC-Region", region)
	req.Header.Set("Authorization", authorization)

	client := &http.Client{Timeout: 60 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("请求失败: %w", err)
	}
	defer resp.Body.Close()

	return io.ReadAll(resp.Body)
}
