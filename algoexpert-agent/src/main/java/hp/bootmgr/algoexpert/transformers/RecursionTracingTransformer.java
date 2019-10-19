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
	
	private final ClassPool CLASS_POOL;
	
	private final CtClass ccCallTrace; 
	
	public RecursionTracingTransformer() {
		CLASS_POOL =  ClassPool.getDefault();
		try {
			ccCallTrace = CLASS_POOL.get("hp.bootmgr.algoexpert.CallTrace");
		} catch (NotFoundException e) {
			LOG.error("Class not found", e);
			throw new RuntimeException(e);
		}
	}
	
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
						boolean isVoid = isVoid(m.getReturnType());
						
						CtMethod tracerMethod = addProxyMethod(m, cc, isVoid);
						
						if(tracerMethod == null) continue;
						
						LOG.trace("\tReplacing original method call...");
						m.setBody(getTracingCodeForRootCall(m.getName(), tracerMethod.getName(), isVoid));
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
	
	private boolean isVoid(CtClass clazz) {
		return "void".equals(clazz.getName());
	}
	
	private CtMethod addProxyMethod(final CtMethod m, final CtClass clazz, final boolean isVoid) throws CannotCompileException, NotFoundException {
		LOG.trace("\tGenerating Tracer Method...");
		final String methodName = m.getName();
		final String methodClass = m.getDeclaringClass().getName();
		final String methodSignature = m.getSignature();
		
		final String proxyMethodName = proxyMethodName(methodName);
		CtMethod copy = CtNewMethod.copy(m, proxyMethodName, clazz, null);
		copy.insertParameter(ccCallTrace);
		copy.addLocalVariable("__call", ccCallTrace);
		copy.insertBefore("__call = $1;");
		LOG.trace("\t\tInjecting Method: {} {}", copy.getName(), copy.getSignature());
		clazz.addMethod(copy);
		
		copy.instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				
				if(methodName.equals(call.getMethodName())
						&& methodClass.equals(call.getEnclosingClass().getName())
						&& methodSignature.equals(call.getSignature())) {
					LOG.trace("\t\t\tCall @ Line# {}", call.getLineNumber());
					call.replace(getTracingCodeForNestedCall(proxyMethodName, isVoid)); 
				}
				
			}
		});
		return copy;
	}
	
	private String getTracingCodeForRootCall(String originalMethodName, String tracingMethodName, boolean isVoid) {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("hp.bootmgr.algoexpert.CallTrace c = new hp.bootmgr.algoexpert.CallTrace();");
		builder.append("c.setParams($args);");
		builder.append("c.setFuncName(\"" + originalMethodName + "\");");
		
		if(!isVoid) {
			builder.append("c.setRetVal(" + tracingMethodName + "(c,$$));");
		} else {
			builder.append(tracingMethodName + "(c,$$);");
		}
		
		builder.append("hp.bootmgr.algoexpert.result.TransformationFactory.transform(c);");
		
		if(!isVoid)
			builder.append("return ($r) c.getRetVal();");
		
		builder.append("}");
		return builder.toString();
	}
	
	private String getTracingCodeForNestedCall(String methodName, boolean isVoid) {
		StringBuilder builder = new StringBuilder();
		builder.append("hp.bootmgr.algoexpert.CallTrace c = new hp.bootmgr.algoexpert.CallTrace();");
		builder.append("__call.calls.add(c);");
		builder.append("c.setParams($args);");
		
		if(!isVoid) {
			builder.append("c.setRetVal(" + methodName + "(c,$$));");
			builder.append("$_ = ($r) c.getRetVal();");
		} else {
			builder.append(methodName + "(c,$$);");
		}
		
		return builder.toString();
	}
	
	private String proxyMethodName(String orig) {
		return "$" + orig + "$";
	}
}
