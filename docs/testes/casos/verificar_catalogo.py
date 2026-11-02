import json
import sys
import traceback
from pathlib import Path

from playwright.sync_api import sync_playwright, expect


projectRoot = Path(__file__).resolve().parents[3]
evidenceDirectory = projectRoot / "docs/testes/evidencias"
resultPath = projectRoot / "docs/testes/resultados/verificacao-navegador.json"
layoutOnly = "--layout-only" in sys.argv
results = [result for result in json.loads(resultPath.read_text(encoding="utf-8"))
           if not result["cenario"].startswith("TC09")] if layoutOnly else []


def capture(page, name):
    evidencePath = evidenceDirectory / name
    attempt = 1
    while evidencePath.exists():
        evidencePath = evidenceDirectory / f"{Path(name).stem}-reexecucao{attempt}.png"
        attempt += 1
    page.screenshot(path=str(evidencePath), full_page=True)


def check(name, action):
    if layoutOnly and not name.startswith("TC09"):
        return
    try:
        action()
        results.append({"cenario": name, "resultado": "aprovado"})
    except Exception as error:
        results.append({"cenario": name, "resultado": "falhou", "diagnostico": traceback.format_exc()})


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(channel="msedge", headless=True)
    context = browser.new_context(viewport={"width": 1280, "height": 900})
    page = context.new_page()
    page.goto("http://localhost:8080/index.html", wait_until="networkidle")
    products = page.request.get("http://localhost:8080/api/produtos").json()

    def verifyCatalog():
        expect(page.locator(".product-card")).to_have_count(len(products))
        assert products and [product["id"] for product in products] == sorted(product["id"] for product in products)
        assert all(set(product) == {"id", "nome", "descricao", "preco", "imagemUrl", "disponivel"} for product in products)
        capture(page, "HU01_TC01_catalogo-1280-verificacao.png")

    check("TC01 catálogo real e contrato público", verifyCatalog)

    def verifyDetail():
        page.get_by_role("link", name="Ver detalhes").first.click()
        page.wait_for_load_state("networkidle")
        expect(page.locator("#detailTitle")).to_have_text(products[0]["nome"])
        expect(page.locator("#detailPrice")).to_contain_text(products[0]["preco"].replace(".", ","))
        capture(page, "HU02_TC03_detalhes-1280-verificacao.png")

    check("TC03 detalhes reais", verifyDetail)

    def verifyMissing():
        response = page.request.get("http://localhost:8080/api/produtos/999999")
        assert response.status == 404 and response.json()["codigo"] == "PRODUTO_NAO_ENCONTRADO"
        page.goto("http://localhost:8080/produto.html?id=999999", wait_until="networkidle")
        expect(page.locator("#notFoundState")).to_be_visible()
        capture(page, "HU02_TC04_produto-inexistente-verificacao.png")

    check("TC04 produto inexistente", verifyMissing)

    def verifyInvalid():
        requests = []
        page.on("request", lambda request: requests.append(request.url) if "/api/produtos" in request.url else None)
        for identifier in ["", "abc", "0", "-1"]:
            requests.clear()
            page.goto("http://localhost:8080/produto.html" + ("?id=" + identifier if identifier else ""), wait_until="networkidle")
            expect(page.locator("#errorState")).to_be_visible()
            expect(page.locator("#retryButton")).to_be_hidden()
            assert not requests
        for identifier in ["0", "-1", "abc"]:
            response = page.request.get("http://localhost:8080/api/produtos/" + identifier)
            assert response.status == 400 and response.json()["codigo"] == "ID_INVALIDO"
        capture(page, "HU02_TC05_id-invalido-verificacao.png")

    check("TC05 IDs ausente, alfabético, zero e negativo sem fetch inválido", verifyInvalid)

    def verifyAvailability():
        product = next(product for product in products if not product["disponivel"])
        page.goto(f"http://localhost:8080/produto.html?id={product['id']}", wait_until="networkidle")
        expect(page.locator("#detailBadge")).to_have_text("Indisponível")
        assert page.get_by_role("button").filter(has_text="Comprar").count() == 0
        capture(page, "HU02_TC06_indisponibilidade-verificacao.png")
        product = next(product for product in products if not product.get("imagemUrl"))
        page.goto(f"http://localhost:8080/produto.html?id={product['id']}", wait_until="networkidle")
        image = page.locator("#detailImage")
        assert image.evaluate("image => image.complete && image.naturalWidth > 0")
        assert "placeholder" in image.get_attribute("src") or "sem-imagem" in image.get_attribute("src")
        capture(page, "HU02_TC06_imagem-substituta-verificacao.png")

    check("TC06 indisponibilidade e imagem substituta", verifyAvailability)

    for screen, path, visibleSelector, story in [
        ("catalogo", "index.html", "#catalogGrid", "HU01"),
        ("detalhes", f"produto.html?id={products[0]['id']}", "#detailCard", "HU02"),
    ]:
        def verifyNetwork(screen=screen, path=path, visibleSelector=visibleSelector, story=story):
            page.route("**/api/produtos**", lambda route: route.abort("failed"))
            page.goto("http://localhost:8080/" + path, wait_until="networkidle")
            expect(page.locator("#errorState")).to_be_visible()
            if screen == "catalogo":
                expect(page.locator("#emptyState")).to_be_hidden()
            capture(page, f"{story}_TC07_{screen}-falha-rede-verificacao.png")
            page.unroute("**/api/produtos**")
            page.get_by_role("button", name="Tentar novamente").click()
            expect(page.locator(visibleSelector)).to_be_visible()
            capture(page, f"{story}_TC07_{screen}-recuperado-verificacao.png")

        check(f"TC07 {screen}: falha real de requisição e tentar novamente", verifyNetwork)

    for width in [360, 1280]:
        for screen, path, story in [("catalogo", "index.html", "HU01"), ("detalhes", f"produto.html?id={products[0]['id']}", "HU02")]:
            def verifyLayout(width=width, screen=screen, path=path, story=story):
                page.set_viewport_size({"width": width, "height": 900})
                page.goto("http://localhost:8080/" + path, wait_until="networkidle")
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth")
                page.keyboard.press("Tab")
                assert page.evaluate("document.activeElement.tagName") == "A"
                outline = page.evaluate("getComputedStyle(document.activeElement).outlineStyle")
                assert outline != "none"
                page.keyboard.press("Tab")
                assert page.evaluate("document.activeElement.tagName") == "A"
                capture(page, f"{story}_TC09_{screen}-{width}-teclado-verificacao.png")
                with page.expect_navigation(wait_until="networkidle"):
                    page.keyboard.press("Enter")
                assert "produto.html" in page.url if screen == "catalogo" else "index.html" in page.url

            check(f"TC09 {screen} {width}px: largura, foco e ativação por teclado", verifyLayout)

    browser.close()

resultPath.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(results, ensure_ascii=True, indent=2))
