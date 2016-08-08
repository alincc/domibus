package eu.domibus.ebms3.common.dao;

import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.wss4j.common.crypto.TrustStoreService;
import no.difi.vefa.edelivery.lookup.model.Endpoint;
import no.difi.vefa.edelivery.lookup.model.ProcessIdentifier;
import no.difi.vefa.edelivery.lookup.model.TransportProfile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/DynamicDiscoveryPModeProviderTest-context.xml")
@DirtiesContext
public class DynamicDiscoveryPModeProviderTest {

    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/";
    private static final String DYNRESPONDER_AND_PARTYSELF = "dynResponderAndPartySelf.xml";
    private static final String MULTIPLE_DYNRESPONDER_AND_PARTYSELF = "multipleDynResponderAndPartySelf.xml";
    private static final String MULTIPLE_DYNINITIATOR_AND_PARTYSELF = "multipleDynInitiatorAndPartySelf.xml";
    private static final String MULTIPLE_DYNRESPONDER_AND_DYNINITIATOR = "multipleDynResponderAndInitiator.xml";
    private static final String NO_DYNINITIATOR_AND_NOT_SELF = "noDynInitiatorAndNotPartySelf.xml";
    private static final String DYNAMIC_DISCOVERY_ENABLED = "dynamicDiscoveryEnabled.xml";

    private static final String TEST_KEYSTORE = "testkeystore.jks";

    private static final String EXPECTED_DYNAMIC_PROCESS_NAME = "testProcessDynamicExpected";
    private static final String UNEXPECTED_DYNAMIC_PROCESS_NAME = "testProcessStaticNotExpected";

    private static final String EXPECTED_COMMON_NAME = "DONOTUSE_TEST";

    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String ALIAS_CN_NOT_AVAILABLE = "cn_not_available";


    private static final String TEST_ACTION_VALUE = "testAction";
    private static final String TEST_SERVICE_VALUE = "serviceValue";
    private static final String TEST_SERVICE_TYPE = "serviceType";
    private static final String UNKNOWN_DYNAMIC_RESPONDER_PARTYID_VALUE = "unkownPartyIdValue";
    private static final String UNKNOWN_DYNAMIC_RESPONDER_PARTYID_TYPE = "unkownPartyIdType";


    private static final String PROCESSIDENTIFIER_ID = "testIdentifierId";
    private static final String PROCESSIDENTIFIER_SCHEME = "testIdentifierScheme";
    private static final String ADDRESS = "http://localhost:9090/anonymous/msh";


    @Autowired
    private JAXBContext jaxbConfigurationObjectContext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @InjectMocks
    private DynamicDiscoveryPModeProvider dynamicDiscoveryPModeProvider;

    @Spy
    ConfigurationDAO configurationDAO;

    @Spy
    DynamicDiscoveryService dynamicDiscoveryService;

    @Spy
    TrustStoreService trustStoreService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testFindDynamicReceiverProcesses_DynResponderAndPartySelf_ProcessInResultExpected() throws Exception {
        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + DYNRESPONDER_AND_PARTYSELF));
        assertTrue(initializeConfiguration(testData));

        DynamicDiscoveryPModeProvider classUnderTest = mock(DynamicDiscoveryPModeProvider.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(testData).when(classUnderTest).getConfiguration();

        Collection<Process> result = classUnderTest.findDynamicReceiverProcesses();

        assertEquals(1, result.size());

        Process foundProcess = result.iterator().next();
        assertTrue(foundProcess.isDynamicResponder());
        assertEquals(EXPECTED_DYNAMIC_PROCESS_NAME, foundProcess.getName());
    }

    @Test
    public void testFindDynamicReceiverProcesses_MultipleDynResponderAndPartySelf_MultipleProcessesInResultExpected() throws Exception {
        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + MULTIPLE_DYNRESPONDER_AND_PARTYSELF));
        assertTrue(initializeConfiguration(testData));

        DynamicDiscoveryPModeProvider classUnderTest = mock(DynamicDiscoveryPModeProvider.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(testData).when(classUnderTest).getConfiguration();

        Collection<Process> result = classUnderTest.findDynamicReceiverProcesses();

        assertEquals(3, result.size());

        for (Process process : result) {
            assertTrue(process.isDynamicResponder());
            assertNotEquals(UNEXPECTED_DYNAMIC_PROCESS_NAME, process.getName());
        }
    }

    @Test
    public void testFindDynamicReceiverProcesses_MultipleDynInitiatorAndPartySelf_NoProcessesInResultExpected() throws Exception {
        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + MULTIPLE_DYNINITIATOR_AND_PARTYSELF));
        assertTrue(initializeConfiguration(testData));

        DynamicDiscoveryPModeProvider classUnderTest = mock(DynamicDiscoveryPModeProvider.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(testData).when(classUnderTest).getConfiguration();

        Collection<Process> result = classUnderTest.findDynamicReceiverProcesses();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindDynamicReceiverProcesses_MultipleDynResponderAndDynInitiator_MultipleInResultExpected() throws Exception {
        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + MULTIPLE_DYNRESPONDER_AND_DYNINITIATOR));
        assertTrue(initializeConfiguration(testData));

        DynamicDiscoveryPModeProvider classUnderTest = mock(DynamicDiscoveryPModeProvider.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(testData).when(classUnderTest).getConfiguration();

        Collection<Process> result = classUnderTest.findDynamicReceiverProcesses();

        assertEquals(3, result.size());

        for (Process process : result) {
            assertTrue(process.isDynamicInitiator());
            assertTrue(process.isDynamicResponder());
        }
    }

    @Test
    public void testDoDynamicThings_NoCandidates_EbMS3ExceptionExpected() throws Exception {
        thrown.expect(EbMS3Exception.class);

        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + NO_DYNINITIATOR_AND_NOT_SELF));
        assertTrue(initializeConfiguration(testData));

        DynamicDiscoveryPModeProvider classUnderTest = mock(DynamicDiscoveryPModeProvider.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(testData).when(classUnderTest).getConfiguration();
        classUnderTest.dynamicReceiverProcesses = classUnderTest.findDynamicReceiverProcesses();

        UserMessage userMessage = buildUserMessageForDoDynamicThingsWithArguments(null, null, null, null, null, UUID.randomUUID().toString());

        classUnderTest.doDynamicThings(userMessage);
    }

    @Test
    public void testDoDynamicThings_DynamicDiscoveryEnabled_NewPartyAddedToCandidates() throws Exception {
        Configuration testData = (Configuration) jaxbConfigurationObjectContext.createUnmarshaller().unmarshal(new File(RESOURCE_PATH + DYNAMIC_DISCOVERY_ENABLED));
        assertTrue(initializeConfiguration(testData));

        doReturn(testData).when(configurationDAO).readEager();
        doReturn(true).when(configurationDAO).configurationExists();
        dynamicDiscoveryPModeProvider.dynamicReceiverProcesses = dynamicDiscoveryPModeProvider.findDynamicReceiverProcesses();

        Endpoint testDataEndpoint = buildAS4EndpointWithArguments(PROCESSIDENTIFIER_ID, PROCESSIDENTIFIER_SCHEME, ADDRESS, ALIAS_CN_AVAILABLE);
        doReturn(testDataEndpoint).when(dynamicDiscoveryService).lookupInformation(UNKNOWN_DYNAMIC_RESPONDER_PARTYID_VALUE, UNKNOWN_DYNAMIC_RESPONDER_PARTYID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);

        doReturn(true).when(trustStoreService).addCertificate(testDataEndpoint.getCertificate(), EXPECTED_COMMON_NAME, true);

        UserMessage userMessage = buildUserMessageForDoDynamicThingsWithArguments(TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE, UNKNOWN_DYNAMIC_RESPONDER_PARTYID_VALUE, UNKNOWN_DYNAMIC_RESPONDER_PARTYID_VALUE, UUID.randomUUID().toString());


        dynamicDiscoveryPModeProvider.doDynamicThings(userMessage);
        Party expectedParty = new Party();
        expectedParty.setName(EXPECTED_COMMON_NAME);
        expectedParty.setEndpoint(ADDRESS);
        Identifier expectedIdentifier = new Identifier();
        expectedIdentifier.setPartyId(EXPECTED_COMMON_NAME);
        expectedParty.getIdentifiers().add(expectedIdentifier);

        assertTrue(dynamicDiscoveryPModeProvider.getConfiguration().getBusinessProcesses().getParties().contains(expectedParty));
    }

    @Test
    public void testExtractCommonName_PublicKeyWithCommonNameAvailable_CorrectCommonNameExpected() throws Exception {

        X509Certificate testData = loadCertificateFromJKS(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE);
        assertNotNull(testData);

        String result = dynamicDiscoveryPModeProvider.extractCommonName(testData);

        assertEquals(EXPECTED_COMMON_NAME, result);
    }

    @Test
    public void testExtractCommonName_PublicKeyWithCommonNameNotAvailable_IllegalArgumentExceptionExpected() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        X509Certificate testData = loadCertificateFromJKS(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_NOT_AVAILABLE);
        assertNotNull(testData);

        dynamicDiscoveryPModeProvider.extractCommonName(testData);
    }


    /**
     * Build UserMessage for testing. Only the fields that are mandatory for the testing doDynamicThings are filled.
     *
     * @param action
     * @param serviceValue
     * @param serviceType
     * @param toPartyId
     * @param toPartyIdType
     * @param messageId
     * @return
     */
    private UserMessage buildUserMessageForDoDynamicThingsWithArguments(String action, String serviceValue, String serviceType, String toPartyId, String toPartyIdType, String messageId) {

        ObjectFactory ebmsObjectFactory = new ObjectFactory();

        UserMessage userMessageToBuild = ebmsObjectFactory.createUserMessage();

        MessageInfo messageInfo = ebmsObjectFactory.createMessageInfo();
        messageInfo.setMessageId(messageId);

        userMessageToBuild.setMessageInfo(messageInfo);


        Service serviceObject = ebmsObjectFactory.createService();
        serviceObject.setValue(serviceValue);
        serviceObject.setType(serviceType);

        CollaborationInfo collaborationInfo = ebmsObjectFactory.createCollaborationInfo();
        collaborationInfo.setAction(action);
        collaborationInfo.setService(serviceObject);

        userMessageToBuild.setCollaborationInfo(collaborationInfo);


        PartyId partyId = ebmsObjectFactory.createPartyId();
        partyId.setValue(UNKNOWN_DYNAMIC_RESPONDER_PARTYID_VALUE);
        partyId.setType((UNKNOWN_DYNAMIC_RESPONDER_PARTYID_TYPE));

        To to = ebmsObjectFactory.createTo();
        to.getPartyId().add(partyId);

        PartyInfo partyInfo = ebmsObjectFactory.createPartyInfo();
        partyInfo.setTo(to);

        userMessageToBuild.setPartyInfo(partyInfo);

        return userMessageToBuild;
    }

    private Endpoint buildAS4EndpointWithArguments(String processIdentifierId, String processIdentifierScheme, String address, String alias) {
        ProcessIdentifier processIdentifier = new ProcessIdentifier(processIdentifierId, processIdentifierScheme);
        TransportProfile transportProfile = TransportProfile.AS4;
        X509Certificate x509Certificate = loadCertificateFromJKS(RESOURCE_PATH + TEST_KEYSTORE, alias);

        Endpoint endpoint = new Endpoint(processIdentifier, transportProfile, address, x509Certificate);

        return endpoint;
    }


    /**
     * Calls private method {@code Configuration#preparePersist} in order to initialize the configuration object properly
     *
     * @param configuration
     * @return
     */
    private boolean initializeConfiguration(Configuration configuration) {
        try {
            Method preparePersist = configuration.getClass().getDeclaredMethod("preparePersist");
            preparePersist.setAccessible(true);
            preparePersist.invoke(configuration);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    /**
     * Load certificate with alias from JKS and return as {@code X509Certificate}.
     * The password is always 1234 in this test.
     *
     * @param filePath
     * @param alias
     * @return
     */
    private X509Certificate loadCertificateFromJKS(String filePath, String alias) {
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fileInputStream, "1234".toCharArray());

            Certificate cert = keyStore.getCertificate(alias);

            return (X509Certificate) cert;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}