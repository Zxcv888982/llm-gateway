import { db } from '../db.js';

// 加权轮询 + 优先级 + 故障自动跳过
export async function selectChannel(model) {
  const channels = await db('channels')
    .where('status', 'active').where('fail_count', '<', 5)
    .orderBy('priority', 'desc').orderBy('weight', 'desc');

  if (channels.length === 0) return null;

  const matched = channels.filter(c => {
    try {
      const models = JSON.parse(c.models);
      return models.includes(model) || models.includes('*');
    } catch { return false; }
  });

  if (matched.length === 0) return null;

  const totalWeight = matched.reduce((s, c) => s + c.weight, 0);
  let r = Math.random() * totalWeight;
  for (const c of matched) {
    r -= c.weight;
    if (r <= 0) return c;
  }
  return matched[0];
}

export async function markChannelSuccess(channelId) {
  await db('channels').where('id', channelId).update({ fail_count: 0, last_used_at: Math.floor(Date.now() / 1000) });
}

export async function markChannelFail(channelId) {
  await db('channels').where('id', channelId).update({ fail_count: db.raw('fail_count + 1'), last_used_at: Math.floor(Date.now() / 1000) });
}

const PRICING = {
  'gpt-4o': { input: 0.0025, output: 0.01 },
  'gpt-4o-mini': { input: 0.00015, output: 0.0006 },
  'gpt-4-turbo': { input: 0.01, output: 0.03 },
  'claude-3-5-sonnet': { input: 0.003, output: 0.015 },
  'claude-3-haiku': { input: 0.00025, output: 0.00125 },
  'deepseek-chat': { input: 0.00014, output: 0.00028 },
  'qwen-turbo': { input: 0.00015, output: 0.00045 },
  'qwen-plus': { input: 0.0008, output: 0.002 },
  'glm-4': { input: 0.001, output: 0.002 },
  'moonshot-v1-8k': { input: 0.0012, output: 0.0012 },
};

export function calculateCost(model, promptTokens, completionTokens) {
  const price = PRICING[model] || { input: 0.001, output: 0.002 };
  return (promptTokens / 1000) * price.input + (completionTokens / 1000) * price.output;
}

export async function logRequest({ userId, apiKeyId, channelId, model, usage, status, error, latencyMs, cost }) {
  await db('requests').insert({
    user_id: userId, api_key_id: apiKeyId || null, channel_id: channelId || null,
    model, prompt_tokens: usage?.prompt_tokens || 0, completion_tokens: usage?.completion_tokens || 0,
    total_tokens: usage?.total_tokens || 0, cost: cost || 0, status, error: error || null, latency_ms: latencyMs || 0
  });
  if (apiKeyId && usage?.total_tokens) {
    await db('api_keys').where('id', apiKeyId).update({ used_tokens: db.raw('used_tokens + ?', usage.total_tokens) });
  }
}
