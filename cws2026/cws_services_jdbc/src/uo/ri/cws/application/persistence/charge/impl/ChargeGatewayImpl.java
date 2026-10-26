package uo.ri.cws.application.persistence.charge.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class ChargeGatewayImpl implements ChargeGateway {

    private final ChargeRecordAssembler assembler = new ChargeRecordAssembler();

    @Override
    public void add(ChargeRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(ChargeRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<ChargeRecord> findById(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<ChargeRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<ChargeRecord> findByPaymentMeanId(String paymentMeanId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCHARGES_FIND_BY_PAYMENT_MEAN_ID"))) {
            pst.setString(1, paymentMeanId);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
