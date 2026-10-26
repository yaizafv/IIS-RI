package uo.ri.cws.application.persistence.paymentmean.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class PaymentMeanGatewayImpl implements PaymentMeanGateway {

    private final PaymentMeanRecordAssembler assembler = new PaymentMeanRecordAssembler();

    @Override
    public void add(PaymentMeanRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_ADD"))) {
            pst.setString(1, record.id);
            pst.setString(2, record.dtype);
            pst.setTimestamp(3, Timestamp.valueOf(record.createdAt));
            pst.setTimestamp(4, Timestamp.valueOf(record.updatedAt));
            pst.setLong(5, record.version);
            pst.setString(6, record.entityState);
            pst.setBigDecimal(7, record.accumulated);
            pst.setString(8, record.clientId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_DELETE"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void update(PaymentMeanRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<PaymentMeanRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<PaymentMeanRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<PaymentMeanRecord> findByClientId(String clientId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TPAYMENTMEANS_FIND_BY_CLIENT_ID"))) {
            pst.setString(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
