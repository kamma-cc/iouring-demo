package cc.kamma.iouringdemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
                    .childHandler(new ChannelInitializer<IOUringServerSocketChannel>() { // (4)
                        @Override
                        public void initChannel(IOUringServerSocketChannel ch) throws Exception {
                            final ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpRequestDecoder());
                            p.addLast(new HttpResponseEncoder());
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
