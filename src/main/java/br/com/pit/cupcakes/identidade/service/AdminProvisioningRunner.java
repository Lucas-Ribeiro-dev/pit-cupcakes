package br.com.pit.cupcakes.identidade.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !test-pg")
public class AdminProvisioningRunner implements CommandLineRunner {

    private final Environment environment;
    private final AdminProvisioningService provisioningService;

    public AdminProvisioningRunner(Environment environment, AdminProvisioningService provisioningService) {
        this.environment = environment;
        this.provisioningService = provisioningService;
    }

    @Override
    public void run(String... args) {
        String email = readProperty("ADMIN_EMAIL", "pit.admin.email");
        String name = readProperty("ADMIN_NAME", "pit.admin.name");
        String password = readProperty("ADMIN_PASSWORD", "pit.admin.password");

        boolean anyConfigured = !email.isBlank() || !name.isBlank() || (password != null && !password.isEmpty());
        if (!anyConfigured) {
            return;
        }
        if (email.isBlank() || name.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalStateException("Provisionamento administrativo incompleto: defina simultaneamente ADMIN_EMAIL, ADMIN_NAME e ADMIN_PASSWORD.");
        }
        provisioningService.provisionAdmin(name, email, password);
    }

    private String readProperty(String envKey, String propKey) {
        String prop = environment.getProperty(propKey);
        if (prop != null && !prop.isEmpty()) {
            return prop;
        }
        String env = environment.getProperty(envKey);
        return env != null ? env : "";
    }
}

