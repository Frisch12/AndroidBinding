package de.fhconfig.android.binding.viewAttributes.ratingBar;


import de.fhconfig.android.binding.Binder;
import de.fhconfig.android.binding.ViewAttribute;
import de.fhconfig.android.binding.listeners.OnRatingBarChangeListenerMulticast;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

/**
 * Rating of Rating Bar
 * Equivalent to android:rating
 * 
 * @name rating
 * @widget RatingBar
 * @type Float
 * 
 * @accepts	Float
 * 
 * @category simple
 * @related http://developer.android.com/reference/android/widget/RatingBar.OnRatingBarChangeListener.html
 * @related http://developer.android.com/reference/android/widget/RatingBar.html
 * 
 * @author andy
 */
public class RatingViewAttribute extends ViewAttribute<RatingBar, Float>
	implements OnRatingBarChangeListener{

	public RatingViewAttribute(RatingBar view) {
		super(Float.class, view, "rating");
		Binder.getMulticastListenerForView(view, OnRatingBarChangeListenerMulticast.class)
			.register(this);
	}

	@Override
	protected void doSetAttributeValue(Object newValue) {
		if(getView()==null) return;
		if (newValue == null){
			getView().setRating(0);
			return;
		}
		if (newValue instanceof Float){
			getView().setRating((Float)newValue);
			return;
		}
	}

	@Override
	public Float get() {
		if(getView()==null) return null;
		return getView().getRating();
	}

	public void onRatingChanged(RatingBar ratingBar, float rating,
			boolean fromUser) {
		if (fromUser) this.notifyChanged();
	}
}