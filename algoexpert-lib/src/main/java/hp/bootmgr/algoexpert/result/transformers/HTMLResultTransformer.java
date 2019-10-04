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

	private final String filePath = "/template.html";
	
	private final String outputFilePath = "trace.html";
	
	private final Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
	
	private final String template;
	
	public HTMLResultTransformer() {
		try {
			this.template = FileUtils.readClassPathFile(filePath);
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
			FileUtils.writeFile(outputFilePath, content);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	private String substitutePlaceHolders(String template, HashMap<String, String> values) {
		StringBuffer builder = new StringBuffer();
		Matcher matcher = pattern.matcher(template);
		while(matcher.find()) {
			matcher.appendReplacement(builder, values.getOrDefault(matcher.group(1), ""));
		}
		matcher.appendTail(builder);
		return builder.toString();
	}
}
