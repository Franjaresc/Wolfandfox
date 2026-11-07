package dev.franjaresc.wolfandfox.route;

import static dev.franjaresc.wolfandfox.ApiTestClient.ROUTE_CLOSURES;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.franjaresc.wolfandfox.ApiTestClient;
import dev.franjaresc.wolfandfox.PostgresContainerConfig;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** The whole application over real HTTP against PostgreSQL 16. Every response is checked against the contract. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
class RouteClosureApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE route_closure");
        api = new ApiTestClient(port);
    }

    @Test
    void closedRouteCanBeReadBackIdentically() throws Exception {
        var created = api.closeRoute(body("RT-0001", "2026-01-15"));

        assertThat(created.status()).isEqualTo(201);
        assertThat(JsonPath.<Integer>read(created.body(), "$.totalPackages")).isEqualTo(46);
        assertThat(JsonPath.<String>read(created.body(), "$.closedBy")).isEqualTo("demo.supervisor");
        var id = JsonPath.<String>read(created.body(), "$.id");
        assertThat(created.location()).isEqualTo(ROUTE_CLOSURES + "/" + id);

        var read = api.get(created.location());

        assertThat(read.status()).isEqualTo(200);
        assertThat(read.body()).isEqualTo(created.body());
    }

    @Test
    void closingTheSameRouteAndDateTwiceConflicts() throws Exception {
        api.closeRoute(body("RT-0001", "2026-01-15"));

        var second = api.closeRoute(body("RT-0001", "2026-01-15"));

        assertThat(second.status()).isEqualTo(409);
        assertThat(second.contentType()).isEqualTo("application/problem+json");
        assertThat(JsonPath.<String>read(second.body(), "$.type"))
                .isEqualTo("urn:wolfandfox:problem:duplicate-route-closure");
        assertThat(JsonPath.<String>read(second.body(), "$.code")).isEqualTo("ROUTE_CLOSURE_DUPLICATE");
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void sameRouteCanCloseOnAnotherDateAndAnotherRouteOnTheSameDate() throws Exception {
        api.closeRoute(body("RT-0001", "2026-01-15"));

        assertThat(api.closeRoute(body("RT-0001", "2026-01-16")).status()).isEqualTo(201);
        assertThat(api.closeRoute(body("RT-0002", "2026-01-15")).status()).isEqualTo(201);
    }

    @Test
    void theRealClockAllowsTodayAndRejectsTomorrow() throws Exception {
        // The application's clock uses its default zone, UTC, in tests.
        var today = LocalDate.now(ZoneOffset.UTC);

        var tomorrow = api.closeRoute(body("RT-0001", today.plusDays(1).toString()));

        assertThat(tomorrow.status()).isEqualTo(400);
        assertThat(JsonPath.<String>read(tomorrow.body(), "$.code")).isEqualTo("VALIDATION_FAILED");
        assertThat(JsonPath.<List<String>>read(tomorrow.body(), "$.errors[*].field")).containsExactly("closureDate");
        assertThat(api.closeRoute(body("RT-0001", today.toString())).status()).isEqualTo(201);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void unknownIdIsNotFound() throws Exception {
        var response = api.get(ROUTE_CLOSURES + "/" + UUID.randomUUID());

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.contentType()).isEqualTo("application/problem+json");
    }

    @Test
    void listsNewestDateFirstThenRouteCode() throws Exception {
        api.closeRoute(body("RT-0002", "2026-01-15"));
        api.closeRoute(body("RT-0003", "2026-01-16"));
        api.closeRoute(body("RT-0001", "2026-01-15"));
        api.closeRoute(body("RT-0001", "2026-01-16"));

        var list = api.get(ROUTE_CLOSURES);

        assertThat(list.status()).isEqualTo(200);
        assertThat(keys(list.body()))
                .containsExactly(
                        "2026-01-16 RT-0001", "2026-01-16 RT-0003", "2026-01-15 RT-0001", "2026-01-15 RT-0002");
    }

    @Test
    void filtersAndPaginatesKeepingTheOrder() throws Exception {
        api.closeRoute(body("RT-0003", "2026-01-15"));
        api.closeRoute(body("RT-0001", "2026-01-15"));
        api.closeRoute(body("RT-0002", "2026-01-15"));
        api.closeRoute(body("RT-0001", "2026-01-16"));

        var page = api.get(ROUTE_CLOSURES + "?date=2026-01-15&page=1&size=2");

        assertThat(keys(page.body())).containsExactly("2026-01-15 RT-0003");
        assertThat(JsonPath.<Integer>read(page.body(), "$.totalElements")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(page.body(), "$.totalPages")).isEqualTo(2);
        assertThat(keys(api.get(ROUTE_CLOSURES + "?routeCode=RT-0001").body()))
                .containsExactly("2026-01-16 RT-0001", "2026-01-15 RT-0001");
    }

    @Test
    void rejectsPageSizesOutsideTheLimits() throws Exception {
        assertThat(api.get(ROUTE_CLOSURES + "?size=101").status()).isEqualTo(400);
        assertThat(api.get(ROUTE_CLOSURES + "?size=0").status()).isEqualTo(400);
        assertThat(api.get(ROUTE_CLOSURES + "?page=-1").status()).isEqualTo(400);
        assertThat(api.get(ROUTE_CLOSURES + "?size=100").status()).isEqualTo(200);
    }

    @Test
    void strictJsonRulesApplyInTheRunningApplication() throws Exception {
        var valid = body("RT-0001", "2026-01-15");
        var unknownField = valid.replace("{", "{\"closedBy\":\"someone.else\",");
        var numberAsString = valid.replace("\"RT-0001\"", "1234");
        var floatAsInteger = valid.replace("\"kmDriven\":87", "\"kmDriven\":87.5");

        for (var invalid : List.of(unknownField, numberAsString, floatAsInteger)) {
            var response = api.closeRoute(invalid);

            assertThat(response.status()).as(invalid).isEqualTo(400);
            assertThat(response.contentType()).isEqualTo("application/problem+json");
        }
        assertThat(rowCount()).isZero();
    }

    private int rowCount() {
        return jdbc.queryForObject("SELECT count(*) FROM route_closure", Integer.class);
    }

    private static List<String> keys(String listBody) {
        List<String> dates = JsonPath.read(listBody, "$.items[*].closureDate");
        List<String> routes = JsonPath.read(listBody, "$.items[*].routeCode");
        return java.util.stream.IntStream.range(0, dates.size())
                .mapToObj(index -> dates.get(index) + " " + routes.get(index))
                .toList();
    }

    private static String body(String routeCode, String date) {
        return """
                {"routeCode":"%s","closureDate":"%s","vehicleCode":"VAN-0042","driverCode":"DRV-0007",
                "packagesDelivered":42,"packagesFailed":3,"packagesReturned":1,"kmDriven":87}
                """.formatted(routeCode, date);
    }
}
