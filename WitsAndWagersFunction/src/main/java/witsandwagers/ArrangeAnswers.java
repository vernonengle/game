package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import dynamodb.DynamoDBClientUtil;
import pojo.GatewayResponse;
import util.ResponseUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArrangeAnswers implements RequestHandler<LinkedHashMap<String, Object>, GatewayResponse> {

    public GatewayResponse handleRequest(final LinkedHashMap<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = ResponseUtil.getHeaderMap("application/json");
        String currentQuestionTableName = System.getenv("CURRENT_QUESTION_TABLE_NAME");
        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);

        Table currentQuestionTable = dynamoDB.getTable(currentQuestionTableName);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", "currentQuestion"));

        logger.log("Querying current question table");
        ItemCollection<QueryOutcome> query = currentQuestionTable.query(spec);
        String currentQuestionId = query.iterator().next().getString("questionId");
        logger.log("Current question is " + currentQuestionId);

        String playerTableName = System.getenv("PLAYER_TABLE_NAME");
        Table playerTable = dynamoDB.getTable(playerTableName);
        ScanSpec scanSpec = new ScanSpec()
                .withProjectionExpression("id");

        logger.log("Querying players table");
        ItemCollection<ScanOutcome> playerScan = playerTable.scan(scanSpec);
        String gameTableName = System.getenv("GAME_TABLE_NAME");
        Table gameTable = dynamoDB.getTable(gameTableName);
        List<Item> gameItemList = new ArrayList<>();
        List<String> gameItemStringList = new ArrayList<>();
        playerScan.forEach(item -> {
                    String playerId = item.getString("id");
                    logger.log("Player " + playerId + " scanned");
                    QuerySpec gameSpec = new QuerySpec()
                            .withKeyConditionExpression("id = :v_id")
                            .withValueMap(new ValueMap()
                                    .withString(":v_id", playerId + ":" + currentQuestionId));

                    gameTable.query(gameSpec).forEach(gameItem -> {
                                gameItemList.add(gameItem);
                                gameItemStringList.add(gameItem.toJSONPretty());
                            }
                    );
                }
        );
        gameItemList.sort(Comparator.comparing(o -> o.getNumber("answer")));
        switch (gameItemList.size()) {
            case 5:
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(0).getString("id")), new AttributeUpdate("position").put(1));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(1).getString("id")), new AttributeUpdate("position").put(2));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(2).getString("id")), new AttributeUpdate("position").put(3));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(3).getString("id")), new AttributeUpdate("position").put(4));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(4).getString("id")), new AttributeUpdate("position").put(5));
                break;
            case 6:
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(0).getString("id")), new AttributeUpdate("position").put(1));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(1).getString("id")), new AttributeUpdate("position").put(2));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(2).getString("id")), new AttributeUpdate("position").put(3));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(3).getString("id")), new AttributeUpdate("position").put(5));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(4).getString("id")), new AttributeUpdate("position").put(6));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(5).getString("id")), new AttributeUpdate("position").put(7));
                break;
            case 7:
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(0).getString("id")), new AttributeUpdate("position").put(1));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(1).getString("id")), new AttributeUpdate("position").put(2));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(2).getString("id")), new AttributeUpdate("position").put(3));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(3).getString("id")), new AttributeUpdate("position").put(4));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(4).getString("id")), new AttributeUpdate("position").put(5));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(5).getString("id")), new AttributeUpdate("position").put(6));
                gameTable.updateItem(new PrimaryKey("id",gameItemList.get(6).getString("id")), new AttributeUpdate("position").put(7));
                break;
            default:
                throw new IllegalArgumentException("Number of players should be 5-7");
        }
        Gson gson = new Gson();
        String output = gson.toJson(gameItemList);
        logger.log(output);
        return new GatewayResponse(output, headers, 200);
    }
}
