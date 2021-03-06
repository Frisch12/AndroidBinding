package de.fhconfig.android.binding.converters;

import de.fhconfig.android.binding.Command;
import de.fhconfig.android.binding.Command.CommandListener;
import de.fhconfig.android.binding.Converter;
import de.fhconfig.android.binding.IObservable;
import de.fhconfig.android.binding.viewAttributes.view.AnimationTrigger;

/**
 * Trigger the provided animation after the command is executed.
 * This is NOT compatible with the newer Property-based animation
 *
 * @author andy
 * @usage animationId command condition
 * @arg animationId integer, specify by "@anim/animation"
 * @arg command de.fhconfig.android.binding.Command
 * @arg @optional condition boolean
 * @return de.fhconfig.android.binding.viewAttributes.view.AnimationTrigger
 */
public class AFTERCOMMAND extends Converter<AnimationTrigger> implements CommandListener {
	AnimationTrigger mTrigger = new AnimationTrigger();
	Command mCommand;
	boolean condition;

	public AFTERCOMMAND(IObservable<?>[] dependents) {
		super(AnimationTrigger.class, dependents);
	}

	/**
	 * this Requires three input
	 * 1. Animation Id
	 * 2. Any Command
	 * 3. Boolean Condition (Optional, if ignore, assume True)
	 */
	@Override
	public AnimationTrigger calculateValue(Object... args) throws Exception {
		if (args.length < 2) return null;
		if (!(args[0] instanceof Integer)) {
			return null;
		}
		mTrigger.setAnimationId((Integer) args[0]);
		if ((mCommand == null) || (!mCommand.equals(args[1])))
			if (args[1] instanceof Command) {
				if (mCommand != null) mCommand.removeCommandListener(this);
				mCommand = (Command) (args[1]);
				mCommand.addCommandListener(this);
			}
		if (args.length < 3)
			condition = true;
		else
			condition = Boolean.TRUE.equals(args[2]);
		return mTrigger;
	}

	public void onBeforeInvoke() {
	}

	public void onAfterInvoke() {
		if (condition)
			mTrigger.notifyAnimationFire();
	}
}
