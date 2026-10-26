package uo.ri.cws.application.persistence.invoice.impl;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.invoice.InvoiceGateway;
import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;
import uo.ri.util.jdbc.Queries;

public class InvoiceGatewayImpl implements InvoiceGateway {

    private final InvoiceRecordAssembler assembler = new InvoiceRecordAssembler();

    @Override
    public void add(InvoiceRecord record) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TINVOICES_ADD"))) {
            pst.setString(1, record.id);
            pst.setLong(2, record.version);
            pst.setLong(3, record.number);
            pst.setDate(4, Date.valueOf(record.date));
            pst.setString(5, record.state);
            pst.setBoolean(6, record.usedForVoucher);
            pst.setBigDecimal(7, record.subtotal);
            pst.setBigDecimal(8, record.total);
            pst.setBigDecimal(9, record.vatAmount);
            pst.setBigDecimal(10, record.vatRate);
            pst.setTimestamp(11, Timestamp.valueOf(record.createdAt));
            pst.setTimestamp(12, Timestamp.valueOf(record.updatedAt));
            pst.setString(13, record.entityState);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(InvoiceRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<InvoiceRecord> findById(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? Optional.of(assembler.toRecord(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<InvoiceRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<InvoiceRecord> findByPayerId(String clientId) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_BY_PAYER_ID"))) {
            pst.setString(1, clientId);
            try (ResultSet rs = pst.executeQuery()) {
                List<InvoiceRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(assembler.toRecord(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<Long> findLastNumber() throws PersistenceException {
        try (
            PreparedStatement pst = Jdbc.getCurrentConnection()
                .prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_LAST_NUMBER"));
            ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                long last = rs.getLong(1);
                return rs.wasNull() ? Optional.empty() : Optional.of(last);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
