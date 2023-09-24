package io.github.matteobertozzi.easerinsights.demo;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.matteobertozzi.easerinsights.util.ImmutableMap;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jvm.BuildInfo;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollectorRegistry;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimensions;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;

public final class DemoMain {
  public static class DemoHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final MetricDimensions execTime = new MetricDimensions.Builder()
      .dimensions("uri")
      .unit(DatumUnit.MILLISECONDS)
      .name("http_exec_time")
      .label("HTTP Exec Time")
      .register(() -> Histogram.newSingleThreaded(new long[] { 5, 10, 25, 50, 75, 100, 250, 500, 1000 }));

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) {
      try {
        final long startTime = System.currentTimeMillis();
        final QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
        switch (queryDecoder.path()) {
          case "/metrics": {
            final String report = MetricCollectorRegistry.INSTANCE.humanReport();
            sendResponse(ctx, req, HttpResponseStatus.OK, HttpHeaderValues.TEXT_PLAIN, report.getBytes(StandardCharsets.UTF_8));
            break;
          }
          case "/metrics/json": {
            sendResponse(ctx, req, MetricCollectorRegistry.INSTANCE.snapshot());
            break;
          }
          default: {
            final StringBuilder report = new StringBuilder();
            report.append("path: ").append(queryDecoder.path()).append("\n");
            report.append("params: ").append(queryDecoder.parameters()).append("\n");
            sendResponse(ctx, req, HttpResponseStatus.NOT_FOUND, HttpHeaderValues.TEXT_PLAIN, report.toString().getBytes(StandardCharsets.UTF_8));
          }
        }
        execTime.get(queryDecoder.path()).update(System.currentTimeMillis() - startTime);
      } catch (final Exception e) {
        e.printStackTrace();
        sendResponse(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, HttpHeaderValues.TEXT_PLAIN, e.getMessage().getBytes(StandardCharsets.UTF_8));
      }
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    private static void sendResponse(final ChannelHandlerContext ctx, final FullHttpRequest req, final Object content) throws JsonProcessingException {
      final String json = mapper.writeValueAsString(content);
      sendResponse(ctx, req, HttpResponseStatus.OK, HttpHeaderValues.APPLICATION_JSON, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(final ChannelHandlerContext ctx, final FullHttpRequest req,
        final HttpResponseStatus status, final AsciiString contentType, final byte[] content) {
      final boolean keepAlive = HttpUtil.isKeepAlive(req);
      final FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), status, Unpooled.wrappedBuffer(content));
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

      if (keepAlive) {
        if (!req.protocolVersion().isKeepAliveDefault()) {
          response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
      } else {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      }

      final ChannelFuture f = ctx.write(response);
      if (!keepAlive) {
        f.addListener(ChannelFutureListener.CLOSE);
      }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
      ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      cause.printStackTrace();
      ctx.close();
    }
  }

  public static class DemoHttpServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(final SocketChannel ch) {
      final ChannelPipeline p = ch.pipeline();
      p.addLast(new HttpServerCodec());
      p.addLast(new HttpContentCompressor());
      p.addLast(new HttpObjectAggregator(1 << 20));
      p.addLast(new DemoHttpServerHandler());
    }
  }

  public static void main(final String[] args) throws Exception {
    final BuildInfo buildInfo = BuildInfo.loadInfoFromManifest("example-http-service-insights");
    System.out.println(buildInfo);
    JvmMetrics.INSTANCE.setBuildInfo(buildInfo);

    // Configure the server.
    final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      final ServerBootstrap b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new DemoHttpServerInitializer());

      final Channel ch = b.bind(57025).sync().channel();
      workerGroup.scheduleAtFixedRate(() -> JvmMetrics.INSTANCE.collect(System.currentTimeMillis()), 0, 15, TimeUnit.SECONDS);

      System.out.println("Open your web browser and navigate to http://127.0.0.1:57025/");

      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
