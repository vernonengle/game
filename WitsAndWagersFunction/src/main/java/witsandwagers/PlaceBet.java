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
import dynamodb.DynamoDBClientUtil;
import pojo.GatewayResponse;
import util.ResponseUtil;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaceBet implements RequestHandler<LinkedHashMap<String, Object>, GatewayResponse> {

    public GatewayResponse handleRequest(final LinkedHashMap<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = ResponseUtil.getHeaderMap("application/json");
        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);

        logger.log("Querying current question table");
        String currentQuestionId = getCurrentQuestionId(dynamoDB);
        QuerySpec spec;
        logger.log("Current question is " + currentQuestionId);

        String playerId = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("playerId");
        String amount = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("amount");
        String position = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("position");
        String betId = playerId + ":" + currentQuestionId + ":" + position;
        spec = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", betId));
        String betsTableName = System.getenv("BETS_TABLE_NAME");
        Table betsTable = dynamoDB.getTable(betsTableName);
        ItemCollection<QueryOutcome> bet = betsTable.query(spec);
        String output = "";
        if (bet.iterator().hasNext()) {
            betsTable.updateItem(new PrimaryKey("id", betId), new AttributeUpdate("amount").put(new BigDecimal(amount)));
            output = betsTable.getItem(new PrimaryKey("id", betId)).toJSONPretty();
        } else {
            ScanSpec scanSpec = new ScanSpec()
                    .withFilterExpression("contains(id, :v_playerId)")
                    .withValueMap(new ValueMap()
                            .withString(":v_playerId", playerId));
            ItemCollection<ScanOutcome> scan = betsTable.scan(scanSpec);
            AtomicInteger betCount = new AtomicInteger();
            scan.forEach(item -> betCount.getAndIncrement());
            if (betCount.get() < 2) {
                Item newBet = new Item()
                        .withPrimaryKey("id", betId)
                        .withNumber("amount", new BigDecimal(amount))
                        .withNumber("position", Integer.valueOf(position));
                betsTable.putItem(newBet);
                output = newBet.toJSONPretty();
            }
        }
        logger.log(output);
        return new GatewayResponse(output, headers, 200);
    }

    private String getCurrentQuestionId(DynamoDB dynamoDB) {
        String currentQuestionTableName = System.getenv("CURRENT_QUESTION_TABLE_NAME");
        Table currentQuestionTable = dynamoDB.getTable(currentQuestionTableName);
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", "currentQuestion"));
        ItemCollection<QueryOutcome> query = currentQuestionTable.query(spec);
        return query.iterator().next().getString("questionId");
    }
}
