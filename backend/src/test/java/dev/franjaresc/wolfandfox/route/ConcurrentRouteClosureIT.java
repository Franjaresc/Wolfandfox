package dev.franjaresc.wolfandfox.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jayway.jsonpath.JsonPath;
import dev.franjaresc.wolfandfox.ApiTestClient;
import dev.franjaresc.wolfandfox.PostgresContainerConfig;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * A barrier holds both requests until each has passed the use case's pre-check. Neither can see the
 * other's row, so only the database constraint can stop the second insert. Without the barrier, the
 * test would usually pass through the pre-check and never exercise this path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
class ConcurrentRouteClosureIT {

    private static final String BODY = """
            {"routeCode":"RT-0001","closureDate":"2026-01-15","vehicleCode":"VAN-0042","driverCode":"DRV-0007",
            "packagesDelivered":42,"packagesFailed":3,"packagesReturned":1,"kmDriven":87}
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private RouteClosureRepositoryPort repository;

    @BeforeEach
    void emptyTable() {
        jdbc.execute("TRUNCATE route_closure");
    }

    @RepeatedTest(5)
    void concurrentClosuresOfTheSameRouteAndDateEndInOneCreatedAndOneConflict() throws Exception {
        var bothPassedThePreCheck = new CyclicBarrier(2);
        doAnswer(invocation -> {
                    var exists = invocation.callRealMethod();
                    bothPassedThePreCheck.await(30, TimeUnit.SECONDS);
                    return exists;
                })
                .when(repository)
                .existsByRouteCodeAndClosureDate(any(), any());
        var api = new ApiTestClient(port);

        List<ApiTestClient.Response> responses;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> api.closeRoute(BODY));
            var second = executor.submit(() -> api.closeRoute(BODY));
            responses = List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS));
        }

        assertThat(responses).extracting(ApiTestClient.Response::status).containsExactlyInAnyOrder(201, 409);
        verify(repository, times(2)).save(any());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM route_closure", Integer.class)).isEqualTo(1);

        var conflict = responses.stream().filter(response -> response.status() == 409).findFirst().orElseThrow();
        assertThat(conflict.contentType()).isEqualTo("application/problem+json");
        assertThat(JsonPath.<String>read(conflict.body(), "$.type"))
                .isEqualTo("urn:wolfandfox:problem:duplicate-route-closure");
        assertThat(conflict.body()).doesNotContainIgnoringCase("sql").doesNotContain("uq_route_closure");
    }
}
