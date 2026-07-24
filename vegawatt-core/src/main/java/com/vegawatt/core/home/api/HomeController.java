package com.vegawatt.core.home.api;

import com.vegawatt.core.access.domain.HomeAccessService;
import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.home.application.AddApplianceCommand;
import com.vegawatt.core.home.application.AddApplianceUseCase;
import com.vegawatt.core.home.application.GetAllLiveHomesQuery;
import com.vegawatt.core.home.application.GetLiveHomeStatusQuery;
import com.vegawatt.core.home.application.RegisterHomeCommand;
import com.vegawatt.core.home.application.RegisterHomeUseCase;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.user.domain.UserRole;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AddApplianceUseCase addApplianceUseCase;
    private final GetAllLiveHomesQuery getAllLiveHomesQuery;
    private final GetLiveHomeStatusQuery getLiveHomeStatusQuery;
    private final HomeAccessService homeAccessService;
    private final HomeAuthorizationService homeAuthorizationService;

    HomeController(RegisterHomeUseCase registerHomeUseCase, AddApplianceUseCase addApplianceUseCase,
                    GetAllLiveHomesQuery getAllLiveHomesQuery, GetLiveHomeStatusQuery getLiveHomeStatusQuery,
                    HomeAccessService homeAccessService, HomeAuthorizationService homeAuthorizationService) {
        this.registerHomeUseCase = registerHomeUseCase;
        this.addApplianceUseCase = addApplianceUseCase;
        this.getAllLiveHomesQuery = getAllLiveHomesQuery;
        this.getLiveHomeStatusQuery = getLiveHomeStatusQuery;
        this.homeAccessService = homeAccessService;
        this.homeAuthorizationService = homeAuthorizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegisterHomeResponse register(@Valid @RequestBody RegisterHomeRequest request,
                                   @AuthenticationPrincipal CurrentUser currentUser) {
        Home home = registerHomeUseCase.execute(toCommand(request, currentUser.userId()));
        return RegisterHomeResponse.from(home);
    }

    @PostMapping("/{homeId}/appliances")
    @ResponseStatus(HttpStatus.CREATED)
    AddApplianceResponse addAppliance(@PathVariable UUID homeId, @Valid @RequestBody AddApplianceRequest request,
                                       @AuthenticationPrincipal CurrentUser currentUser) {
        homeAuthorizationService.requireAccess(currentUser, homeId);
        Appliance appliance = addApplianceUseCase.execute(new AddApplianceCommand(homeId, request.name(),
                request.type(), request.safePowerLimitWatt(), request.simulationMinWatt(),
                request.simulationMaxWatt(), request.catalogItemId()));
        return AddApplianceResponse.from(appliance);
    }

    @GetMapping("/live")
    List<HomeLiveSummaryResponse> liveHomes(@AuthenticationPrincipal CurrentUser currentUser) {
        var summaries = currentUser.role() == UserRole.ADMIN
                ? getAllLiveHomesQuery.execute()
                : getAllLiveHomesQuery.execute(homeAccessService.accessibleHomeIds(currentUser.userId()));
        return summaries.stream().map(HomeLiveSummaryResponse::from).toList();
    }

    @GetMapping("/{homeId}/live")
    HomeLiveStatusResponse liveHome(@PathVariable UUID homeId, @AuthenticationPrincipal CurrentUser currentUser) {
        homeAuthorizationService.requireAccess(currentUser, homeId);
        return getLiveHomeStatusQuery.execute(homeId)
                .map(HomeLiveStatusResponse::from)
                .orElseThrow(() -> new HomeNotFoundException(homeId));
    }

    private static RegisterHomeCommand toCommand(RegisterHomeRequest request, UUID ownerUserId) {
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
                                appliance.simulationMinWatt(), appliance.simulationMaxWatt(),
                                appliance.catalogItemId()))
                        .toList(),
                ownerUserId);
    }
}
