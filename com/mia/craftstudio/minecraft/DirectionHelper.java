package com.mia.craftstudio.minecraft;

import net.minecraft.util.EnumFacing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirectionHelper {

	public static final EnumFacing[] allDirections = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN};
	public static final EnumFacing[] flatDirections = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST};
	public static final Set<ImmutableDirectionHolder> directions;
	private static final Map<String, ImmutableDirectionHolder> s2triptype = new HashMap<String, ImmutableDirectionHolder>();	// Cache to turn a string into a proper set of EnumFacing

	static {
		for (final EnumFacing fd1 : allDirections) {
			final String fd1Letter = fd1.toString().substring(0, 1);
			s2triptype.put(fd1Letter, new ImmutableDirectionHolder(fd1));
			for (final EnumFacing fd2 : allDirections) {
				if (fd1.getOpposite() == fd2) {
					continue;
				}
				final String fd2Letter = fd2.toString().substring(0, 1);
				s2triptype.put(fd1Letter + fd2Letter, new ImmutableDirectionHolder(fd1,fd2));
				for (final EnumFacing fd3 : allDirections) {
					if (fd1.getOpposite() == fd3) {
						continue;
					}
					if (fd2.getOpposite() == fd3) {
						continue;
					}
					final String fd3Letter = fd3.toString().substring(0, 1);
					s2triptype.put(fd1Letter + fd2Letter + fd3Letter, new ImmutableDirectionHolder(fd1,fd2,fd3));
				}
			}
		}

		directions = new HashSet(s2triptype.values());

//		System.out.println(s2triptype.size());
//		for (String s : s2triptype.keySet()) {
//			System.out.println("+ " + s + " | " + s2triptype.get(s));
//		}
//
//		Set<ImmutableDirectionHolder> test = new HashSet(s2triptype.values());
//		System.out.println(test.size());
//		for (ImmutableDirectionHolder directionTriptyque : test) {
//			System.out.println("- " + directionTriptyque);
//		}
	}

	public static ImmutableDirectionHolder getDirection(final String s) {
		final ImmutableDirectionHolder retval = s2triptype.get(s.toLowerCase());
		if (retval == null) {
			throw new RuntimeException(String.format("Invalid direction request : %s", s));
		}
		return retval;
	}

	public static Set<ImmutableDirectionHolder> getAllDirections() {
		return directions;
	}
}
