package cc.kamma.iouringdemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

@SpringBootApplication
public class IouringDemoApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(IouringDemoApplication.class, args);
        EventLoopGroup bossGroup = new IOUringEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new IOUringEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(IOUringServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            final ChannelPipeline p = ch.pipeline();
                            p.addLast("decoder", new HttpRequestDecoder());
                            p.addLast("encoder", new HttpResponseEncoder());
                            p.addLast("aggregator", new HttpObjectAggregator(1048576));
                            p.addLast("handler", new SimpleChannelInboundHandler<FullHttpMessage>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {
                                    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                                    httpResponse.headers().set(CONTENT_LENGTH, 0);
                                    ctx.writeAndFlush(httpResponse);
                                }
                            });
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(8080).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
