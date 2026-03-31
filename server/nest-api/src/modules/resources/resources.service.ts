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

  async getUploadSignature(data: { filename: string; contentType: string }) {
    // TODO: 根据配置的对象存储服务（如 S3 / OSS）生成预签名上传 URL
    const bucket = this.configService.get<string>('OSS_BUCKET', 'speakpro-assets');
    const region = this.configService.get<string>('OSS_REGION', 'us-east-1');

    return {
      uploadUrl: `https://${bucket}.s3.${region}.amazonaws.com/uploads/${Date.now()}_${data.filename}`,
      fields: {
        // TODO: 填充预签名所需的字段
      },
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
}
