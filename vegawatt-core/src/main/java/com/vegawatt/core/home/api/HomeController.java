package com.vegawatt.core.home.api;

import com.vegawatt.core.home.application.GetAllLiveHomesQuery;
import com.vegawatt.core.home.application.GetLiveHomeStatusQuery;
import com.vegawatt.core.home.application.RegisterHomeCommand;
import com.vegawatt.core.home.application.RegisterHomeUseCase;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes")
class HomeController {

    private final RegisterHomeUseCase registerHomeUseCase;
    private final GetAllLiveHomesQuery getAllLiveHomesQuery;
    private final GetLiveHomeStatusQuery getLiveHomeStatusQuery;

    HomeController(RegisterHomeUseCase registerHomeUseCase, GetAllLiveHomesQuery getAllLiveHomesQuery,
                    GetLiveHomeStatusQuery getLiveHomeStatusQuery) {
        this.registerHomeUseCase = registerHomeUseCase;
        this.getAllLiveHomesQuery = getAllLiveHomesQuery;
        this.getLiveHomeStatusQuery = getLiveHomeStatusQuery;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegisterHomeResponse register(@Valid @RequestBody RegisterHomeRequest request) {
        Home home = registerHomeUseCase.execute(toCommand(request));
        return RegisterHomeResponse.from(home);
    }

    @GetMapping("/live")
    List<HomeLiveSummaryResponse> liveHomes() {
        return getAllLiveHomesQuery.execute().stream().map(HomeLiveSummaryResponse::from).toList();
    }

    @GetMapping("/{homeId}/live")
    HomeLiveStatusResponse liveHome(@PathVariable UUID homeId) {
        return getLiveHomeStatusQuery.execute(homeId)
                .map(HomeLiveStatusResponse::from)
                .orElseThrow(() -> new HomeNotFoundException(homeId));
    }

    private static RegisterHomeCommand toCommand(RegisterHomeRequest request) {
        return new RegisterHomeCommand(
                request.name(),
                request.contactEmail(),
                request.energyQuotaKwh(),
                request.budgetQuotaTry(),
                request.baseTariffPerKwh(),
                request.penaltyTariffPerKwh(),
                request.appliances().stream()
                        .map(appliance -> new RegisterHomeCommand.ApplianceCommand(
                                appliance.name(), appliance.type(), appliance.safePowerLimitWatt(),
                                appliance.simulationMinWatt(), appliance.simulationMaxWatt()))
                        .toList());
    }
}
