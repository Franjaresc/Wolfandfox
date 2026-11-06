package dev.franjaresc.wolfandfox;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import java.nio.file.Path;

/**
 * Checks real responses against contracts/openapi.yaml. Generated interfaces catch signature drift
 * at compile time, but not status codes, headers, content types or the JSON actually written.
 */
public final class ContractValidator {

    private static final OpenApiInteractionValidator VALIDATOR = OpenApiInteractionValidator
            .createForSpecificationUrl(Path.of("../contracts/openapi.yaml").toUri().toString())
            .build();

    private ContractValidator() {
    }

    /** Returns the violations in a readable form, or an empty string if the response matches. */
    public static String violations(String method, String path, int status, String contentType, String location, String body) {
        var response = SimpleResponse.Builder.status(status).withContentType(contentType).withBody(body);
        if (location != null) {
            response.withHeader("Location", location);
        }
        var report = VALIDATOR.validateResponse(path, Request.Method.valueOf(method), response.build());
        return report.hasErrors() ? SimpleValidationReportFormat.getInstance().apply(report) : "";
    }

    public static void assertMatches(String method, String path, int status, String contentType, String location, String body) {
        assertThat(violations(method, path, status, contentType, location, body))
                .as("%s %s responded %d: %s", method, path, status, body)
                .isEmpty();
    }
}
