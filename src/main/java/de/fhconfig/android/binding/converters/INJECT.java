package de.fhconfig.android.binding.converters;

import java.util.ArrayList;

import de.fhconfig.android.binding.AttributeInjector;
import de.fhconfig.android.binding.AttributeInjector.InjectParams;
import de.fhconfig.android.binding.Binder.InflateResult;
import de.fhconfig.android.binding.Converter;
import de.fhconfig.android.binding.DynamicObject;
import de.fhconfig.android.binding.IObservable;
import de.fhconfig.android.binding.viewAttributes.templates.Layout;

/**
 * INJECT binding statements into layout templates. <br/>
 * Most probably those templates came from external libraries (e.g. android) that you cannot change <br/>
 * This is much more convenient than everytime you need to copy those standard templates. <br/>
 * <br/>
 * <p/>
 * The most common usage is on those templates for spinners, listviews (e.g. @android:layout/simple_list_item1). <br/>
 * Suppose We need to inject the statement to the textview (@android:id/textview1), then: <br/>
 * binding:itemTemplate="INJECT(@android:layout/simple_list_item1, { id=@android:id/textview1, attr='text', statement='Name' })" <br/>
 * Note it is possible to inject more than one attribute, for example you can change the color of the above mentioned text view too: <br/>
 * binding:itemTemplate="INJECT(@android:layout/simple_list_item1, { id=@android:id/textview1, attr='text', statement='Name' }, { id=@android:id/textview1, attr='backgroundColor', statement='ARGB(255,0,0)' })" <br/>
 *
 * @usage layout params ...
 * @arg layout de.fhconfig.android.binding.viewAttributes.templates.Layout Layout of the template-to-inject
 * @arg params DynamicObject
 * @item id integer Android Id value of the widget
 * @item attr Attribute name of that widget
 * @item statement Binding Statement to be injected
 * @return de.fhconfig.android.binding.viewAttributes.templates.Layout
 */
public class INJECT extends Converter<Layout> {
	public INJECT(IObservable<?>[] dependents) {
		super(Layout.class, dependents);
	}

	private static InjectParams[] process(Object[] args) {
		ArrayList<InjectParams> injectParams = new ArrayList<InjectParams>();
		int len = args.length;
		if (args.length < 2) return new InjectParams[0];
		for (int i = 1; i < len; i++) {
			if (!(args[i] instanceof DynamicObject)) continue;
			try {
				DynamicObject p = (DynamicObject) args[i];
				InjectParams param = new InjectParams();
				param.Id = (Integer) p.getObservableByName("id").get();
				param.AttrName = p.getObservableByName("attr").get().toString();
				param.Statement = p.getObservableByName("statement").get().toString();
				injectParams.add(param);
			} catch (Exception e) {
				continue;
			}
		}
		return injectParams.toArray(new InjectParams[0]);
	}

	@Override
	public Layout calculateValue(Object... args) throws Exception {
		if (args.length < 1 || !(args[0] instanceof Layout)) return null;
		if (args.length < 2) return (Layout) args[0];
		return new InjectLayout((Layout) args[0], process(args));
	}

	public static class InjectLayout extends Layout {
		private final Layout mLayout;
		private final InjectParams[] mParams;

		public InjectLayout(Layout layout, InjectParams[] params) {
			super(layout.getDefaultLayoutId());
			mLayout = layout;
			mParams = params;
		}

		@Override
		public void onAfterInflate(InflateResult result, int position) {
			super.onAfterInflate(result, position);
			AttributeInjector.inject(mParams, result);
		}

		@Override
		public int getLayoutTypeId(int pos) {
			return mLayout.getLayoutTypeId(pos);
		}

		@Override
		public int getLayoutId(int pos) {
			return mLayout.getLayoutId(pos);
		}

		@Override
		public int getTemplateCount() {
			return mLayout.getTemplateCount();
		}

	}
}
