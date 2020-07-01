package com.kmk.powerpeaks.webhook;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.kmk.powerpeaks.webhook.model.AccessTokenResponse;
import com.kmk.powerpeaks.webhook.model.OAuthData;
import com.kmk.powerpeaks.webhook.model.OAuthRequest;
import com.kmk.powerpeaks.webhook.model.WebhookEventRequest;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenRefreshFunction implements RequestHandler<SNSEvent, String> {

    private static final String TABLE_KEY = "athlete_id";
    private static final String STRAVA_OAUTH_REQUEST_URL = "https://www.strava.com/api/v3/oauth/token";

    private static final AccessTokenResponseMapper mapper = new AccessTokenResponseMapper();

    private static final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
    private static final DynamoDB dynamoDB = new DynamoDB(ddb);
    private static final Table table = dynamoDB.getTable(System.getenv("AUTH_TABLE_NAME"));

    @Override
    public String handleRequest(SNSEvent event, Context context)
    {
        refreshToken(event);
        return "200";
    }

    private void refreshToken(SNSEvent event) {

        System.out.println("Records Size: " + event.getRecords().size());
        List<Long> athleteIds =
                event.getRecords()
                     .stream()
                     .map(SNSEvent.SNSRecord::getSNS)
                     .map(SNSEvent.SNS::getMessage)
                     .peek(msg -> System.out.println("Message: " + msg))
                     .map(this::mapWebhookEventRequest)
                     .filter(Objects::nonNull)
                     .map(WebhookEventRequest::getOwnerId)
                     .collect(Collectors.toList());
        System.out.println("Athlete IDs Size: " + athleteIds.size());

        athleteIds.stream()
                  .map(athleteId -> table.getItem(TABLE_KEY, Long.toString(athleteId)))
                  .map(Item::toJSONPretty)
                  .map(this::mapItemToOAuthData)
                  .filter(Objects::nonNull)
                  .filter(this::isTokenExpired)
                  .map(this::requestNewToken)
                  .forEach(this::updateToken);
    }

    private boolean isTokenExpired(OAuthData oAuthData) {

        // Strava will return a new token if within 1 hour of expiry
        long expiry = Instant.ofEpochSecond(oAuthData.getExpiresAt()).minus(1, ChronoUnit.HOURS).getEpochSecond();
        long now = Instant.now().getEpochSecond();

        if (oAuthData.getAccessToken().isEmpty() || now >= expiry) {
            System.out.println("Access token has expired");
            return true;
        } else {
            System.out.println("Access token is still valid");
            return false;
        }
    }

    private void updateToken(OAuthData oAuthData) {
        //Refresh token in DynamoDB
        Item item = new Item()
                .withPrimaryKey(TABLE_KEY, oAuthData.getAthleteId())
                .withString("client_id", oAuthData.getClientId())
                .withString("client_secret", oAuthData.getClientSecret())
                .withString("access_token", oAuthData.getAccessToken())
                .withString("refresh_token", oAuthData.getRefreshToken())
                .withLong("expires_at", oAuthData.getExpiresAt());
        table.putItem(item);
    }

    private OAuthData requestNewToken(OAuthData oAuthData) {
        // Call Strava API for new token
        OAuthRequest request = new OAuthRequest();
        request.setClientId(Long.valueOf(oAuthData.getClientId()));
        request.setClientSecret(oAuthData.getClientSecret());
        request.setRefreshToken(oAuthData.getRefreshToken());

        List<String> headers = List.of("Content-Type", "application/json");
        AsyncHttpClient httpClient = new AsyncHttpClient();
        AccessTokenResponse response = httpClient.executePostRequest(request, STRAVA_OAUTH_REQUEST_URL, headers)
                                                 .thenApply(HttpResponse::body)
                                                 .thenApply(mapper::readAccessTokenResponse)
                                                 .toCompletableFuture().join();

        if (response.getAccessToken().equals(oAuthData.getAccessToken())) {
            System.out.println("Strava returned same access token");
        } else {
            System.out.println("Strava returned new access token");
            oAuthData.setAccessToken(response.getAccessToken());
            oAuthData.setRefreshToken(response.getRefreshToken());
            oAuthData.setExpiresAt(response.getExpiresAt());
        }

        return oAuthData;
    }

    private OAuthData mapItemToOAuthData(String itemJson) {

        try {
            return mapper.readValue(itemJson, OAuthData.class);
        } catch (IOException ex) {
            return null;
        }
    }

    private WebhookEventRequest mapWebhookEventRequest(String message) {

        try {
            return mapper.readValue(message, WebhookEventRequest.class);
        } catch (IOException ex) {
            return null;
        }
    }
}
