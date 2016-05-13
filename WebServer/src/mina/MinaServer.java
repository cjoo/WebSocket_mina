package mina;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import view.IUpdateViewFactory;
import view.MainView;

public final class MinaServer {
	public static void main(String[] args) {
		new MainView();
		open();
	}

	// 服务器监听端口
	private static final int PORT = 1984;

	public static void open() {
		// 服务器端的主要对象
		IoAcceptor acceptor = new NioSocketAcceptor();

		// 设置Filter链
		// acceptor.getFilterChain().addLast("ioFilter", new IoFilterAdapter());

		acceptor.getFilterChain().addLast("coder",
				new ProtocolCodecFilter(new MinaEncoder(), new MinaDecoder()));

		// 设置消息处理类（创建、关闭Session，可读可写等等，继承自接口IoHandler）
		acceptor.setHandler(new MinaServerHandler());
		// 设置接收缓存区大小
		acceptor.getSessionConfig().setReadBufferSize(2048);
		acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE,
				60 * 60 * 2);
		try {
			// 服务器开始监听
			acceptor.bind(new InetSocketAddress(PORT));
			IUpdateViewFactory.getUpdateView().log("服务器已启动");
		} catch (Exception e) {
			e.printStackTrace();
			IUpdateViewFactory.getUpdateView().log(e.getMessage());
		}
	}
}
