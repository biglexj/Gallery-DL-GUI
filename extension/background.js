/**
 * Gallery-DL GUI Connector — background.js (Service Worker)
 *
 * Gestiona el estado de imágenes interceptadas por pestaña y notifica
 * al popup cuando hay nuevas capturas.
 */

chrome.runtime.onInstalled.addListener(() => {
  console.log('[Gallery-DL Connector] Instalado. Service worker activo.');
});

// Mapa: pageUrl → { title: string, urls: string[] }
// Nota: los Maps no persisten entre reinicios del service worker, pero
// para esta sesión de navegación es suficiente.
const pageData = new Map();

// ─── Mensajes desde content.js y popup.js ────────────────────────────────────

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {

  // 1. Content.js encontró una imagen
  if (msg.type === 'IMAGE_INTERCEPTED') {
    const { url, pageUrl, pageTitle } = msg;
    if (!pageData.has(pageUrl)) {
      pageData.set(pageUrl, { title: pageTitle, urls: [] });
    }
    const entry = pageData.get(pageUrl);
    if (!entry.urls.includes(url)) {
      entry.urls.push(url);

      // Actualizar badge del ícono de la extensión
      if (sender.tab?.id) {
        const count = entry.urls.length;
        chrome.action.setBadgeText({
          text: count > 999 ? '999+' : String(count),
          tabId: sender.tab.id,
        });
        chrome.action.setBadgeBackgroundColor({ color: '#a855f7', tabId: sender.tab.id });
      }

      // Notificar al popup si está abierto (ignora error si no lo está)
      chrome.runtime.sendMessage({
        type: 'PANEL_UPDATE',
        pageUrl,
        urls: entry.urls,
      }).catch(() => {});
    }
    sendResponse({ ok: true });
    return true;
  }

  // 2. Popup solicita el estado actual de una página
  if (msg.type === 'GET_INTERCEPTED') {
    const entry = pageData.get(msg.pageUrl);
    sendResponse({ urls: entry?.urls ?? [], title: entry?.title ?? '' });
    return true;
  }

  // 3. Popup limpia la lista de una página
  if (msg.type === 'CLEAR_INTERCEPTED') {
    pageData.delete(msg.pageUrl);
    if (msg.tabId) {
      chrome.action.setBadgeText({ text: '', tabId: msg.tabId });
    }
    sendResponse({ ok: true });
    return true;
  }

  return true;
});
