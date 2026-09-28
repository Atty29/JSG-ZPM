package uk.co.atty29.jsgzpm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class GeneratorGeometry {
    private GeneratorGeometry() {
    }

    public static Direction uAxis(Direction normal) {
        return normal.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }

    public static Direction vAxis(Direction normal) {
        return normal.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
    }

    public static BlockPos planeOffset(BlockPos origin, Direction normal, int u, int v) {
        return origin.relative(uAxis(normal), u).relative(vAxis(normal), v);
    }

    public static Vec3 localOffset(Direction normal, double u, double v, double outward) {
        Direction uAxis = uAxis(normal);
        Direction vAxis = vAxis(normal);
        return new Vec3(
                uAxis.getStepX() * u + vAxis.getStepX() * v + normal.getStepX() * outward,
                uAxis.getStepY() * u + vAxis.getStepY() * v + normal.getStepY() * outward,
                uAxis.getStepZ() * u + vAxis.getStepZ() * v + normal.getStepZ() * outward
        );
    }
}
