package utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketUtil {
	public static String getSecWebSocketAccept(String key) {
		String secKey = getSecWebSocketKey(key);

		String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		secKey += guid;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(secKey.getBytes("iso-8859-1"), 0, secKey.length());
			byte[] sha1Hash = md.digest();
			secKey = new String(
					org.apache.mina.util.Base64.encodeBase64(sha1Hash));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String rtn = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "
				+ secKey + "\r\n\r\n";
		return rtn;
	}

	private static String getSecWebSocketKey(String req) {
		Pattern p = Pattern.compile("^(Sec-WebSocket-Key:).+",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = p.matcher(req);
		if (m.find()) {
			String foundstring = m.group();
			return foundstring.split(":")[1].trim();
		} else {
			return null;
		}

	}

	// 对传入数据进行无掩码转换
	public static byte[] encode(String msg) throws UnsupportedEncodingException {
		// 掩码开始位置
		int masking_key_startIndex = 2;

		byte[] msgByte = msg.getBytes("UTF-8");

		// 计算掩码开始位置
		if (msgByte.length <= 125) {
			masking_key_startIndex = 2;
		} else if (msgByte.length > 65536) {
			masking_key_startIndex = 10;
		} else if (msgByte.length > 125) {
			masking_key_startIndex = 4;
		}

		// 创建返回数据
		byte[] result = new byte[msgByte.length + masking_key_startIndex];

		// 开始计算ws-frame
		// frame-fin + frame-rsv1 + frame-rsv2 + frame-rsv3 + frame-opcode
		result[0] = (byte) 0x81; // 129

		// frame-masked+frame-payload-length
		// 从第9个字节开始是 1111101=125,掩码是第3-第6个数据
		// 从第9个字节开始是 1111110>=126,掩码是第5-第8个数据
		if (msgByte.length <= 125) {
			result[1] = (byte) (msgByte.length);
		} else if (msgByte.length > 65536) {
			result[1] = 0x7F; // 127
		} else if (msgByte.length > 125) {
			result[1] = 0x7E; // 126
			result[2] = (byte) (msgByte.length >> 8);
			result[3] = (byte) (msgByte.length % 256);
		}

		// 将数据编码放到最后
		for (int i = 0; i < msgByte.length; i++) {
			result[i + masking_key_startIndex] = msgByte[i];
		}

		return result;
	}
}
