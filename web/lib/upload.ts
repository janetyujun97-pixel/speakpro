import { api } from "./api";

interface UploadSignature {
  url: string;
  fields: Record<string, string>;
  key: string;
}

export async function getUploadSignature(
  filename: string,
  contentType: string
): Promise<UploadSignature> {
  return api.post<UploadSignature>("/upload/signature", {
    filename,
    contentType,
  });
}

export async function uploadToOSS(
  file: File,
  signature: UploadSignature
): Promise<string> {
  const formData = new FormData();

  Object.entries(signature.fields).forEach(([key, value]) => {
    formData.append(key, value);
  });

  formData.append("file", file);

  const response = await fetch(signature.url, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error("文件上传失败");
  }

  return `${signature.url}/${signature.key}`;
}

export async function uploadFile(file: File): Promise<string> {
  const signature = await getUploadSignature(file.name, file.type);
  return uploadToOSS(file, signature);
}
