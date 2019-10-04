package hp.bootmgr.algoexpert.transformers;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

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
	
	private final ClassPool CLASS_POOL = ClassPool.getDefault();
	
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		CtClass cc = null;
		byte[] byteCode = null;
		try {
			cc = CLASS_POOL.get(className.replaceAll("/", "."));
		} catch (NotFoundException e) {
			System.out.println("Javassit: NotFound: " + e.getMessage());
			return null;
		}
		
		if(cc.hasAnnotation(TraceRecursion.class)) {
			System.out.println("Transforming: " + className);
			for(CtMethod m : cc.getMethods()) {
				if(m.hasAnnotation(Recursive.class)) {
					System.out.println("\tTarget Method: " + m);
					try {
						CtMethod tracerMethod = addProxyMethod(m, cc);
						
						if(tracerMethod == null) continue;
						
						System.out.println("\tReplacing original method call...");
						m.setBody("{ System.out.println(\"[Generated Method Called]\");"
								+ "hp.bootmgr.algoexpert.CallTrace c = new hp.bootmgr.algoexpert.CallTrace();"
								+ "c.setParams($args);"
								+ "c.setRetVal(" + tracerMethod.getName() + "(c,$$));"
								+ "hp.bootmgr.algoexpert.result.TransformationFactory.transform(c);"
								+ "return ($r) c.getRetVal(); }");
						
					} catch (CannotCompileException e) {
						System.out.println("Failed to modify: " + m);
						e.printStackTrace();
					} catch (NotFoundException e) {
						System.out.println("Class not found: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			
			try {
				byteCode = cc.toBytecode();
			} catch (Exception e) {
				System.out.println("Can not convert to byte code");
				e.printStackTrace();
			} finally {
				cc.detach();	
			}
		}
		
		return byteCode;
	}
	
	private CtMethod addProxyMethod(CtMethod m, CtClass clazz) throws CannotCompileException, NotFoundException {
		System.out.println("\tGenerating Tracer Method...");
		final String methodName = m.getName();
		final String methodClass = m.getClass().getName();
		final String methodSignature = m.getSignature();
		CtClass ccTraceRecorder = CLASS_POOL.get("hp.bootmgr.algoexpert.CallTrace");
		
		final String proxyMethodName = proxyMethodName(methodName);
		CtMethod copy = CtNewMethod.copy(m, proxyMethodName, clazz, null);
		copy.insertParameter(ccTraceRecorder);
		copy.addLocalVariable("__call", ccTraceRecorder);
		copy.insertBefore("__call = $1;");
		System.out.println("\t\tInjecting Method: " + copy.getName() + " " + copy.getSignature());
		clazz.addMethod(copy);
		
		copy.instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				if(methodName.equals(call.getMethodName())
						&& methodSignature.equals(call.getSignature())
						&& methodClass.equals(methodClass)) {
					System.out.println("\t\tPatching recursive calls to call generated method...");
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
