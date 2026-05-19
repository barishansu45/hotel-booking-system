import type { User } from '@supabase/supabase-js';

/** Backend expects JWT role `admin` → Spring converts to ROLE_ADMIN. Prefer Supabase app_metadata. */
export function isAdminUser(user: User | null): boolean {
  if (!user) return false;
  const app = user.app_metadata as Record<string, unknown> | undefined;
  const meta = user.user_metadata as Record<string, unknown> | undefined;
  const r = (app?.role ?? meta?.role) as string | undefined;
  return typeof r === 'string' && r.toLowerCase() === 'admin';
}
