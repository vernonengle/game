package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import pojo.GatewayResponse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShowQuestion implements RequestHandler<LinkedHashMap<String, Object>, GatewayResponse> {

    public GatewayResponse handleRequest(final LinkedHashMap<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        String questionsTableName = System.getenv("QUESTIONS_TABLE_NAME");
        String currentQuestionTableName = System.getenv("CURRENT_QUESTION_TABLE_NAME");
        String id = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("id");
        logger.log("Question id: " + id);
        logger.log("Table name: " + questionsTableName);
        logger.log("initializing client");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.AP_SOUTHEAST_1).build();
        DynamoDB dynamoDB = new DynamoDB(client);

        logger.log("Getting table " + questionsTableName);
        Table questionsTable = dynamoDB.getTable(questionsTableName);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", id));

        ItemCollection<QueryOutcome> query = questionsTable.query(spec);
        Item question = query.iterator().next();
        if (question != null) {
            Table currentQuestionTable = dynamoDB.getTable(currentQuestionTableName);
            KeyAttribute primaryKey = new KeyAttribute("id","currentQuestion");
            currentQuestionTable.deleteItem(primaryKey);
            Item currentQuestion = new Item()
                    .withPrimaryKey("id", "currentQuestion")
                    .withString("questionId", question.getString("id"));
            currentQuestionTable.putItem(currentQuestion);
        }
        String output = question.toJSONPretty();
        return new GatewayResponse(output, headers, 200);
    }
}
