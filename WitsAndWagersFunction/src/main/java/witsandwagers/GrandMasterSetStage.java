package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dynamodb.DynamoDBClientUtil;
import pojo.GatewayResponse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class GrandMasterSetStage implements RequestHandler<LinkedHashMap<String, Object>, GatewayResponse> {
    public GatewayResponse handleRequest(final LinkedHashMap<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);

        String Stage_TableName = System.getenv("STAGE_TABLE_NAME");
        String id = (String) ((LinkedHashMap<String, Object>) input.get("queryStringParameters")).get("id");
        Table table = dynamoDB.getTable(Stage_TableName);

        ScanSpec scanSpec = new ScanSpec()
                .withProjectionExpression("id");
        ItemCollection<ScanOutcome> playerScan = table.scan(scanSpec);
        playerScan.forEach(item -> {
            String playerId = item.getString("id");
            KeyAttribute primaryKey = new KeyAttribute("id", playerId);
            table.deleteItem(primaryKey);
        });

        Item item = new Item()
                .withPrimaryKey("id", id);
        table.putItem(item);
        String output = item.toJSONPretty();

        return new GatewayResponse(output,headers, 200);
    }
}