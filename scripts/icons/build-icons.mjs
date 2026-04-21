#!/usr/bin/env node
// 批量栅格化 SpeakPro 品牌图标（方案 A · CLASSIC）
// 输出：
//   app-icons/*.png（多尺寸）
//   ios/SpeakPro/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png
//   android/app/src/main/res/mipmap-*/{ic_launcher.png, ic_launcher_round.png, ic_launcher_foreground.png}

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../..');
// resvg-js 装在 web/ 下，脚本从仓库根运行时需绝对路径引入
const { Resvg } = await import(
  pathToFileURL(join(ROOT, 'web/node_modules/@resvg/resvg-js/index.js')).href
);
const FONT_DIR = join(__dirname, 'fonts');
// 从 @fontsource/* 解压出的静态 TTF（400 italic 足够覆盖本图标所有字形）
const FONT_FILES = [
  join(FONT_DIR, 'Fraunces-Italic.ttf'),
  join(FONT_DIR, 'JetBrainsMono-Regular.ttf'),
];

const SVG_ROUNDED = readFileSync(join(__dirname, 'icon-classic.svg'), 'utf8');
const SVG_SQUARE = readFileSync(join(__dirname, 'icon-classic-square.svg'), 'utf8');
const SVG_FG = readFileSync(join(__dirname, 'icon-foreground.svg'), 'utf8');
const SVG_FAVICON = readFileSync(join(__dirname, 'favicon.svg'), 'utf8');

function render(svg, size) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: size },
    font: {
      fontFiles: FONT_FILES,
      loadSystemFonts: false,
      defaultFontFamily: 'Fraunces',
    },
    background: 'transparent',
  });
  return resvg.render().asPng();
}

function write(path, buf) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, buf);
  console.log('  →', path.replace(ROOT + '/', ''));
}

// ── app-icons/ ───────────────────────────────────────────────────────
console.log('[1/3] app-icons/');
const appIcons = join(ROOT, 'app-icons');
const roundedSizes = [48, 72, 96, 120, 144, 180, 192, 512];
for (const s of roundedSizes) {
  write(join(appIcons, `icon_${s}.png`), render(SVG_ROUNDED, s));
}
// icon_1024_alpha = 带圆角 + 透明角（预览/开发）
write(join(appIcons, 'icon_1024_alpha.png'), render(SVG_ROUNDED, 1024));
// icon_1024 = 方形无圆角、不透明（App Store 提交用）
write(join(appIcons, 'icon_1024.png'), render(SVG_SQUARE, 1024));

// ── iOS AppIcon ──────────────────────────────────────────────────────
console.log('[2/3] iOS AppIcon');
const iosAppIcon = join(
  ROOT,
  'ios/SpeakPro/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png',
);
// App Store 规范：AppIcon 1024 必须方形、不透明
write(iosAppIcon, render(SVG_SQUARE, 1024));

// ── Android mipmap-* ─────────────────────────────────────────────────
console.log('[3/3] Android mipmap-*');
// Launcher icon 尺寸（px）：
// mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192
// adaptive foreground 尺寸（px）：是 launcher 尺寸 × 108/48 = 2.25×
// 但仓库目前仅有 hdpi/xhdpi/xxhdpi，保持不扩充，以免污染其它平台文件。
const androidDpis = [
  { dir: 'mipmap-hdpi', launcher: 72, fg: 162 },
  { dir: 'mipmap-xhdpi', launcher: 96, fg: 216 },
  { dir: 'mipmap-xxhdpi', launcher: 144, fg: 324 },
];
for (const d of androidDpis) {
  const base = join(ROOT, 'android/app/src/main/res', d.dir);
  // Legacy launcher（带圆角 mask，下级平台直接展示）
  write(join(base, 'ic_launcher.png'), render(SVG_ROUNDED, d.launcher));
  // Round variant — 简单起见同样使用 rounded 版本（Android 会再套圆形 mask）
  write(join(base, 'ic_launcher_round.png'), render(SVG_ROUNDED, d.launcher));
  // Adaptive foreground（只有符号，无背景）
  write(join(base, 'ic_launcher_foreground.png'), render(SVG_FG, d.fg));
}

// ── Web app/ 图标 ────────────────────────────────────────────────────
console.log('[4/4] Web app/');
// Next.js 14 文件约定：app/icon.png / app/apple-icon.png / app/favicon.ico
const webApp = join(ROOT, 'web/app');
// 主 icon（浏览器收藏夹 & PWA），用简化 favicon 底 + S，清晰度最高
write(join(webApp, 'icon.png'), render(SVG_FAVICON, 512));
// Apple Touch Icon —— 180×180，带完整圆角版（iOS 会给图标套圆角）
write(join(webApp, 'apple-icon.png'), render(SVG_ROUNDED, 180));

// favicon.ico —— 用 png-to-ico 打包 16/32/48 三档
const pngToIcoMod = await import(
  pathToFileURL(join(ROOT, 'web/node_modules/png-to-ico/index.js')).href
);
const pngToIco = pngToIcoMod.default || pngToIcoMod;
const icoPngs = [16, 32, 48].map((s) => Buffer.from(render(SVG_FAVICON, s)));
const icoBuf = await pngToIco(icoPngs);
writeFileSync(join(webApp, 'favicon.ico'), icoBuf);
console.log('  →', 'web/app/favicon.ico');

console.log('\nDone.');
