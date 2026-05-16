export function resolveAssetUrl(url?: null | string) {
  if (!url) return ''
  if (url.startsWith('data:') || url.startsWith('blob:') || url.startsWith('/api/v1/files/redirect?source=')) {
    return url
  }
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) {
    return `/api/v1/files/redirect?source=${encodeURIComponent(url)}`
  }
  return url
}
