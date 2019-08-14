package witsandwagers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import dynamodb.DynamoDBClientUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import pojo.GatewayResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DynamoDBClientUtil.class})
public class GrandMasterSetStageTest {

    GrandMasterSetStage grandMasterSetStage = new GrandMasterSetStage();

    @Rule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    static DynamoDB dynamoDB;

    Context context = PowerMockito.mock(Context.class);

    @BeforeClass
    public static void setup() {
        PowerMockito.mockStatic(DynamoDBClientUtil.class);
        dynamoDB = PowerMockito.mock(DynamoDB.class);
        PowerMockito.when(DynamoDBClientUtil.getDynamoDBClient(Regions.AP_SOUTHEAST_1)).thenReturn(dynamoDB);
        environmentVariables.set("STAGE_TABLE_NAME","STAGE_TABLE_DB");
    }

    @Test
    public void handleRequest() throws IOException {

    }
}