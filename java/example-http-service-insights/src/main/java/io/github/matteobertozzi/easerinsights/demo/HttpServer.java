/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.demo;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.Span.SpanState;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.rednaco.data.JsonFormat;
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
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.util.AsciiString;

public final class HttpServer {
  private static Map<String, HttpHandler> handlers = Map.of();
  private static HttpResponseHandler respHandler = null;

  private HttpServer() {
    // no-op
  }

  @FunctionalInterface
  public interface HttpHandler {
    void handle(ChannelHandlerContext ctx, FullHttpRequest req, QueryStringDecoder query) throws Exception;
  }

  @FunctionalInterface
  public interface HttpResponseHandler {
    void handle(FullHttpRequest req, QueryStringDecoder query, FullHttpResponse resp, long elapsedMs);
  }

  public static void sendResponse(final ChannelHandlerContext ctx,
      final FullHttpRequest req, final QueryStringDecoder query,
      final Object content) {
    final String json = JsonFormat.INSTANCE.asString(content);
    sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.APPLICATION_JSON, json.getBytes(StandardCharsets.UTF_8));
  }

  public static void sendResponse(final ChannelHandlerContext ctx,
      final FullHttpRequest req, final QueryStringDecoder query,
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

    final long elapsedMs = System.currentTimeMillis() - Long.parseLong(req.headers().get("X-Req-StartMs"));
    respHandler.handle(req, query, response, elapsedMs);
  }

  private static class DemoHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) {
      final QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
      try (RootSpan span = Tracer.newRootSpan()) {
        span.setName("HTTP " + req.method() + " " + req.uri());

        try {
          req.headers().set("X-Req-StartMs", System.currentTimeMillis());

          HttpHandler handler = handlers.get(queryDecoder.path());
          if (handler != null) {
            try (Span x = Tracer.newThreadLocalSpan()) {
              handler.handle(ctx, req, queryDecoder);
            }
            return;
          }

          handler = handlers.get("*");
          if (handler != null) {
            try (Span x = Tracer.newThreadLocalSpan()) {
              handler.handle(ctx, req, queryDecoder);
            }
            return;
          }

          span.setState(SpanState.USER_FAILURE);
          sendResponse(ctx, req, queryDecoder, HttpResponseStatus.NOT_FOUND, HttpHeaderValues.TEXT_PLAIN, queryDecoder.path().getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
          Logger.error(e, "failed to execute request: {} {}", req.method(), req.uri());
          span.setFailureState(e, SpanState.SYSTEM_FAILURE);
          sendResponse(ctx, req, queryDecoder, HttpResponseStatus.INTERNAL_SERVER_ERROR, HttpHeaderValues.TEXT_PLAIN, e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
      }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
      ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      Logger.error("connection failure: {}", cause.getMessage());
      ctx.close();
    }
  }

  private static class DemoHttpServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final CorsConfig CORS_CONFIG = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

    @Override
    public void initChannel(final SocketChannel ch) {
      final ChannelPipeline p = ch.pipeline();
      p.addLast(new HttpServerCodec());
      p.addLast(new CorsHandler(CORS_CONFIG));
      p.addLast(new HttpContentCompressor());
      p.addLast(new HttpObjectAggregator(1 << 20));
      p.addLast(new DemoHttpServerHandler());
    }
  }

  public static void runServer(final int port, final Map<String, HttpHandler> handlers, final HttpResponseHandler respHandler) throws InterruptedException {
    HttpServer.handlers = handlers;
    HttpServer.respHandler = respHandler;

    final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    final EventLoopGroup workerGroup = new NioEventLoopGroup(16);
    try {
      final ServerBootstrap b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new DemoHttpServerInitializer());

      final Channel ch = b.bind(port).sync().channel();
      workerGroup.scheduleAtFixedRate(() -> JvmMetrics.INSTANCE.collect(System.currentTimeMillis()), 0, 15, TimeUnit.SECONDS);

      System.out.println("Open your web browser and navigate to http://127.0.0.1:" + port + "/");

      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
