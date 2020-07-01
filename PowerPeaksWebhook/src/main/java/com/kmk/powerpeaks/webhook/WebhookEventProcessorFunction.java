package com.kmk.powerpeaks.webhook;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kmk.powerpeaks.webhook.model.OAuthData;
import com.kmk.powerpeaks.webhook.model.Power;
import com.kmk.powerpeaks.webhook.model.WebhookEventRequest;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.ActivitiesApi;
import io.swagger.client.api.StreamsApi;
import io.swagger.client.auth.OAuth;
import io.swagger.client.model.DetailedActivity;
import io.swagger.client.model.PowerStream;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebhookEventProcessorFunction implements RequestHandler<SQSEvent, String> {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final AccessTokenResponseMapper mapper = new AccessTokenResponseMapper();

    private static final String AUTH_TABLE_KEY = "athlete_id";
    private static final String EVENTS_TABLE_KEY = "activity_id";
    private static final String ACTIVITIES_TABLE_KEY = "activity_id";
    private static final String POWER_TABLE_KEY = "activity_id";

    private static final String WATTS_STREAM_TYPE = "watts";

    private static final java.time.format.DateTimeFormatter
            DATE_TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
    private static final DynamoDB dynamoDB = new DynamoDB(ddb);
    private static final Table authTable = dynamoDB.getTable(System.getenv("AUTH_TABLE_NAME"));
    private static final Table eventsTable = dynamoDB.getTable(System.getenv("EVENTS_TABLE_NAME"));
    private static final Table activitiesTable = dynamoDB.getTable(System.getenv("ACTIVITIES_TABLE_NAME"));
    private static final Table powerTable = dynamoDB.getTable(System.getenv("POWER_TABLE_NAME"));

    private static LambdaLogger logger;

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        logger = context.getLogger();

        logger.log("Starting function...");

        String accessToken = getAccessToken();
        ApiClient client = Configuration.getDefaultApiClient();
        OAuth stravaOAuth = (OAuth) client.getAuthentication("strava_oauth");
        stravaOAuth.setAccessToken(accessToken);

        logger.log("Records Size: " + sqsEvent.getRecords().size());

        // Get incoming events
        List<WebhookEventRequest> events =
                sqsEvent.getRecords().stream()
                        .map(SQSEvent.SQSMessage::getBody)
                        .map(this::mapMessageToObject)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .peek(event -> logger.log(String.format("Event received: %s", event.toString())))
                        .collect(Collectors.toList());

        // Save events data
        events.forEach(this::persistEvents);

        // Save activities data
        ActivitiesApi activitiesApi = new ActivitiesApi(client);
        events.stream()
              .map(WebhookEventRequest::getObjectId)
              .map(activityId -> getActivity(activitiesApi, activityId))
              .flatMap(Optional::stream)
              .peek(activity -> logger.log(String.format("Persisting Activity: %s", activity.toString())))
              .forEach(this::persistActivity);

        // Save power data
        StreamsApi streamsApi = new StreamsApi(client);
        events.stream()
              .map(WebhookEventRequest::getObjectId)
              .map(activityId -> getPowerStream(streamsApi, activityId)
                      .map(powerStream -> mapPowerStream(activityId, powerStream)))
              .flatMap(Optional::stream)
              .peek(power -> logger.log(String.format("Persisting Power Data: %s", power.toString())))
              .forEach(this::persistPowerStream);

        // log execution details
        LoggerUtil.logEnvironment(sqsEvent, context, gson);

        sqsEvent.getRecords()
                .stream()
                .map(SQSEvent.SQSMessage::getBody)
                .forEach(System.out::println);

        return "200 OK";
    }

    private void persistEvents(WebhookEventRequest webhookEventRequest) {

        Item item = new Item()
                .withPrimaryKey(EVENTS_TABLE_KEY, webhookEventRequest.getObjectId())
                .withString("object_type", webhookEventRequest.getObjectType())
                .withString("aspect_type", webhookEventRequest.getAspectType())
                .withMap("updates", webhookEventRequest.getUpdates())
                .withLong("owner_id", webhookEventRequest.getOwnerId())
                .withInt("subscription_id", webhookEventRequest.getSubscriptionId())
                .withLong("event_time", webhookEventRequest.getEventTime())
                .withString("created_at", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        eventsTable.putItem(item);
    }

    private String getAccessToken() {

        return Optional.of(authTable.getItem(AUTH_TABLE_KEY, Long.toString(2402700)))
                       .map(Item::toJSONPretty)
                       .flatMap(this::mapItemToOAuthData)
                       .get()
                       .getAccessToken();
    }

    private Optional<OAuthData> mapItemToOAuthData(String itemJson) {

        try {
            return Optional.of(mapper.readValue(itemJson, OAuthData.class));
        } catch (IOException ex) {
            logger.log(String.format("Error mapping DynamoDB AUTH item to OAuthData object. Item to map: %s",
                                     itemJson));
            return Optional.empty();
        }
    }

    private Optional<WebhookEventRequest> mapMessageToObject(String message) {

        try {
            return Optional.of(mapper.readValue(message, WebhookEventRequest.class));
        } catch (IOException e) {
            logger.log(String.format("Error mapping webhook event message to OAuthData object. Item to map: %s",
                                     message));
            return Optional.empty();
        }
    }

    private Optional<DetailedActivity> getActivity(ActivitiesApi api, Long activityId) {

        try {
            return Optional.of(api.getActivityById(activityId, false));
        } catch (ApiException e) {
            logger.log(String.format("Error fetching activity. Activity ID: %s. Exception: %s",
                                     activityId, e.getCause().getMessage()));
            return Optional.empty();
        }
    }

    private void persistActivity(DetailedActivity detailedActivity) {

        String startTime = "";
        if (detailedActivity.getStartDate() != null) {
            startTime = detailedActivity.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        Item item = new Item()
                .withPrimaryKey(ACTIVITIES_TABLE_KEY, detailedActivity.getId())
                .withInt("athlete_id", Math.toIntExact(detailedActivity.getAthlete().getId()))
                .withString("name", detailedActivity.getName())
                .withString("description", Optional.ofNullable(detailedActivity.getDescription()).orElse(""))
                .withFloat("distance", detailedActivity.getDistance())
                .withInt("moving_time", detailedActivity.getMovingTime())
                .withInt("elapsed_time", detailedActivity.getElapsedTime())
                .withFloat("total_elevation_gain", detailedActivity.getTotalElevationGain())
                .withInt("workout_type", Optional.ofNullable(detailedActivity.getWorkoutType()).orElse(0))
                .withString("start_date", startTime)
                .withInt("achievement_count", detailedActivity.getAchievementCount())
                .withInt("athlete_count", detailedActivity.getAthleteCount())
                .withInt("total_photo_count", detailedActivity.getTotalPhotoCount())
                .withBoolean("trainer", detailedActivity.isTrainer())
                .withBoolean("commute", detailedActivity.isCommute())
                .withBoolean("manual", detailedActivity.isManual())
                .withBoolean("private", detailedActivity.isPrivate())
                .withBoolean("flagged", detailedActivity.isFlagged())
                .withString("gear_id", detailedActivity.getGearId())
                .withFloat("average_speed", detailedActivity.getAverageSpeed())
                .withFloat("max_speed", detailedActivity.getMaxSpeed())
                .withFloat("average_watts", detailedActivity.getAverageWatts())
                .withInt("weighted_average_watts", detailedActivity.getWeightedAverageWatts())
                .withFloat("kilojoules", detailedActivity.getKilojoules())
                .withBoolean("device_watts", detailedActivity.isDeviceWatts())
                .withInt("max_watts", detailedActivity.getMaxWatts())
                .withFloat("elev_high", detailedActivity.getElevHigh())
                .withFloat("elev_low", detailedActivity.getElevLow());

        PrimaryKey key = new PrimaryKey(POWER_TABLE_KEY, detailedActivity.getId());
        item = setTimestamps(item, activitiesTable, key);

        activitiesTable.putItem(item);
    }

    private Optional<PowerStream> getPowerStream(StreamsApi streamsApi, Long activityId) {
        List<String> streamTypes = List.of(WATTS_STREAM_TYPE);
        try {
            return Optional.of(streamsApi.getActivityStreams(activityId, streamTypes, true).getWatts());
        } catch (ApiException e) {
            logger.log(String.format("Error fetching power stream data. Activity ID: %s. Exception: %s",
                                     activityId, e.getCause().getMessage()));
            return Optional.empty();
        }
    }

    private Power mapPowerStream(Long activityId, PowerStream powerStream) {

        Power power = new Power();
        power.setActivityId(activityId);
        List<Integer> powerList = powerStream.getData();
        Collections.replaceAll(powerList, null, 0);
        power.setPowerStream(powerList);

        return power;
    }

    private void persistPowerStream(Power power) {

        Item item = new Item()
                .withPrimaryKey(POWER_TABLE_KEY, power.getActivityId())
                .withList("power_stream", power.getPowerStream());

        PrimaryKey key = new PrimaryKey(POWER_TABLE_KEY, power.getActivityId());
        item = setTimestamps(item, powerTable, key);

        powerTable.putItem(item);
    }

    private Item setTimestamps(Item activity, Table table, PrimaryKey key) {

        String nowTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        String createdAt = Optional.ofNullable(table.getItem(key))
                                   .map(item -> Optional.ofNullable(item.getString("created_at"))
                                                        .orElse(""))
                                   .orElse(nowTime);

        return activity.withString("updated_at", nowTime).withString("created_at", createdAt);
    }
}
