import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { createHmac } from 'crypto';
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
    const accessKeySecret = this.configService.get<string>('OSS_ACCESS_KEY_SECRET', '');
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

    // HMAC-SHA1 签名计算
    const signature = accessKeySecret
      ? createHmac('sha1', accessKeySecret).update(policy).digest('base64')
      : '';

    return {
      uploadUrl,
      key,
      policy,
      accessKeyId,
      signature,
      contentType: data.contentType,
      expiresIn: 3600,
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

  async delete(id: string): Promise<void> {
    const resource = await this.resourcesRepository.findOne({ where: { id } });
    if (!resource) {
      throw new NotFoundException('资源不存在');
    }
    await this.resourcesRepository.delete(id);
  }
}
