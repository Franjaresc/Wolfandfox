package dev.franjaresc.wolfandfox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Calls a running server over real HTTP and checks every response against the contract. */
public final class ApiTestClient {

    public static final String ROUTE_CLOSURES = "/api/v1/route-closures";

    public record Response(int status, String contentType, String location, String body) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final int port;

    public ApiTestClient(int port) {
        this.port = port;
    }

    public Response closeRoute(String body) throws IOException, InterruptedException {
        return send(request(ROUTE_CLOSURES)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    public Response get(String pathAndQuery) throws IOException, InterruptedException {
        return send(request(pathAndQuery).GET().build());
    }

    private HttpRequest.Builder request(String pathAndQuery) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery));
    }

    private Response send(HttpRequest request) throws IOException, InterruptedException {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        var result = new Response(
                response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(null),
                response.headers().firstValue("Location").orElse(null),
                response.body());
        ContractValidator.assertMatches(
                request.method(),
                request.uri().getPath(),
                result.status(),
                result.contentType(),
                result.location(),
                result.body());
        return result;
    }
}
