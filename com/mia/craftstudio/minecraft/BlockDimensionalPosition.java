package com.mia.craftstudio.minecraft;

import io.netty.buffer.ByteBuf;

public class BlockDimensionalPosition {
	private final int dim, x, y, z;
	private final int hashcode;

	public BlockDimensionalPosition(final int dim, final int x, final int y, final int z) {
		this.dim = dim;
		this.x = x;
		this.y = y;
		this.z = z;

		this.hashcode = (((this.dim * 41) + this.x) * 167 + this.y) * 331 + this.z;
	}

	public BlockDimensionalPosition(final ByteBuf buf) {
		this.dim = buf.readInt();
		this.x = buf.readInt();
		this.y = buf.readInt();
		this.z = buf.readInt();

		this.hashcode = (((this.dim * 41) + this.x) * 167 + this.y) * 331 + this.z;
	}

	public int getDim() {
		return dim;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public void write(final ByteBuf buf) {
		buf.writeInt(this.dim);
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
	}

	@Override
	public String toString() {
		return String.format("[D:%d X:%8d Y:%3d Z:%8d]", this.dim, this.x, this.y, this.z);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof BlockDimensionalPosition) {
			final BlockDimensionalPosition other = (BlockDimensionalPosition)obj;
			return this.dim == other.dim && this.x == other.x && this.y == other.y && this.z == other.z;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}
}
