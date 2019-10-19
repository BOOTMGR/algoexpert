package hp.bootmgr.algoexpert.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hp.bootmgr.algoexpert.transformers.RecursionTracingTransformer;

public class InstrumentationAgent {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstrumentationAgent.class);
	
	public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
		LOG.info("Instrumentation Agent Started");
		inst.addTransformer(new RecursionTracingTransformer());
	}
}
