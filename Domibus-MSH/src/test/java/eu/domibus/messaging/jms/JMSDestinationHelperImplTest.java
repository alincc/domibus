package eu.domibus.messaging.jms;

import eu.domibus.api.regex.RegexUtil;
import eu.domibus.messaging.jms.JMSDestinationHelperImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

/**
 * Created by Cosmin Baciu on 02-Sep-16.
 */
@RunWith(JMockit.class)
public class JMSDestinationHelperImplTest {

    @Injectable
    RegexUtil regexUtil;

    @Injectable
    Properties domibusProperties;

    @Tested
    JMSDestinationHelperImpl jmsDestinationHelper;

    @Test
    public void testIsInternalWithNoInternalExpressionDefined() throws Exception {
        String queueName = "myQueue";
        new Expectations() {{
            domibusProperties.getProperty(anyString);
            result = null;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assert.assertFalse(internal);
    }

    @Test
    public void testIsInternal() throws Exception {
        final String queueName = "myQueue";
        new Expectations() {{
            domibusProperties.getProperty(anyString);
            result = "myexpression";

            regexUtil.matches("myexpression", queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assert.assertTrue(internal);

    }
}