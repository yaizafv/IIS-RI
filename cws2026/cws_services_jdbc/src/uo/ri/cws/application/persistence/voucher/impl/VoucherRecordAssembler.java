package uo.ri.cws.application.persistence.voucher.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;
import uo.ri.cws.application.persistence.voucher.VoucherGateway.VoucherRecord;

public class VoucherRecordAssembler extends BaseRecordAssembler<VoucherRecord> {

    public VoucherRecordAssembler() {
    }

    @Override
    protected VoucherRecord createRecord() {
        return new VoucherRecord();
    }
    
    @Override
    public VoucherRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    private VoucherRecord toCurrentRecord(ResultSet rs) throws SQLException {
        var record = super.toRecord(rs);
        record.code = rs.getString("code");
        record.description = rs.getString("description");
        record.available = rs.getBigDecimal("available");

        return record;
    }

    public List<VoucherRecord> toMechanicRecordList(ResultSet rs)
        throws SQLException {
        List<VoucherRecord> res = new ArrayList<>();
        while (rs.next()) {
            res.add(toCurrentRecord(rs));
        }
        return res;
    }

}
