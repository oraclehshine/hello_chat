function tryRecoverLatin1Utf8(input: string): string {
  try {
    const bytes = Uint8Array.from(input, (char) => char.charCodeAt(0) & 0xff)
    const recovered = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
    return recovered
  } catch {
    return input
  }
}

export function normalizeDisplayText(value: unknown): string {
  if (value == null) return ''
  const raw = String(value)
  if (!raw) return ''
  const cleaned = raw.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '').replace(/�+/g, '')
  if (!/[ÃÂð]/.test(cleaned)) return cleaned
  return tryRecoverLatin1Utf8(cleaned)
}
