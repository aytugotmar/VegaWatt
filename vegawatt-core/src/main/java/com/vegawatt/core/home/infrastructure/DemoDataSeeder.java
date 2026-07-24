package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.access.domain.HomeAccessService;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.common.config.BootstrapAdminProperties;
import com.vegawatt.core.home.application.RegisterHomeCommand;
import com.vegawatt.core.home.application.RegisterHomeUseCase;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Dev-only convenience: seeds two demo homes with realistic appliances under the bootstrap admin
 * account so a fresh local environment has something to look at. Runs only under the "dev" Spring
 * profile and only if the admin doesn't already own any home — unlike the destructive migration
 * this replaces, it never deletes or touches existing data. Reuses {@link RegisterHomeUseCase} (the
 * same path the real registration endpoint uses) so catalog defaults and snapshot fields are
 * resolved correctly instead of being hand-rolled in SQL. */
@Profile("dev")
@Component
class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final HomeAccessService homeAccessService;
    private final RegisterHomeUseCase registerHomeUseCase;
    private final ApplianceCatalogRepository applianceCatalogRepository;

    DemoDataSeeder(UserRepository userRepository, BootstrapAdminProperties bootstrapAdminProperties,
                   HomeAccessService homeAccessService, RegisterHomeUseCase registerHomeUseCase,
                   ApplianceCatalogRepository applianceCatalogRepository) {
        this.userRepository = userRepository;
        this.bootstrapAdminProperties = bootstrapAdminProperties;
        this.homeAccessService = homeAccessService;
        this.registerHomeUseCase = registerHomeUseCase;
        this.applianceCatalogRepository = applianceCatalogRepository;
    }

    @Override
    public void run(String... args) {
        Optional<User> admin = userRepository.findByEmail(bootstrapAdminProperties.email());
        if (admin.isEmpty()) {
            log.info("Skipping demo data seed: no bootstrap admin user configured yet");
            return;
        }

        UUID adminId = admin.get().id();
        if (!homeAccessService.accessibleHomeIds(adminId).isEmpty()) {
            return;
        }

        String contactEmail = admin.get().email();

        registerHomeUseCase.execute(new RegisterHomeCommand(
                "Gül Apartmanı No: 4", contactEmail, new BigDecimal("350.0000"),
                new BigDecimal("1500.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Buzdolabı", "REFRIGERATOR", "250.00", "30.00", "160.00"),
                        appliance("Çamaşır Makinesi", "WASHING_MACHINE", "2200.00", "0.00", "2000.00"),
                        appliance("Salon Kliması", "AIR_CONDITIONER", "2500.00", "400.00", "2100.00"),
                        appliance("Kahve Makinesi", "COFFEE_MACHINE", "1500.00", "0.00", "1400.00"),
                        appliance("Wi-Fi Router", "ROUTER", "30.00", "8.00", "15.00")),
                adminId));

        registerHomeUseCase.execute(new RegisterHomeCommand(
                "Yalı Dairesi - Kadıköy", contactEmail, new BigDecimal("500.0000"),
                new BigDecimal("2200.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Bulaşık Makinesi", "DISHWASHER", "2000.00", "0.00", "1800.00"),
                        appliance("OLED TV", "TELEVISION", "300.00", "1.00", "220.00")),
                adminId));

        log.info("Seeded demo homes for {}", contactEmail);
    }

    private RegisterHomeCommand.ApplianceCommand appliance(String name, String catalogCode,
                                                            String safePowerLimitWatt, String simulationMinWatt,
                                                            String simulationMaxWatt) {
        ApplianceCatalogItem catalogItem = applianceCatalogRepository.findEnabledByCode(catalogCode)
                .orElseThrow(() -> new IllegalStateException("Demo seed catalog code not found: " + catalogCode));
        return new RegisterHomeCommand.ApplianceCommand(name, catalogCode, new BigDecimal(safePowerLimitWatt),
                new BigDecimal(simulationMinWatt), new BigDecimal(simulationMaxWatt), catalogItem.id());
    }
}
