import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';
import { config } from './config.js';
import { initDb } from './db.js';

import authRoutes from './routes/auth.js';
import chatRoutes from './routes/chat.js';
import keyRoutes from './routes/keys.js';
import statsRoutes from './routes/stats.js';

const app = express();

// 安全 & 日志
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(morgan('combined'));

// 全局限流
const limiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.rateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later' }
});
app.use('/api/', limiter);

app.get('/health', (req, res) => res.json({ ok: true, ts: Date.now() }));

app.use('/api/auth', authRoutes);
app.use('/api', keyRoutes);
app.use('/api', statsRoutes);
app.use('/v1', chatRoutes);

app.use((err, req, res, next) => {
  console.error('[error]', err.message);
  res.status(500).json({ error: 'Internal server error' });
});

// 优雅关闭
process.on('SIGTERM', async () => {
  console.log('[shutdown] SIGTERM received, closing...');
  process.exit(0);
});

// 先初始化数据库，再启动服务
initDb().then(() => {
  app.listen(config.port, () => {
    console.log(`[server] LLM Gateway running on http://0.0.0.0:${config.port}`);
    console.log(`[server] Database: ${config.databaseUrl ? 'PostgreSQL' : 'SQLite'}`);
  });
}).catch(err => {
  console.error('[fatal] Database init failed:', err);
  process.exit(1);
});
