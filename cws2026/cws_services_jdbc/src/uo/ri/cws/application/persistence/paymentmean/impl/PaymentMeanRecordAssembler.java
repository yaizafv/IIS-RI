package uo.ri.cws.application.persistence.paymentmean.impl;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CardRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.CashRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.PaymentMeanRecord;
import uo.ri.cws.application.persistence.paymentmean.PaymentMeanGateway.VoucherRecord;
import uo.ri.cws.application.persistence.util.assembler.BaseRecordAssembler;

public class PaymentMeanRecordAssembler
        extends BaseRecordAssembler<PaymentMeanRecord> {

    @Override
    protected PaymentMeanRecord createRecord() {
        return new PaymentMeanRecord();
    }

    @Override
    public PaymentMeanRecord toRecord(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return toCurrentRecord(rs);
    }

    @Override
    public List<PaymentMeanRecord> toRecordList(ResultSet rs)
            throws SQLException {
        List<PaymentMeanRecord> result = new ArrayList<>();
        while (rs.next()) {
            result.add(toCurrentRecord(rs));
        }
        return result;
    }

    private PaymentMeanRecord toCurrentRecord(ResultSet rs) throws SQLException {
        String type = rs.getString("dtype");
        PaymentMeanRecord record;
        if ("CreditCard".equals(type)) {
            CardRecord card = new CardRecord();
            card.number = rs.getString("card_number");
            card.type = rs.getString("card_type");
            Date date = rs.getDate("card_valid_thru");
            card.validThru = date == null ? null
                    : java.time.YearMonth.from(date.toLocalDate());
            record = card;
        } else if ("Voucher".equals(type)) {
            VoucherRecord voucher = new VoucherRecord();
            voucher.code = rs.getString("voucher_code");
            voucher.description = rs.getString("voucher_description");
            voucher.available = rs.getBigDecimal("voucher_available");
            record = voucher;
        } else {
            record = new CashRecord();
        }

        PaymentMeanRecord common = super.toRecord(rs);
        record.id = common.id;
        record.version = common.version;
        record.createdAt = common.createdAt;
        record.updatedAt = common.updatedAt;
        record.entityState = common.entityState;
        record.clientId = rs.getString("client_id");
        record.accumulated = rs.getBigDecimal("accumulated");
        record.dtype = type;
        return record;
    }
}
