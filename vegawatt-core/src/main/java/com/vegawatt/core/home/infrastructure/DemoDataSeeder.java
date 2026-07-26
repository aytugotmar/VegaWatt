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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Dev-only convenience: seeds two demo homes with realistic appliances under the bootstrap admin
 * account so a fresh local environment has something to look at. Runs only under the "dev" Spring
 * profile and only if the admin doesn't already own any home — unlike the destructive migration
 * this replaces, it never deletes or touches existing data. Reuses {@link RegisterHomeUseCase} (the
 * same path the real registration endpoint uses) so catalog defaults and snapshot fields are
 * resolved correctly instead of being hand-rolled in SQL. */
@Order(2)
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
        if (homeAccessService.accessibleHomeIds(adminId).size() >= 8) {
            return;
        }

        String contactEmail = admin.get().email();

        // Home 1
        registerHomeIfMissing("Gül Apartmanı No: 4", contactEmail, new BigDecimal("350.0000"),
                new BigDecimal("1500.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Buzdolabı", "REFRIGERATOR", "250.00", "30.00", "160.00"),
                        appliance("Çamaşır Makinesi", "WASHING_MACHINE", "2400.00", "0.00", "2200.00"),
                        appliance("Salon Kliması", "AIR_CONDITIONER", "2500.00", "400.00", "2200.00"),
                        appliance("Kahve Makinesi", "COFFEE_MACHINE", "1500.00", "0.00", "1300.00"),
                        appliance("Wi-Fi Router", "ROUTER", "40.00", "5.00", "30.00")),
                adminId);

        // Home 2
        registerHomeIfMissing("Yalı Dairesi - Kadıköy", contactEmail, new BigDecimal("500.0000"),
                new BigDecimal("2200.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Bulaşık Makinesi", "DISHWASHER", "2000.00", "0.00", "1800.00"),
                        appliance("OLED TV", "TELEVISION", "180.00", "1.00", "150.00"),
                        appliance("Mikrodalga Fırın", "MICROWAVE", "1700.00", "0.00", "1500.00"),
                        appliance("Ses Sistemi", "SOUND_SYSTEM", "350.00", "1.00", "300.00")),
                adminId);

        // Home 3
        registerHomeIfMissing("Bahçeşehir Müstakil Villa", contactEmail, new BigDecimal("750.0000"),
                new BigDecimal("3500.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Kurutma Makinesi", "DRYER", "3300.00", "0.00", "3000.00"),
                        appliance("Robot Süpürge", "ROBOT_VACUUM", "120.00", "1.00", "80.00"),
                        appliance("Ankastre Fırın", "OVEN", "2800.00", "0.00", "2500.00"),
                        appliance("Bahçe Aydınlatması", "GARDEN_LIGHTING", "180.00", "10.00", "150.00"),
                        appliance("Güvenlik Kamerası", "SECURITY_CAMERA", "30.00", "3.00", "20.00")),
                adminId);

        // Home 4
        registerHomeIfMissing("Çankaya Rezidans D: 12", contactEmail, new BigDecimal("420.0000"),
                new BigDecimal("1800.00"), new BigDecimal("3.50"), new BigDecimal("6.20"),
                List.of(
                        appliance("Yatak Odası Kliması", "AIR_CONDITIONER", "2500.00", "300.00", "2200.00"),
                        appliance("Gaming Bilgisayarı", "GAMING_COMPUTER", "850.00", "2.00", "700.00"),
                        appliance("Oyun Konsolu", "GAME_CONSOLE", "260.00", "1.00", "220.00"),
                        appliance("Akıllı Hoparlör", "SMART_SPEAKER", "30.00", "1.00", "20.00")),
                adminId);

        // Home 5
        registerHomeIfMissing("Karşıyaka Yazlık Daire", contactEmail, new BigDecimal("300.0000"),
                new BigDecimal("1200.00"), new BigDecimal("3.10"), new BigDecimal("5.50"),
                List.of(
                        appliance("No-Frost Buzdolabı", "REFRIGERATOR", "220.00", "30.00", "180.00"),
                        appliance("Inverter Klima", "AIR_CONDITIONER", "2500.00", "250.00", "2000.00"),
                        appliance("Termosifon Su Isıtıcı", "WATER_HEATER", "3300.00", "2.00", "3000.00"),
                        appliance("Fiber Router", "ROUTER", "40.00", "5.00", "30.00")),
                adminId);

        // Home 6
        registerHomeIfMissing("Nilüfer Loft Daire", contactEmail, new BigDecimal("480.0000"),
                new BigDecimal("2000.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Çamaşır Makinesi", "WASHING_MACHINE", "2400.00", "0.00", "2200.00"),
                        appliance("Espresso Makinesi", "COFFEE_MACHINE", "1500.00", "0.00", "1300.00"),
                        appliance("4K Projektör", "PROJECTOR", "450.00", "0.50", "400.00"),
                        appliance("Çalışma Masası PC", "DESKTOP_COMPUTER", "450.00", "1.00", "350.00")),
                adminId);

        // Home 7
        registerHomeIfMissing("Muratpaşa Apart D: 2", contactEmail, new BigDecimal("280.0000"),
                new BigDecimal("1000.00"), new BigDecimal("3.20"), new BigDecimal("5.80"),
                List.of(
                        appliance("Split Klima", "AIR_CONDITIONER", "2500.00", "300.00", "2200.00"),
                        appliance("Su Isıtıcısı (Kettle)", "KETTLE", "2600.00", "0.00", "2400.00"),
                        appliance("Smart TV", "TELEVISION", "180.00", "0.50", "150.00"),
                        appliance("LED Aydınlatma Grubu", "LED_BULB", "25.00", "0.00", "20.00")),
                adminId);

        // Home 8
        registerHomeIfMissing("Trabzon Yayla Evi", contactEmail, new BigDecimal("600.0000"),
                new BigDecimal("2500.00"), new BigDecimal("2.90"), new BigDecimal("5.20"),
                List.of(
                        appliance("Derin Dondurucu", "FREEZER", "240.00", "20.00", "200.00"),
                        appliance("Elektrikli Soba", "ELECTRIC_HEATER", "2500.00", "0.00", "2200.00"),
                        appliance("Buharlı Ütü", "IRON", "2600.00", "0.00", "2400.00"),
                        appliance("LTE Router", "ROUTER", "40.00", "5.00", "30.00")),
                adminId);

        log.info("Seeded 8 demo homes for {}", contactEmail);
    }

    private void registerHomeIfMissing(String name, String contactEmail, BigDecimal monthlyKwhQuota,
                                        BigDecimal monthlyBudgetLimitTry, BigDecimal baseKwhRateTry,
                                        BigDecimal penaltyKwhRateTry,
                                        List<RegisterHomeCommand.ApplianceCommand> appliances, UUID adminId) {
        try {
            registerHomeUseCase.execute(new RegisterHomeCommand(
                    name, contactEmail, monthlyKwhQuota, monthlyBudgetLimitTry, baseKwhRateTry,
                    penaltyKwhRateTry, appliances, adminId));
        } catch (Exception e) {
            log.debug("Home {} already registered or skipped: {}", name, e.getMessage());
        }
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
