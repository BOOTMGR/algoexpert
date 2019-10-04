package hp.bootmgr.algoexpert.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import hp.bootmgr.algoexpert.transformers.RecursionTracingTransformer;

public class InstrumentationAgent {
	public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
		System.out.println("Instrumentation Agent Started");
		inst.addTransformer(new RecursionTracingTransformer());
	}
}
