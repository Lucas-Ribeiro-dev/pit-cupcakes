import argparse
import asyncio
import json
from pathlib import Path

from playwright.async_api import async_playwright, expect


root = Path(__file__).resolve().parents[3]
evidence = root / "docs/testes/evidencias"
results = root / "docs/testes/resultados"


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8093")
    parser.add_argument("--mode", choices=("postgresql", "h2"), required=True)
    args = parser.parse_args()
    if args.base_url != "http://localhost:8093":
        raise ValueError("Use somente o servidor de carrinho isolado em localhost:8093.")
    cases = []
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(channel="msedge", headless=True)
        context = await browser.new_context(viewport={"width": 1280, "height": 900})
        page = await context.new_page()
        errors = []
        page.on("pageerror", lambda error: errors.append(str(error)))

        async def record(name, capture=None):
            item = {"cenario": name, "resultado": "aprovado", "modo": args.mode}
            if capture:
                path = evidence / capture
                await page.screenshot(path=str(path), full_page=True)
                item["evidencia"] = str(path.relative_to(root)).replace("\\", "/")
            cases.append(item)

        try:
            await page.goto(args.base_url + "/index.html", wait_until="networkidle")
            button = page.get_by_role("button", name="Adicionar ao carrinho").first
            await expect(button).to_be_visible()
            await button.click()
            await expect(page.get_by_text("Produto adicionado ao carrinho.").first).to_be_visible()
            await record("CRT01 inclusão pelo catálogo", "HU06_CRT01_adicionar-produto.png")
            await page.get_by_role("link", name="Carrinho").click()
            await expect(page.locator("#cartItems article")).to_have_count(1)
            await record("CRT01 carrinho visitante", "HU06_CRT01_carrinho-visitante.png")
            await page.get_by_role("button", name="Alterar quantidade").click()
            await expect(page.locator("#cartContent")).to_be_visible()
            await page.locator("#cartItems input").fill("3")
            await page.get_by_role("button", name="Alterar quantidade").click()
            await expect(page.locator("#cartItems input")).to_have_value("3")
            await record("CRT03 quantidade alterada", "HU07_CRT03_quantidade.png")
            await page.reload(wait_until="networkidle")
            await expect(page.locator("#cartItems input")).to_have_value("3")
            await record("CRT08 recarga da sessão")
            for width in (360, 1280):
                await page.set_viewport_size({"width": width, "height": 900})
                await page.locator("#cartItems input").focus()
                assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
                await record(f"CRT13 teclado e {width}px", f"HU06_CRT13_carrinho-{width}-teclado.png")
            await page.get_by_role("button", name="Remover").click()
            await expect(page.locator("#cartEmpty")).to_be_visible()
            await record("CRT03 remoção e estado vazio", "HU07_CRT03_removido.png")
            assert not errors, errors
        finally:
            results.joinpath(f"carrinho-navegador-{args.mode}.json").write_text(
                json.dumps({"execucao": "concluída" if not errors and len(cases) == 7 else "parcial",
                            "modo": args.mode, "cenarios": cases, "errosJavaScript": errors}, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8")
            await context.close()
            await browser.close()
    print(json.dumps({"cenarios": len(cases), "modo": args.mode}, ensure_ascii=False))


if __name__ == "__main__":
    asyncio.run(main())
