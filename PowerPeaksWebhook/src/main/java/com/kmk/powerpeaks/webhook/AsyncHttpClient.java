package com.kmk.powerpeaks.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class AsyncHttpClient {

    private AccessTokenResponseMapper mapper = new AccessTokenResponseMapper();

    public CompletionStage<HttpResponse<String>> executeGetRequest(String url, List<String> headers) {

        HttpRequest request = buildBaseRequest(url, headers).build();

        return HttpClient.newHttpClient()
                         .sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletionStage<HttpResponse<String>> executePostRequest(Object requestObj, String url, List<String> headers) {

        String requestBody = mapper.prettyWriteValueAsString(requestObj);

        HttpRequest request = buildBaseRequest(url, headers)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return HttpClient.newHttpClient()
                         .sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder buildBaseRequest(String url, List<String> headers) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                          .uri(URI.create(url))
                          .timeout(Duration.ofSeconds(10));
        if (headers != null && !headers.isEmpty()) {
            builder.headers(headers.toArray(String[]::new));
        }

        return builder;
    }
}
