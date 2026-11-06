import hashlib
from datetime import datetime, timezone
import json
import xml.etree.ElementTree as elementTree
from pathlib import Path


projectRoot = Path(__file__).resolve().parents[3]
reportDirectory = projectRoot / "target/surefire-reports"
groups = {
    "sem_postgresql_externo": ["AddressValidationTest", "AddressApiTest", "PostalCodeClientTest", "AuthenticationSecurityTest", "RegistrationServiceTest", "ProductControllerTest", "ProductServiceTest", "ProductRepositoryTest", "DemoDataLoaderTest", "PostgresTestDatabaseGuardTest"],
    "postgresql": ["AddressShippingPostgresTest", "AddressRestartPostgresTest", "AuthenticationPostgresIntegrationTest", "AuthenticationRestartPostgresTest", "ProductPostgresIntegrationTest"]
}
summary = {}
for group, names in groups.items():
    reports = []
    for name in names:
        paths = list(reportDirectory.glob(f"TEST-*.{name}.xml"))
        if len(paths) != 1:
            raise RuntimeError(f"Relatório ausente ou ambíguo: {name}")
        path = paths[0]
        root = elementTree.parse(path).getroot()
        reports.append({
            "classe": name,
            "testes": int(root.attrib["tests"]), "falhas": int(root.attrib["failures"]),
            "erros": int(root.attrib["errors"]), "ignorados": int(root.attrib["skipped"]),
            "instante_relatorio": root.attrib.get("timestamp"),
            "arquivo_modificado_em_utc": datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat(),
            "arquivo": str(path.relative_to(projectRoot)).replace("\\", "/"),
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "casos": [case.attrib["name"] for case in root.findall("testcase")]
        })
    summary[group] = {"total": {key: sum(report[key] for report in reports) for key in ("testes", "falhas", "erros", "ignorados")}, "classes": reports}
postgresTotals = summary["postgresql"]["total"]
postgresApproved = not any(postgresTotals[key] for key in ("falhas", "erros", "ignorados"))
browserPath = projectRoot / "docs/testes/resultados/enderecos-navegador.json"
browserCompleted = browserPath.exists() and json.loads(browserPath.read_text(encoding="utf-8")).get("execucao") == "concluída"
summary["limites"] = [
    "H2 não comprova migrações ou restrições PostgreSQL.",
    "Relatórios PostgreSQL atuais conferidos sem reexecutar os testes: todos aprovados." if postgresApproved else "Relatórios PostgreSQL contêm falhas, erros ou casos ignorados; não representam aprovação integral.",
    "ViaCEP usa respostas/futuros simulados; não foi realizada consulta real de fumaça.",
    "Navegador com servidor real concluído; apenas GET /api/conta/cep/* interceptado para controlar o ViaCEP. Resultado e capturas separados da interface integralmente simulada anterior." if browserCompleted else "Não há relatório concluído de navegador com servidor real. A interface integralmente simulada anterior não comprova persistência."
]
output = projectRoot / "docs/testes/resultados/enderecos-automatizados.json"
output.write_text(json.dumps(summary, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
print(json.dumps({key: value["total"] for key, value in summary.items() if isinstance(value, dict)}, ensure_ascii=False))
