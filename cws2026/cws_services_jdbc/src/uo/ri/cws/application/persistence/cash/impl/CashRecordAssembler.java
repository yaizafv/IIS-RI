package uo.ri.cws.application.persistence.cash.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.cash.CashGateway.CashRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class CashRecordAssembler extends BaseRecordAssembler<CashRecord> {

    public CashRecordAssembler() {
    }

    @Override
    protected CashRecord createRecord() {
        return new CashRecord();
    }
    
    @Override
    public CashRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }
    
    private CashRecord toCurrentRecord(ResultSet rs) throws SQLException {
        CashRecord record = super.toRecord(rs);
        record.dtype = rs.getString("dtype");
        record.accumulated = rs.getBigDecimal("accumulated");
        record.clientId = rs.getString("client_id");
        return record;
    }

    

}
