package witsandwagers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import pojo.GatewayResponse;

import java.util.HashMap;
import java.util.Map;

public class ShowAnswers implements RequestHandler<Object, Object> {

    public Object handleRequest(final Object input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        logger.log("Initializing db client");
        final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
        String gameTableName = System.getenv("GAME_TABLE_NAME");
        logger.log("Getting table " + gameTableName);
        TableDescription table = ddb.describeTable(gameTableName).getTable();
        logger.log("Describe table done " + gameTableName);
        String output = String.format("{ \"message\": \"show answer\", \"location\": \"%s\" }", table.getTableName());
        return new GatewayResponse(output, headers, 200);
    }
}
