package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dynamodb.DynamoDBClientUtil;
import pojo.GatewayResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class SubmitAnswer implements RequestHandler<LinkedHashMap<String, Object>, GatewayResponse> {
    public GatewayResponse handleRequest(final LinkedHashMap<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);


        String GameTable = System.getenv("GAME_TABLE_NAME");
        String currentQuestionTableName = System.getenv("CURRENT_QUESTION_TABLE_NAME");
        String id = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("id");
        String answer = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("answer");
        Table currentQuestionTable = dynamoDB.getTable(currentQuestionTableName);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", "currentQuestion"));

        logger.log("Querying current question table");
        ItemCollection<QueryOutcome> query = currentQuestionTable.query(spec);
        String currentQuestionId = query.iterator().next().getString("questionId");

        Table table = dynamoDB.getTable(GameTable);

        KeyAttribute primaryKey = new KeyAttribute("id", id+":"+currentQuestionId);
        currentQuestionTable.deleteItem(primaryKey);
        Item item = new Item()
                    .withPrimaryKey("id", id+":"+currentQuestionId)
                    .withString("answer",answer );
            table.putItem(item);
            String output1 = item.toJSONPretty();

            return new GatewayResponse(output1,headers, 200);

    }
}