package dev.franjaresc.wolfandfox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Proves the contract checks can fail. A validator that accepts everything would make them worthless. */
class ContractValidatorTest {

    private static final String PATH = "/api/v1/route-closures/00000000-0000-4000-8000-000000000001";
    private static final String JSON = "application/json";
    private static final String VALID_CLOSURE = """
            {"id":"00000000-0000-4000-8000-000000000001","routeCode":"RT-0001","closureDate":"2026-01-15",
            "vehicleCode":"VAN-0042","driverCode":"DRV-0007","packagesDelivered":42,"packagesFailed":3,
            "packagesReturned":1,"totalPackages":46,"kmDriven":87,"closedBy":"demo.supervisor",
            "closedAt":"2026-01-15T18:30:00Z"%s}
            """;

    @Test
    void acceptsAResponseThatMatchesTheContract() {
        assertThat(ContractValidator.violations("GET", PATH, 200, JSON, null, VALID_CLOSURE.formatted("")))
                .isEmpty();
    }

    @Test
    void detectsAFieldTheContractDoesNotDeclare() {
        var body = VALID_CLOSURE.formatted(",\"internalNote\":\"drift\"");

        assertThat(ContractValidator.violations("GET", PATH, 200, JSON, null, body)).contains("internalNote");
    }

    @Test
    void detectsAMissingRequiredField() {
        var body = VALID_CLOSURE.formatted("").replace("\"totalPackages\":46,", "");

        assertThat(ContractValidator.violations("GET", PATH, 200, JSON, null, body)).contains("totalPackages");
    }

    @Test
    void detectsAFieldOfTheWrongType() {
        var body = VALID_CLOSURE.formatted("").replace("\"kmDriven\":87", "\"kmDriven\":\"87\"");

        assertThat(ContractValidator.violations("GET", PATH, 200, JSON, null, body)).contains("kmDriven");
    }

    @Test
    void detectsAnUndeclaredNullValue() {
        var body = VALID_CLOSURE.formatted(",\"notes\":null");

        assertThat(ContractValidator.violations("GET", PATH, 200, JSON, null, body)).contains("notes");
    }

    @Test
    void detectsAStatusTheContractDoesNotDeclare() {
        assertThat(ContractValidator.violations("GET", PATH, 418, JSON, null, "{}")).isNotEmpty();
    }

    @Test
    void detectsAProblemSentWithTheWrongContentType() {
        var body = "{\"type\":\"urn:wolfandfox:problem:route-closure-not-found\",\"title\":\"x\",\"status\":404}";

        assertThat(ContractValidator.violations("GET", PATH, 404, JSON, null, body)).isNotEmpty();
    }

    @Test
    void detectsAMissingRequiredHeader() {
        assertThat(ContractValidator.violations(
                        "POST", "/api/v1/route-closures", 201, JSON, null, VALID_CLOSURE.formatted("")))
                .contains("Location");
    }
}
