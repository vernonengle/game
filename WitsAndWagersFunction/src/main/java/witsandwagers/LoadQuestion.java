package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.google.gson.Gson;
import dynamodb.DynamoDBClientUtil;
import pojo.GatewayResponse;
import util.ResponseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class LoadQuestion implements RequestHandler<S3EventNotification, Object> {

    public Object handleRequest(final S3EventNotification input, final Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, String> headers = ResponseUtil.getHeaderMap("application/json");
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
        S3EventNotificationRecord record = input.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String questions = s3Client.getObjectAsString(srcBucket, "questions");
        logger.log("questions: " + questions);
        StringTokenizer tokenizer = new StringTokenizer(questions, System.lineSeparator());
        List<String> questionLines = new ArrayList<>();
        String questionsTableName = System.getenv("QUESTIONS_TABLE_NAME");
        DynamoDB dynamoDB = DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1);
        Table questionsTable = dynamoDB.getTable(questionsTableName);

        while(tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            logger.log("line: " + line);
            questionLines.add(line);
            StringTokenizer lineTokenizer = new StringTokenizer(line, "::");
            Item question = new Item()
                    .withPrimaryKey("id", lineTokenizer.nextToken())
                    .withString("question", lineTokenizer.nextToken())
                    .withNumber("answer", Integer.valueOf(lineTokenizer.nextToken()));
            questionsTable.putItem(question);
        }
        Gson gson = new Gson();
        String output = gson.toJson(questionLines);
        logger.log(output);
        return new GatewayResponse(output, headers, 200);
    }
}
