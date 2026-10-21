package uo.ri.cws.application.persistence.invoice.impl;

import java.sql.Connection;
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
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.util.exception.PersistenceException;

public class InvoiceGatewayImpl implements InvoiceGateway {

    private final InvoiceRecordAssembler assembler = new InvoiceRecordAssembler();

    @Override
    public void add(InvoiceRecord t) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINVOICES_ADD"))) {
            pst.setString(1, t.id);
            pst.setLong(2, t.version);
            pst.setLong(3, t.number);
            pst.setDate(4, t.date == null ? null : Date.valueOf(t.date));
            pst.setString(5, t.state);
            pst.setBoolean(6, false);
            pst.setBigDecimal(7, t.subtotal);
            pst.setBigDecimal(8, t.total);
            pst.setBigDecimal(9, t.vatamount);
            pst.setBigDecimal(10, t.vatrate);
            pst.setTimestamp(11, t.createdAt == null ? null
                    : Timestamp.valueOf(t.createdAt));
            pst.setTimestamp(12, t.updatedAt == null ? null
                    : Timestamp.valueOf(t.updatedAt));
            pst.setString(13, t.entityState);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINVOICES_REMOVE"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void update(InvoiceRecord t) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection().prepareStatement(
                Queries.getSQLSentence("TINVOICES_UPDATE"))) {
            pst.setLong(1, t.number);
            pst.setDate(2, t.date == null ? null : Date.valueOf(t.date));
            pst.setString(3, t.state);
            pst.setBigDecimal(4, t.subtotal);
            pst.setBigDecimal(5, t.total);
            pst.setBigDecimal(6, t.vatamount);
            pst.setBigDecimal(7, t.vatrate);
            pst.setTimestamp(8, t.updatedAt == null ? null
                    : Timestamp.valueOf(t.updatedAt));
            pst.setString(9, t.id);
            pst.setLong(10, t.version);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(
                        new SQLException("Invoice was not updated because its version has changed"));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Optional<InvoiceRecord> findById(String id) throws PersistenceException {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_BY_ID"));

            pst.setString(1, id);
            rs = pst.executeQuery();
            return Optional.ofNullable(assembler.toRecord(rs));

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {}
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public List<InvoiceRecord> findAll() throws PersistenceException {
        List<InvoiceRecord> result = new ArrayList<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_ALL"));
            rs = pst.executeQuery();

            result = assembler.toInvoiceRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {}
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
        return result;
    }

    @Override
    public Optional<InvoiceRecord> findByNumber(Long number) throws PersistenceException {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_BY_NUMBER"));

            pst.setLong(1, number);
            rs = pst.executeQuery();
            return Optional.ofNullable(assembler.toRecord(rs));

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {}
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Optional<Long> findLastNumber() throws PersistenceException {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_FIND_LAST_NUMBER"));
            rs = pst.executeQuery();

            if (rs.next()) {
                long lastNumber = rs.getLong(1);
                return rs.wasNull() ? Optional.empty() : Optional.of(lastNumber);
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {}
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

}