document.addEventListener('DOMContentLoaded', () => {
  const pageTitleEl = document.getElementById('page-title');
  const pageUrlEl = document.getElementById('page-url');
  const badgeTotalEl = document.getElementById('badge-total');
  const imgCountNumEl = document.getElementById('img-count-num');
  const galleryGridEl = document.getElementById('gallery-grid');
  const btnDownloadGallery = document.getElementById('btn-download-gallery');
  const btnSendIntercepted = document.getElementById('btn-send-intercepted');
  const btnClearImages = document.getElementById('btn-clear-images');
  const btnSyncCookies = document.getElementById('btn-sync-cookies');
  const statusMsgEl = document.getElementById('status-msg');

  let activeTab = null;
  let currentUrls = [];

  function setStatus(text, color = '#94a3b8') {
    statusMsgEl.textContent = text;
    statusMsgEl.style.color = color;
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  // ── Renderizado del Grid de Miniaturas ────────────────────────────────────
  function renderGallery(urls) {
    currentUrls = urls || [];
    const count = currentUrls.length;

    badgeTotalEl.textContent = `${count} img`;
    imgCountNumEl.textContent = count;
    btnSendIntercepted.disabled = count === 0;

    if (count === 0) {
      galleryGridEl.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          Navega por la galería o abre ilustraciones para capturar URLs directamente
        </div>`;
      return;
    }

    // Renderizar las imágenes con referrerpolicy="no-referrer"
    const itemsHtml = currentUrls.slice(0, 48).map((url, idx) => {
      const safeUrl = escapeHtml(url);
      return `
        <div class="gallery-item" title="Imagen #${idx + 1}&#10;${safeUrl}">
          <img src="${safeUrl}"
               referrerpolicy="no-referrer"
               loading="lazy"
               onerror="this.style.opacity='0.25'; this.alt='CORS block';"
          />
          <span class="item-num">#${idx + 1}</span>
        </div>`;
    }).join('');

    galleryGridEl.innerHTML = itemsHtml;
  }

  // ── Inicialización con Pestaña Activa ──────────────────────────────────────
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    if (!tabs || !tabs[0]) return;
    activeTab = tabs[0];
    pageTitleEl.textContent = activeTab.title || 'Página Web';
    pageUrlEl.textContent = activeTab.url || '';

    // Cargar URLs interceptadas para esta página
    chrome.runtime.sendMessage(
      { type: 'GET_INTERCEPTED', pageUrl: activeTab.url },
      (response) => {
        if (response && response.urls) {
          renderGallery(response.urls);
        }
      }
    );
  });

  // ── 1. Descargar Galería Completa mediante gallery-dl ──────────────────────
  btnDownloadGallery.addEventListener('click', async () => {
    if (!activeTab?.url) return;
    setStatus('Enviando galería a Gallery-DL GUI...', '#c084fc');

    try {
      const res = await fetch('http://127.0.0.1:18274/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: activeTab.url }),
      });

      if (res.ok) {
        setStatus('✅ Galería encolada en Gallery-DL GUI', '#4ade80');
      } else {
        setStatus('❌ Error: El servidor rechazó la solicitud.', '#f87171');
      }
    } catch {
      setStatus('⚠️ No se pudo conectar con la app desktop (¿está abierta?).', '#f87171');
    }
  });

  // ── 2. Enviar Imágenes Interceptadas Directamente ──────────────────────────
  btnSendIntercepted.addEventListener('click', async () => {
    if (currentUrls.length === 0) return;
    setStatus(`Enviando ${currentUrls.length} imágenes a la app...`, '#c084fc');
    btnSendIntercepted.disabled = true;

    try {
      const res = await fetch('http://127.0.0.1:18274/images', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          urls: currentUrls,
          pageUrl: activeTab?.url || '',
          pageTitle: activeTab?.title || 'Descarga_Directa',
        }),
      });

      if (res.ok) {
        const data = await res.json();
        const total = data.queued || currentUrls.length;
        setStatus(`✅ ${total} imágenes enviadas a descargar`, '#4ade80');
      } else {
        setStatus('❌ Error al enviar imágenes a la app.', '#f87171');
        btnSendIntercepted.disabled = false;
      }
    } catch {
      setStatus('⚠️ No se pudo conectar con la app desktop (¿está abierta?).', '#f87171');
      btnSendIntercepted.disabled = false;
    }
  });

  // ── 3. Limpiar Imágenes Interceptadas ─────────────────────────────────────
  btnClearImages.addEventListener('click', () => {
    if (!activeTab) return;
    chrome.runtime.sendMessage(
      { type: 'CLEAR_INTERCEPTED', pageUrl: activeTab.url, tabId: activeTab.id },
      () => {
        renderGallery([]);
        setStatus('Lista de capturas reiniciada.', '#94a3b8');
      }
    );
  });

  // ── 4. Sincronizar Cookies ────────────────────────────────────────────────
  btnSyncCookies.addEventListener('click', async () => {
    if (!activeTab?.url) return;
    setStatus('Extrayendo cookies de sesión...', '#c084fc');

    try {
      const urlObj = new URL(activeTab.url);
      const domain = urlObj.hostname;

      chrome.cookies.getAll({ domain: domain }, async (cookies) => {
        if (!cookies || cookies.length === 0) {
          setStatus('No se encontraron cookies para este dominio.', '#f87171');
          return;
        }

        setStatus(`Enviando ${cookies.length} cookies a la app...`, '#c084fc');
        try {
          const res = await fetch('http://127.0.0.1:18274/cookies', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cookies),
          });

          if (res.ok) {
            setStatus(`✅ Cookies de ${domain} sincronizadas con éxito`, '#4ade80');
          } else {
            setStatus('❌ Error al procesar cookies en la app.', '#f87171');
          }
        } catch {
          setStatus('⚠️ No se pudo conectar con la app desktop.', '#f87171');
        }
      });
    } catch {
      setStatus('❌ Error al analizar la URL de la página.', '#f87171');
    }
  });

  // ── Escuchar Actualizaciones en Vivo del Background ───────────────────────
  chrome.runtime.onMessage.addListener((msg) => {
    if (msg.type === 'PANEL_UPDATE' && msg.pageUrl === activeTab?.url) {
      renderGallery(msg.urls);
    }
  });
});
