package uk.co.atty29.jsgzpm.holder;

public enum ZPMSlotState {
    EMPTY,
    UP,
    MOVING_DOWN,
    DOWN_STANDBY,
    DOWN_SUPPLYING,
    MOVING_UP;

    public boolean isDown() {
        return this == DOWN_STANDBY || this == DOWN_SUPPLYING;
    }

    public boolean isMoving() {
        return this == MOVING_DOWN || this == MOVING_UP;
    }
}
