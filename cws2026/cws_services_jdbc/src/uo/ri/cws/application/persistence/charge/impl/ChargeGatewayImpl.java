package uo.ri.cws.application.persistence.charge.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uo.ri.cws.application.persistence.charge.ChargeGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;

public class ChargeGatewayImpl implements ChargeGateway {

    @Override
    public void add(String invoiceId, String paymentMeanId, BigDecimal amount) throws PersistenceException {
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCHARGES_ADD"))) {
            pst.setString(1, UUID.randomUUID().toString());
            pst.setTimestamp(2, Timestamp.valueOf(now));
            pst.setString(3, "ENABLED");
            pst.setTimestamp(4, Timestamp.valueOf(now));
            pst.setLong(5, 1);
            pst.setBigDecimal(6, amount);
            pst.setString(7, invoiceId);
            pst.setString(8, paymentMeanId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void add(ChargeRecord t) {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(ChargeRecord t) {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<ChargeRecord> findById(String id) {
        throw new NotYetImplementedException();
    }

    @Override
    public List<ChargeRecord> findAll() {
        throw new NotYetImplementedException();
    }

    @Override
    public void removeForClient(String clientId) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TCHARGES_DELETE_FOR_CLIENT"))) {
            pst.setString(1, clientId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

}
