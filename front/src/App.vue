<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">HC</div>
        <div>
          <h1>Hello Chat</h1>
          <p>Vue 3 + Spring Boot 3.0.3</p>
        </div>
      </div>

      <nav class="nav-list">
        <RouterLink to="/login">Login</RouterLink>
        <RouterLink to="/register">Register</RouterLink>
        <RouterLink to="/reset-password">Reset Password</RouterLink>
        <RouterLink to="/home">Home</RouterLink>
        <RouterLink to="/friends">Friends</RouterLink>
        <RouterLink to="/chats">Chats</RouterLink>
        <RouterLink to="/groups">Groups</RouterLink>
        <RouterLink to="/moments">Moments</RouterLink>
      </nav>

      <section class="sidebar-card">
        <h2>Current Phase</h2>
        <p>Phase 4: Moments</p>
        <ul>
          <li>Email account</li>
          <li>Profile management</li>
          <li>Private, group, and moments</li>
        </ul>
      </section>
    </aside>

    <main class="content-panel">
      <RouterView />
    </main>
  </div>
</template>
