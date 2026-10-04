import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { db } from '../db.js';
import { config } from '../config.js';
import { authRequired } from '../middleware/auth.js';

const router = Router();

// 登录失败次数追踪（内存级，重启清零）
const loginAttempts = new Map(); // username -> { count, lockUntil }

router.post('/register', async (req, res) => {
  const { username, password, confirm_password, email } = req.body;

  // 必填校验
  if (!username || !password)
    return res.status(400).json({ error: 'Username and password are required', code: 'MISSING_FIELDS' });

  // 用户名格式：3-20 位字母数字下划线
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(username))
    return res.status(400).json({ error: 'Username must be 3-20 chars: letters, numbers, underscore', code: 'INVALID_USERNAME' });

  // 密码强度：至少 6 位，必须包含字母和数字
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,}$/.test(password))
    return res.status(400).json({ error: 'Password must be at least 6 chars and contain both letters and numbers', code: 'WEAK_PASSWORD' });

  // 确认密码
  if (confirm_password === undefined || password !== confirm_password)
    return res.status(400).json({ error: 'Passwords do not match', code: 'PASSWORD_MISMATCH' });

  // email 格式校验（如果提供了 email）
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
    return res.status(400).json({ error: 'Invalid email format', code: 'INVALID_EMAIL' });

  const exists = await db('users').where('username', username).first();
  if (exists) return res.status(409).json({ error: 'Username taken', code: 'USERNAME_TAKEN' });

  const hash = bcrypt.hashSync(password, 10);
  const [id] = await db('users').insert({ username, email: email || null, password_hash: hash }, 'id');
  const userId = typeof id === 'object' ? id.id : id;
  const token = jwt.sign({ uid: userId }, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
  res.json({ token, user: { id: userId, username, role: 'user', balance: 0 } });
});

router.post('/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password)
    return res.status(400).json({ error: 'Username and password are required', code: 'MISSING_FIELDS' });

  const now = Date.now();
  const record = loginAttempts.get(username);

  // 检查是否被锁定
  if (record && record.lockUntil && record.lockUntil > now) {
    return res.status(429).json({ error: 'Too many failed attempts, try again later', code: 'LOGIN_LOCKED' });
  }

  const user = await db('users').where('username', username).first();
  const valid = user && bcrypt.compareSync(password, user.password_hash);

  if (!valid) {
    // 失败计数
    const current = record && record.lockUntil && record.lockUntil <= now ? { count: 0 } : (record || { count: 0 });
    const newCount = current.count + 1;
    const lockUntil = newCount >= config.loginMaxAttempts ? now + config.loginLockoutMs : null;
    loginAttempts.set(username, { count: newCount, lockUntil });
    return res.status(401).json({ error: 'Invalid credentials', code: 'INVALID_CREDENTIALS' });
  }

  // 登录成功，清除失败记录
  loginAttempts.delete(username);

  const token = jwt.sign({ uid: user.id }, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
  res.json({ token, user: { id: user.id, username: user.username, role: user.role, balance: user.balance } });
});

router.get('/me', authRequired, (req, res) => {
  res.json({ user: req.user });
});

router.post('/change-password', authRequired, async (req, res) => {
  const { old_password, new_password } = req.body;
  if (!old_password || !new_password || new_password.length < 6)
    return res.status(400).json({ error: 'New password must be at least 6 characters', code: 'WEAK_PASSWORD' });
  const user = await db('users').where('id', req.user.id).first();
  if (!bcrypt.compareSync(old_password, user.password_hash))
    return res.status(401).json({ error: 'Current password is incorrect', code: 'INVALID_CREDENTIALS' });
  const hash = bcrypt.hashSync(new_password, 10);
  await db('users').where('id', req.user.id).update({ password_hash: hash });
  res.json({ ok: true });
});

export default router;
