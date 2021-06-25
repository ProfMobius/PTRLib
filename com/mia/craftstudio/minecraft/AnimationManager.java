package com.mia.craftstudio.minecraft;

import java.util.ArrayList;

import com.google.common.collect.HashBiMap;

public enum AnimationManager {
	INSTANCE;

	public static class TrackedObjectAnimationStates {
		public ArrayList<AnimationState> activeAnimations = new ArrayList<AnimationState>();

		public void update(final float timeDelta) {
			for (int i = activeAnimations.size() - 1; i >= 0; i--) {
				final AnimationState anim = activeAnimations.get(i);
				System.out.println(System.currentTimeMillis());
				System.out.println(anim.frameTime);
				final short duration = anim.animation.getAnimationDuration();
				if (anim.frameTime >= duration) {
					if (anim.repeatCount > 0 && --anim.repeatCount == 0) {
						activeAnimations.remove(i); // set flag to del?
						continue;
					}
					anim.frameTime -= duration;
					anim.lastFullFrameRendered -= duration;
				}

				anim.frameTime += timeDelta;
			}
		}
	}

	private ArrayList<TrackedObjectAnimationStates> animationStates = new ArrayList<TrackedObjectAnimationStates>();
	private HashBiMap<Object, TrackedObjectAnimationStates> animationsMap = HashBiMap.create();

	public void update(final float timeDelta) {
		for (int i = animationStates.size() - 1; i >= 0; i--) {
			final TrackedObjectAnimationStates animState = animationStates.get(i);
			animState.update(timeDelta);
			if (animState.activeAnimations.isEmpty()) {
				// Object is no longer animating, remove it from our trackers
				animationStates.remove(i);
				animationsMap.inverse().remove(animState);
			}
		}
	}

	private final ArrayList<AnimationState> EMPTY_LIST = new ArrayList<AnimationState>();
	public static ArrayList<AnimationState> getActiveAnimations(final Object o) {
		final TrackedObjectAnimationStates animObj = INSTANCE.animationsMap.get(o);
		return animObj != null ? animObj.activeAnimations : INSTANCE.EMPTY_LIST;
	}

	public static boolean addAnimationState(final Object o, final AnimationState state) {
		TrackedObjectAnimationStates animObj = INSTANCE.animationsMap.get(o);
		if (animObj == null) {
			animObj = new TrackedObjectAnimationStates();
			INSTANCE.animationsMap.put(o, animObj);
			INSTANCE.animationStates.add(animObj);
			animObj.activeAnimations.add(state);
		} else
			// TODO add in additional animations on the same object
			return false;
		return true;
	}
}
