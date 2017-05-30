package eu.domibus.common.services.impl;

import com.google.common.collect.Lists;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.dao.ProcessDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.*;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.ebms3.common.context.MessageExchangeContext;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.ebms3.sender.MSHDispatcher;
import eu.domibus.util.PojoInstaciatorUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.soap.SOAPMessage;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by dussath on 5/19/17.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageExchangeServiceImplTest {

    
    

    @Mock
    private ProcessDao processDao;
    @Mock
    private ConfigurationDAO configurationDao;
    @Mock
    private MSHDispatcher mshDispatcher;
    @Mock
    private EbMS3MessageBuilder messageBuilder;
    @InjectMocks
    private MessageExchangeServiceImpl messageExchangeService;
    private static Process process;


    @Before
    public void init() {
        Party correctParty = PojoInstaciatorUtil.instanciate(Party.class, "[name:party1]");
        process = PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1,action[name:action]]}", "responderParties{[name:endPoint1];[name:endPoint2]}");
        LegConfiguration uniqueLeg = process.getLegs().iterator().next();
        Service service = new Service();
        service.setName("service");
        uniqueLeg.setService(service);
        Mpc mpc = new Mpc();
        mpc.setName("mpcName");
        uniqueLeg.setDefaultMpc(mpc);
        Configuration configuration = new Configuration();
        configuration.setParty(correctParty);
        when(configurationDao.read()).thenReturn(configuration);
        List<Process> processes = Lists.newArrayList(process);
        when(processDao.findPullProcessesByIniator(correctParty)).thenReturn(processes);
    }
    
    @Test
    public void testSuccessFullOneWayPullConfiguration() throws Exception {

        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]");
        MessageExchangeContext messageExchangeContext = getMessageExchangeContext(process);
        assertEquals(MessageStatus.READY_TO_PULL,messageExchangeContext.getMessageStatus());

    }

    @Test
    public void testOneWayPullOnlySupported(){
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:twoway]", "mepBinding[name:pull]");
        try {
            getMessageExchangeContext(process);
            assertTrue(false);
        }catch (RuntimeException e){

        }
    }

    private MessageExchangeContext getMessageExchangeContext(Process process) {
        List<Process> processes= Lists.newArrayList();
        processes.add(process);
        MessageExchangeContext messageExchangeContext = new MessageExchangeContext("agreementName", "senderParty", "receiverParty", "service", "action", "leg");
        when(processDao.findProcessForMessageContext(messageExchangeContext)).thenReturn(processes);
        messageExchangeService.upgradeMessageExchangeStatus(messageExchangeContext);
        return messageExchangeContext;
    }

    @Test
    public void testIncorrectMultipleProcessFoundForConfiguration(){
        MessageExchangeContext messageExchangeContext = new MessageExchangeContext("agreementName", "senderParty", "receiverParty", "service", "action", "leg");
        List<Process> processes= Lists.newArrayList();
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]");
        processes.add(process);
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:push]");
        processes.add(process);
        when(processDao.findProcessForMessageContext(messageExchangeContext)).thenReturn(processes);
        try {
            messageExchangeService.upgradeMessageExchangeStatus(messageExchangeContext);
            assertTrue(false);
        }catch (RuntimeException e){

        }
    }

    @Test
    public void testInitiatePullRequest() throws Exception {
        messageExchangeService.initiatePullRequest();
        ArgumentCaptor<SOAPMessage> soapMessageCaptor = ArgumentCaptor.forClass(SOAPMessage.class);
        ArgumentCaptor<String> pmodeCaptor= ArgumentCaptor.forClass(String.class);
        //when(messageBuilder.buildSOAPMessage())
        verify(mshDispatcher,times(2)).dispatch(soapMessageCaptor.capture(), pmodeCaptor.capture());
        assertEquals("Mock:endPoint1:service:action:Mock:leg1",pmodeCaptor.getAllValues().get(0));
        assertEquals("Mock:endPoint2:service:action:Mock:leg1",pmodeCaptor.getAllValues().get(1));
    }

    @Test
    public void testInvalidRequest() throws Exception {
        when(messageBuilder.buildSOAPMessage(any(SignalMessage.class),any(LegConfiguration.class))).thenThrow(
                new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "An error occurred while processing your request. Please check the message header for more details.", null,null));
        messageExchangeService.initiatePullRequest();
        verify(mshDispatcher,times(0)).dispatch(any(SOAPMessage.class), any(String.class));
    }



    @Test
    public void testInstantiateSoapMessage() throws Exception {
        PullContext pullContext = new PullContext();
        pullContext.setProcess(process);
        pullContext.setCurrentMsh(PojoInstaciatorUtil.instanciate(Party.class," [name:from]"));
        pullContext.setToBePulled(PojoInstaciatorUtil.instanciate(Party.class," [name:to]"));
        ArgumentCaptor<SignalMessage> signalMessageCaptor = ArgumentCaptor.forClass(SignalMessage.class);
        ArgumentCaptor<LegConfiguration> legConfigurationCaptor = ArgumentCaptor.forClass(LegConfiguration.class);
        messageExchangeService.instantiateSoapMessage(pullContext);
        verify(messageBuilder,times(1)).buildSOAPMessage(signalMessageCaptor.capture(),legConfigurationCaptor.capture());
        SignalMessage value = signalMessageCaptor.getValue();
        LegConfiguration legConfiguration=legConfigurationCaptor.getValue();
        assertEquals("mpcName",value.getPullRequest().getMpc());
        assertEquals("leg1",legConfiguration.getName());
        assertEquals("from:to:service:action:Mock:leg1",pullContext.getpModeKey());
    }

    @Test
    public void testInstantiateSoapMessageWithError() throws Exception {
        PullContext pullContext = new PullContext();
        pullContext.setProcess(process);
        pullContext.setCurrentMsh(PojoInstaciatorUtil.instanciate(Party.class," [name:from]"));
        pullContext.setToBePulled(PojoInstaciatorUtil.instanciate(Party.class," [name:to]"));
        when(messageBuilder.buildSOAPMessage(any(SignalMessage.class),any(LegConfiguration.class))).thenThrow(
                new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "An error occurred while processing your request. Please check the message header for more details.", null,null));
        messageExchangeService.instantiateSoapMessage(pullContext);
        assertFalse(pullContext.isValid());
    }

    @Test
    public void extractConfigurationInfo() throws Exception {
    }

    @Test
    public void checkProcessValidityWithMoreThanOneLeg() throws Exception {
        PullContext pullContext = new PullContext();
        pullContext.setProcess(PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1];[name:leg2]}"));
        messageExchangeService.checkProcessValidity(pullContext);
        assertEquals(false, pullContext.isValid());
    }

    @Test
    public void checkProcessValidityWithZeroLeg() throws Exception {
        PullContext pullContext = new PullContext();
        pullContext.setProcess(PojoInstaciatorUtil.instanciate(Process.class));
        messageExchangeService.checkProcessValidity(pullContext);
        assertEquals(false, pullContext.isValid());
    }

    @Test
    public void checkProcessValidityWithOneLeg() throws Exception {
        PullContext pullContext = new PullContext();
        pullContext.setProcess(PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1]}"));
        messageExchangeService.checkProcessValidity(pullContext);
        assertEquals(true, pullContext.isValid());
    }



}