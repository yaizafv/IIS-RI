package uo.ri.cws.application.persistence.invoice.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        PreparedStatement pst = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_ADD"));

            pst.setString(1, t.id);
            pst.setBigDecimal(2, t.total); 
            pst.setDate(3, t.date == null ? null : Date.valueOf(t.date));
            pst.setLong(4, t.number);
            pst.setString(5, t.state);
            pst.setBigDecimal(6, t.vatamount);
            pst.setLong(7, t.version);

            pst.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void remove(String id) throws PersistenceException {
        PreparedStatement pst = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_DELETE"));

            pst.setString(1, id);
            pst.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void update(InvoiceRecord t) throws PersistenceException {
        PreparedStatement pst = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TINVOICES_UPDATE"));

            // Corregido: se usa t.total o t.subtotal en lugar de t.amount
            pst.setBigDecimal(1, t.total);
            pst.setDate(2, t.date == null ? null : Date.valueOf(t.date));
            pst.setString(3, t.state);
            pst.setBigDecimal(4, t.vatamount);
            pst.setString(5, t.id);

            pst.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {}
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