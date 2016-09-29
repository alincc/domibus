package eu.domibus.common.dao;

import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.model.logging.SignalMessageLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Federico Martini
 * @since 3.2
 */
@Repository
@Transactional
public class SignalMessageLogDao extends MessageLogDao<SignalMessageLog> {

    public SignalMessageLogDao() {
        super(SignalMessageLog.class);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void setMessageStatus(String signalMessageId, MessageStatus messageStatus) {
        final Query messageStatusQuery = em.createNamedQuery("SignalMessageLog.setMessageStatus");
        messageStatusQuery.setParameter("MESSAGE_ID", signalMessageId);
        messageStatusQuery.setParameter("TIMESTAMP", new Date());
        messageStatusQuery.setParameter("MESSAGE_STATUS", messageStatus);
        messageStatusQuery.executeUpdate();
        int result = messageStatusQuery.executeUpdate();
        if (result != 1) {
            em.getTransaction().setRollbackOnly();
            logger.error("Could not update the status of the message with id [" + signalMessageId + "] to: " + messageStatus);
        }
    }

    public MessageStatus getMessageStatus(String messageId) {

        TypedQuery<MessageStatus> query = em.createNamedQuery("SignalMessageLog.getMessageStatus", MessageStatus.class);
        query.setParameter("MESSAGE_ID", messageId);
        try {
            return query.getSingleResult();
        } catch (NoResultException nrEx) {
            logger.debug("Query SignalMessageLog.getMessageStatus did not find any result for message with id [" + messageId + "]", nrEx);
            return MessageStatus.NOT_FOUND;
        }
    }

    public SignalMessageLog findByMessageId(String messageId, MSHRole mshRole) {
        TypedQuery<SignalMessageLog> query = em.createNamedQuery("SignalMessageLog.findByMessageId", SignalMessageLog.class);
        query.setParameter("MESSAGE_ID", messageId);
        query.setParameter("MSH_ROLE", mshRole);

        try {
            return query.getSingleResult();
        } catch (NoResultException nrEx) {
            logger.debug("Query SignalMessageLog.findByMessageId did not find any result for message with id [" + messageId + "] and MSH role [" + mshRole + "]", nrEx);
            return null;
        }
    }

    public Long countMessages(HashMap<String, Object> filters) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<SignalMessageLog> mle = cq.from(SignalMessageLog.class);
        cq.select(cb.count(mle));
        List<Predicate> predicates = getPredicates(filters, cb, mle);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Long> query = em.createQuery(cq);
        return query.getSingleResult();
    }

    public List<SignalMessageLog> findPaged(int from, int max, String column, boolean asc, HashMap<String, Object> filters) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<SignalMessageLog> cq = cb.createQuery(SignalMessageLog.class);
        Root<SignalMessageLog> mle = cq.from(SignalMessageLog.class);
        cq.select(mle);
        List<Predicate> predicates = getPredicates(filters, cb, mle);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        if (column != null) {
            if (asc) {
                cq.orderBy(cb.asc(mle.get(column)));
            } else {
                cq.orderBy(cb.desc(mle.get(column)));
            }

        }
        TypedQuery<SignalMessageLog> query = this.em.createQuery(cq);
        query.setFirstResult(from);
        query.setMaxResults(max);
        return query.getResultList();
    }

}