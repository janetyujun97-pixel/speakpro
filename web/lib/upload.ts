import { api } from "./api";

// OSS 上传签名响应（匹配后端 resources.service.ts 返回结构）
interface UploadSignature {
  uploadUrl: string;
  key: string;
  policy: string;
  accessKeyId: string;
  signature: string;
  contentType: string;
  expiresIn: number;
}

/**
 * 从后端获取 OSS 上传签名
 */
export async function getUploadSignature(
  filename: string,
  contentType: string
): Promise<UploadSignature> {
  return api.post<UploadSignature>("/resources/upload-sign", {
    filename,
    contentType,
  });
}

/**
 * 使用签名直传文件到 OSS
 */
export async function uploadToOSS(
  file: File,
  signature: UploadSignature
): Promise<string> {
  const formData = new FormData();

  // OSS PostObject 要求的字段顺序
  formData.append("key", signature.key);
  formData.append("policy", signature.policy);
  formData.append("OSSAccessKeyId", signature.accessKeyId);
  formData.append("signature", signature.signature);
  formData.append("Content-Type", signature.contentType);
  formData.append("file", file);

  const response = await fetch(signature.uploadUrl, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`文件上传失败: ${response.status}`);
  }

  return `${signature.uploadUrl}/${signature.key}`;
}

/**
 * 一键上传文件（获取签名 → 直传 OSS → 返回文件 URL）
 */
export async function uploadFile(file: File): Promise<string> {
  const signature = await getUploadSignature(file.name, file.type);
  return uploadToOSS(file, signature);
}
