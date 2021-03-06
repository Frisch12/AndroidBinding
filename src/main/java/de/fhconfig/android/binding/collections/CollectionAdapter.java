package de.fhconfig.android.binding.collections;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.Collection;

import de.fhconfig.android.binding.AttributeBinder;
import de.fhconfig.android.binding.Binder;
import de.fhconfig.android.binding.BindingLog;
import de.fhconfig.android.binding.CollectionChangedEventArg;
import de.fhconfig.android.binding.CollectionObserver;
import de.fhconfig.android.binding.Command;
import de.fhconfig.android.binding.IObservable;
import de.fhconfig.android.binding.IObservableCollection;
import de.fhconfig.android.binding.ISyntaxResolver.SyntaxResolveException;
import de.fhconfig.android.binding.Observer;
import de.fhconfig.android.binding.observables.BooleanObservable;
import de.fhconfig.android.binding.utility.CachedModelReflector;
import de.fhconfig.android.binding.utility.EventMarkerHelper;
import de.fhconfig.android.binding.utility.IModelReflector;
import de.fhconfig.android.binding.viewAttributes.adapterView.listView.ItemViewEventMark;
import de.fhconfig.android.binding.viewAttributes.templates.Layout;

public class CollectionAdapter extends BaseAdapter implements CollectionObserver, Filterable, LazyLoadAdapter {
	protected final Handler mHandler;
	protected final Context mContext;
	protected final Layout mLayout, mDropDownLayout;
	protected final IObservableCollection<?> mCollection;
	protected final IModelReflector mReflector;
	protected final Filter mFilter;
	protected final Command mLastShown;
	protected final BooleanObservable mHasMore;
	protected Mode mMode = Mode.LoadWhenStopped;
	protected LazyLoadRootAdapterHelper mHelper;
	private Boolean hasMoreValue;
	private int lastDisplayingFirst = -1;
	private int lastTotal = 0;
	/**
	 * Statement that determines child item is enable/disable
	 */
	private String mEnableItemStatement = null;

	public CollectionAdapter(Context context, IModelReflector reflector, IObservableCollection<?> collection, Layout layout, Layout dropDownLayout,
	                         Filter filter, String enableItemStatement, Command lastShown, BooleanObservable hasMore) throws Exception {
		mHandler = new Handler();
		mContext = context;
		mLayout = layout;
		mDropDownLayout = dropDownLayout;
		mCollection = collection;
		mReflector = reflector;
		mFilter = filter;
		mEnableItemStatement = enableItemStatement;
		mCollection.subscribe(this);
		mLastShown = lastShown;
		mHasMore = hasMore;

		if (mHasMore != null) {
			mHasMore.subscribe(new Observer() {
				@Override
				public void onPropertyChanged(IObservable<?> prop, Collection<Object> initiators) {
					try {
						hasMoreValue = (Boolean) prop.get();
					} catch (Exception ex) {
					}
				}
			});
		}
	}

	public CollectionAdapter(Context context, IModelReflector reflector, IObservableCollection<?> collection, Layout layout, Layout dropDownLayout,
	                         String enableItemStatement) throws Exception {
		this(context, reflector, collection, layout, dropDownLayout, null, enableItemStatement, null, null);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public CollectionAdapter(Context context, IObservableCollection<?> collection, Layout layout, Layout dropDownLayout, Filter filter,
	                         String enableItemStatement, Command lastShown, BooleanObservable hasMore) throws Exception {
		this(context, new CachedModelReflector(collection.getComponentType()), collection, layout, dropDownLayout, filter, enableItemStatement, lastShown, hasMore);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public CollectionAdapter(Context context, IObservableCollection<?> collection, Layout layout, Layout dropDownLayout, String enableItemStatement)
			throws Exception {
		this(context, new CachedModelReflector(collection.getComponentType()), collection, layout, dropDownLayout, enableItemStatement);
	}

	@Override
	public int getViewTypeCount() {
		return mLayout.getTemplateCount();
	}

	@Override
	public int getItemViewType(int position) {
		return mLayout.getLayoutTypeId(position);
	}

	public void subscribeCollectionObserver(CollectionObserver observer) {
		mCollection.subscribe(observer);
	}

	public void unsubscribeCollectionObserver(CollectionObserver observer) {
		mCollection.unsubscribe(observer);
	}

	public int getCount() {
		return mCollection.size();
	}

	public Object getItem(int position) {
		return mCollection.getItem(position);
	}

	public long getItemId(int position) {
		return mCollection.getItemId(position);
	}

	private View getView(int position, View convertView, ViewGroup parent, Layout layout) {
		View returnView = convertView;
		if (position >= mCollection.size()) {
			if (mLastShown != null)
				mLastShown.Invoke(convertView);
			if (hasMoreValue) {
				return new SpinnerView(mContext);
			}
			return returnView;
		}
		try {
			ObservableMapper mapper;

			mCollection.onLoad(position);

			Object item = mCollection.getItem(position);

			if ((convertView == null) || ((mapper = getAttachedMapper(convertView)) == null)) {
				Binder.InflateResult result =
						Binder.inflateView(mContext, layout.getLayoutId(position), parent, false);
				layout.onAfterInflate(result, position);
				ItemViewEventMark mark = new ItemViewEventMark(parent, position, mCollection.getItemId(position));
				EventMarkerHelper.mark(result.rootView, mark);
				mapper = new ObservableMapper();
				Object model = mCollection.getItem(position);
				mapper.startCreateMapping(mReflector, model);
				for (View view : result.processedViews) {
					AttributeBinder.getInstance().bindView(mContext, view, mapper);
				}
				mapper.endCreateMapping();
				returnView = result.rootView;
				this.putAttachedMapper(returnView, mapper);
			} else {
				ItemViewEventMark mark = EventMarkerHelper.getMark(returnView);
				if (null != mark) {
					mark.setIdAndPosition(position, mCollection.getItemId(position));
				}
			}

			mapper.changeMapping(mReflector, item);
			if (mHelper != null && !mHelper.isBusy()) {
				if (item instanceof ILazyLoadRowModel) {
					((ILazyLoadRowModel) item).display(mCollection, position);
				}
			}

			return returnView;
		} catch (Exception e) {
			e.printStackTrace();
			return returnView;
		}
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, mDropDownLayout);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, mLayout);
	}

	private ObservableMapper getAttachedMapper(View convertView) {
		return Binder.getViewTag(convertView).get(ObservableMapper.class);
	}

	private void putAttachedMapper(View convertView, ObservableMapper mapper) {
		Binder.getViewTag(convertView).put(ObservableMapper.class, mapper);
	}

	public Filter getFilter() {
		return mFilter;
	}

	public void setRoot(AbsListView view) {
		if (ILazyLoadRowModel.class.isAssignableFrom(mCollection.getComponentType()))
			mHelper = new LazyLoadRootAdapterHelper(view, this, mMode);
	}

	public void setMode(Mode mode) {
		if (mHelper != null) {
			mHelper.setMode(mode);
		}
		mMode = mode;
	}

	public void onVisibleChildrenChanged(int first, int total) {
		if (total > lastTotal) {
			mCollection.setVisibleChildrenCount(this, total);
		}

		int actualCollectionSize = mCollection.size();
		if (0 == actualCollectionSize) {
			// nothing to show, nothing to hide, reset last*
			lastDisplayingFirst = -1;
			lastTotal = 0;
			return;
		}
		// normalize NEW first and last indexes
		// normalize newTotal, should be greater or equal to 0
		int newTotal = total < 0 ? 0 : total;
		// normalize newFirstIndex, should be less than actualCollectionSize
		int newFirstIndex = (first >= actualCollectionSize) ? actualCollectionSize - 1 : first;
		// normalize newFirstIndex, should be greater or equal to 0
		newFirstIndex = newFirstIndex < 0 ? 0 : newFirstIndex;
		// calculate last new visible index
		int newLastIndex = newFirstIndex + newTotal;
		// normalize newLastIndex, should be equal or greater than less than newFirstIndex
		newLastIndex = (newLastIndex < newFirstIndex) ? newFirstIndex + 1 : newLastIndex;
		// normalize newLastIndex, should be less than actualCollectionSize
		newLastIndex = (newLastIndex > actualCollectionSize) ? actualCollectionSize : newLastIndex;

		// normalize OLD first and last indexes (collection size can be changed between function calls)
		// normalize oldTotal, should be greater or equal to 0
		int oldTotal = lastTotal < 0 ? 0 : lastTotal;
		// normalize oldFirstIndex, should be less or equal to actualCollectionSize
		int oldFirstIndex = (lastDisplayingFirst > actualCollectionSize - 1) ? actualCollectionSize - 1 : lastDisplayingFirst;
		// normalize oldFirstIndex, should be greater or equal to 0
		oldFirstIndex = oldFirstIndex < 0 ? 0 : oldFirstIndex;
		// calculate last old visible index
		int oldLastIndex = oldFirstIndex + oldTotal;
		// normalize oldLastIndex, should be equal or greater than less than oldFirstIndex
		oldLastIndex = (oldLastIndex < oldFirstIndex) ? oldFirstIndex + 1 : oldLastIndex;
		// normalize oldLastIndex, should be less or equal to actualCollectionSize
		oldLastIndex = (oldLastIndex > actualCollectionSize) ? actualCollectionSize : oldLastIndex;

		Object rawItem;

		for (int i = newFirstIndex; i < oldFirstIndex; ++i) {
			rawItem = mCollection.getItem(i);
			if (rawItem instanceof ILazyLoadRowModel) {
				((ILazyLoadRowModel) rawItem).display(mCollection, i);
			}
		}
		for (int i = oldFirstIndex; i < newFirstIndex; ++i) {
			rawItem = mCollection.getItem(i);
			if (rawItem instanceof ILazyLoadRowModel) {
				if (!((ILazyLoadRowModel) rawItem).isMapped())
					((ILazyLoadRowModel) rawItem).hide(mCollection, i);
			}
		}

		for (int i = newLastIndex; i < oldLastIndex; ++i) {
			rawItem = mCollection.getItem(i);
			if (rawItem instanceof ILazyLoadRowModel) {
				if (!((ILazyLoadRowModel) rawItem).isMapped())
					((ILazyLoadRowModel) rawItem).hide(mCollection, i);
			}
		}
		for (int i = oldLastIndex; i < newLastIndex; ++i) {
			rawItem = mCollection.getItem(i);
			if (rawItem instanceof ILazyLoadRowModel) {
				((ILazyLoadRowModel) rawItem).display(mCollection, i);
			}
		}

		// set lastDisplayingFirst and lastTotal;
		lastDisplayingFirst = newFirstIndex;
		lastTotal = newTotal;
	}

	/**
	 * If the statement is null (unset), all items are assumed to be enabled.
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return mEnableItemStatement == null;
	}

	/**
	 * Make individual item enable/disable possible.
	 * This is not possible to do in Item level, but only from ListView's level since
	 * items are rendered by listView and listview seems to omit this value
	 */

	@Override
	public boolean isEnabled(int position) {
		if (mEnableItemStatement == null)
			return true;
		IObservable<?> obs;
		try {
			obs = Binder.getSyntaxResolver().constructObservableFromStatement(mContext, mEnableItemStatement, mCollection.getItem(position));
		} catch (SyntaxResolveException e) {
			BindingLog.exception("CollectionAdapter.isEnabled", e);
			return false;
		}
		// Even if the obs is null, or it's value is null, it is enabled by default
		return obs == null || !Boolean.FALSE.equals(obs.get());
	}

	@Override
	public void onCollectionChanged(IObservableCollection<?> collection,
	                                CollectionChangedEventArg args, Collection<Object> initiators) {
		notifyDataSetChanged();
	}

	private class SpinnerView extends LinearLayout {
		public SpinnerView(Context context) {
			super(context);

			this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			this.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

			ProgressBar progressBar = new ProgressBar(mContext);
			progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			progressBar.setIndeterminate(true);

			this.addView(progressBar);
		}
	}
}