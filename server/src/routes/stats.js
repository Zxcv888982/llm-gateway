import { Router } from 'express';
import { db } from '../db.js';
import { authRequired, adminRequired } from '../middleware/auth.js';

const router = Router();

router.get('/usage', authRequired, async (req, res) => {
  const { range = 'today' } = req.query;
  const now = Math.floor(Date.now() / 1000);
  let since;
  if (range === 'today') since = now - 86400;
  else if (range === 'week') since = now - 7 * 86400;
  else if (range === 'month') since = now - 30 * 86400;
  else since = 0;

  const isAdmin = req.user.role === 'admin';
  const baseQuery = db('requests').where('created_at', '>=', since).where('status', 'success');
  if (!isAdmin) baseQuery.where('user_id', req.user.id);

  const stats = await baseQuery.clone().first(
    db.raw('COUNT(*) as requests, COALESCE(SUM(prompt_tokens),0) as prompt_tokens, COALESCE(SUM(completion_tokens),0) as completion_tokens, COALESCE(SUM(total_tokens),0) as total_tokens, COALESCE(SUM(cost),0) as cost')
  );

  const modelQuery = db('requests').where('created_at', '>=', since).where('status', 'success');
  if (!isAdmin) modelQuery.where('user_id', req.user.id);
  const byModel = await modelQuery.clone()
    .select('model')
    .count('* as requests')
    .sum('total_tokens as tokens')
    .sum('cost as cost')
    .groupBy('model')
    .orderBy('tokens', 'desc')
    .limit(10);

  res.json({ stats: stats || { requests: 0, prompt_tokens: 0, completion_tokens: 0, total_tokens: 0, cost: 0 }, by_model: byModel });
});

router.get('/logs', authRequired, async (req, res) => {
  const { limit = 50, offset = 0, model, status } = req.query;
  const isAdmin = req.user.role === 'admin';
  const query = db('requests').select('id', 'model', 'prompt_tokens', 'completion_tokens', 'total_tokens', 'cost', 'status', 'error', 'latency_ms', 'created_at');
  if (!isAdmin) query.where('user_id', req.user.id);
  if (model) query.where('model', model);
  if (status) query.where('status', status);

  const logs = await query.clone().orderBy('created_at', 'desc').limit(Number(limit)).offset(Number(offset));
  const countQuery = db('requests');
  if (!isAdmin) countQuery.where('user_id', req.user.id);
  if (model) countQuery.where('model', model);
  if (status) countQuery.where('status', status);
  const totalResult = await countQuery.clone().count('* as c').first();
  res.json({ logs, total: Number(totalResult.c) });
});

router.get('/fine-tunes', authRequired, async (req, res) => {
  const query = db('fine_tunes');
  if (req.user.role !== 'admin') query.where('user_id', req.user.id);
  const tasks = await query.orderBy('created_at', 'desc');
  res.json({ tasks });
});

router.post('/fine-tunes', authRequired, async (req, res) => {
  const { model, suffix, training_file, channel_id } = req.body;
  if (!model || !training_file) return res.status(400).json({ error: 'model and training_file required' });
  const [id] = await db('fine_tunes').insert({ user_id: req.user.id, channel_id: channel_id || null, model, suffix: suffix || null, training_file }, 'id');
  const ftId = typeof id === 'object' ? id.id : id;
  res.json({ id: ftId, status: 'pending' });
});

// 模型大全
const MODEL_CATALOG = [
  { id: 'gpt-4o', name: 'GPT-4o', provider: 'OpenAI', category: 'chat', context: '128K', description: '多模态旗舰，强推理' },
  { id: 'gpt-4o-mini', name: 'GPT-4o Mini', provider: 'OpenAI', category: 'chat', context: '128K', description: '轻量高速，低成本' },
  { id: 'gpt-4-turbo', name: 'GPT-4 Turbo', provider: 'OpenAI', category: 'chat', context: '128K', description: '长上下文推理' },
  { id: 'claude-3-5-sonnet', name: 'Claude 3.5 Sonnet', provider: 'Anthropic', category: 'chat', context: '200K', description: '均衡型，代码强' },
  { id: 'claude-3-haiku', name: 'Claude 3 Haiku', provider: 'Anthropic', category: 'chat', context: '200K', description: '极速轻量' },
  { id: 'deepseek-chat', name: 'DeepSeek V3', provider: 'DeepSeek', category: 'chat', context: '64K', description: '国产开源，高性价比' },
  { id: 'deepseek-coder', name: 'DeepSeek Coder', provider: 'DeepSeek', category: 'code', context: '128K', description: '代码专用' },
  { id: 'qwen-turbo', name: 'Qwen Turbo', provider: '通义千问', category: 'chat', context: '1M', description: '超长上下文，低成本' },
  { id: 'qwen-plus', name: 'Qwen Plus', provider: '通义千问', category: 'chat', context: '128K', description: '增强版通用模型' },
  { id: 'qwen-max', name: 'Qwen Max', provider: '通义千问', category: 'chat', context: '32K', description: '旗舰推理' },
  { id: 'glm-4', name: 'GLM-4', provider: '智谱AI', category: 'chat', context: '128K', description: '国产通用大模型' },
  { id: 'glm-4-flash', name: 'GLM-4 Flash', provider: '智谱AI', category: 'chat', context: '128K', description: '免费高速' },
  { id: 'moonshot-v1-8k', name: 'Moonshot V1', provider: '月之暗面', category: 'chat', context: '8K', description: '长文本理解' },
  { id: 'moonshot-v1-128k', name: 'Moonshot V1 128K', provider: '月之暗面', category: 'chat', context: '128K', description: '超长上下文' },
  { id: 'yi-large', name: 'Yi Large', provider: '零一万物', category: 'chat', context: '32K', description: '中文优化' },
  { id: 'doubao-pro', name: 'Doubao Pro', provider: '字节跳动', category: 'chat', context: '32K', description: '豆包专业版' },
  { id: 'doubao-lite', name: 'Doubao Lite', provider: '字节跳动', category: 'chat', context: '32K', description: '豆包轻量版' },
  { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro', provider: 'Google', category: 'chat', context: '1M', description: '百万上下文' },
  { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash', provider: 'Google', category: 'chat', context: '1M', description: '高速低成本' },
  { id: 'llama-3.1-70b', name: 'Llama 3.1 70B', provider: 'Meta', category: 'chat', context: '128K', description: '开源旗舰' },
];

router.get('/catalog', (req, res) => {
  const { category, q } = req.query;
  let list = MODEL_CATALOG;
  if (category && category !== 'all') list = list.filter(m => m.category === category);
  if (q) list = list.filter(m => m.name.toLowerCase().includes(q.toLowerCase()) || m.provider.toLowerCase().includes(q.toLowerCase()));
  res.json({ models: list });
});

export default router;
