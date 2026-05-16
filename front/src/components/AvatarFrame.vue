<template>
  <span class="avatar-frame" :class="[`avatar-${size}`, { interactive }]">
    <img v-if="resolvedSrc && !broken" :src="resolvedSrc" :alt="alt" @error="broken = true" />
    <span v-else class="avatar-fallback">{{ displayInitials }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveAssetUrl } from '../utils/assets'

const props = withDefaults(
  defineProps<{
    src?: string | null
    name?: string
    alt?: string
    size?: 'sm' | 'md' | 'lg' | 'xl' | 'hero'
    interactive?: boolean
  }>(),
  {
    src: '',
    name: '',
    alt: 'avatar',
    size: 'md',
    interactive: false,
  },
)

const broken = ref(false)

const resolvedSrc = computed(() => (props.src ? resolveAssetUrl(props.src) : ''))
const displayInitials = computed(() => (props.name || 'HC').slice(0, 2).toUpperCase())

watch(
  () => props.src,
  () => {
    broken.value = false
  },
)
</script>

<style scoped>
.avatar-frame {
  position: relative;
  display: inline-grid;
  overflow: hidden;
  border-radius: 50%;
  place-items: center;
  background:
    radial-gradient(circle at 28% 20%, rgba(255, 255, 255, 0.86), transparent 28%),
    linear-gradient(135deg, #2f7eff, #69b7ff);
  box-shadow:
    inset 0 0 0 4px rgba(255, 255, 255, 0.94),
    0 12px 28px rgba(37, 99, 255, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.avatar-frame.interactive:hover {
  transform: translateY(-2px) scale(1.02);
  filter: saturate(1.04);
  box-shadow:
    inset 0 0 0 4px rgba(255, 255, 255, 0.98),
    0 18px 36px rgba(37, 99, 255, 0.24);
}

.avatar-frame img,
.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: inherit;
}

.avatar-frame img {
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 900;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.avatar-sm {
  width: 36px;
  height: 36px;
}

.avatar-md {
  width: 42px;
  height: 42px;
}

.avatar-lg {
  width: 52px;
  height: 52px;
}

.avatar-xl {
  width: 72px;
  height: 72px;
}

.avatar-hero {
  width: 128px;
  height: 128px;
}

.avatar-sm .avatar-fallback {
  font-size: 11px;
}

.avatar-md .avatar-fallback {
  font-size: 12px;
}

.avatar-lg .avatar-fallback {
  font-size: 14px;
}

.avatar-xl .avatar-fallback {
  font-size: 20px;
}

.avatar-hero .avatar-fallback {
  font-size: 34px;
}
</style>
