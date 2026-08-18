export const environment = {
  production: true,
  // URL relative : fonctionne quel que soit le domaine (ngrok, VPS futur...)
  // car le dashboard et l'API sont servis sous le meme nom d'hote via nginx (/api/...).
  apiUrl: '/api'
};
