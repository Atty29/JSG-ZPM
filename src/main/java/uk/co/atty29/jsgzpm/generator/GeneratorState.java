package uk.co.atty29.jsgzpm.generator;

public enum GeneratorState {
    IDLE,
    SEALING,
    CHARGING,
    VENTING;

    public boolean isLocked() {
        return this != IDLE;
    }
}
