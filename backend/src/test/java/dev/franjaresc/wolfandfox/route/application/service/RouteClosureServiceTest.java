package dev.franjaresc.wolfandfox.route.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.application.port.in.CloseRouteUseCase.CloseRouteCommand;
import dev.franjaresc.wolfandfox.route.application.port.out.CurrentUserPort;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureNotFoundException;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteClosureServiceTest {

    private static final RouteCode ROUTE = new RouteCode("RT-0001");
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final UserId USER = new UserId("demo.supervisor");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-01-15T18:30:00.123456789Z"), ZoneOffset.UTC);
    private static final CloseRouteCommand COMMAND = new CloseRouteCommand(
            ROUTE,
            DATE,
            new VehicleCode("VAN-0042"),
            new DriverCode("DRV-0007"),
            new PackageCounts(42, 3, 1),
            new Kilometres(87),
            "Fictional note");

    @Mock
    private RouteClosureRepositoryPort repository;

    @Mock
    private CurrentUserPort currentUser;

    private RouteClosureService service;

    @BeforeEach
    void setUp() {
        service = new RouteClosureService(repository, currentUser, CLOCK);
    }

    @Test
    void closeStoresClosureWithServerSetUserAndTime() {
        when(currentUser.currentUser()).thenReturn(USER);
        when(repository.existsByRouteCodeAndClosureDate(ROUTE, DATE)).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());

        var closure = service.close(COMMAND);

        assertThat(closure.routeCode()).isEqualTo(ROUTE);
        assertThat(closure.closureDate()).isEqualTo(DATE);
        assertThat(closure.packages().total()).isEqualTo(46);
        assertThat(closure.notes()).isEqualTo("Fictional note");
        assertThat(closure.closedBy()).isEqualTo(USER);
        verify(repository).save(closure);
    }

    @Test
    void closeTruncatesTimeToWhatTheDatabaseCanStore() {
        when(currentUser.currentUser()).thenReturn(USER);
        when(repository.save(any())).then(returnsFirstArg());

        var closure = service.close(COMMAND);

        assertThat(closure.closedAt()).isEqualTo(Instant.parse("2026-01-15T18:30:00.123456Z"));
    }

    @Test
    void closeRejectsDuplicateWithoutSaving() {
        when(currentUser.currentUser()).thenReturn(USER);
        when(repository.existsByRouteCodeAndClosureDate(ROUTE, DATE)).thenReturn(true);

        assertThatThrownBy(() -> service.close(COMMAND))
                .isInstanceOf(DuplicateRouteClosureException.class)
                .hasMessageContaining("RT-0001")
                .hasMessageContaining("2026-01-15");
        verify(repository, never()).save(any());
    }

    @Test
    void closePropagatesDuplicateDetectedWhileSaving() {
        when(currentUser.currentUser()).thenReturn(USER);
        when(repository.save(any())).thenThrow(new DuplicateRouteClosureException(ROUTE, DATE));

        assertThatThrownBy(() -> service.close(COMMAND)).isInstanceOf(DuplicateRouteClosureException.class);
    }

    @Test
    void getReturnsExistingClosure() {
        var closure = sampleClosure();
        when(repository.findById(closure.id())).thenReturn(Optional.of(closure));

        assertThat(service.get(closure.id())).isEqualTo(closure);
    }

    @Test
    void getRejectsUnknownId() {
        var id = RouteClosureId.newId();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(RouteClosureNotFoundException.class)
                .hasMessageContaining(id.value().toString());
    }

    @Test
    void findReturnsThePageFromTheRepository() {
        var filter = new RouteClosureFilter(DATE, ROUTE);
        var query = new PageQuery(0, 20);
        var page = new PageResult<>(List.of(sampleClosure()), query, 1);
        when(repository.find(filter, query)).thenReturn(page);

        assertThat(service.find(filter, query)).isEqualTo(page);
    }

    private static RouteClosure sampleClosure() {
        return RouteClosure.close(
                ROUTE,
                DATE,
                new VehicleCode("VAN-0042"),
                new DriverCode("DRV-0007"),
                new PackageCounts(42, 3, 1),
                new Kilometres(87),
                null,
                USER,
                CLOCK.instant());
    }
}
