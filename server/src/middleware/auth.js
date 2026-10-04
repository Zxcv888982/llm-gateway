import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import { db } from '../db.js';

export async function authRequired(req, res, next) {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' });
  try {
    const payload = jwt.verify(header.slice(7), config.jwtSecret);
    req.user = await db('users').select('id', 'username', 'role', 'balance').where('id', payload.uid).first();
    if (!req.user) return res.status(401).json({ error: 'User not found' });
    next();
  } catch {
    res.status(401).json({ error: 'Invalid token' });
  }
}

export function adminRequired(req, res, next) {
  if (req.user?.role !== 'admin') return res.status(403).json({ error: 'Admin only' });
  next();
}

// 支持用 API Key 直接调用 /v1/* 兼容接口
export async function apiKeyAuth(req, res, next) {
  const header = req.headers.authorization;
  if (header?.startsWith('Bearer ')) {
    const key = header.slice(7);
    // 先尝试 JWT
    try {
      const payload = jwt.verify(key, config.jwtSecret);
      req.user = await db('users').select('id', 'username', 'role', 'balance').where('id', payload.uid).first();
      if (req.user) return next();
    } catch { /* not jwt */ }
    // 再尝试 API Key
    const apiKey = await db('api_keys as k')
      .join('users as u', 'k.user_id', 'u.id')
      .select('k.id as key_id', 'k.user_id', 'u.username', 'u.role', 'u.balance')
      .where('k.key', key).where('k.status', 'active').first();
    if (apiKey) {
      req.user = { id: apiKey.user_id, username: apiKey.username, role: apiKey.role, balance: apiKey.balance };
      req.apiKeyId = apiKey.key_id;
      return next();
    }
  }
  res.status(401).json({ error: 'Unauthorized' });
}
