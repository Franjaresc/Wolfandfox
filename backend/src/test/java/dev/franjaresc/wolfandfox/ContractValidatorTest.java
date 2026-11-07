package dev.franjaresc.wolfandfox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Proves the contract checks can fail. A validator that accepts everything would make them worthless. */
class ContractValidatorTest {

    private static final String PATH = "/api/v1/route-closures/00000000-0000-4000-8000-000000000001";
    private static final String JSON = "application/json";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PROBLEM =
            "{\"type\":\"urn:wolfandfox:problem:route-closure-not-found\",%s\"title\":\"x\",\"status\":404}";
    private static final String VALID_CLOSURE = """
            {"id":"00000000-0000-4000-8000-000000000001","routeCode":"RT-0001","closureDate":"2026-01-15",
            "vehicleCode":"VAN-0042","driverCode":"DRV-0007","packagesDelivered":42,"packagesFailed":3,
            "packagesReturned":1,"totalPackages":46,"kmDriven":87,"closedBy":"demo.supervisor",
            "closedAt":"2026-01-15T18:30:00Z"%s}
            """;

    /**
     * The wording of a violation depends on the validator's version and the machine's locale, so this
     * checks only that validation fails. The accepting tests here show that a matching body passes.
     */
    private static void assertRejected(int status, String contentType, String body) {
        assertThatThrownBy(() -> ContractValidator.assertMatches("GET", PATH, status, contentType, null, body))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("GET")
                .hasMessageContaining(PATH);
    }

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

        assertRejected(200, JSON, body);
    }

    @Test
    void detectsAnUndeclaredNullValue() {
        assertRejected(200, JSON, VALID_CLOSURE.formatted(",\"notes\":null"));
    }

    @Test
    void acceptsARelativeProblemInstance() {
        var body = PROBLEM.formatted("\"code\":\"NOT_FOUND\",\"instance\":\"/api/v1/route-closures\",");

        assertThat(ContractValidator.violations("GET", PATH, 404, PROBLEM_JSON, null, body)).isEmpty();
    }

    @Test
    void detectsAProblemInstanceThatIsNotAUriReference() {
        assertRejected(404, PROBLEM_JSON, PROBLEM.formatted("\"code\":\"NOT_FOUND\",\"instance\":\"/not a reference\","));
    }

    @Test
    void detectsAProblemTypeThatIsNotAnAbsoluteUri() {
        var body = "{\"type\":\"/relative/type\",\"code\":\"NOT_FOUND\",\"title\":\"x\",\"status\":404}";

        assertRejected(404, PROBLEM_JSON, body);
    }

    @Test
    void detectsAStatusTheContractDoesNotDeclare() {
        assertThat(ContractValidator.violations("GET", PATH, 418, JSON, null, "{}")).isNotEmpty();
    }

    @Test
    void detectsAProblemSentWithTheWrongContentType() {
        var body = PROBLEM.formatted("\"code\":\"NOT_FOUND\",");

        assertThat(ContractValidator.violations("GET", PATH, 404, JSON, null, body)).isNotEmpty();
    }

    @Test
    void detectsAProblemWithoutACode() {
        assertThat(ContractValidator.violations("GET", PATH, 404, PROBLEM_JSON, null, PROBLEM.formatted("")))
                .contains("code");
    }

    @Test
    void detectsAProblemCodeTheContractDoesNotDeclare() {
        var body = PROBLEM.formatted("\"code\":\"SOMETHING_ELSE\",");

        assertThat(ContractValidator.violations("GET", PATH, 404, PROBLEM_JSON, null, body)).isNotEmpty();
    }

    @Test
    void acceptsAProblemWithADeclaredCode() {
        var body = PROBLEM.formatted("\"code\":\"NOT_FOUND\",");

        assertThat(ContractValidator.violations("GET", PATH, 404, PROBLEM_JSON, null, body)).isEmpty();
    }

    @Test
    void detectsAMissingRequiredHeader() {
        assertThat(ContractValidator.violations(
                        "POST", "/api/v1/route-closures", 201, JSON, null, VALID_CLOSURE.formatted("")))
                .contains("Location");
    }
}
