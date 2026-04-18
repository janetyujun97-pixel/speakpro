import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';

/**
 * 邮件下发。教师端密码重置用：
 *   MAIL_PROVIDER=log        —— dev 默认，把 reset 链接打到日志
 *   MAIL_PROVIDER=smtp       —— 走 SMTP（需 MAIL_HOST/PORT/USER/PASS/FROM）
 */
@Injectable()
export class MailProvider {
  private readonly logger = new Logger('MailProvider');
  private transporter: nodemailer.Transporter | null = null;

  constructor(private readonly config: ConfigService) {}

  private getTransporter(): nodemailer.Transporter | null {
    if (this.transporter) return this.transporter;
    const host = this.config.get<string>('MAIL_HOST');
    const port = this.config.get<number>('MAIL_PORT', 465);
    const user = this.config.get<string>('MAIL_USER');
    const pass = this.config.get<string>('MAIL_PASS');
    if (!host || !user || !pass) return null;
    this.transporter = nodemailer.createTransport({
      host,
      port,
      secure: port === 465,
      auth: { user, pass },
    });
    return this.transporter;
  }

  async sendResetEmail(to: string, resetUrl: string): Promise<void> {
    const mode = (this.config.get<string>('MAIL_PROVIDER') || 'log').toLowerCase();
    if (mode !== 'smtp') {
      this.logger.warn(`[DEV MAIL] to=${to} resetUrl=${resetUrl}`);
      return;
    }

    const transporter = this.getTransporter();
    if (!transporter) {
      this.logger.warn('MAIL_PROVIDER=smtp 但 SMTP 凭证未配置，降级为日志模式');
      this.logger.warn(`[DEV MAIL] to=${to} resetUrl=${resetUrl}`);
      return;
    }

    const from = this.config.get<string>('MAIL_FROM', 'no-reply@speakpro.local');
    await transporter.sendMail({
      from,
      to,
      subject: 'SpeakPro 密码重置',
      text: `点击以下链接重置密码，30 分钟内有效：\n\n${resetUrl}\n\n若非本人操作请忽略。`,
      html: `<p>点击以下链接重置密码，30 分钟内有效：</p><p><a href="${resetUrl}">${resetUrl}</a></p><p>若非本人操作请忽略。</p>`,
    });
  }
}
