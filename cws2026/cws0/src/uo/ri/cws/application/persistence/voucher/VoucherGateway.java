package uo.ri.cws.application.persistence.voucher;

import java.math.BigDecimal;

import uo.ri.cws.application.persistence.Gateway;

public interface VoucherGateway extends Gateway<VoucherGateway.VoucherRecord> {
    
    class VoucherRecord extends Gateway.BaseRecord {
        public String code;
        public String description;
        public BigDecimal availableMoney;
    }

}
