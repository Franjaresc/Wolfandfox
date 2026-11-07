package dev.franjaresc.wolfandfox.route.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RouteClosureTest {

    private static final RouteCode ROUTE = new RouteCode("RT-0001");
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final VehicleCode VEHICLE = new VehicleCode("VAN-0042");
    private static final DriverCode DRIVER = new DriverCode("DRV-0007");
    private static final PackageCounts PACKAGES = new PackageCounts(42, 3, 1);
    private static final Kilometres KM = new Kilometres(87);
    private static final UserId USER = new UserId("demo.supervisor");
    private static final Instant NOW = Instant.parse("2026-01-15T18:30:00Z");

    @Test
    void closeRecordsAllValuesWithAFreshId() {
        var closure = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, "Fictional note", USER, NOW, DATE);

        assertThat(closure.id()).isNotNull();
        assertThat(closure.routeCode()).isEqualTo(ROUTE);
        assertThat(closure.closureDate()).isEqualTo(DATE);
        assertThat(closure.vehicleCode()).isEqualTo(VEHICLE);
        assertThat(closure.driverCode()).isEqualTo(DRIVER);
        assertThat(closure.packages().total()).isEqualTo(46);
        assertThat(closure.kmDriven()).isEqualTo(KM);
        assertThat(closure.notes()).isEqualTo("Fictional note");
        assertThat(closure.closedBy()).isEqualTo(USER);
        assertThat(closure.closedAt()).isEqualTo(NOW);
    }

    @Test
    void eachClosureGetsItsOwnId() {
        var first = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE);
        var second = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void notesAreOptional() {
        var closure = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE);

        assertThat(closure.notes()).isNull();
    }

    @Test
    void acceptsNotesAtTheMaximumLength() {
        var notes = "n".repeat(1000);

        var closure = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, notes, USER, NOW, DATE);

        assertThat(closure.notes()).hasSize(1000);
    }

    @Test
    void rejectsNotesOverTheMaximumLength() {
        var notes = "n".repeat(1001);

        assertThatThrownBy(() -> RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, notes, USER, NOW, DATE))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("Notes");
    }

    @Test
    void rejectsMissingRequiredValue() {
        assertThatThrownBy(() -> RouteClosure.close(null, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("Route code");
        assertThatThrownBy(() -> RouteClosure.close(ROUTE, null, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("Closure date");
        assertThatThrownBy(() -> RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, null, DATE))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("Closed at");
        assertThatThrownBy(() -> RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, null))
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("Today");
    }

    @Test
    void acceptsClosingToday() {
        var closure = RouteClosure.close(ROUTE, DATE, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE);

        assertThat(closure.closureDate()).isEqualTo(DATE);
    }

    @Test
    void acceptsClosingYesterday() {
        var yesterday = DATE.minusDays(1);

        var closure = RouteClosure.close(ROUTE, yesterday, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE);

        assertThat(closure.closureDate()).isEqualTo(yesterday);
    }

    @Test
    void rejectsClosingTomorrow() {
        var tomorrow = DATE.plusDays(1);

        assertThatThrownBy(() -> RouteClosure.close(ROUTE, tomorrow, VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW, DATE))
                .isInstanceOf(ClosureDateInFutureException.class)
                .isInstanceOf(InvalidRouteClosureException.class)
                .hasMessageContaining("2026-01-16");
    }

    @Test
    void storedClosuresAreNotRecheckedAgainstToday() {
        var later = new RouteClosure(
                RouteClosureId.newId(), ROUTE, DATE.plusDays(30), VEHICLE, DRIVER, PACKAGES, KM, null, USER, NOW);

        assertThat(later.closureDate()).isEqualTo(DATE.plusDays(30));
    }
}
