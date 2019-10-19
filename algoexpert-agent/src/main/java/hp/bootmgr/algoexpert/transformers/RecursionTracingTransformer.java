package hp.bootmgr.algoexpert.transformers;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hp.bootmgr.algoexpert.annotations.Recursive;
import hp.bootmgr.algoexpert.annotations.TraceRecursion;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class RecursionTracingTransformer implements ClassFileTransformer {
	
	private static final Logger LOG = LoggerFactory.getLogger(RecursionTracingTransformer.class);
	
	private final ClassPool CLASS_POOL = ClassPool.getDefault();
	
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		CtClass cc = null;
		byte[] byteCode = null;
		try {
			cc = CLASS_POOL.get(className.replaceAll("/", "."));
		} catch (NotFoundException e) {
			LOG.trace("Javassit: NotFound: " + e.getMessage());
			return null;
		}
		
		if(cc.hasAnnotation(TraceRecursion.class)) {
			LOG.trace("Transforming: " + className);
			for(CtMethod m : cc.getMethods()) {
				if(m.hasAnnotation(Recursive.class)) {
					LOG.trace("\tTarget Method: " + m);
					try {
						CtMethod tracerMethod = addProxyMethod(m, cc);
						
						if(tracerMethod == null) continue;
						
						LOG.trace("\tReplacing original method call...");
						m.setBody("hp.bootmgr.algoexpert.CallTrace c = new hp.bootmgr.algoexpert.CallTrace();"
								+ "c.setParams($args);"
								+ "c.setFuncName(\"" + m.getName() + "\");"
								+ "c.setRetVal(" + tracerMethod.getName() + "(c,$$));"
								+ "hp.bootmgr.algoexpert.result.TransformationFactory.transform(c);"
								+ "return ($r) c.getRetVal(); }");
						
					} catch (CannotCompileException e) {
						LOG.error("Can not modify", e);
					} catch (NotFoundException e) {
						LOG.error("Class not found: " + e.getMessage(), e);
					}
				}
			}
			
			try {
				byteCode = cc.toBytecode();
			} catch (Exception e) {
				LOG.error("Can not convert to byte code", e);
			} finally {
				cc.detach();	
			}
		}
		
		return byteCode;
	}
	
	private CtMethod addProxyMethod(CtMethod m, CtClass clazz) throws CannotCompileException, NotFoundException {
		LOG.trace("\tGenerating Tracer Method...");
		final String methodName = m.getName();
		final String methodClass = m.getClass().getName();
		final String methodSignature = m.getSignature();
		CtClass ccTraceRecorder = CLASS_POOL.get("hp.bootmgr.algoexpert.CallTrace");
		
		final String proxyMethodName = proxyMethodName(methodName);
		CtMethod copy = CtNewMethod.copy(m, proxyMethodName, clazz, null);
		copy.insertParameter(ccTraceRecorder);
		copy.addLocalVariable("__call", ccTraceRecorder);
		copy.insertBefore("__call = $1;");
		LOG.trace("\t\tInjecting Method: {} {}", copy.getName(), copy.getSignature());
		clazz.addMethod(copy);
		
		copy.instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				if(methodName.equals(call.getMethodName())
						&& methodSignature.equals(call.getSignature())
						&& methodClass.equals(methodClass)) {
					LOG.trace("\t\tPatching recursive calls to call generated method...");
					call.replace("hp.bootmgr.algoexpert.CallTrace c = new hp.bootmgr.algoexpert.CallTrace();"
							+ "__call.calls.add(c);"
							+ "c.setParams($args);"
							+ "c.setRetVal(" + proxyMethodName + "(c,$$));"
							+ "$_ = ($r) c.getRetVal();"); 
				}
			}
		});
		return copy;
	}
	
	private String proxyMethodName(String orig) {
		return "$" + orig + "$";
	}
}
