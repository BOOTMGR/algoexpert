package hp.bootmgr.algoexpert;

import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CallTrace {
	
	private String funcName;
	
	@JsonProperty("name")
	private Object[] params;
	private Object retVal;
	
	@JsonProperty("children")
	public ArrayList<CallTrace> calls = new ArrayList<CallTrace>();
	
	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public Object getRetVal() {
		return retVal;
	}

	public void setRetVal(Object retVal) {
		this.retVal = retVal;
	}
	
	public void setRetVal(int retVal) {
		this.retVal = retVal;
	}
	
	public void setRetVal(long retVal) {
		this.retVal = retVal;
	}
	
	public void setRetVal(float retVal) {
		this.retVal = retVal;
	}
	
	public void setRetVal(double retVal) {
		this.retVal = retVal;
	}
	
	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	@Override
	public String toString() {
		return String.format("Params: %s, Return: %s, Calls: %s", Arrays.toString(params), retVal, calls);
	}
}
