package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class RouteClosureJpaAdapterTest {

    private static final RouteClosure CLOSURE = RouteClosure.close(
            new RouteCode("RT-0001"),
            LocalDate.of(2026, 1, 15),
            new VehicleCode("VAN-0042"),
            new DriverCode("DRV-0007"),
            new PackageCounts(42, 3, 1),
            new Kilometres(87),
            null,
            new UserId("demo.supervisor"),
            Instant.parse("2026-01-15T18:30:00Z"),
            LocalDate.of(2026, 1, 15));

    private final RouteClosureJpaRepository repository = mock(RouteClosureJpaRepository.class);
    private final RouteClosureJpaAdapter adapter = new RouteClosureJpaAdapter(repository);

    @Test
    void translatesTheUniqueRouteAndDateViolationToADuplicate() {
        failSavingWith(violationOf(RouteClosureJpaAdapter.UNIQUE_ROUTE_AND_DATE));

        assertThatThrownBy(() -> adapter.save(CLOSURE)).isInstanceOf(DuplicateRouteClosureException.class);
    }

    @Test
    void doesNotReportOtherConstraintViolationsAsDuplicates() {
        var failure = violationOf("ck_route_closure_km_driven");
        failSavingWith(failure);

        assertThatThrownBy(() -> adapter.save(CLOSURE)).isSameAs(failure);
    }

    @Test
    void ignoresTheConstraintNameInTheMessageWhenTheViolationNamesAnother() {
        var failure = new DataIntegrityViolationException(
                RouteClosureJpaAdapter.UNIQUE_ROUTE_AND_DATE,
                new ConstraintViolationException("failed", new SQLException(), "ck_route_closure_km_driven"));
        failSavingWith(failure);

        assertThatThrownBy(() -> adapter.save(CLOSURE)).isSameAs(failure);
    }

    @Test
    void doesNotReportIntegrityFailuresWithoutAConstraintAsDuplicates() {
        var failure = new DataIntegrityViolationException("no constraint information");
        failSavingWith(failure);

        assertThatThrownBy(() -> adapter.save(CLOSURE)).isSameAs(failure);
    }

    private void failSavingWith(DataIntegrityViolationException failure) {
        when(repository.saveAndFlush(any())).thenThrow(failure);
    }

    private static DataIntegrityViolationException violationOf(String constraintName) {
        return new DataIntegrityViolationException(
                "failed", new ConstraintViolationException("failed", new SQLException(), constraintName));
    }
}
