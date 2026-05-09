#!/usr/bin/env node

const DEFAULT_BASE_URL = 'http://localhost:8083/api/v1';
const BASE_URL = (process.env.API_BASE_URL || DEFAULT_BASE_URL).replace(/\/$/, '');
const CONTINUE_ON_FAILURE = /^(1|true|yes)$/i.test(process.env.SMOKE_CONTINUE || '');
const PASSWORD = process.env.SMOKE_PASSWORD || 'SmokePass123';
const NEXT_PASSWORD = process.env.SMOKE_NEXT_PASSWORD || 'SmokePass124';
const RUN_ID = process.env.SMOKE_RUN_ID || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const state = {
  users: {},
  privateChat: null,
  privateMessage: null,
  group: null,
  groupMessage: null,
  moment: null,
  privateMoment: null,
  comment: null,
  stats: { passed: 0, failed: 0, skipped: 0 },
};

function print(line = '') {
  console.log(line);
}

function json(value) {
  return JSON.stringify(value, null, 2);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function pageList(page) {
  if (!page) return [];
  if (Array.isArray(page)) return page;
  return Array.isArray(page.list) ? page.list : [];
}

function query(params = {}) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : '';
}

async function api(method, path, options = {}) {
  const { token, body, params, raw = false, allowCodes = [0] } = options;
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}${query(params)}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const text = await res.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!res.ok) {
    throw new Error(`${method} ${path} -> HTTP ${res.status}: ${typeof payload === 'string' ? payload : json(payload)}`);
  }
  if (raw) return payload;

  if (!payload || typeof payload !== 'object' || !('code' in payload)) {
    throw new Error(`${method} ${path} returned unexpected payload: ${json(payload)}`);
  }
  if (!allowCodes.includes(payload.code)) {
    throw new Error(`${method} ${path} -> code ${payload.code}: ${payload.message || 'unknown error'}`);
  }
  return payload.data;
}

function client(user) {
  return {
    get: (path, options = {}) => api('GET', path, { ...options, token: user.accessToken }),
    post: (path, body, options = {}) => api('POST', path, { ...options, body, token: user.accessToken }),
    put: (path, body, options = {}) => api('PUT', path, { ...options, body, token: user.accessToken }),
    delete: (path, options = {}) => api('DELETE', path, { ...options, token: user.accessToken }),
  };
}

async function step(name, fn) {
  const started = Date.now();
  try {
    await fn();
    state.stats.passed += 1;
    print(`PASS ${name} (${Date.now() - started}ms)`);
  } catch (error) {
    state.stats.failed += 1;
    print(`FAIL ${name}`);
    print(`     ${error.message}`);
    if (!CONTINUE_ON_FAILURE) {
      throw error;
    }
  }
}

async function optionalStep(name, fn) {
  try {
    await fn();
    state.stats.passed += 1;
    print(`PASS ${name}`);
  } catch (error) {
    state.stats.skipped += 1;
    print(`SKIP ${name}`);
    print(`     ${error.message}`);
  }
}

async function negativeStep(name, fn) {
  let succeeded = false;
  try {
    await fn();
    succeeded = true;
  } catch (error) {
    state.stats.passed += 1;
    print(`PASS ${name}`);
    return;
  }

  if (succeeded) {
    state.stats.failed += 1;
    print(`FAIL ${name}`);
    print('     request unexpectedly succeeded');
    if (!CONTINUE_ON_FAILURE) {
      throw new Error(`${name}: request unexpectedly succeeded`);
    }
  }
}

async function registerUser(alias) {
  const email = `smoke.${RUN_ID}.${alias}@example.com`;
  const captcha = await api('POST', '/auth/email-captcha', {
    body: { email, scene: 'register' },
  });
  const auth = await api('POST', '/auth/register', {
    body: { email, password: PASSWORD, captcha },
  });
  assert(auth.accessToken, `${alias} register did not return accessToken`);
  return { alias, email, password: PASSWORD, ...auth };
}

async function login(email, password) {
  const auth = await api('POST', '/auth/login', { body: { email, password } });
  assert(auth.accessToken, `login for ${email} did not return accessToken`);
  return auth;
}

async function main() {
  print(`Hello Chat broad smoke test`);
  print(`API_BASE_URL=${BASE_URL}`);
  print(`RUN_ID=${RUN_ID}`);
  print('');

  await step('health check', async () => {
    const health = await api('GET', '/auth/health');
    assert(health === 'ok', `expected health ok, got ${json(health)}`);
  });

  await step('register three isolated users', async () => {
    state.users.alice = await registerUser('alice');
    state.users.bob = await registerUser('bob');
    state.users.carol = await registerUser('carol');
    assert(state.users.alice.userId !== state.users.bob.userId, 'users should be unique');
  });

  await step('login, refresh token, and profile update', async () => {
    const aliceLogin = await login(state.users.alice.email, PASSWORD);
    state.users.alice.accessToken = aliceLogin.accessToken;
    state.users.alice.refreshToken = aliceLogin.refreshToken;

    const refreshed = await api('POST', '/auth/refresh', {
      body: { refreshToken: state.users.alice.refreshToken },
    });
    assert(refreshed.accessToken, 'refresh did not return accessToken');
    state.users.alice.accessToken = refreshed.accessToken;
    state.users.alice.refreshToken = refreshed.refreshToken;

    const alice = client(state.users.alice);
    const me = await alice.put('/users/me', {
      nickname: `Smoke Alice ${RUN_ID}`,
      signature: 'Automated smoke profile',
      gender: 2,
      age: 20,
      phone: `13${String(Date.now()).slice(-9)}`,
    });
    assert(me.nickname.includes('Smoke Alice'), 'profile nickname was not updated');
  });

  await step('user search and profile lookup', async () => {
    const alice = client(state.users.alice);
    const found = await alice.get('/users/search', { params: { keyword: state.users.bob.email, page: 1, pageSize: 10 } });
    assert(pageList(found).some((item) => item.userId === state.users.bob.userId), 'bob was not found by user search');

    const profile = await alice.get(`/users/${state.users.bob.userId}/profile`);
    assert(profile.userId === state.users.bob.userId, 'profile lookup returned the wrong user');
  });

  await step('friend request approve flow for two friends', async () => {
    const alice = client(state.users.alice);
    const bob = client(state.users.bob);
    const carol = client(state.users.carol);

    const bobRequest = await alice.post('/users/friend-requests', {
      receiverId: state.users.bob.userId,
      remark: 'smoke friend bob',
    });
    const carolRequest = await alice.post('/users/friend-requests', {
      receiverId: state.users.carol.userId,
      remark: 'smoke friend carol',
    });

    const receivedByBob = await bob.get('/users/friend-requests/received');
    assert(receivedByBob.some((item) => item.requestId === bobRequest.requestId), 'bob did not receive friend request');

    await bob.post(`/users/friend-requests/${bobRequest.requestId}/approve`, {});
    await carol.post(`/users/friend-requests/${carolRequest.requestId}/approve`, {});

    const friends = await alice.get('/users/friends');
    assert(friends.some((item) => item.userId === state.users.bob.userId), 'bob is not in friend list');
    assert(friends.some((item) => item.userId === state.users.carol.userId), 'carol is not in friend list');

    const updated = await alice.put(`/users/friends/${state.users.bob.userId}`, {
      remarkName: 'Smoke Bob',
      friendGroup: 'Smoke Group',
      star: true,
    });
    assert(updated.star === 1 || updated.star === true, 'friend star flag was not updated');
  });

  await step('block and unblock generated user', async () => {
    const bob = client(state.users.bob);
    await bob.post(`/users/blocks/${state.users.carol.userId}`, {});
    const blocked = await bob.get('/users/blocks');
    assert(blocked.some((item) => item.userId === state.users.carol.userId), 'carol is not in bob blocked list');
    await bob.delete(`/users/blocks/${state.users.carol.userId}`);
  });

  await step('phase 5 social presence, search, history, recommendations', async () => {
    const alice = client(state.users.alice);
    await alice.put('/social/presence', { status: 'online' });
    const presence = await alice.get(`/social/presence/${state.users.alice.userId}`);
    assert(presence.status === 'online', 'presence status was not online');

    const search = await alice.get('/social/search', { params: { keyword: 'Smoke', page: 1, pageSize: 10 } });
    assert(search, 'social search returned empty payload');

    const history = await alice.get('/social/search/history');
    assert(Array.isArray(history), 'search history should be a list');

    const recommendations = await alice.get('/social/recommendations');
    assert(recommendations, 'recommendations returned empty payload');
  });

  await step('private chat send, search, pin, read, typing, recall, delete', async () => {
    const alice = client(state.users.alice);
    const bob = client(state.users.bob);
    state.privateChat = await alice.post('/chats/private', { targetUserId: state.users.bob.userId });
    assert(state.privateChat.chatId, 'private chat did not return chatId');

    state.privateMessage = await alice.post(`/chats/${state.privateChat.chatId}/messages`, {
      messageType: 'text',
      content: `private smoke keyword ${RUN_ID}`,
    });
    assert(state.privateMessage.messageId, 'private message did not return messageId');

    const messages = await bob.get(`/chats/${state.privateChat.chatId}/messages`, { params: { page: 1, pageSize: 20 } });
    assert(pageList(messages).some((item) => item.messageId === state.privateMessage.messageId), 'bob could not list private message');

    const searched = await bob.get('/messages/search', {
      params: { chatId: state.privateChat.chatId, keyword: RUN_ID, page: 1, pageSize: 20 },
    });
    assert(pageList(searched).some((item) => item.messageId === state.privateMessage.messageId), 'private search missed the smoke message');

    await alice.post(`/messages/${state.privateMessage.messageId}/pin`, {});
    await alice.delete(`/messages/${state.privateMessage.messageId}/pin`);
    await bob.post(`/chats/${state.privateChat.chatId}/read`, {});
    await alice.post(`/chats/${state.privateChat.chatId}/typing`, { typing: true });
    await alice.post(`/messages/${state.privateMessage.messageId}/recall`, {});

    const disposable = await alice.post(`/chats/${state.privateChat.chatId}/messages`, {
      messageType: 'text',
      content: `private disposable ${RUN_ID}`,
    });
    await alice.delete(`/messages/${disposable.messageId}`);
  });

  await step('group create, update, members, notice, messages, mention all', async () => {
    const alice = client(state.users.alice);
    const bob = client(state.users.bob);
    state.group = await alice.post('/groups', {
      groupName: `Smoke Group ${String(RUN_ID).slice(0, 8)}`,
      description: 'Group created by smoke test',
      memberIds: [state.users.bob.userId, state.users.carol.userId],
    });
    assert(state.group.groupId, 'group did not return groupId');

    const groupId = state.group.groupId;
    await alice.put(`/groups/${groupId}`, {
      groupName: `Smoke G ${String(RUN_ID).slice(0, 8)}`,
      description: 'Group updated by smoke test',
      chatEnabled: true,
      recallLimitMinutes: 10,
    });

    const myGroups = await bob.get('/groups');
    assert(myGroups.some((item) => item.groupId === groupId), 'bob cannot see smoke group');

    const members = await alice.get(`/groups/${groupId}/members`);
    assert(members.length >= 3, 'group should have at least three members');

    await bob.put(`/groups/${groupId}/members/me/nickname`, { nickname: 'Smoke Bob In Group' });
    await alice.post(`/groups/${groupId}/announcement`, { notice: `notice ${RUN_ID}` });
    await bob.get(`/groups/${groupId}/notifications`, { params: { page: 1, pageSize: 10 } });
    await bob.post(`/groups/${groupId}/announcement/read`, {});
    await alice.get(`/groups/${groupId}/announcement/read-stats`);

    state.groupMessage = await alice.post(`/groups/${groupId}/messages`, {
      messageType: 'text',
      content: `group smoke keyword ${RUN_ID}`,
      mentionUserIds: [state.users.bob.userId],
    });
    assert(state.groupMessage.messageId, 'group message did not return messageId');

    const searched = await bob.get(`/groups/${groupId}/messages/search`, {
      params: { keyword: RUN_ID, page: 1, pageSize: 20 },
    });
    assert(pageList(searched).some((item) => item.messageId === state.groupMessage.messageId), 'group search missed the smoke message');

    await bob.get(`/groups/${groupId}/messages`, { params: { page: 1, pageSize: 20 } });
    await bob.post(`/groups/${groupId}/read`, {});
    await alice.post(`/groups/${groupId}/messages/mention-all`, {
      messageType: 'text',
      content: `all hands ${RUN_ID}`,
    });

    const disposable = await alice.post(`/groups/${groupId}/messages`, {
      messageType: 'text',
      content: `group disposable ${RUN_ID}`,
    });
    await alice.post(`/groups/${groupId}/messages/${disposable.messageId}/recall`, {});
  });

  await step('moments create, timeline, visibility, reactions, comments, notifications', async () => {
    const alice = client(state.users.alice);
    const bob = client(state.users.bob);
    const carol = client(state.users.carol);

    state.moment = await alice.post('/moments', {
      content: `public moment smoke keyword ${RUN_ID}`,
      location: 'Smoke City',
      tags: ['smoke', 'e2e'],
      mood: 'focused',
      activity: 'testing',
      visibility: 'public',
    });
    assert(state.moment.momentId, 'moment did not return momentId');

    state.privateMoment = await alice.post('/moments', {
      content: `specified moment ${RUN_ID}`,
      visibility: 'specified',
      visibleUserIds: [state.users.bob.userId],
    });
    assert(state.privateMoment.visibleUserIds.includes(state.users.bob.userId), 'specified moment missing visible user');

    const timeline = await bob.get('/moments', { params: { page: 1, pageSize: 20 } });
    assert(pageList(timeline).some((item) => item.momentId === state.moment.momentId), 'timeline missed public moment');

    const bobPrivateView = await bob.get(`/moments/${state.privateMoment.momentId}`);
    assert(bobPrivateView.momentId === state.privateMoment.momentId, 'bob could not view specified moment');

    await negativeStep('specified moment should be hidden from non-visible user', async () => {
      await carol.get(`/moments/${state.privateMoment.momentId}`);
    });

    await bob.post(`/moments/${state.moment.momentId}/likes`, {});
    await bob.get(`/moments/${state.moment.momentId}/likes`);
    await bob.post(`/moments/${state.moment.momentId}/collect`, {});
    await bob.get('/moments/collections', { params: { page: 1, pageSize: 20 } });

    state.comment = await bob.post(`/moments/${state.moment.momentId}/comments`, {
      content: `comment smoke ${RUN_ID}`,
      mentionUserIds: [state.users.alice.userId],
    });
    assert(state.comment.commentId, 'comment did not return commentId');

    const reply = await alice.post(`/moments/${state.moment.momentId}/comments`, {
      content: `reply smoke ${RUN_ID}`,
      replyToCommentId: state.comment.commentId,
      mentionUserIds: [state.users.bob.userId],
    });
    assert(reply.commentId, 'reply did not return commentId');

    const comments = await alice.get(`/moments/${state.moment.momentId}/comments`);
    assert(comments.some((item) => item.commentId === state.comment.commentId), 'comment list missed smoke comment');

    await bob.delete(`/moments/${state.moment.momentId}/likes`);
    await bob.delete(`/moments/${state.moment.momentId}/collect`);
    await carol.post(`/moments/${state.moment.momentId}/report`, { reason: `smoke report ${RUN_ID}` });

    await alice.get(`/moments/users/${state.users.alice.userId}`, { params: { page: 1, pageSize: 20 } });
    await alice.get(`/moments/users/${state.users.alice.userId}/summary`);

    const unread = await alice.get('/moments/notifications/unread-count');
    assert(typeof unread === 'number', 'unread notification count should be a number');
    const notifications = await alice.get('/moments/notifications', { params: { page: 1, pageSize: 20 } });
    const firstNotification = pageList(notifications)[0];
    if (firstNotification?.notificationId) {
      await alice.put(`/moments/notifications/${firstNotification.notificationId}/read`, {});
    }
    await alice.put('/moments/notifications/read-all', {});

    const updated = await alice.put(`/moments/${state.moment.momentId}`, {
      content: `updated public moment smoke keyword ${RUN_ID}`,
      location: 'Smoke Updated City',
      tags: ['smoke', 'updated'],
      mood: 'done',
      activity: 'verification',
      visibility: 'public',
    });
    assert(updated.content.includes('updated public moment'), 'moment was not updated');

    await alice.delete(`/moments/${state.privateMoment.momentId}`);
  });

  await step('password change, password reset, and logout', async () => {
    const carol = client(state.users.carol);
    await carol.put('/auth/password', { oldPassword: PASSWORD, newPassword: NEXT_PASSWORD });
    const carolLogin = await login(state.users.carol.email, NEXT_PASSWORD);
    state.users.carol.accessToken = carolLogin.accessToken;
    state.users.carol.refreshToken = carolLogin.refreshToken;

    const captcha = await api('POST', '/auth/password-reset-captcha', {
      body: { email: state.users.carol.email, scene: 'reset_password' },
    });
    await api('POST', '/auth/password-reset', {
      body: { email: state.users.carol.email, captcha, newPassword: PASSWORD },
    });
    const resetLogin = await login(state.users.carol.email, PASSWORD);
    state.users.carol.refreshToken = resetLogin.refreshToken;

    await api('POST', '/auth/logout', { body: { refreshToken: state.users.alice.refreshToken } });
    await api('POST', '/auth/logout', { body: { refreshToken: state.users.bob.refreshToken } });
    await api('POST', '/auth/logout', { body: { refreshToken: state.users.carol.refreshToken } });
  });

  print('');
  print(`Summary: ${state.stats.passed} passed, ${state.stats.failed} failed, ${state.stats.skipped} skipped`);
  if (state.stats.failed > 0) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  print('');
  print('Smoke test stopped. Start the backend and database first, then rerun:');
  print(`  API_BASE_URL=${BASE_URL} node scripts/e2e-smoke.mjs`);
  print('');
  print(error.stack || error.message);
  process.exit(1);
});
