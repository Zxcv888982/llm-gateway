import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { db } from '../db.js';
import { config } from '../config.js';
import { authRequired } from '../middleware/auth.js';

const router = Router();

router.post('/register', async (req, res) => {
  const { username, password, email } = req.body;
  if (!username || !password || password.length < 6)
    return res.status(400).json({ error: 'Username and password (min 6 chars) required' });
  const exists = await db('users').where('username', username).first();
  if (exists) return res.status(409).json({ error: 'Username taken' });
  const hash = bcrypt.hashSync(password, 10);
  const [id] = await db('users').insert({ username, email: email || null, password_hash: hash }, 'id');
  const userId = typeof id === 'object' ? id.id : id;
  const token = jwt.sign({ uid: userId }, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
  res.json({ token, user: { id: userId, username, role: 'user', balance: 0 } });
});

router.post('/login', async (req, res) => {
  const { username, password } = req.body;
  const user = await db('users').where('username', username).first();
  if (!user || !bcrypt.compareSync(password, user.password_hash))
    return res.status(401).json({ error: 'Invalid credentials' });
  const token = jwt.sign({ uid: user.id }, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
  res.json({ token, user: { id: user.id, username: user.username, role: user.role, balance: user.balance } });
});

router.get('/me', authRequired, (req, res) => {
  res.json({ user: req.user });
});

router.post('/change-password', authRequired, async (req, res) => {
  const { old_password, new_password } = req.body;
  if (!old_password || !new_password || new_password.length < 6)
    return res.status(400).json({ error: 'New password must be at least 6 characters' });
  const user = await db('users').where('id', req.user.id).first();
  if (!bcrypt.compareSync(old_password, user.password_hash))
    return res.status(401).json({ error: 'Current password is incorrect' });
  const hash = bcrypt.hashSync(new_password, 10);
  await db('users').where('id', req.user.id).update({ password_hash: hash });
  res.json({ ok: true });
});

export default router;
