package dev.franjaresc.wolfandfox.route.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.franjaresc.wolfandfox.ContractValidator;
import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.application.port.in.CloseRouteUseCase;
import dev.franjaresc.wolfandfox.route.application.port.in.FindRouteClosuresQuery;
import dev.franjaresc.wolfandfox.route.application.port.in.GetRouteClosureQuery;
import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureNotFoundException;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RouteClosureController.class)
@Import(StrictJsonConfig.class)
class RouteClosureControllerTest {

    private static final String URL = "/api/v1/route-closures";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final String VALID_BODY = """
            {"routeCode":"RT-0001","closureDate":"2026-01-15","vehicleCode":"VAN-0042","driverCode":"DRV-0007",
            "packagesDelivered":42,"packagesFailed":3,"packagesReturned":1,"kmDriven":87,"notes":"Fictional note"}
            """;
    private static final RouteClosure CLOSURE = RouteClosure.close(
            new RouteCode("RT-0001"),
            DATE,
            new VehicleCode("VAN-0042"),
            new DriverCode("DRV-0007"),
            new PackageCounts(42, 3, 1),
            new Kilometres(87),
            null,
            new UserId("demo.supervisor"),
            Instant.parse("2026-01-15T18:30:00.123456Z"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CloseRouteUseCase closeRoute;

    @MockitoBean
    private FindRouteClosuresQuery findRouteClosures;

    @MockitoBean
    private GetRouteClosureQuery getRouteClosure;

    @Test
    void closingARouteReturnsCreatedWithLocationAndDerivedTotal() throws Exception {
        when(closeRoute.close(any())).thenReturn(CLOSURE);

        send(postBody(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", URL + "/" + CLOSURE.id().value()))
                .andExpect(jsonPath("$.id").value(CLOSURE.id().value().toString()))
                .andExpect(jsonPath("$.totalPackages").value(46))
                .andExpect(jsonPath("$.closedBy").value("demo.supervisor"))
                .andExpect(jsonPath("$.closedAt").value("2026-01-15T18:30:00.123456Z"))
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void passesTheRequestToTheUseCase() throws Exception {
        when(closeRoute.close(any())).thenReturn(CLOSURE);

        send(postBody(VALID_BODY)).andExpect(status().isCreated());

        verify(closeRoute)
                .close(new CloseRouteUseCase.CloseRouteCommand(
                        new RouteCode("RT-0001"),
                        DATE,
                        new VehicleCode("VAN-0042"),
                        new DriverCode("DRV-0007"),
                        new PackageCounts(42, 3, 1),
                        new Kilometres(87),
                        "Fictional note"));
    }

    @Test
    void validationProblemHasTheFullShapeWithFieldErrors() throws Exception {
        var body = VALID_BODY.replace("\"kmDriven\":87", "\"kmDriven\":-1");

        send(postBody(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:validation-error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
                .andExpect(jsonPath("$.instance").value(URL))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("kmDriven"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
        verifyNoInteractions(closeRoute);
    }

    @Test
    void validationMessagesAreEnglishWhateverTheRequestedLanguage() throws Exception {
        var body = VALID_BODY.replace("\"kmDriven\":87", "\"kmDriven\":-1");

        send(postBody(body).header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than or equal to 0"));
    }

    @Test
    void reportsEveryMissingRequiredField() throws Exception {
        send(postBody("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(8)))
                .andExpect(jsonPath("$.errors[*].field", hasItem("routeCode")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("closureDate")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("kmDriven")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            "routeCode":"RT-0001"     | "routeCode":"rt-0001"     | routeCode
            "vehicleCode":"VAN-0042"  | "vehicleCode":"VAN 0042"  | vehicleCode
            "driverCode":"DRV-0007"   | "driverCode":"D"          | driverCode
            "closureDate":"2026-01-15"| "closureDate":"15/01/2026"| closureDate
            "packagesDelivered":42    | "packagesDelivered":-1    | packagesDelivered
            "packagesFailed":3        | "packagesFailed":-1       | packagesFailed
            "packagesReturned":1      | "packagesReturned":-1     | packagesReturned
            "kmDriven":87             | "kmDriven":3000000000     | kmDriven
            """)
    void rejectsAnInvalidFieldNamingIt(String valid, String invalid, String field) throws Exception {
        send(postBody(VALID_BODY.replace(valid, invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verifyNoInteractions(closeRoute);
    }

    @Test
    void rejectsNotesOverTheMaximumLength() throws Exception {
        var body = VALID_BODY.replace("Fictional note", "n".repeat(1001));

        send(postBody(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("notes"));
    }

    @Test
    void rejectsAnUnknownJsonFieldNamingIt() throws Exception {
        var body = VALID_BODY.replace("{\"routeCode\"", "{\"closedBy\":\"someone.else\",\"routeCode\"");

        send(postBody(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("closedBy"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a recognized field"));
        verifyNoInteractions(closeRoute);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            "kmDriven":87          | "kmDriven":87.5    | kmDriven
            "kmDriven":87          | "kmDriven":"87"    | kmDriven
            "routeCode":"RT-0001"  | "routeCode":1234   | routeCode
            "notes":"Fictional note" | "notes":1.5      | notes
            "notes":"Fictional note" | "notes":true     | notes
            """)
    void rejectsJsonOfTheWrongTypeInsteadOfCoercingIt(String valid, String invalid, String field) throws Exception {
        send(postBody(VALID_BODY.replace(valid, invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verifyNoInteractions(closeRoute);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{not json", ""})
    void rejectsAMalformedBodyWithoutInternalDetails(String body) throws Exception {
        send(postBody(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    void duplicateProblemHasTheFullShape() throws Exception {
        when(closeRoute.close(any())).thenThrow(new DuplicateRouteClosureException(new RouteCode("RT-0001"), DATE));

        send(postBody(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:duplicate-route-closure"))
                .andExpect(jsonPath("$.code").value("ROUTE_CLOSURE_DUPLICATE"))
                .andExpect(jsonPath("$.title").value("Route already closed"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Route RT-0001 is already closed for 2026-01-15"))
                .andExpect(jsonPath("$.instance").value(URL));
    }

    @Test
    void returnsAnExistingClosure() throws Exception {
        when(getRouteClosure.get(CLOSURE.id())).thenReturn(CLOSURE);

        send(get(URL + "/" + CLOSURE.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeCode").value("RT-0001"));
    }

    @Test
    void notFoundProblemHasTheFullShape() throws Exception {
        when(getRouteClosure.get(CLOSURE.id())).thenThrow(new RouteClosureNotFoundException(CLOSURE.id()));
        var path = URL + "/" + CLOSURE.id().value();

        send(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:route-closure-not-found"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Route closure not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString(CLOSURE.id().value().toString())))
                .andExpect(jsonPath("$.instance").value(path));
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() throws Exception {
        send(get(URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
        verifyNoInteractions(getRouteClosure);
    }

    @Test
    void listAppliesDefaultPagingWithoutFilters() throws Exception {
        var query = new PageQuery(0, 20);
        when(findRouteClosures.find(new RouteClosureFilter(null, null), query))
                .thenReturn(new PageResult<>(List.of(CLOSURE), query, 1));

        send(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listPassesFiltersAndAcceptsTheMaximumPageSize() throws Exception {
        var query = new PageQuery(2, 100);
        var filter = new RouteClosureFilter(DATE, new RouteCode("RT-0001"));
        when(findRouteClosures.find(filter, query)).thenReturn(new PageResult<>(List.of(), query, 0));

        send(get(URL).param("date", "2026-01-15").param("routeCode", "RT-0001").param("page", "2").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @ParameterizedTest
    @CsvSource({
        "page,-1", "page,abc", "size,0", "size,-1", "size,101", "size,abc", "date,15-01-2026", "routeCode,rt-0001"
    })
    void listRejectsInvalidParametersNamingThem(String parameter, String value) throws Exception {
        send(get(URL).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field", endsWith(parameter)));
        verifyNoInteractions(findRouteClosures);
    }

    @Test
    void unexpectedFailureRevealsNothingInternal() throws Exception {
        when(closeRoute.close(any()))
                .thenThrow(new IllegalStateException("SQL [insert into route_closure] at org.hibernate.Secret"));

        var result = send(postBody(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:wolfandfox:problem:internal-error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andReturn();

        assertRevealsNothingInternal(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"routeCode\":", "{\"unexpected\":1}", "{\"kmDriven\":\"87\"}"})
    void bodyErrorsRevealNothingInternal(String body) throws Exception {
        var result = send(postBody(body)).andExpect(status().isBadRequest()).andReturn();

        assertRevealsNothingInternal(result);
    }

    @Test
    void acceptJsonStillGetsAProblemContentType() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON));
    }

    @Test
    void frameworkErrorsAreProblemsToo() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_SUPPORTED"));
        mvc.perform(delete(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_SUPPORTED"));
    }

    @Test
    void anUnknownPathIsNotFoundWithACode() throws Exception {
        mvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private static void assertRevealsNothingInternal(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentAsString())
                .doesNotContainIgnoringCase("exception")
                .doesNotContainIgnoringCase("sql")
                .doesNotContain("tools.jackson")
                .doesNotContain("org.")
                .doesNotContain("dev.franjaresc")
                .doesNotContain("\tat ")
                .doesNotContain("route_closure");
    }

    private static MockHttpServletRequestBuilder postBody(String body) {
        return post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Sends the request and checks the response against the contract, whatever the test expects. */
    private ResultActions send(MockHttpServletRequestBuilder request) throws Exception {
        var actions = mvc.perform(request);
        var response = actions.andReturn().getResponse();
        var servletRequest = actions.andReturn().getRequest();
        ContractValidator.assertMatches(
                servletRequest.getMethod(),
                servletRequest.getRequestURI(),
                response.getStatus(),
                response.getContentType(),
                response.getHeader("Location"),
                response.getContentAsString());
        return actions;
    }
}
