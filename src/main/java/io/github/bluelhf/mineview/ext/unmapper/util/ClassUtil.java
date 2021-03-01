package io.github.bluelhf.mineview.ext.unmapper.util;

import java.util.HashMap;

public class ClassUtil {

	public static String getSimpleDesc(String type) {
		StringBuilder desc = new StringBuilder();
		desc.append("[".repeat(Math.max(0, countArraySize(type))));
		type = type.replace("[]", "");
		if (VARS.containsKey(type)) {
			desc.append(VARS.get(type));
		} else {
			desc.append("L").append(type).append(";");
		}
		return desc.toString();
	}

	private static int countArraySize(String type) {
		int size = 0;
		for (byte b : type.getBytes()) {
			if (b == ']') {
				size++;
			}
		}
		return size;
	}

	public static final HashMap<String, String> VARS = new HashMap<>();

	static {
		VARS.put("byte", "B");
		VARS.put("char", "C");
		VARS.put("double", "D");
		VARS.put("float", "F");
		VARS.put("int", "I");
		VARS.put("long", "J");
		VARS.put("short", "S");
		VARS.put("boolean", "Z");
		VARS.put("void", "V");
	}

}
