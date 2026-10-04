import { Router } from 'express';
import { apiKeyAuth } from '../middleware/auth.js';
import { selectChannel, markChannelSuccess, markChannelFail, calculateCost, logRequest } from '../services/gateway.js';
import { db } from '../db.js';

const router = Router();

router.post('/chat/completions', apiKeyAuth, async (req, res) => {
  const { model, messages, stream = false, temperature, max_tokens, top_p } = req.body;
  if (!model || !messages) return res.status(400).json({ error: 'model and messages required' });

  const startTime = Date.now();
  const maxRetries = 3;
  let lastError = null;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    const channel = await selectChannel(model);
    if (!channel) {
      await logRequest({ userId: req.user.id, apiKeyId: req.apiKeyId, model, status: 'error', error: 'No available channel', latencyMs: Date.now() - startTime });
      return res.status(503).json({ error: 'No available channel for model: ' + model });
    }

    try {
      const url = channel.base_url.replace(/\/$/, '') + '/chat/completions';
      const body = { model, messages, stream, temperature, max_tokens, top_p };

      const upstream = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + channel.api_key },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(120000),
      });

      if (!upstream.ok) {
        const errText = await upstream.text();
        await markChannelFail(channel.id);
        lastError = `Upstream ${upstream.status}: ${errText.slice(0, 200)}`;
        continue;
      }

      await markChannelSuccess(channel.id);

      if (stream) {
        res.setHeader('Content-Type', 'text/event-stream');
        res.setHeader('Cache-Control', 'no-cache');
        res.setHeader('Connection', 'keep-alive');

        const reader = upstream.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let usage = null;

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop();
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6);
              if (data === '[DONE]') {
                res.write('data: [DONE]\n\n');
              } else {
                try {
                  const parsed = JSON.parse(data);
                  if (parsed.usage) usage = parsed.usage;
                } catch { /* ignore */ }
                res.write(line + '\n');
              }
            }
          }
        }
        res.end();

        const cost = calculateCost(model, usage?.prompt_tokens || 0, usage?.completion_tokens || 0);
        await logRequest({ userId: req.user.id, apiKeyId: req.apiKeyId, channelId: channel.id, model, usage, status: 'success', latencyMs: Date.now() - startTime, cost });
        return;
      } else {
        const result = await upstream.json();
        const usage = result.usage || { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 };
        const cost = calculateCost(model, usage.prompt_tokens, usage.completion_tokens);
        await logRequest({ userId: req.user.id, apiKeyId: req.apiKeyId, channelId: channel.id, model, usage, status: 'success', latencyMs: Date.now() - startTime, cost });
        return res.json(result);
      }
    } catch (err) {
      await markChannelFail(channel.id);
      lastError = err.message;
      continue;
    }
  }

  await logRequest({ userId: req.user.id, apiKeyId: req.apiKeyId, model, status: 'error', error: lastError, latencyMs: Date.now() - startTime });
  res.status(502).json({ error: 'All channels failed', detail: lastError });
});

router.get('/models', apiKeyAuth, async (req, res) => {
  const channels = await db('channels').where('status', 'active');
  const modelSet = new Set();
  for (const c of channels) {
    try { JSON.parse(c.models).forEach(m => modelSet.add(m)); } catch { /* ignore */ }
  }
  const data = [...modelSet].filter(m => m !== '*').map(id => ({ id, object: 'model', created: 0 }));
  res.json({ object: 'list', data });
});

export default router;
