import json
import sys
import uuid
from pathlib import Path

from playwright.sync_api import sync_playwright, expect


projectRoot = Path(__file__).resolve().parents[3]
evidenceDirectory = projectRoot / "docs/testes/evidencias"
resultDirectory = projectRoot / "docs/testes/resultados"
baseUrl = "http://localhost:8091"
results = []
email = f"navegador-{uuid.uuid4()}@example.com"
password = " senha fictícia "


def availablePath(directory, name):
    path = directory / name
    attempt = 1
    while path.exists():
        path = directory / f"{Path(name).stem}-reexecucao{attempt}{Path(name).suffix}"
        attempt += 1
    return path


def capture(page, name):
    path = availablePath(evidenceDirectory, name)
    page.screenshot(path=str(path), full_page=True)
    return str(path.relative_to(projectRoot)).replace("\\", "/")


def record(name, evidence=None):
    results.append({"cenario": name, "resultado": "aprovado", "evidencia": evidence})


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(channel="msedge", headless=True)
    context = browser.new_context(viewport={"width": 1280, "height": 900})
    page = context.new_page()
    try:
        page.goto(baseUrl + "/paginas/conta/cadastro.html", wait_until="networkidle")
        page.get_by_label("Nome", exact=True).fill("Cliente Navegador")
        page.get_by_label("E-mail", exact=True).fill(email)
        page.get_by_label("Senha", exact=True).fill(password)
        page.get_by_label("Confirmar senha", exact=True).fill("divergente")
        posts = []
        page.on("request", lambda request: posts.append(request.url) if request.method == "POST" else None)
        page.get_by_role("button", name="Criar conta", exact=True).click()
        expect(page.locator("#formMessage")).to_have_text("As senhas não coincidem.")
        assert not posts
        record("AUTH12 confirmação divergente impede POST")
        page.get_by_label("Confirmar senha", exact=True).fill(password)
        page.get_by_role("button", name="Criar conta", exact=True).click()
        expect(page.locator("#formMessage")).to_contain_text("Conta criada com sucesso")
        expect(page.get_by_label("Senha", exact=True)).to_have_value("")
        assert context.request.get(baseUrl + "/api/autenticacao/sessao").status == 401
        record("AUTH01 cadastro real sem login automático", capture(page, "HU03_AUTH01_cadastro-sucesso.png"))
        page.locator("#loginLink").click()
        page.wait_for_url("**/login.html")
        page.get_by_label("E-mail", exact=True).fill(email)
        page.get_by_label("Senha", exact=True).fill("senha errada")
        page.get_by_role("button", name="Entrar", exact=True).click()
        expect(page.locator("#formMessage")).to_have_text("E-mail ou senha inválidos.")
        record("AUTH05 mensagem genérica", capture(page, "HU04_AUTH05_login-invalido.png"))
        page.get_by_label("E-mail", exact=True).fill(email.upper())
        page.get_by_label("Senha", exact=True).fill(password)
        page.get_by_role("button", name="Entrar", exact=True).click()
        page.wait_for_url("**/minha-conta.html")
        expect(page.locator("#accountEmail")).to_have_text(email)
        page.reload(wait_until="networkidle")
        expect(page.locator("#accountName")).to_have_text("Cliente Navegador")
        expect(page.locator("#accountEmail")).to_have_text(email)
        record("AUTH04 sessão após recarga", capture(page, "HU04_AUTH04_sessao-recarga.png"))
        sessionResponse = context.request.get(baseUrl + "/api/autenticacao/sessao")
        assert "no-store" in sessionResponse.headers["cache-control"]
        cookie = next(cookie for cookie in context.cookies() if cookie["name"] == "JSESSIONID")
        assert cookie["httpOnly"] and cookie["sameSite"] == "Lax"
        assert context.request.get(baseUrl + "/api/admin/futuro").status == 403
        assert context.request.post(baseUrl + "/api/autenticacao/logout").status == 403
        assert context.request.get(baseUrl + "/api/autenticacao/sessao").status == 200
        record("AUTH07/09 CSRF e autorização no servidor")
        for width in [360, 1280]:
            page.set_viewport_size({"width": width, "height": 900})
            page.reload(wait_until="networkidle")
            expect(page.locator("#accountEmail")).to_have_text(email)
            assert page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            record(f"AUTH12 minha conta {width}px", capture(page, f"HU04_AUTH12_minha-conta-{width}.png"))
        page.get_by_role("button", name="Sair", exact=True).click()
        page.wait_for_url("**/login.html?saida=1")
        expect(page.locator("#formMessage")).to_have_text("Sessão encerrada.")
        assert context.request.get(baseUrl + "/api/autenticacao/sessao").status == 401
        record("AUTH08 saída real", capture(page, "HU05_AUTH08_logout.png"))
        page.goto(baseUrl + "/paginas/conta/minha-conta.html", wait_until="networkidle")
        page.wait_for_url("**/login.html")
        assert page.locator("#accountName").count() == 0
        record("AUTH08 conta privada após sair redireciona")
        for width in [360, 1280]:
            page.set_viewport_size({"width": width, "height": 900})
            for route in ["cadastro", "login"]:
                page.goto(baseUrl + f"/paginas/conta/{route}.html", wait_until="networkidle")
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth")
                firstField = page.locator("input").first
                firstField.focus()
                page.keyboard.press("Tab")
                focused = page.locator(":focus")
                assert focused.count() == 1
                assert focused.evaluate("element => getComputedStyle(element).outlineStyle") != "none"
                record(f"AUTH12 {route} teclado {width}px", capture(page, f"HU03_AUTH12_{route}-{width}-teclado.png"))
        page.route("**/api/autenticacao/login", lambda route: route.abort())
        page.get_by_label("E-mail", exact=True).fill(email)
        page.get_by_label("Senha", exact=True).fill(password)
        page.get_by_role("button", name="Entrar", exact=True).click()
        expect(page.locator("#formMessage")).to_contain_text("Falha de conexão")
        expect(page.get_by_role("button", name="Entrar", exact=True)).to_be_enabled()
        record("AUTH12 falha de rede recuperável", capture(page, "HU04_AUTH12_falha-rede.png"))
        page.unroute("**/api/autenticacao/login")
        page.goto(baseUrl + "/index.html", wait_until="networkidle")
        products = context.request.get(baseUrl + "/api/produtos")
        assert products.status == 200
        if products.json():
            product = products.json()[0]
            page.goto(baseUrl + f"/produto.html?id={product['id']}", wait_until="networkidle")
            expect(page.locator("#detailTitle")).to_have_text(product["nome"])
            record("AUTH11 catálogo e detalhes públicos", capture(page, "HU02_AUTH11_detalhes-publicos.png"))
        else:
            expect(page.locator("#emptyState")).to_be_visible()
            record("AUTH11 catálogo vazio público; detalhes com dados pendentes", capture(page, "HU01_AUTH11_catalogo-publico.png"))
    except Exception as error:
        results.append({"cenario": "Fluxo de autenticação", "resultado": "falhou", "tipo": type(error).__name__})
        capture(page, "AUTH12_falha.png")
        raise
    finally:
        resultPath = availablePath(resultDirectory, "autenticacao-navegador.json")
        resultPath.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
        browser.close()

print(f"{len(results)} verificações registradas em {resultPath.name}.")
