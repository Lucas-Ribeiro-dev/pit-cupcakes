import argparse
import asyncio
import json
import os
import uuid
from pathlib import Path

from playwright.async_api import async_playwright, expect


root = Path(__file__).resolve().parents[3]
evidence = root / "docs/testes/evidencias"
results = root / "docs/testes/resultados"


async def main():
    parser = argparse.ArgumentParser(
        description="Verificação integrada em navegador real: catálogo, carrinho, login, checkout, pedidos e administração."
    )
    parser.add_argument("--base-url", default="http://localhost:8094")
    parser.add_argument("--admin-email", default=os.environ.get("TEST_BROWSER_ADMIN_EMAIL", "admin.fluxo.isolado@example.com"))
    parser.add_argument("--admin-password", default=os.environ.get("TEST_BROWSER_ADMIN_PASSWORD", "AdminFluxo#2026"))
    args = parser.parse_args()

    if args.base_url != "http://localhost:8094":
        raise ValueError("Use somente o servidor isolado do fluxo integrado em http://localhost:8094.")

    evidence.mkdir(parents=True, exist_ok=True)
    results.mkdir(parents=True, exist_ok=True)

    cases = []
    js_errors = []
    customer_email = f"fluxo-{uuid.uuid4().hex[:10]}@example.com"
    customer_password = "SenhaCliente#2026"

    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(channel="msedge", headless=True)
        context = await browser.new_context(viewport={"width": 1280, "height": 900})
        page = await context.new_page()
        page.on("pageerror", lambda err: js_errors.append(str(err)))

        async def record(cenario, capture=None):
            entry = {"cenario": cenario, "resultado": "aprovado"}
            if capture:
                path = evidence / capture
                await page.screenshot(path=str(path), full_page=True)
                entry["evidencia"] = str(path.relative_to(root)).replace("\\", "/")
            cases.append(entry)

        try:
            # 1. Catalogo e carrinho como visitante (fecha CRT01..CRT13 pendentes)
            await page.goto(args.base_url + "/index.html", wait_until="networkidle")
            add_btn = page.get_by_role("button", name="Adicionar ao carrinho").first
            await expect(add_btn).to_be_visible()
            await add_btn.click()
            await expect(page.get_by_text("Produto adicionado ao carrinho.").first).to_be_visible()
            await record("CRT01 inclusão pelo catálogo", "HU06_CRT01_adicionar-produto.png")

            await page.get_by_role("link", name="Carrinho").click()
            await expect(page.locator("#cartItems article")).to_have_count(1)
            await record("CRT01 carrinho visitante", "HU06_CRT01_carrinho-visitante.png")

            await page.locator("#cartItems input").fill("3")
            await page.get_by_role("button", name="Alterar quantidade").click()
            await expect(page.locator("#cartItems input")).to_have_value("3")
            await record("CRT03 quantidade alterada", "HU07_CRT03_quantidade.png")

            await page.reload(wait_until="networkidle")
            await expect(page.locator("#cartItems input")).to_have_value("3")
            await record("CRT08 recarga da sessão do carrinho")

            for width in (360, 1280):
                await page.set_viewport_size({"width": width, "height": 900})
                await page.locator("#cartItems input").focus()
                assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
                await record(f"CRT13 teclado e {width}px no carrinho", f"HU06_CRT13_carrinho-{width}-teclado.png")

            await page.get_by_role("button", name="Remover").click()
            await expect(page.locator("#cartEmpty")).to_be_visible()
            await record("CRT03 remoção e estado vazio", "HU07_CRT03_removido.png")

            # Readiciona item para seguir ao checkout
            await page.goto(args.base_url + "/index.html", wait_until="networkidle")
            await page.get_by_role("button", name="Adicionar ao carrinho").first.click()
            await expect(page.get_by_text("Produto adicionado ao carrinho.").first).to_be_visible()
            await page.get_by_role("link", name="Carrinho").click()
            await expect(page.locator("#checkoutLink")).to_be_visible()

            # 2. Visitante aciona checkout -> redirecionado para login -> cria conta -> entra -> volta ao checkout com carrinho
            await page.locator("#checkoutLink").click()
            await page.wait_for_url("**/paginas/conta/login.html?destino=checkout")
            await page.locator("#loginLink").click()
            await page.wait_for_url("**/paginas/conta/cadastro.html?destino=checkout")

            await page.fill("#nome", "Cliente Fluxo Integrado")
            await page.fill("#email", customer_email)
            await page.fill("#senha", customer_password)
            await page.fill("#confirmacao", customer_password)
            await page.locator("button[type=submit]").click()
            await expect(page.locator("#formMessage")).to_contain_text("Conta criada com sucesso")

            await page.locator("#loginLink").click()
            await page.wait_for_url("**/paginas/conta/login.html?destino=checkout")
            await page.fill("#email", customer_email)
            await page.fill("#senha", customer_password)
            await page.locator("button[type=submit]").click()
            await page.wait_for_url("**/paginas/compra/checkout.html")
            await expect(page.locator("#checkoutMain")).to_be_visible()
            await record("CRT09 e CHK01 login preservando carrinho e retorno ao checkout", "CHK01_checkout-apos-login-1280.png")

            await page.set_viewport_size({"width": 360, "height": 900})
            assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            await record("CHK01 checkout responsivo 360px", "CHK01_checkout-360.png")
            await page.set_viewport_size({"width": 1280, "height": 900})

            # 3. Cadastra endereco de entrega em Brasilia-DF
            await page.goto(args.base_url + "/paginas/conta/enderecos.html", wait_until="networkidle")
            await expect(page.locator("#privateContent")).to_be_visible()

            async def mock_viacep(route):
                await route.fulfill(
                    status=200,
                    content_type="application/json",
                    body=json.dumps({
                        "cep": "70000001",
                        "logradouro": "Esplanada Fictícia Bloco A",
                        "bairro": "Zona Cívico-Administrativa",
                        "cidade": "Brasília",
                        "uf": "DF"
                    })
                )

            await page.route("**/api/conta/cep/*", mock_viacep)
            await page.fill("#recipient", "Cliente Fluxo Integrado")
            await page.fill("#postalCode", "70000-001")
            await page.click("#lookupButton")
            await expect(page.locator("#street")).to_have_value("Esplanada Fictícia Bloco A")
            await page.fill("#number", "10")
            await page.locator("#addressForm button[type=submit]").click()
            await expect(page.locator("#addressList article")).to_have_count(1)
            await page.unroute("**/api/conta/cep/*")

            # 4. Checkout com ENTREGA, recusa simulada (mantendo carrinho) e aprovacao simulada
            await page.goto(args.base_url + "/paginas/compra/checkout.html", wait_until="networkidle")
            await expect(page.locator("#checkoutMain")).to_be_visible()
            await page.locator("input[name=modalidade][value=ENTREGA]").check()
            await page.click("#reviewButton")
            await expect(page.locator("#revisionSummarySection")).to_be_visible()
            await expect(page.locator("#revisionShippingDisplay")).to_contain_text("8,00")
            await record("CHK02 revisão de entrega com frete calculado no servidor", "CHK02_revisao-entrega.png")

            await page.locator("input[name=resultadoSimulado][value=RECUSADO]").check()
            await page.click("#confirmButton")
            await expect(page.locator("#orderResultSection")).to_be_visible()
            await expect(page.locator("#orderResultSummary")).to_contain_text("recusado")
            await record("CHK03 pagamento simulado recusado e carrinho preservado", "CHK03_pagamento-recusado.png")

            # Nova revisao e confirmacao aprovada
            await page.click("#reviewButton")
            await expect(page.locator("#revisionSummarySection")).to_be_visible()
            await page.locator("input[name=resultadoSimulado][value=APROVADO]").check()
            await page.click("#confirmButton")
            await expect(page.locator("#orderResultSection")).to_be_visible()
            await expect(page.locator("#orderResultSummary")).to_contain_text("aprovado")
            await record("CHK04 pagamento simulado aprovado e pedido criado", "CHK04_pagamento-aprovado.png")

            # 5. Consulta de Meus pedidos e detalhes pelo cliente
            await page.goto(args.base_url + "/paginas/conta/pedidos.html", wait_until="networkidle")
            await expect(page.locator("#ordersList article")).to_have_count(2)
            await record("PED01 listagem de Meus pedidos em 1280px", "PED01_meus-pedidos-1280.png")

            await page.set_viewport_size({"width": 360, "height": 900})
            assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            await record("PED01 listagem de Meus pedidos em 360px", "PED01_meus-pedidos-360.png")
            await page.set_viewport_size({"width": 1280, "height": 900})

            await page.locator("#ordersList article a").first.click()
            await expect(page.locator("#orderDetailContent")).to_be_visible()
            await expect(page.locator("#detailStatus")).to_contain_text("RECEBIDO")
            approved_order_url = page.url
            await record("PED02 detalhes do pedido aprovado", "PED02_detalhe-pedido.png")

            # 6. Cliente tenta acessar tela administrativa -> Acesso negado
            await page.goto(args.base_url + "/admin/produtos.html", wait_until="networkidle")
            await expect(page.locator("#adminForbidden")).to_be_visible()
            await record("ADM01 bloqueio de tela administrativa para perfil CLIENTE", "ADM01_cliente-acesso-negado.png")

            # 7. Logout do cliente e login como ADMIN
            await page.get_by_role("button", name="Sair").click()
            await page.wait_for_url("**/paginas/conta/login.html?saida=1")
            await page.goto(args.base_url + "/paginas/conta/login.html?destino=admin-produtos", wait_until="networkidle")
            await page.fill("#email", args.admin_email)
            await page.fill("#senha", args.admin_password)
            await page.locator("button[type=submit]").click()
            await page.wait_for_url("**/admin/produtos.html")
            await expect(page.locator("#adminContent")).to_be_visible()

            await page.fill("#productName", "Cupcake Admin Fluxo Teste")
            await page.fill("#productDescription", "Criado pelo administrador durante verificação de navegador.")
            await page.fill("#productPrice", "16.50")
            await page.select_option("#productImage", "/assets/img/cupcake-chocolate.svg")
            await page.click("#saveProductButton")
            await expect(page.locator("#productFormMessage")).to_contain_text("Produto cadastrado com sucesso.")
            await record("ADM02 cadastro de produto pelo ADMIN em 1280px", "ADM02_admin-produtos-1280.png")

            await page.set_viewport_size({"width": 360, "height": 900})
            assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            await record("ADM02 administração de produtos em 360px", "ADM02_admin-produtos-360.png")
            await page.set_viewport_size({"width": 1280, "height": 900})

            # 8. Atendimento do pedido pelo ADMIN ate CONCLUIDO
            await page.goto(args.base_url + "/admin/pedidos.html", wait_until="networkidle")
            await expect(page.locator("#adminOrdersContent")).to_be_visible()
            await page.locator("#adminOrdersList article button").first.click()
            await expect(page.locator("#adminSelectedOrderSection")).to_be_visible()

            for next_status in ("EM_PREPARACAO", "SAIU_PARA_ENTREGA", "CONCLUIDO"):
                await page.select_option("#nextStatusSelect", next_status)
                await page.click("#updateStatusButton")
                await expect(page.locator("#adminSelectedOrderBody")).to_contain_text(next_status)

            await record("ADM03 atendimento de pedido pelo ADMIN até CONCLUIDO em 1280px", "ADM03_admin-atendimento-1280.png")
            await page.set_viewport_size({"width": 360, "height": 900})
            assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            await record("ADM03 atendimento de pedidos em 360px", "ADM03_admin-atendimento-360.png")
            await page.set_viewport_size({"width": 1280, "height": 900})

            # 9. Cliente entra novamente e acompanha pedido CONCLUIDO com historico preservado
            await page.get_by_role("button", name="Sair").click()
            await page.wait_for_url("**/paginas/conta/login.html?saida=1")
            await page.fill("#email", customer_email)
            await page.fill("#senha", customer_password)
            await page.locator("button[type=submit]").click()
            await page.wait_for_url("**/paginas/conta/minha-conta.html")

            await page.goto(approved_order_url, wait_until="networkidle")
            await expect(page.locator("#detailStatus")).to_contain_text("CONCLUIDO")
            await record("PED03 acompanhamento pelo cliente após conclusão do pedido", "PED03_acompanhamento-concluido.png")

            assert not js_errors, js_errors
        finally:
            results.joinpath("fluxo-completo-navegador.json").write_text(
                json.dumps(
                    {
                        "execucao": "concluída" if not js_errors and len(cases) == 19 else "parcial",
                        "baseUrl": args.base_url,
                        "totalCenarios": len(cases),
                        "cenarios": cases,
                        "errosJavaScript": js_errors,
                    },
                    ensure_ascii=False,
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            await context.close()
            await browser.close()

    print(json.dumps({"cenarios": len(cases), "errosJavaScript": len(js_errors)}, ensure_ascii=False))


if __name__ == "__main__":
    asyncio.run(main())

