package hp.bootmgr.algoexpert.result;

import hp.bootmgr.algoexpert.CallTrace;
import hp.bootmgr.algoexpert.result.transformers.HTMLResultTransformer;

public class TransformationFactory {
	
	private TransformationFactory() {}
	
	private static Transformer transformer = null;
	
	public static void transform(CallTrace trace) {
		if(transformer == null) {
			setTransformer(getDefaultTransformer());
		}
		transformer.transform(trace);
	}
	
	private static Transformer getDefaultTransformer() {
		return new HTMLResultTransformer();
	}
	
	public static void setTransformer(Transformer t) {
		transformer = t;
	}
}
