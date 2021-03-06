package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.*;
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
        //if activestage is not place bet then return GatewayResponse("", headers, 200);
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = ResponseUtil.getHeaderMap("application/json");
        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);
        String Stage_TableName = System.getenv("STAGE_TABLE_NAME");
        Table table1 = dynamoDB.getTable(Stage_TableName);
        Item item1 = table1.getItem("id", "PlaceBetStage");

        if (!item1.getString("id").equals("PlaceBetStage")) {
            return new GatewayResponse("", headers, 200);
        }

        logger.log("Querying current question table");
        String currentQuestionId = getCurrentQuestionId(dynamoDB);
        QuerySpec spec, spec1;
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
        String PlayerTableName = System.getenv("PLAYER_TABLE_NAME");
        Table PlayerTable = dynamoDB.getTable(PlayerTableName);
        spec1 = new QuerySpec()
                .withKeyConditionExpression("id = :v_id")
                .withValueMap(new ValueMap()
                        .withString(":v_id", playerId));
        ItemCollection<QueryOutcome> query = PlayerTable.query(spec1);
        String PlayerMoney = query.iterator().next().getString("money");
        logger.log("player money is " + PlayerMoney);
        double Pmoney = Double.parseDouble(PlayerMoney);

        Integer amounty = Integer.parseUnsignedInt(amount);


        if (amounty <= Pmoney) {
            if (bet.iterator().hasNext()) {
                betsTable.updateItem(new PrimaryKey("id", betId), new AttributeUpdate("amount").put(new BigDecimal(amount)));
                double UpdateMoney = Pmoney - amounty;
                PlayerTable.updateItem(new PrimaryKey("id", playerId), new AttributeUpdate("money").put(new BigDecimal(UpdateMoney)));
                output = betsTable.getItem(new PrimaryKey("id", betId)).toJSONPretty();
            }
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
                double UpdateMoney = Pmoney - amounty;
                PlayerTable.updateItem(new PrimaryKey("id", playerId), new AttributeUpdate("money").put(new BigDecimal(UpdateMoney)));
                betsTable.putItem(newBet);
                output = newBet.toJSON();
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
