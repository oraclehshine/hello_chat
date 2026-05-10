#!/usr/bin/env node

const DEFAULT_BASE_URL = 'http://localhost:8083/api/v1';
const BASE_URL = (process.env.API_BASE_URL || DEFAULT_BASE_URL).replace(/\/$/, '');
const PASSWORD = process.env.MODULE_PASSWORD || 'ModulePass123';
const NEXT_PASSWORD = process.env.MODULE_NEXT_PASSWORD || 'ModulePass124';
const RUN_ID = process.env.MODULE_RUN_ID || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const CONTINUE = /^(1|true|yes)$/i.test(process.env.MODULE_CONTINUE || '');

const state = {
  users: {},
  chats: {},
  groups: {},
  moments: {},
  stats: { modules: 0, cases: 0, passed: 0, failed: 0 },
};

function log(line = '') {
  console.log(line);
}

function pretty(value) {
  return JSON.stringify(value, null, 2);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function list(pageOrList) {
  if (!pageOrList) return [];
  if (Array.isArray(pageOrList)) return pageOrList;
  return Array.isArray(pageOrList.list) ? pageOrList.list : [];
}

function query(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

async function request(method, path, options = {}) {
  const { token, body, params, headers = {}, unwrap = true } = options;
  const finalHeaders = { Accept: 'application/json', ...headers };
  let finalBody = body;
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = 'application/json';
    finalBody = JSON.stringify(body);
  }
  if (token) finalHeaders.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}${query(params)}`, {
    method,
    headers: finalHeaders,
    body: finalBody,
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
  if (!unwrap) return { status: res.status, ok: res.ok, payload };
  if (!res.ok || !payload || payload.code !== 0) {
    throw new Error(`${method} ${path} -> HTTP ${res.status}: ${typeof payload === 'string' ? payload : pretty(payload)}`);
  }
  return payload.data;
}

async function expectError(method, path, options = {}) {
  const response = await request(method, path, { ...options, unwrap: false });
  if (response.ok && response.payload?.code === 0) {
    throw new Error(`${method} ${path} unexpectedly succeeded`);
  }
  assert(response.status >= 400 || response.payload?.code !== 0, `${method} ${path} did not return an error`);
  return response.payload;
}

function api(user) {
  return {
    get: (path, options = {}) => request('GET', path, { ...options, token: user.accessToken }),
    post: (path, body, options = {}) => request('POST', path, { ...options, body, token: user.accessToken }),
    put: (path, body, options = {}) => request('PUT', path, { ...options, body, token: user.accessToken }),
    delete: (path, options = {}) => request('DELETE', path, { ...options, token: user.accessToken }),
    expectError: (method, path, options = {}) => expectError(method, path, { ...options, token: user.accessToken }),
  };
}

async function testCase(name, fn) {
  state.stats.cases += 1;
  const started = Date.now();
  try {
    await fn();
    state.stats.passed += 1;
    log(`  PASS ${name} (${Date.now() - started}ms)`);
  } catch (error) {
    state.stats.failed += 1;
    log(`  FAIL ${name}`);
    log(`       ${error.message}`);
    if (!CONTINUE) throw error;
  }
}

async function module(name, fn) {
  state.stats.modules += 1;
  log('');
  log(`MODULE ${name}`);
  await fn();
}

async function register(alias) {
  const email = `module.${RUN_ID}.${alias}@example.com`;
  const captcha = await request('POST', '/auth/email-captcha', { body: { email, scene: 'register' } });
  const auth = await request('POST', '/auth/register', { body: { email, password: PASSWORD, captcha } });
  assert(auth.accessToken && auth.refreshToken && auth.userId, `${alias} register auth payload is incomplete`);
  return { alias, email, password: PASSWORD, ...auth };
}

async function login(email, password) {
  const auth = await request('POST', '/auth/login', { body: { email, password } });
  assert(auth.accessToken && auth.refreshToken, `login payload is incomplete for ${email}`);
  return auth;
}

async function makeFriends(requester, receiver) {
  const req = await api(requester).post('/users/friend-requests', {
    receiverId: receiver.userId,
    remark: `friend ${requester.alias} -> ${receiver.alias}`,
  });
  await api(receiver).post(`/users/friend-requests/${req.requestId}/approve`, {});
  return req;
}

async function authModule() {
  await testCase('health endpoint', async () => {
    const health = await request('GET', '/auth/health');
    assert(health === 'ok', 'health should be ok');
  });

  await testCase('register five isolated users with captcha', async () => {
    for (const alias of ['alice', 'bob', 'carol', 'dave', 'erin']) {
      state.users[alias] = await register(alias);
    }
    assert(new Set(Object.values(state.users).map((u) => u.userId)).size === 5, 'user ids should be unique');
  });

  await testCase('reject invalid auth inputs', async () => {
    const email = `module.${RUN_ID}.bad@example.com`;
    await expectError('POST', '/auth/email-captcha', { body: { email, scene: 'unknown' } });
    await expectError('POST', '/auth/register', { body: { email, password: 'weak', captcha: '000000' } });
    await expectError('POST', '/auth/login', { body: { email: state.users.alice.email, password: 'WrongPass123' } });
  });

  await testCase('login, refresh, password change/reset, logout', async () => {
    const aliceLogin = await login(state.users.alice.email, PASSWORD);
    state.users.alice.accessToken = aliceLogin.accessToken;
    state.users.alice.refreshToken = aliceLogin.refreshToken;
    const refreshed = await request('POST', '/auth/refresh', { body: { refreshToken: aliceLogin.refreshToken } });
    assert(refreshed.accessToken, 'refresh should return access token');
    state.users.alice.accessToken = refreshed.accessToken;

    await api(state.users.erin).put('/auth/password', { oldPassword: PASSWORD, newPassword: NEXT_PASSWORD });
    const erinLogin = await login(state.users.erin.email, NEXT_PASSWORD);
    state.users.erin.accessToken = erinLogin.accessToken;
    state.users.erin.refreshToken = erinLogin.refreshToken;

    const captcha = await request('POST', '/auth/password-reset-captcha', {
      body: { email: state.users.erin.email, scene: 'reset_password' },
    });
    await request('POST', '/auth/password-reset', {
      body: { email: state.users.erin.email, captcha, newPassword: PASSWORD },
    });
    const resetLogin = await login(state.users.erin.email, PASSWORD);
    state.users.erin.accessToken = resetLogin.accessToken;
    state.users.erin.refreshToken = resetLogin.refreshToken;
  });
}

async function userFriendModule() {
  await testCase('profile read/update/search/email update', async () => {
    const alice = api(state.users.alice);
    const updated = await alice.put('/users/me', {
      nickname: `Module Alice ${RUN_ID}`,
      signature: 'module profile signature',
      gender: 2,
      age: 21,
      phone: `14${String(Date.now()).slice(-9)}`,
    });
    assert(updated.nickname.includes('Module Alice'), 'nickname should update');
    const me = await alice.get('/users/me');
    assert(me.userId === state.users.alice.userId, 'me should be alice');
    const search = await alice.get('/users/search', { params: { keyword: state.users.bob.email, page: 1, pageSize: 5 } });
    assert(list(search).some((u) => u.userId === state.users.bob.userId), 'bob should be searchable');

    const newEmail = `module.${RUN_ID}.alice.new@example.com`;
    const captcha = await request('POST', '/auth/email-captcha', { body: { email: newEmail, scene: 'modify_email' } });
    const emailUpdated = await alice.put('/users/me/email', { email: newEmail, captcha });
    assert(emailUpdated.email === newEmail, 'email should update');
    state.users.alice.email = newEmail;
  });

  await testCase('friend request sent/received/approve/reject/duplicate', async () => {
    await makeFriends(state.users.alice, state.users.bob);
    await makeFriends(state.users.alice, state.users.carol);

    const daveToAlice = await api(state.users.dave).post('/users/friend-requests', {
      receiverId: state.users.alice.userId,
      remark: 'please reject me',
    });
    const received = await api(state.users.alice).get('/users/friend-requests/received');
    assert(received.some((r) => r.requestId === daveToAlice.requestId), 'alice should receive dave request');
    await api(state.users.alice).post(`/users/friend-requests/${daveToAlice.requestId}/reject`, {});

    await api(state.users.alice).expectError('POST', '/users/friend-requests', {
      body: { receiverId: state.users.bob.userId, remark: 'duplicate friend' },
    });
    await api(state.users.alice).expectError('POST', '/users/friend-requests', {
      body: { receiverId: state.users.alice.userId, remark: 'self' },
    });
  });

  await testCase('friend metadata, block/unblock, delete friend', async () => {
    const alice = api(state.users.alice);
    const friend = await alice.put(`/users/friends/${state.users.bob.userId}`, {
      remarkName: 'Bob Remark',
      friendGroup: 'Module Friends',
      star: true,
    });
    assert(friend.remarkName === 'Bob Remark', 'remark should update');
    assert(friend.star === 1 || friend.star === true, 'star should update');

    await api(state.users.bob).post(`/users/blocks/${state.users.dave.userId}`, {});
    const blocked = await api(state.users.bob).get('/users/blocks');
    assert(blocked.some((u) => u.userId === state.users.dave.userId), 'dave should be blocked');
    await api(state.users.bob).delete(`/users/blocks/${state.users.dave.userId}`);

    await makeFriends(state.users.bob, state.users.dave);
    await api(state.users.bob).delete(`/users/friends/${state.users.dave.userId}`);
    const friends = await api(state.users.bob).get('/users/friends');
    assert(!friends.some((u) => u.userId === state.users.dave.userId), 'dave friendship should be deleted');
  });
}

async function socialModule() {
  await testCase('presence states and lookup', async () => {
    const alice = api(state.users.alice);
    for (const status of ['online', 'busy', 'invisible', 'offline']) {
      const presence = await alice.put('/social/presence', { status });
      assert(presence.status === status, `presence should be ${status}`);
    }
    await alice.expectError('PUT', '/social/presence', { body: { status: 'away' } });
    const bobPresence = await alice.get(`/social/presence/${state.users.bob.userId}`);
    assert(bobPresence.userId === state.users.bob.userId, 'presence lookup should return bob');
  });

  await testCase('global social search/history/recommendations', async () => {
    const alice = api(state.users.alice);
    const result = await alice.get('/social/search', { params: { keyword: 'Module' } });
    assert(result.users || result.groups || result.moments, 'social search payload should include sections');
    const history = await alice.get('/social/search/history');
    assert(Array.isArray(history), 'history should be an array');
    const recs = await alice.get('/social/recommendations');
    assert(recs, 'recommendations should return payload');
  });
}

async function privateChatModule() {
  await testCase('private chat guardrails and idempotent create', async () => {
    const alice = api(state.users.alice);
    await alice.expectError('POST', '/chats/private', { body: { targetUserId: state.users.alice.userId } });
    await alice.expectError('POST', '/chats/private', { body: { targetUserId: state.users.dave.userId } });
    const first = await alice.post('/chats/private', { targetUserId: state.users.bob.userId });
    const second = await alice.post('/chats/private', { targetUserId: state.users.bob.userId });
    assert(first.chatId === second.chatId, 'private chat create should be idempotent');
    state.chats.aliceBob = first;
  });

  await testCase('private message validation/send/list/search/read/typing/pin/recall/delete', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const chatId = state.chats.aliceBob.chatId;
    await alice.expectError('POST', `/chats/${chatId}/messages`, { body: { messageType: 'text', content: '' } });
    await alice.expectError('POST', `/chats/${chatId}/messages`, { body: { messageType: 'audio', content: 'x' } });

    const message = await alice.post(`/chats/${chatId}/messages`, {
      messageType: 'text',
      content: `module private keyword ${RUN_ID}`,
    });
    const reply = await bob.post(`/chats/${chatId}/messages`, {
      messageType: 'text',
      content: `module private reply ${RUN_ID}`,
    });
    const messages = await alice.get(`/chats/${chatId}/messages`, { params: { page: 1, pageSize: 10 } });
    assert(list(messages).some((m) => m.messageId === message.messageId), 'sent private message should be listed');
    const searched = await bob.get('/messages/search', { params: { chatId, keyword: RUN_ID, page: 1, pageSize: 10 } });
    assert(list(searched).length >= 2, 'private search should find both messages');
    await bob.post(`/chats/${chatId}/read`, {});
    await alice.post(`/chats/${chatId}/typing`, { typing: true });
    await alice.post(`/messages/${message.messageId}/pin`, {});
    await alice.delete(`/messages/${message.messageId}/pin`);
    await alice.expectError('POST', `/messages/${reply.messageId}/recall`, { body: {} });
    await bob.post(`/messages/${reply.messageId}/recall`, {});
    const disposable = await alice.post(`/chats/${chatId}/messages`, { messageType: 'text', content: 'delete me' });
    await alice.delete(`/messages/${disposable.messageId}`);
  });
}

async function groupModule() {
  await testCase('group create/search/update/members/admin/mute/announcement', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const carol = api(state.users.carol);
    const group = await alice.post('/groups', {
      groupName: `Module Group ${RUN_ID.slice(0, 6)}`,
      description: 'module group detail test',
      memberIds: [state.users.bob.userId, state.users.carol.userId],
    });
    state.groups.main = group;
    const groupId = group.groupId;
    assert(group.memberCount >= 3, 'group should include owner and two members');

    const search = await alice.get('/groups/search', { params: { keyword: 'Module', page: 1, pageSize: 10 } });
    assert(list(search).some((g) => g.groupId === groupId), 'group should be searchable');
    const updated = await alice.put(`/groups/${groupId}`, {
      groupName: `Module G ${RUN_ID.slice(0, 6)}`,
      description: 'updated detail test',
      chatEnabled: true,
      recallLimitMinutes: 30,
    });
    assert(updated.description === 'updated detail test', 'group description should update');

    await bob.put(`/groups/${groupId}/members/me/nickname`, { nickname: 'Bob In Module Group' });
    await alice.post(`/groups/${groupId}/members/${state.users.bob.userId}/admin`, {});
    await alice.delete(`/groups/${groupId}/members/${state.users.bob.userId}/admin`);
    await alice.post(`/groups/${groupId}/members/${state.users.carol.userId}/mute`, { minutes: 1 });
    await alice.delete(`/groups/${groupId}/members/${state.users.carol.userId}/mute`);
    await alice.post(`/groups/${groupId}/announcement`, { notice: `module notice ${RUN_ID}` });
    await bob.get(`/groups/${groupId}/announcement`);
    await bob.post(`/groups/${groupId}/announcement/read`, {});
    const stats = await alice.get(`/groups/${groupId}/announcement/read-stats`);
    assert(typeof stats.readCount === 'number' && typeof stats.memberCount === 'number', 'announcement stats should include read/member counts');
    await carol.get(`/groups/${groupId}/notifications`, { params: { page: 1, pageSize: 10 } });
  });

  await testCase('group messages/search/files/mention/read/recall/delete', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const groupId = state.groups.main.groupId;
    await bob.expectError('POST', `/groups/${groupId}/messages`, { body: { messageType: 'audio', content: 'x' } });
    const message = await alice.post(`/groups/${groupId}/messages`, {
      messageType: 'text',
      content: `module group keyword ${RUN_ID}`,
      mentionUserIds: [state.users.bob.userId],
    });
    const reply = await bob.post(`/groups/${groupId}/messages`, {
      messageType: 'text',
      content: `module group reply ${RUN_ID}`,
      replyToMessageId: message.messageId,
    });
    await alice.post(`/groups/${groupId}/messages/mention-all`, {
      messageType: 'text',
      content: `module group mention all ${RUN_ID}`,
    });
    const messages = await bob.get(`/groups/${groupId}/messages`, { params: { page: 1, pageSize: 20 } });
    assert(list(messages).some((m) => m.messageId === reply.messageId), 'group reply should be listed');
    const searched = await bob.get(`/groups/${groupId}/messages/search`, { params: { keyword: RUN_ID, page: 1, pageSize: 20 } });
    assert(list(searched).length >= 2, 'group search should find messages');
    await bob.get(`/groups/${groupId}/files`, { params: { page: 1, pageSize: 5 } });
    await bob.post(`/groups/${groupId}/read`, {});
    await alice.expectError('POST', `/groups/${groupId}/messages/${reply.messageId}/recall`, { body: {} });
    await bob.post(`/groups/${groupId}/messages/${reply.messageId}/recall`, {});
    const disposable = await alice.post(`/groups/${groupId}/messages`, { messageType: 'text', content: 'delete group message' });
    await alice.delete(`/groups/${groupId}/messages/${disposable.messageId}`);
  });

  await testCase('group add/remove members, join request, invite join, transfer owner, leave/dissolve', async () => {
    const alice = api(state.users.alice);
    const dave = api(state.users.dave);
    await makeFriends(state.users.alice, state.users.dave);

    const addGroup = await alice.post('/groups', {
      groupName: `Add Group ${RUN_ID.slice(0, 6)}`,
      description: 'add/remove members',
      memberIds: [state.users.bob.userId, state.users.carol.userId],
    });
    await alice.post(`/groups/${addGroup.groupId}/members`, { memberIds: [state.users.dave.userId] });
    let members = await alice.get(`/groups/${addGroup.groupId}/members`);
    assert(members.some((m) => m.userId === state.users.dave.userId), 'dave should be added');
    await alice.delete(`/groups/${addGroup.groupId}/members/${state.users.dave.userId}`);

    const requestGroup = await alice.post('/groups', {
      groupName: `Req Group ${RUN_ID.slice(0, 6)}`,
      description: 'join request group',
      memberIds: [state.users.bob.userId, state.users.carol.userId],
    });
    const joinReq = await dave.post(`/groups/${requestGroup.groupId}/join-requests`, { message: 'please let me in' });
    await dave.get('/groups/join-requests/me');
    const pending = await alice.get(`/groups/${requestGroup.groupId}/join-requests`);
    assert(pending.some((r) => r.requestId === joinReq.requestId), 'join request should be pending');
    await alice.post(`/groups/${requestGroup.groupId}/join-requests/${joinReq.requestId}/approve`, {});

    const inviteGroup = await alice.post('/groups', {
      groupName: `Inv Group ${RUN_ID.slice(0, 6)}`,
      description: 'invite group',
      memberIds: [state.users.bob.userId, state.users.carol.userId],
    });
    await dave.post(`/groups/invite/${inviteGroup.inviteCode}/join`, {});
    await alice.post(`/groups/${inviteGroup.groupId}/owner`, { targetUserId: state.users.bob.userId });
    await api(state.users.carol).post(`/groups/${inviteGroup.groupId}/leave`, {});
    await api(state.users.bob).delete(`/groups/${inviteGroup.groupId}`);
  });
}

async function momentModule() {
  await testCase('moment validation and visibility', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const dave = api(state.users.dave);
    await alice.expectError('POST', '/moments', { body: { content: 'spam content should be blocked', visibility: 'public' } });
    await alice.expectError('POST', '/moments', { body: { content: 'bad visibility', visibility: 'specified' } });

    const publicMoment = await alice.post('/moments', {
      content: `module public moment ${RUN_ID}`,
      location: 'Module City',
      tags: ['module', 'public'],
      mood: 'curious',
      activity: 'testing',
      visibility: 'public',
    });
    const friendsMoment = await alice.post('/moments', {
      content: `module friends moment ${RUN_ID}`,
      visibility: 'friends',
      tags: ['friends'],
    });
    const specifiedMoment = await alice.post('/moments', {
      content: `module specified moment ${RUN_ID}`,
      visibility: 'specified',
      visibleUserIds: [state.users.bob.userId],
    });
    state.moments.public = publicMoment;
    state.moments.friends = friendsMoment;
    state.moments.specified = specifiedMoment;

    const bobView = await bob.get(`/moments/${specifiedMoment.momentId}`);
    assert(bobView.momentId === specifiedMoment.momentId, 'specified user should view moment');
    await dave.expectError('GET', `/moments/${specifiedMoment.momentId}`);
  });

  await testCase('moment feeds, likes, comments, replies, collections, reports, notifications', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const carol = api(state.users.carol);
    const momentId = state.moments.public.momentId;
    const timeline = await bob.get('/moments', { params: { page: 1, pageSize: 20 } });
    assert(list(timeline).some((m) => m.momentId === momentId), 'timeline should include public moment');
    await bob.get(`/moments/users/${state.users.alice.userId}`, { params: { page: 1, pageSize: 20 } });
    await bob.get(`/moments/users/${state.users.alice.userId}/summary`);

    await bob.post(`/moments/${momentId}/likes`, {});
    const likes = await alice.get(`/moments/${momentId}/likes`);
    assert(likes.some((u) => u.userId === state.users.bob.userId), 'likes should include bob');
    await bob.post(`/moments/${momentId}/collect`, {});
    const collections = await bob.get('/moments/collections', { params: { page: 1, pageSize: 20 } });
    assert(list(collections).some((m) => m.momentId === momentId), 'collections should include moment');

    const comment = await bob.post(`/moments/${momentId}/comments`, {
      content: `module comment ${RUN_ID}`,
      mentionUserIds: [state.users.alice.userId],
    });
    const reply = await alice.post(`/moments/${momentId}/comments`, {
      content: `module reply ${RUN_ID}`,
      replyToCommentId: comment.commentId,
      mentionUserIds: [state.users.bob.userId],
    });
    const comments = await carol.get(`/moments/${momentId}/comments`);
    assert(comments.some((c) => c.commentId === comment.commentId), 'comments should include bob comment');
    await alice.delete(`/moments/${momentId}/comments/${reply.commentId}`);

    await carol.post(`/moments/${momentId}/report`, { reason: `module report ${RUN_ID}` });
    const unread = await alice.get('/moments/notifications/unread-count');
    assert(typeof unread === 'number', 'unread count should be numeric');
    const notifications = await alice.get('/moments/notifications', { params: { page: 1, pageSize: 20 } });
    const first = list(notifications)[0];
    if (first?.notificationId) await alice.put(`/moments/notifications/${first.notificationId}/read`, {});
    await alice.put('/moments/notifications/read-all', {});

    await bob.delete(`/moments/${momentId}/likes`);
    await bob.delete(`/moments/${momentId}/collect`);
  });

  await testCase('moment update/delete permissions', async () => {
    const alice = api(state.users.alice);
    const bob = api(state.users.bob);
    const momentId = state.moments.public.momentId;
    await bob.expectError('PUT', `/moments/${momentId}`, { body: { content: 'not mine' } });
    const updated = await alice.put(`/moments/${momentId}`, {
      content: `module public moment updated ${RUN_ID}`,
      tags: ['module', 'updated'],
      visibility: 'public',
    });
    assert(updated.content.includes('updated'), 'moment should update');
    await bob.expectError('DELETE', `/moments/${momentId}`);
    await alice.delete(`/moments/${state.moments.specified.momentId}`);
  });
}

async function fileModule() {
  await testCase('file upload validation or configured OSS upload', async () => {
    const form = new FormData();
    form.set('scene', 'attachment');
    form.set('file', new Blob(['module file upload test'], { type: 'text/plain' }), `module-${RUN_ID}.txt`);
    const response = await request('POST', '/files/upload', {
      token: state.users.alice.accessToken,
      body: form,
      unwrap: false,
    });
    if (response.ok && response.payload?.code === 0) {
      assert(response.payload.data.fileId, 'upload success should include fileId');
    } else {
      assert(response.status >= 400 || response.payload?.code !== 0, 'upload should either succeed or return api error');
      assert(String(response.payload?.message || '').includes('oss') || String(response.payload?.message || '').includes('upload'), 'upload error should explain oss/upload issue');
    }
  });
}

async function cleanupModule() {
  await testCase('logout all generated users', async () => {
    for (const user of Object.values(state.users)) {
      if (user.refreshToken) {
        await request('POST', '/auth/logout', { body: { refreshToken: user.refreshToken } });
      }
    }
  });
}

async function main() {
  log('Hello Chat module regression test');
  log(`API_BASE_URL=${BASE_URL}`);
  log(`RUN_ID=${RUN_ID}`);

  await module('auth', authModule);
  await module('user-friend', userFriendModule);
  await module('social', socialModule);
  await module('private-chat', privateChatModule);
  await module('group', groupModule);
  await module('moment', momentModule);
  await module('file', fileModule);
  await module('cleanup', cleanupModule);

  log('');
  log(`Summary: ${state.stats.modules} modules, ${state.stats.passed}/${state.stats.cases} cases passed, ${state.stats.failed} failed`);
  if (state.stats.failed > 0) process.exitCode = 1;
}

main().catch((error) => {
  log('');
  log('Module regression test stopped. Make sure the backend is running, then rerun:');
  log('  node scripts/e2e-modules.mjs');
  log('');
  log(error.stack || error.message);
  process.exit(1);
});
