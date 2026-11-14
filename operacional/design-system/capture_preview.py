from pathlib import Path

from playwright.sync_api import sync_playwright


preview_dir = Path(__file__).resolve().parent
preview_url = (preview_dir / "index.html").as_uri()

with sync_playwright() as playwright:
    browser = playwright.chromium.launch(
        headless=True,
        executable_path="C:/Program Files/Google/Chrome/Application/chrome.exe",
    )
    for width in (360, 1280):
        page = browser.new_page(viewport={"width": width, "height": 800}, device_scale_factor=1)
        page.goto(preview_url, wait_until="load")
        page.evaluate("document.fonts.ready")
        result = page.evaluate("""() => ({
            viewport: innerWidth,
            document: document.documentElement.scrollWidth,
            lilita: document.fonts.check('16px "Lilita One"'),
            varela: document.fonts.check('16px "Varela Round"'),
            images: [...document.images].every(image => image.complete && image.naturalWidth > 0)
        })""")
        assert result["document"] <= result["viewport"], result
        assert result["lilita"] and result["varela"] and result["images"], result
        assert page.get_by_role("button", name="Indisponível").first.is_disabled()
        page.get_by_role("radio", name="Recusado Preserva os itens no carrinho.").check()
        assert page.get_by_role("radio", name="Recusado Preserva os itens no carrinho.").is_checked()
        page.get_by_label("E-mail com erro").focus()
        assert page.evaluate("document.activeElement.id") == "preview-error-field"
        assert page.get_by_label("E-mail com erro").get_attribute("aria-describedby") == "preview-error-text"
        page.get_by_role("radio", name="Aprovado Confirma o pedido e limpa o carrinho.").check()
        page.get_by_label("E-mail com erro").evaluate("element => element.blur()")
        page.screenshot(path=str(preview_dir / f"preview-{width}.png"), full_page=True)
        print(f"{width}px: {result}")
        page.close()
    browser.close()
