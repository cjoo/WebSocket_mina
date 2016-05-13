package mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

/**
 * 解码器（暂时根据ismask == 1有掩码来判断是否为握手数据）
 * 
 * <br>
 * 数据交互协议说明:<br>
 * 【第一个字节】暂时没用，不进行说明； <br>
 * 【第二个字节】第一位存放掩码（1:有掩码；0:无掩码）； <br>
 * 后7位表示传输的内容长度（由于7位最多只能描述127所以这个值会代表三种情况, <br>
 * 第一种是消息内容少于126存储消息长度, <br>
 * 第二种是消息长度大于等于126且少于UINT16的情况此值为126, <br>
 * 第三种是消息长度大于UINT16的情况下此值为127; <br>
 * 后两种情况的消息长度存储到紧随后面的byte[],分别是UINT16(2个字节)和UINT64(4个字节)） <br>
 * 【第三个字节-第六个字节 或第五个字节-第八个字节 或第七个字节-第十一个字节（三种情况）】掩码内容<br>
 * 【第七个字节及之后的字节 或第九个字节及之后的字节 或第十二个字节及之后的字节（三种情况）】传输的真正内容<br>
 * 
 * @author jian.cao
 * 
 */
public class MinaDecoder extends DemuxingProtocolDecoder {
	public static final byte MASK = 0x1;// 1000 0000
	public static final byte HAS_EXTEND_DATA = 126;
	public static final byte HAS_EXTEND_DATA_CONTINUE = 127;
	public static final byte PAYLOADLEN = 0x7F;// 0111 1111

	public MinaDecoder() {
		addMessageDecoder(new BaseSocketBeanDecoder());
	}

	class BaseSocketBeanDecoder extends MessageDecoderAdapter {
		public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
			if (in.remaining() < 2) {
				return NEED_DATA;
			}
			in.get();// 第一个字节
			byte head2 = in.get();// 第二个字节
			byte datalength = (byte) (head2 & PAYLOADLEN);// 得到第二个字节后七位的值
			int length = 0;
			if (datalength < HAS_EXTEND_DATA) {// 第一种是消息内容少于126存储消息长度
				length = datalength;
			} else if (datalength == HAS_EXTEND_DATA) {// 第二种是消息长度大于等于126且少于UINT16的情况此值为126
				if (in.remaining() < 2) {
					return NEED_DATA;
				}
				byte[] extended = new byte[2];
				in.get(extended);
				int shift = 0;
				length = 0;
				for (int i = extended.length - 1; i >= 0; i--) {
					length = length + ((extended[i] & 0xFF) << shift);
					shift += 8;
				}
			} else if (datalength == HAS_EXTEND_DATA_CONTINUE) {// 第三种是消息长度大于UINT16的情况下此值为127
				if (in.remaining() < 4) {
					return NEED_DATA;
				}
				byte[] extended = new byte[4];
				in.get(extended);
				int shift = 0;
				length = 0;
				for (int i = extended.length - 1; i >= 0; i--) {
					length = length + ((extended[i] & 0xFF) << shift);
					shift += 8;
				}
			}

			int ismask = head2 >> 7 & MASK;// 得到第二个字节第一位的值
			if (ismask == 1) {// 有掩码
				if (in.remaining() < 4 + length) {
					return NEED_DATA;
				} else {
					return OK;
				}
			} else {// 无掩码
				if (in.remaining() < length) {
					return NEED_DATA;
				} else {
					return OK;
				}
			}
		}

		public MessageDecoderResult decode(IoSession session, IoBuffer in,
				ProtocolDecoderOutput out) throws Exception {
			in.get();
			byte head2 = in.get();
			byte datalength = (byte) (head2 & PAYLOADLEN);
			if (datalength < HAS_EXTEND_DATA) {
			} else if (datalength == HAS_EXTEND_DATA) {
				byte[] extended = new byte[2];
				in.get(extended);
			} else if (datalength == HAS_EXTEND_DATA_CONTINUE) {
				byte[] extended = new byte[4];
				in.get(extended);
			}

			int ismask = head2 >> 7 & MASK;
			MinaBean message = new MinaBean();
			byte[] date = null;
			if (ismask == 1) {// 有掩码
				// 获取掩码
				byte[] mask = new byte[4];
				in.get(mask);

				date = new byte[in.remaining()];
				in.get(date);
				for (int i = 0; i < date.length; i++) {
					// 数据进行异或运算
					date[i] = (byte) (date[i] ^ mask[i % 4]);
				}
			} else {
				date = new byte[in.remaining()];
				in.get(date);
				message.setWebAccept(true);
			}
			message.setContent(new String(date, "UTF-8"));
			out.write(message);
			return OK;
		}
	}
}
