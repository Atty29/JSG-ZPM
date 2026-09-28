package uk.co.atty29.jsgzpm.alarm;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum AtlantisAlarmState implements StringRepresentable {
    IDLE("idle"),
    OFFWORLD("offworld"),
    GENERAL("general");

    private final String serializedName;

    AtlantisAlarmState(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
