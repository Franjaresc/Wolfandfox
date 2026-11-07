package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.franjaresc.wolfandfox.PostgresContainerConfig;
import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Runs without a surrounding test transaction, so every write really commits or fails. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({RouteClosureJpaAdapter.class, PostgresContainerConfig.class})
class RouteClosurePersistenceIT {

    private static final LocalDate JAN_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate JAN_16 = LocalDate.of(2026, 1, 16);
    private static final String INSERT = """
            INSERT INTO route_closure (id, route_code, closure_date, vehicle_code, driver_code,
                packages_delivered, packages_failed, packages_returned, km_driven, closed_by, closed_at)
            VALUES (gen_random_uuid(), ?, ?, 'VAN-0042', 'DRV-0007', ?, ?, ?, ?, 'demo.supervisor', now())
            """;

    @Autowired
    private RouteClosureJpaAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void emptyTable() {
        jdbc.execute("TRUNCATE route_closure");
    }

    @Test
    void flywayAppliedTheInitialMigrationOnPostgres16() {
        assertThat(jdbc.queryForObject(
                        "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SHOW server_version", String.class)).startsWith("16.");
    }

    @Test
    void schemaUsesExpectedTypesAndStoresNoTotal() {
        assertThat(columnType("id")).isEqualTo("uuid");
        assertThat(columnType("closed_at")).isEqualTo("timestamp with time zone");
        assertThat(jdbc.queryForList(
                        "SELECT column_name FROM information_schema.columns WHERE table_name = 'route_closure'",
                        String.class))
                .noneMatch(column -> column.contains("total"));
    }

    @Test
    void databaseRejectsADuplicateRouteAndDateByNamedConstraint() {
        jdbc.update(INSERT, "RT-0001", JAN_15, 1, 1, 1, 1);

        assertThatThrownBy(() -> jdbc.update(INSERT, "RT-0001", JAN_15, 2, 2, 2, 2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(RouteClosureJpaAdapter.UNIQUE_ROUTE_AND_DATE);
    }

    @Test
    void databaseRejectsNegativeValuesByNamedConstraint() {
        assertRejected(-1, 0, 0, 0, "ck_route_closure_packages_delivered");
        assertRejected(0, -1, 0, 0, "ck_route_closure_packages_failed");
        assertRejected(0, 0, -1, 0, "ck_route_closure_packages_returned");
        assertRejected(0, 0, 0, -1, "ck_route_closure_km_driven");
    }

    @Test
    void savedClosureReadsBackUnchanged() {
        var closure = closure("RT-0001", JAN_15, "Fictional note");

        adapter.save(closure);

        assertThat(adapter.findById(closure.id())).contains(closure);
    }

    @Test
    void optionalNotesReadBackAsNull() {
        var closure = closure("RT-0001", JAN_15, null);

        adapter.save(closure);

        assertThat(adapter.findById(closure.id())).contains(closure);
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(adapter.findById(RouteClosureId.newId())).isEmpty();
    }

    @Test
    void reportsExistenceByRouteAndDate() {
        adapter.save(closure("RT-0001", JAN_15, null));

        assertThat(adapter.existsByRouteCodeAndClosureDate(new RouteCode("RT-0001"), JAN_15)).isTrue();
        assertThat(adapter.existsByRouteCodeAndClosureDate(new RouteCode("RT-0001"), JAN_16)).isFalse();
        assertThat(adapter.existsByRouteCodeAndClosureDate(new RouteCode("RT-0002"), JAN_15)).isFalse();
    }

    @Test
    void savingADuplicateFromPostgresRaisesTheDomainError() {
        adapter.save(closure("RT-0001", JAN_15, null));

        assertThatThrownBy(() -> adapter.save(closure("RT-0001", JAN_15, null)))
                .isInstanceOf(DuplicateRouteClosureException.class);
    }

    @Test
    void ordersByDateDescendingThenRouteCodeAscending() {
        adapter.save(closure("RT-0002", JAN_15, null));
        adapter.save(closure("RT-0003", JAN_16, null));
        adapter.save(closure("RT-0001", JAN_15, null));
        adapter.save(closure("RT-0001", JAN_16, null));

        var page = adapter.find(new RouteClosureFilter(null, null), new PageQuery(0, 20));

        assertThat(page.items())
                .extracting(closure -> closure.closureDate() + " " + closure.routeCode().value())
                .containsExactly(
                        "2026-01-16 RT-0001", "2026-01-16 RT-0003", "2026-01-15 RT-0001", "2026-01-15 RT-0002");
    }

    @Test
    void filtersByDateAndRouteCode() {
        adapter.save(closure("RT-0001", JAN_15, null));
        adapter.save(closure("RT-0001", JAN_16, null));
        adapter.save(closure("RT-0002", JAN_15, null));
        var firstPage = new PageQuery(0, 20);

        assertThat(adapter.find(new RouteClosureFilter(JAN_15, null), firstPage).totalElements()).isEqualTo(2);
        assertThat(adapter.find(new RouteClosureFilter(null, new RouteCode("RT-0001")), firstPage).totalElements())
                .isEqualTo(2);
        assertThat(adapter.find(new RouteClosureFilter(JAN_15, new RouteCode("RT-0001")), firstPage).totalElements())
                .isEqualTo(1);
    }

    @Test
    void returnsTheRequestedPageWithTotals() {
        adapter.save(closure("RT-0001", JAN_15, null));
        adapter.save(closure("RT-0002", JAN_15, null));
        adapter.save(closure("RT-0003", JAN_15, null));

        var page = adapter.find(new RouteClosureFilter(null, null), new PageQuery(1, 2));

        assertThat(page.items()).extracting(closure -> closure.routeCode().value()).containsExactly("RT-0003");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    private void assertRejected(int delivered, int failed, int returned, int km, String constraint) {
        assertThatThrownBy(() -> jdbc.update(INSERT, "RT-0009", JAN_15, delivered, failed, returned, km))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    private String columnType(String column) {
        return jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns"
                        + " WHERE table_name = 'route_closure' AND column_name = ?",
                String.class,
                column);
    }

    private static RouteClosure closure(String routeCode, LocalDate date, String notes) {
        return RouteClosure.close(
                new RouteCode(routeCode),
                date,
                new VehicleCode("VAN-0042"),
                new DriverCode("DRV-0007"),
                new PackageCounts(42, 3, 1),
                new Kilometres(87),
                notes,
                new UserId("demo.supervisor"),
                Instant.parse("2026-01-15T18:30:00.123456Z"),
                date);
    }
}
