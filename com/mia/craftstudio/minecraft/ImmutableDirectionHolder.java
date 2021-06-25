package com.mia.craftstudio.minecraft;

import net.minecraft.util.EnumFacing;

public class ImmutableDirectionHolder {
    private final int offset[];
    private final EnumFacing[] directions;

    public ImmutableDirectionHolder(final EnumFacing... facings) {
        directions = facings;
        offset = getOffsets();
    }

    private int[] getOffsets() {
        final int[] offsets = new int[3];
        for (final EnumFacing fd : directions) {
            if (fd == null) {
                continue;
            }

            offsets[0] += fd.getFrontOffsetX();
            offsets[1] += fd.getFrontOffsetY();
            offsets[2] += fd.getFrontOffsetZ();
        }
        return offsets;
    }

    public int[] getOffset() {
        return offset;
    }

    public EnumFacing[] getDirections() {
        return directions;
    }

    @Override
    public String toString() {
        String s = "ImmutableDirectionHolder : ";
        for (final EnumFacing direction : directions) {
            s += direction;
            s += " ";
        }
        s += " | ";
        for (final float v : offset) {
            s += v;
            s += " ";
        }

        return s;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ImmutableDirectionHolder)) {
            return false;
        }

        final ImmutableDirectionHolder o = (ImmutableDirectionHolder) obj;

        if (directions.length != o.directions.length) {
            return false;
        }

        if (offset[0] != o.offset[0] || offset[1] != o.offset[1] || offset[2] != o.offset[2]) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 1024 * 1024 * offset[0] + 1024 * offset[1] + offset[2];
    }
}
