export type AppRole = 'SUPER_ADMIN' | 'ADMIN_PRINCIPAL' | 'ADMIN_SECONDAIRE' | 'SUPERVISOR' | 'AGENT';

/**
 * Source unique de vérité : quels rôles ont accès à quelle section.
 * Utilisé à la fois par roleGuard (protection réelle des routes) et par
 * MainLayoutComponent (affichage de la sidebar), pour éviter que les deux
 * se désynchronisent.
 */
export const ROUTE_ROLES: Record<string, AppRole[]> = {
  organizations: ['SUPER_ADMIN'],
  users: ['SUPER_ADMIN', 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE'],
  forms: ['ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE'],
  collectes: ['ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPERVISOR'],
  'audit-logs': ['SUPER_ADMIN', 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE'],
  'sync-logs': ['SUPER_ADMIN', 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPERVISOR'],
  missions: ['ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPERVISOR'],
};

