package dynamodb;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public class DynamoDBClientUtil {

    public static DynamoDB getDynamoDBClient(Regions region) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(region).build();
        return new DynamoDB(client);
    }
}
