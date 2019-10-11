package hp.bootmgr.algoexpert.result.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hp.bootmgr.algoexpert.CallTrace;
import hp.bootmgr.algoexpert.result.Transformer;
import hp.bootmgr.algoexpert.utils.FileUtils;

public class HTMLResultTransformer implements Transformer {

	private final String PATH_TEMPLATE_FILE = "/template.html";
	
	private final String PATH_OUT_FILE = "%s_trace.html";
	
	private final Pattern PATRN_PLACEHOLDERS = Pattern.compile("\\$\\{([^}]+)\\}");
	
	private final String template;
	
	public HTMLResultTransformer() {
		try {
			this.template = FileUtils.readClassPathFile(PATH_TEMPLATE_FILE);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void transform(CallTrace trace) {
		try {
			HashMap<String, String> values = new HashMap<String, String>();
			values.put("json", new ObjectMapper().writeValueAsString(trace));
			String content = substitutePlaceHolders(template, values);
			FileUtils.writeFile(getOutFileName(trace), content);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	private String getOutFileName(CallTrace trace) {
		return String.format(PATH_OUT_FILE, trace.getFuncName());
	}
	
	private String substitutePlaceHolders(String template, HashMap<String, String> values) {
		StringBuffer builder = new StringBuffer();
		Matcher matcher = PATRN_PLACEHOLDERS.matcher(template);
		while(matcher.find()) {
			matcher.appendReplacement(builder, values.getOrDefault(matcher.group(1), ""));
		}
		matcher.appendTail(builder);
		return builder.toString();
	}
}
