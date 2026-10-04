import knex from 'knex';
import { config } from './config.js';
import bcrypt from 'bcryptjs';
import fs from 'fs';
import path from 'path';

const isPG = !!config.databaseUrl;

export const db = knex({
  client: isPG ? 'pg' : 'better-sqlite3',
  connection: isPG ? config.databaseUrl : { filename: config.dbPath },
  useNullAsDefault: !isPG,
  pool: { min: 2, max: 20 },
});

const now = isPG ? db.raw('EXTRACT(EPOCH FROM NOW())::int') : db.raw("strftime('%s','now')");

export async function initDb() {
  if (!isPG) {
    const dir = path.dirname(config.dbPath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  }

  // users
  if (!await db.schema.hasTable('users')) {
    await db.schema.createTable('users', t => {
      t.increments('id').primary();
      t.string('username').unique().notNullable();
      t.string('email').unique();
      t.string('password_hash').notNullable();
      t.string('role').defaultTo('user');
      t.float('balance').defaultTo(0);
      t.integer('created_at').defaultTo(now);
    });
  }

  // api_keys
  if (!await db.schema.hasTable('api_keys')) {
    await db.schema.createTable('api_keys', t => {
      t.increments('id').primary();
      t.integer('user_id').unsigned().notNullable().references('id').inTable('users').onDelete('CASCADE');
      t.string('name').notNullable();
      t.string('key').unique().notNullable();
      t.string('status').defaultTo('active');
      t.bigInteger('usage_limit').defaultTo(0);
      t.bigInteger('used_tokens').defaultTo(0);
      t.integer('created_at').defaultTo(now);
    });
    await db.schema.raw('CREATE INDEX IF NOT EXISTS idx_api_keys_key ON api_keys(key)');
    await db.schema.raw('CREATE INDEX IF NOT EXISTS idx_api_keys_user ON api_keys(user_id)');
  }

  // channels
  if (!await db.schema.hasTable('channels')) {
    await db.schema.createTable('channels', t => {
      t.increments('id').primary();
      t.string('name').notNullable();
      t.string('provider').notNullable();
      t.string('base_url').notNullable();
      t.string('api_key').notNullable();
      t.text('models').notNullable();
      t.integer('weight').defaultTo(1);
      t.integer('priority').defaultTo(0);
      t.string('status').defaultTo('active');
      t.integer('fail_count').defaultTo(0);
      t.integer('last_used_at').defaultTo(0);
      t.integer('created_at').defaultTo(now);
    });
  }

  // requests
  if (!await db.schema.hasTable('requests')) {
    await db.schema.createTable('requests', t => {
      t.increments('id').primary();
      t.integer('user_id').unsigned();
      t.integer('api_key_id').unsigned();
      t.integer('channel_id').unsigned();
      t.string('model');
      t.integer('prompt_tokens').defaultTo(0);
      t.integer('completion_tokens').defaultTo(0);
      t.integer('total_tokens').defaultTo(0);
      t.float('cost').defaultTo(0);
      t.string('status');
      t.text('error');
      t.integer('latency_ms');
      t.integer('created_at').defaultTo(now);
    });
    await db.schema.raw('CREATE INDEX IF NOT EXISTS idx_requests_user ON requests(user_id, created_at)');
    await db.schema.raw('CREATE INDEX IF NOT EXISTS idx_requests_channel ON requests(channel_id, created_at)');
    await db.schema.raw('CREATE INDEX IF NOT EXISTS idx_requests_model ON requests(model, created_at)');
  }

  // fine_tunes
  if (!await db.schema.hasTable('fine_tunes')) {
    await db.schema.createTable('fine_tunes', t => {
      t.increments('id').primary();
      t.integer('user_id').unsigned().notNullable().references('id').inTable('users').onDelete('CASCADE');
      t.integer('channel_id').unsigned();
      t.string('model').notNullable();
      t.string('suffix');
      t.string('training_file');
      t.string('status').defaultTo('pending');
      t.string('fine_tuned_model');
      t.text('error');
      t.integer('created_at').defaultTo(now);
    });
  }

  // 初始化管理员
  const adminExists = await db('users').where('username', 'admin').first();
  if (!adminExists) {
    const hash = bcrypt.hashSync(config.adminPassword, 10);
    await db('users').insert({ username: 'admin', password_hash: hash, role: 'admin', balance: 999999 });
    console.log('[init] admin created, password:', config.adminPassword);
  }

  console.log(`[db] ${isPG ? 'PostgreSQL' : 'SQLite'} ready`);
}
