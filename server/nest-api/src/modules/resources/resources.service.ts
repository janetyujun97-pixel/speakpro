import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { Resource } from './entities/resource.entity';

@Injectable()
export class ResourcesService {
  constructor(
    @InjectRepository(Resource)
    private readonly resourcesRepository: Repository<Resource>,
    private readonly configService: ConfigService,
  ) {}

  async getUploadSignature(data: { filename: string; contentType: string; folder?: string }) {
    const bucket = this.configService.get<string>('OSS_BUCKET', 'speakpro');
    const region = this.configService.get<string>('OSS_REGION', 'oss-cn-hangzhou');
    const accessKeyId = this.configService.get<string>('OSS_ACCESS_KEY_ID', '');
    const endpoint = this.configService.get<string>('OSS_ENDPOINT', `https://${region}.aliyuncs.com`);

    const folder = data.folder || 'uploads';
    const ext = data.filename.split('.').pop() || '';
    const key = `${folder}/${Date.now()}_${Math.random().toString(36).slice(2, 8)}.${ext}`;
    const uploadUrl = `${endpoint}/${bucket}`;

    // 生成 Policy (有效期 1 小时)
    const expiration = new Date(Date.now() + 3600 * 1000).toISOString();
    const policyObj = {
      expiration,
      conditions: [
        { bucket },
        ['content-length-range', 0, 50 * 1024 * 1024], // 最大 50MB
        ['starts-with', '$key', folder],
        ['eq', '$Content-Type', data.contentType],
      ],
    };
    const policy = Buffer.from(JSON.stringify(policyObj)).toString('base64');

    // 注意：生产环境需要用 accessKeySecret 计算 HMAC-SHA1 签名
    // 当前返回 policy 和 key，前端使用 STS Token 方式上传
    return {
      uploadUrl,
      key,
      policy,
      accessKeyId,
      contentType: data.contentType,
      expiresIn: 3600,
      // 生产环境应使用 STS 临时凭证或服务端签名
      // signature: hmacSha1(accessKeySecret, policy),
    };
  }

  async create(data: Partial<Resource>): Promise<Resource> {
    const resource = this.resourcesRepository.create(data);
    return this.resourcesRepository.save(resource);
  }

  async findAll(filters?: { examType?: string; type?: string }): Promise<Resource[]> {
    const query = this.resourcesRepository.createQueryBuilder('r');

    if (filters?.examType) {
      query.andWhere('r.exam_type = :examType', { examType: filters.examType });
    }
    if (filters?.type) {
      query.andWhere('r.type = :type', { type: filters.type });
    }

    query.orderBy('r.created_at', 'DESC');
    return query.getMany();
  }
}
