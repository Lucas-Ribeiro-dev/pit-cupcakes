import argparse
import asyncio
import functools
import json
import threading
import uuid
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from playwright.async_api import async_playwright, expect


projectRoot = Path(__file__).resolve().parents[3]
evidenceDirectory = projectRoot / "docs/testes/evidencias"
resultDirectory = projectRoot / "docs/testes/resultados"
results = []


def availablePath(directory, name):
    path = directory / name
    attempt = 1
    while path.exists():
        path = directory / f"{Path(name).stem}-reexecucao{attempt}{Path(name).suffix}"
        attempt += 1
    return path


class QuietHandler(SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass


async def verify(baseUrl, simulated):
    suffix = "-api-simulada" if simulated else ""
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(channel="msedge", headless=True)
        context = await browser.new_context(viewport={"width": 1280, "height": 900})
        page = await context.new_page()
        pageErrors = []
        page.on("pageerror", lambda error: pageErrors.append(str(error)))
        addresses = []

        async def fulfill(route, body, status=200):
            await route.fulfill(status=status, content_type="application/json", body=json.dumps(body), headers={"Cache-Control": "no-store"})

        async def mockApi(route):
            request = route.request
            path = request.url.split("/api/", 1)[1]
            if path == "autenticacao/sessao":
                await fulfill(route, {"id": 1, "nome": "Pessoa fictícia — API simulada", "email": "simulado@example.com", "perfil": "CLIENTE"})
            elif path == "autenticacao/csrf":
                await fulfill(route, {"token": "simulado", "headerName": "X-CSRF-TOKEN", "parameterName": "_csrf"})
            elif path.startswith("conta/enderecos"):
                identifier = path.split("/")[-1].split("?")[0]
                if request.method == "GET":
                    await fulfill(route, next(item for item in addresses if str(item["id"]) == identifier) if identifier.isdigit() else addresses)
                elif request.method == "POST":
                    data = request.post_data_json
                    data.update(id=len(addresses)+1, versao=0)
                    data["cep"] = data["cep"].replace("-", "")
                    addresses.append(data)
                    await fulfill(route, data, 201)
                elif request.method == "PUT":
                    current = next(item for item in addresses if str(item["id"]) == identifier)
                    data = request.post_data_json
                    if data["versao"] != current["versao"]:
                        await fulfill(route, {"codigo": "ENDERECO_DESATUALIZADO", "mensagem": "Endereço alterado em outra sessão."}, 409)
                    else:
                        data.update(id=current["id"], versao=current["versao"]+1)
                        current.update(data)
                        await fulfill(route, current)
                elif request.method == "DELETE":
                    addresses[:] = [item for item in addresses if str(item["id"]) != identifier]
                    await route.fulfill(status=204)
            elif path == "conta/frete/cotacao":
                data = request.post_data_json
                if data["modalidade"] == "RETIRADA":
                    await fulfill(route, {"modalidade": "RETIRADA", "valor": "0.00"})
                else:
                    await fulfill(route, {"codigo": "ENTREGA_NAO_CONFIGURADA", "mensagem": "Entrega ainda não configurada. Retirada gratuita disponível."}, 422)
            else:
                await fulfill(route, {"codigo": "NAO_SIMULADO", "mensagem": "Rota não simulada."}, 404)

        if simulated:
            await context.route("**/api/**", mockApi)
        else:
            email = f"endereco-navegador-{uuid.uuid4()}@example.com"
            password = " senha fictícia "
            await page.goto(baseUrl + "/paginas/conta/cadastro.html", wait_until="networkidle")
            await page.get_by_label("Nome", exact=True).fill("Cliente Endereço Fictício")
            await page.get_by_label("E-mail", exact=True).fill(email)
            await page.get_by_label("Senha", exact=True).fill(password)
            await page.get_by_label("Confirmar senha", exact=True).fill(password)
            await page.get_by_role("button", name="Criar conta", exact=True).click()
            await expect(page.locator("#formMessage")).to_contain_text("Conta criada")
            await page.goto(baseUrl + "/paginas/conta/login.html?destino=enderecos", wait_until="networkidle")
            await page.get_by_label("E-mail", exact=True).fill(email)
            await page.get_by_label("Senha", exact=True).fill(password)
            await page.get_by_role("button", name="Entrar", exact=True).click()
            await page.wait_for_url("**/enderecos.html")

        await page.goto(baseUrl + "/paginas/conta/enderecos.html", wait_until="networkidle")
        await expect(page.locator("#addressList")).to_contain_text("Nenhum endereço")

        async def capture(name):
            if simulated:
                await page.evaluate("""() => {
                  let notice = document.querySelector('#simulationNotice');
                  if (!notice) { notice = document.createElement('p'); notice.id = 'simulationNotice'; document.querySelector('main').prepend(notice); }
                  notice.textContent = 'TESTE COM API SIMULADA — não comprova persistência ou PostgreSQL';
                }""")
            path = availablePath(evidenceDirectory, name + suffix + ".png")
            await page.screenshot(path=str(path), full_page=True)
            return str(path.relative_to(projectRoot)).replace("\\", "/")

        async def record(name, image=None):
            results.append({"cenario": name, "resultado": "aprovado", "modo": "API simulada" if simulated else "servidor PostgreSQL; CEP simulado", "evidencia": image})

        await record("ADR09 lista vazia", await capture("ADR09_lista-vazia"))
        if not simulated:
            session = await context.request.get(baseUrl + "/api/autenticacao/sessao")
            assert session.status == 200 and "no-store" in session.headers["cache-control"]
            assert (await session.json())["email"] == email
            cookie = next(cookie for cookie in await context.cookies() if cookie["name"] == "JSESSIONID")
            assert cookie["httpOnly"] and cookie["sameSite"] == "Lax"
            assert (await context.request.post(baseUrl + "/api/conta/frete/cotacao", data={"modalidade": "RETIRADA"})).status == 403
            visitor = await browser.new_context()
            assert (await visitor.request.get(baseUrl + "/api/conta/enderecos")).status == 401
            await visitor.close()
            await record("ADR05 cadastro/login reais, sessão privada, cookie, visitante e CSRF", await capture("ADR05_sessao-autenticada"))

        async def mockPostal(route):
            postalCode = route.request.url.rsplit("/", 1)[1].replace("-", "")
            if postalCode in ("70000001", "70000002"):
                await asyncio.sleep(0.7)
                await fulfill(route, {"cep": postalCode, "logradouro": "Rua retornada", "bairro": "Bairro retornado", "cidade": "Brasília", "uf": "DF"})
            elif postalCode == "70000003":
                await fulfill(route, {"cep": postalCode, "logradouro": "Rua atual", "bairro": "Bairro atual", "cidade": "Região administrativa", "uf": "DF"})
            else:
                codes = {"70000004": (404, "CEP_NAO_ENCONTRADO", "CEP não encontrado. Confira ou preencha manualmente."),
                         "70000005": (502, "SERVICO_CEP_INDISPONIVEL", "Consulta de CEP indisponível. Você pode preencher manualmente."),
                         "70000006": (504, "CONSULTA_CEP_EXPIRADA", "A consulta de CEP demorou demais. Preencha manualmente ou tente novamente.")}
                status, code, message = codes[postalCode]
                await fulfill(route, {"codigo": code, "mensagem": message}, status)

        await context.route("**/api/conta/cep/*", mockPostal)
        await page.locator("#postalCode").fill("70000001")
        await page.locator("#postalCode").press("Tab")
        await expect(page.locator("#postalMessage")).to_contain_text("Buscando")
        await page.locator("#street").fill("Rua digitada durante consulta")
        await expect(page.locator("#postalMessage")).to_contain_text("Consulta concluída")
        await expect(page.locator("#street")).to_have_value("Rua digitada durante consulta")
        await record("ADR07 preserva campo editado durante consulta", await capture("ADR07_campo-preservado"))
        await page.locator("#postalCode").fill("70000002")
        await page.locator("#postalCode").press("Tab")
        await expect(page.locator("#postalMessage")).to_contain_text("Buscando")
        await page.locator("#postalCode").fill("70000003")
        await page.locator("#postalCode").press("Tab")
        await expect(page.locator("#street")).to_have_value("Rua atual")
        await page.wait_for_timeout(850)
        await expect(page.locator("#street")).to_have_value("Rua atual")
        await record("ADR07 resposta de CEP anterior ignorada", await capture("ADR07_cep-atual"))
        for postalCode, expected in [("70000004", "CEP não encontrado"), ("70000005", "indisponível"), ("70000006", "demorou")]:
            await page.locator("#postalCode").fill(postalCode)
            await page.get_by_role("button", name="Buscar CEP", exact=True).click()
            await expect(page.locator("#postalMessage")).to_contain_text(expected)
            await record(f"ADR06 erro recuperável {postalCode}", await capture(f"ADR06_cep-{postalCode}"))

        await page.locator("#recipient").fill("Pessoa de teste")
        await page.locator("#street").fill("Rua fictícia <b>texto</b>")
        await page.locator("#number").fill("S/N")
        await page.locator("#neighborhood").fill("Bairro fictício")
        await page.locator("#city").fill("Brasília")
        await page.locator("#state").fill("DF")
        await page.get_by_role("button", name="Salvar endereço", exact=True).click()
        await expect(page.locator("#addressList")).to_contain_text("Rua fictícia <b>texto</b>")
        assert await page.locator("#addressList b").count() == 0
        await record("ADR01 salvar manualmente após falha do CEP; texto seguro", await capture("ADR01_endereco-salvo"))
        for width in (360, 1280):
            await page.set_viewport_size({"width": width, "height": 900})
            await page.reload(wait_until="networkidle")
            await expect(page.locator("#addressList")).to_contain_text("Rua fictícia")
            assert await page.evaluate("document.documentElement.scrollWidth <= innerWidth")
            await page.locator("#recipient").focus()
            await page.keyboard.press("Tab")
            await expect(page.locator("#postalCode")).to_be_focused()
            await record(f"ADR09 recarga e teclado {width}px", await capture(f"ADR09_enderecos-{width}-teclado"))

        await page.get_by_role("button", name="Consultar retirada", exact=True).click()
        await expect(page.locator("#shippingMessage")).to_contain_text("R$ 0,00")
        await record("FRT04 retirada gratuita", await capture("FRT04_retirada"))
        await page.get_by_role("button", name="Consultar entrega", exact=True).click()
        await expect(page.locator("#shippingMessage")).to_contain_text("Entrega ainda não configurada")
        await record("FRT04 entrega sem configuração", await capture("FRT04_sem-configuracao"))
        await page.get_by_role("button", name="Editar", exact=True).click()
        await expect(page.locator("#formTitle")).to_have_text("Editar endereço")
        await expect(page.locator("#shippingMessage")).to_have_text("")
        await page.locator("#number").fill("2")
        await page.get_by_role("button", name="Salvar endereço", exact=True).click()
        await expect(page.locator("#addressList")).to_contain_text(", 2")
        await record("ADR04 editar endereço", await capture("ADR04_endereco-editado"))

        await page.get_by_role("button", name="Editar", exact=True).click()
        await expect(page.locator("#number")).to_have_value("2")
        await page.locator("#number").fill("Meu texto preservado")
        if simulated:
            addresses[0]["versao"] += 1
        else:
            stored = (await (await context.request.get(baseUrl + "/api/conta/enderecos")).json())[0]
            stored.pop("id")
            stored["numero"] = "Outra sessão"
            csrf = await (await context.request.get(baseUrl + "/api/autenticacao/csrf")).json()
            addressId = (await (await context.request.get(baseUrl + "/api/conta/enderecos")).json())[0]["id"]
            response = await context.request.put(baseUrl + f"/api/conta/enderecos/{addressId}", data=stored, headers={csrf["headerName"]: csrf["token"]})
            assert response.status == 200
        await page.get_by_role("button", name="Salvar endereço", exact=True).click()
        await expect(page.locator("#formMessage")).to_contain_text("Seu texto foi preservado")
        await expect(page.locator("#number")).to_have_value("Meu texto preservado")
        await record("ADR04 conflito não perde texto digitado", await capture("ADR04_conflito-preserva-texto"))
        if not simulated:
            response = await context.request.delete(baseUrl + f"/api/conta/enderecos/{addressId}?versao={stored['versao']}", headers={csrf["headerName"]: csrf["token"]})
            assert response.status == 409 and (await response.json())["codigo"] == "ENDERECO_DESATUALIZADO"
            current = await context.request.get(baseUrl + f"/api/conta/enderecos/{addressId}")
            assert current.status == 200 and (await current.json())["numero"] == "Outra sessão"
            await record("ADR04 DELETE com versão antiga retorna 409 e preserva endereço")
        await page.get_by_role("button", name="Cancelar edição / limpar", exact=True).click()
        page.once("dialog", lambda dialog: dialog.dismiss())
        await page.get_by_role("button", name="Remover", exact=True).click()
        await expect(page.locator("#addressList article")).to_have_count(1)
        page.once("dialog", lambda dialog: dialog.accept())
        await page.get_by_role("button", name="Remover", exact=True).click()
        await expect(page.locator("#addressList")).to_contain_text("Nenhum endereço")
        await record("ADR04 remoção exige confirmação", await capture("ADR04_endereco-removido"))
        if not simulated:
            assert (await context.request.get(baseUrl + f"/api/conta/enderecos/{addressId}")).status == 404
            await page.get_by_role("button", name="Sair", exact=True).click()
            await page.wait_for_url("**/login.html?saida=1")
            assert (await context.request.get(baseUrl + "/api/autenticacao/sessao")).status == 401
            await page.goto(baseUrl + "/paginas/conta/enderecos.html", wait_until="networkidle")
            await page.wait_for_url("**/login.html?destino=enderecos")
            await record("ADR05 logout e tela privada direcionam ao login", await capture("ADR05_logout-rota-privada"))
        assert not pageErrors, pageErrors
        await record("ADR09 sem erros JavaScript")
        await context.close()
        await browser.close()


async def main():
    parser = argparse.ArgumentParser(description="Verifica endereços. CEP sempre simulado; --simulate-api também simula persistência e autenticação.")
    parser.add_argument("--base-url", default="http://localhost:8092")
    parser.add_argument("--simulate-api", action="store_true")
    args = parser.parse_args()
    server = None
    completed = False
    try:
        baseUrl = args.base_url
        if args.simulate_api:
            handler = functools.partial(QuietHandler, directory=str(projectRoot / "src/main/resources/static"))
            server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
            threading.Thread(target=server.serve_forever, daemon=True).start()
            baseUrl = f"http://127.0.0.1:{server.server_port}"
        elif baseUrl != "http://localhost:8092":
            raise ValueError("Use somente o servidor isolado localhost:8092.")
        await verify(baseUrl, args.simulate_api)
        completed = True
    finally:
        if server:
            server.shutdown()
            server.server_close()
        name = "enderecos-interface-simulada.json" if args.simulate_api else "enderecos-navegador.json"
        output = {"execucao": "concluída" if completed else "interrompida por falha", "modo": "API integralmente simulada" if args.simulate_api else "PostgreSQL real; CEP simulado", "cenarios": results}
        resultDirectory.joinpath(name).write_text(json.dumps(output, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
        print(json.dumps({"aprovados": len(results), "relatorio": name}, ensure_ascii=False))


if __name__ == "__main__":
    asyncio.run(main())
