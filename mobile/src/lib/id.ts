/**
 * RFC4122 v4 UUID, generated client-side so offline creates get stable IDs that
 * sync cleanly. Math.random-based — sufficient for client row identifiers.
 */
export function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/** Current time as an ISO string (used for created_at/updated_at). */
export function nowIso(): string {
  return new Date().toISOString();
}
