package uk.co.atty29.jsgzpm.power;

/**
 * Discharge strategies for an Ancient Power Controller.
 *
 * Sequential preserves the Phase 4 behaviour. Reserve modes always treat the
 * last linked holder (the farthest holder after network sorting) as the reserve.
 */
public enum BankDischargeMode {
    SEQUENTIAL("mode.jsgzpm.bank.sequential"),
    BALANCED("mode.jsgzpm.bank.balanced"),
    HIGHEST_CHARGE_FIRST("mode.jsgzpm.bank.highest_charge_first"),
    RESERVE_BANK("mode.jsgzpm.bank.reserve_bank"),
    EMERGENCY_RESERVE("mode.jsgzpm.bank.emergency_reserve");

    private final String translationKey;

    BankDischargeMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public BankDischargeMode next() {
        BankDischargeMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static BankDischargeMode fromName(String name) {
        if (name == null || name.isBlank()) return SEQUENTIAL;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return SEQUENTIAL;
        }
    }
}
