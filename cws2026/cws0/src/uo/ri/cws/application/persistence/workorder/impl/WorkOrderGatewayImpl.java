package uo.ri.cws.application.persistence.workorder.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.cws.application.persistence.util.jdbc.Queries;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.util.exception.PersistenceException;

public class WorkOrderGatewayImpl implements WorkOrderGateway {

    private final WorkOrderRecordAssembler assembler = new WorkOrderRecordAssembler();

    @Override
    public void add(WorkOrderRecord record) throws PersistenceException {
    }

    @Override
    public void remove(String id) throws PersistenceException {
    }

    @Override
    public void update(WorkOrderRecord record) throws PersistenceException {

    }

    @Override
    public Optional<WorkOrderRecord> findById(String id) throws PersistenceException {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findAll() throws PersistenceException {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findNotInvoicedByClientNif(String nif) {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findByIds(List<String> workOrderIds) {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findByVehicleId(String id) {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findByMechanicId(String id) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_BY_MECHANIC_ID"));
            pst.setString(1, id);
            rs = pst.executeQuery();
            return assembler.toWorkOrderRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public List<WorkOrderRecord> findByState(String state) {
        return null;
    }

    @Override
    public List<WorkOrderRecord> findNotInvoicedWorkOrdersByClientNif(String nif) {
        List<WorkOrderRecord> workOrders = new ArrayList<WorkOrderRecord>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_NOT_INVOICED_BY_CLIENT_NIF"));
            pst.setString(1, nif);
            rs = pst.executeQuery();
            workOrders = assembler.toWorkOrderRecordList(rs);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {
            }
        }
        return workOrders;
    }

    @Override
    public void setInvoiced(String workOrderId, String invoiceId) {
        PreparedStatement pst = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TWORKORDERS_UPDATE_INVOICED_BY_ID"));
            pst.setString(1, "INVOICED");
            pst.setString(2, invoiceId);
            pst.setString(3, workOrderId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
