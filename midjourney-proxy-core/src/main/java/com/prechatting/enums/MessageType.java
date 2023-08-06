package com.prechatting.enums;


public enum MessageType {
	/**
	 * 创建.
	 */
	CREATE,
	/**
	 * 修改.
	 */
	UPDATE,
	/**
	 * 删除.
	 */
	DELETE,

	INTERACTION_SUCCESS,

	INTERACTION_CREATE;

	public static MessageType of(String type) {
		return switch (type) {
			case "MESSAGE_CREATE" -> CREATE;
			case "MESSAGE_UPDATE" -> UPDATE;
			case "MESSAGE_DELETE" -> DELETE;
			case "INTERACTION_SUCCESS"-> INTERACTION_SUCCESS;
			case "INTERACTION_CREATE"-> INTERACTION_CREATE;
			default -> null;
		};
	}
}
