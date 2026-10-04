import { Router } from 'express';
import { nanoid } from 'nanoid';
import { db } from '../db.js';
import { authRequired, adminRequired } from '../middleware/auth.js';

const router = Router();

router.get('/', authRequired, async (req, res) => {
  const keys = await db('api_keys').where('user_id', req.user.id).orderBy('created_at', 'desc');
  res.json({ keys });
});

router.post('/', authRequired, async (req, res) => {
  const { name, usage_limit = 0 } = req.body;
  if (!name) return res.status(400).json({ error: 'Name required' });
  const key = 'sk-' + nanoid(32);
  const [id] = await db('api_keys').insert({ user_id: req.user.id, name, key, usage_limit }, 'id');
  const keyId = typeof id === 'object' ? id.id : id;
  res.json({ id: keyId, name, key, status: 'active', used_tokens: 0 });
});

router.delete('/:id', authRequired, async (req, res) => {
  const deleted = await db('api_keys').where('id', req.params.id).where('user_id', req.user.id).del();
  if (deleted === 0) return res.status(404).json({ error: 'Not found' });
  res.json({ ok: true });
});

// 渠道管理（管理员）
router.get('/channels', authRequired, adminRequired, async (req, res) => {
  const channels = await db('channels').orderBy('priority', 'desc').orderBy('id', 'asc');
  res.json({ channels });
});

router.post('/channels', authRequired, adminRequired, async (req, res) => {
  const { name, provider, base_url, api_key, models, weight = 1, priority = 0 } = req.body;
  if (!name || !provider || !base_url || !api_key || !models)
    return res.status(400).json({ error: 'Missing required fields' });
  const [id] = await db('channels').insert({ name, provider, base_url, api_key, models: JSON.stringify(models), weight, priority }, 'id');
  const chId = typeof id === 'object' ? id.id : id;
  res.json({ id: chId, name, status: 'active' });
});

router.put('/channels/:id', authRequired, adminRequired, async (req, res) => {
  const { name, base_url, api_key, models, weight, priority, status } = req.body;
  const updates = {};
  if (name !== undefined) updates.name = name;
  if (base_url !== undefined) updates.base_url = base_url;
  if (api_key !== undefined) updates.api_key = api_key;
  if (models !== undefined) updates.models = JSON.stringify(models);
  if (weight !== undefined) updates.weight = weight;
  if (priority !== undefined) updates.priority = priority;
  if (status !== undefined) { updates.status = status; if (status === 'active') updates.fail_count = 0; }
  await db('channels').where('id', req.params.id).update(updates);
  res.json({ ok: true });
});

router.delete('/channels/:id', authRequired, adminRequired, async (req, res) => {
  await db('channels').where('id', req.params.id).del();
  res.json({ ok: true });
});

export default router;
