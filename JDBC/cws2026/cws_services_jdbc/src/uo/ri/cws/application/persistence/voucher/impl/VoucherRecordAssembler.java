package uo.ri.cws.application.persistence.voucher.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;

public class VoucherRecordAssembler extends BaseRecordAssembler<VoucherRecord> {

    @Override
    protected VoucherRecord createRecord() {
        return new VoucherRecord();
    }

    @Override
    public VoucherRecord toRecord(ResultSet rs) throws SQLException {
        VoucherRecord record = super.toRecord(rs);
        record.clientId = rs.getString("client_id");
        record.accumulated = rs.getBigDecimal("accumulated");
        record.dtype = rs.getString("dtype");
        record.code = rs.getString("code");
        record.description = rs.getString("description");
        record.available = rs.getBigDecimal("available");
        return record;
    }
}
