let csrfToken;

export async function refreshCsrf() {
  try {
    const response = await fetch('/api/autenticacao/csrf', { credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new Error();
    csrfToken = await response.json();
  } catch {
    throw new Error('Não foi possível obter a proteção de segurança. Verifique sua conexão e tente novamente.');
  }
}

export async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = new Headers(options.headers);
  if (method !== 'GET') {
    if (!csrfToken) await refreshCsrf();
    headers.set(csrfToken.headerName, csrfToken.token);
  }
  let response;
  try {
    response = await fetch(path, { ...options, headers, credentials: 'same-origin', cache: 'no-store' });
  } catch {
    throw new Error('Falha de conexão. Verifique sua rede e tente novamente.');
  }
  const body = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    if (body?.codigo === 'CSRF_INVALIDO') {
      csrfToken = undefined;
      await refreshCsrf();
    }
    const error = new Error(body?.mensagem || 'Não foi possível concluir a solicitação.');
    error.status = response.status;
    error.code = body?.codigo;
    throw error;
  }
  return body;
}

export async function logout() {
  await api('/api/autenticacao/logout', { method: 'POST' });
  csrfToken = undefined;
  try {
    await refreshCsrf();
  } catch {
    csrfToken = undefined;
  }
}
