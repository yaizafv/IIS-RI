package uo.ri.cws.domain.types;

import java.util.UUID;

import uo.ri.util.assertion.ArgumentChecks;

public record VoucherCode(String code) {

	public VoucherCode {
		code = normalize(code);
		ArgumentChecks.isTrue(isValid(code), "Voucher code cannot be null or blank");
	}

	private static String normalize(String code) {
		return code == null ? null : code.trim();
	}

	public static boolean isValid(String code) {
		return code != null && !code.isBlank();
	}

	public static VoucherCode of(String value) {
		return new VoucherCode(value);
	}

	/**
	 * @return a new VoucherCode instance with a unique code.
	 */
	public static VoucherCode generate() {
		return new VoucherCode(UUID.randomUUID().toString());
	}

	@Override
	public String toString() {
		return code;
	}
}
