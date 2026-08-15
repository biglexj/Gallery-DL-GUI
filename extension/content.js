/**
 * Gallery-DL GUI Connector — content.js v3
 *
 * Estrategia robusta y precisa para capturar imágenes de DeviantArt / Pixiv:
 *
 * 1. PRIORIDAD 1: Extracción estructurada desde __NEXT_DATA__ (DeviantArt Next.js state).
 *    Extrae directamente los objetos media de cada deviation con su baseUri original
 *    y token de autenticación. Esto garantiza calidad máxima (sin resize) y acceso
 *    a contenido +18 / restricción de edad ya autenticado por la sesión del navegador.
 *
 * 2. PRIORIDAD 2: Escaneo de imágenes en el DOM filtrando ESTRICTAMENTE dominios de galería
 *    (images-wixmp-*.wixmp.com, i.pximg.net) y excluyendo cualquier CDN de UI/temas
 *    (st.deviantart.net, avatares, banners, icons).
 *
 * 3. Normalización a máxima resolución:
 *    Toma el path antes de /v1/fill/ y conserva intacto el ?token=eyJ...
 */

console.log('[Gallery-DL Connector] content.js v3 activo en:', location.href);

// ─── Filtros de CDN ─────────────────────────────────────────────────────────

// Solo CDN real de imágenes de arte
const WIXMP_HOST_RE = /^images[-.]wixmp[^.]*\.wixmp\.com$/i;
const PIXIV_HOST_RE = /^(?:i|i-cf)\.pximg\.net$/i;

// Dominios de UI / estáticos que NUNCA contienen arte descargable
const BLOCKED_HOSTS = new Set([
  'st.deviantart.net',
  'a.deviantart.net',
  'bg.deviantart.net',
  'fc06.deviantart.net',
  'th00.deviantart.net',
  'th01.deviantart.net',
  'th02.deviantart.net',
  'th03.deviantart.net',
  's.gravatar.com',
  'secure.gravatar.com',
  'www.deviantart.com',
  'deviantart.com',
]);

const BLOCKED_PATHS = [
  /\/avatars?\//i,
  /\/userpic\//i,
  /\/eclipse\//i,
  /\/static\//i,
  /\/promo\//i,
  /\/emoticons?\//i,
  /\/badges?\//i,
  /\/header\//i,
  /\/minigallery\//i,
  /\/icon/i,
  /\/logo/i,
];

/**
 * Valida si una URL pertenece a un archivo de imagen de galería genuino.
 */
function isValidArtworkUrl(url) {
  if (!url || typeof url !== 'string' || !url.startsWith('http')) return false;

  try {
    const parsed = new URL(url);
    const host = parsed.hostname.toLowerCase();

    // Bloquear dominios de UI conocidos
    if (BLOCKED_HOSTS.has(host)) return false;

    // Bloquear rutas de UI conocidas
    if (BLOCKED_PATHS.some(re => re.test(parsed.pathname))) return false;

    // Solo permitir WixMP (DeviantArt) o Pixiv
    const isWixmp = WIXMP_HOST_RE.test(host);
    const isPixiv = PIXIV_HOST_RE.test(host);

    return isWixmp || isPixiv;
  } catch {
    return false;
  }
}

/**
 * Convierte una URL de WixMP con resize a su versión original sin compresión,
 * preservando el query string con el token de autenticación.
 */
function toOriginalArtworkUrl(url) {
  try {
    const u = new URL(url);
    if (WIXMP_HOST_RE.test(u.hostname)) {
      const v1Index = u.pathname.indexOf('/v1/');
      if (v1Index !== -1) {
        const basePath = u.pathname.substring(0, v1Index);
        return `${u.protocol}//${u.host}${basePath}${u.search}`;
      }
    }
    return url;
  } catch {
    return url;
  }
}

// ─── Extracción estructurada de DeviantArt (__NEXT_DATA__) ───────────────────

function extractFromDeviantArtNextData() {
  const urls = [];
  try {
    const scriptEl = document.getElementById('__NEXT_DATA__');
    if (!scriptEl) return urls;

    const data = JSON.parse(scriptEl.textContent || '{}');
    const entities = data?.props?.pageProps?.initialState?.entities;

    if (entities?.deviation) {
      const deviations = entities.deviation;

      // Si estamos en la página de un deviation individual (/art/titulo-123456)
      const isArtPage = location.pathname.includes('/art/');
      const artMatch = location.pathname.match(/-([0-9]+)$/);
      const targetId = artMatch ? artMatch[1] : null;

      if (isArtPage && targetId && deviations[targetId]) {
        // Extraer únicamente la obra principal que el usuario está visualizando
        const dev = deviations[targetId];
        const media = dev?.media;
        if (media?.baseUri) {
          const token = media.token?.[0] ? `?token=${media.token[0]}` : '';
          const fullUrl = `${media.baseUri}${token}`;
          if (isValidArtworkUrl(fullUrl)) {
            urls.push(toOriginalArtworkUrl(fullUrl));
          }
        }
      } else {
        // En galerías y colecciones, extraer todas las deviations del catálogo
        for (const id of Object.keys(deviations)) {
          const dev = deviations[id];
          const media = dev?.media;
          if (!media?.baseUri) continue;

          const baseUri = media.baseUri;
          const token = media.token?.[0] ? `?token=${media.token[0]}` : '';
          const fullUrl = `${baseUri}${token}`;

          if (isValidArtworkUrl(fullUrl)) {
            urls.push(toOriginalArtworkUrl(fullUrl));
          }
        }
      }
    }

    // Fallback genérico si no se encontró nada estructurado
    if (urls.length === 0) {
      const rawText = scriptEl.textContent || '';
      const wixmpMatches = rawText.match(/https:\\\/\\\/images[-.]wixmp[^"\\]+/g) || [];
      for (const raw of wixmpMatches) {
        const clean = raw.replace(/\\\//g, '/');
        if (isValidArtworkUrl(clean)) {
          urls.push(toOriginalArtworkUrl(clean));
        }
      }
    }
  } catch (e) {
    console.warn('[Gallery-DL Connector] Error procesando __NEXT_DATA__:', e);
  }
  return [...new Set(urls)];
}

// ─── Captura y Despacho ───────────────────────────────────────────────────────

const capturedUrls = new Set();

function dispatchImage(rawUrl) {
  if (!rawUrl) return;
  const fullUrl = toOriginalArtworkUrl(rawUrl.trim());
  if (!isValidArtworkUrl(fullUrl)) return;
  if (capturedUrls.has(fullUrl)) return;

  capturedUrls.add(fullUrl);
  chrome.runtime.sendMessage({
    type: 'IMAGE_INTERCEPTED',
    url: fullUrl,
    pageUrl: location.href,
    pageTitle: document.title,
  });
}

function scanDOMImages() {
  // 1. Deviations en galería o art stage
  document.querySelectorAll('img').forEach(img => {
    dispatchImage(img.src);
    dispatchImage(img.dataset?.src);
    dispatchImage(img.dataset?.lazySrc);
    dispatchImage(img.currentSrc);
  });

  // 2. Elementos <picture> <source>
  document.querySelectorAll('source[srcset]').forEach(source => {
    source.srcset.split(',').forEach(entry => {
      const u = entry.trim().split(/\s+/)[0];
      dispatchImage(u);
    });
  });
}

// ─── MutationObserver para scroll dinámico / lazy load ───────────────────────

const observer = new MutationObserver(mutations => {
  for (const m of mutations) {
    for (const node of m.addedNodes) {
      if (node.nodeType !== Node.ELEMENT_NODE) continue;
      if (node.tagName === 'IMG') {
        dispatchImage(node.src);
        dispatchImage(node.dataset?.src);
      }
      node.querySelectorAll?.('img').forEach(img => {
        dispatchImage(img.src);
        dispatchImage(img.dataset?.src);
      });
      node.querySelectorAll?.('source[srcset]').forEach(src => {
        src.srcset.split(',').forEach(e => dispatchImage(e.trim().split(/\s+/)[0]));
      });
    }

    if (
      m.type === 'attributes' &&
      m.target.tagName === 'IMG' &&
      ['src', 'data-src', 'current-src'].includes(m.attributeName)
    ) {
      dispatchImage(m.target.src || m.target.dataset?.src);
    }
  }
});

// ─── Ciclo de Vida ───────────────────────────────────────────────────────────

function runCapture() {
  // Paso 1: Extracción directa de NextData (máxima precisión)
  const nextUrls = extractFromDeviantArtNextData();
  nextUrls.forEach(u => dispatchImage(u));

  // Paso 2: Escaneo del DOM
  if (document.body) {
    scanDOMImages();
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['src', 'data-src'],
    });
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', runCapture);
} else {
  runCapture();
}

window.addEventListener('load', () => {
  runCapture();
});
