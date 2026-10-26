package uo.ri.cws.application.persistence.workorder.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uo.ri.cws.application.persistence.util.jdbc.Jdbc;
import uo.ri.util.jdbc.Queries;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway;
import uo.ri.util.exception.NotYetImplementedException;
import uo.ri.util.exception.PersistenceException;

public class WorkOrderGatewayImpl implements WorkOrderGateway {

    private final WorkOrderRecordAssembler assembler = new WorkOrderRecordAssembler();

    @Override
    public void add(WorkOrderRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void remove(String id) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public void update(WorkOrderRecord record) throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public Optional<WorkOrderRecord> findById(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_BY_ID"))) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toWorkOrderRecord(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<WorkOrderRecord> findAll() throws PersistenceException {
        throw new NotYetImplementedException();
    }

    @Override
    public List<WorkOrderRecord> findByIds(List<String> workOrderIds) {
        throw new NotYetImplementedException();
    }

    @Override
    public List<WorkOrderRecord> findByVehicleId(String id) {
        throw new NotYetImplementedException();
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
        throw new NotYetImplementedException();
    }

    @Override
    public List<WorkOrderRecord> findNotInvoicedWorkOrdersByClientNif(String nif) {
        List<WorkOrderRecord> workOrders = new ArrayList<WorkOrderRecord>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            Connection c = Jdbc.getCurrentConnection();
            pst = c.prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_NOT_INVOICED_WORK_ORDERS_BY_CLIENT_NIF"));
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
    public List<WorkOrderRecord> findByClientNif(String nif) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TWORKORDERS_FIND_BY_CLIENT_NIF"))) {
            pst.setString(1, nif);
            try (ResultSet rs = pst.executeQuery()) {
                return assembler.toWorkOrderRecordList(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void setInvoiced(String workOrderId, String invoiceId) {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TWORKORDERS_SET_INVOICED"))) {
            pst.setString(1, invoiceId);
            pst.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pst.setString(3, workOrderId);
            if (pst.executeUpdate() != 1) {
                throw new PersistenceException(new SQLException("Work order was not invoiced: " + workOrderId));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<WorkOrderRecord> findEligibleForVoucher() throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TVOUCHERS_WORKORDERS_ELIGIBLE"));
            ResultSet rs = pst.executeQuery()) {
            List<WorkOrderRecord> result = new ArrayList<>();
            while (rs.next()) {
                WorkOrderRecord record = new WorkOrderRecord();
                record.vehicleId = rs.getString("client_id");
                record.id = rs.getString("workorder_id");
                result.add(record);
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void markUsedForVoucher(String id) throws PersistenceException {
        try (PreparedStatement pst = Jdbc.getCurrentConnection()
            .prepareStatement(Queries.getSQLSentence("TWORKORDERS_MARK_USED_FOR_VOUCHER"))) {
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
